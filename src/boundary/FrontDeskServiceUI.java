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
import entity.WalkInRegistration;
import entity.Redemption;
import entity.Reward;
import entity.RoomArrangement;
import java.time.LocalDate;
import java.util.function.Function;
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
          "Rooms (availability, assign, move, manage)",
          "Stay (check in, check out)",
          "Billing (payments, loyalty rewards)",
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
          "Mark a booking as no-show",
          "Booking records (view receipts)"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 6, "go back");
  }

  public int getRoomMenuChoice() {
    MessageUI.displayMenuScreen("ROOMS", null,
        "Main Menu  >  Front-Desk Service  >  Rooms",
        new String[] {
          "Check room availability",
          "Assign a room to a pending booking",
          "Move a booking to another room",
          "Room status board",
          "Manage rooms"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 5, "go back");
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
          "Request a loyalty reward for a booking"
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

  /**
   * Asks for a booking ID, showing a list of movable bookings first.
   *
   * Only bookings that already have a room assigned (CONFIRMED or CHECKED_IN)
   * can be moved. This method displays them in a table so the officer can
   * see which booking to pick without having to remember the ID.
   *
   * @param bookings the list of bookings that can be moved
   * @param data the resort data for looking up guest names
   * @return the chosen booking ID, or null if cancelled
   */
  public String inputBookingId(ListInterface<Booking> bookings, ResortData data) {
    if (bookings == null || bookings.isEmpty()) {
      displayMessage("");
      displayMessage("  No booking can be moved.");
      displayMessage("  Only a CONFIRMED or CHECKED_IN booking with a room");
      displayMessage("  can be moved to another room.");
      return null;
    }

    displaySectionHeading("Bookings that can be moved");
    displayTableHeading(String.format(
        "  %-5s %-7s %-20s %-16s %-6s %-6s %-11s %-11s %s",
        "NO", "BOOKING", "GUEST", "IC / PASSPORT", "TYPE", "ROOM",
        "CHECK IN", "CHECK OUT", "STATUS"));

    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking booking = bookings.getEntry(i);
      Guest guest = data.findGuest(booking.getGuestId());

      System.out.printf("  [%d]   %-7s %-20s %-16s %-6s %-6s %-11s %-11s %s%n",
          i,
          booking.getBookingId(),
          guest == null ? "-" : truncate(guest.getFullName(), 20),
          guest == null ? "-" : truncate(guest.getIcPassportNo(), 16),
          booking.getTypeId(),
          booking.getRoomNo() == null ? "-" : booking.getRoomNo(),
          booking.getCheckInDate(),
          booking.getCheckOutDate(),
          booking.getBookingStatus());
    }

    displayThinRule();
    System.out.printf("  %d booking(s) with rooms assigned.%n",
        bookings.getNumberOfEntries());
    displayMessage("");
    displayMessage("  Enter the number of the booking to move, or 0 to cancel.");

    int position = inputListPosition(bookings.getNumberOfEntries(),
        "Number of the booking to move");
    if (position < 0) {
      return null;
    }
    return bookings.getEntry(position).getBookingId();
  }

  public String inputConfirmationNumber() {
    String code = MessageUI.readCode(scanner, "Confirmation number", 8);
    return MessageUI.isCancelled(code) ? null : code;
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

  /**
   * Asks how many nights the stay runs for.
   *
   * @return the number of nights, or -1 if cancelled
   */
  public int inputNights() {
    int nights = MessageUI.readInt(scanner, "Number of nights", 1, 30);
    return (nights == MessageUI.CANCELLED_INT) ? -1 : nights;
  }

  public double inputAmount(String prompt) {
    return MessageUI.readAmount(scanner, prompt);
  }

  /**
   * Asks for an amount under wording the caller chooses.
   *
   * @param prompt what the amount is for
   * @param note what to put in brackets, so a screen that has already said
   *     what 0 does need not repeat it
   * @return the amount, or MessageUI.CANCELLED_AMOUNT if cancelled
   */
  public double inputAmount(String prompt, String note) {
    return MessageUI.readAmount(scanner, prompt, note);
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

  /**
   * Offers the ways a party could be housed when the type they asked for is
   * gone, and lets the officer pick one.
   *
   * The capacity and the nightly price are both shown, because the guest is
   * being asked to accept something other than what they came for and needs to
   * see what it costs before they agree to it.
   *
   * @param options the arrangements that would house the party
   * @param guests how many people are staying
   * @param typeOf looks up a room's type, for its name
   * @return the chosen arrangement, or null to cancel the booking instead
   */
  public RoomArrangement chooseArrangement(ListInterface<RoomArrangement> options,
      int guests, Function<Room, RoomType> typeOf) {
    MessageUI.displaySectionHeading("Other ways to house " + guests + " guest(s)");
    MessageUI.displayTableHeading(String.format("  %-5s %-34s %-20s %5s %12s",
        "NO", "ARRANGEMENT", "ROOMS", "SLEEPS", "RATE/NIGHT"));

    for (int i = 1; i <= options.getNumberOfEntries(); i++) {
      RoomArrangement option = options.getEntry(i);
      System.out.printf("  [%d]   %-34s %-20s %5d %12.2f%n",
          i, truncate(option.describe(typeOf), 34), truncate(option.roomNumbers(), 20),
          option.getTotalCapacity(), option.getTotalRatePerNight());
    }
    MessageUI.displayThinRule();
    MessageUI.displayMessage("  Each room becomes its own booking under the same guest.");
    MessageUI.displayMessage("  Enter 0 to cancel the booking instead.");

    int picked = MessageUI.readInt(scanner, "Arrangement number", 1,
        options.getNumberOfEntries());
    return (picked == MessageUI.CANCELLED_INT) ? null : options.getEntry(picked);
  }

  /**
   * Lists the bookings that have a bill, as the records to pick a receipt from.
   *
   * The NO in the first column is what the officer types to open a receipt, so
   * the table and the prompt under it are asking for the same thing.
   *
   * @param invoices one bill per row, in the order they are numbered
   * @param data used to reach each bill's booking and guest
   * @return true if anything was listed
   */
  public boolean displayBookingRecords(ListInterface<Invoice> invoices, ResortData data) {
    if (invoices.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  There are no booking records yet.");
      return false;
    }

    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format(
        "  %-5s %-9s %-20s %-16s %-11s %-11s %12s  %s",
        "NO", "INVOICE", "GUEST", "IC / PASSPORT", "CHECK IN", "CHECK OUT",
        "BILL (RM)", "STATUS"));

    for (int i = 1; i <= invoices.getNumberOfEntries(); i++) {
      Invoice invoice = invoices.getEntry(i);
      Booking booking = data.findBooking(invoice.getBookingId());
      Guest guest = (booking == null) ? null : data.findGuest(booking.getGuestId());

      System.out.printf("  [%d]   %-9s %-20s %-16s %-11s %-11s %12.2f  %s%n",
          i,
          invoice.getInvoiceId(),
          guest == null ? "-" : truncate(guest.getFullName(), 20),
          guest == null ? "-" : truncate(guest.getIcPassportNo(), 16),
          booking == null ? "-" : booking.getCheckInDate().toString(),
          booking == null ? "-" : booking.getCheckOutDate().toString(),
          invoice.getTotalAmount(),
          invoice.getPaymentStatus());
    }

    MessageUI.displayThinRule();
    System.out.printf("  %d record(s).%n", invoices.getNumberOfEntries());
    return true;
  }

  /**
   * Lists the bookings that may still be amended, and takes the one picked.
   *
   * The number in the first column is what the officer types, so the table and
   * the prompt below it ask for the same thing. Long lists are paged, and the
   * row numbers count from the top of the whole listing rather than the page,
   * so a booking keeps the same number whichever page it is read from.
   *
   * @param bookings the bookings that can be changed, earliest arrival first
   * @param data used to reach each booking's guest
   * @return the chosen booking, or null if the officer quit
   */
  public Booking chooseBookingToAmend(ListInterface<Booking> bookings, ResortData data) {
    return chooseBooking(bookings, data, "Number of the booking to edit", new String[] {
      "No booking can be amended.",
      "Only a stay that has not started yet can be changed,",
      "so a booking arriving today or earlier is left alone."
    });
  }

  /**
   * Lists bookings with a pickable number in the first column, and returns the
   * one chosen.
   *
   * @param bookings the bookings to offer
   * @param data used to reach each booking's guest
   * @param prompt what the number is being asked for
   * @param emptyMessage what to say, line by line, when there is nothing to show
   * @return the chosen booking, or null if the officer quit
   */
  public Booking chooseBooking(ListInterface<Booking> bookings, ResortData data,
      String prompt, String[] emptyMessage) {
    if (bookings.isEmpty()) {
      MessageUI.displayBlankLine();
      for (String line : emptyMessage) {
        MessageUI.displayMessage("  " + line);
      }
      return null;
    }

    int total = bookings.getNumberOfEntries();
    int totalPages = MessageUI.pageCount(total);
    int page = 1;

    while (true) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format(
          "  %-5s %-8s %-20s %-16s %-6s %-6s %-11s %-11s %s",
          "NO", "BOOKING", "GUEST", "IC / PASSPORT", "TYPE", "ROOM",
          "CHECK IN", "CHECK OUT", "STATUS"));

      int from = MessageUI.firstRowOnPage(page);
      int upTo = MessageUI.lastRowOnPage(page, total);

      for (int i = from; i <= upTo; i++) {
        Booking booking = bookings.getEntry(i);
        Guest guest = data.findGuest(booking.getGuestId());

        System.out.printf("  [%d]   %-8s %-20s %-16s %-6s %-6s %-11s %-11s %s%n",
            i,
            booking.getBookingId(),
            guest == null ? "-" : truncate(guest.getFullName(), 20),
            guest == null ? "-" : truncate(guest.getIcPassportNo(), 16),
            booking.getTypeId(),
            booking.getRoomNo() == null ? "-" : booking.getRoomNo(),
            booking.getCheckInDate(), booking.getCheckOutDate(),
            booking.getBookingStatus());
      }

      MessageUI.displayThinRule();

      if (totalPages == 1) {
        System.out.printf("  %d booking(s).%n", total);
        int picked = MessageUI.readInt(scanner, prompt, 1, total);
        return (picked == MessageUI.CANCELLED_INT) ? null : bookings.getEntry(picked);
      }

      System.out.printf("  Showing %d-%d of %d booking(s).%n", from, upTo, total);

      // One prompt does both jobs. A row number is what the officer is really
      // after, so a bare number always means a booking; paging is a letter, so
      // the two can never be mistaken for each other.
      while (true) {
        System.out.printf("  %s (1-%d), [N]ext page, [P]revious, 0 to go back: ",
            prompt, total);
        String input = MessageUI.readLine(scanner, "0").trim().toLowerCase();

        if (input.equals("0") || input.equals("q")) {
          return null;
        }
        if (input.isEmpty() || input.equals("n")) {
          page = (page >= totalPages) ? 1 : page + 1;
          break;
        }
        if (input.equals("p")) {
          page = (page <= 1) ? totalPages : page - 1;
          break;
        }

        try {
          int picked = Integer.parseInt(input);
          if (picked < 1 || picked > total) {
            MessageUI.displayError("There is no booking " + picked
                + " in that list. Enter 1 to " + total + ".");
            continue;
          }
          return bookings.getEntry(picked);
        } catch (NumberFormatException notANumber) {
          MessageUI.displayError("Enter a booking number, N, P, or 0 to go back.");
        }
      }
    }
  }

  /**
   * Asks which part of a booking is being changed.
   *
   * @return the chosen option, or 0 to finish with this booking
   */
  public int getAmendFieldChoice(Booking booking) {
    MessageUI.displayMenuScreen("EDIT BOOKING " + booking.getBookingId(), null,
        "Main Menu  >  Front-Desk Service  >  Bookings  >  Edit",
        new String[] {
          "Check-in date",
          "Number of nights (sets the check-out date)",
          "Number of guests",
          "Room type"
        },
        "Done - go back");
    return MessageUI.readMenuChoice(scanner, 4, "finish editing");
  }

  /**
   * Shows what a booking is about to become, next to what it is now.
   *
   * @param label what is changing
   * @param before its current value
   * @param after what it would become
   */
  public void displayProposedChange(String label, String before, String after) {
    MessageUI.displaySectionHeading("Confirm the change");
    MessageUI.displayField(label + " now", before);
    MessageUI.displayField(label + " after", after);
  }

  /**
   * Lists rooms that are unavailable only because they are already booked.
   *
   * The guest holding each room is deliberately not named: the officer needs
   * to know the room is spoken for and when it frees up, not who has it. The
   * dates shown are the clashing stay's, so it is clear how long the wait is.
   *
   * @param rooms the rooms already taken
   * @param data used to find the stay occupying each one
   * @param checkIn the first night that was asked for
   * @param checkOut the morning the guest would have left
   */
  public void displayTakenRooms(ListInterface<Room> rooms, ResortData data,
      LocalDate checkIn, LocalDate checkOut) {
    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format("  %-6s %-18s %-10s %-25s %s",
        "ROOM", "TYPE", "STATUS", "OCCUPIED", "FREE FROM"));

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      RoomType type = data.findRoomType(room.getTypeId());

      // The stay standing in the way - the one whose dates overlap what was
      // asked for. Its guest is not shown, only when the room comes back.
      Booking clashing = null;
      ListInterface<Booking> bookings = data.getBookingList();
      for (int b = 1; b <= bookings.getNumberOfEntries(); b++) {
        Booking booking = bookings.getEntry(b);
        if (!room.getRoomNo().equals(booking.getRoomNo())) {
          continue;
        }
        if (Booking.STATUS_CANCELLED.equals(booking.getBookingStatus())
            || Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus())
            || Booking.STATUS_NO_SHOW.equals(booking.getBookingStatus())) {
          continue;
        }
        if (booking.getCheckInDate().isBefore(checkOut)
            && checkIn.isBefore(booking.getCheckOutDate())) {
          clashing = booking;
          break;
        }
      }

      System.out.printf("  %-6s %-18s %-10s %-25s %s%n",
          room.getRoomNo(),
          truncate(type == null ? room.getTypeId() : type.getTypeName(), 18),
          room.getOccupancyStatus(),
          clashing == null ? "-"
              : clashing.getCheckInDate() + " to " + clashing.getCheckOutDate(),
          clashing == null ? "-" : clashing.getCheckOutDate().toString());
    }

    MessageUI.displayThinRule();
    MessageUI.displayMessage("  These rooms are taken for the dates asked for.");
  }

  /**
   * Asks how soon a room just vacated is wanted back.
   *
   * The officer at the desk is the only one who knows whether somebody is
   * waiting for this room, so the lane is taken from them rather than worked
   * out from the booking records.
   *
   * @param roomNo the room being handed to housekeeping
   * @return 1 for urgent, 2 for the normal round, or -1 if cancelled
   */
  public int inputCleaningUrgency(String roomNo) {
    MessageUI.displaySectionHeading("Housekeeping");
    MessageUI.displayMessage("  Room " + roomNo + " goes to housekeeping to be cleaned.");
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("   [1]  Urgent - a guest is waiting for this room");
    MessageUI.displayMessage("   [2]  Normal - clean it in the usual order");
    MessageUI.displayThinRule();

    int choice = MessageUI.readInt(scanner, "How soon is it needed", 1, 2);
    return (choice == MessageUI.CANCELLED_INT) ? -1 : choice;
  }

  /**
   * Lists the guests standing at the counter, and takes the one picked.
   *
   * Only registrations already called through from the walk-in queue appear:
   * a booking is made from one of these or from nothing at all. The IC is
   * shown against each so the officer can match the person in front of them
   * to the row without having to type the document out.
   *
   * @param served the registrations currently IN_SERVICE
   * @param data used to reach each registration's guest
   * @return the chosen registration, or null if the officer quit
   */
  public WalkInRegistration chooseServedRegistration(
      ListInterface<WalkInRegistration> served, ResortData data) {
    if (served.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  Nobody is at the counter waiting to be booked.");
      MessageUI.displayMessage("  Call a guest from Walk-In Registration first:");
      MessageUI.displayMessage("  Walk-In Registration > Queue > Serve next guest.");
      return null;
    }

    // How many are waiting, split by lane, so the officer can see at a glance
    // whether anybody urgent is still unserved.
    int urgent = 0;
    for (int i = 1; i <= served.getNumberOfEntries(); i++) {
      if (served.getEntry(i).isUrgent()) {
        urgent++;
      }
    }
    int normal = served.getNumberOfEntries() - urgent;

    int total = served.getNumberOfEntries();
    int totalPages = MessageUI.pageCount(total);
    int page = 1;

    while (true) {
      MessageUI.displayBlankLine();
      System.out.printf("  Urgent: %d     Normal: %d     Waiting to be booked: %d%n",
          urgent, normal, total);
      MessageUI.displayBlankLine();

      MessageUI.displayTableHeading(String.format(
          "  %-5s %-7s %-16s %-15s %-8s %-6s %-11s %s",
          "NO", "REG ID", "IC / PASSPORT", "REQUESTED", "PRIORITY", "NIGHTS",
          "CHECK IN", "STATUS"));

      int from = MessageUI.firstRowOnPage(page);
      int upTo = MessageUI.lastRowOnPage(page, total);

      for (int i = from; i <= upTo; i++) {
        WalkInRegistration reg = served.getEntry(i);
        Guest guest = data.findGuest(reg.getGuestId());
        RoomType type = data.findRoomType(reg.getRequestedTypeId());

        System.out.printf("  [%d]   %-7s %-16s %-15s %-8s %-6d %-11s %s%n",
            i,
            reg.getRegId(),
            guest == null ? "-" : truncate(guest.getIcPassportNo(), 16),
            truncate(type == null ? reg.getRequestedTypeId() : type.getTypeName(), 15),
            reg.getPriority(),
            reg.getRequestedNights(),
            reg.getRequestedCheckInDate() == null ? "-"
                : reg.getRequestedCheckInDate().toString(),
            reg.getStatus());
      }

      MessageUI.displayThinRule();
      MessageUI.displayMessage("  Urgent guests are listed first - work down from"
          + " the top.");

      // A short list needs no paging, so the number is asked for outright.
      if (totalPages == 1) {
        int picked = MessageUI.readInt(scanner, "Number of the guest to book",
            1, total);
        return (picked == MessageUI.CANCELLED_INT) ? null : served.getEntry(picked);
      }

      System.out.printf("  Showing %d-%d of %d.%n", from, upTo, total);

      // S picks a guest, so a bare number can mean the page without ambiguity.
      Integer chosen = readPagedSelection(page, totalPages, total, "guest");
      if (chosen == null) {
        return null;
      }
      if (chosen > 0) {
        return served.getEntry(chosen);
      }
      page = -chosen;
    }
  }

  /**
   * Reads a paging command or a selection from one prompt.
   *
   * Paging and picking share a screen, so they have to share a prompt without
   * being mistaken for each other: a bare number moves between pages, and S
   * starts a selection. That way "3" never accidentally books guest three when
   * the officer meant page three.
   *
   * @param page the page on screen now
   * @param totalPages how many there are
   * @param total how many rows in the whole listing
   * @param noun what is being selected, for the wording
   * @return a positive row number to select, a negated page number to move to,
   *     or null to go back
   */
  private Integer readPagedSelection(int page, int totalPages, int total, String noun) {
    while (true) {
      System.out.printf("  Page %d of %d - [N]ext, [P]revious, [1-%d] jump,"
          + " [S]elect, 0 to go back: ", page, totalPages, totalPages);
      String input = MessageUI.readLine(scanner, "0").trim().toLowerCase();

      if (input.equals("0") || input.equals("q")) {
        return null;
      }
      if (input.isEmpty() || input.equals("n")) {
        return -((page >= totalPages) ? 1 : page + 1);
      }
      if (input.equals("p")) {
        return -((page <= 1) ? totalPages : page - 1);
      }
      if (input.equals("s")) {
        int picked = MessageUI.readInt(scanner,
            "Number of the " + noun + " to book", 1, total);
        if (picked == MessageUI.CANCELLED_INT) {
          continue;
        }
        return picked;
      }

      try {
        int wanted = Integer.parseInt(input);
        if (wanted >= 1 && wanted <= totalPages) {
          return -wanted;
        }
        MessageUI.displayError("There is no page " + wanted + ". Enter 1 to "
            + totalPages + ", or S to select.");
      } catch (NumberFormatException notANumber) {
        MessageUI.displayError("Enter N, P, a page number, S to select,"
            + " or 0 to go back.");
      }
    }
  }

  /**
   * Shows the booking a reward is being asked for, so the officer can check
   * they have the right guest before anything is submitted.
   *
   * @param booking the stay found by its confirmation number
   * @param guest whose stay it is
   * @param data used to name the room type
   */
  public void displayBookingForRedemption(Booking booking, Guest guest, ResortData data) {
    RoomType type = data.findRoomType(booking.getTypeId());

    MessageUI.displaySectionHeading("Booking " + booking.getConfirmationNumber());
    MessageUI.displayField("Guest", guest.getFullName());
    MessageUI.displayField("IC / Passport", guest.getIcPassportNo());
    MessageUI.displayField("Contact", guest.getContactNumber());
    MessageUI.displayField("Booking", booking.getBookingId());
    MessageUI.displayField("Room type",
        (type == null ? booking.getTypeId() : type.getTypeName()));
    MessageUI.displayField("Room", booking.getRoomNo() == null
        ? "not assigned yet" : booking.getRoomNo());
    MessageUI.displayField("Check-in", booking.getCheckInDate().toString());
    MessageUI.displayField("Check-out", booking.getCheckOutDate().toString());
    MessageUI.displayField("Nights", booking.getNumberOfNights() + " night(s)");
    MessageUI.displayField("Guests", String.valueOf(booking.getNumberOfGuests()));
    MessageUI.displayField("Status", booking.getBookingStatus());
  }

  /**
   * The banner above the reward list: who is redeeming, against which stay,
   * and what they have to spend.
   *
   * @param booking the stay the reward belongs to
   * @param guest whose stay it is
   * @param member their loyalty record
   */
  public void displayRedemptionHeader(Booking booking, Guest guest, Member member) {
    MessageUI.displayBlankLine();
    MessageUI.displayField("Member", guest.getFullName()
        + "  (" + member.getMemberId() + ")");
    MessageUI.displayField("IC / Passport", guest.getIcPassportNo());
    MessageUI.displayField("Confirmation no.", booking.getConfirmationNumber());
    MessageUI.displayField("Stay", booking.getCheckInDate() + " to "
        + booking.getCheckOutDate() + "  (" + booking.getNumberOfNights()
        + " night(s))");
    MessageUI.displayField("Tier", member.getTier());
    MessageUI.displayField("Points available",
        String.valueOf(member.getPointsBalance()));
  }

  /**
   * Offers the reward catalogue and takes the one picked.
   *
   * What the member cannot have is still listed, with the reason against it,
   * so the officer can tell the guest why rather than simply not seeing it.
   *
   * @param rewards the catalogue
   * @param member who is redeeming
   * @return the chosen reward, or null if the officer went back
   */
  public Reward chooseRewardForMember(ListInterface<Reward> rewards, Member member) {
    if (rewards.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  There are no rewards in the catalogue.");
      return null;
    }

    MessageUI.displaySectionHeading("Rewards");
    MessageUI.displayTableHeading(String.format("  %-5s %-28s %-10s %8s %10s  %s",
        "NO", "REWARD", "CATEGORY", "POINTS", "VALUE", "AVAILABLE?"));

    for (int i = 1; i <= rewards.getNumberOfEntries(); i++) {
      Reward reward = rewards.getEntry(i);
      System.out.printf("  [%d]   %-28s %-10s %8d %10.2f  %s%n",
          i, truncate(reward.getRewardName(), 28),
          truncate(reward.getCategory(), 10), reward.getPointsRequired(),
          reward.getCashValue(), availabilityFor(reward, member));
    }

    MessageUI.displayThinRule();
    MessageUI.displayMessage("  The request goes to Loyalty for approval."
        + " No points are taken yet.");

    int picked = MessageUI.readInt(scanner, "Reward number", 1,
        rewards.getNumberOfEntries());
    return (picked == MessageUI.CANCELLED_INT) ? null : rewards.getEntry(picked);
  }

  /** Why a member can or cannot have a reward, in a few words. */
  private String availabilityFor(Reward reward, Member member) {
    if (!reward.isActive()) {
      return "No - withdrawn";
    }
    if (reward.getStockQuantity() <= 0) {
      return "No - out of stock";
    }
    if (member.getPointsBalance() < reward.getPointsRequired()) {
      return "No - needs "
          + (reward.getPointsRequired() - member.getPointsBalance()) + " more";
    }
    if (Member.tierRank(member.getTier()) < Member.tierRank(reward.getMinimumTier())) {
      return "No - " + reward.getMinimumTier() + " only";
    }
    return "Yes";
  }

  /** Wipes the screen, so nothing of the last action is left behind. */
  public void clearScreen() {
    MessageUI.clearScreen();
  }

  /**
   * The receipt handed to the guest once their stay is paid for.
   *
   * Printed as a document rather than as another status line: it is what the
   * guest takes away, so it names them, the room and the nights, and shows
   * what was tendered and what came back as change.
   *
   * @param booking the booking now paid for
   * @param invoice its settled bill
   * @param payments every payment taken against it
   * @param guestName who is paying
   * @param changeDue anything handed back
   */
  public void displayReceipt(Booking booking, Invoice invoice,
      ListInterface<Payment> payments, String guestName, double changeDue,
      ResortData data) {
    MessageUI.displayBlankLine();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("TARUMT RESORT MANAGEMENT SYSTEM");
    MessageUI.displayBoxCentred("OFFICIAL RECEIPT");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  Receipt for : " + invoice.getInvoiceId());
    MessageUI.displayBoxLine("  Issued      : " + java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")));
    MessageUI.displayBoxBottom();

    MessageUI.displayBlankLine();
    MessageUI.displayField("Guest", guestName);
    MessageUI.displayField("Booking", booking.getBookingId()
        + "   (confirmation " + booking.getConfirmationNumber() + ")");
    MessageUI.displayField("Room", booking.getRoomNo() == null
        ? "-" : booking.getRoomNo());
    MessageUI.displayField("Stay", booking.getCheckInDate() + " to "
        + booking.getCheckOutDate() + "  (" + booking.getNumberOfNights()
        + " night(s))");

    MessageUI.displaySectionHeading("Charges");
    System.out.printf("  %-28s   RM%10.2f%n", "Room charge", invoice.getRoomCharge());
    System.out.printf("  %-28s   RM%10.2f%n", "Service charge (10%)",
        invoice.getServiceCharge());
    System.out.printf("  %-28s   RM%10.2f%n", "SST (6%)", invoice.getTaxAmount());
    MessageUI.displayThinRule();
    System.out.printf("  %-28s   RM%10.2f%n", "TOTAL", invoice.getTotalAmount());

    if (payments != null && !payments.isEmpty()) {
      MessageUI.displaySectionHeading("Paid by");
      for (int i = 1; i <= payments.getNumberOfEntries(); i++) {
        Payment payment = payments.getEntry(i);
        System.out.printf("  %-28s   RM%10.2f%s%n",
            payment.getMethod(), payment.getAmount(),
            (payment.getReference() == null || payment.getReference().isBlank())
                ? "" : "   ref " + payment.getReference());
      }
    }

    MessageUI.displayThinRule();

    // The guest's own arithmetic: what they handed over, what the stay cost,
    // and what came back. Without the tendered line the change appears from
    // nowhere, because the invoice only ever records the amount owed.
    if (changeDue > 0) {
      System.out.printf("  %-28s   RM%10.2f%n", "CASH TENDERED",
          invoice.getAmountPaid() + changeDue);
      System.out.printf("  %-28s  -RM%10.2f%n", "Less: total bill",
          invoice.getAmountPaid());
      MessageUI.displayThinRule();
      System.out.printf("  %-28s   RM%10.2f%n", "CHANGE GIVEN", changeDue);
      MessageUI.displayBlankLine();
    }

    System.out.printf("  %-28s   RM%10.2f%n", "AMOUNT PAID", invoice.getAmountPaid());
    System.out.printf("  %-28s   %s%n", "Status", invoice.getPaymentStatus());

    displayLoyaltyOnReceipt(booking, data);

    MessageUI.displayBlankLine();
    MessageUI.displayRule();
    MessageUI.displayMessage("  Thank you for staying with TARUMT Resort.");
    MessageUI.displayRule();
  }

  /**
   * The loyalty lines on a receipt: what the guest asked for and where it got to.
   *
   * A reward is requested at the desk but granted by Loyalty, so the receipt
   * has to say which of the two has happened - a guest holding a slip that
   * says nothing cannot tell whether their spa session is booked or still
   * waiting on somebody else.
   *
   * @param booking the stay this receipt covers
   * @param data used to reach the redemptions, rewards and membership
   */
  private void displayLoyaltyOnReceipt(Booking booking, ResortData data) {
    Member member = data.findMemberByGuest(booking.getGuestId());
    if (member == null) {
      return;
    }

    ListInterface<Redemption> mine = data.getRedemptionList().filter(
        redemption -> booking.getBookingId().equals(redemption.getBookingId()));
    if (mine.isEmpty()) {
      return;
    }

    MessageUI.displaySectionHeading("Loyalty rewards");

    for (int i = 1; i <= mine.getNumberOfEntries(); i++) {
      Redemption redemption = mine.getEntry(i);
      Reward reward = data.findReward(redemption.getRewardId());
      String name = (reward == null) ? redemption.getRewardId() : reward.getRewardName();

      if (Redemption.APPROVED.equals(redemption.getStatus())) {
        System.out.printf("  %-28s   %s%n", truncate(name, 28), "APPROVED");
        System.out.printf("  %-28s   %d points%n", "  Points spent",
            redemption.getPointsUsed());
        // The confirmation number is what the spa or restaurant asks for, so
        // it belongs on the slip the guest carries to them.
        System.out.printf("  %-28s   %s%n", "  Booking confirmation",
            booking.getConfirmationNumber());
      } else if (Redemption.REJECTED.equals(redemption.getStatus())) {
        System.out.printf("  %-28s   %s%n", truncate(name, 28), "DECLINED");
        if (redemption.getRejectReason() != null) {
          MessageUI.displayMessage("    " + redemption.getRejectReason());
        }
      } else {
        System.out.printf("  %-28s   %s%n", truncate(name, 28),
            "PENDING APPROVAL");
        System.out.printf("  %-28s   %d points%n", "  Points to be spent",
            redemption.getPointsUsed());
        MessageUI.displayMessage("    Awaiting a loyalty officer - no points"
            + " taken yet.");
      }
    }

    MessageUI.displayThinRule();
    System.out.printf("  %-28s   %s (%s)%n", "Member", member.getMemberId(),
        member.getTier());
    System.out.printf("  %-28s   %d points%n", "Points remaining",
        member.getPointsBalance());
  }

  /** Thanks a guest whose booking could not be met, and apologises for it. */
  public void displayBookingCancelled(String guestName) {
    MessageUI.displayBlankLine();
    MessageUI.displayRule();
    MessageUI.displayMessage("  We are sorry, " + guestName + ".");
    MessageUI.displayMessage("");
    MessageUI.displayMessage("  We could not house your party for those dates, and your");
    MessageUI.displayMessage("  booking has been cancelled. Nothing has been charged.");
    MessageUI.displayMessage("");
    MessageUI.displayMessage("  Thank you for considering TARUMT Resort. We hope to");
    MessageUI.displayMessage("  welcome you another time.");
    MessageUI.displayRule();
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

  /**
   * Truncates text to fit in a table column.
   *
   * @param text the text to truncate
   * @param width the maximum width
   * @return the truncated text, or "-" if null
   */
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
      // Shown as progress rather than a bare gap: "1819 / 5000" says how far
      // along they are, where "3181 to go" only says how far is left.
      int needed = member.getLifetimePoints() + member.getPointsToNextTier();
      MessageUI.displayField("Progress to " + member.getNextTier(),
          member.getLifetimePoints() + " / " + needed + "  ("
              + member.getPointsToNextTier() + " more)");
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