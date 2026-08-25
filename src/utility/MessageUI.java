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


  /**
   * Width of every framed box drawn by this class. Wide enough for the longest
   * heading the system prints, and narrow enough to fit an 80-column console
   * without wrapping.
   */
  public static final int SCREEN_WIDTH = 76;

  /**
   * The block-letter alphabet used by displayBanner(). Each letter is 5 rows
   * tall, which is the smallest size that still reads clearly as large type on
   * a console. Only the characters the system's banners actually need are
   * defined - the letters A-Z, the space, and the hyphen.
   *
   * Held as two parallel arrays rather than a map: BANNER_KEYS[i] is the
   * character, and BANNER_GLYPHS[i] holds its five rows. A plain array is used
   * because the assignment does not permit any class from the Java Collections
   * Framework, and because a fixed 28-entry alphabet that never changes at
   * runtime does not need a dynamic structure to hold it.
   */
  private static final char[] BANNER_KEYS = {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      ' ', '-'
  };

  private static final String[][] BANNER_GLYPHS = {
      {" ### ", "#   #", "#####", "#   #", "#   #"},   // A
      {"#### ", "#   #", "#### ", "#   #", "#### "},   // B
      {" ####", "#    ", "#    ", "#    ", " ####"},   // C
      {"#### ", "#   #", "#   #", "#   #", "#### "},   // D
      {"#####", "#    ", "#### ", "#    ", "#####"},   // E
      {"#####", "#    ", "#### ", "#    ", "#    "},   // F
      {" ####", "#    ", "#  ##", "#   #", " ####"},   // G
      {"#   #", "#   #", "#####", "#   #", "#   #"},   // H
      {"#####", "  #  ", "  #  ", "  #  ", "#####"},   // I
      {"#####", "   # ", "   # ", "#  # ", " ##  "},   // J
      {"#   #", "#  # ", "###  ", "#  # ", "#   #"},   // K
      {"#    ", "#    ", "#    ", "#    ", "#####"},   // L
      {"#   #", "## ##", "# # #", "#   #", "#   #"},   // M
      {"#   #", "##  #", "# # #", "#  ##", "#   #"},   // N
      {" ### ", "#   #", "#   #", "#   #", " ### "},   // O
      {"#### ", "#   #", "#### ", "#    ", "#    "},   // P
      {" ### ", "#   #", "#   #", "#  # ", " ## #"},   // Q
      {"#### ", "#   #", "#### ", "#  # ", "#   #"},   // R
      {" ####", "#    ", " ### ", "    #", "#### "},   // S
      {"#####", "  #  ", "  #  ", "  #  ", "  #  "},   // T
      {"#   #", "#   #", "#   #", "#   #", " ### "},   // U
      {"#   #", "#   #", "#   #", " # # ", "  #  "},   // V
      {"#   #", "#   #", "# # #", "## ##", "#   #"},   // W
      {"#   #", " # # ", "  #  ", " # # ", "#   #"},   // X
      {"#   #", " # # ", "  #  ", "  #  ", "  #  "},   // Y
      {"#####", "   # ", "  #  ", " #   ", "#####"},   // Z
      {"     ", "     ", "     ", "     ", "     "},   // space
      {"     ", "     ", "#####", "     ", "     "}    // hyphen
  };

  /**
   * Task: Finds the block-letter glyph for a character.
   *
   * Replaces what a map lookup would do. The alphabet has 28 entries, so a
   * linear scan costs at most 28 comparisons per character drawn - far too
   * small a cost to justify a lookup structure, and it keeps this class free of
   * any collection class.
   *
   * @param letter the character to look up
   * @return the five rows of that character, or null if it is not defined
   */
  private static String[] findGlyph(char letter) {
    for (int i = 0; i < BANNER_KEYS.length; i++) {
      if (BANNER_KEYS[i] == letter) {
        return BANNER_GLYPHS[i];
      }
    }
    return null;
  }

  /**
   * Number of rows tall each block letter stands.
   */
  private static final int BANNER_HEIGHT = 5;

  /**
   * Prints a word in large block letters, centred inside the frame.
   *
   * Console text is always one size - it is the terminal, not the program, that
   * decides how big a character is drawn. Building the title out of many small
   * characters is what makes it read as large type.
   *
   * Any character the block alphabet does not define is skipped, and a word too
   * wide for the frame falls back to ordinary centred text so the border can
   * never be pushed out of alignment.
   *
   * @param text the word to draw, letters and spaces only
   */
  public static void displayBanner(String text) {
    String upper = text.toUpperCase();

    // Work out the drawn width first - if it cannot fit, do not draw it.
    int width = 0;
    for (int i = 0; i < upper.length(); i++) {
      String[] glyph = findGlyph(upper.charAt(i));
      if (glyph != null) {
        width += glyph[0].length() + 1;
      }
    }
    if (width == 0 || width > SCREEN_WIDTH - 4) {
      displayBoxCentred(text);
      return;
    }

    // Every row is padded to the same width before centring. Trimming them
    // individually would centre each row on its own length, which makes the
    // upright strokes of letters like T and I lean from row to row.
    int usable = SCREEN_WIDTH - 4;
    int leftPadding = Math.max(0, (usable - width) / 2);

    for (int row = 0; row < BANNER_HEIGHT; row++) {
      StringBuilder line = new StringBuilder(" ".repeat(leftPadding));
      for (int i = 0; i < upper.length(); i++) {
        String[] glyph = findGlyph(upper.charAt(i));
        if (glyph != null) {
          line.append(glyph[row]).append(' ');
        }
      }
      System.out.printf("| %-" + usable + "s |%n", line.toString());
    }
  }

  /**
   * Prints the top edge of a framed box.
   */
  public static void displayBoxTop() {
    System.out.println("+" + "=".repeat(SCREEN_WIDTH - 2) + "+");
  }

  /**
   * Prints the bottom edge of a framed box.
   */
  public static void displayBoxBottom() {
    System.out.println("+" + "=".repeat(SCREEN_WIDTH - 2) + "+");
  }

  /**
   * Prints a divider between sections inside a framed box.
   */
  public static void displayBoxDivider() {
    System.out.println("|" + "-".repeat(SCREEN_WIDTH - 2) + "|");
  }

  /**
   * Prints one left-aligned line inside a framed box, padded so the closing
   * border always lines up.
   *
   * @param text the line's content, indented by two spaces inside the frame
   */
  public static void displayBoxLine(String text) {
    System.out.printf("| %-" + (SCREEN_WIDTH - 4) + "s |%n", text);
  }

  /**
   * Prints one centred line inside a framed box, used for titles.
   *
   * @param text the line's content
   */
  public static void displayBoxCentred(String text) {
    int usable = SCREEN_WIDTH - 4;
    int padding = Math.max(0, (usable - text.length()) / 2);
    System.out.printf("| %-" + usable + "s |%n", " ".repeat(padding) + text);
  }

  /**
   * Prints an empty line inside a framed box, for spacing.
   */
  public static void displayBoxBlank() {
    displayBoxLine("");
  }

  /**
   * Prints one blank line outside any frame, used to separate a framed screen
   * from whatever the console printed before it.
   *
   * Exists so callers never need a System.out statement of their own - under
   * the ECB pattern only boundary and utility classes write to the console.
   */
  public static void displayBlankLine() {
    System.out.println();
  }

  /**
   * Prints one numbered menu option inside a framed box, so every menu across
   * the system lines its numbers and labels up the same way.
   *
   * @param option the option number the user types, 0 being back/exit
   * @param label what the option does
   */
  public static void displayMenuOption(int option, String label) {
    displayBoxLine(String.format("  [%d]  %s", option, label));
  }

  /**
   * Prints a menu option with a short description underneath it, so a menu of
   * whole subsystems explains what each one is for rather than relying on its
   * name alone.
   *
   * @param option the option number the user types
   * @param label the subsystem's name
   * @param description one short line saying what the subsystem does
   */
  public static void displaySubsystemOption(int option, String label, String description) {
    displayBoxLine(String.format("  [%d]  %s", option, label));
    displayBoxLine(String.format("       %s", description));
    displayBoxBlank();
  }

  public static void displayInvalidChoiceMessage() {
    System.out.println("\nInvalid choice");
  }

  /**
   * Reads one trimmed line of input, returning "0" once the input has ended.
   *
   * A plain scanner.nextLine() throws NoSuchElementException at end of input -
   * which happens when the user closes the console, or when input is piped in
   * and runs out. Every prompt in the system treats "0" as cancel, so returning
   * it here unwinds the current action instead of crashing the program.
   *
   * @param scanner the Scanner to read from
   * @return the trimmed line, or "0" if there is no more input
   */
  public static String readLine(Scanner scanner) {
    return readLine(scanner, "0");
  }

  /**
   * Reads one trimmed line of input, returning a caller-chosen value once the
   * input has ended. Used by the few prompts whose cancel key is not "0".
   *
   * @param scanner the Scanner to read from
   * @param endOfInputValue what to return when there is no more input
   * @return the trimmed line, or endOfInputValue if there is no more input
   */
  public static String readLine(Scanner scanner, String endOfInputValue) {
    if (!scanner.hasNextLine()) {
      System.out.println();
      return endOfInputValue;
    }
    return scanner.nextLine().trim();
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
   * Shows an error for when the user just presses Enter without typing
   * anything, then reminds them of the valid range.
   *
   * @param maxOption the highest valid menu option (options run 1..maxOption)
   * @param exitLabel what option 0 does, e.g. "exit" or "go back"
   */
  public static void displayEmptyChoiceMessage(int maxOption, String exitLabel) {
    System.out.println("\nChoice cannot be empty!");
    System.out.println("Please enter a number from 1 to " + maxOption
        + ", or 0 to " + exitLabel + ".");
  }

  /**
   * Reads a menu choice, re-prompting until the user enters a whole number
   * in the range 0..maxOption. Empty input gets a "cannot be empty" error;
   * anything else invalid (letters, out-of-range numbers) gets the
   * invalid-choice error instead of crashing. The screen is never cleared
   * while re-prompting, so the user can see what went wrong.
   *
   * @param scanner the Scanner to read from
   * @param maxOption the highest valid menu option (options run 1..maxOption)
   * @param exitLabel what option 0 does, e.g. "exit" or "go back"
   * @return a valid choice between 0 and maxOption, or 0 if input has ended
   */
  public static int readMenuChoice(Scanner scanner, int maxOption, String exitLabel) {
    while (true) {
      System.out.print("Enter choice: ");

      // No more input - the user closed the console or input was piped in and
      // ran out. Reading on would throw NoSuchElementException, so 0 is
      // returned instead to back out of the menu the same way a typed 0 does.
      if (!scanner.hasNextLine()) {
        System.out.println();
        return 0;
      }

      String input = scanner.nextLine().trim();

      if (input.isEmpty()) {
        displayEmptyChoiceMessage(maxOption, exitLabel);
        continue;
      }

      try {
        int choice = Integer.parseInt(input);
        if (choice >= 0 && choice <= maxOption) {
          System.out.println();
          return choice;
        }
      } catch (NumberFormatException e) {
        // fall through to the invalid-choice message below
      }

      displayInvalidChoiceMessage(maxOption, exitLabel);
    }
  }

  public static void displayExitMessage() {
    System.out.println("\nExiting system");
  }

  /**
   * Clears the console so each screen starts fresh instead of scrolling under
   * the previous one.
   *
   * System.console() returns null when the program is not attached to a real
   * terminal - which is the case inside the NetBeans Output window. There the
   * cls/clear command would run without error but leave the window untouched,
   * so blank lines are printed instead to scroll the old screen out of sight.
   */
  public static void clearScreen() {
    if (System.console() == null) {
      scrollScreen();
      return;
    }

    try {
      if (System.getProperty("os.name").toLowerCase().contains("win")) {
        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
      } else {
        new ProcessBuilder("clear").inheritIO().start().waitFor();
      }
    } catch (Exception e) {
      // fall back to printing blank lines if the OS command isn't available
      scrollScreen();
    }
  }

  /**
   * Pushes the previous screen out of view by printing blank lines. Used where
   * a true clear is not possible, such as the NetBeans Output window.
   */
  private static void scrollScreen() {
    for (int i = 0; i < 50; i++) {
      System.out.println();
    }
  }

}
