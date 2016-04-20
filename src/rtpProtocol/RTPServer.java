package rtpProtocol;

import java.net.*;
import java.util.*;

import rtpPacket.*;
import util.*;

public class RTPServer {

  private Printer p;

  private boolean corruption;
  private boolean logging;

  private Mailman mailman;

  private String sIP;
  private int sPort, window;

  private PacketBuffer buffer;

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

    p = new Printer(false);
    mailman = new Mailman(sPort, sIP);
  }

  //============================================================================
  // Start Server Methods
  //============================================================================

  public void start () {
    if(running) {
      Printer.errorLn("Server is already running.");
      return;
    }

    running = true;
    buffer = new PacketBuffer();

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
      p.logStatus("receiving at " + sIP + ":" + sPort + " with window size " + window);
      for(;;) {
        RTPPacket in = mailman.receive();
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

    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        RTPPacket syn = newConnectionRequest();

        p.logStatus("received a connection request");
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
    String connection = syn.hash();
    RTPPacket synack = factories.get(connection).createSYNACK(syn);

    for(;;) {
      mailman.send(synack);
      p.logStatus("sent connection confirmation");

      RTPUtil.stall(200);

      if(buffer.hasSYNFIN()) {
        RTPPacket synfin = buffer.getSYNFIN();
        String key = synfin.hash();
        if(factories.containsKey(key)) {
          factories.get(key).setConnected(true);
          long RTT = RTPUtil.longFromByte(synfin.getData());
          factories.get(key).setRTT(RTT);
          float secRTT = (float) RTT/1000;
          p.logStatus("received RTT probe of " + secRTT + " seconds");
        }
      }

      if(factories.get(connection).isConnected()) {
        p.logStatus("connected to client at " + connection);
        return;
      };

      p.logInfo("no response to SYNACK... resending");
    }
  }

  //============================================================================
  // Data post methods
  //============================================================================

  private void listenForGet () {

    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        RTPPacket get = newGetRequest();

        String key = get.hash();

        String filename = new String(get.getData());
        p.logStatus("received a GET request for " + filename);

        if(RTPUtil.filesExist(filename)) {
          RTPService postProcess = new RTPService(mailman, factories.get(key), logging);

          posts.put(key, postProcess);

          byte[] data = RTPUtil.getFileBytes(filename);
          posts.get(key).startPost(data, filename);
        }
        else {
          p.logError("requested file " + filename + " does not exist");
        }
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
    //p.logStatus("accepting ACKS");

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
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        if(buffer.hasDATA()) handleData();
        else if(buffer.hasDATAFIN()) handleDataFin();
        else RTPUtil.stall();
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

  private void handleDataFin () {
    RTPPacket datafin = buffer.getDATAFIN();
    String key = datafin.hash();
    if(openProcess(key)) endGet(key);
  }

  private boolean openProcess (String key) {
    Boolean openGetProcess = gets.containsKey(key);
    return openGetProcess;
  }

  private void sendToProcess (String key, RTPPacket data) {
    gets.get(key).handleData(data);
  }

  private void createGetProcess (String key, RTPPacket data) {
    p.logStatus("incoming DATA from " + key);

    PacketFactory factory = factories.get(key);
    RTPService getProcess = new RTPService(mailman, factory, logging);

    gets.put(key, getProcess);
    gets.get(key).startGet();
    gets.get(key).handleData(data);
  }

  private void endGet(String key) {
    p.logStatus("GET process complete");
    byte[] data = gets.get(key).getData();
    RTPUtil.createPOSTFile(data);
    gets.remove(key);
  }

  //============================================================================
  // End connection methods
  //============================================================================

  private void listenForFin () {
    for(;;) {
      RTPUtil.stall();

      RTPPacket fin = newFIN();
      String key = fin.hash();
      PacketFactory factory = factories.remove(key);
      sendFINACK(fin, factory);

      p.logStatus("connection terminated with " + fin.hash());
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
          p.logStatus("received a request to terminate connection " + key);
          return fin;
        }
      }
    }
  }

  private void sendFINACK (RTPPacket fin, PacketFactory factory) {
    RTPPacket finack = factory.createFINACK(fin);

    for(;;) {
      mailman.send(finack);
      p.logStatus("confirming connection termination");

      RTPUtil.stall(200);

      if(buffer.hasEND()) {
        RTPPacket end = buffer.getEND();
        String key = end.hash();
        if(factories.containsKey(key)) factories.remove(key);
        if(gets.containsKey(key)) gets.remove(key);
        if(posts.containsKey(key)) posts.remove(key);
        return;
      }
    }
  }

  //============================================================================
  // Testing methods
  //============================================================================

  public void setLogging(boolean l) {
    this.p = new Printer(l);
    this.logging = l;
    this.mailman.setLogging(l);
  }

  public void setCorrupted(boolean c) {
    this.corruption = c;
    this.mailman.setCorrupted(c);
  }
}
