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

  private String postFilename;
  private int unackedBytes;
  private HashMap<Integer, RTPPacket> packetsToSend;
  private HashMap<Integer, RTPPacket> sentPackets;
  private HashMap<Integer, Boolean> acked;
  private HashMap<Integer, Long> timeSent;

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

  public void startPost (byte[] data, String filename) {
    if(postComplete) return;
    postComplete = false;
    postFilename = filename;
    unackedBytes = 0;
    acked = new HashMap<Integer, Boolean>();
    timeSent = new HashMap<Integer, Long>();
    sentPackets = new HashMap<Integer, RTPPacket>();
    packetsToSend = packetize(data);
    sendData();
  }

  private HashMap<Integer, RTPPacket> packetize (byte[] data) {
    HashMap<Integer, RTPPacket> toReturn = new HashMap<Integer, RTPPacket>();

    RTPPacket[] dataPackets = factory.packageBytes(data);
    for(RTPPacket p : dataPackets)
      toReturn.put(p.getSeqNum(), p);

    p.logStatus("sending " + postFilename + " as " + toReturn.size() + " packets");
    return toReturn;
  }

  private void sendData () {
    new Thread(new Runnable() {@Override public void run() {
      Object[] packetList = packetsToSend.values().toArray();
      p.logStatus("starting estimated RTT " + factory.getRTT());
      for(Object p : packetList)
        sendPacket((RTPPacket) p, false);
      while(!postComplete) {
        postComplete = resendUnacked();
      }
      p.logStatus("end estimated RTT " + factory.getRTT());
      sendDataFin();
      p.logStatus("POST complete for " + postFilename);
    }}).start();
  }

  private void sendPacket(RTPPacket packet, boolean isResend) {
  //  int windowFull = 0, windowOpen = 0;
    for(;;) {
      int bytesOut;
      if(!isResend) bytesOut = unackedBytes + packet.getSize();
      else bytesOut = unackedBytes;
      boolean recvWindowFull = (bytesOut > recvWindow);

      if(!recvWindowFull) {
    //    decreaseRTT();
    //    windowFull = 0; windowOpen++;
        mailman.send(packet);
        timeSent.put(packet.getSeqNum(), System.currentTimeMillis());

        if(!isResend) {
          sentPackets.put(packet.getSeqNum(), packet);
          unackedBytes += packet.getSize();
        }

        p.logSend("sent packet " + packet.getSeqNum(), unackedBytes);
        return;
      }

      else {
        // windowFull++;
        //
        // if(windowFull >= 2) {
        //   windowFull = 0;
        //   increaseRTT();
        // }

        p.logInfo("receiver window full");
        if(!isResend) resendUnacked();
      }
    }
  }

  // private void increaseRTT() {
  //   //p.logStatus("RTT was " + factory.getRTT());
  //   long newRTT = (long) (factory.getRTT() * 2;
  //   //p.logStatus("new RTT " + newRTT);
  //
  //   factory.setRTT(newRTT);
  //   float secRTT = (float) factory.getRTT()/1000;
  //   p.logStatus("adjusted connection RTT delay to " + secRTT);
  // }
  //
  // private void decreaseRTT() {
  //   long newRTT = (long) (factory.getRTT() * .99);
  //   factory.setRTT(newRTT);
  //   float secRTT = (float) factory.getRTT()/1000;
  //   p.logError("adjusted connection RTT delay to " + secRTT);
  // }

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

      Long timeOut = timeSent.get(seqNum);
      if(timeOut != null) {
        long delay = System.currentTimeMillis() - timeOut;
        updateRTT(delay);
      }

      if(ackedPacket != null)
        unackedBytes -= ackedPacket.getSize();
    }
  }

  public void sendDataFin() {
    RTPPacket datafin = factory.createDATAFIN();

    for(;;) {
      p.logSend("sending DATAFIN packet", datafin.getSeqNum());
      mailman.send(datafin);

      RTPUtil.stall();

      if(acked.get(datafin.getSeqNum()) != null) {
        p.logStatus("received DATAFIN confirmation");
        postComplete = true;
        return;
      }

      p.logInfo("no response to DATAFIN... resending");
    }

  }

  private void updateRTT(long newDelay) {
    long oldDelay = factory.getRTT();
    long newRTT = (long) ((.8*oldDelay) + (.2*newDelay));
    factory.setRTT(newRTT);
    p.logInfo("estimated RTT " + newRTT);
  }

  public boolean isPostComplete() {
    return postComplete;
  }

  public void stall() { RTPUtil.stall(factory.getRTT()); }

}
