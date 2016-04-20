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
    //socket = RTPUtil.openSocket(sPort, sIP);
    receivePackets();
  }

  private void receivePackets () {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        //RTPPacket in = RTPUtil.recvPacket(socket);
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
    //socket.close();
    mailman.fire();

    System.exit(0);
  }

  //============================================================================
  // Methods for connecting to server
  //============================================================================

  private boolean connect () {
    if(this.connected) return false;

    this.factory = new PacketFactory(sPort, sIP, window);

    RTPPacket synack = sendSYN();
    int recvWindow = synack.getWindowSize();
    factory.setRecvWindow(recvWindow);

    p.logStatus("connected to server at " + this.dIP + ":" + this.dPort +
              " with window size " + recvWindow);

    return true;
  }

  private RTPPacket sendSYN () {
    RTPPacket syn = factory.createSYN(dPort, dIP);

    for(;;) {
      //RTPUtil.sendPacket(socket, syn, corruption);

      mailman.send(syn);
      p.logSend("sent SYN packet");

      RTPUtil.stall();

      if(buffer.hasSYNACK()) {
        RTPPacket synack = buffer.getSYNACK();
        p.logReceive("received SYNACK packet");
        return synack;
      }

      p.logInfo("no response to SYN... resending");
    }
  }

  //============================================================================
  // Data get methods
  //============================================================================

  public void get (String filename) {
    getComplete = false;

    if(!this.connected)
      connected = connect();

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

    if(!this.connected)
      connected = connect();

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
    //return new RTPService (socket, factory);
  }

  private void sendGet (String filename) {
    RTPPacket get = factory.createGET(filename.getBytes());

    for(;;) {
    //  RTPUtil.sendPacket(socket, get, corruption);
      mailman.send(get);
      p.logSend("sent GET packet for " + filename, get.getSeqNum());

      RTPUtil.stall();

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
          RTPUtil.stall();
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
          RTPUtil.stall();
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

      RTPUtil.stall();

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

}
