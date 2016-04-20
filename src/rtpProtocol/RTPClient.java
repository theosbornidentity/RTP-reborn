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

  public void start () {
    receivePackets();
    this.factory = new PacketFactory(sPort, sIP, window);
    connect();
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
    int recvWindow = synack.getWindowSize();
    factory.setRecvWindow(recvWindow);

    this.connected = sendSYNFIN(startTime);

    p.logStatus("connected to server at " + this.dIP + ":" + this.dPort +
              " with window size " + recvWindow);

    return this.connected;
  }

  private RTPPacket sendSYN () {
    RTPPacket syn = factory.createSYN(dPort, dIP);

    for(;;) {
      mailman.send(syn);
      p.logStatus("sent connection request");

      stall();

      if(buffer.hasSYNACK()) {
        RTPPacket synack = buffer.getSYNACK();
        p.logStatus("received connection confirmation");
        return synack;
      }

      p.logInfo("no response to SYN... resending");
    }
  }

  private boolean sendSYNFIN(long startTime) {
    long RTT = System.currentTimeMillis() - startTime;

    RTPPacket synfin = factory.createSYNFIN(RTT);
    factory.setRTT(RTT);

    for(;;) {
      mailman.send(synfin);
      float secRTT = (float) RTT/1000;
      p.logStatus("sending server RTT probe of " + secRTT + " seconds");

      stall();

      long synfinSendTime = System.currentTimeMillis();

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

    if(!this.connected) {
      p.logError("not yet connected to server");
      return;
    }

    getProcess = createConnectionService();
    getProcess.startGet();

    if(this.connected) {
      sendGet(filename);
      processIncomingData(filename);
    }
  }

  public void getPost (String getFilename, String postFilename) {
    getComplete = false;
    postComplete = false;

    if(!this.connected) {
      p.logError("not yet connected to server");
      return;
    }

    getProcess = createConnectionService();
    getProcess.startGet();

    postProcess = createConnectionService();
    byte[] data = RTPUtil.getFileBytes(postFilename);
    processOutgoingData(data);

    sendGet(getFilename);
    processIncomingData(getFilename);

  }

  //============================================================================
  // Methods for GET
  //============================================================================

  private RTPService createConnectionService () {
    return new RTPService (mailman, factory, logging);
  }

  private void sendGet (String filename) {
    RTPPacket get = factory.createGET(filename.getBytes());

    for(;;) {
      mailman.send(get);
      p.logSend("sent GET packet for " + filename, get.getSeqNum());

      stall();

      if(buffer.hasDATA()) {
        p.logStatus("incoming data");
        return;
      }

      p.logInfo("no response to GET... resending");
    }
  }

  private void processIncomingData (String filename) {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        if(getProcess.isGetComplete()) {
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
    RTPUtil.createGETFile(filename, data);
    getComplete = true;
  }


  //============================================================================
  // Methods for POST
  //============================================================================

  private void processOutgoingData(byte[] data) {
    postProcess.startPost(data);
    listenForAck();
  }

  private void listenForAck () {
    p.logStatus("accepting ACKs");

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
    p.logStatus("POST process completed");
    postProcess = null;
    postComplete = true;
  }

  //============================================================================
  // Methods for disconnecting from server
  //============================================================================

  private void sendFIN () {
    RTPPacket fin = factory.createFinPacket();

    for(;;) {
    //  RTPUtil.sendPacket(socket, fin, corruption);
      mailman.send(fin);
      p.logSend("sent FIN packet");

      stall();

      if(buffer.hasFINACK()) {
        p.logReceive("received FINACK packet");
        return;
      }

      p.logInfo("no response to FIN... resending");
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
