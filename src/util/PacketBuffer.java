package util;

import java.util.*;

public class PacketBuffer {

  ArrayList<RTPPacket> SYN;
  ArrayList<RTPPacket> SYNACK;
  ArrayList<RTPPacket> GET;
  ArrayList<RTPPacket> GETPOST;
  HashMap<Integer, RTPPacket> DATA;
  HashMap<Integer, RTPPacket> ACK;
  ArrayList<RTPPacket> DATAFIN;
  ArrayList<RTPPacket> FIN;
  ArrayList<RTPPacket> FINACK;

  public class PacketBuffer() {
    SYN = new ArrayList<RTPPacket>();
    SYNACK = new ArrayList<RTPPacket>();
    GET = new ArrayList<RTPPacket>();
    GETPOST = new ArrayList<RTPPacket>();
    DATA = new HashMap<Integer, RTPPacket>();
    ACK = new ArrayList<Integer, RTPPacket>();
    DATAFIN = new ArrayList<RTPPacket>();
    FIN = new ArrayList<RTPPacket>();
    FINACK = new ArrayList<RTPPacket>();
  }

  //============================================================================
  // PUT METHOD
  //============================================================================

  public void put(RTPPacket in) {

    if(in.isType(State.SYN)) {
      if(!SYN.contains(in)) SYN.add(in);
    }

    else if(in.isType(State.SYNACK)) {
      if(!SYNACK.contains(in)) SYNACK.add(in);
    }

    else if(in.isType(State.GET)) {
      if(!GET.contains(in)) GET.add(in);
    }

    else if(in.isType(State.GETPOST)) {
      if(!GETPOST.contains(in)) GETPOST.add(in);
    }

    else if(in.isType(State.DATA)) {
      int seqNum = in.getSeqNum();
      if(!DATA.containsKey(seqNum)) DATA.put(seqNum, in);
    }

    else if(in.isType(State.ACK)) {
      int ackNum = in.getAckNum();
      if(!DATA.containsKey(ackNum)) ACK.put(ackNum, in);
    }

    else if(in.isType(State.DATAFIN)) {
      if(!DATAFIN.contains(in)) DATAFIN.add(in);
    }

    else if(in.isType(State.FIN)) {
      if(!FIN.contains(in)) FIN.add(in);
    }

    else if(in.isType(State.FINACK)) {
      if(!FINACK.contains(in)) FINACK.add(in);
    }

    else {
      Print.errorLn("Unknown packet type at packet buffer.")
    }
  }

  //============================================================================
  // HAS METHODS
  //============================================================================

  public RTPPacket hasSYN() {
    return !SYN.isEmpty();
  }

  public RTPPacket hasSYNACK() {
    return !SYNACK.isEmpty();
  }

  public RTPPacket hasGET() {
    return !GET.isEmpty();
  }

  public RTPPacket hasGETPOST() {
    return !GETPOST.isEmpty();
  }

  public RTPPacket hasDATA(int seqNum) {
    return !DATA.containsKey(seqNum);
  }

  public RTPPacket hasACK(int ackNum) {
    return !ACK.isEmpty(ackNum);
  }

  public RTPPacket hasDATAFIN() {
    return !DATAFIN.isEmpty();
  }

  public RTPPacket hasFIN() {
    return !FIN.isEmpty();
  }

  public RTPPacket hasFINACK() {
    return !FINACK.isEmpty();
  }

  //============================================================================
  // GET METHODS
  //============================================================================

  public RTPPacket getSYN() {
    return SYN.remove(0);
  }

  public RTPPacket getSYNACK() {
    return SYNACK.remove(0);
  }

  public RTPPacket getGET() {
    return GET.remove(0);
  }

  public RTPPacket getGETPOST() {
    return GETPOST.remove(0);
  }

  public RTPPacket getDATA(int seqNum) {
    return DATA.remove(seqNum);
  }

  public RTPPacket getACK(int ackNum) {
    return ACK.remove(ackNum);
  }

  public RTPPacket getDATAFIN() {
    return DATAFIN.remove(0);
  }

  public RTPPacket getFIN() {
    return FIN.remove(0);
  }

  public RTPPacket getFINACK() {
    return FINACK.remove(0);
  }

  //============================================================================
  // OTHER METHODS
  //============================================================================

  public void empty() {
    SYN = new ArrayList<RTPPacket>();
    SYNACK = new ArrayList<RTPPacket>();
    GET = new ArrayList<RTPPacket>();
    GETPOST = new ArrayList<RTPPacket>();
    DATA = new HashMap<Integer, RTPPacket>();
    ACK = new ArrayList<Integer, RTPPacket>();
    DATAFIN = new ArrayList<RTPPacket>();
    FIN = new ArrayList<RTPPacket>();
    FINACK = new ArrayList<RTPPacket>();
  }
}
