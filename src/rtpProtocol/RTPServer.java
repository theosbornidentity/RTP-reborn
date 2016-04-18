package rtpProtocol;

import java.net.*;
import java.util.*;

import rtpPacket.*;
import util.*;

public class RTPServer {

  private String sIP;
  private int sPort, window;

  private DatagramSocket socket;
  private PacketBuffer buffer;

  private HashMap<String, PacketFactory> factories;
  private HashMap<String, PacketService> posts;
  private HashMap<String, PacketService> gets;

  private boolean running;

  public RTPServer (int sPort, int win) {
    this.sPort = sPort;
    this.sIP = RTPUtil.getIPAddress();
    this.window = win;
    this.factories = new HashMap<String, PacketFactory>();
    this.posts = new HashMap<String, PacketService>();
    this.gets = new HashMap<String, PacketService>();
  }

  //============================================================================
  // Start Server Methods
  //============================================================================

  public void start () {
    if(running) {
      Print.errorLn("Server is already running.");
      return;
    }

    running = true;
    socket = RTPUtil.openSocket(sPort, sIP);
    receivePackets();

    acceptConnections();
    listenForGet();
    listenForData();
    listenForFin();
  }

  public void receivePackets () {
    new Thread(new Runnable() {@Override public void run() {
      Print.statusLn("Receiving at " + sIP + ":" + sPort + "...\n");
      for(;;) {
        RTPPacket in = RTPUtil.recvPacket(socket);
        buffer.put(in);
      }
    }}).start();
  }

  //============================================================================
  // Establish connection methods
  //============================================================================

  private void acceptConnections (RTPPacket syn) {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        Print.statusLn("Accepting connections...");
        RTPPacket syn = newConnectionRequest();

        Print.recvLn("\tReceived new SYN packet");
        createConnection(syn);

        sendSYNACK(syn, factory);
      }
    }}).start();
  }

  private RTPPacket newConnectionRequest () {
    for(;;) {
      if(buffer.hasSYN()) {
        RTPPacket syn = buffer.getSYN();
        String key = syn.hash();
        boolean connectionExists = (factories.get(key) != null);
        if (!connectionExists)
          return syn;
      }
      else RTPUtil.wait();
    }
  }

  private PacketFactory createConnection(RTPPacket syn) {
    int recvWindow = syn.getWindowSize();
    PacketFactory factory = new PacketFactory(sPort, sIP, window, recvWindow);
    factories.put(syn.hash(), factory);
  }

  private void sendSYNACK (RTPPacket syn, PacketFactory factory) {
    RTPPacket synack = factory.createSynAckPacket(syn);

    for(;;) {
      RTPUtil.sendPacket(socket, synack);
      Print.sendLn("\tSent SYNACK Packet");

      RTPUtil.wait();

      if(buffer.hasGET() || buffer.hasPOST())
        return;

      Print.infoLn("\tNo response to SYNACK... resending\n");
    }
    return null;
  }

  //============================================================================
  // Data post methods
  //============================================================================

  private void listenForGet () {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        Print.statusLn("Accepting GET requests...");

        RTPPacket get = newGetRequest();
        String key = get.getHash();
        String filename = new String(get.getData());
        Print.statusLn("Received a new GET request for " + filename);

        RTPService postProcess = new RTPService(socket, factory);
        posts.put(key, postProcess);

        byte[] data = RTPUtil.getFileBytes(filename);
        processOutgoingData(filename, key, data);
      }
    }}).start();
  }

  private void newGetRequest () {
    for(;;) {
      if(buffer.hasGET()) {
        RTPPacket get = buffer.getGET();
        String key = get.hash();
        boolean getExists = (gets.get(key) != null);
        if (!getExists)
          return get;
      }
      RTPUtil.wait();
    }
  }

  private void processOutgoingData(String filename, String key, byte[] data) {
    new Thread(new Runnable() {@Override public void run() {
      posts.get(key).startPost(data);

      for(;;) {
        if(posts.get(key).isPostComplete()) {
          endPOST(filename, key);
          return;
        }
        else if(buffer.hasACK())
          posts.get(key).handleAck(buffer.getACK());
        else
          RTPUtil.wait();
      }
    }}).start();
  }

  private void endPOST(String filename, String key) {
    Print.statusLn("POST process completed for " + filename);
  }

  //============================================================================
  // Data get methods
  //============================================================================

  private void listenForData () {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        if(buffer.hasDATAFIN()) handleDataFin();
        else if(buffer.hasDATA()) handleData();
        else RTPUtil.wait();
      }
    }}).start();
  }

  private void handleData () {
    RTPPacket data = buffer.getDATA();
    String key = data.hash();
    if(openProcess(key))
      sendToProcess(key, data);
    else
      createGetProcess(key, data);
  }

  private void openProcess (String key) {
    Boolean openGetProcess = gets.containsKey(key);
    return openGetProcess;
  }

  private void sendToProcess (String key, RTPPacket data) {
    gets.get(key).handleData(data);
  }

  private void createGetProcess (String key, RTPPacket data) {
    Print.statusLn("\tNew incoming file transfer.");

    PacketFactory factory = factories.get(key);
    RTPService getProcess = new RTPService(socket, factory);
    gets.put(key, getProcess);

    gets.get(key).handleData(data);
  }

  private void handleDataFin () {
    RTPPacket dataFin = buffer.getDATAFIN();
    String key = dataFin.hash();
    if(openProcess(key))
      endGET(key);
  }

  private void endGET(String key) {
    Print.statusLn("Get process complete.");
    gets.get(key).endGetProcess();
    byte[] data = gets.get(key).getData();
    RTPUtil.createPOSTFile("ship.jpg", data);
  }

  //============================================================================
  // End connection methods
  //============================================================================

  private void listenForFin (RTPPacket syn) {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        RTPPacket fin = newFIN();
        String key = fin.hash();
        PacketFactory factory = factories.remove(key);
        sendFINACK(fin, factory);

        Print.statusLn("Connection terminated with " + fin.hash());
      }
    }}).start();
  }

  private RTPPacket newFIN () {
    for(;;) {
      if(buffer.hasFIN()) {
        RTPPacket fin = buffer.getFIN();
        String key = syn.hash();
        boolean connectionExists = (factories.get(key) != null);
        if (!connectionExists) {
          Print.recvLn("\tReceived new FIN packet");
          return fin;
        }
      }
      RTPUtil.wait();
    }
  }

  private void sendFINACK (RTPPacket fin, PacketFactory factory) {
    RTPPacket finack = factory.createFinAckPacket(fin);

    for(int i = 0; i < 3; i++) {
      RTPUtil.sendPacket(socket, finack);
      Print.sendLn("\tSent FINACK Packet");
    }
  }
}
