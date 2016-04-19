package rtpProtocol;

import java.net.*;

import rtpPacket.*;
import util.*;

public class RTPClient {

  private String sIP, dIP;
  private int sPort, dPort;
  private int window;

  private DatagramSocket socket;

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
  }

  //============================================================================
  // Start/End Client Methods
  //============================================================================

  public void start () {
    socket = RTPUtil.openSocket(sPort, sIP);
    receivePackets();
  }

  private void receivePackets () {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        RTPPacket in = RTPUtil.recvPacket(socket);
        buffer.put(in);
      }
    }}).start();
  }

  public void disconnect () {
    if(!this.connected) {
      Print.errorLn("Client not yet connected.");
      return;
    }
    sendFIN();
    Print.successLn("Closing socket.");
    socket.close();
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

    Print.statusLn("Connected to server at " + this.dIP + ":" + this.dPort +
                   "\n\tWindow size " + recvWindow + "\n");
    return true;
  }

  private RTPPacket sendSYN () {
    RTPPacket syn = factory.createSYN(dPort, dIP);

    for(;;) {
      RTPUtil.sendPacket(socket, syn);
      Print.sendLn("\tSent SYN Packet");

      RTPUtil.stall();

      if(buffer.hasSYNACK()) {
        Print.recvLn("\tReceived SYNACK Packet\n");
        return buffer.getSYNACK();
      }

      Print.infoLn("\tNo response to SYN... resending\n");
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
    return new RTPService (socket, factory);
  }

  private void sendGet (String filename) {
    RTPPacket get = factory.createGET(filename.getBytes());

    for(;;) {
      RTPUtil.sendPacket(socket, get);
      Print.sendLn("\tSent GET Packet for " + filename);

      RTPUtil.stall();

      if(buffer.hasDATA()) {
        Print.statusLn("\tIncoming data...\n");
        return;
      }

      Print.infoLn("\tNo response to GET... resending\n");
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
    Print.statusLn("GET process completed for " + filename);
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
    Print.statusLn("\tAccepting ACKS...");

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
    Print.statusLn("POST process completed");
    postProcess = null;
    postComplete = true;
  }

  //============================================================================
  // Methods for disconnecting from server
  //============================================================================

  private void sendFIN () {
    RTPPacket fin = factory.createFinPacket();

    for(;;) {
      RTPUtil.sendPacket(socket, fin);
      Print.sendLn("\tSent FIN Packet");

      RTPUtil.stall();

      if(buffer.hasFINACK()) {
        Print.recvLn("\tReceived FINACK Packet\n");
        return;
      }

      Print.infoLn("\tNo response to FIN... resending\n");
    }
  }
}
