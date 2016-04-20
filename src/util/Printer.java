package util;

public class Printer {

  public boolean logging;

  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_PURPLE = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37m";

  public Printer (boolean logging) {
    this.logging = logging;
  }
  //============================================================================
  // Logging helper methods (Yes, I'm lazy.)
  //============================================================================

  public void logSend (String s) {
    if(logging) sendLn(s);
  }

  public void logSend (String s, int n) {
    if(!logging) return;
    send(s + "\t");
    infoLn(n);
  }

  public void logReceive (String s) {
    if(logging) recvLn(s);
  }

  public void logReceive (String s, int n) {
    if(!logging) return;
    recv(s);
    infoLn("\tTotal bytes: " + n);
  }

  public void logInfo (String s) {
    if(logging) infoLn(s);
  }

  public void logStatus (String s) {
    statusLn("----" + s);
  }

  public void logCorruption (String s) {
    if(logging) errorLn("----" + s);
  }

  public void logError (String s) {
    errorLn("----" + s);
  }

  public static void success(Object m) {
    System.out.print(ANSI_GREEN + m.toString() + ANSI_RESET);
  }

  public static void successLn(Object m) {
    System.out.println(ANSI_GREEN + m.toString() + ANSI_RESET);
  }

  public static void error(Object m) {
    System.out.print(ANSI_RED + m.toString() + ANSI_RESET);
  }

  public static void errorLn(Object m) {
    System.out.println(ANSI_RED + m.toString() + ANSI_RESET);
  }

  public static void send(Object m) {
    System.out.print(ANSI_YELLOW + m.toString() + ANSI_RESET);
  }

  public static void sendLn(Object m) {
    System.out.println(ANSI_YELLOW + m.toString() + ANSI_RESET);
  }

  public static void recv(Object m) {
    System.out.print(ANSI_BLUE + m.toString() + ANSI_RESET);
  }

  public static void recvLn(Object m) {
    System.out.println(ANSI_BLUE + m.toString() + ANSI_RESET);
  }

  public static void prompt(Object m) {
    System.out.print(ANSI_WHITE + m.toString() + ANSI_RESET);
  }

  public static void promptLn(Object m) {
    System.out.println(ANSI_WHITE + m.toString() + ANSI_RESET);
  }

  public static void status(Object m) {
    System.out.print(ANSI_GREEN + m.toString() + ANSI_RESET);
  }

  public static void statusLn(Object m) {
    System.out.println(ANSI_GREEN + m.toString() + ANSI_RESET);
  }

  public static void info(Object m) {
    System.out.print(ANSI_PURPLE + m.toString() + ANSI_RESET);
  }

  public static void infoLn(Object m) {
    System.out.println(ANSI_PURPLE + m.toString() + ANSI_RESET);
  }

  public static void text(Object m) {
    System.out.print(m.toString());
  }

  public static void textLn(Object m) {
    System.out.println(m.toString());
  }
}
