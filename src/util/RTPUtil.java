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

public class RTPUtil {

  public static String getIPAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    }
    catch (UnknownHostException e) {
      Printer.errorLn("Could not determine IP Address");
      return null;
    }
  }

  public static int getAvailablePort() {
    try {
      DatagramSocket temp = new DatagramSocket();
      int portNum = temp.getLocalPort();
      temp.close();
      return portNum;
    } catch (IOException e) {
      Printer.errorLn("\tCould not determine available port.");
      return -1;
    }
  }

  public static DatagramSocket openSocket(int sPort, String sIP) {
    try {
      return new DatagramSocket(sPort, InetAddress.getByName(sIP));
    }
    catch (SocketException e) {
      Printer.errorLn("Invalid Port Address");
      System.exit(0);
    }
    catch (UnknownHostException e) {
      Printer.errorLn("Invalid IP Address");
      System.exit(0);
    }
    return null;
  }

  public static byte[][] splitData(byte[] data) {
    int size = RTPPacket.MAX_SIZE - 100, n = (data.length)/size,
        remainder = data.length - (n*size), packets = (remainder > 0) ? n+1 : n;

    byte[][] toReturn = new byte[packets][];
    ByteBuffer buff = ByteBuffer.wrap(data);

    for(int i = 0; i < packets; i++) {
      byte[] toInsert;
      if(i == packets - 1) toInsert = new byte[data.length - size*(packets-1)];
      else toInsert = new byte[size];
      buff.get(toInsert);
      toReturn[i] = toInsert;
    }

    return toReturn;
  }

  public static void delay() {
    try {
      Thread.sleep(1);
    } catch(InterruptedException e) {
      Printer.errorLn(e.getMessage());
    }
  }

  public static void stall() {
    try {
      Thread.sleep(100);
    } catch(InterruptedException e) {
      Printer.errorLn(e.getMessage());
    }
  }

  public static void stall(int t) {
    try {
      Thread.sleep(t);
    } catch(InterruptedException e) {
      Printer.errorLn(e.getMessage());
    }
  }

  public static byte[] getFileBytes(String filename) {
    try {
      Path path = Paths.get("src/fta/" + filename);
      return Files.readAllBytes(path);
    } catch (IOException e) {
      Printer.errorLn("Could not find file.");
      return null;
    }
  }

  public static void createGETFile(String filename, byte[] data) {
    try {
      checkTransfer(filename, data);
      Path path = Paths.get("src/fta/get_F.jpg");
      Files.write(path, data);
    }
    catch (IOException e) {
      Printer.errorLn("Could not create file.");
      e.printStackTrace();
    }
  }

  public static void createPOSTFile(String filename, byte[] data) {
    try {
      checkTransfer(filename, data);
      Path path = Paths.get("src/fta/post_G.jpg");
      Files.write(path, data);
    }
    catch (IOException e) {
      Printer.errorLn("Could not create file.");
      e.printStackTrace();
    }
  }

  public static void checkTransfer(String filename, byte[] bytes) {
    byte[] original = new byte[0];
    try {
      Path path = Paths.get("src/fta/" + filename);
      original = Files.readAllBytes(path);
    } catch (IOException e) {
      Printer.errorLn("Could not check transfer.");
    }
    boolean passed = true;
    if(bytes.length != original.length)
      Printer.errorLn("Lost bytes in transfer " + original.length + ":" + bytes.length);
    else {
      for(int i = 0; i < bytes.length; i++)
        if(bytes[i] != original[i])
          passed = false;
      if(passed) Printer.successLn("PASSED.");
      else Printer.errorLn("Failed to upload image correctly.");
    }
  }

  public static byte[] buildDataFromHash(int totalBytes, HashMap<Integer, RTPPacket> packets) {
    Object[] seqNums = packets.keySet().toArray();
    Arrays.sort(seqNums);

    ByteBuffer buff = ByteBuffer.allocate(totalBytes);
    byte[] toReturn = new byte[totalBytes];

    for(Object seqNum: seqNums) {
      byte[] data = packets.get((int) seqNum).getData();
      buff.put(data);
    }

    buff.flip();
    buff.get(toReturn);
    return toReturn;
  }

}
