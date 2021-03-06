package rtpPacket;

import util.*;

import java.util.concurrent.ThreadLocalRandom;

public class PacketFactory {

  private String sIP, dIP;
  private int sPort, dPort, window, recvWindow, seqNum, ackNum;
  private boolean connected;
  private long RTT;

  public PacketFactory(int sPort, String sIP, int window) {
    this.sPort = sPort;
    this.sIP = sIP;
    this.window = window;
    this.recvWindow = recvWindow;

    this.seqNum = ThreadLocalRandom.current().nextInt(0, 10000);
    this.ackNum = 0;

    this.RTT = 200;
  }

  public PacketFactory(int sPort, String sIP, int window, int recvWindow) {
    this.sPort = sPort;
    this.sIP = sIP;
    this.window = window;
    this.recvWindow = recvWindow;

    this.seqNum = ThreadLocalRandom.current().nextInt(0, 10000);
    this.ackNum = 0;

    this.RTT = 200;
  }

  public long getRTT() { return this.RTT; }

  public void setRTT(long l) { if(l != 0) this.RTT = l; }

  public int getRecvWindow () { return this.recvWindow; }

  public void setRecvWindow (int recvWindow) { this.recvWindow = recvWindow; }

  public boolean isConnected () { return this.connected; }

  public void setConnected (boolean conn) { this.connected = conn; }

  public RTPPacket createSYN (int dPort, String dIP) {
    this.dPort = dPort;
    this.dIP = dIP;
    return createPacket(State.SYN);
  }

  public RTPPacket createSYNACK (RTPPacket syn) {
    this.dPort = syn.getSourcePort();
    this.dIP = syn.getSourceIP();
    this.ackNum = syn.getSeqNum();
    return createACK(State.SYNACK);
  }

  public RTPPacket createSYNFIN (long time) {
    return createPacket(State.SYNFIN, RTPUtil.longToByte(time));
  }

  public RTPPacket createGET (byte[] filename) {
    return createPacket(State.GET, filename);
  }

  public RTPPacket[] packageBytes(byte[] data) {
    byte[][] bytes = RTPUtil.splitData(data);
    RTPPacket[] toReturn = new RTPPacket[bytes.length];
    for (int i = 0; i < bytes.length; i++)
      toReturn[i] = createPacket(State.DATA, bytes[i]);
    return toReturn;
  }

  public RTPPacket createDATAFIN () {
    return createPacket(State.DATAFIN);
  }

  public RTPPacket createACK (RTPPacket in) {
    this.ackNum = in.getSeqNum();
    return createACK(State.ACK);
  }

  public RTPPacket createFIN () {
    return createPacket(State.FIN);
  }

  public RTPPacket createFINACK (RTPPacket fin) {
    this.ackNum = fin.getSeqNum();
    return createACK(State.FINACK);
  }

  public RTPPacket createEND () {
    return createPacket(State.END);
  }

  private RTPPacket createPacket (State code) {
    RTPPacket toSend = new RTPPacket(window, code.ordinal(), seqNum, ackNum, sPort, dPort, sIP, dIP);
    this.seqNum += toSend.getPacketHeader().getPacketSize();
    return toSend;
  }

  private RTPPacket createACK (State code) {
    RTPPacket toSend = new RTPPacket(window, code.ordinal(), seqNum, ackNum, sPort, dPort, sIP, dIP);
    return toSend;
  }

  private RTPPacket createPacket (State code, byte[] data) {
    RTPPacket toSend = new RTPPacket(window, code.ordinal(), seqNum, ackNum, sPort, dPort, sIP, dIP, data);
    this.seqNum += toSend.getPacketHeader().getPacketSize();
    return toSend;
  }
}
