package boundary;

import adt.ListInterface;
import control.ResortData;
import entity.Booking;
import entity.Guest;
import entity.Invoice;
import entity.Member;
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
 * @author Yong Le
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
          "Bookings (create, edit, update, delete)",
          "Rooms (availability, assign, expedite cleaning , manage)",
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
          "Edit / update a booking (dates or guest count)",
          "Cancel a booking",
          "Delete an unassigned booking",
          "Mark a booking as no-show"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 5, "go back");
  }

  public int getRoomMenuChoice() {
    MessageUI.displayMenuScreen("ROOMS", null,
        "Main Menu  >  Front-Desk Service  >  Rooms",
        new String[] {
          "Check room availability",
          "Assign a room to a pending booking",
          "Move a booking to another room",
          "Request urgent cleaning for a waiting booking",
          "Room status board",
          "Manage rooms"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 6, "go back");
  }

  public int getRoomManagementMenuChoice() {
    MessageUI.displayMenuScreen("MANAGE ROOMS", null,
        "Main Menu  >  Front-Desk Service  >  Rooms  >  Manage Rooms",
        new String[] {
          "List every room",
          "Add a room",
          "Remove a room",
          "Take a room out of service",
          "Return a room to service"
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
          "Search by 8-digit confirmation number",
          "Search by booking ID",
          "Search by guest name",
          "Search by room number",
          "Filter by status",
          "Display all bookings (sorted by ID)"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 6, "go back");
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
    String id = MessageUI.readIdNumber(scanner, "Booking number", "BK", 4);
    return MessageUI.isCancelled(id) ? null : id;
  }

  public String inputConfirmationNumber() {
    String number = MessageUI.readIdNumber(scanner, "8-digit confirmation number", "", 8);
    return MessageUI.isCancelled(number) ? null : number;
  }

  public String inputRoomNo() {
    String roomNo = MessageUI.readIdNumber(scanner, "Room number", "", 4);
    return MessageUI.isCancelled(roomNo) ? null : roomNo;
  }

  public String inputNewRoomNo() {
    String roomNo = MessageUI.readIdNumber(scanner, "New room number", "", 4);
    return MessageUI.isCancelled(roomNo) ? null : roomNo;
  }

  public int inputFloorNumber() {
    int floor = MessageUI.readInt(scanner, "Floor number", 1, 20);
    return (floor == MessageUI.CANCELLED_INT) ? -1 : floor;
  }

  public String inputInvoiceId() {
    String id = MessageUI.readIdNumber(scanner, "Invoice number", "INV", 4);
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
    MessageUI.displayTableHeading(String.format("  %-5s %-6s %-18s %5s %12s  %s",
        "NO", "TYPE", "NAME", "MAX", "RATE/NIGHT", "DESCRIPTION"));

    // The number in the first column is what the officer types, so the table
    // and the prompt below it are asking for the same thing.
    for (int i = 1; i <= types.getNumberOfEntries(); i++) {
      RoomType type = types.getEntry(i);
      System.out.printf("  [%d]   %-6s %-18s %5d %12.2f  %s%n",
          i, type.getTypeId(), type.getTypeName(), type.getMaxOccupancy(),
          type.getBaseRatePerNight(), type.getDescription());
    }
    MessageUI.displayThinRule();

    int picked = MessageUI.readInt(scanner, "Room type number", 1,
        types.getNumberOfEntries());
    return (picked == MessageUI.CANCELLED_INT) ? null : types.getEntry(picked).getTypeId();
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

  /**
   * Asks whether to run the same lookup again with a different value.
   *
   * Sits under a result that is already on screen, so answering no is what
   * ends the action - these screens no longer finish with a pause, because
   * the question doubles as the way out.
   *
   * @param question what running it again would do
   * @return true to go round again
   */
  /**
   * Asks for a row number from the listing just shown.
   *
   * Re-prompts until the number names a row that is actually there, so a typo
   * costs one line rather than ending the action.
   *
   * @param size how many rows the listing had
   * @param prompt what the number is being asked for
   * @return the 1-based row number, or -1 if the user typed 0 to quit
   */
  public int inputListPosition(int size, String prompt) {
    while (true) {
      System.out.printf("  %s (1-%d, 0 to quit): ", prompt, size);
      String input = MessageUI.readLine(scanner);

      if (MessageUI.isCancelKey(input)) {
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

  public boolean confirmAnother(String question) {
    MessageUI.displayBlankLine();
    return MessageUI.confirm(scanner, question);
  }

  /**
   * Pauses under a caller-chosen wording.
   *
   * @param prompt what to tell the user, without its trailing dots
   */
  public void pause(String prompt) {
    MessageUI.pause(scanner, prompt);
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

  public void displayField(String label, String value) {
    MessageUI.displayField(label, value);
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
    MessageUI.displayField("Confirmation no.", booking.getConfirmationNumber());
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

    int total = bookings.getNumberOfEntries();
    int totalPages = MessageUI.pageCount(total);

    for (int page = 1; page <= totalPages; page++) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format(
          "  %-4s %-7s %-20s %-6s %-6s %-11s %-11s %-12s %s",
          "NO", "BOOKING", "GUEST", "TYPE", "ROOM", "CHECK IN", "CHECK OUT",
          "STATUS", "PRI"));

      // NO counts from the top of the whole listing, not the page, because it
      // is the number the officer types to act on a booking.
      int from = MessageUI.firstRowOnPage(page);
      int upTo = MessageUI.lastRowOnPage(page, total);

      for (int i = from; i <= upTo; i++) {
        Booking booking = bookings.getEntry(i);
        Guest guest = data.findGuest(booking.getGuestId());

        System.out.printf("  %-4d %-7s %-20s %-6s %-6s %-11s %-11s %-12s %s%n",
            i,
            booking.getBookingId(),
            guest == null ? "-" : truncate(guest.getFullName(), 20),
            booking.getTypeId(),
            booking.getRoomNo() == null ? "-" : booking.getRoomNo(),
            booking.getCheckInDate(), booking.getCheckOutDate(),
            booking.getBookingStatus(),
            booking.isUrgent() ? "URG" : "-");
      }

      MessageUI.displayThinRule();
      System.out.printf("  %d booking(s).%n", total);

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
  public boolean displayRoomList(ListInterface<Room> rooms, ResortData data) {
    if (rooms.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  There are no rooms on record.");
      return false;
    }

    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format(
        "  %-4s %-6s %-6s %-5s %-10s %-22s %s",
        "NO", "ROOM", "TYPE", "FLOOR", "OCCUPANCY", "HOUSEKEEPING", "SERVICE"));

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      System.out.printf(
          "  %-4d %-6s %-6s %-5d %-10s %-22s %s%n",
          i, room.getRoomNo(), room.getTypeId(), room.getFloorNo(),
          room.getOccupancyStatus(), room.getHousekeepingStatus(),
          room.isOutOfService() ? "OUT OF SERVICE" : "IN SERVICE");
    }
    MessageUI.displayThinRule();
    System.out.printf("  %d room(s).%n", rooms.getNumberOfEntries());
    return true;
  }

  public void displayRoom(Room room, ResortData data) {
    RoomType type = data.findRoomType(room.getTypeId());
    MessageUI.displayBlankLine();
    MessageUI.displayField("Room number", room.getRoomNo());
    MessageUI.displayField("Type", room.getTypeId()
        + (type == null ? "" : " - " + type.getTypeName()));
    MessageUI.displayField("Floor", String.valueOf(room.getFloorNo()));
    MessageUI.displayField("Occupancy", room.getOccupancyStatus());
    MessageUI.displayField("Housekeeping", room.getHousekeepingStatus());
    String sellable;
    if (room.isOutOfService()) {
      sellable = "No - out of service";
    } else if (!Room.VACANT.equals(room.getOccupancyStatus())) {
      sellable = "No - " + room.getOccupancyStatus().toLowerCase();
    } else if (!Room.READY_FOR_CHECK_IN.equals(room.getHousekeepingStatus())) {
      sellable = "No - not ready";
    } else {
      sellable = "Yes";
    }
    MessageUI.displayField("Can be sold now", sellable);
  }

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

  /**
   * Shows a stay whose check-out date was worked out from the nights, so the
   * dates are on screen at the moment the booking is accepted.
   */
  public void displayCalculatedStay(java.time.LocalDate checkIn,
      java.time.LocalDate checkOut, int nights) {
    java.time.format.DateTimeFormatter dayFormat =
        java.time.format.DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy");

    MessageUI.displayBlankLine();
    MessageUI.displayField("Number of nights", nights + " night(s)");
    MessageUI.displayField("Check-in", checkIn.format(dayFormat));
    MessageUI.displayField("Check-out", checkOut.format(dayFormat));
    MessageUI.displayBlankLine();
  }

  /**
   * Shows what a checked-out guest's loyalty account looks like right now.
   *
   * Looked up by guest ID against the shared member list rather than passed
   * in ready-made, so this always reflects the record loyalty itself would
   * show - the same balance, the same tier - not a snapshot the front desk
   * happened to be carrying.
   *
   * @param guestId whose account to show
   * @param data the shared registry, used to find the guest and their member
   * record
   */
  public void displayLoyaltyOutcome(String guestId, ResortData data) {
    Member member = data.findMemberByGuest(guestId);
    if (member == null) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  This guest is not enrolled in Loyalty & Rewards.");
      return;
    }

    Guest guest = data.findGuest(guestId);

    MessageUI.displaySectionHeading("Loyalty & Rewards");
    MessageUI.displayField("Member", (guest == null ? "-" : guest.getFullName())
        + " (" + member.getMemberId() + ")");
    MessageUI.displayField("Tier", member.getTier());
    MessageUI.displayField("Points balance", String.valueOf(member.getPointsBalance()));
    MessageUI.displayField("Lifetime points", String.valueOf(member.getLifetimePoints()));

    if (member.getNextTier() != null) {
      MessageUI.displayField("Points to " + member.getNextTier(),
          String.valueOf(member.getPointsToNextTier()));
    } else {
      MessageUI.displayField("Tier standing", "Highest tier reached");
    }
  }

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
    MessageUI.endLongOutput(scanner);
  }
}