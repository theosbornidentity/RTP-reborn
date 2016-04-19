package rtpProtocol;

import java.net.*;
import java.util.*;

import rtpPacket.*;
import util.*;

public class RTPServer {

  private final String FILETOVERIFY = "ship.jpg";

  private String sIP;
  private int sPort, window;

  private DatagramSocket socket;

  private PacketBuffer buffer;
  // private PacketBuffer dataBuffer;
  // private PacketBuffer ackBuffer;

  private HashMap<String, PacketFactory> factories;
  private HashMap<String, RTPService> posts;
  private HashMap<String, RTPService> gets;

  private boolean running;

  public RTPServer (int sPort, int win) {
    this.sPort = sPort;
    this.sIP = RTPUtil.getIPAddress();
    this.window = win;
    this.factories = new HashMap<String, PacketFactory>();
    this.posts = new HashMap<String, RTPService>();
    this.gets = new HashMap<String, RTPService>();
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

    buffer = new PacketBuffer();
    // dataBuffer = new PacketBuffer();
    // ackBuffer = new PacketBuffer();

    receivePackets();

    RTPUtil.stall();

    acceptConnections();
    listenForGet();
    listenForData();
    listenForAck();
    listenForFin();
  }

  public void receivePackets () {
    new Thread(new Runnable() {@Override public void run() {
      Print.statusLn("Receiving at " + sIP + ":" + sPort + "...\n");
      for(;;) {
        RTPPacket in = RTPUtil.recvPacket(socket);
        if(in.isType(State.DATA)) buffer.put(in);
        else if(in.isType(State.ACK)) buffer.put(in);
        else buffer.put(in);
      }
    }}).start();
  }

  //============================================================================
  // Establish connection methods
  //============================================================================

  private void acceptConnections () {
    Print.statusLn("\tAccepting SYN requests...");

    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        RTPPacket syn = newConnectionRequest();

        Print.recvLn("\tReceived new SYN packet");
        createConnection(syn);

        sendSYNACK(syn, factories.get(syn.hash()));
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
      else RTPUtil.stall();
    }
  }

  private void createConnection(RTPPacket syn) {
    int recvWindow = syn.getWindowSize();
    PacketFactory factory = new PacketFactory(sPort, sIP, window, recvWindow);
    factories.put(syn.hash(), factory);
  }

  private void sendSYNACK (RTPPacket syn, PacketFactory factory) {
    RTPPacket synack = factories.get(syn.hash()).createSYNACK(syn);

    for(;;) {
      RTPUtil.sendPacket(socket, synack);
      Print.sendLn("\tSent SYNACK Packet");

      RTPUtil.stall();

      if(factories.get(syn.hash()).isConnected()) return;

      Print.infoLn("\tNo response to SYNACK... resending\n");
    }
  }

  //============================================================================
  // Data post methods
  //============================================================================

  private void listenForGet () {
    Print.statusLn("\tAccepting GET requests...");

    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        RTPPacket get = newGetRequest();

        String key = get.hash();
        factories.get(key).setConnected(true);

        String filename = new String(get.getData());
        Print.statusLn("\tReceived a new GET request for " + filename);

        RTPService postProcess = new RTPService(socket, factories.get(key));
        posts.put(key, postProcess);

        byte[] data = RTPUtil.getFileBytes(filename);
        posts.get(key).startPost(data);
      }
    }}).start();
  }

  private RTPPacket newGetRequest () {
    for(;;) {
      RTPUtil.stall();

      if(buffer.hasGET()) {
        RTPPacket get = buffer.getGET();
        String key = get.hash();

        boolean getExists = posts.containsKey(key);
        if (!getExists)
          return get;

        boolean existingGetComplete = posts.get(key).isPostComplete();
        if (existingGetComplete) {
          posts.remove(key);
          return get;
        }

      }
    }
  }

  private void listenForAck () {
    Print.statusLn("\tAccepting ACKS...");

    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        if(buffer.hasACK()) {
          RTPPacket ack = buffer.getACK();
          posts.get(ack.hash()).handleAck(ack);
        }
        else
          RTPUtil.stall();
      }
    }}).start();
  }

  //============================================================================
  // Data get methods
  //============================================================================

  private void listenForData () {
    Print.statusLn("\tAccepting incoming DATA packets...");

    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        if(buffer.hasDATA()) handleData();
        else RTPUtil.stall();
      }
    }}).start();

    new Thread(new Runnable() {@Override public void run() {
      listenForGetFinish();
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

  private boolean openProcess (String key) {
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
    gets.get(key).startGet();
    gets.get(key).handleData(data);
  }

  private void listenForGetFinish () {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        Object[] keys = gets.keySet().toArray();
        for(Object key : keys) {
          boolean finished = gets.get(key.toString()).isGetComplete();
          if(finished) {
            endGet(key.toString());
            return;
          };
        }
        RTPUtil.stall();
      }
    }}).start();
  }

  private void endGet(String key) {
    Print.statusLn("Get process complete.");
    byte[] data = gets.get(key).getData();
    RTPUtil.createPOSTFile(FILETOVERIFY, data);
    gets.remove(key);
  }

  //============================================================================
  // End connection methods
  //============================================================================

  private void listenForFin () {
      Print.statusLn("\tAccepting FIN requests...");

      for(;;) {
        RTPUtil.stall();

        RTPPacket fin = newFIN();
        String key = fin.hash();
        PacketFactory factory = factories.remove(key);
        sendFINACK(fin, factory);

        Print.statusLn("Connection terminated with " + fin.hash());
      }
  }

  private RTPPacket newFIN () {
    for(;;) {
      RTPUtil.stall();

      if(buffer.hasFIN()) {
        RTPPacket fin = buffer.getFIN();
        String key = fin.hash();
        boolean connectionExists = (factories.get(key) != null);
        if (connectionExists) {
          Print.recvLn("\tReceived new FIN packet for connection " + key);
          return fin;
        }
      }
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
