package util;

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;

import java.util.*;
import java.nio.file.*;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import rtpPacket.*;
import util.*;

public class Mailman {

  private boolean logging;
  private Printer p;

  private boolean corrupted;
  private final int CORRUPTION = 10;
  private final int MAX_DELAY = 50;

  private DatagramSocket socket;
  //private boolean open;

  public Mailman (int sPort, String sIP) {
    this.socket = RTPUtil.openSocket(sPort, sIP);
  //  this.open = true;
    this.p = new Printer(false);
  }

  public void setLogging (boolean logging) {
    this.logging = logging;
    this.p = new Printer(logging);
  }

  public void setCorrupted (boolean corrupted) {
    this.corrupted = corrupted;
  }

  //============================================================================
  // Send methods
  //============================================================================

  public void send (RTPPacket in) {
    DatagramPacket toSend = prepare(in);
    if (corrupted) unreliableSend(toSend);
    else sendNormal(toSend);
  }

  private DatagramPacket prepare (RTPPacket in) {
    byte[] bytes = in.toBytes();
    bytes = stamp(bytes);

    if(corrupted) bytes = corrupt(bytes);

    InetAddress dIP = null;
    try {
      dIP = InetAddress.getByName(in.getPacketHeader().getDestIP());
    } catch (UnknownHostException e) {
      p.logError("unknown host at IP address");
      System.exit(0);
    }

    int dPort = in.getPacketHeader().getDestPort();
    DatagramPacket toSend = new DatagramPacket(bytes, bytes.length, dIP, dPort);

    return toSend;
  }

  private byte[] stamp (byte[] bytes) {
    ByteBuffer buff = ByteBuffer.allocate(bytes.length + 8);
    buff.putLong(getStamp(bytes))
        .put(bytes);
    buff.flip();
    byte[] toReturn = new byte[buff.remaining()];
    buff.get(toReturn);
    return toReturn;
  }

  private long getStamp (byte[] bytes) {
    Checksum stamp = new Adler32();
    stamp.update(bytes, 0, bytes.length);
    return stamp.getValue();
  }

  private void sendNormal (DatagramPacket toSend) {
    try {
      socket.send(toSend);
    }
    catch (UnknownHostException e) {
      Printer.error("Invalid Destination IP Address");
      System.exit(0);
    }
    catch (IOException e) {
      Printer.error(e.getMessage());
      System.exit(0);
    }
  }

  //============================================================================
  // Corrupted send methods
  //============================================================================

  private void unreliableSend (DatagramPacket toSend) {
    Random rand = new Random();
    int testVal = rand.nextInt(100);
    if(testVal <= 100 - CORRUPTION) {
      randomDelay();
      sendNormal(toSend);
    } else {
      p.logCorruption("oh silly me, I dropped your packet");
    }
  }

  private byte[] corrupt(byte[] bytes) {
    Random rand = new Random();
    int testVal = rand.nextInt(100);
    if(testVal <= 100 - CORRUPTION) {
      return bytes;
    }
    else {
      p.logCorruption("mailman wrote in your letter");
      int indexToCorrupt = rand.nextInt(bytes.length - 1);
      bytes[indexToCorrupt] = 0;
      return bytes;
    }
  }

  public void randomDelay() {
    Random rand = new Random();
    try {
      Thread.sleep(rand.nextInt(MAX_DELAY));
    } catch(InterruptedException e) {
      Printer.errorLn(e.getMessage());
    }
  }

  //============================================================================
  // Receive methods
  //============================================================================

  public RTPPacket receive() {
    try {
      for (;;) {
        byte[] receivedBytes = new byte[RTPPacket.MAX_SIZE];
        DatagramPacket mail = new DatagramPacket(receivedBytes, RTPPacket.MAX_SIZE);
        socket.receive(mail);

        RTPPacket packet = tossIfDamaged(receivedBytes);
        if(packet != null) return packet;
      }
    }
    catch (IOException e) {
      System.exit(0);
    }
    return null;
  }

  private RTPPacket tossIfDamaged (byte[] bytes) {
    try {
      RTPPacket opened = new RTPPacket();

      ByteBuffer buff = ByteBuffer.wrap(bytes);
      long stamp = buff.getLong();

      byte[] withoutStamp = new byte[buff.remaining()];
      buff.get(withoutStamp);
      opened.buildFromBytes(withoutStamp);

      if(stamp == getStamp(opened.toBytes())) return opened;

      p.logInfo("mailman tossed damaged mail");
      return null;
   }
   catch (BufferOverflowException e) {
      p.logCorruption("mailman lost part of the packet");
      return null;
   }
  }

  public void fire () { socket.close(); }

}
