package rtpProtocol;

import java.net.*;
import java.util.*;

import rtpPacket.*;
import util.*;

public class RTPService {

  private boolean logging;
  private Printer p;

  private Mailman mailman;
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

  public RTPService (Mailman mailman, PacketFactory factory, boolean logging) {
    this.mailman = mailman;
    this.factory = factory;
    this.recvWindow = factory.getRecvWindow();

    this.logging = logging;
    this.p = new Printer(logging);
  }

  //============================================================================
  // GET methods
  //============================================================================

  public void startGet () {
    getComplete = false;
    lastRecvTime = System.currentTimeMillis();
    this.receivedPackets = new HashMap<Integer, RTPPacket>();
  }

  public boolean handleData (RTPPacket data) {
    Boolean notReceived = !receivedPackets.containsKey(data.getSeqNum());
    if(notReceived) bufferData(data);
    sendAck(data);
    return false;
  }

  private void bufferData (RTPPacket data) {
    lastRecvTime = System.currentTimeMillis();

    int key = data.getSeqNum();
    receivedPackets.put(key, data);
    recvDataBytes += data.getDataSize();
    p.logReceive("received DATA packet " + key, recvDataBytes);
  }

  private void sendAck(RTPPacket data) {
    RTPPacket ack = factory.createACK(data);
    mailman.send(ack);
    p.logSend("sent ACK ", ack.getAckNum());
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

    p.logStatus("packets to send: " + toReturn.size());
    return toReturn;
  }

  private void sendData () {
    Object[] packetList = packetsToSend.values().toArray();
    for(Object p : packetList)
      sendPacket((RTPPacket) p, false);
    while(!postComplete) {
      postComplete = resendUnacked();
    }
    sendDataFin();
  }

  private void sendPacket(RTPPacket packet, boolean isResend) {
    for(;;) {
      int bytesOut;
      if(!isResend) bytesOut = unackedBytes + packet.getSize();
      else bytesOut = unackedBytes;
      boolean recvWindowFull = (bytesOut > recvWindow);

      if(!recvWindowFull) {
        mailman.send(packet);
        if(!isResend) {
          sentPackets.put(packet.getSeqNum(), packet);
          unackedBytes += packet.getSize();
        }
        p.logSend("sent packet " + packet.getSeqNum(), unackedBytes);
        return;
      }
      else {
        p.logInfo("receiver window full");
        if(!isResend) resendUnacked();
        stall();
      }
    }
  }

  private boolean resendUnacked() {
    stall();

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
      p.logReceive("ACK received " + seqNum);
      acked.put(seqNum, true);
      unackedBytes -= ackedPacket.getSize();
    }
  }

  public void sendDataFin() {
    RTPPacket datafin = factory.createDATAFIN();
    p.logSend("sending DATAFIN packet", datafin.getSeqNum());
    sendPacket(datafin, false);
    p.logStatus("POST completed");
  }

  public boolean isPostComplete() {
    return postComplete;
  }

  public void stall() { RTPUtil.stall(factory.getRTT()); }

}
