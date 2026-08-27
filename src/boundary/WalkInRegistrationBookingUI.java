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
 * Data flow: this UI - WalkInRegistrationBookingMaintenance - WalkInGuestDAO
 * - walkInGuests.dat. Nothing is stored here; 0 cancels every prompt.
 *
 * ADT use: takes ListInterface&lt;WalkInGuest&gt; only to read and print. It
 * never adds, removes or reorders - the control class owns the list and has
 * already searched, filtered or sorted it before passing it here.
 *
 * @author Tan Chee Yan
 */
public class WalkInRegistrationBookingUI {

  private Scanner scanner = MessageUI.scanner;

  private static final DateTimeFormatter REPORT_TIMESTAMP =
      DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, hh:mm a");

  private static final int REPORT_WIDTH = 78;

  private static final int TABLE_WIDTH = 100;
  private static final int ROWS_PER_PAGE = 20;

  private static final int PAGE_QUIT = -1;

  // ===== MENUS - each draws one screen and returns the number typed =====

  private void displayMenuScreen(String title, String subtitle, String breadcrumb,
      String[] options, String backLabel) {
    MessageUI.clearScreen();
    System.out.println();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred(spaced(title));
    if (subtitle != null) {
      MessageUI.displayBoxCentred(subtitle);
    }
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();

    MessageUI.displayBoxLine("  " + breadcrumb);
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxBlank();

    for (int i = 0; i < options.length; i++) {
      MessageUI.displayMenuOption(i + 1, options[i]);
    }

    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(0, backLabel);
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxBottom();
  }

  private String spaced(String text) {
    if (text.length() > 30) {
      return text;
    }

    StringBuilder spacedText = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      if (i > 0) {
        spacedText.append(' ');
      }
      spacedText.append(text.charAt(i));
    }
    return spacedText.toString();
  }

  public int getMenuChoice() {
    displayMenuScreen("WALK-IN REGISTRATION",
        "& S T A N D A R D   B O O K I N G",
        "Main Menu  >  Walk-In Registration",
        new String[] {
          "Guest registration",
          "Queue operations",
          "Search & filter",
          "Sorted listings",
          "Reports"
        },
        "Back to main menu");

    return MessageUI.readMenuChoice(scanner, 5, "go back to the main menu");
  }

  public int getRegistrationMenuChoice() {
    displayMenuScreen("GUEST REGISTRATION", null,
        "Main Menu  >  Walk-In Registration  >  Guest Registration",
        new String[] {
          "Register normal walk-in (join back of queue)",
          "Register urgent walk-in (exception case)",
          "Undo last registration"
        },
        "Back");

    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  public int getQueueMenuChoice() {
    displayMenuScreen("QUEUE OPERATIONS", null,
        "Main Menu  >  Walk-In Registration  >  Queue Operations",
        new String[] {
          "Serve next guest",
          "Display current queue",
          "Cancel a waiting guest"
        },
        "Back");

    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  public int getSearchMenuChoice() {
    displayMenuScreen("SEARCH & FILTER", null,
        "Main Menu  >  Walk-In Registration  >  Search & Filter",
        new String[] {
          "Search by guest ID",
          "Search by name (partial match)",
          "Filter by status",
          "Filter by guest type"
        },
        "Back");

    return MessageUI.readMenuChoice(scanner, 4, "go back");
  }

  public int getSortMenuChoice() {
    displayMenuScreen("SORTED LISTINGS", null,
        "Main Menu  >  Walk-In Registration  >  Sorted Listings",
        new String[] {
          "By arrival time (earliest first)",
          "By guest name (A-Z)",
          "By waiting time (longest first)",
          "By service order (urgent first, then arrival)"
        },
        "Back");

    return MessageUI.readMenuChoice(scanner, 4, "go back");
  }

  public int getReportMenuChoice() {
    displayMenuScreen("REPORTS", null,
        "Main Menu  >  Walk-In Registration  >  Reports",
        new String[] {
          "Queue Performance Analysis Report",
          "Urgency Exception Audit Report"
        },
        "Back");

    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  public void startAction(String title) {
    MessageUI.clearScreen();
    System.out.println();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxCentred(title);
    MessageUI.displayBoxBottom();
  }

  // ===== CHARACTER TESTS - hand-written instead of regex =====

  private boolean isLetter(char character) {
    return (character >= 'A' && character <= 'Z')
        || (character >= 'a' && character <= 'z');
  }

  private boolean isDigit(char character) {
    return character >= '0' && character <= '9';
  }

  private boolean containsLetter(String text) {
    for (int i = 0; i < text.length(); i++) {
      if (isLetter(text.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private boolean hasOnlyNameCharacters(String name) {
    for (int i = 0; i < name.length(); i++) {
      char character = name.charAt(i);
      boolean permitted = isLetter(character)
          || isDigit(character)
          || character == ' '
          || character == '\''
          || character == '-'
          || character == '.'
          || character == '/'
          || character == '@';

      if (!permitted) {
        return false;
      }
    }
    return true;
  }

  private boolean hasOnlyContactCharacters(String contactNumber) {
    for (int i = 0; i < contactNumber.length(); i++) {
      char character = contactNumber.charAt(i);
      if (!isDigit(character) && character != ' ' && character != '-') {
        return false;
      }
    }
    return true;
  }

  private String digitsOf(String text) {
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char character = text.charAt(i);
      if (isDigit(character)) {
        digits.append(character);
      }
    }
    return digits.toString();
  }

  private boolean isAllDigits(String text, int length) {
    if (text.length() != length) {
      return false;
    }
    for (int i = 0; i < text.length(); i++) {
      if (!isDigit(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  // ===== INPUT - validated here, so the control class can trust what it gets =====

  /**
   * Builds the WalkInGuest entity the control class then adds to its List ADT
   * and pushes onto its undo Stack.
   *
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

  private String inputName() {
    while (true) {
      System.out.print("Enter guest name     (0 to cancel): ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String name = MessageUI.readLine(scanner);

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
      if (!containsLetter(name)) {
        displayMessage("Invalid name! A name must contain letters - it cannot be "
            + "only numbers or symbols.");
        displayMessage("Example: Ali Bakar");
        continue;
      }
      if (!hasOnlyNameCharacters(name)) {
        displayMessage("Invalid name! Letters, numbers, spaces, apostrophes, "
            + "hyphens and full stops only.");
        displayMessage("Example: Nur Aisyah binti Rahman");
        continue;
      }

      return name;
    }
  }

  private String inputContactNumber() {
    while (true) {
      System.out.print("Enter contact number (0 to cancel): ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String contactNumber = MessageUI.readLine(scanner);

      if (contactNumber.isEmpty()) {
        displayMessage("Contact number cannot be empty!");
        displayContactNumberRule();
        continue;
      }
      if (contactNumber.equals("0")) {
        return null;
      }

      if (!hasOnlyContactCharacters(contactNumber)) {
        displayMessage("Invalid contact number! It may contain digits only, "
            + "with optional dashes or spaces.");
        displayContactNumberRule();
        continue;
      }

      String digitsOnly = digitsOf(contactNumber);

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

  private void displayContactNumberRule() {
    System.out.println("  The contact number must contain 9 to 10 digits and start with 0.");
    System.out.println("  Dashes and spaces are optional.");
    System.out.println("  Examples: 012-3456789, 012 345 6789, 0123456789, 03-12345678");
  }

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
      System.out.printf("  [%d]  %s%n", i + 1, presets[i]);
    }
    System.out.printf("  [%d]  Other (type your own)%n", presets.length + 1);
    System.out.println("  [0]  Cancel registration");

    int choice = MessageUI.readMenuChoice(scanner, presets.length + 1, "cancel");

    if (choice == 0) {
      return null;
    }
    if (choice <= presets.length) {
      return presets[choice - 1];
    }

    while (true) {
      System.out.print("Enter reason         (0 to cancel): ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String reason = MessageUI.readLine(scanner);

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

  public String inputGuestIdToSearch() {
    while (true) {
      System.out.print("\nEnter guest ID to search (e.g. 1006, or 0 to cancel): ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String guestId = MessageUI.readLine(scanner).toUpperCase();

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

      if (!isAllDigits(guestId, 4)) {
        displayMessage("Invalid guest ID! Enter a 4-digit number, e.g. 1006.");
        continue;
      }

      return "WG" + guestId;
    }
  }

  public String inputNameToSearch() {
    while (true) {
      System.out.print("\nEnter name or part of a name (or 0 to cancel): ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String text = MessageUI.readLine(scanner);

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

  public String inputStatusFilter() {
    System.out.println("\nShow guests with which status?");
    System.out.println("  [1]  Waiting");
    System.out.println("  [2]  Served");
    System.out.println("  [3]  Cancelled");
    System.out.println("  [0]  Back");

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

  public int inputTypeFilter() {
    System.out.println("\nShow guests of which type?");
    System.out.println("  [1]  Urgent (exception cases)");
    System.out.println("  [2]  Normal");
    System.out.println("  [0]  Back");

    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  /**
   * Walks the List ADT by position (1-based) with getEntry, so the number shown
   * against each guest is the same one the user types to pick them.
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

  public int inputPositionToCancel(int maximumPosition) {
    while (true) {
      System.out.print("\nEnter the position to cancel (1-" + maximumPosition
          + ", or 0 to go back): ");
      if (!scanner.hasNextLine()) {
        return 0;
      }
      String input = MessageUI.readLine(scanner);

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

  public boolean confirm(String prompt) {
    while (true) {
      System.out.print("\n" + prompt + " (Y/N): ");
      if (!scanner.hasNextLine()) {
        return false;
      }
      String answer = MessageUI.readLine(scanner).toUpperCase();

      if (answer.equals("Y") || answer.equals("YES")) {
        return true;
      }
      if (answer.equals("N") || answer.equals("NO")) {
        return false;
      }

      displayMessage("Please answer Y or N.");
    }
  }

  // ===== OUTPUT - given a ready-made list, these decide only how it looks =====

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
   * Every queue, search, filter and sorted listing ends up here already ordered.
   *
   * Reads the List ADT three ways: getNumberOfEntries() to size the pages,
   * getEntry() to fetch each row, and countIf() to count those still waiting.
   *
   * @return true if the user quit with [Q], so the caller can skip its pause
   */
  public boolean displayGuestList(ListInterface<WalkInGuest> list, String heading,
      String emptyMessage) {
    if (list == null || list.isEmpty()) {
      System.out.println("\n" + emptyMessage);
      return false;
    }

    int totalRows = list.getNumberOfEntries();
    int totalPages = (totalRows + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
    int currentPage = 1;

    while (true) {
      int firstRow = (currentPage - 1) * ROWS_PER_PAGE + 1;
      int lastRow = Math.min(currentPage * ROWS_PER_PAGE, totalRows);

      // Primed from the earlier rows, or page 2 would restart the count at 0.
      int guestsAhead = countWaitingBefore(list, firstRow);

      if (totalPages > 1) {
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
        return false;
      }

      int command = readPageCommand(currentPage, totalPages);
      if (command == PAGE_QUIT) {
        MessageUI.clearScreen();
        return true;
      }
      currentPage = command;
    }
  }

  /** Counts by walking the List rather than countIf, since only rows 1 to
   * firstRow-1 are wanted and countIf has no position range. */
  private int countWaitingBefore(ListInterface<WalkInGuest> list, int firstRow) {
    int waitingSoFar = 0;
    for (int row = 1; row < firstRow; row++) {
      if (WalkInGuest.STATUS_WAITING.equals(list.getEntry(row).getStatus())) {
        waitingSoFar++;
      }
    }
    return waitingSoFar;
  }

  private int readPageCommand(int currentPage, int totalPages) {
    while (true) {
      System.out.println();
      System.out.print("[N]ext page, [P]revious page, [1-" + totalPages
          + "] jump to page, [Q] quit listing: ");

      if (!scanner.hasNextLine()) {
        return PAGE_QUIT;
      }
      String input = MessageUI.readLine(scanner).toUpperCase();

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
        // fall through
      }

      displayMessage("Invalid input! Enter N, P, a page number, or Q.");
    }
  }

  public void displayGuestServedMessage(WalkInGuest guest) {
    if (guest == null) {
      displayMessage("No guests are waiting - there is no one to serve.");
      return;
    }

    System.out.println();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxCentred("NOW SERVING");
    MessageUI.displayBoxBottom();
    displayGuest(guest);
  }

  public void displayMessage(String message) {
    System.out.println("\n" + message);
  }

  public void displayReportHeader(String reportTitle) {
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

  public void displayReportFooter() {
    System.out.println("-".repeat(REPORT_WIDTH));
    System.out.println(centre("END OF THE REPORT"));
    System.out.println("=".repeat(REPORT_WIDTH));
  }

  public void displayReportSection(String title) {
    System.out.println();
    System.out.println(title);
    System.out.println("-".repeat(REPORT_WIDTH));
  }

  public void displayReportLine(String label, String value) {
    System.out.printf("  %-46s : %s%n", label, value);
  }

  public void displayBlankLine() {
    System.out.println();
  }

  public void displayExceptionRow(WalkInGuest guest) {
    System.out.printf("  %-9s %-24s %-17s %-10s %s%n",
        guest.getGuestId(),
        guest.getName(),
        guest.getFormattedArrivalTime(),
        guest.getStatus(),
        guest.getUrgencyReason() == null ? "-" : guest.getUrgencyReason());
  }

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

    int labelWidth = 0;
    for (String label : labels) {
      if (label != null && label.length() > labelWidth) {
        labelWidth = label.length();
      }
    }

    for (int i = 0; i < labels.length; i++) {
      int barWidth = (int) Math.round((double) counts[i] / highest * maximumBarWidth);
      if (counts[i] > 0 && barWidth == 0) {
        barWidth = 1;
      }
      System.out.printf("  %-" + labelWidth + "s | %-" + maximumBarWidth + "s %d%n",
          labels[i], "*".repeat(barWidth), counts[i]);
    }
  }

  public void pause() {
    System.out.print("\nPress Enter to continue...");
    if (scanner.hasNextLine()) {
      scanner.nextLine();
    }
  }

  private String columnHeadings() {
    return String.format("%-4s %-9s %-26s %-14s %-8s %-17s %-9s %-5s",
        "No.", "Guest ID", "Name", "Contact", "Type", "Arrived", "Status", "Ahead");
  }

  private String singleGuestHeadings() {
    return String.format("%-9s %-26s %-14s %-8s %-17s %-9s",
        "Guest ID", "Name", "Contact", "Type", "Arrived", "Status");
  }

  private String centre(String text) {
    if (text.length() >= REPORT_WIDTH) {
      return text;
    }
    int padding = (REPORT_WIDTH - text.length()) / 2;
    return " ".repeat(padding) + text;
  }
}
