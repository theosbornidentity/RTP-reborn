package fta;

import java.util.Scanner;
import java.io.*;
import java.nio.file.*;

import util.*;
import rtpProtocol.*;

public class FTAClient {

  private static RTPClient client;

  public static void run (Scanner scanner) {

    Print.promptLn("Running client...");
    scanner.nextLine();
    startClientPrompt(scanner);
    startClientCommandsPrompt(scanner);

  }

  public static void startClientPrompt (Scanner scanner) {
    Print.promptLn("\nPlease start the client:");
    Print.promptLn("\tfta-client [Server IP Address]:[Server Port Number] [Window Size in Bytes]\n");

    String command = scanner.nextLine();
    String[] args = command.split(" ");
    boolean validCommand = (args.length == 3) &&
                           args[0].equalsIgnoreCase("fta-client");

    if(validCommand) {
      String[] serverAddressAndPort = args[1].split(":");
      validCommand = validCommand && (serverAddressAndPort.length == 2);

      if (validCommand) {
        start(serverAddressAndPort[0],
              Integer.parseInt(serverAddressAndPort[1]),
              Integer.parseInt(args[2]));
        return;
      }
    }

    Print.errorLn("Invalid command.\n");
    startClientPrompt(scanner);
  }

  public static void startClientCommandsPrompt (Scanner scanner) {
    Print.promptLn("\nAccepted client commands:");
    Print.promptLn("\tget [filename]\n" +
                   "\tget-post [get filname] [post filename]\n" +
                   "\tdisconnect\n");

    String command = scanner.nextLine();
    String[] args = command.split(" ");

    boolean validCommand = (args.length > 0) && (args.length < 3) && (
                            (args[0].equalsIgnoreCase("get") && args.length == 2 && filesExist(args[1])) ||
                            (args[0].equalsIgnoreCase("get-post") && args.length == 3 && filesExist(args[1], args[2])) ||
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
      Print.errorLn("Invalid command.\n");
    }

    startClientCommandsPrompt(scanner);
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
      Print.errorLn("Could not find one or more files.");
      return false;
    }
  }

  public static void start (String dIP, int dPort, int window) {
    Print.promptLn("Starting client...\n" +
                   "\tDestination " + dIP + ":" + dPort + "\n" +
                   "\tWindow Size: " + window + "\n");
    client = new RTPClient(dIP, dPort, window);
    client.start();
  }

  public static void get (String getFile) {
    Print.promptLn("Downloading file " + getFile + "...\n");
    client.get(getFile);
  }

  public static void getPost (String getFile, String postFile) {
    Print.promptLn("Downloading file " + getFile + " and uploading file " + postFile + "...\n");
    client.getPost(getFile, postFile);
  }

  public static void disconnect () {
    Print.promptLn("Disconnecting from server...\n");
    client.disconnect();
  }
}
