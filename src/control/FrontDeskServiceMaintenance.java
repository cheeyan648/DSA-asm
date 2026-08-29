package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.FrontDeskServiceUI;
import entity.Booking;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Invoice;
import entity.Payment;
import entity.Member;
import entity.Redemption;
import entity.Reward;
import entity.Room;
import entity.RoomArrangement;
import entity.RoomAssignment;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import utility.MessageUI;

/**
 * Front-Desk Service - the bookings, the rooms they occupy and the money.
 *
 * This is where a guest becomes a stay. It records the booking, finds a room
 * for it, takes the payments and closes the stay at the end. It is also the
 * module that sets the rest of the system in motion: checking a guest out
 * hands a dirty room to housekeeping and a completed stay to loyalty.
 *
 * A room is only ever given out when both this module and housekeeping agree -
 * free for the dates AND clean. Neither can answer that alone, which is why
 * the check lives in the shared service rather than here.
 *
 * COLLECTION ADTs USED
 *   TreeInterface<String, Booking> - the confirmation tree, held in
 *       ResortData. A guest quotes an eight-character code and the booking
 *       is found by descending the tree rather than scanning every record.
 *   ListInterface<Booking> / <Room> / <Invoice> - the bookings, rooms and
 *       bills. filter() picks what each screen may act on, sort() orders the
 *       listings, and countIf() builds the report tallies.
 *   MapInterface<String, Room> - roomNo to Room, consulted on every
 *       availability check.
 *
 * INTERACTS WITH
 *   Walk-In Registration. Reads registrations that are IN_SERVICE and turns
 *   one into a booking. Receives: regId, guestId, room type, nights, dates.
 *
 *   Housekeeping. At check-out the room is dirtied and a cleaning task is
 *   raised. Passes: roomNo, bookingId and the lane the officer chose
 *   (URGENT or NORMAL).
 *
 *   Loyalty and Rewards. At check-out the settled bill earns points.
 *   Passes: bookingId, guestId and the invoice total. Also submits reward
 *   requests against a booking, which Loyalty approves or rejects.
 *
 * @author Tew Yong Le
 */
public class FrontDeskServiceMaintenance {

  private final FrontDeskServiceUI ui = new FrontDeskServiceUI();
  private final ResortData data;
  private final ResortService service;
  private final String staffId;

  public FrontDeskServiceMaintenance(ResortService service, String staffId) {
    this.service = service;
    this.data = service.getData();
    this.staffId = staffId;
  }

  // ==================================================================
  // MENU
  // ==================================================================

  public void run() {
    int choice;
    do {
      choice = ui.getMenuChoice();
      switch (choice) {
        case 1:
          runBookingMenu();
          break;
        case 2:
          runRoomMenu();
          break;
        case 3:
          runStayMenu();
          break;
        case 4:
          runBillingMenu();
          break;
        case 5:
          runSearchMenu();
          break;
        case 6:
          runReportMenu();
          break;
        default:
          break;
      }
    } while (choice != 0);

    data.saveFrontDesk();
  }

  private void runBookingMenu() {
    int choice;
    do {
      choice = ui.getBookingMenuChoice();
      switch (choice) {
        case 1:
          createBooking();
          break;
        case 2:
          amendBooking();
          break;
        case 3:
          cancelBooking();
          break;
        case 4:
          deleteBooking();
          break;
        case 5:
          markNoShow();
          break;
        case 6:
          displayBookingRecords();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  /**
   * The bookings that have been billed, and their receipts.
   *
   * Only bookings with an invoice appear: a booking with no bill has nothing
   * to show a receipt for. The listing is redrawn after each receipt so
   * several can be looked at without leaving and coming back.
   */
  private void displayBookingRecords() {
    while (true) {
      ui.startAction("BOOKING RECORDS");

      // Newest first, so the booking just taken is at the top of the list.
      ListInterface<Invoice> records = copyOfInvoices(data.getInvoiceList());
      records.sort(java.util.Comparator.comparing(Invoice::getIssuedAt,
          java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));

      if (!ui.displayBookingRecords(records, data)) {
        ui.pause();
        return;
      }

      ui.displayMessage("");
      int position = ui.inputListPosition(records.getNumberOfEntries(),
          "Number of the record to view its receipt");
      if (position < 0) {
        return;
      }

      Invoice invoice = records.getEntry(position);
      Booking booking = data.findBooking(invoice.getBookingId());
      if (booking == null) {
        ui.displayError("That record's booking is missing.");
        ui.pause();
        continue;
      }

      // A receipt reprinted from the records shows no change: whatever was
      // handed back went back at the counter, not now.
      displayReceipt(booking, invoice, 0.0);
    }
  }

  /** A copy, so sorting a listing cannot reorder the stored invoices. */
  private ListInterface<Invoice> copyOfInvoices(ListInterface<Invoice> source) {
    ListInterface<Invoice> copy = new ArrayList<>();
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      copy.add(source.getEntry(i));
    }
    return copy;
  }

  private void runRoomMenu() {
    int choice;
    do {
      choice = ui.getRoomMenuChoice();
      switch (choice) {
        case 1:
          checkAvailability();
          break;
        case 2:
          assignRoomToBooking();
          break;
        case 3:
          moveBookingToAnotherRoom();
          break;
        case 4:
          displayRoomBoard();
          break;
        case 5:
          runRoomManagementMenu();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runRoomManagementMenu() {
    int choice;
    do {
      choice = ui.getRoomManagementMenuChoice();
      switch (choice) {
        case 1:
          displayRoomList();
          break;
        case 2:
          addRoom();
          break;
        case 3:
          removeRoom();
          break;
        case 4:
          changeServiceState(true);
          break;
        case 5:
          changeServiceState(false);
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void displayRoomList() {
    ui.startAction("ALL ROOMS");
    ui.displayRoomList(data.getRoomList(), data);
    ui.pause();
  }

  private void addRoom() {
    ui.startAction("ADD A ROOM");

    // Keep room lookup indexes in sync with the room list before checking
    // whether the new room number already exists.
    data.rebuildIndexes();

    String roomNo = ui.inputNewRoomNo();
    if (roomNo == null) {
      return;
    }

    if (data.findRoom(roomNo) != null) {
      ui.displayError("Room " + roomNo + " already exists.");
      ui.pause();
      return;
    }

    String typeId = ui.inputRoomType(data.getRoomTypeList());
    if (typeId == null) {
      return;
    }

    int floor = ui.inputFloorNumber();
    if (floor < 0) {
      return;
    }

    ServiceResult<Room> added = service.addRoom(roomNo, typeId, floor, staffId);
    if (added.isFailure()) {
      ui.displayError(added.getMessage());
      ui.pause();
      return;
    }

    // addRoom() updates the shared list; rebuild the indexes so findRoom()
    // and the other fast room lookups can see the new room immediately.
    data.rebuildIndexes();

    ui.displaySuccess(added.getMessage());
    ui.displayMessage("  It cannot be sold until it has been cleaned and inspected.");
    ui.pause();
  }

  private void removeRoom() {
    ui.startAction("REMOVE A ROOM");

    // Make sure the room lookup reflects the current room list.
    data.rebuildIndexes();

    if (!ui.displayRoomList(data.getRoomList(), data)) {
      ui.pause();
      return;
    }

    ui.displayMessage("");
    String roomNo;
    Room room;
    while (true) {
      roomNo = ui.inputRoomNo();
      if (roomNo == null) {
        return;
      }
      room = data.findRoom(roomNo);
      if (room != null) {
        break;
      }
      ui.displayError("There is no room " + roomNo
          + ". Enter another number, or 0 to go back.");
    }

    ui.displayRoom(room, data);
    ui.displayMessage("");

    if (!ui.confirm("Remove room " + roomNo + " from the resort?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    ServiceResult<Room> removed = service.removeRoom(roomNo);
    if (removed.isFailure()) {
      ui.displayError(removed.getMessage());
    } else {
      // Keep indexes consistent after the room is removed from the list.
      data.rebuildIndexes();
      ui.displaySuccess(removed.getMessage());
    }
    ui.pause();
  }

  private void changeServiceState(boolean block) {
    ui.startAction(block ? "TAKE A ROOM OUT OF SERVICE" : "RETURN A ROOM TO SERVICE");

    if (!ui.displayRoomList(data.getRoomList(), data)) {
      ui.pause();
      return;
    }

    ui.displayMessage("");
    String roomNo = ui.inputRoomNo();
    if (roomNo == null) {
      return;
    }

    ServiceResult<Room> changed = service.setRoomOutOfService(roomNo, block, staffId);
    if (changed.isFailure()) {
      ui.displayError(changed.getMessage());
    } else {
      ui.displaySuccess(changed.getMessage());
      if (!block) {
        ui.displayMessage("  It cannot be sold until it has been cleaned again.");
      }
    }
    ui.pause();
  }

  private void runStayMenu() {
    int choice;
    do {
      choice = ui.getStayMenuChoice();
      switch (choice) {
        case 1:
          checkIn();
          break;
        case 2:
          checkOut();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runBillingMenu() {
    int choice;
    do {
      choice = ui.getBillingMenuChoice();
      switch (choice) {
        case 1:
          viewInvoice();
          break;
        case 2:
          recordPayment();
          break;
        case 3:
          requestLoyaltyReward();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runSearchMenu() {
    int choice;
    do {
      choice = ui.getSearchMenuChoice();
      switch (choice) {
        case 1:
          searchByConfirmationNumber();
          break;
        case 2:
          searchByBookingId();
          break;
        case 3:
          searchByGuestName();
          break;
        case 4:
          searchByRoom();
          break;
        case 5:
          filterByStatus();
          break;
        case 6:
          displayAllBookings();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runReportMenu() {
    int choice;
    do {
      choice = ui.getReportMenuChoice();
      switch (choice) {
        case 1:
          occupancyRevenueReport();
          break;
        case 2:
          outstandingBalanceReport();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  // ==================================================================
  // BOOKINGS
  // ==================================================================

  /**
   * Turns a guest who has been called to the counter into a booking.
   *
   * A booking is only ever made from a walk-in registration. The IC or passport
   * number is the only thing typed here: everything else - the name, the room
   * type, the nights and the dates - is read back from what the guest already
   * gave at registration, so the same stay cannot be recorded two different
   * ways in the two modules. A guest who is not in the queue is sent to
   * register first rather than being booked from scratch.
   */
  private void createBooking() {
    ui.startAction("CREATE A NEW BOOKING");

    ui.displayMessage("  A booking is made from a walk-in registration.");
    ui.displayMessage("  The guest must have been called to the counter first.");
    ui.displayMessage("");

    // Everyone who has reached the counter and has no booking yet. Listing
    // them beats asking for a document: the officer picks a number rather
    // than copying an IC off a screen they have just left.
    ListInterface<WalkInRegistration> atCounter = data.getRegistrationList().filter(
        reg -> WalkInRegistration.STATUS_IN_SERVICE.equals(reg.getStatus()));

    // Urgent guests first, then whoever was called earliest. The urgency
    // granted at the door has to survive all the way to the booking, or the
    // exception the officer made at registration counts for nothing here.
    atCounter.sort(java.util.Comparator
        .comparing((WalkInRegistration reg) -> reg.isUrgent() ? 0 : 1)
        .thenComparing(WalkInRegistration::getCalledAt,
            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

    WalkInRegistration calledWalkIn = ui.chooseServedRegistration(atCounter, data);
    if (calledWalkIn == null) {
      ui.pause();
      return;
    }

    Guest guest = data.findGuest(calledWalkIn.getGuestId());
    if (guest == null) {
      ui.displayError("That registration's guest record is missing.");
      ui.pause();
      return;
    }

    RoomType type = data.findRoomType(calledWalkIn.getRequestedTypeId());
    if (type == null) {
      ui.displayError("The requested room type " + calledWalkIn.getRequestedTypeId()
          + " no longer exists.");
      ui.pause();
      return;
    }

    int nights = calledWalkIn.getRequestedNights();

    // The registration is the only source of the dates. Nothing is re-asked
    // here: the check-out follows from the nights the guest already gave.
    LocalDate checkIn = calledWalkIn.getRequestedCheckInDate();
    if (checkIn == null) {
      ui.displayError("Registration " + calledWalkIn.getRegId()
          + " has no check-in date on record.");
      ui.displayMessage("  It predates the current registration form"
          + " - re-register this guest.");
      ui.pause();
      return;
    }
    LocalDate checkOut = checkIn.plusDays(nights);

    ui.displaySuccess("This guest was sent over from the walk-in queue.");

    ui.displayMessage("");
    ui.displayField("Guest", guest.getFullName() + " (" + guest.getGuestId() + ")");
    ui.displayField("IC / Passport", guest.getIcPassportNo());
    ui.displayField("Contact", guest.getContactNumber());
    ui.displayField("Walk-in registration", calledWalkIn.getRegId());
    ui.displayField("Room type", type.getTypeName() + " (" + type.getTypeId() + ")");
    ui.displayField("Priority", calledWalkIn.getPriority());
    if (calledWalkIn.isUrgent()) {
      ui.displayField("Urgency reason", calledWalkIn.getUrgencyReason());
    }
    ui.displayCalculatedStay(checkIn, checkOut, nights);

    if (!ui.confirm("Create the booking from these details?")) {
      ui.displayMessage("  Booking cancelled - the guest is still IN_SERVICE.");
      ui.pause();
      return;
    }

    int guests = ui.inputGuestCount(type.getMaxOccupancy());
    if (guests < 0) {
      ui.displayMessage("  Booking cancelled.");
      ui.pause();
      return;
    }

    Booking booking = new Booking(
        data.nextBookingId(),
        guest.getGuestId(),
        type.getTypeId(),
        checkIn,
        checkOut,
        guests,
        calledWalkIn.getPriority(),
        Booking.SOURCE_WALK_IN,
        calledWalkIn.getRegId(),
        type.getBaseRatePerNight(),
        LocalDateTime.now(),
        staffId);

    data.addBooking(booking);

    // The walk-in is finished the moment it becomes a booking.
    LocalDateTime now = LocalDateTime.now();
    calledWalkIn.setStatus(WalkInRegistration.STATUS_BOOKED);
    calledWalkIn.setBookingId(booking.getBookingId());
    calledWalkIn.setBookedAt(now);
    calledWalkIn.setServedBy(staffId);
    calledWalkIn.setServedAt(now);

    data.saveAll();

    ui.displaySuccess("Booking " + booking.getBookingId()
        + " created from registration " + calledWalkIn.getRegId() + ".");
    ui.displayBooking(booking, data);

    // A booking is not finished until the guest has a room and has paid for
    // it. Both steps run here rather than being left to separate menu items,
    // because a guest standing at the counter expects to leave with a room.
    ui.displayMessage("");
    ui.displaySectionHeading("Room");
    int outcome = assignRoomTo(booking);

    if (outcome == ROOM_ASSIGNED) {
      settleBookingAtCounter(booking);
      ui.pause();
      return;
    }

    if (outcome == ROOM_CANCELLED) {
      // Rooms were available and the officer backed out, so there is nothing
      // to solve: the booking waits as PENDING for a room to be assigned.
      ui.displayMessage("");
      ui.displayMessage("  Booking " + booking.getBookingId()
          + " stays PENDING with no room.");
      ui.displayMessage("  Give it one from Rooms > Assign a room to a pending"
          + " booking.");
      ui.pause();
      return;
    }

    // Nothing of the type they asked for can be made ready. Rather than
    // leaving them with a booking that may never get a room, the party is
    // offered whatever combination of free rooms would actually house them.
    offerAlternativesOrCancel(booking, guest, guests);
    ui.pause();
  }

  /**
   * Last resort when the requested room type cannot be met.
   *
   * The party is shown every arrangement of free rooms that would house them -
   * three Standard Twins, or a Deluxe King and a Standard Twin - and takes one
   * or cancels. Splitting a party creates one booking per room, all under the
   * same guest and the same registration, because a booking holds a single
   * room; keeping them separate means each room bills and checks out normally.
   *
   * @param booking the booking that could not be given a room
   * @param guest whose party it is
   * @param guests how many people are staying
   */
  private void offerAlternativesOrCancel(Booking booking, Guest guest, int guests) {
    // Only rooms that are ready now. Cleaning work is raised and ordered at
    // check-out, so a room still waiting on housekeeping is not on offer here
    // - the guest is given what can actually be handed over today.
    ListInterface<RoomArrangement> options = service.findRoomArrangements(
        guests, booking.getCheckInDate(), booking.getCheckOutDate());

    if (options.isEmpty()) {
      ui.displayMessage("");
      ui.displayError("No combination of ready rooms can house "
          + guests + " guest(s) for those dates.");
      cancelUnhousedBooking(booking, guest);
      return;
    }

    ui.displayMessage("");
    ui.displayMessage("  The room type asked for cannot be given, but the party");
    ui.displayMessage("  can still be housed by splitting it across other rooms.");

    RoomArrangement chosen = ui.chooseArrangement(options, guests, this::typeOfRoom);
    if (chosen == null) {
      cancelUnhousedBooking(booking, guest);
      return;
    }

    bookArrangement(booking, guest, chosen);
  }

  /**
   * Gives a party its chosen arrangement, one booking per room.
   *
   * The booking already created takes the first room; any further rooms get
   * bookings of their own against the same guest and registration. Each is
   * priced at its own room's rate, so a party in a suite and a twin pays for
   * one of each rather than two of either.
   */
  private void bookArrangement(Booking booking, Guest guest, RoomArrangement chosen) {
    ListInterface<Room> rooms = chosen.getRooms();
    ListInterface<Booking> made = new ArrayList<>();

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      RoomType type = typeOfRoom(room);

      Booking target;
      if (i == 1) {
        // Re-point the booking already made at the room it is actually getting.
        booking.setTypeId(room.getTypeId());
        booking.setRatePerNight(type.getBaseRatePerNight());
        target = booking;
      } else {
        target = new Booking(data.nextBookingId(), guest.getGuestId(),
            room.getTypeId(), booking.getCheckInDate(), booking.getCheckOutDate(),
            1, booking.getPriority(), booking.getSource(), booking.getRegId(),
            type.getBaseRatePerNight(), LocalDateTime.now(), staffId);
        data.addBooking(target);
      }

      ServiceResult<Booking> assigned = service.assignRoom(target.getBookingId(),
          room.getRoomNo(), staffId, RoomAssignment.REASON_INITIAL);
      if (assigned.isFailure()) {
        ui.displayError("Room " + room.getRoomNo() + ": " + assigned.getMessage());
        continue;
      }
      made.add(target);
    }

    data.saveAll();

    if (made.isEmpty()) {
      ui.displayError("None of those rooms could be given after all.");
      cancelUnhousedBooking(booking, guest);
      return;
    }

    ui.displayMessage("");
    ui.displaySuccess(made.getNumberOfEntries()
        + " booking(s) created for " + guest.getFullName() + ".");
    ui.displayMessage("  Rooms " + chosen.roomNumbers() + ", all under this guest.");

    for (int i = 1; i <= made.getNumberOfEntries(); i++) {
      Booking each = made.getEntry(i);
      ui.displayMessage("");
      ui.displaySectionHeading("Booking " + each.getBookingId()
          + " - room " + each.getRoomNo());
      ui.displayBooking(each, data);
      settleBookingAtCounter(each);
    }
  }

  /**
   * Cancels a booking nothing could house, and says so to the guest.
   *
   * The room was never given, so there is nothing to release and nothing has
   * been charged - the booking is simply closed and the guest thanked.
   */
  private void cancelUnhousedBooking(Booking booking, Guest guest) {
    booking.setBookingStatus(Booking.STATUS_CANCELLED);

    // The walk-in ends with the booking: they were served, not left waiting.
    WalkInRegistration reg = (booking.getRegId() == null)
        ? null : data.findRegistration(booking.getRegId());
    if (reg != null) {
      reg.setBookingId(null);
      reg.setStatus(WalkInRegistration.STATUS_CANCELLED);
    }

    data.saveAll();

    ui.displayBookingCancelled(guest.getFullName());
    ui.displayMessage("");
    ui.displayMessage("  Booking " + booking.getBookingId() + " has been cancelled.");
  }

  private RoomType typeOfRoom(Room room) {
    return (room == null) ? null : data.findRoomType(room.getTypeId());
  }

  /**
   * Takes what is owed on a booking that has just been given a room.
   *
   * The bill is raised the moment the room is assigned, so it can be collected
   * here while the guest is still at the counter - which is what stops a room
   * being held for a stay nobody has paid for.
   *
   * @param booking the booking now holding a room
   */
  private void settleBookingAtCounter(Booking booking) {
    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice == null) {
      ui.displayError("No invoice was raised for this booking.");
      return;
    }

    // Asked before the money is taken, while the guest is still deciding: a
    // membership costs nothing and this stay is what earns their first points,
    // so the offer is worth more now than after they have paid and left.
    offerMembership(booking);

    ui.displayMessage("");
    ui.displaySectionHeading("Payment");
    ui.displayInvoice(invoice, service.paymentsFor(invoice.getInvoiceId()));

    // The bill is settled here or the booking does not stand. The officer is
    // asked for the amount rather than asked whether to take one, because a
    // guest at the counter is paying - the only way out is to abandon the
    // booking, which cancels it rather than leaving a room held for nothing.
    double changeDue = collectPayment(invoice, "0 to abandon the booking");
    if (changeDue == ABANDONED) {
      abandonUnpaidBooking(booking);
      return;
    }

    displayReceipt(booking, invoice, changeDue);
  }

  /**
   * Offers a free membership to a guest who does not have one.
   *
   * Nothing is awarded here. Enrolling only opens the account; the points for
   * this stay are worked out and stored when the guest checks out, which is
   * when the bill is final and the stay actually happened.
   *
   * @param booking the stay being paid for
   */
  private void offerMembership(Booking booking) {
    Guest guest = data.findGuest(booking.getGuestId());
    if (guest == null || data.findMemberByGuest(booking.getGuestId()) != null) {
      return;
    }

    ui.displayMessage("");
    ui.displaySectionHeading("Loyalty membership");
    ui.displayMessage("  " + guest.getFullName() + " is not a member yet.");
    ui.displayMessage("  Membership is free. Points are earned on every stay"
        + " and spent on");
    ui.displayMessage("  spa sessions, dining and activities during a visit.");
    ui.displayMessage("");

    if (!ui.confirm("Join the loyalty programme?")) {
      ui.displayMessage("  No membership taken. They will be enrolled"
          + " automatically at check-out.");
      return;
    }

    ServiceResult<Member> enrolled = service.enrolMember(booking.getGuestId());
    if (enrolled.isFailure()) {
      ui.displayError(enrolled.getMessage());
      return;
    }

    ui.displaySuccess("Welcome aboard - member "
        + enrolled.getValue().getMemberId() + ", "
        + enrolled.getValue().getTier() + " tier.");
    ui.displayMessage("  Points for this stay are added when they check out.");
  }

  /** Returned by collectPayment when the officer gave up on the bill. */
  private static final double ABANDONED = -1.0;

  /**
   * Takes the money for a bill in one go.
   *
   * The whole balance is settled in a single payment: anything short of it is
   * refused and asked again, because a stay half paid for is not paid for, and
   * a guest standing at the counter can hand over the rest there and then.
   * Anything above the balance is change rather than revenue, so only what is
   * owed reaches the invoice and the remainder goes back to the guest.
   *
   * @param invoice the bill to settle
   * @param quitLabel what typing 0 does, as the prompt should word it
   * @return the change to hand back, or ABANDONED if they gave up
   */
  private double collectPayment(Invoice invoice, String quitLabel) {
    final double owed = invoice.getOutstandingBalance();

    while (!invoice.isSettled()) {
      // The method is settled first, because it decides what amounts may be
      // taken: only cash can be over-tendered, since only cash has change to
      // give back. A card or transfer moves an exact sum.
      String method = ui.inputPaymentMethod();
      if (method == null) {
        return ABANDONED;
      }
      boolean isCash = Payment.CASH.equals(method);

      double tendered = ui.inputAmount("Amount received", isCash
          ? String.format("RM%.2f due, more is fine, %s", owed, quitLabel)
          : String.format("RM%.2f exactly, %s", owed, quitLabel));
      if (tendered == MessageUI.CANCELLED_AMOUNT) {
        return ABANDONED;
      }

      // A part payment leaves a room held for a stay nobody has paid for, so
      // it is refused outright rather than recorded and chased later.
      if (tendered + 0.005 < owed) {
        ui.displayError(String.format(
            "RM%.2f is not enough - the full RM%.2f is due.", tendered, owed));
        continue;
      }

      // Nothing hands change back on a card or a transfer: the guest is
      // charged what the terminal is told, so the sum has to be exact.
      if (!isCash && tendered > owed + 0.005) {
        ui.displayError(String.format(
            "A %s payment must be exactly RM%.2f - there is no change to give.",
            method, owed));
        ui.displayMessage("  Enter the exact amount, or pay by cash instead.");
        continue;
      }

      String reference = null;
      if (Payment.requiresReference(method)) {
        reference = ui.inputPaymentReference();
        if (reference == null) {
          ui.displayMessage("  Payment cancelled - the bill is still open.");
          continue;
        }
      }

      ServiceResult<Payment> result = service.recordPayment(invoice.getInvoiceId(),
          owed, method, reference, staffId);

      if (result.isFailure()) {
        ui.displayError(result.getMessage());
        continue;
      }

      ui.displaySuccess(result.getMessage());

      double change = tendered - owed;
      if (change > 0.005) {
        ui.displayMessage(String.format("  Tendered RM%.2f - change due RM%.2f.",
            tendered, change));
        return change;
      }
      return 0.0;
    }

    return 0.0;
  }

  /**
   * Shows what was paid, then clears the screen once the officer has done with
   * it - the receipt is the last thing on screen, and nothing follows it until
   * they say so.
   *
   * @param booking the booking now paid for
   * @param invoice its settled bill
   * @param changeDue anything handed back to the guest
   */
  private void displayReceipt(Booking booking, Invoice invoice, double changeDue) {
    Guest guest = data.findGuest(booking.getGuestId());

    ui.displayReceipt(booking, invoice, service.paymentsFor(invoice.getInvoiceId()),
        guest == null ? "-" : guest.getFullName(), changeDue, data);

    ui.pause("Press ENTER to close the receipt");
    ui.clearScreen();
  }

  /**
   * Gives up a booking the guest would not pay for.
   *
   * The room was reserved the moment it was assigned, so it has to be handed
   * back - leaving it held for an unpaid booking would keep it from the next
   * guest for nothing.
   */
  private void abandonUnpaidBooking(Booking booking) {
    releaseRoomAndReservation(booking);
    booking.setBookingStatus(Booking.STATUS_CANCELLED);

    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice != null) {
      data.getInvoiceList().removeEntry(invoice);
    }

    data.saveAll();

    ui.displayMessage("");
    ui.displayError("Booking " + booking.getBookingId()
        + " was not paid for and has been cancelled.");
    ui.displayMessage("  Room " + booking.getRoomNo() + " is free again.");
  }

  /**
   * Changes a booking's dates or guest count.
   *
   * The room has to be checked again afterwards: dates that were free when the
   * booking was made may since have been taken by somebody else.
   */
  private void amendBooking() {
    while (true) {
      ui.startAction("EDIT A BOOKING");

      ListInterface<Booking> amendable = amendableBookings();
      Booking booking = ui.chooseBookingToAmend(amendable, data);
      if (booking == null) {
        ui.pause();
        return;
      }

      editBookingFields(booking);

      if (!ui.confirmAnother("Edit another booking?")) {
        return;
      }
    }
  }

  /**
   * The bookings that may still be changed.
   *
   * A stay that has started cannot be amended: the guest is already in the
   * room, so moving the dates under them would rewrite something that has
   * happened. Only a booking arriving tomorrow or later is offered, and a
   * cancelled or finished one never is.
   *
   * @return the amendable bookings, earliest arrival first
   */
  private ListInterface<Booking> amendableBookings() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);

    ListInterface<Booking> amendable = data.getBookingList().filter(booking ->
        !booking.getCheckInDate().isBefore(tomorrow)
            && (Booking.STATUS_PENDING.equals(booking.getBookingStatus())
                || Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus())));

    amendable.sort(java.util.Comparator.comparing(Booking::getCheckInDate));
    return amendable;
  }

  /**
   * Edits one booking, a field at a time, until the officer is done.
   *
   * Each change is shown against what it replaces and confirmed on its own,
   * rather than the whole booking being retyped to alter one thing.
   */
  private void editBookingFields(Booking booking) {
    while (true) {
      ui.startAction("EDIT BOOKING " + booking.getBookingId());
      ui.displayBooking(booking, data);

      int choice = ui.getAmendFieldChoice(booking);
      switch (choice) {
        case 1:
          amendCheckInDate(booking);
          break;
        case 2:
          amendNights(booking);
          break;
        case 3:
          amendGuestCount(booking);
          break;
        case 4:
          amendRoomType(booking);
          break;
        default:
          return;
      }
    }
  }

  private void amendCheckInDate(Booking booking) {
    ui.startAction("CHANGE THE CHECK-IN DATE");
    ui.displayBooking(booking, data);
    ui.displayMessage("");

    LocalDate checkIn = ui.inputDate("New check-in date");
    if (checkIn == null) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }
    if (checkIn.isBefore(LocalDate.now().plusDays(1))) {
      ui.displayError("A booking can only be moved to tomorrow or later.");
      ui.pause();
      return;
    }

    // The stay keeps its length: moving the arrival moves the departure with
    // it, so a guest who booked three nights still has three.
    LocalDate checkOut = checkIn.plusDays(booking.getNumberOfNights());

    ui.displayProposedChange("Stay",
        booking.getCheckInDate() + " to " + booking.getCheckOutDate(),
        checkIn + " to " + checkOut);

    applyDateChange(booking, checkIn, checkOut);
  }

  private void amendNights(Booking booking) {
    ui.startAction("CHANGE THE NUMBER OF NIGHTS");
    ui.displayBooking(booking, data);
    ui.displayMessage("");

    int nights = ui.inputNights();
    if (nights < 0) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    LocalDate checkOut = booking.getCheckInDate().plusDays(nights);

    ui.displayProposedChange("Nights",
        booking.getNumberOfNights() + " night(s), leaving " + booking.getCheckOutDate(),
        nights + " night(s), leaving " + checkOut);

    applyDateChange(booking, booking.getCheckInDate(), checkOut);
  }

  /**
   * Moves a booking's dates once the officer has confirmed them.
   *
   * A booking already holding a room has to be checked against the other
   * stays first: dates that were free when it was made may since have gone.
   */
  private void applyDateChange(Booking booking, LocalDate checkIn, LocalDate checkOut) {
    if (booking.getRoomNo() != null
        && service.hasDateClash(booking.getRoomNo(), checkIn, checkOut,
            booking.getBookingId())) {
      ui.displayMessage("");
      ui.displayError("Room " + booking.getRoomNo()
          + " is already booked over those dates. Move the guest first.");
      ui.pause();
      return;
    }

    ui.displayMessage("");
    if (!ui.confirm("Apply this change?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    booking.setCheckInDate(checkIn);
    booking.setCheckOutDate(checkOut);
    repriceBooking(booking);

    ui.displaySuccess("Booking " + booking.getBookingId() + " updated.");
    ui.displayBooking(booking, data);
    ui.pause("Press ENTER to accept the edit");
  }

  private void amendGuestCount(Booking booking) {
    ui.startAction("CHANGE THE NUMBER OF GUESTS");
    ui.displayBooking(booking, data);
    ui.displayMessage("");

    RoomType type = data.findRoomType(booking.getTypeId());
    int maximum = (type == null) ? 6 : type.getMaxOccupancy();
    ui.displayMessage("  " + (type == null ? booking.getTypeId() : type.getTypeName())
        + " sleeps up to " + maximum + ".");

    int guests = ui.inputGuestCount(maximum);
    if (guests < 0) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    ui.displayProposedChange("Guests",
        String.valueOf(booking.getNumberOfGuests()), String.valueOf(guests));

    ui.displayMessage("");
    if (!ui.confirm("Apply this change?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    booking.setNumberOfGuests(guests);
    data.saveFrontDesk();

    ui.displaySuccess("Booking " + booking.getBookingId() + " updated.");
    ui.displayBooking(booking, data);
    ui.pause("Press ENTER to accept the edit");
  }

  /**
   * Moves a booking to a different room type.
   *
   * The room goes back if it no longer suits: a booking cannot keep a Standard
   * Twin while claiming to be a Family Suite, so it returns to PENDING and is
   * given a room of the new type from the Rooms menu.
   */
  private void amendRoomType(Booking booking) {
    ui.startAction("CHANGE THE ROOM TYPE");
    ui.displayBooking(booking, data);
    ui.displayMessage("");

    String typeId = ui.inputRoomType(data.getRoomTypeList());
    if (typeId == null) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }
    if (typeId.equals(booking.getTypeId())) {
      ui.displayMessage("  That is already the booking's room type.");
      ui.pause();
      return;
    }

    RoomType wanted = data.findRoomType(typeId);
    if (wanted.getMaxOccupancy() < booking.getNumberOfGuests()) {
      ui.displayError(wanted.getTypeName() + " sleeps " + wanted.getMaxOccupancy()
          + ", but this booking is for " + booking.getNumberOfGuests() + " guest(s).");
      ui.displayMessage("  Change the number of guests first, or pick a larger type.");
      ui.pause();
      return;
    }

    RoomType current = data.findRoomType(booking.getTypeId());
    ui.displayProposedChange("Room type",
        (current == null ? booking.getTypeId() : current.getTypeName())
            + String.format("  (RM%.2f/night)", booking.getRatePerNight()),
        wanted.getTypeName()
            + String.format("  (RM%.2f/night)", wanted.getBaseRatePerNight()));

    if (booking.getRoomNo() != null) {
      ui.displayMessage("");
      ui.displayMessage("  Room " + booking.getRoomNo()
          + " does not match the new type and will be given up.");
      ui.displayMessage("  The booking returns to PENDING until a room is assigned.");
    }

    ui.displayMessage("");
    if (!ui.confirm("Apply this change?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    if (booking.getRoomNo() != null) {
      releaseRoomAndReservation(booking);
      booking.setRoomNo(null);
      booking.setBookingStatus(Booking.STATUS_PENDING);
    }

    booking.setTypeId(typeId);
    booking.setRatePerNight(wanted.getBaseRatePerNight());
    repriceBooking(booking);

    ui.displaySuccess("Booking " + booking.getBookingId() + " updated.");
    ui.displayBooking(booking, data);
    ui.pause("Press ENTER to accept the edit");
  }

  /** Keeps the bill in step with the nights and rate now booked. */
  private void repriceBooking(Booking booking) {
    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice != null) {
      invoice.setRoomCharge(booking.getRatePerNight() * booking.getNumberOfNights());
      invoice.setAmountPaid(service.sumPayments(invoice.getInvoiceId()));
    }
    data.saveAll();
  }

  /**
   * Cancels a booking and gives up its room.
   *
   * If housekeeping was cleaning a room for this booking, that reservation is
   * released so the task falls back to the normal lane - but the cleaning goes
   * ahead, because the room is dirty whether or not anyone is waiting.
   */
  private void cancelBooking() {
    // Answering no returns to the listing rather than ending the action, so a
    // booking picked by mistake costs one answer instead of the whole screen.
    while (true) {
      ui.startAction("CANCEL A BOOKING");

      ListInterface<Booking> cancellable = cancellableBookings();
      Booking booking = ui.chooseBooking(cancellable, data,
          "Number of the booking to cancel", new String[] {
            "No booking can be cancelled.",
            "A guest who has checked in must be checked out instead."
          });

      if (booking == null) {
        ui.pause();
        return;
      }

      ui.displayBooking(booking, data);
      ui.displayMessage("");

      if (!ui.confirm("Are you sure you want to cancel booking "
          + booking.getBookingId() + "?")) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      releaseRoomAndReservation(booking);
      booking.setBookingStatus(Booking.STATUS_CANCELLED);
      data.saveAll();

      ui.displaySuccess("Booking " + booking.getBookingId() + " cancelled.");
      if (booking.getRoomNo() != null) {
        ui.displayMessage("  Room " + booking.getRoomNo() + " has been given up.");
      }
      ui.pause("Press ENTER to accept the cancellation");
    }
  }

  /**
   * The bookings that may still be cancelled.
   *
   * A guest already in the room is checked out rather than cancelled, and one
   * that has finished or been cancelled already has nothing left to give up.
   *
   * @return the cancellable bookings, earliest arrival first
   */
  private ListInterface<Booking> cancellableBookings() {
    ListInterface<Booking> cancellable = data.getBookingList().filter(booking ->
        Booking.STATUS_PENDING.equals(booking.getBookingStatus())
            || Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus()));

    cancellable.sort(java.util.Comparator.comparing(Booking::getCheckInDate));
    return cancellable;
  }

  /**
   * Permanently removes only a booking that has not yet affected any room,
   * invoice, or walk-in record.  Once a booking is connected to operational
   * data, cancellation preserves the audit trail instead.
   */
  private void deleteBooking() {
    while (true) {
      ui.startAction("DELETE AN UNASSIGNED BOOKING");

      ListInterface<Booking> deletable = deletableBookings();
      Booking booking = ui.chooseBooking(deletable, data,
          "Number of the booking to delete", new String[] {
            "No booking can be deleted.",
            "Only a pending booking with no room, no invoice and no walk-in",
            "behind it can be removed - cancel the others instead, so their",
            "operational history is kept."
          });

      if (booking == null) {
        ui.pause();
        return;
      }

      ui.displayBooking(booking, data);
      ui.displayMessage("");
      ui.displayMessage("  Deleting removes the booking entirely. It cannot be undone.");

      if (!ui.confirm("Are you sure you want to delete booking "
          + booking.getBookingId() + "?")) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      data.removeBooking(booking);
      data.saveFrontDesk();

      ui.displaySuccess("Booking " + booking.getBookingId() + " deleted.");
      ui.pause("Press ENTER to accept the deletion");
    }
  }

  /**
   * The bookings that may be removed outright rather than cancelled.
   *
   * Once a booking has taken a room, raised a bill or come from a walk-in, it
   * has a history worth keeping, so only an untouched pending one qualifies.
   *
   * @return the deletable bookings, earliest arrival first
   */
  private ListInterface<Booking> deletableBookings() {
    ListInterface<Booking> deletable = data.getBookingList().filter(booking ->
        Booking.STATUS_PENDING.equals(booking.getBookingStatus())
            && booking.getRoomNo() == null
            && booking.getRegId() == null
            && data.findInvoiceByBooking(booking.getBookingId()) == null);

    deletable.sort(java.util.Comparator.comparing(Booking::getCheckInDate));
    return deletable;
  }

  /** Records that a confirmed guest never arrived. */
  private void markNoShow() {
    while (true) {
      ui.startAction("MARK A BOOKING AS NO-SHOW");

      ListInterface<Booking> expected = noShowCandidates();
      Booking booking = ui.chooseBooking(expected, data,
          "Number of the booking that never arrived", new String[] {
            "No booking is waiting to be marked as a no-show.",
            "Only a CONFIRMED booking - one holding a room whose guest has",
            "not checked in - can be recorded as a no-show."
          });

      if (booking == null) {
        ui.pause();
        return;
      }

      ui.displayBooking(booking, data);
      ui.displayMessage("");
      ui.displayMessage("  The room will be given up and the booking closed.");

      if (!ui.confirm("Are you sure booking " + booking.getBookingId()
          + " never arrived?")) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      String heldRoom = booking.getRoomNo();
      releaseRoomAndReservation(booking);
      booking.setBookingStatus(Booking.STATUS_NO_SHOW);
      data.saveAll();

      ui.displaySuccess("Booking " + booking.getBookingId() + " recorded as a no-show.");
      if (heldRoom != null) {
        ui.displayMessage("  Room " + heldRoom + " is free again.");
      }
      ui.pause("Press ENTER to accept the no-show");
    }
  }

  /**
   * The bookings whose guest could still fail to turn up.
   *
   * Only a CONFIRMED booking qualifies: one that is holding a room but whose
   * guest has not checked in. A pending booking has no room to give up, and a
   * checked-in guest plainly arrived.
   *
   * @return the candidates, earliest arrival first
   */
  private ListInterface<Booking> noShowCandidates() {
    ListInterface<Booking> expected = data.getBookingList().filter(
        booking -> Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus()));

    expected.sort(java.util.Comparator.comparing(Booking::getCheckInDate));
    return expected;
  }

  /**
   * Frees whatever a booking was holding.
   *
   * A reserved room that was never slept in goes straight back to being
   * available rather than being sent for cleaning - nobody has been in it.
   */
  private void releaseRoomAndReservation(Booking booking) {
    if (booking.getRoomNo() != null) {
      Room room = data.findRoom(booking.getRoomNo());
      if (room != null && Room.RESERVED.equals(room.getOccupancyStatus())) {
        room.setOccupancyStatus(Room.VACANT);
      }

      RoomAssignment open = data.findOpenAssignment(booking.getBookingId());
      if (open != null) {
        open.setReleasedAt(LocalDateTime.now());
      }
    }

    // Housekeeping is no longer holding a room for this booking.
    HousekeepingTask reserved = data.getTaskList().search(
        task -> booking.getBookingId().equals(task.getReservedForBookingId()));
    if (reserved != null) {
      reserved.setReservedForBookingId(null);
      reserved.setRemark("Booking cancelled - cleaning continues in the normal lane");
      service.refreshTaskPriority(reserved);
    }
  }

  // ==================================================================
  // ROOMS
  // ==================================================================

  /**
   * Shows which rooms could be given out over a date range.
   *
   * Rooms that only need cleaning are listed too, so an officer can offer a
   * short wait rather than turning a guest away.
   */
  private void checkAvailability() {
    ui.startAction("CHECK ROOM AVAILABILITY");

    String typeId = ui.inputRoomType(data.getRoomTypeList());
    if (typeId == null) {
      return;
    }

    LocalDate checkIn = ui.inputDate("Check-in date");
    if (checkIn == null) {
      return;
    }

    LocalDate checkOut = ui.inputDate("Check-out date");
    if (checkOut == null) {
      return;
    }
    if (!checkOut.isAfter(checkIn)) {
      ui.displayError("The check-out date must be after the check-in date.");
      ui.pause();
      return;
    }

    RoomType type = data.findRoomType(typeId);
    String typeName = (type == null) ? typeId : type.getTypeName();

    ListInterface<Room> ready = service.findAvailableRooms(typeId, checkIn, checkOut);
    ui.displaySectionHeading("Ready to assign now");
    if (ready.isEmpty()) {
      ui.displayMessage("  No " + typeName + " room is ready for those dates.");
    } else {
      ui.displayRoomBoard(ready, data);
    }

    ListInterface<Room> cleanable = service.findCleanableRooms(typeId, checkIn, checkOut);
    if (!cleanable.isEmpty()) {
      ui.displaySectionHeading("Could be ready once cleaned");
      ui.displayRoomBoard(cleanable, data);
      ui.displayMessage("  Closest to ready is listed first.");
    }

    // The rooms of this type that are simply spoken for. Shown so the officer
    // can see the type is sold rather than merely dirty, and when each one
    // frees up - without naming whose booking it is, which is not their
    // business at this desk.
    ListInterface<Room> taken = takenRoomsOfType(typeId, checkIn, checkOut);
    if (!taken.isEmpty()) {
      ui.displaySectionHeading("Already booked over these dates");
      ui.displayTakenRooms(taken, data, checkIn, checkOut);
    }

    ui.pause();
  }

  /**
   * Rooms of a type that are unavailable because somebody already has them.
   *
   * Only a date clash counts here: a room that is merely dirty is listed under
   * cleaning instead, and one out of service is not on offer at all.
   *
   * @param typeId the type being asked about
   * @param checkIn the first night wanted
   * @param checkOut the morning the guest would leave
   * @return the rooms of that type already booked across those dates
   */
  private ListInterface<Room> takenRoomsOfType(String typeId, LocalDate checkIn,
      LocalDate checkOut) {
    ListInterface<Room> taken = new ArrayList<>();
    ListInterface<Room> rooms = data.getRoomList();

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (!typeId.equals(room.getTypeId()) || room.isOutOfService()) {
        continue;
      }
      if (service.hasDateClash(room.getRoomNo(), checkIn, checkOut, null)) {
        taken.add(room);
      }
    }
    return taken;
  }

  /**
   * Hands out a room to a booking that is waiting for one.
   *
   * The waiting bookings are listed urgent first, which is what carries the
   * urgency a walk-in guest was granted at the door all the way through to the
   * officer handing out rooms - the same ordering housekeeping uses when it
   * decides which room to prepare next. The booking is then picked by its
   * position in that list, so working down it from the top serves the urgent
   * guests before the normal ones without anyone having to remember to.
   */
  private void assignRoomToBooking() {
    while (true) {
      ui.startAction("ASSIGN A ROOM");

      ListInterface<Booking> pending = service.pendingBookingsByPriority();

      if (!ui.displayBookingList(pending, data,
          "No booking is waiting for a room.")) {
        ui.pause();
        return;
      }

      ui.displayMessage("");
      ui.displayMessage("  Urgent bookings are listed first - work down from the top.");

      int position = ui.inputListPosition(pending.getNumberOfEntries(),
          "Position of the booking to give a room");
      if (position < 0) {
        return;
      }

      // Giving a room raises the bill, so the chance to settle it is offered
      // here too - the same rule whichever screen the room came from.
      Booking given = pending.getEntry(position);
      if (assignRoomTo(given) == ROOM_ASSIGNED) {
        settleBookingAtCounter(given);
      }

      if (!ui.confirmAnother("Give a room to another booking?")) {
        return;
      }
    }
  }

  /**
   * Finds and assigns a room for a booking.
   *
   * Only a room housekeeping has finished with is offered - the service checks
   * both that it is free for the dates and that it is clean, so a room cannot
   * be sold out from under a cleaner. When nothing is ready the officer is
   * offered the cleaning route rather than simply being refused, which is what
   * stops a guest being turned away over a room twenty minutes from ready.
   *
   * @param booking the booking to give a room to
   * @return true if the booking now holds a room
   */
  /** A room was given to the booking. */
  private static final int ROOM_ASSIGNED = 1;
  /** No room of that type is ready, so alternatives are worth offering. */
  private static final int ROOM_NONE_READY = 0;
  /** The officer backed out. Nothing further should be offered. */
  private static final int ROOM_CANCELLED = -1;

  private int assignRoomTo(Booking booking) {
    ListInterface<Room> available = service.findAvailableRooms(
        booking.getTypeId(), booking.getCheckInDate(), booking.getCheckOutDate());

    if (!available.isEmpty()) {
      ui.displayMessage("  These rooms are vacant for the dates and cleaned.");
      String roomNo = ui.chooseRoom(available);
      if (roomNo == null) {
        // Backing out is a decision, not a shortage: rooms were on offer and
        // the officer declined them, so nothing else is proposed.
        ui.displayMessage("  Assignment cancelled.");
        return ROOM_CANCELLED;
      }

      ServiceResult<Booking> assigned = service.assignRoom(booking.getBookingId(),
          roomNo, staffId, RoomAssignment.REASON_INITIAL);

      if (assigned.isFailure()) {
        ui.displayError(assigned.getMessage());
        return ROOM_CANCELLED;
      }

      ui.displaySuccess(assigned.getMessage());
      ui.displayBooking(booking, data);
      return ROOM_ASSIGNED;
    }

    // Only a room housekeeping has finished with can be sold. The front desk
    // no longer asks for a room to be cleaned out of turn: cleaning work is
    // raised at check-out and ordered there, so what is dirty now is simply
    // not on offer. A party that cannot be housed is offered other room types.
    RoomType wanted = data.findRoomType(booking.getTypeId());
    ui.displayError("No " + (wanted == null ? booking.getTypeId() : wanted.getTypeName())
        + " room is ready for those dates.");
    return ROOM_NONE_READY;
  }

  /**
   * Moves a guest to a different room, keeping the history of both.
   *
   * This version displays a list of movable bookings first so the officer
   * can see which bookings have rooms assigned before choosing one.
   */
  private void moveBookingToAnotherRoom() {
    ui.startAction("MOVE A BOOKING TO ANOTHER ROOM");

    // Get all bookings that have a room assigned and are still active
    ListInterface<Booking> movable = data.getBookingList().filter(booking ->
        booking.getRoomNo() != null
        && (Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus())
            || Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())));

    // Show the list and let the user pick one
    String bookingId = ui.inputBookingId(movable, data);
    if (bookingId == null) {
      ui.pause();
      return;
    }

    Booking booking = data.findBooking(bookingId);
    if (booking == null) {
      ui.displayError("That booking no longer exists.");
      ui.pause();
      return;
    }

    // Double-check the booking still has a room
    if (booking.getRoomNo() == null) {
      ui.displayError("This booking no longer has a room to move from.");
      ui.pause();
      return;
    }

    ui.displayBooking(booking, data);
    ui.displayMessage("");

    // Find available rooms of the same type for the booking's dates
    ListInterface<Room> available = service.findAvailableRooms(
        booking.getTypeId(), booking.getCheckInDate(), booking.getCheckOutDate());

    // Remove the current room from the list (can't move to the same room)
    ListInterface<Room> otherRooms = new ArrayList<>();
    for (int i = 1; i <= available.getNumberOfEntries(); i++) {
      Room room = available.getEntry(i);
      if (!room.getRoomNo().equals(booking.getRoomNo())) {
        otherRooms.add(room);
      }
    }

    if (otherRooms.isEmpty()) {
      ui.displayError("No other room of that type is ready for those dates.");
      ui.pause();
      return;
    }

    ui.displaySectionHeading("Available rooms to move to");
    String roomNo = ui.chooseRoom(otherRooms);
    if (roomNo == null) {
      ui.displayMessage("  Move cancelled.");
      ui.pause();
      return;
    }

    String reason = ui.inputAssignmentReason();
    if (reason == null) {
      ui.displayMessage("  Move cancelled.");
      ui.pause();
      return;
    }

    String from = booking.getRoomNo();
    ServiceResult<Booking> moved = service.assignRoom(booking.getBookingId(),
        roomNo, staffId, reason);

    if (moved.isSuccess()) {
      ui.displaySuccess("Moved from room " + from + " to " + roomNo + ".");
      ui.displayMessage("  Room " + from + " has been sent for cleaning.");
    } else {
      ui.displayError(moved.getMessage());
    }
    ui.pause();
  }

  /** The whole room board - both statuses and whether each room is sellable. */
  private void displayRoomBoard() {
    ui.startAction("ROOM STATUS BOARD");
    ui.displayRoomBoard(data.getRoomList(), data);
    ui.displayMessage("  A room is assignable only when it is vacant AND cleaned.");
    ui.pause();
  }

  // ==================================================================
  // STAY
  // ==================================================================

  private void checkIn() {
    ui.startAction("CHECK IN");

    ListInterface<Booking> confirmed = data.getBookingList().filter(
        booking -> Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus()));
    confirmed.sort(java.util.Comparator.comparing(Booking::getCheckInDate));

    Booking booking = ui.chooseBooking(confirmed, data,
        "Number of the booking checking in", new String[] {
          "No booking is waiting to check in.",
          "Only a CONFIRMED booking - one holding a room - can check in."
        });
    if (booking == null) {
      ui.pause();
      return;
    }

    // A stay is paid for when it is booked, so by the time a guest arrives to
    // check in there is nothing left to collect. An open bill here means the
    // booking was made some other way; it is sent to Billing rather than
    // taking money at this desk, which is not what this screen is for.
    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice != null && !invoice.isSettled()) {
      ui.displayInvoice(invoice, service.paymentsFor(invoice.getInvoiceId()));
      ui.displayMessage("");
      ui.displayError(String.format("RM%.2f is still outstanding on %s.",
          invoice.getOutstandingBalance(), invoice.getInvoiceId()));
      ui.displayMessage("  A stay is paid for when it is booked, so this one is"
          + " unusual.");
      ui.displayMessage("  Settle it at Billing > Record a payment, then check"
          + " the guest in.");
      ui.pause();
      return;
    }

    ServiceResult<Booking> result = service.checkIn(booking.getBookingId());
    if (result.isFailure()) {
      ui.displayError(result.getMessage());
      ui.pause();
      return;
    }

    ui.displaySuccess(result.getMessage());
    ui.displayBooking(booking, data);
    ui.pause();
  }

  /**
   * Ends a stay.
   *
   * One action with consequences in three other places, so what happened is
   * reported back in full rather than with a bare "done".
   */
  private void checkOut() {
    ui.startAction("CHECK OUT");

    ListInterface<Booking> stayed = data.getBookingList().filter(
        booking -> Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus()));
    stayed.sort(java.util.Comparator.comparing(Booking::getCheckOutDate));

    Booking booking = ui.chooseBooking(stayed, data,
        "Number of the booking checking out", new String[] {
          "No guest is currently checked in.",
          "Only a CHECKED_IN booking can check out."
        });
    if (booking == null) {
      ui.pause();
      return;
    }

    // Nothing is collected here. The stay was paid for when it was booked and
    // a bill cannot be part paid, so a guest who is checked in has settled up
    // already - check-out is only about handing the room back.
    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice != null && !invoice.isSettled()) {
      ui.displayInvoice(invoice, service.paymentsFor(invoice.getInvoiceId()));
      ui.displayMessage("");
      ui.displayError(String.format("RM%.2f is still outstanding on %s.",
          invoice.getOutstandingBalance(), invoice.getInvoiceId()));
      ui.displayMessage("  Settle it at Billing > Record a payment, then check"
          + " the guest out.");
      ui.pause();
      return;
    }

    ui.displayBooking(booking, data);
    ui.displayMessage("");
    if (!ui.confirm("Check this guest out?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    // Check-out is the one place the front desk hands work to housekeeping,
    // so it is where the urgency is set. The officer knows whether the room
    // is wanted back today; nothing in the records does.
    int urgency = ui.inputCleaningUrgency(booking.getRoomNo());
    if (urgency < 0) {
      ui.displayMessage("  Check-out cancelled - nothing has been changed.");
      ui.pause();
      return;
    }
    boolean urgent = (urgency == 1);

    ServiceResult<Booking> result = service.checkOut(booking.getBookingId(),
        staffId, urgent);
    if (result.isFailure()) {
      ui.displayError(result.getMessage());
      ui.pause();
      return;
    }

    ui.displaySuccess(result.getMessage());
    ui.displayMessage("");
    ui.displayMessage("  Room " + booking.getRoomNo()
        + " is now DIRTY and has gone to housekeeping.");
    ui.displayMessage(urgent
        ? "  It went to the URGENT lane and is cleaned before the rest."
        : "  It joins the normal cleaning round.");
    ui.displayMessage("  Housekeeping sees it under Cleaning Queue.");

    // Whatever this stay just did to the guest's loyalty account is looked
    // up fresh rather than assumed, so it is shown exactly as Loyalty & Rewards
    // would show it - the same tier, the same balance - not restated from the
    // one-line message the checkout already printed.
    ui.displayLoyaltyOutcome(result.getValue().getGuestId(), data);
    ui.pause();
  }

  // ==================================================================
  // BILLING
  // ==================================================================

  /**
   * Shows a bill picked from a list rather than typed in.
   *
   * Nobody remembers invoice numbers, so the bills are listed with the guest,
   * the dates and the amount, and a row number is what gets typed.
   */
  private void viewInvoice() {
    while (true) {
      ui.startAction("VIEW AN INVOICE");

      // Newest first: the bill somebody asks about is usually a recent one.
      ListInterface<Invoice> invoices = copyOfInvoices(data.getInvoiceList());
      invoices.sort(java.util.Comparator.comparing(Invoice::getIssuedAt,
          java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));

      if (!ui.displayBookingRecords(invoices, data)) {
        ui.pause();
        return;
      }

      ui.displayMessage("");
      int position = ui.inputListPosition(invoices.getNumberOfEntries(),
          "Number of the invoice to view");
      if (position < 0) {
        return;
      }

      Invoice invoice = invoices.getEntry(position);
      ui.clearScreen();
      ui.startAction("INVOICE " + invoice.getInvoiceId());
      ui.displayInvoice(invoice, service.paymentsFor(invoice.getInvoiceId()));
      ui.pause("Press ENTER to go back to the list");
    }
  }

  private void recordPayment() {
    ui.startAction("RECORD A PAYMENT");

    ListInterface<Invoice> open = data.getInvoiceList().filter(
        invoice -> !invoice.isSettled());

    if (open.isEmpty()) {
      ui.displayMessage("  Every invoice is settled.");
      ui.pause();
      return;
    }

    ui.displaySectionHeading("Unsettled invoices");
    ui.displayTableHeading(String.format("  %-8s %-7s %-20s %10s %10s  %s",
        "INVOICE", "BOOKING", "GUEST", "TOTAL", "OWED", "STATUS"));

    for (int i = 1; i <= open.getNumberOfEntries(); i++) {
      Invoice invoice = open.getEntry(i);
      Booking booking = data.findBooking(invoice.getBookingId());
      Guest guest = (booking == null) ? null : data.findGuest(booking.getGuestId());

      System.out.printf("  %-8s %-7s %-20s %10.2f %10.2f  %s%n",
          invoice.getInvoiceId(), invoice.getBookingId(),
          guest == null ? "-" : guest.getFullName(),
          invoice.getTotalAmount(), invoice.getOutstandingBalance(),
          invoice.getPaymentStatus());
    }
    ui.displayThinRule();

    Invoice invoice;
    while (true) {
      String invoiceId = ui.inputInvoiceId();
      if (invoiceId == null) {
        return;
      }

      invoice = data.findInvoice(invoiceId);
      if (invoice != null) {
        break;
      }

      ui.displayError("No invoice with ID " + invoiceId
          + ". Enter another number, or 0 to go back.");
    }

    takePaymentFor(invoice);
    ui.pause();
  }

  /** Takes one payment against a bill. */
  private void takePaymentFor(Invoice invoice) {
    ui.displayInvoice(invoice, service.paymentsFor(invoice.getInvoiceId()));

    if (invoice.isSettled()) {
      ui.displayMessage("");
      ui.displayMessage("  This invoice is already settled.");
      return;
    }

    // A bill is settled in one payment wherever it is taken, so this screen
    // collects it the same way the counter does. Nothing anywhere can leave a
    // stay part paid.
    double changeDue = collectPayment(invoice, "0 to cancel");
    if (changeDue == ABANDONED) {
      ui.displayMessage("  Payment cancelled - the bill is still open.");
      return;
    }

    Booking booking = data.findBooking(invoice.getBookingId());
    if (booking != null) {
      displayReceipt(booking, invoice, changeDue);
    }
  }

  /** Takes an approved loyalty reward off a live bill. */
  /**
   * Requests a loyalty reward against a booking.
   *
   * Driven by the 8-digit confirmation number the guest is carrying rather
   * than an internal ID, because that is the only reference they have. The
   * booking is shown and confirmed first, so a mistyped number is caught
   * before anything is submitted.
   *
   * Nothing is granted here. The request goes into the pending queue for
   * Loyalty to approve, which is what keeps the desk from awarding rewards
   * to itself - the points are only taken when a loyalty officer processes it.
   */
  private void requestLoyaltyReward() {
    ui.startAction("REQUEST A LOYALTY REWARD");

    // --- Screen one: which booking? -------------------------------
    Booking booking = promptByConfirmationNumber();
    if (booking == null) {
      return;
    }

    Guest guest = data.findGuest(booking.getGuestId());
    if (guest == null) {
      ui.displayError("That booking's guest record is missing.");
      ui.pause();
      return;
    }

    ui.displayBookingForRedemption(booking, guest, data);
    ui.displayMessage("");

    if (!ui.confirm("Is this the right booking?")) {
      ui.displayMessage("  Nothing has been requested.");
      ui.pause();
      return;
    }

    // A reward is enjoyed during the stay, so a stay already finished or
    // called off has nothing left to attach one to.
    if (Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus())
        || Booking.STATUS_CANCELLED.equals(booking.getBookingStatus())
        || Booking.STATUS_NO_SHOW.equals(booking.getBookingStatus())) {
      ui.displayError("A " + booking.getBookingStatus()
          + " booking can no longer take a reward.");
      ui.displayMessage("  Rewards are enjoyed before or during the stay.");
      ui.pause();
      return;
    }

    Member member = data.findMemberByGuest(booking.getGuestId());
    if (member == null) {
      ui.displayError(guest.getFullName() + " is not a loyalty member yet.");
      ui.displayMessage("  Membership starts automatically at their first"
          + " check-out, so they");
      ui.displayMessage("  will be enrolled once this stay is complete.");
      ui.pause();
      return;
    }

    // --- Screen two: which reward? --------------------------------
    ui.clearScreen();
    ui.startAction("REQUEST A LOYALTY REWARD");
    ui.displayRedemptionHeader(booking, guest, member);

    ListInterface<Reward> catalogue = data.getRewardList();
    Reward chosen = ui.chooseRewardForMember(catalogue, member);
    if (chosen == null) {
      ui.displayMessage("  Nothing has been requested.");
      ui.pause();
      return;
    }

    ServiceResult<Redemption> requested =
        service.requestRedemption(member.getMemberId(), chosen.getRewardId());

    if (requested.isFailure()) {
      ui.displayError(requested.getMessage());
      ui.pause();
      return;
    }

    // The booking is remembered on the request, so Loyalty can see which stay
    // the reward belongs to when they come to approve it.
    Redemption redemption = requested.getValue();
    redemption.setBookingId(booking.getBookingId());
    data.saveLoyalty();

    ui.displayMessage("");
    ui.displaySuccess("Request " + redemption.getRedemptionId()
        + " submitted for " + chosen.getRewardName() + ".");
    ui.displayMessage("  It is waiting in the loyalty queue. No points have"
        + " been taken yet.");
    ui.displayMessage("  A loyalty officer approves it at Loyalty & Rewards >"
        + " Rewards &");
    ui.displayMessage("  redemptions > Process the next pending request.");
    ui.pause("Press ENTER to accept the request");
    ui.clearScreen();
  }

  /**
   * Finds a booking from the 8-digit number the guest is carrying.
   *
   * Looked up through the confirmation tree rather than by scanning, and a
   * number that names nothing is re-asked rather than ending the action.
   *
   * @return the booking, or null if the officer went back
   */
  private Booking promptByConfirmationNumber() {
    while (true) {
      String confirmation = ui.inputConfirmationNumber();
      if (confirmation == null) {
        return null;
      }

      Booking booking = data.findBookingByConfirmation(confirmation);
      if (booking != null) {
        return booking;
      }

      ui.displayError("No booking carries confirmation number " + confirmation + ".");
      ui.displayMessage("  Check the guest's confirmation slip, or enter 0 to"
          + " go back.");
    }
  }

  // ==================================================================
  // SEARCH
  // ==================================================================

  /**
   * Finds a booking by ID, using the tree rather than scanning the list.
   *
   * A number that names no booking is re-asked rather than ending the action,
   * so a typo costs one line instead of sending the officer back to the menu.
   *
   * @return the booking, or null if the user typed 0 to quit
   */
  private Booking promptForBooking() {
    while (true) {
      String bookingId = ui.inputBookingId();
      if (bookingId == null) {
        return null;
      }

      Booking booking = data.findBooking(bookingId);
      if (booking != null) {
        return booking;
      }

      ui.displayError("No booking with ID " + bookingId
          + ". Enter another number, or 0 to go back.");
    }
  }

  private void searchByBookingId() {
    // Redrawn for each number entered rather than ending after one lookup, so
    // working through several bookings does not mean leaving and re-entering
    // the screen between each.
    while (true) {
      ui.startAction("SEARCH BY BOOKING NUMBER");

      String bookingId = ui.inputBookingId();
      if (bookingId == null) {
        return;
      }

      Booking booking = data.findBooking(bookingId);
      if (booking == null) {
        ui.displayError("No booking " + bookingId
            + ". Enter another number, or 0 to go back.");
        ui.pause("Press ENTER to try another number");
        continue;
      }

      ui.displayBooking(booking, data);

      Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
      if (invoice != null) {
        ui.displayInvoice(invoice, service.paymentsFor(invoice.getInvoiceId()));
      }

      if (!ui.confirmAnother("Look up another booking number?")) {
        return;
      }
    }
  }

  /** Searches the confirmation-number tree used for fast caller lookup. */
  private void searchByConfirmationNumber() {
    while (true) {
      ui.startAction("SEARCH BY 8-DIGIT CONFIRMATION NUMBER");

      String confirmationNumber = ui.inputConfirmationNumber();
      if (confirmationNumber == null) {
        return;
      }

      Booking booking = data.findBookingByConfirmation(confirmationNumber);
      if (booking == null) {
        ui.displayError("No booking has confirmation number " + confirmationNumber
            + ". Enter another number, or 0 to go back.");
        ui.pause("Press ENTER to try another number");
        continue;
      }

      ui.displayBooking(booking, data);
      Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
      if (invoice != null) {
        ui.displayInvoice(invoice, service.paymentsFor(invoice.getInvoiceId()));
      }

      if (!ui.confirmAnother("Look up another confirmation number?")) {
        return;
      }
    }
  }

  private void searchByGuestName() {
    while (true) {
      ui.startAction("SEARCH BY GUEST NAME");

      String term = ui.inputGuestName();
      if (term == null) {
        return;
      }

      final String lower = term.toLowerCase();
      ListInterface<Booking> matches = data.getBookingList().filter(booking -> {
        Guest guest = data.findGuest(booking.getGuestId());
        return guest != null && guest.getFullName().toLowerCase().contains(lower);
      });

      // Newest first, so the booking just made is at the top.
      matches.sort(java.util.Comparator.comparing(Booking::getCreatedAt,
          java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));

      ui.displayBookingList(matches, data, "No booking matched \"" + term + "\".");

      if (!ui.confirmAnother("Search for another name?")) {
        return;
      }
    }
  }

  /**
   * Finds every booking for a room.
   *
   * The room is checked against the room table first, so a mistyped number is
   * reported as such rather than silently returning nothing.
   */
  private void searchByRoom() {
    ui.startAction("SEARCH BY ROOM NUMBER");

    String roomNo;
    while (true) {
      roomNo = ui.inputRoomNo();
      if (roomNo == null) {
        return;
      }
      if (data.findRoom(roomNo) != null) {
        break;
      }
      ui.displayError("Room " + roomNo
          + " does not exist. Enter another number, or 0 to go back.");
    }

    final String searchRoomNo = roomNo;
    ListInterface<Booking> matches = data.getBookingList().filter(
        booking -> searchRoomNo.equals(booking.getRoomNo()));

    ui.displayBookingList(matches, data, "Room " + roomNo + " has never been booked.");
    ui.pause();
  }

  private void filterByStatus() {
    while (true) {
      ui.startAction("FILTER BOOKINGS BY STATUS");

      String status = ui.inputStatusFilter();
      if (status == null) {
        return;
      }

      ListInterface<Booking> matches = data.getBookingList().filter(
          booking -> status.equals(booking.getBookingStatus()));

      matches.sort(java.util.Comparator.comparing(Booking::getCreatedAt,
          java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));

      ui.displayBookingList(matches, data, "No booking is " + status + ".");

      if (!ui.confirmAnother("Filter by another status?")) {
        return;
      }
    }
  }

  /**
   * Lists every booking in ID order.
   *
   * The order comes from walking the tree, so no sort is needed - keeping the
   * bookings in a search tree gives the sorted listing for nothing.
   */
  private void displayAllBookings() {
    ui.startAction("ALL BOOKINGS");
    ui.displayBookingList(data.getBookingsSorted(), data, "There are no bookings.");
    ui.pause();
  }

  // ==================================================================
  // REPORTS
  // ==================================================================

  /** How full the resort has been and what it earned. */
  private void occupancyRevenueReport() {
    ui.displayReportHeader("OCCUPANCY & REVENUE REPORT");

    ListInterface<Booking> bookings = data.getBookingList();
    if (bookings.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  There are no bookings to analyse.");
      ui.pause();
      return;
    }

    int confirmed = bookings.countIf(Booking::holdsRoom);
    int checkedOut = bookings.countIf(
        booking -> Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus()));
    int pending = bookings.countIf(
        booking -> Booking.STATUS_PENDING.equals(booking.getBookingStatus()));
    int cancelled = bookings.countIf(
        booking -> Booking.STATUS_CANCELLED.equals(booking.getBookingStatus()));

    ui.displaySectionHeading("Bookings");
    ui.displayReportLine("Total bookings", String.valueOf(bookings.getNumberOfEntries()));
    ui.displayReportLine("  Holding a room now", String.valueOf(confirmed));
    ui.displayReportLine("  Completed stays", String.valueOf(checkedOut));
    ui.displayReportLine("  Waiting for a room", String.valueOf(pending));
    ui.displayReportLine("  Cancelled", String.valueOf(cancelled));

    // Occupancy is measured in room-nights: how many were sold against how
    // many the resort had to sell. Counting bookings instead would treat a
    // one-night stay and a fortnight the same.
    int roomCount = data.getRoomList().getNumberOfEntries();
    int soldNights = 0;
    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking booking = bookings.getEntry(i);
      if (booking.holdsRoom()
          || Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus())) {
        soldNights += booking.getNumberOfNights();
      }
    }

    // Measured over the span the bookings actually cover.
    LocalDate earliest = null;
    LocalDate latest = null;
    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking booking = bookings.getEntry(i);
      if (earliest == null || booking.getCheckInDate().isBefore(earliest)) {
        earliest = booking.getCheckInDate();
      }
      if (latest == null || booking.getCheckOutDate().isAfter(latest)) {
        latest = booking.getCheckOutDate();
      }
    }

    long spanDays = (earliest == null) ? 0 : ChronoUnit.DAYS.between(earliest, latest);
    long availableNights = spanDays * roomCount;

    ui.displaySectionHeading("Occupancy");
    ui.displayReportLine("Rooms in the resort", String.valueOf(roomCount));
    ui.displayReportLine("Room-nights sold", String.valueOf(soldNights));
    ui.displayReportLine("Room-nights available", String.valueOf(availableNights));
    ui.displayReportLine("Occupancy rate", availableNights == 0 ? "-"
        : String.format("%.1f%%", (soldNights * 100.0) / availableNights));

    displayRevenue();
    displayOccupancyByType();
    displayBookingsBySource(bookings);

    ui.displayReportFooter();
  }

  /** What has been billed and what has been collected. */
  private void displayRevenue() {
    double billed = 0;
    double collected = 0;

    ListInterface<Invoice> invoices = data.getInvoiceList();
    for (int i = 1; i <= invoices.getNumberOfEntries(); i++) {
      Invoice invoice = invoices.getEntry(i);
      billed += invoice.getTotalAmount();
      collected += invoice.getAmountPaid();
    }

    ui.displaySectionHeading("Revenue");
    ui.displayReportLine("Total billed", String.format("RM%.2f", billed));
    ui.displayReportLine("Total collected", String.format("RM%.2f", collected));
    ui.displayReportLine("Still outstanding", String.format("RM%.2f", billed - collected));

    if (invoices.getNumberOfEntries() > 0) {
      ui.displayReportLine("Average invoice",
          String.format("RM%.2f", billed / invoices.getNumberOfEntries()));
    }
  }

  /** Which room types actually sell. */
  private void displayOccupancyByType() {
    ListInterface<RoomType> types = data.getRoomTypeList();
    String[] labels = new String[types.getNumberOfEntries()];
    double[] values = new double[types.getNumberOfEntries()];

    for (int i = 1; i <= types.getNumberOfEntries(); i++) {
      final String typeId = types.getEntry(i).getTypeId();
      labels[i - 1] = typeId;
      values[i - 1] = data.getBookingList().countIf(
          booking -> typeId.equals(booking.getTypeId())
              && (booking.holdsRoom()
                  || Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus())));
    }

    ui.displayBarChart("Bookings by room type", "Bookings", labels, values);
  }

  /** Where the bookings came from, and how many were urgent walk-ins. */
  private void displayBookingsBySource(ListInterface<Booking> bookings) {
    String[] sources = {
      Booking.SOURCE_WALK_IN, Booking.SOURCE_ONLINE,
      Booking.SOURCE_PHONE, Booking.SOURCE_CORPORATE
    };
    String[] labels = new String[sources.length];
    double[] values = new double[sources.length];

    for (int i = 0; i < sources.length; i++) {
      final String source = sources[i];
      labels[i] = source.replace("_", "");
      values[i] = bookings.countIf(booking -> source.equals(booking.getSource()));
    }

    ui.displayBarChart("Bookings by source", "Bookings", labels, values);

    int walkIns = bookings.countIf(
        booking -> Booking.SOURCE_WALK_IN.equals(booking.getSource()));
    int urgentWalkIns = bookings.countIf(
        booking -> Booking.SOURCE_WALK_IN.equals(booking.getSource()) && booking.isUrgent());

    ui.displayReportLine("Urgent share of walk-ins", walkIns == 0 ? "-"
        : String.format("%.1f%%  (%d of %d)",
            (urgentWalkIns * 100.0) / walkIns, urgentWalkIns, walkIns));
  }

  /**
   * What is still owed, and how long it has been owed for.
   *
   * The ageing bands are what turn a list of debts into something actionable:
   * a bill unpaid for a month needs chasing differently from one raised today.
   */
  private void outstandingBalanceReport() {
    ui.displayReportHeader("OUTSTANDING BALANCE REPORT");

    ListInterface<Invoice> unsettled = data.getInvoiceList().filter(
        invoice -> !invoice.isSettled());

    if (unsettled.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  Every invoice is settled. Nothing is outstanding.");
      ui.displayReportFooter();
      return;
    }

    ui.displaySectionHeading("Unsettled invoices");
    ui.displayTableHeading(String.format("  %-8s %-7s %-18s %10s %10s %-9s %s",
        "INVOICE", "BOOKING", "GUEST", "TOTAL", "OWED", "STATUS", "AGE"));

    double totalOwed = 0;
    double band1 = 0;
    double band2 = 0;
    double band3 = 0;
    int dataProblems = 0;

    for (int i = 1; i <= unsettled.getNumberOfEntries(); i++) {
      Invoice invoice = unsettled.getEntry(i);
      Booking booking = data.findBooking(invoice.getBookingId());
      Guest guest = (booking == null) ? null : data.findGuest(booking.getGuestId());

      long ageDays = ChronoUnit.DAYS.between(
          invoice.getIssuedAt().toLocalDate(), LocalDate.now());
      double owed = invoice.getOutstandingBalance();
      totalOwed += owed;

      if (ageDays <= 7) {
        band1 += owed;
      } else if (ageDays <= 30) {
        band2 += owed;
      } else {
        band3 += owed;
      }

      // Checking out with money still owed should be impossible, so if one
      // appears here it is a fault worth investigating rather than a debt.
      String flag = "";
      if (booking != null
          && Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus())) {
        flag = "  <-- checked out with a balance";
        dataProblems++;
      }

      System.out.printf("  %-8s %-7s %-18s %10.2f %10.2f %-9s %dd%s%n",
          invoice.getInvoiceId(), invoice.getBookingId(),
          guest == null ? "-" : truncate(guest.getFullName(), 18),
          invoice.getTotalAmount(), owed, invoice.getPaymentStatus(), ageDays, flag);
    }
    ui.displayThinRule();

    ui.displaySectionHeading("Summary");
    ui.displayReportLine("Unsettled invoices", String.valueOf(unsettled.getNumberOfEntries()));
    ui.displayReportLine("Total outstanding", String.format("RM%.2f", totalOwed));

    ui.displaySectionHeading("Ageing");
    ui.displayReportLine("0 to 7 days", String.format("RM%.2f", band1));
    ui.displayReportLine("8 to 30 days", String.format("RM%.2f", band2));
    ui.displayReportLine("Over 30 days", String.format("RM%.2f", band3));

    ui.displayBarChart("Outstanding by age", "RM",
        new String[] {"0-7", "8-30", "31+"},
        new double[] {band1, band2, band3});

    if (dataProblems > 0) {
      ui.displaySectionHeading("Data integrity");
      ui.displayReportLine("Checked out with a balance owing",
          dataProblems + "  (should be impossible - investigate)");
    }

    ui.displayReportFooter();
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
}