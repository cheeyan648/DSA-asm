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
   */
  private static final java.util.Map<Character, String[]> BANNER_FONT = buildBannerFont();

  private static java.util.Map<Character, String[]> buildBannerFont() {
    java.util.Map<Character, String[]> font = new java.util.HashMap<>();
    font.put('A', new String[] {" ### ", "#   #", "#####", "#   #", "#   #"});
    font.put('B', new String[] {"#### ", "#   #", "#### ", "#   #", "#### "});
    font.put('C', new String[] {" ####", "#    ", "#    ", "#    ", " ####"});
    font.put('D', new String[] {"#### ", "#   #", "#   #", "#   #", "#### "});
    font.put('E', new String[] {"#####", "#    ", "#### ", "#    ", "#####"});
    font.put('F', new String[] {"#####", "#    ", "#### ", "#    ", "#    "});
    font.put('G', new String[] {" ####", "#    ", "#  ##", "#   #", " ####"});
    font.put('H', new String[] {"#   #", "#   #", "#####", "#   #", "#   #"});
    font.put('I', new String[] {"#####", "  #  ", "  #  ", "  #  ", "#####"});
    font.put('J', new String[] {"#####", "   # ", "   # ", "#  # ", " ##  "});
    font.put('K', new String[] {"#   #", "#  # ", "###  ", "#  # ", "#   #"});
    font.put('L', new String[] {"#    ", "#    ", "#    ", "#    ", "#####"});
    font.put('M', new String[] {"#   #", "## ##", "# # #", "#   #", "#   #"});
    font.put('N', new String[] {"#   #", "##  #", "# # #", "#  ##", "#   #"});
    font.put('O', new String[] {" ### ", "#   #", "#   #", "#   #", " ### "});
    font.put('P', new String[] {"#### ", "#   #", "#### ", "#    ", "#    "});
    font.put('Q', new String[] {" ### ", "#   #", "#   #", "#  # ", " ## #"});
    font.put('R', new String[] {"#### ", "#   #", "#### ", "#  # ", "#   #"});
    font.put('S', new String[] {" ####", "#    ", " ### ", "    #", "#### "});
    font.put('T', new String[] {"#####", "  #  ", "  #  ", "  #  ", "  #  "});
    font.put('U', new String[] {"#   #", "#   #", "#   #", "#   #", " ### "});
    font.put('V', new String[] {"#   #", "#   #", "#   #", " # # ", "  #  "});
    font.put('W', new String[] {"#   #", "#   #", "# # #", "## ##", "#   #"});
    font.put('X', new String[] {"#   #", " # # ", "  #  ", " # # ", "#   #"});
    font.put('Y', new String[] {"#   #", " # # ", "  #  ", "  #  ", "  #  "});
    font.put('Z', new String[] {"#####", "   # ", "  #  ", " #   ", "#####"});
    font.put(' ', new String[] {"     ", "     ", "     ", "     ", "     "});
    font.put('-', new String[] {"     ", "     ", "#####", "     ", "     "});
    return font;
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
      String[] glyph = BANNER_FONT.get(upper.charAt(i));
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
        String[] glyph = BANNER_FONT.get(upper.charAt(i));
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
   * @return a valid choice between 0 and maxOption
   */
  public static int readMenuChoice(Scanner scanner, int maxOption, String exitLabel) {
    while (true) {
      System.out.print("Enter choice: ");
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
