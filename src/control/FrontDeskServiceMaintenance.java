package control;

import adt.ListInterface;
import boundary.FrontDeskServiceUI;
import entity.Booking;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Invoice;
import entity.Payment;
import entity.Redemption;
import entity.Room;
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
 * @author Lim Yong Le
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
        default:
          break;
      }
    } while (choice != 0);
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
          requestUrgentCleaning();
          break;
        case 5:
          displayRoomBoard();
          break;
        case 6:
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
          applyLoyaltyDiscount();
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

    // A number that finds no bookable guest is re-asked rather than ending the
    // action, so the officer can try the passport after the IC without having
    // to come back through the menu.
    String icPassport;
    Guest guest;
    WalkInRegistration calledWalkIn;
    while (true) {
      icPassport = MessageUI.readIcPassport(MessageUI.scanner,
          "Guest IC / Passport number");
      if (MessageUI.isCancelled(icPassport)) {
        ui.displayMessage("  Booking cancelled.");
        ui.pause();
        return;
      }

      guest = data.findGuestByIc(icPassport);
      if (guest == null) {
        ui.displayError("No guest holds IC / Passport " + icPassport + ".");
        ui.displayMessage("  Register them in Walk-In Registration first,"
            + " or enter another number. 0 goes back.");
        continue;
      }

      // Only a guest who has been called to the counter can be booked. Anyone
      // still WAITING has not reached the desk yet, so their turn has not come.
      final String bookingGuestId = guest.getGuestId();
      calledWalkIn = data.getRegistrationList().search(
          reg -> bookingGuestId.equals(reg.getGuestId())
              && WalkInRegistration.STATUS_IN_SERVICE.equals(reg.getStatus()));

      if (calledWalkIn == null) {
        ui.displayError(guest.getFullName()
            + " has no walk-in registration waiting to be booked.");
        ui.displayMessage("  Only a guest called to the counter (IN_SERVICE)"
            + " can be booked.");
        ui.displayMessage("  Call them from the queue first, or enter another"
            + " number. 0 goes back.");
        continue;
      }

      break;
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
    ui.displayField("IC / Passport", icPassport);
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

    ui.displayMessage("");
    if (ui.confirm("Assign a room now?")) {
      assignRoomTo(booking);
    } else {
      ui.displayMessage("  The booking stays PENDING until a room is assigned.");
    }

    ui.pause();
  }

  /**
   * Changes a booking's dates or guest count.
   *
   * The room has to be checked again afterwards: dates that were free when the
   * booking was made may since have been taken by somebody else.
   */
  private void amendBooking() {
    ui.startAction("AMEND A BOOKING");

    Booking booking = promptForBooking();
    if (booking == null) {
      return;
    }
    if (Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus())
        || Booking.STATUS_CANCELLED.equals(booking.getBookingStatus())) {
      ui.displayError("A " + booking.getBookingStatus() + " booking cannot be amended.");
      ui.pause();
      return;
    }

    ui.displayBooking(booking, data);

    LocalDate checkIn = ui.inputDate("New check-in date");
    if (checkIn == null) {
      ui.displayMessage("  Amendment cancelled.");
      ui.pause();
      return;
    }

    LocalDate checkOut = ui.inputDate("New check-out date");
    if (checkOut == null) {
      ui.displayMessage("  Amendment cancelled.");
      ui.pause();
      return;
    }
    if (!checkOut.isAfter(checkIn)) {
      ui.displayError("The check-out date must be after the check-in date.");
      ui.pause();
      return;
    }

    RoomType type = data.findRoomType(booking.getTypeId());
    int guests = ui.inputGuestCount(type == null ? 6 : type.getMaxOccupancy());
    if (guests < 0) {
      ui.displayMessage("  Amendment cancelled.");
      ui.pause();
      return;
    }

    // A booking that already holds a room must not be moved onto dates
    // somebody else has taken.
    if (booking.getRoomNo() != null
        && service.hasDateClash(booking.getRoomNo(), checkIn, checkOut,
            booking.getBookingId())) {
      ui.displayError("Room " + booking.getRoomNo()
          + " is already booked over those dates. Move the guest first.");
      ui.pause();
      return;
    }

    booking.setCheckInDate(checkIn);
    booking.setCheckOutDate(checkOut);
    booking.setNumberOfGuests(guests);

    // The bill follows the nights actually booked.
    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice != null) {
      invoice.setRoomCharge(booking.getRatePerNight() * booking.getNumberOfNights());
      invoice.setAmountPaid(service.sumPayments(invoice.getInvoiceId()));
    }

    data.saveFrontDesk();
    ui.displaySuccess("Booking " + booking.getBookingId() + " amended.");
    ui.displayBooking(booking, data);
    ui.pause();
  }

  /**
   * Cancels a booking and gives up its room.
   *
   * If housekeeping was cleaning a room for this booking, that reservation is
   * released so the task falls back to the normal lane - but the cleaning goes
   * ahead, because the room is dirty whether or not anyone is waiting.
   */
  private void cancelBooking() {
    ui.startAction("CANCEL A BOOKING");

    Booking booking = promptForBooking();
    if (booking == null) {
      return;
    }
    if (Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())) {
      ui.displayError("This guest has already checked in. Check them out instead.");
      ui.pause();
      return;
    }
    if (Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus())
        || Booking.STATUS_CANCELLED.equals(booking.getBookingStatus())) {
      ui.displayError("This booking is already " + booking.getBookingStatus() + ".");
      ui.pause();
      return;
    }

    ui.displayBooking(booking, data);
    ui.displayMessage("");

    if (!ui.confirm("Cancel this booking?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    releaseRoomAndReservation(booking);
    booking.setBookingStatus(Booking.STATUS_CANCELLED);
    data.saveAll();

    ui.displaySuccess("Booking " + booking.getBookingId() + " cancelled.");
    ui.pause();
  }

  /**
   * Permanently removes only a booking that has not yet affected any room,
   * invoice, or walk-in record.  Once a booking is connected to operational
   * data, cancellation preserves the audit trail instead.
   */
  private void deleteBooking() {
    ui.startAction("DELETE AN UNASSIGNED BOOKING");

    Booking booking = promptForBooking();
    if (booking == null) {
      return;
    }
    if (!Booking.STATUS_PENDING.equals(booking.getBookingStatus())
        || booking.getRoomNo() != null
        || booking.getRegId() != null
        || data.findInvoiceByBooking(booking.getBookingId()) != null) {
      ui.displayError("Only an unassigned standalone pending booking can be deleted.");
      ui.displayMessage("  Cancel this booking instead to keep its operational history.");
      ui.pause();
      return;
    }

    ui.displayBooking(booking, data);
    if (!ui.confirm("Permanently delete this booking?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    data.removeBooking(booking);
    data.saveFrontDesk();
    ui.displaySuccess("Booking " + booking.getBookingId() + " deleted.");
    ui.pause();
  }

  /** Records that a confirmed guest never arrived. */
  private void markNoShow() {
    ui.startAction("MARK A BOOKING AS NO-SHOW");

    Booking booking = promptForBooking();
    if (booking == null) {
      return;
    }
    if (!Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus())) {
      ui.displayError("Only a CONFIRMED booking can be a no-show - this one is "
          + booking.getBookingStatus() + ".");
      ui.pause();
      return;
    }

    ui.displayBooking(booking, data);
    ui.displayMessage("");

    if (!ui.confirm("Mark this booking as a no-show?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    releaseRoomAndReservation(booking);
    booking.setBookingStatus(Booking.STATUS_NO_SHOW);
    data.saveAll();

    ui.displaySuccess("Booking " + booking.getBookingId() + " recorded as a no-show.");
    ui.pause();
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

    ListInterface<Room> ready = service.findAvailableRooms(typeId, checkIn, checkOut);
    ui.displaySectionHeading("Ready to assign now");
    if (ready.isEmpty()) {
      ui.displayMessage("  No " + typeId + " room is ready for those dates.");
    } else {
      ui.displayRoomBoard(ready, data);
    }

    ListInterface<Room> cleanable = service.findCleanableRooms(typeId, checkIn, checkOut);
    if (!cleanable.isEmpty()) {
      ui.displaySectionHeading("Could be ready once cleaned");
      ui.displayRoomBoard(cleanable, data);
      ui.displayMessage("  Closest to ready is listed first.");
    }

    ui.pause();
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

      assignRoomTo(pending.getEntry(position));

      if (!ui.confirmAnother("Give a room to another booking?")) {
        return;
      }
    }
  }

  /**
   * Finds and assigns a room for a booking.
   *
   * When nothing is ready the officer is offered the cleaning route rather
   * than simply being refused, which is what stops a guest being turned away
   * over a room that is twenty minutes from ready.
   */
  private void assignRoomTo(Booking booking) {
    ListInterface<Room> available = service.findAvailableRooms(
        booking.getTypeId(), booking.getCheckInDate(), booking.getCheckOutDate());

    if (!available.isEmpty()) {
      String roomNo = ui.chooseRoom(available);
      if (roomNo == null) {
        ui.displayMessage("  Assignment cancelled.");
        return;
      }

      ServiceResult<Booking> assigned = service.assignRoom(booking.getBookingId(),
          roomNo, staffId, RoomAssignment.REASON_INITIAL);

      if (assigned.isSuccess()) {
        ui.displaySuccess(assigned.getMessage());
        ui.displayBooking(booking, data);
      } else {
        ui.displayError(assigned.getMessage());
      }
      return;
    }

    ui.displayError("No " + booking.getTypeId() + " room is ready for those dates.");

    ListInterface<Room> cleanable = service.findCleanableRooms(
        booking.getTypeId(), booking.getCheckInDate(), booking.getCheckOutDate());

    if (cleanable.isEmpty()) {
      ui.displayMessage("  No room of that type can be made ready either.");
      ui.displayMessage("  Offer a different room type, or cancel the booking.");
      return;
    }

    ui.displaySectionHeading("Rooms that could be cleaned for this booking");
    ui.displayRoomBoard(cleanable, data);

    if (ui.confirm("Ask housekeeping to prepare one of these?")) {
      ServiceResult<HousekeepingTask> expedited =
          service.requestUrgentCleaning(booking.getBookingId(), staffId);

      if (expedited.isSuccess()) {
        ui.displaySuccess(expedited.getMessage());
        ui.displayMessage("  The booking stays PENDING until the room is ready.");
      } else {
        ui.displayError(expedited.getMessage());
      }
    }
  }

  /** Moves a guest to a different room, keeping the history of both. */
  private void moveBookingToAnotherRoom() {
    ui.startAction("MOVE A BOOKING TO ANOTHER ROOM");

    Booking booking = promptForBooking();
    if (booking == null) {
      return;
    }
    if (booking.getRoomNo() == null) {
      ui.displayError("This booking has no room to move from.");
      ui.pause();
      return;
    }
    if (!booking.holdsRoom()) {
      ui.displayError("A " + booking.getBookingStatus() + " booking cannot be moved.");
      ui.pause();
      return;
    }

    ui.displayBooking(booking, data);

    ListInterface<Room> available = service.findAvailableRooms(
        booking.getTypeId(), booking.getCheckInDate(), booking.getCheckOutDate());
    if (available.isEmpty()) {
      ui.displayError("No other room of that type is ready.");
      ui.pause();
      return;
    }

    String roomNo = ui.chooseRoom(available);
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

  /** Asks housekeeping to prepare a room out of turn for a waiting booking. */
  private void requestUrgentCleaning() {
    ui.startAction("REQUEST URGENT CLEANING");

    ListInterface<Booking> pending = data.getBookingList().filter(
        booking -> Booking.STATUS_PENDING.equals(booking.getBookingStatus()));

    if (!ui.displayBookingList(pending, data, "No booking is waiting for a room.")) {
      ui.pause();
      return;
    }

    Booking booking = promptForBooking();
    if (booking == null) {
      return;
    }

    ServiceResult<HousekeepingTask> expedited =
        service.requestUrgentCleaning(booking.getBookingId(), staffId);

    if (expedited.isSuccess()) {
      ui.displaySuccess(expedited.getMessage());
      if (booking.isUrgent()) {
        ui.displayMessage("  The booking is URGENT, so the task went to the urgent lane.");
      } else {
        ui.displayMessage("  The booking is NORMAL, so the task stays in the normal lane.");
      }
    } else {
      ui.displayError(expedited.getMessage());
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

    if (!ui.displayBookingList(confirmed, data, "No booking is waiting to check in.")) {
      ui.pause();
      return;
    }

    Booking booking = promptForBooking();
    if (booking == null) {
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

    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice != null && !invoice.isSettled()) {
      ui.displayMessage("");
      if (ui.confirm(String.format("Take a payment now? RM%.2f is outstanding.",
          invoice.getOutstandingBalance()))) {
        takePaymentFor(invoice);
      }
    }
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

    if (!ui.displayBookingList(stayed, data, "No guest is currently checked in.")) {
      ui.pause();
      return;
    }

    Booking booking = promptForBooking();
    if (booking == null) {
      return;
    }

    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice == null) {
      ui.displayError("This booking has no invoice.");
      ui.pause();
      return;
    }

    ui.displayInvoice(invoice, service.paymentsFor(invoice.getInvoiceId()));

    // The bill has to be settled first: once the guest has gone there is
    // nobody left to collect from.
    if (!invoice.isSettled()) {
      ui.displayMessage("");
      ui.displayError(String.format("RM%.2f is still outstanding.",
          invoice.getOutstandingBalance()));

      if (!ui.confirm("Settle the bill now?")) {
        ui.displayMessage("  Check-out abandoned - the bill is still open.");
        ui.pause();
        return;
      }

      takePaymentFor(invoice);
      if (!invoice.isSettled()) {
        ui.displayError("The bill is still not settled. Check-out abandoned.");
        ui.pause();
        return;
      }
    }

    ui.displayMessage("");
    if (!ui.confirm("Check this guest out?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    ServiceResult<Booking> result = service.checkOut(booking.getBookingId(), staffId);
    if (result.isFailure()) {
      ui.displayError(result.getMessage());
      ui.pause();
      return;
    }

    ui.displaySuccess(result.getMessage());
    ui.displayMessage("");
    ui.displayMessage("  The room has been sent to housekeeping as DIRTY.");

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

  private void viewInvoice() {
    ui.startAction("VIEW AN INVOICE");

    Booking booking = promptForBooking();
    if (booking == null) {
      return;
    }

    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    if (invoice == null) {
      ui.displayError("This booking has no invoice - no room has been assigned yet.");
      ui.pause();
      return;
    }

    ui.displayInvoice(invoice, service.paymentsFor(invoice.getInvoiceId()));
    ui.pause();
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

    double amount = ui.inputAmount(String.format("Amount received (RM%.2f outstanding)",
        invoice.getOutstandingBalance()));
    if (amount == MessageUI.CANCELLED_AMOUNT) {
      ui.displayMessage("  Payment cancelled.");
      return;
    }

    String method = ui.inputPaymentMethod();
    if (method == null) {
      ui.displayMessage("  Payment cancelled.");
      return;
    }

    String reference = null;
    if (Payment.requiresReference(method)) {
      reference = ui.inputPaymentReference();
      if (reference == null) {
        ui.displayMessage("  Payment cancelled.");
        return;
      }
    }

    ServiceResult<Payment> result = service.recordPayment(invoice.getInvoiceId(),
        amount, method, reference, staffId);

    if (result.isSuccess()) {
      ui.displaySuccess(result.getMessage());
    } else {
      ui.displayError(result.getMessage());
    }
  }

  /** Takes an approved loyalty reward off a live bill. */
  private void applyLoyaltyDiscount() {
    ui.startAction("APPLY A LOYALTY REWARD TO A BILL");

    ListInterface<Redemption> approved = data.getRedemptionList().filter(
        redemption -> Redemption.APPROVED.equals(redemption.getStatus())
            && redemption.getInvoiceId() == null);

    if (approved.isEmpty()) {
      ui.displayMessage("  There is no approved reward waiting to be applied.");
      ui.pause();
      return;
    }

    ui.displaySectionHeading("Approved rewards not yet applied");
    ui.displayTableHeading(String.format("  %-8s %-7s %-30s %s",
        "REDEEM", "MEMBER", "REWARD", "VALUE"));

    for (int i = 1; i <= approved.getNumberOfEntries(); i++) {
      Redemption redemption = approved.getEntry(i);
      entity.Reward reward = data.findReward(redemption.getRewardId());

      System.out.printf("  %-8s %-7s %-30s RM%.2f%n",
          redemption.getRedemptionId(), redemption.getMemberId(),
          reward == null ? redemption.getRewardId() : reward.getRewardName(),
          reward == null ? 0.0 : reward.getCashValue());
    }
    ui.displayThinRule();

    String redemptionId = MessageUI.readRequiredText(MessageUI.scanner,
        "Redemption ID (e.g. RD0002)");
    if (MessageUI.isCancelled(redemptionId)) {
      return;
    }

    String bookingId = ui.inputBookingId();
    if (bookingId == null) {
      return;
    }

    ServiceResult<Invoice> result =
        service.applyRedemptionToInvoice(redemptionId, bookingId);

    if (result.isSuccess()) {
      ui.displaySuccess(result.getMessage());
      ui.displayInvoice(result.getValue(),
          service.paymentsFor(result.getValue().getInvoiceId()));
    } else {
      ui.displayError(result.getMessage());
    }
    ui.pause();
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
    double discounts = 0;

    ListInterface<Invoice> invoices = data.getInvoiceList();
    for (int i = 1; i <= invoices.getNumberOfEntries(); i++) {
      Invoice invoice = invoices.getEntry(i);
      billed += invoice.getTotalAmount();
      collected += invoice.getAmountPaid();
      discounts += invoice.getDiscountAmount();
    }

    ui.displaySectionHeading("Revenue");
    ui.displayReportLine("Total billed", String.format("RM%.2f", billed));
    ui.displayReportLine("Total collected", String.format("RM%.2f", collected));
    ui.displayReportLine("Still outstanding", String.format("RM%.2f", billed - collected));
    ui.displayReportLine("Given away as loyalty discounts",
        String.format("RM%.2f", discounts));

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

  private String truncate(String text, int width) {
    if (text == null) {
      return "-";
    }
    return (text.length() <= width) ? text : text.substring(0, width - 1) + ".";
  }
}
