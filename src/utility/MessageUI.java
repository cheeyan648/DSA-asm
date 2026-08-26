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


  // ==================================================================
  // SHARED SCREEN BUILDING
  //
  // Every module draws its screens through these, so a menu in Loyalty is
  // laid out exactly like a menu in Housekeeping. Before this existed each
  // module framed its own screens and they drifted apart - different widths,
  // different ways of saying "0 to go back", different table headings.
  // ==================================================================

  /**
   * Draws a full menu screen: the framed title, a breadcrumb showing where the
   * user is, the numbered options and a back/exit line.
   *
   * @param title the screen's name, shown large
   * @param subtitle a second title line, or null for none
   * @param breadcrumb the path to this screen, e.g. "Main Menu &gt; Front Desk"
   * @param options what the numbered options do, starting at [1]
   * @param backLabel what option [0] does
   */
  public static void displayMenuScreen(String title, String subtitle, String breadcrumb,
      String[] options, String backLabel) {
    clearScreen();
    displayBlankLine();

    displayBoxTop();
    displayBoxBlank();
    displayBoxCentred(spaced(title));
    if (subtitle != null && !subtitle.isBlank()) {
      displayBoxCentred(subtitle);
    }
    displayBoxBlank();
    displayBoxDivider();

    // The breadcrumb means the user can always see which part of the system
    // they are in without having to remember how they got there.
    displayBoxLine("  " + breadcrumb);
    displayBoxDivider();
    displayBoxBlank();

    for (int i = 0; i < options.length; i++) {
      displayMenuOption(i + 1, options[i]);
    }

    displayBoxBlank();
    displayMenuOption(0, backLabel);
    displayBoxBlank();
    displayBoxBottom();
  }

  /**
   * Spaces out a title so it reads as a heading rather than a word.
   *
   * @param text the title
   * @return the title with a space between each character
   */
  public static String spaced(String text) {
    StringBuilder spacedOut = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      if (i > 0) {
        spacedOut.append(' ');
      }
      spacedOut.append(text.charAt(i));
    }
    return spacedOut.toString();
  }

  /**
   * Clears the screen and announces the action about to run, so each action
   * starts clean instead of under the menu it was chosen from.
   *
   * @param title the name of the action being started
   */
  public static void startAction(String title) {
    clearScreen();
    displayBlankLine();
    displayBoxTop();
    displayBoxCentred(title);
    displayBoxBottom();
    displayBlankLine();
  }

  /**
   * Prints a heading for a section within a screen.
   *
   * @param title the section's name
   */
  public static void displaySectionHeading(String title) {
    displayBlankLine();
    System.out.println(title);
    System.out.println("-".repeat(Math.min(SCREEN_WIDTH, title.length() + 20)));
  }

  /** Prints a full-width rule, used to close a table. */
  public static void displayRule() {
    System.out.println("=".repeat(SCREEN_WIDTH));
  }

  /** Prints a lighter full-width rule, used under a table heading. */
  public static void displayThinRule() {
    System.out.println("-".repeat(SCREEN_WIDTH));
  }

  /**
   * Prints a table heading with a rule under it.
   *
   * @param heading the column headings, already spaced into columns
   */
  public static void displayTableHeading(String heading) {
    System.out.println(heading);
    displayThinRule();
  }

  /**
   * Prints one message line.
   *
   * @param message what to tell the user
   */
  public static void displayMessage(String message) {
    System.out.println(message);
  }

  /**
   * Prints a message that reports something went wrong.
   *
   * Prefixed rather than merely printed so a refusal cannot be mistaken for
   * ordinary output when it appears in a long screen.
   *
   * @param message what went wrong
   */
  public static void displayError(String message) {
    System.out.println("  [!] " + message);
  }

  /**
   * Prints a message that reports something succeeded.
   *
   * @param message what happened
   */
  public static void displaySuccess(String message) {
    System.out.println("  [OK] " + message);
  }

  /**
   * Prints one label-and-value line, with the labels lined up.
   *
   * @param label what the value is
   * @param value the value
   */
  public static void displayField(String label, String value) {
    System.out.printf("  %-28s : %s%n", label, value);
  }

  /**
   * Prints one line of a report - a label and a figure lined up down the page.
   *
   * @param label what is being measured
   * @param value the figure
   */
  public static void displayReportLine(String label, String value) {
    System.out.printf("  %-44s %s%n", label, value);
  }

  /**
   * Waits for the user to press Enter before moving on.
   *
   * Called at the end of every action so its output is still on screen when
   * the user is ready to leave it - without this the next menu clears the
   * screen immediately and the result is never read.
   *
   * @param scanner the Scanner to read from
   */
  public static void pause(Scanner scanner) {
    displayBlankLine();
    System.out.print("Press ENTER to continue...");
    if (scanner.hasNextLine()) {
      scanner.nextLine();
    } else {
      displayBlankLine();
    }
  }

  // ==================================================================
  // VALIDATED INPUT
  //
  // Every prompt takes 0 to cancel, so the user can always back out of a
  // half-finished action rather than being forced to complete it.
  // ==================================================================

  /** What every text prompt returns when the user cancels. */
  public static final String CANCELLED = " CANCELLED";

  /** What the numeric prompts return when the user cancels. */
  public static final int CANCELLED_INT = Integer.MIN_VALUE;

  /** What the money prompts return when the user cancels. */
  public static final double CANCELLED_AMOUNT = Double.NEGATIVE_INFINITY;

  /**
   * Asks for a line of text, re-prompting until something is entered.
   *
   * @param scanner the Scanner to read from
   * @param prompt what to ask for
   * @return the text, or CANCELLED if the user typed 0
   */
  public static String readRequiredText(Scanner scanner, String prompt) {
    while (true) {
      System.out.print("  " + prompt + " (0 to cancel): ");
      String input = readLine(scanner);

      if ("0".equals(input)) {
        return CANCELLED;
      }
      if (input.isEmpty()) {
        displayError("This cannot be left blank.");
        continue;
      }
      return input;
    }
  }

  /**
   * Asks for a line of text that may be left blank.
   *
   * @param scanner the Scanner to read from
   * @param prompt what to ask for
   * @return the text, "" if skipped, or CANCELLED if the user typed 0
   */
  public static String readOptionalText(Scanner scanner, String prompt) {
    System.out.print("  " + prompt + " (ENTER to skip, 0 to cancel): ");
    String input = readLine(scanner, "");
    return "0".equals(input) ? CANCELLED : input;
  }

  /**
   * Asks for a whole number inside a range.
   *
   * @param scanner the Scanner to read from
   * @param prompt what to ask for
   * @param min the lowest acceptable value
   * @param max the highest acceptable value
   * @return the number, or CANCELLED_INT if the user typed 0
   */
  public static int readInt(Scanner scanner, String prompt, int min, int max) {
    while (true) {
      System.out.printf("  %s (%d-%d, 0 to cancel): ", prompt, min, max);
      String input = readLine(scanner);

      if ("0".equals(input)) {
        return CANCELLED_INT;
      }
      if (input.isEmpty()) {
        displayError("This cannot be left blank.");
        continue;
      }

      try {
        int value = Integer.parseInt(input);
        if (value < min || value > max) {
          displayError("Please enter a number from " + min + " to " + max + ".");
          continue;
        }
        return value;
      } catch (NumberFormatException notANumber) {
        displayError("Please enter a whole number.");
      }
    }
  }

  /**
   * Asks for an amount of money that cannot be negative.
   *
   * @param scanner the Scanner to read from
   * @param prompt what to ask for
   * @return the amount, or CANCELLED_AMOUNT if the user typed 0
   */
  public static double readAmount(Scanner scanner, String prompt) {
    while (true) {
      System.out.print("  " + prompt + " (RM, 0 to cancel): ");
      String input = readLine(scanner);

      if ("0".equals(input)) {
        return CANCELLED_AMOUNT;
      }
      if (input.isEmpty()) {
        displayError("This cannot be left blank.");
        continue;
      }

      try {
        double value = Double.parseDouble(input);
        if (value < 0) {
          displayError("An amount cannot be negative.");
          continue;
        }
        return Math.round(value * 100.0) / 100.0;
      } catch (NumberFormatException notANumber) {
        displayError("Please enter an amount, e.g. 150.00");
      }
    }
  }

  /**
   * Asks for a date.
   *
   * @param scanner the Scanner to read from
   * @param prompt what the date is for
   * @return the date, or null if the user typed 0
   */
  public static java.time.LocalDate readDate(Scanner scanner, String prompt) {
    java.time.format.DateTimeFormatter format =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    while (true) {
      System.out.print("  " + prompt + " (dd/MM/yyyy, 0 to cancel): ");
      String input = readLine(scanner);

      if ("0".equals(input)) {
        return null;
      }
      if (input.isEmpty()) {
        displayError("This cannot be left blank.");
        continue;
      }

      try {
        return java.time.LocalDate.parse(input, format);
      } catch (java.time.format.DateTimeParseException badDate) {
        displayError("Please enter a real date as dd/MM/yyyy, e.g. 25/12/2026");
      }
    }
  }

  /**
   * Asks a yes or no question.
   *
   * Anything other than y or n is refused rather than assumed, because these
   * questions guard actions that cannot be undone.
   *
   * @param scanner the Scanner to read from
   * @param question what to ask
   * @return true for yes, false for no
   */
  public static boolean confirm(Scanner scanner, String question) {
    while (true) {
      System.out.print("  " + question + " (y/n): ");
      String input = readLine(scanner, "n").toLowerCase();

      if (input.equals("y") || input.equals("yes")) {
        return true;
      }
      if (input.equals("n") || input.equals("no")) {
        return false;
      }
      displayError("Please answer y or n.");
    }
  }

  /**
   * Asks the user to pick one of a set of values.
   *
   * @param scanner the Scanner to read from
   * @param prompt what is being chosen
   * @param choices the values on offer
   * @return the chosen value, or CANCELLED if the user typed 0
   */
  public static String readChoice(Scanner scanner, String prompt, String[] choices) {
    displayBlankLine();
    for (int i = 0; i < choices.length; i++) {
      System.out.printf("    [%d]  %s%n", i + 1, choices[i]);
    }
    displayBlankLine();

    int picked = readInt(scanner, prompt, 1, choices.length);
    return (picked == CANCELLED_INT) ? CANCELLED : choices[picked - 1];
  }

  /** Whether a prompt was cancelled. */
  public static boolean isCancelled(String value) {
    return CANCELLED.equals(value);
  }

  // ==================================================================
  // PAGING
  // ==================================================================

  /** How many rows of a listing are shown at a time. */
  public static final int PAGE_SIZE = 15;

  /**
   * Shows one page of a listing and asks whether to continue.
   *
   * Long listings are paged so the earliest rows are not scrolled off the top
   * before they can be read.
   *
   * @param scanner the Scanner to read from
   * @param page which page has just been shown, counting from 1
   * @param totalPages how many pages there are altogether
   * @return true if the user wants the next page
   */
  public static boolean askForNextPage(Scanner scanner, int page, int totalPages) {
    if (page >= totalPages) {
      return false;
    }

    displayThinRule();
    System.out.printf("  Page %d of %d. Show the next page? (y/n): ", page, totalPages);
    String input = readLine(scanner, "n").toLowerCase();
    return input.equals("y") || input.equals("yes");
  }

  /**
   * How many pages a listing of a given size will take.
   *
   * @param rows how many rows there are
   * @return the number of pages, at least 1
   */
  public static int pageCount(int rows) {
    if (rows <= 0) {
      return 1;
    }
    return (rows + PAGE_SIZE - 1) / PAGE_SIZE;
  }

  // ==================================================================
  // TEXT CHARTS
  //
  // Written by hand rather than with a library so the bars scale to whatever
  // the data happens to be, and so the numbers stay printed beside the chart -
  // the chart illustrates, but the figures are what is authoritative.
  // ==================================================================

  /** How tall a bar chart is drawn, in rows. */
  public static final int CHART_HEIGHT = 8;

  /**
   * Draws a vertical bar chart in text.
   *
   * @param title what the chart shows
   * @param yAxisLabel the unit being measured
   * @param labels the name under each bar
   * @param values the height of each bar
   */
  public static void displayBarChart(String title, String yAxisLabel,
      String[] labels, double[] values) {
    displaySectionHeading(title);

    if (labels.length == 0) {
      displayMessage("  (no data to chart)");
      return;
    }

    double highest = 0;
    for (double value : values) {
      if (value > highest) {
        highest = value;
      }
    }

    // Every bar would be full height if the tallest were zero, so a chart with
    // nothing in it is said plainly instead of drawn misleadingly.
    if (highest <= 0) {
      displayMessage("  (every value is zero)");
      return;
    }

    System.out.printf("  %s%n", yAxisLabel);

    for (int row = CHART_HEIGHT; row >= 1; row--) {
      StringBuilder line = new StringBuilder("  |");
      for (double value : values) {
        int barHeight = (int) Math.round((value / highest) * CHART_HEIGHT);
        line.append(barHeight >= row ? "  #  " : "     ");
      }
      System.out.println(line);
    }

    // Columns are five wide so a four-character label still has a gap after it.
    StringBuilder axis = new StringBuilder("  +");
    for (int i = 0; i < values.length; i++) {
      axis.append("-----");
    }
    System.out.println(axis);

    StringBuilder labelLine = new StringBuilder("   ");
    for (String label : labels) {
      String shown = (label.length() > 4) ? label.substring(0, 4) : label;
      labelLine.append(String.format("%-5s", shown));
    }
    System.out.println(labelLine);

    // The chart shows the shape; these are the actual figures.
    displayBlankLine();
    for (int i = 0; i < labels.length; i++) {
      System.out.printf("    %-20s %10.2f%n", labels[i], values[i]);
    }
  }
}
