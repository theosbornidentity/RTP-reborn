package rtpProtocol;

import java.net.*;

import rtpPacket.*;
import util.*;

public class RTPClient {

  private Printer p;
  private boolean corruption;
  private boolean logging;

  private String sIP, dIP;
  private int sPort, dPort;
  private int window;

  //private DatagramSocket socket;
  private Mailman mailman;

  private PacketBuffer buffer;

  private PacketFactory factory;
  private RTPService getProcess;
  private RTPService postProcess;

  private boolean connected, getComplete, postComplete;

  public RTPClient (String dIP, int dPort, int window) {
    this.sIP = RTPUtil.getIPAddress();
    this.sPort = RTPUtil.getAvailablePort();

    this.dIP = dIP;
    this.dPort = dPort;
    this.window = window;

    this.buffer = new PacketBuffer();

    mailman = new Mailman(sPort, sIP);
    p = new Printer(false);
  }

  //============================================================================
  // Start/End Client Methods
  //============================================================================

  public boolean start () {
    receivePackets();
    this.factory = new PacketFactory(sPort, sIP, window);
    connect();
    return connected;
  }

  private void receivePackets () {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        RTPPacket in = mailman.receive();
        buffer.put(in);
      }
    }}).start();
  }

  public void disconnect () {
    if(!this.connected) {
      p.logError("client not yet connected");
      return;
    }
    sendFIN();
    logError("firing mailman and ending connection")
    mailman.fire();
    System.exit(0);
  }

  //============================================================================
  // Methods for connecting to server
  //============================================================================

  private boolean connect () {
    if(this.connected) return false;

    long startTime = System.currentTimeMillis();

    RTPPacket synack = sendSYN();

    if(synack == null) {
      p.logError("no server running at " + this.dIP + ":" + this.dPort);
      return false;
    }

    int recvWindow = synack.getWindowSize();
    factory.setRecvWindow(recvWindow);

    this.connected = sendSYNFIN(startTime);

    p.logStatus("connected to server at " + this.dIP + ":" + this.dPort +
              " with window size " + recvWindow);

    return this.connected;
  }

  private RTPPacket sendSYN () {
    RTPPacket syn = factory.createSYN(dPort, dIP);

    int timeout = 0;
    for(;;) {
      if(timeout >= 10) return null;

      mailman.send(syn);
      p.logStatus("sent connection request");

      stall();

      if(buffer.hasSYNACK()) {
        RTPPacket synack = buffer.getSYNACK();
        p.logStatus("received connection confirmation");
        return synack;
      }

      p.logInfo("no response to SYN... resending");
      timeout++;
    }
  }

  private boolean sendSYNFIN(long startTime) {
    long RTT = System.currentTimeMillis() - startTime;

    RTPPacket synfin = factory.createSYNFIN(RTT);
    factory.setRTT(RTT);

    for(;;) {
      mailman.send(synfin);
      long synfinSendTime = System.currentTimeMillis();

      float secRTT = (float) RTT/1000;
      p.logStatus("sending server RTT probe of " + secRTT + " seconds");

      stall();

      while(!buffer.hasSYNACK()) {
        boolean secondsPassed = (System.currentTimeMillis() - synfinSendTime > 2000);
        if (secondsPassed) return true;
        stall();
      }

      buffer.getSYNACK();

      p.logInfo("no response to SYNFIN... resending");
    }
  }

  //============================================================================
  // Data get methods
  //============================================================================

  public void get (String filename) {
    getComplete = false;
    buffer = new PacketBuffer();

    if(!this.connected) {
      p.logError("not yet connected to server");
      return;
    }

    getProcess = createConnectionService();
    getProcess.startGet();

    boolean receiving = sendGet(filename);
    if(receiving) processIncomingData(filename);
    else p.logError(filename + " does not exist at server");
  }

  public void getPost (String getFilename, String postFilename) {
    getComplete = false;
    postComplete = false;
    buffer = new PacketBuffer();

    if(!this.connected) {
      p.logError("not yet connected to server");
      return;
    }

    getProcess = createConnectionService();
    getProcess.startGet();

    postProcess = createConnectionService();
    byte[] data = RTPUtil.getFileBytes(postFilename);
    processOutgoingData(data, postFilename);

    boolean receiving = sendGet(getFilename);
    if(receiving) processIncomingData(getFilename);
    else p.logError(getFilename + " does not exist at server");
  }

  //============================================================================
  // Methods for GET
  //============================================================================

  private RTPService createConnectionService () {
    return new RTPService (mailman, factory, logging);
  }

  private boolean sendGet (String filename) {
    RTPPacket get = factory.createGET(filename.getBytes());

    int timeout = 0;
    for(;;) {
      if(timeout >= 10) return false;
      mailman.send(get);
      p.logSend("sent GET packet for " + filename, get.getSeqNum());

      stall();

      if(buffer.hasDATA()) return true;

      p.logInfo("no response to GET... resending");
      timeout++;
    }
  }

  private void processIncomingData (String filename) {
    p.logStatus("incoming DATA for " + filename);

    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        if(buffer.hasDATAFIN()) {
          endGet(filename);
          return;
        }
        else if(buffer.hasDATA()){
          getProcess.handleData(buffer.getDATA());
        }
        else
          stall();
      }
    }}).start();
  }

  private void endGet (String filename) {
    p.logStatus("GET process completed for " + filename);
    byte[] data = getProcess.getData();
    getProcess = null;
    RTPUtil.createGETFile(data);
    getComplete = true;
  }


  //============================================================================
  // Methods for POST
  //============================================================================

  private void processOutgoingData(byte[] data, String filename) {
    postProcess.startPost(data, filename);
    listenForAck();
  }

  private void listenForAck () {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        if(postProcess.isPostComplete()) {
          endPOST();
          return;
        }
        else if(buffer.hasACK()) {
          RTPPacket ack = buffer.getACK();
          postProcess.handleAck(ack);
        }
        else
          stall();
      }
    }}).start();
  }

  private void endPOST () {
    postProcess = null;
    postComplete = true;
  }

  //============================================================================
  // Methods for disconnecting from server
  //============================================================================

  private void sendFIN () {
    RTPPacket fin = factory.createFIN();

    for(;;) {
      p.logStatus("sending termination request");
      mailman.send(fin);

      stall();

      if(buffer.hasFINACK()) {
        p.logStatus("received confirmation of termination request");
        sendEND();
        return;
      }

      p.logInfo("no response to FIN... resending");
    }
  }

  private boolean sendEND () {
    RTPPacket end = factory.createEND();

    for(;;) {
      p.logStatus("updating server with END status");
      mailman.send(end);
      long endSendTime = System.currentTimeMillis();

      stall();

      while(!buffer.hasFINACK()) {
        boolean secondsPassed = (System.currentTimeMillis() - endSendTime > 2000);
        if (secondsPassed) return true;
        stall();
      }

      buffer.getFINACK();

      p.logInfo("no response to END... resending");
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

  public void stall() { RTPUtil.stall(factory.getRTT()); }

}
