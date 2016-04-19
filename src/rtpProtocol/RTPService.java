package rtpProtocol;

import java.net.*;
import java.util.*;

import rtpPacket.*;
import util.*;

public class RTPService {

  private DatagramSocket socket;
  private PacketFactory factory;
  private int recvWindow;

  private int unackedBytes;
  private HashMap<Integer, RTPPacket> packetsToSend;
  private HashMap<Integer, Boolean> acked;
  private boolean postComplete;

  private int recvDataBytes;
  private long lastRecvTime;
  private HashMap<Integer, RTPPacket> receivedPackets;
  private boolean getComplete;

  public RTPService (DatagramSocket socket, PacketFactory factory) {
    this.socket = socket;
    this.factory = factory;
    this.recvWindow = factory.getRecvWindow();
  }

  //============================================================================
  // GET methods
  //============================================================================

  public void startGet () {
    getComplete = false;
    lastRecvTime = System.currentTimeMillis();
    this.receivedPackets = new HashMap<Integer, RTPPacket>();

    new Thread(new Runnable() {@Override public void run() {
      updateGetStatus();
    }}).start();
  }

  public boolean handleData (RTPPacket data) {
    if(getComplete) return true;
    Boolean notReceived = !receivedPackets.containsKey(data.getSeqNum());
    if(notReceived) bufferData(data);
    sendAck(data);
    return false;
  }

  private void updateGetStatus () {
    for(;;) {
      RTPUtil.stall();

      boolean noRecentUpdate = (System.currentTimeMillis() - lastRecvTime) > 5000;

      if(noRecentUpdate) {
        getComplete = true;
        Print.statusLn("GET completed.");
        return;
      }
    }
  }

  private void bufferData (RTPPacket data) {
    lastRecvTime = System.currentTimeMillis();

    int key = data.getSeqNum();
    receivedPackets.put(key, data);
    recvDataBytes += data.getDataSize();
    Print.recv("\tReceived packet " + key + " ");
    Print.infoLn(recvDataBytes);
  }

  private void sendAck(RTPPacket data) {
    RTPPacket ack = factory.createACK(data);
    RTPUtil.sendPacket(socket, ack);
    Print.sendLn("\tSent ACK " + ack.getAckNum());
  }

  public boolean isGetComplete () {
    return getComplete;
  }

  public byte[] getData () {
    return RTPUtil.buildDataFromHash(recvDataBytes, receivedPackets);
  }

  //============================================================================
  // Post methods
  //============================================================================

  public void startPost (byte[] data) {
    new Thread(new Runnable() {@Override public void run() {
      if(postComplete) return;
      postComplete = false;
      unackedBytes = 0;
      packetsToSend = packetize(data);
      acked = new HashMap<Integer, Boolean>();
      sendData();
    }}).start();
  }

  private HashMap<Integer, RTPPacket> packetize (byte[] data) {
    HashMap<Integer, RTPPacket> toReturn = new HashMap<Integer, RTPPacket>();

    RTPPacket[] dataPackets = factory.packageBytes(data);
    for(RTPPacket p : dataPackets)
      toReturn.put(p.getSeqNum(), p);

    Print.statusLn("\tPackets to send: " + toReturn.size());
    return toReturn;
  }

  private void sendData () {
    Object[] packetList = packetsToSend.values().toArray();
    for(Object p : packetList)
      sendPacket((RTPPacket) p);
    while(!postComplete)
      postComplete = resendUnacked();
  }


  private void sendPacket(RTPPacket p) {
    for(;;) {
      int bytesOut = unackedBytes + p.getSize();
      boolean recvWindowFull = (bytesOut > recvWindow);

      if(!recvWindowFull) {
        RTPUtil.sendPacket(socket, p);
        unackedBytes += p.getSize();
        Print.send("\tSent packet " + p.getSeqNum());
        Print.infoLn(" " + unackedBytes);
        return;
      }
      else {
        Print.infoLn("\tReceiver window full.");
        RTPUtil.stall();
      }
    }
  }

  private boolean resendUnacked() {
    boolean allAcked = true;
    Object[] packetList = packetsToSend.values().toArray();
    for(Object p: packetList) {
      RTPPacket packet = (RTPPacket) p;
      boolean unacked = !(acked.get(packet.getSeqNum()) != null);
      if(unacked) {
        sendPacket(packet);
        allAcked = false;
      }
    }
    return false;
  }

  public void handleAck (RTPPacket ack) {
    int seqNum = ack.getAckNum();
    Print.recvLn("\tAck received " + seqNum);
    acked.put(seqNum, true);
    unackedBytes -= packetsToSend.get(seqNum).getSize();
  }

  public boolean isPostComplete() {
    return postComplete;
  }
}

// private RTPPacket getNextPacket (int seqNum) {
//   int timer = 0;
//   for (;;) {
//     if(timer > 50) getComplete = true;
//     boolean received = receivedPackets.containsKey(seqNum);
//     if(received) return receivedPackets.get(seqNum);
//     else {
//       RTPUtil.stall();
//       timer++;
//     }
//   }
// }
