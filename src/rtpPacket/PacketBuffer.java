package rtpPacket;

import java.util.*;

import util.*;

public class PacketBuffer {

  ArrayList<ArrayList<RTPPacket>> queues;
  boolean queueIsBusy;

  public PacketBuffer() {
    this.queues = new ArrayList<ArrayList<RTPPacket>>();

    this.queues.add(State.SYN.ordinal(), new ArrayList<RTPPacket>());
    this.queues.add(State.SYNACK.ordinal(), new ArrayList<RTPPacket>());
    this.queues.add(State.SYNFIN.ordinal(), new ArrayList<RTPPacket>());
    this.queues.add(State.GET.ordinal(), new ArrayList<RTPPacket>());
    this.queues.add(State.DATA.ordinal(), new ArrayList<RTPPacket>());
    this.queues.add(State.DATAFIN.ordinal(), new ArrayList<RTPPacket>());
    this.queues.add(State.ACK.ordinal(), new ArrayList<RTPPacket>());
    this.queues.add(State.FIN.ordinal(), new ArrayList<RTPPacket>());
    this.queues.add(State.FINACK.ordinal(), new ArrayList<RTPPacket>());
  }

  //============================================================================
  // PUT METHOD
  //============================================================================

  public void put(RTPPacket in) {
    if (in.isType(State.SYN))          safePut(State.SYN, in);
    else if (in.isType(State.SYNACK))  safePut(State.SYNACK, in);
    else if (in.isType(State.SYNFIN))  safePut(State.SYNFIN, in);
    else if (in.isType(State.GET))     safePut(State.GET, in);
    else if (in.isType(State.DATA))    safePut(State.DATA, in);
    else if (in.isType(State.DATAFIN)) safePut(State.DATAFIN, in);
    else if (in.isType(State.ACK))     safePut(State.ACK, in);
    else if (in.isType(State.FIN))     safePut(State.FIN, in);
    else if (in.isType(State.FINACK))  safePut(State.FINACK, in);
  }

  private void safePut(State state, RTPPacket in) {
    while(queueIsBusy) { RTPUtil.delay(); }
    queueIsBusy = true;
      boolean exists = queues.get(state.ordinal()).contains(in);
      if(!exists) queues.get(state.ordinal()).add(in);
    queueIsBusy = false;
  }

  //============================================================================
  // HAS METHODS
  //============================================================================

  public boolean hasSYN() {
    return !queues.get(State.SYN.ordinal()).isEmpty();
  }

  public boolean hasSYNACK() {
    return !queues.get(State.SYNACK.ordinal()).isEmpty();
  }

  public boolean hasSYNFIN() {
    return !queues.get(State.SYNFIN.ordinal()).isEmpty();
  }

  public boolean hasGET() {
    return !queues.get(State.GET.ordinal()).isEmpty();
  }

  public boolean hasDATA() {
    return !queues.get(State.DATA.ordinal()).isEmpty();
  }

  public boolean hasDATAFIN() {
    return !queues.get(State.DATAFIN.ordinal()).isEmpty();
  }

  public boolean hasACK() {
    return !queues.get(State.ACK.ordinal()).isEmpty();
  }

  public boolean hasFIN() {
    return !queues.get(State.FIN.ordinal()).isEmpty();
  }

  public boolean hasFINACK() {
    return !queues.get(State.FINACK.ordinal()).isEmpty();
  }

  //============================================================================
  // GET METHODS
  //============================================================================

  public RTPPacket getSYN() {
    return safeGet(State.SYN);
  }

  public RTPPacket getSYNACK() {
    return safeGet(State.SYNACK);
  }

  public RTPPacket getSYNFIN() {
    return safeGet(State.SYNFIN);
  }

  public RTPPacket getGET() {
    return safeGet(State.GET);
  }

  public RTPPacket getDATA() {
    return safeGet(State.DATA);
  }

  public RTPPacket getDATAFIN() {
    return safeGet(State.DATAFIN);
  }

  public RTPPacket getACK() {
    return safeGet(State.ACK);
  }

  public RTPPacket getFIN() {
    return safeGet(State.FIN);
  }

  public RTPPacket getFINACK() {
    return safeGet(State.FINACK);
  }

  private RTPPacket safeGet(State state) {
    while(queueIsBusy) { RTPUtil.delay(); }
    queueIsBusy = true;
      RTPPacket toReturn = queues.get(state.ordinal()).remove(0);
    queueIsBusy = false;
    return toReturn;
  }
}
