package fta;

import java.util.Scanner;

import util.*;
import rtpProtocol.*;

public class FTAServer {

  private static RTPServer server;

  private static boolean corrupted;
  private static boolean logging;

  public static void run (Scanner scanner, boolean c, boolean l) {
    corrupted = c;
    logging = l;

    Printer.promptLn("Running RTPServer...\n");
    scanner.nextLine();
    startServerPrompt(scanner);
  }

  public static void startServerPrompt(Scanner scanner) {
    Printer.promptLn("Please start the server:\n" +
                   "\tfta-server <Port Number> <Window Size in Bytes >= 1000>\n");
    String command = scanner.nextLine();
    String[] args = command.split(" ");
    boolean validCommand = (args.length == 3) &&
                           (args[0].equalsIgnoreCase("fta-server")) &&
                           (Integer.parseInt(args[2]) >= 1000);

    boolean started = false;
    if(validCommand) {
      started = start(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
      return;
    }
    else {
      Printer.errorLn("Invalid command.");
    }

    if(!started) startServerPrompt(scanner);
  }

  public static boolean start(int sPort, int window) {
    Printer.promptLn("Starting server...\n" +
                   "\tIP: " + RTPUtil.getIPAddress() + "\n" +
                   "\tPort: " + sPort + "\n" +
                   "\tWindow Size: " + window + "\n");

    server = new RTPServer(sPort, window);
    server.setLogging(logging);
    server.setCorrupted(corrupted);
    return server.start("src/fta/input/");
  }
}
