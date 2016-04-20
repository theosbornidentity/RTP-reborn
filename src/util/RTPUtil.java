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

  public static Printer p = new Printer(true);

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

  public static void stall(long t) {
    try {
      Thread.sleep(t);
    } catch(InterruptedException e) {
      Printer.errorLn(e.getMessage());
    }
  }

  public static boolean filesExist (String... files) {
    try {
      for(String filename: files) {
        Path path = Paths.get("src/fta/" + filename);
        Files.readAllBytes(path);
      }
      return true;
    }
    catch (IOException e) {
      return false;
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

  public static byte[] getFileBytes(String filename, String pathway) {
    try {
      Path path = Paths.get(pathway + filename);
      return Files.readAllBytes(path);
    } catch (IOException e) {
      Printer.errorLn("Could not find file.");
      return null;
    }
  }

  public static void createGETFile(byte[] data) {
    try {
      Path path = Paths.get("src/fta/output/get_F.jpg");
      Files.write(path, data);
      p.logStatus("created file at src/fta/output/get_F.jpg");
    }
    catch (IOException e) {
      Printer.errorLn("Could not create file.");
      e.printStackTrace();
    }
  }

  public static void createPOSTFile(byte[] data) {
    try {
      Path path = Paths.get("src/fta/output/post_G.jpg");
      Files.write(path, data);
      p.logStatus("created file at src/fta/output/post_G.jpg");
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

  public static byte[] longToByte (long l) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(l);
    return buffer.array();
  }

  public static long longFromByte (byte[] b) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(b);
    buffer.flip();
    return buffer.getLong();
  }
}
