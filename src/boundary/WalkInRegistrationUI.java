package boundary;

import adt.ListInterface;
import control.ResortData;
import entity.Guest;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import utility.MessageUI;

/**
 * Every screen and prompt for Walk-In Registration.
 *
 * Under the ECB pattern this is the only class in the module allowed to print
 * or read, so the control class can be tested without a console attached.
 * Screens are drawn through MessageUI rather than with print statements of its
 * own, so this module looks identical to the other three.
 *
 * @author Tan Chee Yan
 */
public class WalkInRegistrationUI {

  private final Scanner scanner = MessageUI.scanner;

  /**
   * The standard urgency reasons.
   *
   * Offered as a list rather than left as free text so the audit report can
   * group them - typed reasons would be worded differently every time and
   * could not be counted.
   */
  private static final String[] URGENCY_REASONS = {
    "Elderly or disabled guest",
    "Medical or emergency situation",
    "Travelling with an infant or young children",
    "Complaint escalation",
    "Other (describe)"
  };

  // ==================================================================
  // MENUS
  // ==================================================================

  public int getMenuChoice() {
    MessageUI.displayMenuScreen("WALK-IN REGISTRATION", null,
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
    MessageUI.displayMenuScreen("GUEST REGISTRATION", null,
        "Main Menu  >  Walk-In Registration  >  Guest Registration",
        new String[] {
          "Register normal walk-in (joins the back of the queue)",
          "Register urgent walk-in (exception case)",
          "Undo last registration"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  public int getQueueMenuChoice() {
    MessageUI.displayMenuScreen("QUEUE OPERATIONS", null,
        "Main Menu  >  Walk-In Registration  >  Queue Operations",
        new String[] {
          "Serve next guest (hands over to Front Desk)",
          "Cancel a waiting guest",
          "Mark a called guest as no-show"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  public int getSearchMenuChoice() {
    MessageUI.displayMenuScreen("SEARCH & FILTER", null,
        "Main Menu  >  Walk-In Registration  >  Search & Filter",
        new String[] {
          "Display current waiting queue",
          "Search by registration number",
          "Search by guest name (partial match)",
          "Filter by status",
          "Filter by priority"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 5, "go back");
  }

  public int getReportMenuChoice() {
    MessageUI.displayMenuScreen("REPORTS", null,
        "Main Menu  >  Walk-In Registration  >  Reports",
        new String[] {
          "Queue Performance Analysis Report",
          "Urgency Exception Audit Report"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  // ==================================================================
  // INPUT
  // ==================================================================

  /**
   * Asks for the identity document that identifies a returning guest.
   *
   * @return the IC or passport number, or null if cancelled
   */
  public String inputIcPassport() {
    String ic = MessageUI.readIcPassport(scanner, "IC / Passport number");
    return MessageUI.isCancelled(ic) ? null : ic;
  }

  /**
   * Collects the details of a guest the resort has not seen before.
   *
   * @param guestId the ID already reserved for them
   * @param icPassportNo the document they were looked up by
   * @return the new guest, or null if cancelled
   */
  public Guest inputNewGuest(String guestId, String icPassportNo) {
    MessageUI.displayMessage("");
    MessageUI.displayMessage("  This guest is not on record. Please take their details.");
    MessageUI.displayMessage("");

    String name = MessageUI.readName(scanner, "Full name");
    if (MessageUI.isCancelled(name)) {
      return null;
    }

    String contact = MessageUI.readPhone(scanner, "Contact number");
    if (MessageUI.isCancelled(contact)) {
      return null;
    }

    String email = MessageUI.readOptionalEmail(scanner, "Email");
    if (MessageUI.isCancelled(email)) {
      return null;
    }

    return new Guest(guestId, name, icPassportNo, contact, email,
        java.time.LocalDateTime.now());
  }

  /**
   * Asks which room type the guest wants.
   *
   * The types are listed with their rate and capacity so the officer can
   * answer the guest's questions without leaving the screen.
   *
   * @param types the room types on offer
   * @return the chosen type's ID, or null if cancelled
   */
  public String inputRoomType(ListInterface<RoomType> types) {
    MessageUI.displaySectionHeading("Room types");
    MessageUI.displayTableHeading(
        String.format("  %-5s %-6s %-18s %5s %12s  %s",
            "NO", "TYPE", "NAME", "MAX", "RATE/NIGHT", "DESCRIPTION"));

    // The number in the first column is what the officer types. Without it the
    // table showed only type IDs like RT01 while the prompt asked for 1-5,
    // leaving the reader to work out that the two lined up.
    for (int i = 1; i <= types.getNumberOfEntries(); i++) {
      RoomType type = types.getEntry(i);
      System.out.printf("  [%d]   %-6s %-18s %5d %12.2f  %s%n",
          i, type.getTypeId(), type.getTypeName(), type.getMaxOccupancy(),
          type.getBaseRatePerNight(), type.getDescription());
    }
    MessageUI.displayThinRule();

    int picked = MessageUI.readInt(scanner, "Room type number", 1,
        types.getNumberOfEntries());
    if (picked == MessageUI.CANCELLED_INT) {
      return null;
    }
    return types.getEntry(picked).getTypeId();
  }

  /**
   * Asks how many nights the guest wants.
   *
   * @return the number of nights, or -1 if cancelled
   */
  public int inputNights() {
    int nights = MessageUI.readInt(scanner, "Number of nights", 1, 30);
    return (nights == MessageUI.CANCELLED_INT) ? -1 : nights;
  }

  /**
   * Asks which day the guest wants to arrive.
   *
   * Re-asked rather than refused when a past date is given, so a mistyped year
   * costs one line instead of the whole registration.
   *
   * @return the arrival date, or null if cancelled
   */
  public LocalDate inputCheckInDate() {
    while (true) {
      LocalDate checkIn = MessageUI.readDate(scanner, "Check-in date");
      if (checkIn == null) {
        return null;
      }
      if (checkIn.isBefore(LocalDate.now())) {
        MessageUI.displayError("The check-in date cannot be in the past.");
        continue;
      }
      return checkIn;
    }
  }

  /**
   * Shows the stay the guest asked for, with the departure date it works out
   * to, so both are on screen before the registration is stored.
   *
   * @param checkIn the arrival date given
   * @param nights how many nights were asked for
   */
  public void displayCalculatedStay(LocalDate checkIn, int nights) {
    DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy");

    MessageUI.displayBlankLine();
    MessageUI.displayField("Nights requested", nights + " night(s)");
    MessageUI.displayField("Check-in", checkIn.format(dayFormat));
    MessageUI.displayField("Check-out", checkIn.plusDays(nights).format(dayFormat));
    MessageUI.displayBlankLine();
  }

  /**
   * Asks why a guest is being given an urgent place in the queue.
   *
   * A reason is mandatory: letting a guest jump the queue without recording
   * why is what would make the override impossible to audit afterwards.
   *
   * @return the reason, or null if cancelled
   */
  public String inputUrgencyReason() {
    MessageUI.displaySectionHeading("Reason for the urgent exception");
    MessageUI.displayMessage("  An urgent guest is served before everyone waiting,");
    MessageUI.displayMessage("  so the reason is recorded and reviewed.");

    String chosen = MessageUI.readChoice(scanner, "Reason", URGENCY_REASONS);
    if (MessageUI.isCancelled(chosen)) {
      return null;
    }

    if (chosen.startsWith("Other")) {
      String described = MessageUI.readRequiredText(scanner, "Describe the reason");
      return MessageUI.isCancelled(described) ? null : described;
    }
    return chosen;
  }

  /**
   * Asks which status to filter a listing by.
   *
   * @return the status, or null if cancelled
   */
  public String inputStatusFilter() {
    String chosen = MessageUI.readChoice(scanner, "Status", new String[] {
      WalkInRegistration.STATUS_WAITING,
      WalkInRegistration.STATUS_IN_SERVICE,
      WalkInRegistration.STATUS_BOOKED,
      WalkInRegistration.STATUS_CANCELLED,
      WalkInRegistration.STATUS_NO_SHOW
    });
    return MessageUI.isCancelled(chosen) ? null : chosen;
  }

  /**
   * Asks which priority to filter a listing by.
   *
   * @return the priority, or null if cancelled
   */
  public String inputPriorityFilter() {
    String chosen = MessageUI.readChoice(scanner, "Priority", new String[] {
      WalkInRegistration.PRIORITY_URGENT,
      WalkInRegistration.PRIORITY_NORMAL
    });
    return MessageUI.isCancelled(chosen) ? null : chosen;
  }

  public String inputRegistrationId() {
    String id = MessageUI.readIdNumber(scanner, "Registration number", "WR", 4);
    return MessageUI.isCancelled(id) ? null : id;
  }

  /**
   * Asks which waiting guest to act on, by their position in the queue.
   *
   * The position is what the officer can see on the screen in front of them
   * and on the queue display the guest is standing next to; the registration
   * ID is not. Asking for the ID meant reading it off the table and retyping
   * its prefix, which is the step that went wrong.
   *
   * Re-prompts until a position that is actually in the queue is entered, so a
   * typo costs one line rather than sending the officer back to the menu.
   *
   * @param queueSize how many guests are waiting
   * @return the 1-based position, or -1 if the user typed 0 to quit
   */
  public int inputQueuePosition(int queueSize) {
    return inputListPosition(queueSize, "Position to cancel");
  }

  /**
   * Asks for a row number from the listing just shown.
   *
   * Re-prompts until the number names a row that is actually there, so a typo
   * costs one line rather than sending the officer back to the menu to start
   * the action again.
   *
   * @param size how many rows the listing had
   * @param prompt what the number is being asked for
   * @return the 1-based row number, or -1 if the user typed 0 to quit
   */
  public int inputListPosition(int size, String prompt) {
    while (true) {
      System.out.printf("  %s (1-%d, 0 to quit): ", prompt, size);
      String input = MessageUI.readLine(scanner);

      if ("0".equals(input)) {
        return -1;
      }
      if (input.isEmpty()) {
        MessageUI.displayError("This cannot be left blank.");
        continue;
      }

      try {
        int position = Integer.parseInt(input);
        if (position < 1 || position > size) {
          MessageUI.displayError("There is no number " + position
              + " in that list. Enter a number from 1 to " + size + ".");
          continue;
        }
        return position;
      } catch (NumberFormatException notANumber) {
        MessageUI.displayError("Please enter one of the numbers shown in the"
            + " first column, e.g. 1.");
      }
    }
  }

  /**
   * Says how many guests are still waiting, in a sentence that reads properly
   * whatever the number is.
   *
   * @param waiting how many guests are still in the queue
   * @return e.g. "1 guest is still waiting" or "3 guests are still waiting"
   */
  public static String waitingSentence(int waiting) {
    if (waiting == 0) {
      return "no guests are still waiting";
    }
    if (waiting == 1) {
      return "1 guest is still waiting";
    }
    return waiting + " guests are still waiting";
  }

  public String inputSearchName() {
    String name = MessageUI.readRequiredText(scanner, "Guest name (or part of it)");
    return MessageUI.isCancelled(name) ? null : name;
  }

  public boolean confirm(String question) {
    return MessageUI.confirm(scanner, question);
  }

  public void pause() {
    MessageUI.pause(scanner);
  }

  /**
   * Pauses under a caller-chosen wording.
   *
   * @param prompt what to tell the user, without its trailing dots
   */
  public void pause(String prompt) {
    MessageUI.pause(scanner, prompt);
  }

  /**
   * Asks whether to run the same lookup again with a different value.
   *
   * Sits under a result that is already on screen. Answering no is what ends
   * the action, which is why these screens no longer finish with a pause: the
   * question doubles as the way out.
   *
   * @param question what running it again would do
   * @return true to go round again
   */
  public boolean confirmAnother(String question) {
    MessageUI.displayBlankLine();
    return MessageUI.confirm(scanner, question);
  }

  /**
   * Asks which order to sort the listing by before anything is displayed.
   *
   * Shown as its own menu screen so the guest picks an order first, rather
   * than being shown a default ordering they did not ask for.
   *
   * @return the chosen order, or 0 to leave
   */
  public int getSortChoice() {
    MessageUI.displayMenuScreen("SORTED LISTINGS", null,
        "Main Menu  >  Walk-In Registration  >  Sorted Listings",
        new String[] {
          "Arrival time (earliest first)",
          "Guest name (A-Z)",
          "Waiting time (longest first)",
          "Status, then arrival time"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 4, "go back");
  }

  // ==================================================================
  // DISPLAY
  // ==================================================================

  public void startAction(String title) {
    MessageUI.startAction(title);
  }

  public void displayMessage(String message) {
    MessageUI.displayMessage(message);
  }

  public void displayError(String message) {
    MessageUI.displayError(message);
  }

  public void displaySuccess(String message) {
    MessageUI.displaySuccess(message);
  }

  /**
   * Shows one registration in full.
   *
   * @param reg the registration
   * @param guestName who it is for
   * @param typeName the room type they asked for
   */
  public void displayRegistration(WalkInRegistration reg, String guestName, String typeName) {
    MessageUI.displayBlankLine();
    MessageUI.displayField("Registration ID", reg.getRegId());
    MessageUI.displayField("Guest", guestName + " (" + reg.getGuestId() + ")");
    MessageUI.displayField("Priority", reg.getPriority());
    if (reg.isUrgent()) {
      MessageUI.displayField("Urgency reason", reg.getUrgencyReason());
    }
    MessageUI.displayField("Requested", typeName + ", " + reg.getRequestedNights() + " night(s)");
    if (reg.getRequestedCheckInDate() != null) {
      MessageUI.displayField("Stay", reg.getRequestedCheckInDate() + " to "
          + reg.getRequestedCheckOutDate());
    }
    MessageUI.displayField("Arrived", reg.getFormattedArrivalTime());
    MessageUI.displayField("Joined queue", reg.getFormattedQueuedAt());
    if (reg.getServedAt() != null) {
      MessageUI.displayField("Left queue", reg.getFormattedServedAt());
    }
    MessageUI.displayField("Waiting", reg.getFormattedWaitingTime());
    MessageUI.displayField("Status", reg.getStatus());

    if (reg.getServedBy() != null) {
      MessageUI.displayField("Served by", reg.getServedBy());
    }
    if (reg.getBookingId() != null) {
      MessageUI.displayField("Became booking", reg.getBookingId());
    }
  }

  /**
   * Lists registrations as a table, a page at a time.
   *
   * @param list the registrations to show
   * @param data used to turn a guest ID into a name
   * @param emptyMessage what to say when there is nothing to show
   * @return true if anything was shown
   */
  public boolean displayRegistrationList(ListInterface<WalkInRegistration> list,
      ResortData data, String emptyMessage) {
    return displayRegistrationList(list, data, emptyMessage, null);
  }

  /**
   * Lists registrations a page at a time under a title of the caller's.
   *
   * Each page replaces the one before it rather than printing underneath it,
   * so the screen always shows exactly one page. That means the title has to
   * be redrawn after every clear, which is why the caller hands it in here
   * instead of printing it once beforehand.
   *
   * @param title the heading to redraw above each page, or null for none
   */
  public boolean displayRegistrationList(ListInterface<WalkInRegistration> list,
      ResortData data, String emptyMessage, String title) {
    if (list.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  " + emptyMessage);
      return false;
    }

    int total = list.getNumberOfEntries();
    int totalPages = MessageUI.pageCount(total);

    int page = 1;
    while (true) {
      // Only a listing that actually pages needs the screen wiped; a single
      // page would lose whatever the caller printed above it for nothing.
      if (totalPages > 1 && title != null) {
        startAction(title);
      }

      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format(
          "  %-4s %-7s %-22s %-8s %-5s %-11s %-16s %s",
          "NO", "REG ID", "GUEST", "PRIORITY", "TYPE", "STATUS",
          "QUEUED", "LEFT QUEUE"));

      int from = MessageUI.firstRowOnPage(page);
      int upTo = MessageUI.lastRowOnPage(page, total);

      for (int i = from; i <= upTo; i++) {
        WalkInRegistration reg = list.getEntry(i);
        Guest guest = data.findGuest(reg.getGuestId());
        String name = (guest == null) ? "-" : guest.getFullName();

        // Both ends of the wait are shown: when they joined the queue, and
        // when they left it. A guest still waiting has no second stamp yet.
        System.out.printf("  %-4d %-7s %-22s %-8s %-5s %-11s %-16s %s%n",
            i, reg.getRegId(), truncate(name, 22), reg.getPriority(),
            reg.getRequestedTypeId(), reg.getStatus(),
            reg.getFormattedQueuedAt(), reg.getFormattedServedAt());
      }

      MessageUI.displayThinRule();

      // Which rows these are, not just how many there are altogether - on a
      // paged listing the count alone does not say where the reader is.
      if (totalPages > 1) {
        System.out.printf("  Showing %d-%d of %d registration(s).%n",
            from, upTo, total);
      } else {
        System.out.printf("  %d registration(s).%n", total);
      }

      int next = MessageUI.readPageCommand(scanner, page, totalPages);
      if (next == MessageUI.PAGE_QUIT) {
        break;
      }
      page = next;
    }
    return true;
  }

  /**
   * Shows the waiting queue in the order guests will actually be served.
   *
   * The position and the number of guests ahead are worked out here rather
   * than stored, because they describe where someone sits in this particular
   * line rather than anything about the guest.
   *
   * @param serviceOrder the waiting registrations, urgent lane first
   * @param data used to turn a guest ID into a name
   * @param urgentCount how many are in the urgent lane
   * @param normalCount how many are in the normal lane
   * @return true if anyone is waiting
   */
  public boolean displayQueue(ListInterface<WalkInRegistration> serviceOrder,
      ResortData data, int urgentCount, int normalCount) {
    if (serviceOrder.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  The walk-in queue is empty. No guests are waiting.");
      return false;
    }

    int total = serviceOrder.getNumberOfEntries();
    int totalPages = MessageUI.pageCount(total);

    int page = 1;
    while (true) {
      // Each page replaces the last, so the title is redrawn with it.
      if (totalPages > 1) {
        startAction("CURRENT WALK-IN QUEUE");
      }

      MessageUI.displayBlankLine();
      System.out.printf("  Urgent: %d     Normal: %d     Total: %d%n",
          urgentCount, normalCount, total);
      MessageUI.displayBlankLine();

      MessageUI.displayTableHeading(String.format("  %-4s %-7s %-24s %-8s %-8s %-7s %s",
          "POS", "REG ID", "GUEST", "PRIORITY", "ARRIVED", "WAITED", "AHEAD"));

      // POS counts from the front of the whole queue, not from the top of the
      // page, because it is the number the officer types to act on a guest.
      int from = MessageUI.firstRowOnPage(page);
      int upTo = MessageUI.lastRowOnPage(page, total);

      for (int i = from; i <= upTo; i++) {
        WalkInRegistration reg = serviceOrder.getEntry(i);
        Guest guest = data.findGuest(reg.getGuestId());
        String name = (guest == null) ? "-" : guest.getFullName();

        System.out.printf("  %-4d %-7s %-24s %-8s %-8s %-7s %d%n",
            i, reg.getRegId(), truncate(name, 24), reg.getPriority(),
            shortTime(reg), reg.getFormattedWaitingTime(), i - 1);
      }

      MessageUI.displayThinRule();

      if (totalPages > 1) {
        System.out.printf("  Showing %d-%d of %d waiting.%n", from, upTo, total);
      }
      MessageUI.displayMessage("  Position 1 is served next.");

      int next = MessageUI.readPageCommand(scanner, page, totalPages);
      if (next == MessageUI.PAGE_QUIT) {
        break;
      }
      page = next;
    }
    return true;
  }

  /** The arrival time as just the clock time, to keep the queue table narrow. */
  private String shortTime(WalkInRegistration reg) {
    if (reg.getArrivalTime() == null) {
      return "-";
    }
    return String.format("%02d:%02d",
        reg.getArrivalTime().getHour(), reg.getArrivalTime().getMinute());
  }

  /** Shortens a value that would otherwise push a table column out of line. */
  private String truncate(String text, int width) {
    if (text == null) {
      return "-";
    }
    return (text.length() <= width) ? text : text.substring(0, width - 1) + ".";
  }

  // ==================================================================
  // REPORTS
  // ==================================================================

  public void displayReportHeader(String title) {
    MessageUI.beginLongOutput();
    MessageUI.displayBlankLine();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("TARUMT RESORT MANAGEMENT SYSTEM");
    MessageUI.displayBoxCentred(title);
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  Generated: " + java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")));
    MessageUI.displayBoxBottom();
  }

  /**
   * Opens the Queue Performance and Urgency Audit reports in the plain,
   * unboxed layout used across the printed report pack, rather than the
   * boxed screen style the rest of the module uses.
   *
   * @param title the report's name
   */
  public void displayPrintedReportHeader(String title) {
    MessageUI.clearScreen();
    MessageUI.displayRule();
    MessageUI.displayMessage(MessageUI.centre(
        "TUNKU ABDUL RAHMAN UNIVERSITY OF MANAGEMENT AND TECHNOLOGY"));
    MessageUI.displayMessage(MessageUI.centre(
        "TARUMT RESORT - WALK-IN REGISTRATION SUBSYSTEM"));
    MessageUI.displayMessage(MessageUI.centre(title));
    MessageUI.displayRule();
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("Generated at: " + java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, hh:mm a")));
  }

  /**
   * Closes a printed-style report with its centred sign-off and a plain
   * pause, matching the report pack rather than the boxed screens' wording.
   */
  public void displayPrintedReportFooter() {
    MessageUI.displayBlankLine();
    MessageUI.displayRule();
    MessageUI.displayMessage(MessageUI.centre("END OF THE REPORT"));
    MessageUI.displayRule();
    MessageUI.pause(scanner, "Press Enter to continue");
  }

  public void displaySectionHeading(String title) {
    MessageUI.displaySectionHeading(title);
  }

  public void displayReportLine(String label, String value) {
    MessageUI.displayReportLine(label, value);
  }

  public void displayBarChart(String title, String yAxisLabel, String[] labels,
      double[] values) {
    MessageUI.displayBarChart(title, yAxisLabel, labels, values);
  }

  public void displayReportFooter() {
    MessageUI.displayBlankLine();
    MessageUI.displayRule();
    MessageUI.displayMessage("  END OF REPORT");
    MessageUI.displayRule();
    MessageUI.endLongOutput(scanner);
  }
}
