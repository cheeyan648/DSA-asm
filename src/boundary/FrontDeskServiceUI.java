package boundary;

import adt.ListInterface;
import control.ResortData;
import entity.Booking;
import entity.Guest;
import entity.Invoice;
import entity.Payment;
import entity.Room;
import entity.RoomAssignment;
import entity.RoomType;
import java.time.LocalDate;
import java.util.Scanner;
import utility.MessageUI;

/**
 * Every screen and prompt for Front-Desk Service.
 *
 * @author Lim Yong Le
 */
public class FrontDeskServiceUI {

  private final Scanner scanner = MessageUI.scanner;

  // ==================================================================
  // MENUS
  // ==================================================================

  public int getMenuChoice() {
    MessageUI.displayMenuScreen("FRONT-DESK SERVICE", null,
        "Main Menu  >  Front-Desk Service",
        new String[] {
          "Bookings (create, amend, cancel)",
          "Rooms (availability, assign, expedite cleaning)",
          "Stay (check in, check out)",
          "Billing (payments, loyalty discount)",
          "Search & display",
          "Reports"
        },
        "Back to main menu");
    return MessageUI.readMenuChoice(scanner, 6, "go back to the main menu");
  }

  public int getBookingMenuChoice() {
    MessageUI.displayMenuScreen("BOOKINGS", null,
        "Main Menu  >  Front-Desk Service  >  Bookings",
        new String[] {
          "Create a new booking",
          "Amend a booking (dates or guest count)",
          "Cancel a booking",
          "Mark a booking as no-show"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 4, "go back");
  }

  public int getRoomMenuChoice() {
    MessageUI.displayMenuScreen("ROOMS", null,
        "Main Menu  >  Front-Desk Service  >  Rooms",
        new String[] {
          "Check room availability",
          "Assign a room to a pending booking",
          "Move a booking to another room",
          "Request urgent cleaning for a waiting booking",
          "Room status board"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 5, "go back");
  }

  public int getStayMenuChoice() {
    MessageUI.displayMenuScreen("STAY", null,
        "Main Menu  >  Front-Desk Service  >  Stay",
        new String[] {
          "Check in a guest",
          "Check out a guest"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  public int getBillingMenuChoice() {
    MessageUI.displayMenuScreen("BILLING", null,
        "Main Menu  >  Front-Desk Service  >  Billing",
        new String[] {
          "View an invoice",
          "Record a payment",
          "Apply an approved loyalty reward to a bill"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  public int getSearchMenuChoice() {
    MessageUI.displayMenuScreen("SEARCH & DISPLAY", null,
        "Main Menu  >  Front-Desk Service  >  Search & Display",
        new String[] {
          "Search by booking ID",
          "Search by guest name",
          "Search by room number",
          "Filter by status",
          "Display all bookings (sorted by ID)"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 5, "go back");
  }

  public int getReportMenuChoice() {
    MessageUI.displayMenuScreen("REPORTS", null,
        "Main Menu  >  Front-Desk Service  >  Reports",
        new String[] {
          "Occupancy & Revenue Report",
          "Outstanding Balance Report"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  // ==================================================================
  // INPUT
  // ==================================================================

  public String inputBookingId() {
    String id = MessageUI.readRequiredText(scanner, "Booking ID (e.g. BK0004)");
    return MessageUI.isCancelled(id) ? null : id;
  }

  public String inputRoomNo() {
    String roomNo = MessageUI.readRequiredText(scanner, "Room number (e.g. 1005)");
    return MessageUI.isCancelled(roomNo) ? null : roomNo;
  }

  public String inputInvoiceId() {
    String id = MessageUI.readRequiredText(scanner, "Invoice ID (e.g. INV0004)");
    return MessageUI.isCancelled(id) ? null : id;
  }

  public String inputGuestName() {
    String name = MessageUI.readRequiredText(scanner, "Guest name (or part of it)");
    return MessageUI.isCancelled(name) ? null : name;
  }

  public LocalDate inputDate(String prompt) {
    return MessageUI.readDate(scanner, prompt);
  }

  public int inputGuestCount(int maximum) {
    int guests = MessageUI.readInt(scanner, "Number of guests", 1, maximum);
    return (guests == MessageUI.CANCELLED_INT) ? -1 : guests;
  }

  public double inputAmount(String prompt) {
    return MessageUI.readAmount(scanner, prompt);
  }

  /**
   * Asks which room type is wanted, listing the rates.
   *
   * @param types the types on offer
   * @return the chosen type's ID, or null if cancelled
   */
  public String inputRoomType(ListInterface<RoomType> types) {
    MessageUI.displaySectionHeading("Room types");
    MessageUI.displayTableHeading(String.format("  %-6s %-18s %5s %12s  %s",
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
    return (picked == MessageUI.CANCELLED_INT) ? null : types.getEntry(picked).getTypeId();
  }

  public String inputBookingSource() {
    String source = MessageUI.readChoice(scanner, "Booking source", new String[] {
      Booking.SOURCE_ONLINE, Booking.SOURCE_PHONE, Booking.SOURCE_CORPORATE
    });
    return MessageUI.isCancelled(source) ? null : source;
  }

  public String inputPaymentMethod() {
    String method = MessageUI.readChoice(scanner, "Payment method", new String[] {
      Payment.CASH, Payment.CARD, Payment.EWALLET, Payment.BANK_TRANSFER
    });
    return MessageUI.isCancelled(method) ? null : method;
  }

  public String inputPaymentReference() {
    String reference = MessageUI.readRequiredText(scanner, "Approval / transaction reference");
    return MessageUI.isCancelled(reference) ? null : reference;
  }

  public String inputStatusFilter() {
    String status = MessageUI.readChoice(scanner, "Booking status", new String[] {
      Booking.STATUS_PENDING, Booking.STATUS_CONFIRMED, Booking.STATUS_CHECKED_IN,
      Booking.STATUS_CHECKED_OUT, Booking.STATUS_CANCELLED, Booking.STATUS_NO_SHOW
    });
    return MessageUI.isCancelled(status) ? null : status;
  }

  /**
   * Asks the user to pick one of the rooms on offer.
   *
   * @param rooms the rooms that could be given
   * @return the chosen room number, or null if cancelled
   */
  public String chooseRoom(ListInterface<Room> rooms) {
    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format("  %-4s %-6s %-6s %-6s %-22s %s",
        "NO", "ROOM", "TYPE", "FLOOR", "HOUSEKEEPING", "OCCUPANCY"));

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      System.out.printf("  %-4d %-6s %-6s %-6d %-22s %s%n",
          i, room.getRoomNo(), room.getTypeId(), room.getFloorNo(),
          room.getHousekeepingStatus(), room.getOccupancyStatus());
    }
    MessageUI.displayThinRule();

    int picked = MessageUI.readInt(scanner, "Room number from the list", 1,
        rooms.getNumberOfEntries());
    return (picked == MessageUI.CANCELLED_INT) ? null : rooms.getEntry(picked).getRoomNo();
  }

  public String inputAssignmentReason() {
    String reason = MessageUI.readChoice(scanner, "Reason for the move", new String[] {
      RoomAssignment.REASON_UPGRADE,
      RoomAssignment.REASON_GUEST_REQUEST,
      RoomAssignment.REASON_MAINTENANCE
    });
    return MessageUI.isCancelled(reason) ? null : reason;
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

  public void displaySectionHeading(String title) {
    MessageUI.displaySectionHeading(title);
  }

  /**
   * Shows one booking in full, with its guest and bill.
   *
   * @param booking the booking
   * @param data used to look up the guest, room type and invoice
   */
  public void displayBooking(Booking booking, ResortData data) {
    Guest guest = data.findGuest(booking.getGuestId());
    RoomType type = data.findRoomType(booking.getTypeId());

    MessageUI.displayBlankLine();
    MessageUI.displayField("Booking ID", booking.getBookingId());
    MessageUI.displayField("Guest", (guest == null ? "-" : guest.getFullName())
        + " (" + booking.getGuestId() + ")");
    MessageUI.displayField("Room type", type == null ? booking.getTypeId()
        : type.getTypeName() + " (" + booking.getTypeId() + ")");
    MessageUI.displayField("Room", booking.getRoomNo() == null
        ? "not assigned yet" : booking.getRoomNo());
    MessageUI.displayField("Stay", booking.getCheckInDate() + " to "
        + booking.getCheckOutDate() + "  (" + booking.getNumberOfNights() + " night(s))");
    MessageUI.displayField("Guests", String.valueOf(booking.getNumberOfGuests()));
    MessageUI.displayField("Status", booking.getBookingStatus());
    MessageUI.displayField("Priority", booking.getPriority());
    MessageUI.displayField("Source", booking.getSource()
        + (booking.getRegId() == null ? "" : "  (from " + booking.getRegId() + ")"));
    MessageUI.displayField("Rate per night", String.format("RM%.2f", booking.getRatePerNight()));

    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice != null) {
      MessageUI.displayField("Invoice", invoice.getInvoiceId() + "  "
          + invoice.getPaymentStatus()
          + String.format("  (RM%.2f of RM%.2f paid)",
              invoice.getAmountPaid(), invoice.getTotalAmount()));
    }
  }

  /**
   * Shows a bill broken into its parts.
   *
   * Every line is shown rather than just the total, because a guest querying
   * their bill at the counter wants to see where the figure came from.
   *
   * @param invoice the bill
   * @param payments the payments taken against it
   */
  public void displayInvoice(Invoice invoice, ListInterface<Payment> payments) {
    MessageUI.displaySectionHeading("Invoice " + invoice.getInvoiceId()
        + "  (booking " + invoice.getBookingId() + ")");

    System.out.printf("  %-28s   RM%10.2f%n", "Room charge", invoice.getRoomCharge());
    System.out.printf("  %-28s   RM%10.2f%n", "Service charge (10%)",
        invoice.getServiceCharge());
    System.out.printf("  %-28s   RM%10.2f%n", "SST (6%)", invoice.getTaxAmount());

    if (invoice.getDiscountAmount() > 0) {
      System.out.printf("  %-28s  -RM%10.2f%n", "Loyalty discount",
          invoice.getDiscountAmount());
    }

    MessageUI.displayThinRule();
    System.out.printf("  %-28s   RM%10.2f%n", "TOTAL", invoice.getTotalAmount());
    System.out.printf("  %-28s   RM%10.2f%n", "Paid", invoice.getAmountPaid());
    System.out.printf("  %-28s   RM%10.2f%n", "Outstanding",
        invoice.getOutstandingBalance());
    System.out.printf("  %-28s   %s%n", "Status", invoice.getPaymentStatus());

    if (payments != null && !payments.isEmpty()) {
      MessageUI.displaySectionHeading("Payments received");
      MessageUI.displayTableHeading(String.format("  %-7s %10s  %-14s %-18s %s",
          "PAY ID", "AMOUNT", "METHOD", "REFERENCE", "TAKEN"));

      for (int i = 1; i <= payments.getNumberOfEntries(); i++) {
        Payment payment = payments.getEntry(i);
        System.out.printf("  %-7s %10.2f  %-14s %-18s %s%n",
            payment.getPaymentId(), payment.getAmount(), payment.getMethod(),
            (payment.getReference() == null || payment.getReference().isBlank())
                ? "-" : payment.getReference(),
            payment.getPaidAt().toLocalDate());
      }
      MessageUI.displayThinRule();
    }
  }

  /**
   * Lists bookings as a table, a page at a time.
   *
   * @param bookings the bookings to show
   * @param data used to turn a guest ID into a name
   * @param emptyMessage what to say when there is nothing to show
   * @return true if anything was shown
   */
  public boolean displayBookingList(ListInterface<Booking> bookings, ResortData data,
      String emptyMessage) {
    if (bookings.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  " + emptyMessage);
      return false;
    }

    int totalPages = MessageUI.pageCount(bookings.getNumberOfEntries());
    int shown = 0;

    for (int page = 1; page <= totalPages; page++) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format("  %-7s %-20s %-6s %-6s %-11s %-11s %-12s %s",
          "BOOKING", "GUEST", "TYPE", "ROOM", "CHECK IN", "CHECK OUT", "STATUS", "PRI"));

      int upTo = Math.min(shown + MessageUI.PAGE_SIZE, bookings.getNumberOfEntries());
      for (int i = shown + 1; i <= upTo; i++) {
        Booking booking = bookings.getEntry(i);
        Guest guest = data.findGuest(booking.getGuestId());

        System.out.printf("  %-7s %-20s %-6s %-6s %-11s %-11s %-12s %s%n",
            booking.getBookingId(),
            guest == null ? "-" : truncate(guest.getFullName(), 20),
            booking.getTypeId(),
            booking.getRoomNo() == null ? "-" : booking.getRoomNo(),
            booking.getCheckInDate(), booking.getCheckOutDate(),
            booking.getBookingStatus(),
            booking.isUrgent() ? "URG" : "-");
      }
      shown = upTo;

      MessageUI.displayThinRule();
      System.out.printf("  %d booking(s).%n", bookings.getNumberOfEntries());

      if (!MessageUI.askForNextPage(scanner, page, totalPages)) {
        break;
      }
    }
    return true;
  }

  /**
   * Shows every room with both of its statuses and whether it can be sold.
   *
   * This is the screen that makes the front desk and housekeeping visibly one
   * system: the assignable column is the two modules' answers combined.
   *
   * @param rooms the rooms
   * @param data used to look up each room's type
   */
  public void displayRoomBoard(ListInterface<Room> rooms, ResortData data) {
    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format("  %-6s %-16s %-10s %-22s %s",
        "ROOM", "TYPE", "OCCUPANCY", "HOUSEKEEPING", "ASSIGNABLE?"));

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      RoomType type = data.findRoomType(room.getTypeId());

      String assignable;
      if (room.isOutOfService()) {
        assignable = "No (out of service)";
      } else if (!Room.VACANT.equals(room.getOccupancyStatus())) {
        assignable = "No (" + room.getOccupancyStatus().toLowerCase() + ")";
      } else if (!Room.READY_FOR_CHECK_IN.equals(room.getHousekeepingStatus())) {
        assignable = "No (not cleaned)";
      } else {
        assignable = "Yes";
      }

      System.out.printf("  %-6s %-16s %-10s %-22s %s%n",
          room.getRoomNo(),
          type == null ? room.getTypeId() : truncate(type.getTypeName(), 16),
          room.getOccupancyStatus(), room.getHousekeepingStatus(), assignable);
    }
    MessageUI.displayThinRule();
  }

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

  public void displayReportLine(String label, String value) {
    MessageUI.displayReportLine(label, value);
  }

  public void displayBarChart(String title, String yAxisLabel, String[] labels,
      double[] values) {
    MessageUI.displayBarChart(title, yAxisLabel, labels, values);
  }

  public void displayTableHeading(String heading) {
    MessageUI.displayTableHeading(heading);
  }

  public void displayThinRule() {
    MessageUI.displayThinRule();
  }

  public void displayReportFooter() {
    MessageUI.displayBlankLine();
    MessageUI.displayRule();
    MessageUI.displayMessage("  END OF REPORT");
    MessageUI.displayRule();
  }
}
