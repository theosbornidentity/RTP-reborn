import util.*;
import fta.*;

import java.util.Scanner;

public class Main {
  public static void main (String[] args) {

    Scanner scanner = new Scanner(System.in);

    boolean corrupted = determineCorruption(scanner);
    boolean logging = determineLogging(scanner);
    boolean server = isServer(scanner);

    if(server) FTAServer.run(scanner, corrupted, logging);
    else FTAClient.run(scanner, corrupted, logging);

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
