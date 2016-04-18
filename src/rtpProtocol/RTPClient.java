package rtpProtocol;

import java.net.*;

import rtpPacket.*;
import util.*;

public class RTPClient {

  private String sIP, dIP;
  private int sPort, dPort;
  private int window, recvWindow;

  private DatagramSocket socket;
  private PacketBuffer buffer;
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

    RTPPacket synack = sendSYN();
    this.recvWindow = synack.getWindowSize();
    this.factory = new PacketFactory(sPort, sIP, window, recvWindow);

    Print.statusLn("Connected to server at " + this.dIP + ":" + this.dPort +
                   "\tWindow size " + this.recvWindow + "\n");
    return true;
  }

  private RTPPacket sendSYN () {
    RTPPacket syn = factory.createSynPacket(dPort, dIP);

    for(;;) {
      RTPUtil.sendPacket(socket, syn);
      Print.sendLn("\tSent SYN Packet");

      RTPUtil.wait();

      if(buffer.hasSYNACK()) {
        Print.recvLn("\tReceived SYNACK Packet\n");
        return buffer.getSYNACK();
      }

      Print.infoLn("\tNo response to SYN... resending\n");
    }
    return null;
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
      sendGET(filename);
      processIncomingData(filename);
    }
  }

  public void getPost (String getFilename, String postFilename) {
    getComplete = false;
    postComplete = false;

    if(!this.connected)
      connected = connect();

    getProcess = createConnectionService();
    postProcess = createConnectionService();

    if(this.connected) {
      new Thread(new Runnable() {@Override public void run() {
        sendGET(filename);
        processIncomingData(getFilename);
      }}).start();

      byte[] data = RTPUtil.getFileBytes(postFilename);
      //sendPOST(postFilename, data);
      processOutgoingData(postFilename, data);
    }
  }
  
  //============================================================================
  // Methods for GET
  //============================================================================

  private RTPService createConnectionService () {
    return RTPService (socket, factory);
  }

  private void sendGET (String filename) {
    RTPPacket get = factory.createGetPacket(filename.getBytes());

    for(;;) {
      RTPUtil.sendPacket(socket, get);
      Print.sendLn("\tSent GET Packet for " + filename);

      RTPUtil.wait();

      if(buffer.hasDATA()) {
        Print.statusLn("\tIncoming data...\n");
        return;
      }

      Print.infoLn("\tNo response to GET... resending\n");
    }
  }

  private void processIncomingData (String filename) {
    for(;;) {
      if(getProcess.isGetComplete()) {
        endGET(filename);
        return;
      }
      else if(buffer.hasDATA())
        getProcess.handleData(buffer.getDATA());
      else
        RTPUtil.wait();
    }
  }

  private void endGET (String filename) {
    Print.statusLn("GET process completed for " + filename);
    byte[] data = getProcess.getData();
    getProcess = null;
    RTPUtil.createGETFile(filename);
    getComplete = true;
  }


  //============================================================================
  // Methods for POST
  //============================================================================

  private void processOutgoingData(String filename, byte[] data) {
    postProcess.startPost(data);

    for(;;) {
      if(postProcess.isPostComplete()) {
        endPOST(filename);
        return;
      }
      else if(buffer.hasACK())
        postProcess.handleAck(buffer.getACK());
      else
        RTPUtil.wait();
    }
  }

  private void endPOST (String filename) {
    Print.statusLn("POST process completed for " + filename);
    //sendDataFin();
    postProcess = null;
    postComplete = true;
  }

  private boolean recvDataFinAck(String filename) {
    if(buffer.hasDATAFIN()) {
      RTPPacket datafinack = buffer.getDATAFIN();
      String ackedFile = new String(datafinack.getData());
      if(ackedFile.equals(filename)) {
        Print.statusLn("\tServer acked DATAFIN...\n");
        return true;
      }
    }
    return false;
  }

  //============================================================================
  // Methods for disconnecting from server
  //============================================================================

  private void sendFIN () {
    RTPPacket fin = factory.createFinPacket();

    for(;;) {
      RTPUtil.sendPacket(socket, fin);
      Print.sendLn("\tSent FIN Packet");

      RTPUtil.wait();

      if(buffer.hasFINACK()) {
        Print.recvLn("\tReceived FINACK Packet\n");
        return;
      }

      Print.infoLn("\tNo response to FIN... resending\n");
    }
    return null;
  }
}

  // private void sendPOST (String filename, byte[] data) {
  //   //Print.statusLn("\tSending " + data.length + " bytes...");
  //   RTPPacket post = factory.createPostPacket(data);
  //   for(;;) {
  //     RTPUtil.sendPacket(socket, post);
  //     Print.sendLn("\tSent POST Packet for " + filename);
  //
  //     RTPUtil.wait();
  //
  //     if(buffer.hasACK()) {
  //       boolean isPostAck = buffer.getACK().getAckNum() == data.length;
  //       if(isPostAck) {
  //         Print.statusLn("\tReceived ACK for POST...\n");
  //         return;
  //       }
  //     }
  //
  //     Print.infoLn("\tNo response to POST... resending\n");
  //   }
  // }

// private void sendDataFin (String filename) {
//   RTPPacket datafin = factory.createDataFinPacket(filename.getBytes());
//
//   for(;;) {
//     RTPUtil.sendPacket(socket, datafin);
//     Print.sendLn("\tSent DATAFIN Packet for " + filename);
//
//     RTPUtil.wait();
//
//     if(recvDataFinAck(filename))
//       return;
//
//     Print.infoLn("\tNo response to DATAFIN... resending\n");
//   }
// }
