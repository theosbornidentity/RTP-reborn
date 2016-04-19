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
  private HashMap<Integer, RTPPacket> sentPackets;
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
    if(postComplete) return;
    postComplete = false;
    unackedBytes = 0;
    acked = new HashMap<Integer, Boolean>();
    sentPackets = new HashMap<Integer, RTPPacket>();
    packetsToSend = packetize(data);

    new Thread(new Runnable() {@Override public void run() {
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
      sendPacket((RTPPacket) p, false);
    while(!postComplete)
      postComplete = resendUnacked();
    Print.statusLn("File POST successful.");
  }


  private void sendPacket(RTPPacket p, boolean isResend) {
    for(;;) {
      int bytesOut;
      if(!isResend) bytesOut = unackedBytes + p.getSize();
      else bytesOut = unackedBytes;
      boolean recvWindowFull = (bytesOut > recvWindow);

      if(!recvWindowFull) {
        RTPUtil.sendPacket(socket, p);
        if(!isResend) {
          sentPackets.put(p.getSeqNum(), p);
          unackedBytes += p.getSize();
        }
        Print.send("\tSent packet " + p.getSeqNum());
        Print.infoLn(" " + unackedBytes);
        return;
      }
      else {
        Print.infoLn("\tReceiver window full.");
        if(!isResend) resendUnacked();
        RTPUtil.stall();
      }
    }
  }

  private boolean resendUnacked() {
    boolean allAcked = true;
    Object[] packetList = sentPackets.values().toArray();
    for(Object p: packetList) {
      RTPPacket packet = (RTPPacket) p;
      boolean unacked = !(acked.get(packet.getSeqNum()) != null);
      if(unacked) {
        sendPacket(packet, true);
        allAcked = false;
      }
    }
    return allAcked;
  }

  public void handleAck (RTPPacket ack) {

    int seqNum = ack.getAckNum();
    RTPPacket ackedPacket = packetsToSend.get(seqNum);

    boolean isDuplicate = false;
    if(acked.get(seqNum) != null) isDuplicate = true;

    if(!isDuplicate) {
      Print.recvLn("\tAck received " + seqNum);
      acked.put(seqNum, true);
      unackedBytes -= ackedPacket.getSize();
    }
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
