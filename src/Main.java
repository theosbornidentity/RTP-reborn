import util.*;
import fta.*;
import rdba.*;

import java.util.Scanner;
import java.io.*;

public class Main {
  public static void main (String[] args) {

    Scanner scanner = new Scanner(System.in);

    boolean rdba = determineRDBA(scanner);

    if(rdba) {
      startRDBA(scanner);
      return;
    }

    boolean corrupted = determineCorruption(scanner);
    boolean logging = determineLogging(scanner);
    boolean server = isServer(scanner);

    if(server) FTAServer.run(scanner, corrupted, logging);
    else FTAClient.run(scanner, corrupted, logging);

  }

  public static boolean determineRDBA (Scanner scanner) {
    for (;;) {
      Printer.promptLn("Run RDBA application? (Y/N)");

      String response = scanner.next();

      Boolean rdba = response.equalsIgnoreCase("y"),
              fta = response.equalsIgnoreCase("n");

      if(rdba) return true;
      else if(fta) return false;

      Printer.errorLn("Invalid input.");
    }
  }

  public static void startRDBA (Scanner scanner) {
    for (;;) {
      Printer.promptLn("Run 'client' or 'server'?");

      String response = scanner.next();

      Boolean server = response.equalsIgnoreCase("server"),
              client = response.equalsIgnoreCase("client");

      scanner.nextLine();

      try {
        if(server) {
          Printer.promptLn("\nPlease start the server:");
          Printer.promptLn("\t<Server Port Number>\n");
          String[] args = scanner.nextLine().split(" ");
          dbengineRTP.main(args);
          return;
        }
        else if(client) {
          Printer.promptLn("\nPlease start the client:");
          Printer.promptLn("\t<Server IP Address>:<Server Port Number> <get filename> <optional post filename>\n");
          String[] args = scanner.nextLine().split(" ");
          dbclientRTP.main(args);
          return;
        }
      }
      catch(IOException e) {
        Printer.errorLn("IOException thrown");
        System.exit(0);
      }
      Printer.errorLn("Invalid input.");
    }
  }

  public static boolean determineCorruption (Scanner scanner) {
    for (;;) {
      Printer.promptLn("Simulate corrupted network? (Y/N)");

      String response = scanner.next();

      Boolean simulateCorruption = response.equalsIgnoreCase("y"),
              normalNetwork = response.equalsIgnoreCase("n");

      if(simulateCorruption || normalNetwork) return simulateCorruption;

      Printer.errorLn("Invalid input.");
    }
  }

  public static boolean determineLogging (Scanner scanner) {
    for (;;) {
      Printer.promptLn("Print excessive amount of logs? (Y/N)");

      String response = scanner.next();

      Boolean excessiveLogging = response.equalsIgnoreCase("y"),
              noLogging = response.equalsIgnoreCase("n");

      if(excessiveLogging || noLogging) return excessiveLogging;

      Printer.errorLn("Invalid input.");
    }
  }

  public static boolean isServer (Scanner scanner) {
    for (;;) {
      Printer.promptLn("Run 'client' or 'server'?");

      String response = scanner.next();

      Boolean server = response.equalsIgnoreCase("server"),
              client = response.equalsIgnoreCase("client");

      if(server || client) return server;

      Printer.errorLn("Invalid input.");
    }
  }
}
