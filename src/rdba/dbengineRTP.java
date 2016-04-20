package rdba;

import java.net.*;
import java.io.*;

import rtpProtocol.*;
import util.*;

public class dbengineRTP {

  private static final int WINDOW = 5000;
  private static Printer p = new Printer(false);

  public static void main(String[] args) throws IOException {

    try {
      if (args.length != 1)
        throw new IllegalArgumentException("Parameters: <Port>");
      int serverPort = Integer.parseInt(args[0]);

      p.statusLn("starting server from dbengineRTP");

      RTPServer server = new RTPServer(serverPort, WINDOW);
      server.start("src/fta/input/");

    } catch (IllegalArgumentException e) {
        System.out.println("\nIllegal parameters\n" + e.getMessage() + "\n");
    } catch (Exception e) {
        System.out.println("\nUnexpected exception thrown: " + e.getMessage() +"\n");
    }
  }
}
