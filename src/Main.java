import util.*;
import fta.*;

import java.util.Scanner;

public class Main {
  public static void main (String[] args) {

    Scanner scanner = new Scanner(System.in);

    Print.promptLn("\nRun client or server?");

    while(true) {
      String command = scanner.next();

      if (command.equalsIgnoreCase("server")) {
        FTAServer.run(scanner);
        return;
      }

      else if (command.equalsIgnoreCase("client")) {
        Print.statusLn("Running RTPClient...\n");
        FTAClient.run(scanner);
        return;
      }

      Print.errorLn("Invalid input.");
    }
  }
}
