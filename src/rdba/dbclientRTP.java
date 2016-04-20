package rdba;

import java.net.*;
import java.io.*;

import rtpProtocol.*;
import util.*;

public class dbclientRTP {

  private static final int WINDOW = 5000;
  private static Printer p = new Printer(false);

  public static void main(String[] args) throws IOException {

    try {
      // Test for at least 2 arguments
      if (args.length < 2 || args.length > 3)
        throw new IllegalArgumentException("Parameter(s): <Server>:<Port> <get file>");

      // Test to make sure server IP address and port number both exist
      String[] sp = args[0].split(":");
      if (sp.length != 2)
        throw new IllegalArgumentException("Parameter(s): <Server>:<Port> <get file> <post file>");

      String server = sp[0];
      int serverPort = Integer.parseInt(sp[1]);

      // Assemble remaining arguments into a query to be processed by server

      RTPClient rtp = new RTPClient(server, serverPort, WINDOW);

      p.logStatus("starting dbclient file receive");

      rtp.start();

      byte[] data = new byte[0];
      if (args.length == 2) data = rtp.get(args[1]);
      else if (args.length == 3) data = rtp.getPost(args[1], args[2]);

      RTPUtil.createGETFile(data);

      RTPUtil.stall(5000);
      
      rtp.disconnect();
      p.logStatus("disconnected from server, process complete");

    } catch (IllegalArgumentException e) {
        System.out.println("\nIllegal parameters\n" + e.getMessage() + "\n");
    } catch (Exception e) {
        System.out.println("\nUnexpected exception thrown: " + e.getMessage() +"\n");
    }
  }
}
