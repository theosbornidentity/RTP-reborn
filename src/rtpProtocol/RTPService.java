package rtpProtocol;

import java.net.*;
import java.util.*;

import rtpPacket.*;
import util.*;

public class RTPService {

  private DatagramSocket socket;
  private PacketFactory factory;
  private int recvWindow;

  private int numUnackedBytes;
  private ArrayList<RTPPacket> packetsToSend;
  private ArrayList<RTPPacket> unackedPackets;
  private boolean postComplete;

  private int recvDataBytes, recvTimer;
  private Hashmap<Integer, RTPPacket> receivedPackets;
  private boolean getComplete;

  public PacketService (DatagramSocket socket, PacketFactory factory) {
    this.socket = socket;
    this.factory = factory;
    this.recvWindow = factory.getRecvWindow();
  }

  //============================================================================
  // GET methods
  //============================================================================

  public void startGet () {
    getComplete = false;
    recvTimer = 0;
    this.receivedPackets = Hashmap<Integer, RTPPacket>();
  }

  private RTPPacket getNextPacket (int seqNum) {
    int timer = 0;
    for (;;) {
      if(timer > 50) getComplete = true;
      boolean received = receivedPackets.containsKey(seqNum);
      if(received) return receivedPackets.get(seqNum);
      else {
        RTPUtil.wait();
        waitTime++;
      }
    }
  }

  public void handleData (RTPPacket data) {
    updateGetStatus();
    if(getComplete) return;
    Boolean notReceived = !receivedPackets.containsKey(data.getSeqNum());
    if(notReceived) bufferData(data);
    sendAck(data);
  }

  private void updateGetStatus () {
    if(recvTimer > 50) {
      getComplete = true;
      Print.statusLn("GET completed.");
      return;
    }
  }

  private void bufferData (RTPPacket data) {
    String key = data.getSeqNum();
    receivedPackets.put(key, data);
    recvDataBytes += data.getDataSize();
    Print.recv("\tReceived packet " + key + " ");
    Print.infoLn(recvDataBytes);
  }

  private void sendAck(RTPPacket data) {
    recvTimer = 0;
    RTPPacket ack = factory.createACK(data);
    RTPUtil.sendPacket(socket, ack);
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
    postComplete = false;

    numPacketsSent = 0;
    numUnackedBytes = 0;

    packetsToSend = packetize(data);
    unackedPackets = new ArrayList<RTPPacket>();

    sendData(packetsToSend);
    resendData();
  }

  private void packetize(data) {
    HashMap<Integer, RTPPacket> toReturn = new HashMap<Integer, RTPPacket>();

    RTPPacket[] dataPackets = factory.packageBytes();
    toReturn.addAll(dataPackets);

    Print.statusLn("\tPackets to send: " + toReturn.size());
    return toReturn;
  }

  private void sendData (ArrayList<RTPPacket> toSend) {
    new Thread(new Runnable() {@Override public void run() {
      for(RTPPacket p : toSend) {
        int bytesOut = unackedBytes + p.getSize();
        boolean recvWindowFull = (bytesOut > recvWindow);

        if(!recvWindowFull) {
          RTPUtil.sendPacket(socket, p);
          unackedBytes += p.getSize();
          unackedPackets.add(p);
          Print.send("\tSent packet " + p.getSeqNum());
          Print.infoLn(" " + unackedPackets.size() + ":" + unackedBytes);
        }

        else {
          Print.infoLn("\tReceiver window full.");
        }
      }
    }}).start();
  }

  private void resendData () {
    new Thread(new Runnable() {@Override public void run() {
      int timer = 0;
      while(!postComplete) {
        boolean hasUnackedPackets = !unackedPackets.isEmpty();
        if(hasUnackedPackets) {
          Print.sendLn("\tResending " + unackedPackets.size() + " packets.")
          sendData(unackedPackets);
          timer++;
        }
        else {
          Print.statusLn("\tNo unacked packets.")
          RTPUtil.wait();
          timer++;
        }
        if(timer > 50) postComplete = true;
      }
    }}).start();
  }

  public void handleAck (RTPPacket ack) {
    int dataSeqNum = ack.getAckNum();
    for(RTPPacket p : unackedPackets) {
      if(p.getSeqNum == dataSeqNum) {
        unackedPackets.remove(p);
        unackedBytes -= p.getSize();
        return;
      }
    }
  }

  public boolean isPostComplete() {
    return postComplete;
  }
}
