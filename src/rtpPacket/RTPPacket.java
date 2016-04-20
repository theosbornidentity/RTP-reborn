package rtpPacket;

import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;

import util.*;

/**
 * Author: Andrew Osborn
 * Date: 3/12/2016
 *
 * RTPPacket creates a useful class for building packets to be sent.
 */

public class RTPPacket {

  public static final int MAX_SIZE = 1000;
  private RTPHeader header;
  private byte[] data;

  /**
  * Constructors
  */

  public RTPPacket () {
    this.header = new RTPHeader();
    this.data = new byte[0];
  }

  public RTPPacket (RTPHeader header, byte[] data) {
    this.header = header;
    this.data = data;
    this.header.setDataSize(data.length);
  }

  public RTPPacket (int win, int code, int seqNum, int ackNum,
                    int sPort, int dPort, String sIP, String dIP) {
    this.header =
      new RTPHeader(0, win, code, seqNum, ackNum, sPort, dPort, sIP, dIP);
    this.data = new byte[0];
  }

  public RTPPacket (int win, int code, int seqNum, int ackNum,
                    int sPort, int dPort, String sIP, String dIP, byte[] data) {
    this.header =
      new RTPHeader(data.length, win, code, seqNum, ackNum, sPort, dPort, sIP, dIP);
    this.data = data;
  }

  /**
  * Get Methods
  */

  public RTPHeader getPacketHeader() { return this.header; }

  public byte[] getData() { return this.data; }

  public int getDataSize() { return this.data.length; }

  public int getHeaderSize() { return this.header.getHeaderSize(); }

  public int getSize() { return this.header.getPacketSize(); }

  public int getWindowSize() { return this.header.getWindowSize(); }

  public int getCode() { return this.header.getCode(); }

  public int getSeqNum() { return this.header.getSeqNum(); }

  public int getAckNum() { return this.header.getAckNum(); }

  public int getSourcePort() { return this.header.getSourcePort(); }

  public int getDestPort() { return this.header.getDestPort(); }

  public String getSourceIP() { return this.header.getSourceIP(); }

  public String getDestIP() { return this.header.getDestIP(); }

  /**
  * Set Methods
  */

  public void setPacketHeader(RTPHeader header) { this.header = header; }

  public void setPacketHeader(byte[] bytes) { this.header.buildFromBytes(bytes); }

  public void setData(byte[] d) {
    this.data = d;
    this.header.setDataSize(data.length);
  }

  public void setDataSize(int s) { this.header.setDataSize(s); }

  public void setWindowSize(int w) { this.header.setWindowSize(w); }

  public void setCode(int c) { this.header.setCode(c); }

  public void setSeqNum(int s) { this.header.setSeqNum(s); }

  public void setAckNum(int a) { this.header.setAckNum(a); }

  public void setSourcePort(int s) { this.header.setSourcePort(s); }

  public void setDestPort(int d) { this.header.setDestPort(d); }

  public void setSourceIP(String s) { this.header.setSourceIP(s); }

  public void setDestIP(String d) { this.header.setDestIP(d); }

  /**
  * Boolean Methods
  */

  public boolean isType(State s) {
    return this.header.isType(s);
  }

  /**
  * toByte method for converting packet to a byte array
  *
  * Parameters: -
  * Returns: byte[]
  */

  public byte[] toBytes() {
    ByteBuffer buff = ByteBuffer.allocate(this.getSize());
    buff.put(this.header.toBytes());
    buff.put(this.data);
    buff.flip();
    byte[] bytes = new byte[buff.remaining()];
    buff.get(bytes);
    return bytes;
  }

  /**
  * buildFromBytes sets packet instance variables from byte array
  *
  * Parameters: byte[]
  * Returns: -
  */

  public void buildFromBytes(byte[] packet) {
    try {
      ByteBuffer buff = ByteBuffer.wrap(packet);
      int headerSize = buff.get();
      buff = ByteBuffer.wrap(packet);
      byte[] headerBytes = new byte[headerSize];
      buff.get(headerBytes);
      this.header.buildFromBytes(headerBytes);
      byte[] dataBytes = new byte[header.getDataSize()];
      buff.get(dataBytes);
      this.data = dataBytes;
    } catch (BufferOverflowException e) {
        Printer.errorLn("------------header size corrupted, disposing---------------");
    }
  }

  /**
  * Helper Methods
  */

  public String hash() {
    return this.header.getSourceIP() + ":" + this.header.getSourcePort();
  }

  /**
  * Testing Methods
  */

  public String toString () {
    return this.header.toString() + "\nData: (" + this.data.length + ")" + this.data.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null || !RTPPacket.class.isAssignableFrom(obj.getClass()))
        return false;

    final RTPPacket packet = (RTPPacket) obj;

    if ( this.header.equals(packet.getPacketHeader()) &&
         this.data == packet.getData())
        return true;

    return false;
  }
}
