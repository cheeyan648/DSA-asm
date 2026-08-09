package boundary;

import adt.ListInterface;
import entity.WalkInGuest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import utility.MessageUI;

/**
 * Console interface for the Walk-In Registration & Standard Booking module.
 *
 * This class only talks to the user - it collects input and prints output. All
 * decisions about the queue itself belong to
 * control.WalkInRegistrationBookingMaintenance.
 *
 * @author Tan Chee Yan
 */
public class WalkInRegistrationBookingUI {

  // Shared with every other UI class - see MessageUI.scanner for why.
  private Scanner scanner = MessageUI.scanner;

  private static final DateTimeFormatter REPORT_TIMESTAMP =
      DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, hh:mm a");

  // Width of the report rules, wide enough for the widest report row.
  private static final int REPORT_WIDTH = 78;

  // Width of the guest table rules. Matches the columns in columnHeadings()
  // plus the leading "No." and trailing "Ahead" columns added per row.
  private static final int TABLE_WIDTH = 100;

  // How many guests one page of a listing shows. Long listings are split into
  // pages of this size so a busy queue cannot scroll off the top of the screen
  // in one burst.
  private static final int ROWS_PER_PAGE = 20;

  // Returned by readPageCommand() when the user has finished with a listing.
  // Not a valid page number, so it can never be mistaken for one.
  private static final int PAGE_QUIT = -1;

  // ==================================================================
  // MENUS
  // ==================================================================

  public int getMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nWALK-IN REGISTRATION & STANDARD BOOKING");
    System.out.println("1. Guest registration");
    System.out.println("2. Queue operations");
    System.out.println("3. Search & filter");
    System.out.println("4. Sorted listings");
    System.out.println("5. Reports");
    System.out.println("0. Back to main menu");

    return MessageUI.readMenuChoice(scanner, 5, "go back to the main menu");
  }

  public int getRegistrationMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nGUEST REGISTRATION");
    System.out.println("1. Register normal walk-in (join back of queue)");
    System.out.println("2. Register urgent walk-in (exception case)");
    System.out.println("3. Undo last registration");
    System.out.println("0. Back");

    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  public int getQueueMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nQUEUE OPERATIONS");
    System.out.println("1. Serve next guest");
    System.out.println("2. Display current queue");
    System.out.println("3. Cancel a waiting guest");
    System.out.println("0. Back");

    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  public int getSearchMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nSEARCH & FILTER");
    System.out.println("1. Search by guest ID");
    System.out.println("2. Search by name (partial match)");
    System.out.println("3. Filter by status");
    System.out.println("4. Filter by guest type");
    System.out.println("0. Back");

    return MessageUI.readMenuChoice(scanner, 4, "go back");
  }

  public int getSortMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nSORTED LISTINGS");
    System.out.println("1. By arrival time (earliest first)");
    System.out.println("2. By guest name (A-Z)");
    System.out.println("3. By waiting time (longest first)");
    System.out.println("4. By service order (urgent first, then arrival)");
    System.out.println("0. Back");

    return MessageUI.readMenuChoice(scanner, 4, "go back");
  }

  public int getReportMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nREPORTS");
    System.out.println("1. Queue Performance Analysis Report");
    System.out.println("2. Urgency Exception Audit Report");
    System.out.println("0. Back");

    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  // ==================================================================
  // SCREEN
  // ==================================================================

  /**
   * Clears the screen and prints the title of the action about to run, so each
   * action starts on a clean screen with the menu it was chosen from no longer
   * cluttering the display.
   *
   * @param title the name of the action being started
   */
  public void startAction(String title) {
    MessageUI.clearScreen();
    System.out.println();
    System.out.println("=".repeat(title.length() + 4));
    System.out.println("  " + title);
    System.out.println("=".repeat(title.length() + 4));
  }

  // ==================================================================
  // INPUT
  // ==================================================================

  /**
   * Prompts for the details of a new walk-in guest.
   *
   * The guest ID is NOT asked for - it is assigned automatically by the control
   * class, so the front-desk officer cannot mistype it or reuse one. Entering 0
   * at the name prompt abandons the registration and returns null, as does
   * running out of input, so the caller must always null-check the result.
   *
   * @param urgent true to register the guest as an urgent exception case, false
   * for a normal walk-in joining the back of the queue
   * @param assignedId the ID the control class has reserved for this guest,
   * shown to the user so they know what it will be
   * @return the new guest, or null if the user cancelled
   */
  public WalkInGuest inputWalkInGuest(boolean urgent, String assignedId) {
    System.out.println("\nAssigned guest ID: " + assignedId + " (auto-generated)");
    System.out.println("(enter 0 as the name to cancel)");

    String name = inputName();
    if (name == null) {
      return null;
    }

    String contactNumber = inputContactNumber();
    if (contactNumber == null) {
      return null;
    }

    String urgencyReason = null;
    if (urgent) {
      urgencyReason = inputUrgencyReason();
      if (urgencyReason == null) {
        return null;
      }
    }

    return new WalkInGuest(assignedId, name, contactNumber, urgent, urgencyReason,
        LocalDateTime.now());
  }

  /**
   * Reads a guest name, re-prompting until it is a plausible name.
   *
   * A name must contain at least one letter, so a digits-only entry like "123"
   * is rejected. Digits themselves are allowed within a name (e.g. "John Lim
   * 2nd"), as are the punctuation marks that appear in real names - spaces,
   * apostrophes, hyphens, full stops and the "s/o" and "a/p" slashes common in
   * Malaysian names.
   *
   * @return the trimmed name, or null if the user entered 0 to cancel
   */
  private String inputName() {
    while (true) {
      System.out.print("Enter guest name     : ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String name = scanner.nextLine().trim();

      if (name.isEmpty()) {
        displayMessage("Name cannot be empty!");
        continue;
      }
      if (name.equals("0")) {
        return null;
      }

      if (name.length() < 2) {
        displayMessage("Name is too short! Please enter at least 2 characters.");
        continue;
      }
      if (name.length() > 40) {
        displayMessage("Name is too long! Please keep it to 40 characters or fewer.");
        continue;
      }
      if (!name.matches(".*[A-Za-z].*")) {
        displayMessage("Invalid name! A name must contain letters - it cannot be "
            + "only numbers or symbols.");
        displayMessage("Example: Ali Bakar");
        continue;
      }
      if (!name.matches("[A-Za-z0-9 '\\-./@]+")) {
        displayMessage("Invalid name! Letters, numbers, spaces, apostrophes, "
            + "hyphens and full stops only.");
        displayMessage("Example: Nur Aisyah binti Rahman");
        continue;
      }

      return name;
    }
  }

  /**
   * Reads a contact number, re-prompting until it is a valid Malaysian phone
   * number of 9 to 10 digits.
   *
   * Dashes and spaces are allowed for readability and ignored when counting, so
   * "012-3456789", "012 345 6789" and "0123456789" are all accepted and all
   * count as 10 digits. Each way the input can be wrong produces its own
   * message saying what the rule is and how many digits were actually entered,
   * rather than one generic "invalid" for every case.
   *
   * @return the trimmed contact number, or null if the user cancelled
   */
  private String inputContactNumber() {
    while (true) {
      System.out.print("Enter contact number : ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String contactNumber = scanner.nextLine().trim();

      if (contactNumber.isEmpty()) {
        displayMessage("Contact number cannot be empty!");
        displayContactNumberRule();
        continue;
      }
      if (contactNumber.equals("0")) {
        return null;
      }

      // Only digits, spaces and dashes are permitted at all.
      if (!contactNumber.matches("[0-9 \\-]+")) {
        displayMessage("Invalid contact number! It may contain digits only, "
            + "with optional dashes or spaces.");
        displayContactNumberRule();
        continue;
      }

      String digitsOnly = contactNumber.replaceAll("[^0-9]", "");

      if (digitsOnly.length() < 9) {
        displayMessage("Contact number is too short! You entered "
            + digitsOnly.length() + " digit(s).");
        displayContactNumberRule();
        continue;
      }
      if (digitsOnly.length() > 10) {
        displayMessage("Contact number is too long! You entered "
            + digitsOnly.length() + " digit(s).");
        displayContactNumberRule();
        continue;
      }
      if (!digitsOnly.startsWith("0")) {
        displayMessage("Invalid contact number! A Malaysian number must start "
            + "with 0.");
        displayContactNumberRule();
        continue;
      }

      return contactNumber;
    }
  }

  /**
   * States the contact number rule, shown after every contact number error so
   * the user always knows what is expected without having to guess.
   */
  private void displayContactNumberRule() {
    System.out.println("  The contact number must contain 9 to 10 digits and start with 0.");
    System.out.println("  Dashes and spaces are optional.");
    System.out.println("  Examples: 012-3456789, 012 345 6789, 0123456789, 03-12345678");
  }

  /**
   * Reads why an urgent guest is being allowed to skip the chronological queue.
   *
   * A free-text reason is offered alongside the common presets so unusual cases
   * are still recordable. The reason is what makes the exception auditable
   * later in the Urgency Exception Audit Report.
   *
   * @return the chosen reason, or null if the user cancelled
   */
  private String inputUrgencyReason() {
    String[] presets = {
      "Elderly or unwell guest",
      "Wheelchair / mobility assistance",
      "Travelling with infant or young children",
      "Medical or emergency situation",
      "Complaint escalation"
    };

    System.out.println("\nReason for urgency:");
    for (int i = 0; i < presets.length; i++) {
      System.out.println((i + 1) + ". " + presets[i]);
    }
    System.out.println((presets.length + 1) + ". Other (type your own)");
    System.out.println("0. Cancel registration");

    int choice = MessageUI.readMenuChoice(scanner, presets.length + 1, "cancel");

    if (choice == 0) {
      return null;
    }
    if (choice <= presets.length) {
      return presets[choice - 1];
    }

    while (true) {
      System.out.print("Enter reason         : ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String reason = scanner.nextLine().trim();

      if (reason.isEmpty()) {
        displayMessage("Reason cannot be empty!");
        continue;
      }
      if (reason.equals("0")) {
        return null;
      }

      return reason;
    }
  }

  /**
   * Reads a guest ID to search for. Accepts the number alone or with the WG
   * prefix, so both "1006" and "WG1006" work.
   *
   * @return the full guest ID including the WG prefix, or null if cancelled
   */
  public String inputGuestIdToSearch() {
    while (true) {
      System.out.print("\nEnter guest ID to search (e.g. 1006, or 0 to cancel): ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String guestId = scanner.nextLine().trim().toUpperCase();

      if (guestId.isEmpty()) {
        displayMessage("Guest ID cannot be empty!");
        continue;
      }
      if (guestId.equals("0")) {
        return null;
      }

      if (guestId.startsWith("WG")) {
        guestId = guestId.substring(2).trim();
      }

      if (!guestId.matches("\\d{4}")) {
        displayMessage("Invalid guest ID! Enter a 4-digit number, e.g. 1006.");
        continue;
      }

      return "WG" + guestId;
    }
  }

  /**
   * Reads a name fragment to search for.
   *
   * @return the trimmed search text, or null if cancelled
   */
  public String inputNameToSearch() {
    while (true) {
      System.out.print("\nEnter name or part of a name (or 0 to cancel): ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String text = scanner.nextLine().trim();

      if (text.isEmpty()) {
        displayMessage("Search text cannot be empty!");
        continue;
      }
      if (text.equals("0")) {
        return null;
      }

      return text;
    }
  }

  /**
   * Asks which status to filter the queue by.
   *
   * @return one of the WalkInGuest.STATUS_* values, or null if cancelled
   */
  public String inputStatusFilter() {
    System.out.println("\nShow guests with which status?");
    System.out.println("1. Waiting");
    System.out.println("2. Served");
    System.out.println("3. Cancelled");
    System.out.println("0. Back");

    int choice = MessageUI.readMenuChoice(scanner, 3, "go back");

    switch (choice) {
      case 1:
        return WalkInGuest.STATUS_WAITING;
      case 2:
        return WalkInGuest.STATUS_SERVED;
      case 3:
        return WalkInGuest.STATUS_CANCELLED;
      default:
        return null;
    }
  }

  /**
   * Asks which guest type to filter the queue by.
   *
   * @return 1 for urgent, 2 for normal, 0 if cancelled
   */
  public int inputTypeFilter() {
    System.out.println("\nShow guests of which type?");
    System.out.println("1. Urgent (exception cases)");
    System.out.println("2. Normal");
    System.out.println("0. Back");

    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  /**
   * Prints a compact one-line-per-guest list for choosing from.
   *
   * Deliberately not paged: this list exists only so the user can pick a
   * position from it, and a pager between the list and the prompt would swallow
   * the very keystroke they are about to type. Keeping each entry to one short
   * line means even a long queue stays manageable on screen.
   *
   * @param list the guests to choose from
   * @param heading the title to show above them
   */
  public void displayGuestPickList(ListInterface<WalkInGuest> list, String heading) {
    if (list == null || list.isEmpty()) {
      return;
    }

    System.out.println("\n" + heading);
    System.out.println("-".repeat(72));

    for (int position = 1; position <= list.getNumberOfEntries(); position++) {
      WalkInGuest guest = list.getEntry(position);
      System.out.printf("%3d. %-9s %-26s %-8s waited %s%n",
          position, guest.getGuestId(), guest.getName(), guest.getGuestType(),
          guest.getFormattedWaitingTime());
    }

    System.out.println("-".repeat(72));
    System.out.println("Total waiting: " + list.getNumberOfEntries());
  }

  /**
   * Asks which waiting guest to cancel, by queue position.
   *
   * @param maximumPosition how many guests are currently waiting
   * @return the 1-based position chosen, or 0 to cancel
   */
  public int inputPositionToCancel(int maximumPosition) {
    while (true) {
      System.out.print("\nEnter the position to cancel (1-" + maximumPosition
          + ", or 0 to go back): ");
      if (!scanner.hasNextLine()) {
        return 0;
      }
      String input = scanner.nextLine().trim();

      if (input.isEmpty()) {
        displayMessage("Position cannot be empty!");
        continue;
      }

      try {
        int position = Integer.parseInt(input);
        if (position >= 0 && position <= maximumPosition) {
          return position;
        }
      } catch (NumberFormatException e) {
        // fall through to the error below
      }

      displayMessage("Invalid position! Enter a number from 1 to " + maximumPosition
          + ", or 0 to go back.");
    }
  }

  /**
   * Asks the user to confirm an action they cannot undo.
   *
   * @param prompt the question to ask
   * @return true only if the user answered yes
   */
  public boolean confirm(String prompt) {
    while (true) {
      System.out.print("\n" + prompt + " (Y/N): ");
      if (!scanner.hasNextLine()) {
        return false;
      }
      String answer = scanner.nextLine().trim().toUpperCase();

      if (answer.equals("Y") || answer.equals("YES")) {
        return true;
      }
      if (answer.equals("N") || answer.equals("NO")) {
        return false;
      }

      displayMessage("Please answer Y or N.");
    }
  }

  // ==================================================================
  // OUTPUT
  // ==================================================================

  /**
   * Prints one guest's details, or a "no guest" notice when there is none.
   *
   * @param guest the guest to display, may be null
   */
  public void displayGuest(WalkInGuest guest) {
    if (guest == null) {
      System.out.println("\nNo guest to display.");
      return;
    }

    System.out.println();
    System.out.println(singleGuestHeadings());
    System.out.println("-".repeat(REPORT_WIDTH + 10));
    System.out.println(guest);
    System.out.println();
    System.out.println("Time waited   : " + guest.getFormattedWaitingTime()
        + (WalkInGuest.STATUS_WAITING.equals(guest.getStatus()) ? " (and counting)" : ""));

    if (guest.isUrgent() && guest.getUrgencyReason() != null) {
      System.out.println("Urgency reason: " + guest.getUrgencyReason());
    }
  }

  /**
   * Prints a list of guests under a heading, numbering each row.
   *
   * Walks the list with its iterator rather than indexing, so the display works
   * for any ListInterface implementation.
   *
   * The "Ahead" column counts how many guests must still be served before that
   * row's guest - so the guest at the front shows 0. It is only meaningful for
   * a guest who is still waiting; a served or cancelled guest shows "-" because
   * they are no longer in the line at all.
   *
   * @param list the guests to print
   * @param heading the title to show above them
   * @param emptyMessage what to say when the list has no entries
   */
  public void displayGuestList(ListInterface<WalkInGuest> list, String heading,
      String emptyMessage) {
    if (list == null || list.isEmpty()) {
      System.out.println("\n" + emptyMessage);
      return;
    }

    int totalRows = list.getNumberOfEntries();
    int totalPages = (totalRows + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
    int currentPage = 1;

    while (true) {
      int firstRow = (currentPage - 1) * ROWS_PER_PAGE + 1;
      int lastRow = Math.min(currentPage * ROWS_PER_PAGE, totalRows);

      // Everything before this page still counts towards the "ahead" figures,
      // so the running total is primed by walking the earlier rows. Without
      // this, page 2 would restart the count at 0 and misreport the queue.
      int guestsAhead = countWaitingBefore(list, firstRow);

      if (totalPages > 1) {
        // Re-clear on each page so the pages replace each other instead of
        // scrolling the earlier ones off the top.
        MessageUI.clearScreen();
      }

      System.out.println("\n" + heading);
      System.out.println(columnHeadings());
      System.out.println("-".repeat(TABLE_WIDTH));

      for (int rowNumber = firstRow; rowNumber <= lastRow; rowNumber++) {
        WalkInGuest guest = list.getEntry(rowNumber);
        boolean waiting = WalkInGuest.STATUS_WAITING.equals(guest.getStatus());

        System.out.printf("%-4d %s %-5s%n", rowNumber, guest,
            waiting ? String.valueOf(guestsAhead) : "-");

        // Only a guest who is still waiting occupies a place in the line, so
        // only they push the following waiting guests further back.
        if (waiting) {
          guestsAhead++;
        }
      }

      System.out.println("-".repeat(TABLE_WIDTH));
      System.out.printf("Showing %d-%d of %d   |   Page %d of %d   |   Still waiting: %d%n",
          firstRow, lastRow, totalRows, currentPage, totalPages,
          list.countIf(guest -> WalkInGuest.STATUS_WAITING.equals(guest.getStatus())));
      System.out.println("(\"Ahead\" = guests to be served before this one; \"-\" = "
          + "no longer in the queue)");

      if (totalPages == 1) {
        return;
      }

      int command = readPageCommand(currentPage, totalPages);
      if (command == PAGE_QUIT) {
        return;
      }
      currentPage = command;
    }
  }

  /**
   * Counts how many guests before a given row are still waiting, so a page that
   * does not start at row 1 still reports correct "ahead" figures.
   *
   * @param list the full list being displayed
   * @param firstRow the 1-based row the page starts at
   * @return the number of waiting guests in rows 1 to firstRow - 1
   */
  private int countWaitingBefore(ListInterface<WalkInGuest> list, int firstRow) {
    int waitingSoFar = 0;
    for (int row = 1; row < firstRow; row++) {
      if (WalkInGuest.STATUS_WAITING.equals(list.getEntry(row).getStatus())) {
        waitingSoFar++;
      }
    }
    return waitingSoFar;
  }

  /**
   * Asks which page to show next.
   *
   * @param currentPage the page on screen now
   * @param totalPages how many pages there are altogether
   * @return the page number to move to, or PAGE_QUIT to stop paging
   */
  private int readPageCommand(int currentPage, int totalPages) {
    while (true) {
      System.out.println();
      System.out.print("[N]ext page, [P]revious page, [1-" + totalPages
          + "] jump to page, [Q] quit listing: ");

      if (!scanner.hasNextLine()) {
        return PAGE_QUIT;
      }
      String input = scanner.nextLine().trim().toUpperCase();

      if (input.isEmpty()) {
        displayMessage("Please enter N, P, a page number, or Q.");
        continue;
      }

      if (input.equals("Q")) {
        return PAGE_QUIT;
      }

      if (input.equals("N")) {
        if (currentPage >= totalPages) {
          displayMessage("You are already on the last page.");
          continue;
        }
        return currentPage + 1;
      }

      if (input.equals("P")) {
        if (currentPage <= 1) {
          displayMessage("You are already on the first page.");
          continue;
        }
        return currentPage - 1;
      }

      try {
        int page = Integer.parseInt(input);
        if (page >= 1 && page <= totalPages) {
          return page;
        }
        displayMessage("There is no page " + page + ". Enter a page from 1 to "
            + totalPages + ".");
        continue;
      } catch (NumberFormatException e) {
        // fall through to the message below
      }

      displayMessage("Invalid input! Enter N, P, a page number, or Q.");
    }
  }

  /**
   * Announces the guest who has just been taken off the front of the queue.
   *
   * @param guest the guest now being served, or null if the queue was empty
   */
  public void displayGuestServedMessage(WalkInGuest guest) {
    if (guest == null) {
      displayMessage("No guests are waiting - there is no one to serve.");
      return;
    }

    System.out.println("\nNOW SERVING");
    displayGuest(guest);
  }

  /**
   * Prints a one-line confirmation or error.
   *
   * @param message the text to show
   */
  public void displayMessage(String message) {
    System.out.println("\n" + message);
  }

  /**
   * Prints the banner every report opens with.
   *
   * @param reportTitle the name of the report
   */
  public void displayReportHeader(String reportTitle) {
    // A report is a full-screen document, so it starts from a cleared screen
    // rather than under the menu it was chosen from.
    MessageUI.clearScreen();
    System.out.println();
    System.out.println("=".repeat(REPORT_WIDTH));
    System.out.println(centre("TUNKU ABDUL RAHMAN UNIVERSITY OF MANAGEMENT AND TECHNOLOGY"));
    System.out.println(centre("TARUMT RESORT - WALK-IN REGISTRATION SUBSYSTEM"));
    System.out.println(centre(reportTitle));
    System.out.println("=".repeat(REPORT_WIDTH));
    System.out.println("Generated at: " + LocalDateTime.now().format(REPORT_TIMESTAMP));
    System.out.println("-".repeat(REPORT_WIDTH));
  }

  /**
   * Prints the footer every report closes with.
   */
  public void displayReportFooter() {
    System.out.println("-".repeat(REPORT_WIDTH));
    System.out.println(centre("END OF THE REPORT"));
    System.out.println("=".repeat(REPORT_WIDTH));
  }

  /**
   * Prints a section title within a report.
   *
   * @param title the section name
   */
  public void displayReportSection(String title) {
    System.out.println();
    System.out.println(title);
    System.out.println("-".repeat(REPORT_WIDTH));
  }

  /**
   * Prints one "label : value" statistic line within a report.
   *
   * @param label what the figure measures
   * @param value the figure itself
   */
  public void displayReportLine(String label, String value) {
    System.out.printf("  %-46s : %s%n", label, value);
  }

  /**
   * Prints a horizontal bar chart, one row per category.
   *
   * The longest bar is scaled to fit the report width, so the chart stays
   * readable whether the biggest count is 3 or 300.
   *
   * @param labels the category names
   * @param counts how many fall in each category, same length as labels
   * @param maximumBarWidth the widest a bar may be drawn
   */
  public void displayBarChart(String[] labels, int[] counts, int maximumBarWidth) {
    if (labels == null || counts == null || labels.length == 0) {
      System.out.println("  (no data to chart)");
      return;
    }

    int highest = 0;
    for (int count : counts) {
      if (count > highest) {
        highest = count;
      }
    }

    if (highest == 0) {
      System.out.println("  (no activity recorded)");
      return;
    }

    // Size the label column to the longest label so captions are never cut off
    // and the bars still line up with each other.
    int labelWidth = 0;
    for (String label : labels) {
      if (label != null && label.length() > labelWidth) {
        labelWidth = label.length();
      }
    }

    for (int i = 0; i < labels.length; i++) {
      // Scale each bar against the highest count so the chart always fills the
      // available width without ever overflowing it.
      int barWidth = (int) Math.round((double) counts[i] / highest * maximumBarWidth);
      if (counts[i] > 0 && barWidth == 0) {
        barWidth = 1;
      }
      System.out.printf("  %-" + labelWidth + "s | %-" + maximumBarWidth + "s %d%n",
          labels[i], "*".repeat(barWidth), counts[i]);
    }
  }

  /**
   * Waits for the user to press Enter so output stays on screen instead of
   * being wiped by the clearScreen() at the top of the next menu.
   */
  public void pause() {
    System.out.print("\nPress Enter to continue...");
    // hasNextLine() guards against input being exhausted (e.g. piped input or
    // Ctrl+D), which would otherwise throw NoSuchElementException here.
    if (scanner.hasNextLine()) {
      scanner.nextLine();
    }
  }

  /**
   * Column headings for a full guest table, matching the layout of
   * WalkInGuest.toString() plus the leading "No." and trailing "Ahead" columns
   * that displayGuestList() adds.
   */
  private String columnHeadings() {
    return String.format("%-4s %-9s %-26s %-14s %-8s %-17s %-9s %-5s",
        "No.", "Guest ID", "Name", "Contact", "Type", "Arrived", "Status", "Ahead");
  }

  /**
   * Column headings for a single guest shown on its own, where there is no
   * queue position or "ahead" count to report.
   */
  private String singleGuestHeadings() {
    return String.format("%-9s %-26s %-14s %-8s %-17s %-9s",
        "Guest ID", "Name", "Contact", "Type", "Arrived", "Status");
  }

  /**
   * Pads text with leading spaces so it sits in the middle of a report line.
   */
  private String centre(String text) {
    if (text.length() >= REPORT_WIDTH) {
      return text;
    }
    int padding = (REPORT_WIDTH - text.length()) / 2;
    return " ".repeat(padding) + text;
  }
}
