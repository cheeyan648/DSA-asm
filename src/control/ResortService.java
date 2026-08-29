package control;

import adt.ArrayList;
import adt.ListInterface;
import entity.Booking;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Invoice;
import entity.Member;
import entity.Payment;
import entity.PointTransaction;
import entity.Redemption;
import entity.Reward;
import entity.Room;
import entity.RoomArrangement;
import entity.RoomAssignment;
import entity.RoomStatusLog;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Every operation that touches more than one module.
 *
 * These live together rather than inside the four maintenance classes because
 * each one is a single piece of work that spans them: checking a guest out
 * ends a stay, dirties a room, raises a cleaning task and awards points, and
 * those four things either all happen or the system is left inconsistent.
 * Putting them here also keeps the modules from calling into each other, which
 * is what made the original code impossible to follow.
 *
 * The rule that runs through all of it: a room may only be given to a guest
 * when Front-Desk says it is free for the dates AND Housekeeping says it is
 * clean. Neither module can answer that alone, which is exactly why the
 * separate versions of this system could sell a dirty room.
 *
 * SHARED BY ALL FOUR MODULES
 * This is not any one member's module. It holds the rules that need more
 * than one module to answer - room availability, check-out, points awarded
 * from a settled bill - so no module has to reach into another's data.
 * Each module calls the operations it needs; none of them calls each other.
 *
 * @author Wong Chee Yan
 */
public class ResortService {

  /** Points earned per ringgit spent, before the tier multiplier. */
  public static final double POINTS_PER_RINGGIT = 1.0;

  private final ResortData data;

  public ResortService(ResortData data) {
    this.data = data;
  }

  public ResortData getData() {
    return data;
  }

  // ==================================================================
  // GUESTS
  // ==================================================================

  /**
   * Finds the guest behind an identity document, or creates one.
   *
   * Matching on the document rather than the name is what stops one person
   * becoming two records because they gave their name slightly differently on
   * a second visit.
   *
   * @return the existing or newly created guest
   */
  public Guest findOrCreateGuest(String icPassportNo, String fullName,
      String contactNumber, String email) {
    Guest existing = data.findGuestByIc(icPassportNo);
    if (existing != null) {
      return existing;
    }

    Guest guest = new Guest(data.nextGuestId(), fullName, icPassportNo,
        contactNumber, email, LocalDateTime.now());
    data.addGuest(guest);
    return guest;
  }

  /** A guest's name, or a dash when the record cannot be found. */
  public String guestNameOf(String guestId) {
    Guest guest = data.findGuest(guestId);
    return (guest == null) ? "-" : guest.getFullName();
  }

  // ==================================================================
  // ROOM AVAILABILITY - the M2/M3 boundary
  // ==================================================================

  /**
   * Whether a room can be given to a guest for a date range.
   *
   * Four things must hold: the room is in service, nobody is in it, it has
   * been cleaned, and no existing booking already covers those dates. The
   * first three are properties of the room and the fourth depends on the
   * dates, which is why the check cannot live on the room alone.
   *
   * @param roomNo the room being considered
   * @param checkIn the arrival date
   * @param checkOut the departure date
   * @param exceptBookingId a booking to ignore when checking for clashes, so a
   * booking does not conflict with itself while being amended
   * @return true if the room may be assigned
   */
  public boolean isRoomAvailable(String roomNo, LocalDate checkIn, LocalDate checkOut,
      String exceptBookingId) {
    Room room = data.findRoom(roomNo);
    if (room == null || !room.isAssignable()) {
      return false;
    }
    return !hasDateClash(roomNo, checkIn, checkOut, exceptBookingId);
  }

  /**
   * Whether an existing booking already holds a room over a date range.
   *
   * Only confirmed and checked-in bookings count. A cancelled or no-show
   * booking must not block its room, or the room could never be sold again.
   */
  public boolean hasDateClash(String roomNo, LocalDate checkIn, LocalDate checkOut,
      String exceptBookingId) {
    ListInterface<Booking> bookings = data.getBookingList();
    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking booking = bookings.getEntry(i);
      if (!booking.holdsRoom() || !roomNo.equals(booking.getRoomNo())) {
        continue;
      }
      if (exceptBookingId != null && exceptBookingId.equals(booking.getBookingId())) {
        continue;
      }
      if (booking.overlaps(checkIn, checkOut)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Every room of a type that could be given to a guest for those dates.
   *
   * All of them are returned rather than just the first, because the officer
   * may have a reason to prefer one - a floor, a view, a room away from the
   * lift.
   */
  public ListInterface<Room> findAvailableRooms(String typeId, LocalDate checkIn,
      LocalDate checkOut) {
    ListInterface<Room> available = new ArrayList<>();
    ListInterface<Room> rooms = data.getRoomList();

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (typeId != null && !typeId.equals(room.getTypeId())) {
        continue;
      }
      if (isRoomAvailable(room.getRoomNo(), checkIn, checkOut, null)) {
        available.add(room);
      }
    }
    return available;
  }

  /**
   * Rooms that only need cleaning before they could be given to a guest.
   *
   * Offered when nothing is ready and someone is waiting: a dirty room can
   * still be promised if housekeeping is asked to clean it out of turn. They
   * come back closest-to-ready first, so the room that will free up soonest is
   * the one offered.
   */
  public ListInterface<Room> findCleanableRooms(String typeId, LocalDate checkIn,
      LocalDate checkOut) {
    ListInterface<Room> cleanable = new ArrayList<>();
    ListInterface<Room> rooms = data.getRoomList();

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (typeId != null && !typeId.equals(room.getTypeId())) {
        continue;
      }
      if (room.isCleanable() && !hasDateClash(room.getRoomNo(), checkIn, checkOut, null)) {
        cleanable.add(room);
      }
    }

    cleanable.sort(Comparator.comparingInt(Room::getReadinessRank));
    return cleanable;
  }

  /** The most rooms a single party may be split across. */
  public static final int MAX_ROOMS_PER_PARTY = 3;

  /**
   * Ways of housing a party when no single room of their type will do.
   *
   * Rather than turning a party away because the type they asked for is gone,
   * the rooms that ARE free are combined until they hold everybody: six guests
   * become three Standard Twins, or a Deluxe King and a Standard Twin. Only
   * arrangements where every room is needed are returned - offering four twins
   * to a party of six would waste a room somebody else could have - and they
   * come back cheapest first, so the guest is offered the best price.
   *
   * @param guests how many people are staying
   * @param checkIn the first night
   * @param checkOut the morning they leave
   * @return the arrangements that would house them, cheapest first
   */
  public ListInterface<RoomArrangement> findRoomArrangements(int guests,
      LocalDate checkIn, LocalDate checkOut) {
    ListInterface<RoomArrangement> found = new ArrayList<>();
    if (guests < 1) {
      return found;
    }

    // Every room that is free and clean for the dates, whatever its type. A
    // room still waiting on housekeeping is not offered: cleaning work is
    // raised and ordered at check-out, not asked for from here.
    ListInterface<Room> free = findAvailableRooms(null, checkIn, checkOut);

    if (free.isEmpty()) {
      return found;
    }

    buildArrangements(free, guests, 1, new RoomArrangement(), found);
    found.sort(Comparator.comparingDouble(RoomArrangement::getTotalRatePerNight));
    return found;
  }

  /**
   * Walks the free rooms, growing one arrangement at a time.
   *
   * Rooms are only ever added in increasing position, so each combination is
   * reached once rather than once per ordering. A branch stops as soon as it
   * houses the party, because adding another room to an arrangement that
   * already fits could only make it wasteful.
   */
  private void buildArrangements(ListInterface<Room> free, int guests, int from,
      RoomArrangement sofar, ListInterface<RoomArrangement> found) {
    if (sofar.getRoomCount() >= MAX_ROOMS_PER_PARTY) {
      return;
    }

    for (int i = from; i <= free.getNumberOfEntries(); i++) {
      Room room = free.getEntry(i);
      RoomType type = data.findRoomType(room.getTypeId());

      RoomArrangement extended = copyOfArrangement(sofar);
      extended.add(room, type);

      if (extended.holds(guests)) {
        // Same-type rooms are interchangeable, so keep only the first
        // arrangement of any given shape rather than one per room number.
        if (extended.isMinimalFor(guests, this::typeOfRoom)
            && !hasSameShape(found, extended)) {
          found.add(extended);
        }
        continue;
      }

      buildArrangements(free, guests, i + 1, extended, found);
    }
  }

  private RoomArrangement copyOfArrangement(RoomArrangement source) {
    RoomArrangement copy = new RoomArrangement();
    ListInterface<Room> rooms = source.getRooms();
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      copy.add(room, data.findRoomType(room.getTypeId()));
    }
    return copy;
  }

  private RoomType typeOfRoom(Room room) {
    return (room == null) ? null : data.findRoomType(room.getTypeId());
  }

  /** Whether an equivalent arrangement - same types, same counts - is already listed. */
  private boolean hasSameShape(ListInterface<RoomArrangement> found,
      RoomArrangement candidate) {
    // Compared on the sorted key rather than the written description, so a
    // twin-then-king arrangement is recognised as the king-then-twin one.
    String shape = candidate.shapeKey(this::typeOfRoom);
    for (int i = 1; i <= found.getNumberOfEntries(); i++) {
      if (shape.equals(found.getEntry(i).shapeKey(this::typeOfRoom))) {
        return true;
      }
    }
    return false;
  }

  // ==================================================================
  // FLOW A - walk-in becomes a booking
  // ==================================================================

  /**
   * Calls the next guest from the waiting list.
   *
   * The urgent lane is drained before the normal one, and within a lane the
   * earliest arrival goes first. Once called the guest leaves the queue for
   * good: the status becomes IN_SERVICE and there is no way back into the
   * line, so a guest cannot be both waiting and being served.
   *
   * @param staffId the officer calling them
   * @return the registration now being served
   */
  public ServiceResult<WalkInRegistration> serveNextGuest(String staffId) {
    // Registration and booking run at their own pace. Calling a guest through
    // does not wait on the last one being booked: several can be served and
    // sent over, and the front desk works through them in priority order. A
    // guest already called simply stays IN_SERVICE until a booking is made.
    WalkInRegistration next = data.getWaitingList().next();
    if (next == null) {
      return ServiceResult.fail("No guests are waiting.");
    }

    next.leaveQueue(WalkInRegistration.STATUS_IN_SERVICE, LocalDateTime.now());
    next.setServedBy(staffId);
    data.saveRegistrations();

    return ServiceResult.ok("Now serving " + guestNameOf(next.getGuestId())
        + " (" + next.getRegId() + ").", next);
  }

  /**
   * The guest currently at the counter, if anyone is.
   *
   * A registration is IN_SERVICE from the moment it is called until a booking
   * is made from it, so one sitting in that state is a guest whose business is
   * still unfinished - whatever interrupted it.
   *
   * @return the registration being served, or null if the counter is free
   */
  public WalkInRegistration findGuestAtCounter() {
    return data.getRegistrationList().search(
        reg -> WalkInRegistration.STATUS_IN_SERVICE.equals(reg.getStatus()));
  }

  /**
   * Turns a registration being served into a booking.
   *
   * The booking is recorded before any room is looked for, which is why it
   * starts PENDING with no room. The urgency the guest was given at the door
   * is copied onto the booking rather than decided again here, so it travels
   * unchanged into housekeeping.
   *
   * @param regId the registration to convert
   * @param checkIn the arrival date
   * @param checkOut the departure date
   * @param numberOfGuests how many people will stay
   * @param staffId the officer creating it
   * @return the new booking
   */
  public ServiceResult<Booking> convertRegistrationToBooking(String regId,
      LocalDate checkIn, LocalDate checkOut, int numberOfGuests, String staffId) {
    WalkInRegistration reg = data.findRegistration(regId);
    if (reg == null) {
      return ServiceResult.fail("Registration " + regId + " was not found.");
    }
    if (!WalkInRegistration.STATUS_IN_SERVICE.equals(reg.getStatus())) {
      return ServiceResult.fail("Only a guest currently being served can be booked in.");
    }

    RoomType type = data.findRoomType(reg.getRequestedTypeId());
    if (type == null) {
      return ServiceResult.fail("Room type " + reg.getRequestedTypeId() + " no longer exists.");
    }
    if (numberOfGuests > type.getMaxOccupancy()) {
      return ServiceResult.fail(type.getTypeName() + " takes at most "
          + type.getMaxOccupancy() + " guests.");
    }
    if (!checkOut.isAfter(checkIn)) {
      return ServiceResult.fail("The departure date must be after the arrival date.");
    }

    Booking booking = new Booking(data.nextBookingId(), reg.getGuestId(),
        reg.getRequestedTypeId(), checkIn, checkOut, numberOfGuests,
        reg.getPriority(), Booking.SOURCE_WALK_IN, reg.getRegId(),
        type.getBaseRatePerNight(), LocalDateTime.now(), staffId);
    data.addBooking(booking);

    reg.setStatus(WalkInRegistration.STATUS_BOOKED);
    reg.setBookedAt(LocalDateTime.now());
    reg.setBookingId(booking.getBookingId());

    data.saveRegistrations();
    data.saveFrontDesk();

    return ServiceResult.ok("Booking " + booking.getBookingId()
        + " created for " + guestNameOf(reg.getGuestId()) + ".", booking);
  }

  /**
   * Gives a room to a booking and raises its bill.
   *
   * This is the point where a PENDING booking becomes CONFIRMED: the room is
   * held, the assignment is recorded so the room's history survives a later
   * move, and the bill is priced from the nights actually booked.
   *
   * @param bookingId the booking to place
   * @param roomNo the room to give it
   * @param staffId the officer assigning it
   * @param reason why this room, for the assignment history
   * @return the booking, now confirmed
   */
  public ServiceResult<Booking> assignRoom(String bookingId, String roomNo,
      String staffId, String reason) {
    Booking booking = data.findBooking(bookingId);
    if (booking == null) {
      return ServiceResult.fail("Booking " + bookingId + " was not found.");
    }
    if (Booking.STATUS_CANCELLED.equals(booking.getBookingStatus())
        || Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus())) {
      return ServiceResult.fail("A " + booking.getBookingStatus()
          + " booking cannot be given a room.");
    }

    Room room = data.findRoom(roomNo);
    if (room == null) {
      return ServiceResult.fail("Room " + roomNo + " does not exist.");
    }
    if (room.isOutOfService()) {
      return ServiceResult.fail("Room " + roomNo + " is out of service.");
    }
    if (!Room.READY_FOR_CHECK_IN.equals(room.getHousekeepingStatus())) {
      return ServiceResult.fail("Room " + roomNo + " is not ready - housekeeping status is "
          + room.getHousekeepingStatus() + ".");
    }
    if (!Room.VACANT.equals(room.getOccupancyStatus())) {
      return ServiceResult.fail("Room " + roomNo + " is " + room.getOccupancyStatus() + ".");
    }
    if (hasDateClash(roomNo, booking.getCheckInDate(), booking.getCheckOutDate(), bookingId)) {
      return ServiceResult.fail("Room " + roomNo + " is already booked over those dates.");
    }

    // A booking being moved gives up its old room first, and that room is
    // dirtied - somebody has been in it.
    RoomAssignment open = data.findOpenAssignment(bookingId);
    if (open != null) {
      open.setReleasedAt(LocalDateTime.now());
      releaseRoomForCleaning(open.getRoomNo(), bookingId, staffId);
    }

    data.getAssignmentList().add(new RoomAssignment(data.nextAssignmentId(), bookingId,
        roomNo, LocalDateTime.now(), staffId, reason));

    booking.setRoomNo(roomNo);
    if (Booking.STATUS_PENDING.equals(booking.getBookingStatus())) {
      booking.setBookingStatus(Booking.STATUS_CONFIRMED);
    }
    room.setOccupancyStatus(Room.RESERVED);

    // The bill is raised once, when the room is first given.
    Invoice invoice = data.findInvoiceByBooking(bookingId);
    if (invoice == null) {
      double roomCharge = booking.getRatePerNight() * booking.getNumberOfNights();
      invoice = new Invoice(data.nextInvoiceId(), bookingId, roomCharge, LocalDateTime.now());
      data.getInvoiceList().add(invoice);
    }

    // Nobody is waiting on this room any more.
    HousekeepingTask reserved = data.getTaskList().search(
        task -> bookingId.equals(task.getReservedForBookingId()));
    if (reserved != null) {
      reserved.setReservedForBookingId(null);
      reserved.setPriority(HousekeepingTask.PRIORITY_NORMAL);
    }

    data.saveFrontDesk();
    data.saveMasters();
    data.saveHousekeeping();

    return ServiceResult.ok("Room " + roomNo + " assigned to " + bookingId
        + ". Invoice " + invoice.getInvoiceId() + " raised.", booking);
  }


  /**
   * Sets a task's lane from whether a booking is waiting on its room.
   *
   * Priority is never typed in, and it is not inherited from how the guest
   * joined the walk-in queue: urgency at the door decides who is served first
   * at the counter, not what housekeeping cleans first. What matters here is
   * only whether somebody is waiting to move in. A room with a live booking
   * against it is cleaned ahead of the rest, whether that guest walked in as
   * a normal or an urgent case, because either way they cannot check in until
   * it is done. Everything else is cleaned in housekeeping's own order.
   *
   * If the waiting booking is cancelled the task falls back to the normal
   * lane, but the cleaning still goes ahead, because the room is dirty either
   * way.
   */
  public void refreshTaskPriority(HousekeepingTask task) {
    String reservedFor = task.getReservedForBookingId();
    String updated = HousekeepingTask.PRIORITY_NORMAL;

    if (reservedFor != null) {
      Booking waiting = data.findBooking(reservedFor);
      if (waiting != null
          && !Booking.STATUS_CANCELLED.equals(waiting.getBookingStatus())) {
        updated = HousekeepingTask.PRIORITY_URGENT;
      }
    }

    if (!updated.equals(task.getPriority())) {
      task.setPriority(updated);
      // The lane is part of the queue, not the task, so a task already waiting
      // has to be taken out and put back to move lanes.
      if (task.isPendingCleaning() && data.getCleaningQueue().removeEntry(task)) {
        enqueueIfNeedsCleaning(task);
      }
    }
  }

  // ==================================================================
  // ROOM MANAGEMENT
  //
  // Rooms are the thing both modules argue over: the front desk wants to sell
  // them and housekeeping decides whether they may be sold. Creating and
  // retiring them lives here, in the one place both can see, so a room cannot
  // exist for one module and not the other.
  // ==================================================================

  /**
   * Adds a room to the resort.
   *
   * A new room starts DIRTY rather than ready. Nobody has prepared it yet, and
   * a room that could be sold the moment it is keyed in is exactly how a guest
   * ends up in a room nobody made up - so it goes through housekeeping like
   * any other dirty room before it can be given out.
   *
   * @param roomNo the room number, unique across the resort
   * @param typeId the room type it belongs to
   * @param floorNo which floor it is on
   * @param staffId who is adding it
   * @return the new room
   */
  public ServiceResult<Room> addRoom(String roomNo, String typeId, int floorNo,
      String staffId) {
    if (roomNo == null || roomNo.isBlank()) {
      return ServiceResult.fail("A room number is needed.");
    }
    if (data.findRoom(roomNo) != null) {
      return ServiceResult.fail("Room " + roomNo + " already exists.");
    }
    if (data.findRoomType(typeId) == null) {
      return ServiceResult.fail("There is no room type " + typeId + ".");
    }

    Room room = new Room(roomNo, typeId, floorNo, Room.VACANT, Room.DIRTY,
        false, null, "Added " + LocalDate.now());
    data.getRoomList().add(room);

    // Preparing it is a cleaning job like any other, which is what puts the
    // new room in front of housekeeping instead of leaving it as a room
    // nobody has been told about.
    HousekeepingTask task = new HousekeepingTask(data.nextTaskId(), roomNo,
        HousekeepingTask.TYPE_DEEP_CLEAN, null, LocalDateTime.now());
    task.setRemark("New room - prepare before first sale");
    data.getTaskList().add(task);
    enqueueIfNeedsCleaning(task);
    logStatusChange(task, null, HousekeepingTask.DIRTY, staffId, false,
        "Room added");

    data.saveMasters();
    data.saveHousekeeping();

    return ServiceResult.ok("Room " + roomNo + " added and sent to housekeeping"
        + " to be prepared (task " + task.getTaskId() + ").", room);
  }

  /**
   * Removes a room from the resort.
   *
   * Refused while anybody is in it or booked into it. A room number that
   * disappeared from under a live booking would leave that booking pointing at
   * nothing, which is worse than keeping a room that is no longer wanted.
   *
   * @param roomNo the room to remove
   * @return what happened
   */
  public ServiceResult<Room> removeRoom(String roomNo) {
    Room room = data.findRoom(roomNo);
    if (room == null) {
      return ServiceResult.fail("There is no room " + roomNo + ".");
    }
    if (Room.OCCUPIED.equals(room.getOccupancyStatus())) {
      return ServiceResult.fail("Room " + roomNo
          + " has a guest in it and cannot be removed.");
    }

    Booking held = data.getBookingList().search(
        booking -> roomNo.equals(booking.getRoomNo())
            && !Booking.STATUS_CANCELLED.equals(booking.getBookingStatus())
            && !Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus()));
    if (held != null) {
      return ServiceResult.fail("Booking " + held.getBookingId()
          + " is holding room " + roomNo + ". Move it first.");
    }

    // Any cleaning still queued for it is pointless once the room is gone.
    ListInterface<HousekeepingTask> queued = data.getTaskList().filter(
        task -> roomNo.equals(task.getRoomNo()) && task.isPendingCleaning());
    for (int i = 1; i <= queued.getNumberOfEntries(); i++) {
      data.getCleaningQueue().removeEntry(queued.getEntry(i));
    }

    data.getRoomList().removeEntry(room);
    data.saveMasters();
    data.saveHousekeeping();

    return ServiceResult.ok("Room " + roomNo + " removed. "
        + queued.getNumberOfEntries() + " queued cleaning task(s) dropped.", room);
  }

  /**
   * Takes a room out of service, or puts it back.
   *
   * Blocking is how a room with a fault is kept off the market without
   * deleting it - the room still exists, its history is intact, and it simply
   * cannot be sold until somebody returns it to service.
   *
   * @param roomNo the room
   * @param block true to take it out of service, false to return it
   * @param staffId who is doing it
   * @return what happened
   */
  public ServiceResult<Room> setRoomOutOfService(String roomNo, boolean block,
      String staffId) {
    Room room = data.findRoom(roomNo);
    if (room == null) {
      return ServiceResult.fail("There is no room " + roomNo + ".");
    }

    if (block) {
      if (Room.OCCUPIED.equals(room.getOccupancyStatus())) {
        return ServiceResult.fail("Room " + roomNo
            + " has a guest in it and cannot be taken out of service.");
      }
      if (room.isOutOfService()) {
        return ServiceResult.fail("Room " + roomNo + " is already out of service.");
      }

      // A room that has been sold is not free to withdraw. The guest has paid
      // for it and is expected, so it stays theirs until somebody moves the
      // booking - checking only for a guest already in the room would let a
      // confirmed stay be blocked out from under them.
      Booking holding = data.getBookingList().search(
          booking -> roomNo.equals(booking.getRoomNo())
              && (Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus())
                  || Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())));
      if (holding != null) {
        return ServiceResult.fail("Booking " + holding.getBookingId()
            + " is holding room " + roomNo + ". Move it first.");
      }

      room.setOutOfService(true);
      room.setHousekeepingStatus(Room.BLOCKED);

      HousekeepingTask open = data.findOpenTaskForRoom(roomNo);
      if (open != null) {
        String fromStatus = open.getStatus();
        data.getCleaningQueue().removeEntry(open);
        open.setStatus(HousekeepingTask.BLOCKED);
        open.setRemark("Taken out of service");
        logStatusChange(open, fromStatus, HousekeepingTask.BLOCKED, staffId, false,
            "Taken out of service");
      } else {
        HousekeepingTask task = new HousekeepingTask(data.nextTaskId(), roomNo,
            HousekeepingTask.TYPE_MAINTENANCE, null, LocalDateTime.now());
        task.setStatus(HousekeepingTask.BLOCKED);
        task.setRemark("Taken out of service");
        data.getTaskList().add(task);
        logStatusChange(task, null, HousekeepingTask.BLOCKED, staffId, false,
            "Taken out of service");
      }

      data.saveMasters();
      data.saveHousekeeping();
      return ServiceResult.ok("Room " + roomNo
          + " is out of service and cannot be sold.", room);
    }

    if (!room.isOutOfService()) {
      return ServiceResult.fail("Room " + roomNo + " is not out of service.");
    }

    // It comes back dirty, not ready: it has to be cleaned and inspected
    // before anybody may be put in it.
    room.setOutOfService(false);
    room.setHousekeepingStatus(Room.DIRTY);

    HousekeepingTask prepared;
    HousekeepingTask open = data.findOpenTaskForRoom(roomNo);
    if (open != null && open.isCleaningType()) {
      String fromStatus = open.getStatus();
      open.setStatus(HousekeepingTask.DIRTY);
      open.setRemark("Returned to service - prepare before sale");
      enqueueIfNeedsCleaning(open);
      logStatusChange(open, fromStatus, HousekeepingTask.DIRTY, staffId, false,
          "Returned to service");
      prepared = open;
    } else {
      HousekeepingTask predecessor =
          (open != null && open.isMaintenanceType()) ? open : null;
      if (predecessor != null) {
        String fromStatus = predecessor.getStatus();
        predecessor.setStatus(HousekeepingTask.DIRTY);
        logStatusChange(predecessor, fromStatus, HousekeepingTask.DIRTY, staffId, false,
            "Returned to service");
      }
      prepared = createFollowOnCleaningTask(predecessor, roomNo,
          HousekeepingTask.TYPE_DEEP_CLEAN, staffId,
          "Returned to service - prepare before sale");
    }

    data.saveMasters();
    data.saveHousekeeping();

    return ServiceResult.ok("Room " + roomNo + " is back in service and queued"
        + " for cleaning (task " + prepared.getTaskId() + ").", room);
  }

  /**
   * Every booking still waiting for a room, urgent ones first.
   *
   * The order is the point. A walk-in guest granted an urgency exception at
   * the door is still urgent when they reach the front desk, and this is what
   * carries that ordering across to the officer handing out rooms - so the
   * urgent guest is served before the normal ones, exactly as the cleaning
   * queue prepares the urgent room first.
   *
   * @return the pending bookings, urgent first and earliest-booked within each
   */
  public ListInterface<Booking> pendingBookingsByPriority() {
    ListInterface<Booking> pending = data.getBookingList().filter(
        booking -> Booking.STATUS_PENDING.equals(booking.getBookingStatus())
            && booking.getRoomNo() == null);

    pending.sort(java.util.Comparator
        .comparing((Booking booking) -> booking.isUrgent() ? 0 : 1)
        .thenComparing(Booking::getCreatedAt,
            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

    return pending;
  }

  // ==================================================================
  // CHECK IN / CHECK OUT
  // ==================================================================

  /**
   * Takes a guest into their room.
   *
   * The room is checked again rather than trusted from when it was assigned -
   * it may have been blocked or taken out of service in between.
   */
  public ServiceResult<Booking> checkIn(String bookingId) {
    Booking booking = data.findBooking(bookingId);
    if (booking == null) {
      return ServiceResult.fail("Booking " + bookingId + " was not found.");
    }
    if (!Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus())) {
      return ServiceResult.fail("Only a CONFIRMED booking can check in - this one is "
          + booking.getBookingStatus() + ".");
    }
    if (booking.getRoomNo() == null) {
      return ServiceResult.fail("This booking has no room assigned yet.");
    }

    Room room = data.findRoom(booking.getRoomNo());
    if (room == null || room.isOutOfService()) {
      return ServiceResult.fail("Room " + booking.getRoomNo() + " is not usable.");
    }
    if (!Room.READY_FOR_CHECK_IN.equals(room.getHousekeepingStatus())) {
      return ServiceResult.fail("Room " + booking.getRoomNo()
          + " is no longer ready - housekeeping status is "
          + room.getHousekeepingStatus() + ".");
    }
    if (LocalDate.now().isBefore(booking.getCheckInDate())) {
      return ServiceResult.fail("This booking does not start until "
          + booking.getCheckInDate() + ".");
    }

    // The stay is paid for before the key is handed over. Collecting at the
    // counter is the only point where the guest is certain to still be there.
    Invoice invoice = data.findInvoiceByBooking(bookingId);
    if (invoice == null) {
      return ServiceResult.fail("This booking has no invoice to settle.");
    }
    if (!invoice.isSettled()) {
      return ServiceResult.fail(String.format(
          "RM%.2f is still outstanding on invoice %s. Settle the bill before check-in.",
          invoice.getOutstandingBalance(), invoice.getInvoiceId()));
    }

    booking.setBookingStatus(Booking.STATUS_CHECKED_IN);
    room.setOccupancyStatus(Room.OCCUPIED);

    data.saveFrontDesk();
    data.saveMasters();

    return ServiceResult.ok(guestNameOf(booking.getGuestId())
        + " checked in to room " + booking.getRoomNo() + ".", booking);
  }

  /**
   * Ends a stay - and everything that follows from it.
   *
   * One action with four consequences: the booking closes, the room is given
   * up and dirtied, housekeeping is told to clean it, and if the guest is a
   * member their points are awarded. The bill must be settled first, because
   * once the guest has left there is nobody to collect from.
   *
   * @param bookingId the stay to end
   * @param staffId the officer checking them out
   * @return a description of everything that happened
   */
  public ServiceResult<Booking> checkOut(String bookingId, String staffId) {
    return checkOut(bookingId, staffId, false);
  }

  /**
   * Ends a stay and says how soon the room is wanted back.
   *
   * Checkout is the one moment the front desk hands work to housekeeping, so
   * it is also where the urgency is set: the officer knows whether somebody is
   * waiting for this room, and nothing else in the system does.
   *
   * @param bookingId the stay ending
   * @param staffId the officer closing it
   * @param urgentCleaning whether the room is wanted back ahead of the round
   * @return the closed booking
   */
  public ServiceResult<Booking> checkOut(String bookingId, String staffId,
      boolean urgentCleaning) {
    Booking booking = data.findBooking(bookingId);
    if (booking == null) {
      return ServiceResult.fail("Booking " + bookingId + " was not found.");
    }
    if (!Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())) {
      return ServiceResult.fail("Only a CHECKED_IN booking can check out - this one is "
          + booking.getBookingStatus() + ".");
    }

    Invoice invoice = data.findInvoiceByBooking(bookingId);
    if (invoice == null) {
      return ServiceResult.fail("This booking has no invoice.");
    }
    if (!invoice.isSettled()) {
      return ServiceResult.fail(String.format(
          "RM%.2f is still outstanding on invoice %s. Settle the bill first.",
          invoice.getOutstandingBalance(), invoice.getInvoiceId()));
    }

    booking.setBookingStatus(Booking.STATUS_CHECKED_OUT);

    RoomAssignment open = data.findOpenAssignment(bookingId);
    if (open != null) {
      open.setReleasedAt(LocalDateTime.now());
    }

    StringBuilder outcome = new StringBuilder();
    outcome.append(guestNameOf(booking.getGuestId()))
        .append(" checked out of room ").append(booking.getRoomNo()).append(".");

    HousekeepingTask task = releaseRoomForCleaning(booking.getRoomNo(), bookingId,
        staffId, urgentCleaning);
    if (task != null) {
      outcome.append(" Cleaning task ").append(task.getTaskId())
          .append(" raised (").append(task.getPriority()).append(" lane).");
    }

    ServiceResult<PointTransaction> points = awardPointsForStay(bookingId);
    if (points.isSuccess()) {
      outcome.append(' ').append(points.getMessage());
    }

    data.saveAll();
    return ServiceResult.ok(outcome.toString(), booking);
  }

  /**
   * Hands a room back to housekeeping after someone has been in it.
   *
   * The room is dirtied and a cleaning task raised. If another booking is
   * already waiting on this room the task inherits that booking's urgency, so
   * a waiting guest is not left behind normal cleaning work.
   *
   * @return the cleaning task raised, or null if the room was unknown
   */
  private HousekeepingTask releaseRoomForCleaning(String roomNo, String bookingId,
      String staffId) {
    return releaseRoomForCleaning(roomNo, bookingId, staffId, false);
  }

  /**
   * Hands a room back to housekeeping, in the lane the officer asked for.
   *
   * @param urgentCleaning whether the room is wanted back ahead of the round
   * @return the cleaning task raised, or null if the room was unknown
   */
  private HousekeepingTask releaseRoomForCleaning(String roomNo, String bookingId,
      String staffId, boolean urgentCleaning) {
    Room room = data.findRoom(roomNo);
    if (room == null) {
      return null;
    }

    room.setOccupancyStatus(Room.VACANT);
    room.setHousekeepingStatus(Room.DIRTY);

    HousekeepingTask task = new HousekeepingTask(data.nextTaskId(), roomNo,
        HousekeepingTask.TYPE_CHECKOUT_CLEAN, bookingId, LocalDateTime.now());

    // The officer at the desk decides the lane. They are the only one who
    // knows whether anybody is waiting on this room, so the answer is taken
    // from them rather than guessed at from the booking records.
    task.setPriority(urgentCleaning
        ? HousekeepingTask.PRIORITY_URGENT : HousekeepingTask.PRIORITY_NORMAL);
    task.setRemark(urgentCleaning
        ? "Raised on check-out - wanted back urgently"
        : "Raised on check-out");

    data.getTaskList().add(task);
    enqueueIfNeedsCleaning(task);
    logStatusChange(task, null, HousekeepingTask.DIRTY, staffId, false,
        task.getRemark());

    return task;
  }

  // ==================================================================
  // BILLING
  // ==================================================================

  /**
   * Takes a payment against a bill.
   *
   * The invoice's paid total is recalculated from the payments themselves
   * rather than incremented, so the two can never drift apart.
   */
  public ServiceResult<Payment> recordPayment(String invoiceId, double amount,
      String method, String reference, String staffId) {
    Invoice invoice = data.findInvoice(invoiceId);
    if (invoice == null) {
      return ServiceResult.fail("Invoice " + invoiceId + " was not found.");
    }
    if (amount <= 0) {
      return ServiceResult.fail("A payment must be more than zero.");
    }
    if (amount > invoice.getOutstandingBalance() + 0.005) {
      return ServiceResult.fail(String.format(
          "That is more than the RM%.2f outstanding.", invoice.getOutstandingBalance()));
    }

    // A bill is settled in full or not at all: a room is only ever given to a
    // guest who has paid for the stay, so a part payment would leave a booking
    // in a state the rest of the system does not allow.
    if (amount + 0.005 < invoice.getOutstandingBalance()) {
      return ServiceResult.fail(String.format(
          "The full RM%.2f is due - a bill cannot be part paid.",
          invoice.getOutstandingBalance()));
    }
    if (Payment.requiresReference(method) && (reference == null || reference.isBlank())) {
      return ServiceResult.fail("A reference is required for a " + method + " payment.");
    }

    Payment payment = new Payment(data.nextPaymentId(), invoiceId, amount, method,
        reference, LocalDateTime.now(), staffId);
    data.getPaymentList().add(payment);

    invoice.setAmountPaid(sumPayments(invoiceId));
    data.saveFrontDesk();

    String message = String.format("RM%.2f received. %s is now %s",
        amount, invoiceId, invoice.getPaymentStatus());
    if (!invoice.isSettled()) {
      message += String.format(" (RM%.2f outstanding)", invoice.getOutstandingBalance());
    }
    return ServiceResult.ok(message + ".", payment);
  }

  /** What has actually been paid against a bill. */
  public double sumPayments(String invoiceId) {
    double total = 0.0;
    ListInterface<Payment> payments = data.getPaymentList();
    for (int i = 1; i <= payments.getNumberOfEntries(); i++) {
      Payment payment = payments.getEntry(i);
      if (invoiceId.equals(payment.getInvoiceId())) {
        total += payment.getAmount();
      }
    }
    return Math.round(total * 100.0) / 100.0;
  }

  /** The payments taken against one bill. */
  public ListInterface<Payment> paymentsFor(String invoiceId) {
    return data.getPaymentList().filter(p -> invoiceId.equals(p.getInvoiceId()));
  }

  // ==================================================================
  // HOUSEKEEPING
  // ==================================================================

  /**
   * Moves a cleaning task to its next status.
   *
   * The workflow is enforced here rather than trusted to the caller: a room
   * cannot be inspected without being cleaned, cannot skip inspection to reach
   * ready, and cannot move backwards. A failed inspection sends it back to be
   * done again and is counted, which is what the re-cleaning reports are built
   * from.
   *
   * @param taskId the task to advance
   * @param toStatus the status being requested
   * @param staffId who is making the change
   * @param remark a note, required when blocking a room
   * @return the task at its new status
   */
  public ServiceResult<HousekeepingTask> updateTaskStatus(String taskId, String toStatus,
      String staffId, String remark) {
    HousekeepingTask task = data.findTask(taskId);
    if (task == null) {
      return ServiceResult.fail("Task " + taskId + " was not found.");
    }

    String fromStatus = task.getStatus();
    if (!HousekeepingTask.isValidTransition(task.getTaskType(), fromStatus, toStatus)) {
      if (task.isMaintenanceType()
          && (HousekeepingTask.CLEANING_IN_PROGRESS.equals(toStatus)
              || HousekeepingTask.INSPECTED.equals(toStatus)
              || HousekeepingTask.READY_FOR_CHECK_IN.equals(toStatus))) {
        return ServiceResult.fail("MAINTENANCE is not a cleaning task.");
      }
      if (task.isInspectionType()
          && HousekeepingTask.CLEANING_IN_PROGRESS.equals(toStatus)) {
        return ServiceResult.fail("Inspection is not started from the cleaning queue.");
      }
      return ServiceResult.fail(
          HousekeepingTask.explainInvalidTransition(fromStatus, toStatus));
    }
    if (HousekeepingTask.BLOCKED.equals(toStatus) && (remark == null || remark.isBlank())) {
      return ServiceResult.fail("A reason is required to block a room.");
    }

    Room room = data.findRoom(task.getRoomNo());
    if (room == null) {
      return ServiceResult.fail("Room " + task.getRoomNo() + " no longer exists.");
    }

    boolean otherOpenWork = HousekeepingTask.READY_FOR_CHECK_IN.equals(toStatus)
        && roomHasOtherOpenTask(task.getRoomNo(), task.getTaskId());

    task.setStatus(toStatus);
    if (remark != null && !remark.isBlank()) {
      task.setRemark(remark);
    }

    HousekeepingTask followOn = null;
    switch (toStatus) {
      case HousekeepingTask.CLEANING_IN_PROGRESS:
        task.setStartedAt(LocalDateTime.now());
        task.setAssignedTo(staffId);
        // It is being worked on, so it is no longer waiting to be picked up.
        data.getCleaningQueue().removeEntry(task);
        break;

      case HousekeepingTask.DIRTY:
        if (task.isMaintenanceType()
            && HousekeepingTask.BLOCKED.equals(fromStatus)) {
          // Maintenance is finished. The room stays dirty and a separate
          // cleaning job is raised - the maintenance row is not converted.
          room.setOutOfService(false);
          followOn = createFollowOnCleaningTask(task, task.getRoomNo(),
              HousekeepingTask.TYPE_DEEP_CLEAN, staffId,
              "Maintenance completed. Room remains DIRTY and requires cleaning.");
        } else if (HousekeepingTask.INSPECTED.equals(fromStatus)) {
          task.incrementInspectionFailCount();
          task.setStartedAt(null);
          followOn = createFollowOnCleaningTask(task, task.getRoomNo(),
              task.isCleaningType() ? task.getTaskType()
                  : HousekeepingTask.TYPE_CHECKOUT_CLEAN,
              staffId, "Inspection failed - clean it again");
        } else {
          enqueueIfNeedsCleaning(task);
        }
        break;

      case HousekeepingTask.READY_FOR_CHECK_IN:
        task.setCompletedAt(LocalDateTime.now());
        if (!otherOpenWork) {
          room.setLastCleanedAt(LocalDateTime.now());
        }
        break;

      case HousekeepingTask.BLOCKED:
        data.getCleaningQueue().removeEntry(task);
        room.setOutOfService(true);
        break;

      default:
        break;
    }

    if (!otherOpenWork) {
      room.setHousekeepingStatus(toStatus);
    }
    logStatusChange(task, fromStatus, toStatus, staffId, false, remark);
    data.saveHousekeeping();
    data.saveMasters();

    String message = "Room " + task.getRoomNo() + ": " + fromStatus + " -> " + toStatus + ".";
    if (task.isMaintenanceType() && HousekeepingTask.DIRTY.equals(toStatus)
        && followOn != null) {
      message = "Maintenance completed. Room remains DIRTY and requires cleaning."
          + " Cleaning task " + followOn.getTaskId() + " queued.";
    } else if (HousekeepingTask.INSPECTED.equals(fromStatus)
        && HousekeepingTask.DIRTY.equals(toStatus) && followOn != null) {
      message += " Inspection failed. Re-clean task " + followOn.getTaskId()
          + " queued.";
    } else if (HousekeepingTask.READY_FOR_CHECK_IN.equals(toStatus)) {
      if (otherOpenWork) {
        message += " The task is finished, but the room is not ready while another"
            + " housekeeping job on this room is still open.";
      } else {
        message += " The room is now available to the front desk.";
        if (task.getReservedForBookingId() != null) {
          message += " Booking " + task.getReservedForBookingId() + " is waiting on it.";
        }
      }
    }
    return ServiceResult.ok(message, task);
  }

  // Whether another unfinished housekeeping task exists for the same room.
  private boolean roomHasOtherOpenTask(String roomNo, String exceptTaskId) {
    return data.getTaskList().search(other -> roomNo.equals(other.getRoomNo())
        && !exceptTaskId.equals(other.getTaskId())
        && other.isActiveWork()) != null;
  }

  /**
   * Raises a new DIRTY cleaning task for a room and puts it on the queue.
   *
   * The predecessor is kept in the list as history and marked superseded so
   * it cannot occupy the queue. Booking urgency, if any, is copied across so
   * a waiting guest is not dropped to the normal lane.
   */
  private HousekeepingTask createFollowOnCleaningTask(HousekeepingTask predecessor,
      String roomNo, String cleaningType, String staffId, String reason) {
    String type = cleaningType;
    if (type == null || !isCleaningTaskType(type)) {
      type = predecessor != null && predecessor.isCleaningType()
          ? predecessor.getTaskType()
          : HousekeepingTask.TYPE_CHECKOUT_CLEAN;
    }

    HousekeepingTask followOn = new HousekeepingTask(data.nextTaskId(), roomNo, type,
        predecessor == null ? null : predecessor.getBookingId(), LocalDateTime.now());
    if (predecessor != null) {
      followOn.setReservedForBookingId(predecessor.getReservedForBookingId());
      followOn.setRemark("Follow-on of " + predecessor.getTaskId() + ": " + reason);
      predecessor.setRemark("Superseded by " + followOn.getTaskId() + ": " + reason);
    } else {
      followOn.setRemark(reason);
    }

    data.getTaskList().add(followOn);
    refreshTaskPriority(followOn);
    enqueueIfNeedsCleaning(followOn);
    logStatusChange(followOn, null, HousekeepingTask.DIRTY, staffId, false, reason);
    return followOn;
  }

  private boolean isCleaningTaskType(String taskType) {
    return HousekeepingTask.TYPE_CHECKOUT_CLEAN.equals(taskType)
        || HousekeepingTask.TYPE_STAYOVER_CLEAN.equals(taskType)
        || HousekeepingTask.TYPE_DEEP_CLEAN.equals(taskType);
  }

  /**
   * Drops a follow-on cleaning job raised for a predecessor that is being
   * rolled back, without deleting the history row.
   */
  private void cancelFollowOnCleaningTasks(String predecessorId) {
    if (predecessorId == null) {
      return;
    }
    String prefix = "Follow-on of " + predecessorId + ":";
    ListInterface<HousekeepingTask> followOns = data.getTaskList().filter(
        task -> task.getRemark() != null && task.getRemark().startsWith(prefix)
            && task.isPendingCleaning());
    for (int i = 1; i <= followOns.getNumberOfEntries(); i++) {
      HousekeepingTask followOn = followOns.getEntry(i);
      data.getCleaningQueue().removeEntry(followOn);
      followOn.setCompletedAt(LocalDateTime.now());
      followOn.setRemark(followOn.getRemark() + " (cancelled by rollback)");
    }
  }

  /**
   * Puts a dirty cleaning task on the cleaning queue, once.
   *
   * Inspection and maintenance stay off this queue. A task already waiting is
   * not added a second time.
   */
  public void enqueueIfNeedsCleaning(HousekeepingTask task) {
    if (task == null || !task.isPendingCleaning()) {
      return;
    }
    if (data.getCleaningQueue().toServiceOrder().contains(task)) {
      return;
    }
    data.getCleaningQueue().enqueue(task, task.getPriority());
  }

  /** Writes one row of a room's status history. */
  private void logStatusChange(HousekeepingTask task, String fromStatus, String toStatus,
      String staffId, boolean isRollback, String remark) {
    RoomStatusLog log = new RoomStatusLog(data.nextStatusLogId(), task.getTaskId(),
        task.getRoomNo(), fromStatus, toStatus, LocalDateTime.now(), staffId,
        isRollback, remark);
    data.getStatusLogList().add(log);

    // Only a real workflow step is rollbackable. Opening DIRTY rows have no
    // previous status, and compensating rows are the undo rather than a new
    // undoable update.
    if (!isRollback && fromStatus != null) {
      data.getStatusRollbackStack().push(log);
    }
  }

  /**
   * Undoes the most recent status change on a task.
   *
   * The row being undone is not deleted - a compensating row is added instead.
   * The reports count inspection failures and re-cleaning from this history,
   * so removing rows would quietly change figures that have already been
   * reported; adding one leaves the trail complete and still puts the room
   * back where it was.
   *
   * @param staffId who is undoing it
   * @return the task at its restored status
   */
  public ServiceResult<HousekeepingTask> rollbackLastStatusChange(String staffId) {
    if (data.getStatusRollbackStack().isEmpty()) {
      return ServiceResult.fail("There is no status update to roll back.");
    }

    RoomStatusLog latest = data.getStatusRollbackStack().peek();

    HousekeepingTask task = data.findTask(latest.getTaskId());
    if (task == null) {
      return ServiceResult.fail("The task behind that update no longer exists.");
    }

    Room room = data.findRoom(latest.getRoomNo());
    if (room == null) {
      return ServiceResult.fail("Room " + latest.getRoomNo() + " no longer exists.");
    }

    String restoreTo = latest.getFromStatus();
    String undoing = latest.getToStatus();

    // A failure that is being undone should stop counting against the room.
    if (HousekeepingTask.DIRTY.equals(undoing)
        && HousekeepingTask.INSPECTED.equals(restoreTo)) {
      task.decrementInspectionFailCount();
      cancelFollowOnCleaningTasks(task.getTaskId());
      task.setRemark(null);
    }
    if (task.isMaintenanceType() && HousekeepingTask.DIRTY.equals(undoing)
        && HousekeepingTask.BLOCKED.equals(restoreTo)) {
      cancelFollowOnCleaningTasks(task.getTaskId());
      task.setRemark(null);
      room.setOutOfService(true);
    }

    task.setStatus(restoreTo);
    if (!task.isStayoverService()) {
      room.setHousekeepingStatus(restoreTo);
    }

    if (HousekeepingTask.CLEANING_IN_PROGRESS.equals(undoing)
        && (HousekeepingTask.DIRTY.equals(restoreTo)
            || HousekeepingTask.NOT_CLEANED.equals(restoreTo))) {
      task.setStartedAt(null);
      task.setAssignedTo(null);
    }
    if (HousekeepingTask.READY_FOR_CHECK_IN.equals(undoing)
        || HousekeepingTask.CLEANED.equals(undoing)) {
      task.setCompletedAt(null);
    }

    // The queue holds whoever is waiting to be cleaned, so it has to follow.
    // Always drop first so a restore cannot create a second copy of the same
    // task; put it back only when the restored status still needs cleaning.
    data.getCleaningQueue().removeEntry(task);
    enqueueIfNeedsCleaning(task);

    data.getStatusRollbackStack().pop();
    logStatusChange(task, undoing, restoreTo, staffId, true,
        "Rollback of " + latest.getLogId());
    data.saveHousekeeping();
    data.saveMasters();

    return ServiceResult.ok("Rolled back room " + task.getRoomNo() + ": "
        + undoing + " -> " + restoreTo + ".", task);
  }

  // ==================================================================
  // LOYALTY
  // ==================================================================

  /**
   * Awards the points a completed stay earned.
   *
   * Points come from the bill actually paid, multiplied by the rate the
   * member's tier earns at. The ledger row is written first and the cached
   * balance updated from it, so the total on the member always has a history
   * behind it.
   *
   * @param bookingId the stay that has just been paid for
   * @return the ledger row written, or a failure explaining why none was
   */
  public ServiceResult<PointTransaction> awardPointsForStay(String bookingId) {
    Booking booking = data.findBooking(bookingId);
    if (booking == null) {
      return ServiceResult.fail("Booking " + bookingId + " was not found.");
    }

    Invoice invoice = data.findInvoiceByBooking(bookingId);
    if (invoice == null || !invoice.isSettled()) {
      return ServiceResult.fail("Points are awarded once the bill is settled.");
    }

    // Awarding twice for one stay would be a real defect, so an existing row
    // for this booking stops it.
    PointTransaction existing = data.getTransactionList().search(
        txn -> bookingId.equals(txn.getBookingId())
            && PointTransaction.EARN.equals(txn.getTxnType()));
    if (existing != null) {
      return ServiceResult.fail("Points for " + bookingId + " have already been awarded.");
    }

    // A guest who has paid for a stay has already given the resort everything
    // a membership needs, so the first completed stay enrols them rather than
    // throwing the points away. Their next visit starts with a balance.
    Member member = data.findMemberByGuest(booking.getGuestId());
    boolean newlyEnrolled = false;
    if (member == null) {
      ServiceResult<Member> enrolled = enrolMember(booking.getGuestId());
      if (enrolled.isFailure()) {
        return ServiceResult.fail(enrolled.getMessage());
      }
      member = enrolled.getValue();
      newlyEnrolled = true;
    }

    int points = (int) Math.round(
        invoice.getTotalAmount() * POINTS_PER_RINGGIT * member.getMultiplier());
    if (points <= 0) {
      return ServiceResult.fail("This stay did not earn any points.");
    }

    String previousTier = member.getTier();
    member.setPointsBalance(member.getPointsBalance() + points);
    member.setLifetimePoints(member.getLifetimePoints() + points);
    member.setPointsExpiryDate(LocalDate.now().plusMonths(Member.POINTS_VALIDITY_MONTHS));

    PointTransaction txn = new PointTransaction(data.nextTransactionId(),
        member.getMemberId(), bookingId, PointTransaction.EARN, points,
        member.getPointsBalance(), LocalDate.now(),
        String.format("Stay %s at %s rate", bookingId, previousTier));
    data.getTransactionList().add(txn);

    if (member.refreshTier()) {
    }

    data.saveLoyalty();

    String message = newlyEnrolled
        ? "Enrolled as loyalty member " + member.getMemberId() + " and awarded "
            + points + " points."
        : points + " points awarded to " + member.getMemberId() + ".";
    if (!previousTier.equals(member.getTier())) {
      message += " Tier upgraded from " + previousTier + " to " + member.getTier() + ".";
    }
    return ServiceResult.ok(message, txn);
  }

  /**
   * Puts a redemption request into the pending queue.
   *
   * Nothing is checked beyond the obvious here, and no points are taken. The
   * eligibility rules are applied when the request is processed instead, so
   * the reason for a refusal is recorded against the request rather than
   * disappearing at the counter.
   */
  public ServiceResult<Redemption> requestRedemption(String memberId, String rewardId) {
    Member member = data.findMember(memberId);
    if (member == null) {
      return ServiceResult.fail("Member " + memberId + " was not found.");
    }

    Reward reward = data.findReward(rewardId);
    if (reward == null) {
      return ServiceResult.fail("Reward " + rewardId + " was not found.");
    }
    if (!reward.isActive()) {
      return ServiceResult.fail(reward.getRewardName() + " is no longer offered.");
    }

    // The price is captured now, so a later change cannot alter a request
    // already waiting in the queue.
    Redemption redemption = new Redemption(data.nextRedemptionId(), memberId, rewardId,
        reward.getPointsRequired(), LocalDate.now());

    // Checked here as well as at approval. The rules are the same either way,
    // but a guest told at the counter that they are short is better served
    // than one who waits in the queue only to be refused - and it keeps
    // requests nobody could ever grant out of the loyalty officer's list.
    String refusal = checkRedemptionEligibility(member, reward, redemption);
    if (refusal != null) {
      return ServiceResult.fail(reward.getRewardName() + " cannot be requested: "
          + refusal + ".");
    }
    data.getRedemptionList().add(redemption);
    data.getPendingRedemptions().add(redemption);

    data.saveLoyalty();
    return ServiceResult.ok("Request " + redemption.getRedemptionId()
        + " queued for " + reward.getRewardName() + ".", redemption);
  }

  /**
   * Decides the oldest waiting redemption request.
   *
   * Whoever asked first is dealt with first - loyalty has no urgent lane.
   * Points are only taken if every check passes; a refusal costs the member
   * nothing and records why.
   *
   * @param staffId the manager processing it
   * @return the request, now approved or rejected
   */
  public ServiceResult<Redemption> processNextRedemption(String staffId) {
    ListInterface<Redemption> pending = data.getPendingRedemptions();
    if (pending.isEmpty()) {
      return ServiceResult.fail("There are no redemption requests waiting.");
    }
    return processRedemption(pending.getEntry(1).getRedemptionId(), staffId);
  }

  /**
   * Decides one particular request.
   *
   * The officer picks which to deal with rather than always taking the head of
   * the queue, so a request that cannot be met yet does not hold up the rest.
   * The eligibility rules are the same either way.
   *
   * @param redemptionId the request being decided
   * @param staffId who is deciding it
   * @return the request, approved or rejected with its reason
   */
  public ServiceResult<Redemption> processRedemption(String redemptionId,
      String staffId) {
    ListInterface<Redemption> pending = data.getPendingRedemptions();

    int at = -1;
    for (int i = 1; i <= pending.getNumberOfEntries(); i++) {
      if (pending.getEntry(i).getRedemptionId().equals(redemptionId)) {
        at = i;
        break;
      }
    }
    if (at < 0) {
      return ServiceResult.fail("Request " + redemptionId
          + " is not waiting for a decision.");
    }

    Redemption redemption = pending.remove(at);
    Member member = data.findMember(redemption.getMemberId());
    Reward reward = data.findReward(redemption.getRewardId());

    redemption.setProcessedDate(LocalDate.now());
    redemption.setProcessedBy(staffId);

    String refusal = checkRedemptionEligibility(member, reward, redemption);
    if (refusal != null) {
      redemption.setStatus(Redemption.REJECTED);
      redemption.setRejectReason(refusal);
      data.saveLoyalty();
      return ServiceResult.ok("Request " + redemption.getRedemptionId()
          + " rejected: " + refusal, redemption);
    }

    redemption.setStatus(Redemption.APPROVED);
    member.setPointsBalance(member.getPointsBalance() - redemption.getPointsUsed());
    reward.reduceStock();

    // The booking goes on the row so the ledger reads both ways: an EARN says
    // which stay paid for the points, and a REDEEM says which stay spent them.
    data.getTransactionList().add(new PointTransaction(data.nextTransactionId(),
        member.getMemberId(), redemption.getBookingId(), PointTransaction.REDEEM,
        -redemption.getPointsUsed(), member.getPointsBalance(), LocalDate.now(),
        redemption.getRedemptionId() + " - " + reward.getRewardName()));

    data.saveLoyalty();
    return ServiceResult.ok("Request " + redemption.getRedemptionId() + " approved - "
        + reward.getRewardName() + ", " + redemption.getPointsUsed()
        + " points deducted.", redemption);
  }

  /**
   * Why a redemption cannot be honoured, or null if it can.
   *
   * Checked at processing time rather than when requested, because stock and
   * balances move while a request waits its turn.
   */
  private String checkRedemptionEligibility(Member member, Reward reward,
      Redemption redemption) {
    if (member == null) {
      return "the member record no longer exists";
    }
    if (reward == null) {
      return "the reward no longer exists";
    }
    if (!reward.isActive()) {
      return reward.getRewardName() + " is no longer offered";
    }
    if (reward.getStockQuantity() <= 0) {
      return reward.getRewardName() + " is out of stock";
    }
    if (Member.tierRank(member.getTier()) < Member.tierRank(reward.getMinimumTier())) {
      return "member tier " + member.getTier() + " is below the required "
          + reward.getMinimumTier();
    }
    if (member.getPointsBalance() < redemption.getPointsUsed()) {
      return String.format("only %d of the %d points required are available",
          member.getPointsBalance(), redemption.getPointsUsed());
    }
    return null;
  }

  /** Signs a guest up to the loyalty programme. */
  public ServiceResult<Member> enrolMember(String guestId) {
    Guest guest = data.findGuest(guestId);
    if (guest == null) {
      return ServiceResult.fail("Guest " + guestId + " was not found.");
    }

    Member existing = data.findMemberByGuest(guestId);
    if (existing != null) {
      return ServiceResult.fail(guest.getFullName() + " is already member "
          + existing.getMemberId() + ".");
    }

    Member member = new Member(data.nextMemberId(), guestId, LocalDate.now());
    data.addMember(member);

    data.saveLoyalty();
    return ServiceResult.ok(guest.getFullName() + " enrolled as "
        + member.getMemberId() + " (" + member.getTier() + ").", member);
  }

  /**
   * Expires points that have passed their date.
   *
   * Run on demand rather than on a timer, since a console system is only alive
   * while somebody is using it. The whole balance expires at once because the
   * points carry a single expiry date rather than being tracked in batches.
   *
   * @return how many members were affected
   */
  public ServiceResult<Integer> expireOverduePoints() {
    LocalDate today = LocalDate.now();
    int affected = 0;

    ListInterface<Member> members = data.getMemberList();
    for (int i = 1; i <= members.getNumberOfEntries(); i++) {
      Member member = members.getEntry(i);
      if (member.getPointsExpiryDate() == null
          || !member.getPointsExpiryDate().isBefore(today)
          || member.getPointsBalance() <= 0) {
        continue;
      }

      int lost = member.getPointsBalance();
      member.setPointsBalance(0);
      member.setPointsExpiryDate(today.plusMonths(Member.POINTS_VALIDITY_MONTHS));

      data.getTransactionList().add(new PointTransaction(data.nextTransactionId(),
          member.getMemberId(), null, PointTransaction.EXPIRE, -lost, 0, today,
          "Points expired"));
      affected++;
    }

    if (affected > 0) {
      data.saveLoyalty();
    }
    return ServiceResult.ok(affected + " member(s) had points expire.", affected);
  }
}
