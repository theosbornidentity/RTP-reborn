package rtpProtocol;

import java.net.*;

import rtpPacket.*;
import util.*;

public class RTPClient {

  private String sIP, dIP;
  private int sPort, dPort;
  private int window, recvWindow;

  private DatagramSocket socket;
  private PacketBuffer buffer;

  public RTPClient (String dIP, int dPort, int window) {
    this.sIP = RTPUtil.getIPAddress();
    this.sPort = RTPUtil.getAvailablePort();

    this.dIP = dIP;
    this.dPort = dPort;
    this.window = window;

    this.buffer = new PacketBuffer();

  }

  public void start () {
    socket = RTPUtil.openSocket(sPort, sIP);
    receivePackets();
  }

  private void receivePackets() {
    new Thread(new Runnable() {@Override public void run() {
      for(;;) {
        RTPPacket in = RTPUtil.recvPacket(socket);
        buffer.put(in);
      }
    }}).start();
  }

  public void get () {

  }

  public void getPost () {

  }

  public void disconnect () {

  }
}
