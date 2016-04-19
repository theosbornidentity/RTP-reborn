package rtpPacket;

/**
 * Author: Andrew Osborn
 * Date: 3/12/2016
 *
 * RTPHeader creates a useful class for encoding and decoding byte arrays
 * that contain header information.
 *
 * Header format: [Header Size (1), Packet Size (2), Data Size (2), Window Size (4), Code (1),
 *                Sequence Number (4), Ack Number (4), Source Port (4),
 *                Destination Port (4), Source IP Length (1), Dest IP Length (1),
 *                SourceIP (varies), DestinationIP (varies)]
 *
 * Total length of header = 28 + SIP length + DIP length
 *
 */

import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;

import util.*;

public class RTPHeader {

  public static final int BASE_LENGTH = 28;

  private long checksum;
  private byte headerSize;
  private short packetSize;
  private short dataSize;
  private int windowSize;
  private byte code;
  private int seqNum;
  private int ackNum;
  private int sPort;
  private int dPort;
  private String sIP;
  private String dIP;

  /**
  * Constructors
  */

  public RTPHeader() {
    this.headerSize = (byte) BASE_LENGTH;
    this.packetSize = 0;
    this.dataSize = 0;
    this.windowSize = 0;
    this.code = 0;
    this.seqNum = 0;
    this.ackNum = 0;
    this.sPort = 0;
    this.dPort = 0;
    this.sIP = "";
    this.dIP = "";
  }

  public RTPHeader(int dataSize, int windowSize, int code, int seqNum, int ackNum,
                    int sPort, int dPort, String sIP, String dIP) {
    this.headerSize = (byte) (BASE_LENGTH + sIP.getBytes().length + dIP.getBytes().length);
    this.packetSize = (short) (this.headerSize + dataSize);
    this.dataSize = (short) dataSize;
    this.windowSize = windowSize;
    this.code = (byte) code;
    this.seqNum = seqNum;
    this.ackNum = ackNum;
    this.sPort = sPort;
    this.dPort = dPort;
    this.sIP = sIP;
    this.dIP = dIP;
  }

  /**
  * Get Methods
  */

  public int getHeaderSize() { return (int) this.headerSize; }

  public int getPacketSize() { return (int) this.packetSize; }

  public int getDataSize() { return (int) this.dataSize; }

  public int getWindowSize() { return this.windowSize; }

  public int getCode() { return (int) this.code; }

  public int getSeqNum() { return (int) this.seqNum; }

  public int getAckNum() { return (int) this.ackNum; }

  public int getSourcePort() { return this.sPort; }

  public int getDestPort() { return this.dPort; }

  public String getSourceIP() { return this.sIP; }

  public String getDestIP() { return this.dIP; }

  /**
  * Set Methods
  */

  public void setDataSize(int s) {
    this.packetSize = (short) (this.headerSize + s);
    this.dataSize = (short) s;
  }

  public void setWindowSize(int w) { this.windowSize = w; }

  public void setCode(int c) { this.code = (byte) c; }

  public void setSeqNum(int s) { this.seqNum = s; }

  public void setAckNum(int a) { this.ackNum = a; }

  public void setSourcePort(int s) { this.sPort = s; }

  public void setDestPort(int d) { this.dPort = d; }

  public void setSourceIP(String s) {
    int diff = s.getBytes().length - this.sIP.getBytes().length;
    this.headerSize += diff;
    this.packetSize += diff;
    this.sIP = s;
  }

  public void setDestIP(String d) {
    short diff = (byte) (d.getBytes().length - this.dIP.getBytes().length);
    this.headerSize += diff;
    this.packetSize += diff;
    this.dIP = d;
  }


  /**
  * Boolean Methods
  */

  public boolean isType(State s) {
    return s.ordinal() == (int) this.code;
  }

  /**
  * toByte method for converting packet header to a byte array
  *
  * Parameters: -
  * Returns: byte[]
  */

  public byte[] toBytes() {
    byte[] sArray = sIP.getBytes();
    byte[] dArray = dIP.getBytes();

    ByteBuffer buff = ByteBuffer.allocate(headerSize);

    buff
      .put(headerSize)
      .putShort(packetSize)
      .putShort(dataSize)
      .putInt(windowSize)
      .put(code)
      .putInt(seqNum)
      .putInt(ackNum)
      .putInt(sPort)
      .putInt(dPort)
      .put((byte) sArray.length)
      .put((byte) dArray.length)
      .put(sArray)
      .put(dArray);

    buff.flip();
    byte[] b = new byte[buff.remaining()];
    buff.get(b);
    return b;

  }

  /**
  * buildFromBytes sets header instance variables from byte array
  *
  * Parameters: byte[]
  * Returns: -
  */

  public void buildFromBytes(byte[] packet) {
    //Print.errorLn("header bytes: " + packet.length);

    if(packet.length == 0)
      return;

    try {
      ByteBuffer buff = ByteBuffer.wrap(packet);
      this.headerSize = buff.get();
      this.packetSize = buff.getShort();
      this.dataSize = buff.getShort();
      this.windowSize = buff.getInt();
      this.code = buff.get();
      this.seqNum = buff.getInt();
      this.ackNum = buff.getInt();
      this.sPort = buff.getInt();
      this.dPort = buff.getInt();
      int sLength = buff.get();
      int dLength = buff.get();

      byte[] bytes = new byte[sLength];
      buff.get(bytes);
      this.sIP = new String(bytes);

      buff.position(BASE_LENGTH + sLength);

      bytes = new byte[dLength];
      buff.get(bytes);
      this.dIP = new String(bytes);
   } catch (BufferOverflowException e) {
       Print.errorLn("------------header size corrupted, disposing---------------");
   }
  }

  /**
  * Testing Methods
  */

  public String toString() {
    return "Header Size (1): " + this.headerSize + "\n" +
           "Packet Size (2): " + this.packetSize + "\n" +
           "Data Size (2): " + this.dataSize + "\n" +
           "Window Size (4): " + this.windowSize + "\n" +
           "Code (1): " + this.code + "\n" +
           "Sequence Number (4): " + this.seqNum + "\n" +
           "Ack Number (4): " + this.ackNum + "\n" +
           "Source Port (4): " + this.sPort + "\n" +
           "Destination Port (4): " + this.dPort + "\n" +
           "Source IP (" + this.sIP.getBytes().length + "): " + this.sIP + "\n" +
           "Destination IP (" + this.dIP.getBytes().length + "): " + this.dIP;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null || !RTPHeader.class.isAssignableFrom(obj.getClass()))
        return false;

    final RTPHeader header = (RTPHeader) obj;

    if ( this.headerSize == header.getHeaderSize() &&
         this.dataSize == header.getDataSize() &&
         this.windowSize == header.getWindowSize() &&
         this.code == header.getCode() &&
         this.seqNum == header.getSeqNum() &&
         this.ackNum == header.getAckNum() &&
         this.sPort == header.getSourcePort() &&
         this.dPort == header.getDestPort() &&
         this.sIP.equals(header.getSourceIP())  &&
         this.dIP.equals(header.getDestIP()))
        return true;
    return false;
  }
}
