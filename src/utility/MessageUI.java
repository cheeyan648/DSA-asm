package utility;

import java.util.Scanner;

/**
 *
 * @author Kat Tan
 */
public class MessageUI {

  /**
   * One shared Scanner for the whole program. Every UI class must use this
   * instead of creating its own "new Scanner(System.in)" - multiple Scanners
   * on System.in buffer ahead and steal each other's input, which makes the
   * program skip prompts or crash with NoSuchElementException.
   */
  public static final Scanner scanner = new Scanner(System.in);


  public static void displayInvalidChoiceMessage() {
    System.out.println("\nInvalid choice");
  }

  /**
   * Shows an invalid-choice error that also tells the user the valid range,
   * e.g. "Please enter a number from 1 to 4, or 0 to exit."
   *
   * @param maxOption the highest valid menu option (options run 1..maxOption)
   * @param exitLabel what option 0 does, e.g. "exit" or "go back"
   */
  public static void displayInvalidChoiceMessage(int maxOption, String exitLabel) {
    System.out.println("\nInvalid choice!");
    System.out.println("Please enter a number from 1 to " + maxOption
        + ", or 0 to " + exitLabel + ".");
  }

  /**
   * Reads a menu choice, re-prompting until the user enters a whole number
   * in the range 0..maxOption. Non-numeric input (e.g. letters) is rejected
   * with the same error instead of crashing. The screen is never cleared
   * while re-prompting, so the user can see what went wrong.
   *
   * @param scanner the Scanner to read from
   * @param maxOption the highest valid menu option (options run 1..maxOption)
   * @param exitLabel what option 0 does, e.g. "exit" or "go back"
   * @return a valid choice between 0 and maxOption
   */
  public static int readMenuChoice(Scanner scanner, int maxOption, String exitLabel) {
    while (true) {
      System.out.print("Enter choice: ");
      String input = scanner.nextLine().trim();

      try {
        int choice = Integer.parseInt(input);
        if (choice >= 0 && choice <= maxOption) {
          System.out.println();
          return choice;
        }
      } catch (NumberFormatException e) {
        // fall through to the same error message below
      }

      displayInvalidChoiceMessage(maxOption, exitLabel);
    }
  }

  public static void displayExitMessage() {
    System.out.println("\nExiting system");
  }

  public static void clearScreen() {
    try {
      if (System.getProperty("os.name").toLowerCase().contains("win")) {
        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
      } else {
        new ProcessBuilder("clear").inheritIO().start().waitFor();
      }
    } catch (Exception e) {
      // fall back to printing blank lines if the OS command isn't available
      for (int i = 0; i < 50; i++) {
        System.out.println();
      }
    }
  }

}
