package fta;

import java.util.Scanner;
import java.io.*;
import java.nio.file.*;

import util.*;
import rtpProtocol.*;

public class FTAClient {

  private static RTPClient client;

  private static boolean corrupted;
  private static boolean logging;

  public static void run (Scanner scanner, boolean c, boolean l) {
    corrupted = c;
    logging = l;

    Printer.promptLn("Running client...");
    scanner.nextLine();

    startClientPrompt(scanner);

    Printer.promptLn("\nAccepted client commands:");
    Printer.promptLn("\tget <filename>\n" +
                   "\tget-post <get filname> <post filename>\n" +
                   "\tdisconnect\n");

    startClientCommandsPrompt(scanner);
  }

  public static void startClientPrompt (Scanner scanner) {
    Printer.promptLn("\nPlease start the client:");
    Printer.promptLn("\tfta-client <Server IP Address>:<Server Port Number> <Window Size in Bytes >= 1000>\n");

    String command = scanner.nextLine();
    String[] args = command.split(" ");
    boolean validCommand = (args.length == 3) &&
                           args[0].equalsIgnoreCase("fta-client") &&
                           (Integer.parseInt(args[2]) >= 1000);

    if(validCommand) {
      String[] serverAddressAndPort = args[1].split(":");
      validCommand = validCommand && (serverAddressAndPort.length == 2);

      if (validCommand) {
        boolean started = start(serverAddressAndPort[0],
                          Integer.parseInt(serverAddressAndPort[1]),
                          Integer.parseInt(args[2]));
        if(started) return;
      }
    }

    Printer.errorLn("Invalid command.\n");
    startClientPrompt(scanner);
  }

  public static void startClientCommandsPrompt (Scanner scanner) {

    String command = scanner.nextLine();
    String[] args = command.split(" ");

    boolean validCommand = (args.length > 0) && (args.length < 4) && (
                            (args[0].equalsIgnoreCase("get") && args.length == 2) ||
                            (args[0].equalsIgnoreCase("get-post") && args.length == 3 && filesExist(args[2])) ||
                            (args[0].equalsIgnoreCase("disconnect") && args.length == 1));

    if (validCommand) {
      args[0] = args[0].toLowerCase();
      switch (args[0]) {
        case "get":        get(args[1]);
                           break;
        case "get-post":   getPost(args[1], args[2]);
                           break;
        case "disconnect": disconnect();
                           break;
      }
    }
    else {
      Printer.errorLn("Invalid command.\n");
    }

    startClientCommandsPrompt(scanner);
  }

  public static boolean filesExist (String... files) {
    try {
      for(String filename: files) {
        Path path = Paths.get("src/fta/input/" + filename);
        Files.readAllBytes(path);
      }
      return true;
    }
    catch (IOException e) {
      Printer.errorLn("Can't post a file that doesn't exist.");
      return false;
    }
  }

  public static boolean start (String dIP, int dPort, int window) {
    Printer.promptLn("Starting client...\n" +
                   "\tDestination " + dIP + ":" + dPort + "\n" +
                   "\tWindow Size: " + window + "\n");
    client = new RTPClient(dIP, dPort, window);
    client.setLogging(logging);
    client.setCorrupted(corrupted);
    return client.start();
  }

  public static void get (String getFile) {
    Printer.promptLn("Downloading file " + getFile + "...\n");
    byte[] data = client.get(getFile);
    RTPUtil.createGETFile(data);
  }

  public static void getPost (String getFile, String postFile) {
    Printer.promptLn("Downloading file " + getFile + " and uploading file " + postFile + "...\n");
    byte[] data = client.getPost(getFile, postFile);
    RTPUtil.createGETFile(data);
  }

  public static void disconnect () {
    Printer.promptLn("Disconnecting from server...\n");
    client.disconnect();
  }
}
