package fta;

import java.util.Scanner;

import util.Print;
import rtpProtocol.*;

public class FTAServer {

  private static RTPServer server;
  private static Scanner scanner;

  public static void run (Scanner scanner) {
    this.scanner = scanner;

    Print.promptLn("Running server...\n");
    startServerPrompt();
    startClientCommandsPrompt();

  }

  public static void startServerPrompt() {
    Print.promptLn("Please start the server:\n" +
                   "\tfta-server [Port Number] [Window Size in Bytes > 1000]\n");

    String command = scanner.nextLine();
    String[] args = command.split(" ");
    boolean validCommand = (args.length == 3) &&
                           (args[0].equalsIgnoreCase("fta-server")) &&
                           (Integer.parseInt(args[2]) > 1000);

    if(validCommand) {
      start(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
      return;
    }

    Print.errorLn("Invalid command.");
    startServerPrompt();
  }

  public static void start(int sPort, int window) {
    Print.promptLn("Starting client...\n" +
                   "\tPort: " + sPort + "\n" +
                   "\tWindow Size: " + window + "\n");
    server = new RTPServer(sPort, window);
    server.start();
  }
}
