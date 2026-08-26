package boundary;

import adt.ListInterface;
import control.ResortData;
import entity.Guest;
import entity.RoomType;
import entity.WalkInRegistration;
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
    MessageUI.displayMenuScreen("WALK-IN REGISTRATION",
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
          "Display current waiting queue",
          "Cancel a waiting guest",
          "Mark a called guest as no-show"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 4, "go back");
  }

  public int getSearchMenuChoice() {
    MessageUI.displayMenuScreen("SEARCH & FILTER", null,
        "Main Menu  >  Walk-In Registration  >  Search & Filter",
        new String[] {
          "Search by registration ID",
          "Search by guest name (partial match)",
          "Filter by status",
          "Filter by priority"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 4, "go back");
  }

  public int getSortMenuChoice() {
    MessageUI.displayMenuScreen("SORTED LISTINGS", null,
        "Main Menu  >  Walk-In Registration  >  Sorted Listings",
        new String[] {
          "By arrival time (earliest first)",
          "By guest name (A-Z)",
          "By waiting time (longest first)",
          "By service order (urgent lane first, then arrival)"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 4, "go back");
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
    String ic = MessageUI.readRequiredText(scanner, "IC / Passport number");
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

    String name = MessageUI.readRequiredText(scanner, "Full name");
    if (MessageUI.isCancelled(name)) {
      return null;
    }

    String contact = MessageUI.readRequiredText(scanner, "Contact number");
    if (MessageUI.isCancelled(contact)) {
      return null;
    }

    String email = MessageUI.readOptionalText(scanner, "Email");
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
        String.format("  %-6s %-18s %5s %12s  %s",
            "TYPE", "NAME", "MAX", "RATE/NIGHT", "DESCRIPTION"));

    for (int i = 1; i <= types.getNumberOfEntries(); i++) {
      RoomType type = types.getEntry(i);
      System.out.printf("  %-6s %-18s %5d %12.2f  %s%n",
          type.getTypeId(), type.getTypeName(), type.getMaxOccupancy(),
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
    String id = MessageUI.readRequiredText(scanner, "Registration ID (e.g. WR0003)");
    return MessageUI.isCancelled(id) ? null : id;
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
    MessageUI.displayField("Arrived", reg.getFormattedArrivalTime());
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
    if (list.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  " + emptyMessage);
      return false;
    }

    int totalPages = MessageUI.pageCount(list.getNumberOfEntries());
    int shown = 0;

    for (int page = 1; page <= totalPages; page++) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format("  %-7s %-24s %-8s %-6s %-6s %-11s %s",
          "REG ID", "GUEST", "PRIORITY", "TYPE", "NIGHTS", "STATUS", "ARRIVED"));

      int upTo = Math.min(shown + MessageUI.PAGE_SIZE, list.getNumberOfEntries());
      for (int i = shown + 1; i <= upTo; i++) {
        WalkInRegistration reg = list.getEntry(i);
        Guest guest = data.findGuest(reg.getGuestId());
        String name = (guest == null) ? "-" : guest.getFullName();

        System.out.printf("  %-7s %-24s %-8s %-6s %6d %-11s %s%n",
            reg.getRegId(), truncate(name, 24), reg.getPriority(),
            reg.getRequestedTypeId(), reg.getRequestedNights(), reg.getStatus(),
            reg.getFormattedArrivalTime());
      }
      shown = upTo;

      MessageUI.displayThinRule();
      System.out.printf("  %d registration(s).%n", list.getNumberOfEntries());

      if (!MessageUI.askForNextPage(scanner, page, totalPages)) {
        break;
      }
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

    MessageUI.displayBlankLine();
    System.out.printf("  Urgent: %d     Normal: %d     Total: %d%n",
        urgentCount, normalCount, serviceOrder.getNumberOfEntries());

    int totalPages = MessageUI.pageCount(serviceOrder.getNumberOfEntries());
    int shown = 0;

    for (int page = 1; page <= totalPages; page++) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format("  %-4s %-7s %-24s %-8s %-8s %-7s %s",
          "POS", "REG ID", "GUEST", "PRIORITY", "ARRIVED", "WAITED", "AHEAD"));

      int upTo = Math.min(shown + MessageUI.PAGE_SIZE, serviceOrder.getNumberOfEntries());
      for (int i = shown + 1; i <= upTo; i++) {
        WalkInRegistration reg = serviceOrder.getEntry(i);
        Guest guest = data.findGuest(reg.getGuestId());
        String name = (guest == null) ? "-" : guest.getFullName();

        System.out.printf("  %-4d %-7s %-24s %-8s %-8s %-7s %d%n",
            i, reg.getRegId(), truncate(name, 24), reg.getPriority(),
            shortTime(reg), reg.getFormattedWaitingTime(), i - 1);
      }
      shown = upTo;

      MessageUI.displayThinRule();
      MessageUI.displayMessage("  Position 1 is served next.");

      if (!MessageUI.askForNextPage(scanner, page, totalPages)) {
        break;
      }
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
    MessageUI.clearScreen();
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
  }
}
