package test;

import adt.ListInterface;
import control.ResortData;
import control.ResortService;
import control.ServiceResult;
import entity.Booking;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Invoice;
import entity.Member;
import entity.Payment;
import entity.PointTransaction;
import entity.Redemption;
import entity.Room;
import entity.RoomAssignment;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * System tests - whole journeys through all four modules at once.
 *
 * Where the integration tests check that two modules agree, these follow a
 * guest from the moment they walk through the door to the moment their room is
 * ready for the next person, and check that the system is in a sensible state
 * at every step. A journey that works end to end is the only real proof the
 * four modules are one system rather than four programs sharing a menu.
 *
 * @author Tan Chee Yan
 */
public class SystemTest {

  private final TestRunner runner = new TestRunner();

  private static final String FRONT_DESK = "ST001";
  private static final String HOUSEKEEPER = "ST003";
  private static final String SUPERVISOR = "ST005";
  private static final String MANAGER = "ST006";

  public static void main(String[] args) {
    SystemTest tests = new SystemTest();
    boolean allPassed = tests.runAll();
    System.exit(allPassed ? 0 : 1);
  }

  public boolean runAll() {
    System.out.println();
    System.out.println("=".repeat(76));
    System.out.println("  SYSTEM TESTS - whole journeys across all four modules");
    System.out.println("=".repeat(76));

    testFullGuestJourney();
    testUrgentGuestJumpsTheQueueEndToEnd();
    testRoomIsRecycledForTheNextGuest();
    testMemberJourneyEarnAndSpend();
    testSystemStaysConsistentUnderLoad();
    testStartupFromNothing();

    return runner.report("SYSTEM TEST SUMMARY");
  }

  private ResortData freshData() {
    ResortData data = new ResortData();
    data.deleteAllFiles();
    return new ResortData();
  }

  // ==================================================================
  // THE FULL JOURNEY
  // ==================================================================

  /**
   * One guest, all the way through: arrive, wait, be served, get a room, pay,
   * leave, and have the room cleaned for the next person.
   *
   * This is the journey the four separate modules could not complete - the
   * trail stopped dead when a guest was marked served.
   */
  private void testFullGuestJourney() {
    runner.suite("A guest walks in and stays - the whole journey");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    // --- Arriving -------------------------------------------------
    Guest guest = service.findOrCreateGuest("880101-14-1234", "Journey Test Guest",
        "012-1112222", "journey@example.com");
    runner.check("a new guest is created on arrival", guest != null);
    runner.check("and can be found again by their document",
        data.findGuestByIc("880101-14-1234") != null);

    WalkInRegistration reg = new WalkInRegistration(data.nextRegistrationId(),
        guest.getGuestId(), LocalDateTime.now(),
        WalkInRegistration.PRIORITY_NORMAL, null, "RT03", 2);
    data.getRegistrationList().add(reg);
    data.getWaitingList().enqueue(reg, reg.getPriority());

    runner.checkEquals("they are waiting", WalkInRegistration.STATUS_WAITING,
        reg.getStatus());
    runner.check("and are in the queue",
        data.getWaitingList().toServiceOrder().contains(reg));

    // --- Being served ---------------------------------------------
    // Everyone ahead of them is dealt with first, which is the queue working.
    // Only one guest holds the counter at a time, so each is cleared before
    // the next is called - otherwise the unfinished one keeps their place.
    while (!reg.equals(data.getWaitingList().peekNext())) {
      ServiceResult<WalkInRegistration> other = service.serveNextGuest(FRONT_DESK);
      if (other.isFailure()) {
        break;
      }
      other.getValue().setStatus(WalkInRegistration.STATUS_CANCELLED);
    }

    ServiceResult<WalkInRegistration> called = service.serveNextGuest(FRONT_DESK);
    runner.check("their turn comes", called.isSuccess());
    runner.checkEquals("and it is them", reg.getRegId(), called.getValue().getRegId());
    runner.checkEquals("they are now being served",
        WalkInRegistration.STATUS_IN_SERVICE, reg.getStatus());

    // --- Becoming a booking ---------------------------------------
    LocalDate from = LocalDate.now();
    LocalDate to = from.plusDays(2);

    ServiceResult<Booking> booked = service.convertRegistrationToBooking(
        reg.getRegId(), from, to, 2, FRONT_DESK);
    runner.check("a booking is created for them", booked.isSuccess());

    Booking booking = booked.getValue();
    runner.checkEquals("it starts with no room", null, booking.getRoomNo());
    runner.checkEquals("and the registration points at it",
        booking.getBookingId(), reg.getBookingId());

    // --- Getting a room -------------------------------------------
    // Make sure exactly one RT03 room is ready, so the outcome is predictable.
    Room target = data.findRoom("3001");
    target.setOccupancyStatus(Room.VACANT);
    target.setHousekeepingStatus(Room.READY_FOR_CHECK_IN);
    target.setOutOfService(false);

    ListInterface<Room> available = service.findAvailableRooms("RT03", from, to);
    runner.check("housekeeping reports at least one ready room",
        !available.isEmpty());

    ServiceResult<Booking> assigned = service.assignRoom(booking.getBookingId(),
        "3001", FRONT_DESK, RoomAssignment.REASON_INITIAL);
    runner.check("the room can be assigned", assigned.isSuccess());
    runner.checkEquals("the booking is confirmed", Booking.STATUS_CONFIRMED,
        booking.getBookingStatus());
    runner.checkEquals("the room is held", Room.RESERVED,
        target.getOccupancyStatus());
    runner.check("and nobody else can have it",
        !service.isRoomAvailable("3001", from, to, null));

    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    runner.check("a bill is raised", invoice != null);
    runner.checkAmount("priced at the rate times the nights",
        booking.getRatePerNight() * 2, invoice.getRoomCharge());
    runner.checkEquals("and it is unpaid", Invoice.UNPAID, invoice.getPaymentStatus());

    // --- Paying ---------------------------------------------------
    // The stay is paid for at the counter, before the key is handed over.
    runner.check("check-in is refused while the bill is unpaid",
        service.checkIn(booking.getBookingId()).isFailure());

    // A deposit is not a payment: the whole stay is settled in one go, so
    // half the balance is turned away and the bill is left as it was.
    double half = invoice.getOutstandingBalance() / 2;
    runner.check("a half payment is refused",
        service.recordPayment(invoice.getInvoiceId(), half, Payment.CARD,
            "APPR-TEST01", FRONT_DESK).isFailure());
    runner.checkEquals("the bill is still unpaid", Invoice.UNPAID,
        invoice.getPaymentStatus());
    runner.check("and an unpaid bill still refuses check-in",
        service.checkIn(booking.getBookingId()).isFailure());

    service.recordPayment(invoice.getInvoiceId(), invoice.getOutstandingBalance(),
        Payment.CASH, null, FRONT_DESK);
    runner.checkEquals("settling it makes it paid", Invoice.PAID,
        invoice.getPaymentStatus());
    runner.checkEquals("one payment settles the whole bill", 1,
        service.paymentsFor(invoice.getInvoiceId()).getNumberOfEntries());
    runner.checkAmount("and it adds up to the bill", invoice.getTotalAmount(),
        service.sumPayments(invoice.getInvoiceId()));

    // --- Checking in ----------------------------------------------
    ServiceResult<Booking> checkedIn = service.checkIn(booking.getBookingId());
    runner.check("the guest can check in once it is settled", checkedIn.isSuccess());
    runner.checkEquals("the booking is checked in", Booking.STATUS_CHECKED_IN,
        booking.getBookingStatus());
    runner.checkEquals("and the room is occupied", Room.OCCUPIED,
        target.getOccupancyStatus());

    // --- Leaving --------------------------------------------------
    int tasksBefore = data.getTaskList().getNumberOfEntries();

    ServiceResult<Booking> out = service.checkOut(booking.getBookingId(), FRONT_DESK);
    runner.check("now they can check out", out.isSuccess());
    runner.checkEquals("the stay is complete", Booking.STATUS_CHECKED_OUT,
        booking.getBookingStatus());

    // --- What follows automatically -------------------------------
    runner.checkEquals("the room is vacant again", Room.VACANT,
        target.getOccupancyStatus());
    runner.checkEquals("but dirty", Room.DIRTY, target.getHousekeepingStatus());
    runner.checkEquals("a cleaning task was raised without being asked for",
        tasksBefore + 1, data.getTaskList().getNumberOfEntries());
    runner.check("the room cannot be sold in the meantime",
        !service.isRoomAvailable("3001", to, to.plusDays(2), null));

    // --- Cleaning it for the next guest ---------------------------
    HousekeepingTask task = data.findOpenTaskForRoom("3001");
    runner.check("the cleaning task exists", task != null);
    runner.check("and is waiting in the queue",
        data.getCleaningQueue().toServiceOrder().contains(task));

    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.CLEANING_IN_PROGRESS, HOUSEKEEPER, null);
    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.INSPECTED, HOUSEKEEPER, null);
    ServiceResult<HousekeepingTask> done = service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.READY_FOR_CHECK_IN, SUPERVISOR, null);

    runner.check("the room can be cleaned through to ready", done.isSuccess());
    runner.checkEquals("the room is ready again", Room.READY_FOR_CHECK_IN,
        target.getHousekeepingStatus());

    // The loop closes: the room is back where the journey started.
    runner.check("and it is sellable to the next guest",
        service.isRoomAvailable("3001", to, to.plusDays(2), null));
  }

  /**
   * The same journey for an urgent guest, where nothing is ready and a room
   * has to be cleaned out of turn for them.
   */
  private void testUrgentGuestJumpsTheQueueEndToEnd() {
    runner.suite("An urgent guest is served first and has a room prepared");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    // Three normal guests are already waiting.
    for (int i = 1; i <= 3; i++) {
      Guest waiting = service.findOrCreateGuest("77010" + i + "-14-000" + i,
          "Waiting Guest " + i, "012-000000" + i, "");
      WalkInRegistration reg = new WalkInRegistration(data.nextRegistrationId(),
          waiting.getGuestId(), LocalDateTime.now().minusMinutes(60 - i),
          WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 1);
      data.getRegistrationList().add(reg);
      data.getWaitingList().enqueue(reg, reg.getPriority());
    }

    int normalWaiting = data.getWaitingList().getNormalCount();
    runner.check("several normal guests are waiting", normalWaiting >= 3);

    // The urgent guest arrives last of all.
    Guest urgentGuest = service.findOrCreateGuest("660101-14-9876",
        "Urgent Test Guest", "019-9998888", "");
    WalkInRegistration urgent = new WalkInRegistration(data.nextRegistrationId(),
        urgentGuest.getGuestId(), LocalDateTime.now(),
        WalkInRegistration.PRIORITY_URGENT, "Wheelchair / mobility assistance",
        "RT01", 1);
    data.getRegistrationList().add(urgent);
    data.getWaitingList().enqueue(urgent, urgent.getPriority());

    runner.checkEquals("they go into the urgent lane", 1,
        data.getWaitingList().getUrgentCount());
    runner.check("the normal guests are all still waiting",
        data.getWaitingList().getNormalCount() >= 3);

    // The rule in action: last to arrive, first to be called.
    ServiceResult<WalkInRegistration> called = service.serveNextGuest(FRONT_DESK);
    runner.check("somebody is called", called.isSuccess());
    runner.checkEquals("and it is the urgent guest, who arrived last",
        urgent.getRegId(), called.getValue().getRegId());

    LocalDate from = LocalDate.now();
    LocalDate to = from.plusDays(1);

    ServiceResult<Booking> booked = service.convertRegistrationToBooking(
        urgent.getRegId(), from, to, 1, FRONT_DESK);
    runner.check("their booking is created", booked.isSuccess());

    Booking booking = booked.getValue();
    runner.checkEquals("carrying the urgency forward", Booking.PRIORITY_URGENT,
        booking.getPriority());

    // Nothing of that type is ready.
    ListInterface<Room> rooms = data.getRoomList();
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if ("RT01".equals(room.getTypeId())) {
        room.setOccupancyStatus(Room.VACANT);
        room.setHousekeepingStatus(Room.DIRTY);
        room.setOutOfService(false);
      }
    }
    runner.check("no RT01 room is ready",
        service.findAvailableRooms("RT01", from, to).isEmpty());

    // The front desk can no longer ask for a room to be cleaned out of turn:
    // a dirty room is simply not sellable, and the booking waits PENDING.
    runner.checkEquals("the urgent booking waits as PENDING",
        Booking.STATUS_PENDING, booking.getBookingStatus());

    // Housekeeping works its own round. Once a room comes through to ready,
    // the waiting guest can have it.
    HousekeepingTask task = data.findOpenTaskForRoom("1001");
    runner.check("the dirty room has a cleaning task", task != null);

    if (!HousekeepingTask.CLEANING_IN_PROGRESS.equals(task.getStatus())) {
      service.updateTaskStatus(task.getTaskId(),
          HousekeepingTask.CLEANING_IN_PROGRESS, HOUSEKEEPER, null);
    }
    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.INSPECTED, HOUSEKEEPER, null);
    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.READY_FOR_CHECK_IN, SUPERVISOR, null);

    Room prepared = data.findRoom(task.getRoomNo());
    runner.checkEquals("the room is ready", Room.READY_FOR_CHECK_IN,
        prepared.getHousekeepingStatus());

    ServiceResult<Booking> assigned = service.assignRoom(booking.getBookingId(),
        task.getRoomNo(), FRONT_DESK, RoomAssignment.REASON_INITIAL);
    runner.check("and the waiting guest gets it", assigned.isSuccess());
    runner.checkEquals("their booking is confirmed at last",
        Booking.STATUS_CONFIRMED, booking.getBookingStatus());
  }

  /** A room must go round the loop and come back sellable. */
  private void testRoomIsRecycledForTheNextGuest() {
    runner.suite("A room is recycled from one guest to the next");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Room room = data.findRoom("2003");
    room.setOccupancyStatus(Room.VACANT);
    room.setHousekeepingStatus(Room.READY_FOR_CHECK_IN);
    room.setOutOfService(false);

    LocalDate firstFrom = LocalDate.now();
    LocalDate firstTo = firstFrom.plusDays(1);

    // First guest.
    Guest first = service.findOrCreateGuest("900101-14-1111", "First Guest",
        "012-1111111", "");
    Booking firstBooking = new Booking(data.nextBookingId(), first.getGuestId(),
        "RT02", firstFrom, firstTo, 2, Booking.PRIORITY_NORMAL,
        Booking.SOURCE_ONLINE, null, 420.00, LocalDateTime.now(), FRONT_DESK);
    data.addBooking(firstBooking);

    service.assignRoom(firstBooking.getBookingId(), "2003", FRONT_DESK,
        RoomAssignment.REASON_INITIAL);

    // Paid at the counter before the key is handed over.
    Invoice firstInvoice = data.findInvoiceByBooking(firstBooking.getBookingId());
    service.recordPayment(firstInvoice.getInvoiceId(),
        firstInvoice.getOutstandingBalance(), Payment.CASH, null, FRONT_DESK);

    service.checkIn(firstBooking.getBookingId());
    service.checkOut(firstBooking.getBookingId(), FRONT_DESK);

    runner.checkEquals("after the first guest the room is dirty", Room.DIRTY,
        room.getHousekeepingStatus());

    // Second guest cannot have it yet.
    LocalDate secondFrom = firstTo;
    LocalDate secondTo = secondFrom.plusDays(2);

    Guest second = service.findOrCreateGuest("910202-14-2222", "Second Guest",
        "012-2222222", "");
    Booking secondBooking = new Booking(data.nextBookingId(), second.getGuestId(),
        "RT02", secondFrom, secondTo, 2, Booking.PRIORITY_NORMAL,
        Booking.SOURCE_PHONE, null, 420.00, LocalDateTime.now(), FRONT_DESK);
    data.addBooking(secondBooking);

    ServiceResult<Booking> tooSoon = service.assignRoom(secondBooking.getBookingId(),
        "2003", FRONT_DESK, RoomAssignment.REASON_INITIAL);
    runner.check("the next guest cannot have the room while it is dirty",
        tooSoon.isFailure());
    runner.check("and is told why",
        tooSoon.getMessage().toLowerCase().contains("not ready"));

    // Clean it.
    HousekeepingTask task = data.findOpenTaskForRoom("2003");
    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.CLEANING_IN_PROGRESS, HOUSEKEEPER, null);
    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.INSPECTED, HOUSEKEEPER, null);
    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.READY_FOR_CHECK_IN, SUPERVISOR, null);

    ServiceResult<Booking> nowFine = service.assignRoom(secondBooking.getBookingId(),
        "2003", FRONT_DESK, RoomAssignment.REASON_INITIAL);
    runner.check("once cleaned, the next guest can have it", nowFine.isSuccess());

    // Same-day turnover is allowed - the previous stay ended the day this one
    // begins, which is how a hotel actually works.
    runner.checkEquals("the second booking is confirmed", Booking.STATUS_CONFIRMED,
        secondBooking.getBookingStatus());

    // Two separate stays are on record for the room.
    ListInterface<RoomAssignment> history = data.getAssignmentList().filter(
        assignment -> "2003".equals(assignment.getRoomNo()));
    runner.check("the room's assignment history holds both stays",
        history.getNumberOfEntries() >= 2);
  }

  // ==================================================================
  // LOYALTY END TO END
  // ==================================================================

  /** A member earns points from a real stay, then spends them on a real bill. */
  private void testMemberJourneyEarnAndSpend() {
    runner.suite("A member earns points from a stay and spends them on a bill");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Guest guest = service.findOrCreateGuest("950505-14-5555", "Loyalty Test Guest",
        "016-5555555", "loyalty@example.com");

    ServiceResult<Member> enrolled = service.enrolMember(guest.getGuestId());
    runner.check("the guest can be enrolled", enrolled.isSuccess());

    Member member = enrolled.getValue();
    runner.checkEquals("they start on Silver", Member.SILVER, member.getTier());
    runner.checkEquals("with no points", 0, member.getPointsBalance());

    // A stay expensive enough to move them up a tier.
    Room room = data.findRoom("3001");
    room.setOutOfService(false);
    room.setOccupancyStatus(Room.VACANT);
    room.setHousekeepingStatus(Room.READY_FOR_CHECK_IN);

    LocalDate from = LocalDate.now();
    LocalDate to = from.plusDays(2);

    Booking booking = new Booking(data.nextBookingId(), guest.getGuestId(),
        "RT03", from, to, 2, Booking.PRIORITY_NORMAL, Booking.SOURCE_ONLINE,
        null, 780.00, LocalDateTime.now(), FRONT_DESK);
    data.addBooking(booking);

    service.assignRoom(booking.getBookingId(), "3001", FRONT_DESK,
        RoomAssignment.REASON_INITIAL);

    // Paid at the counter before the key is handed over.
    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    service.recordPayment(invoice.getInvoiceId(), invoice.getOutstandingBalance(),
        Payment.BANK_TRANSFER, "MBB-TEST123", FRONT_DESK);

    service.checkIn(booking.getBookingId());
    service.checkOut(booking.getBookingId(), FRONT_DESK);

    runner.check("the stay earned points", member.getPointsBalance() > 0);
    runner.checkEquals("the balance and lifetime totals agree on a first stay",
        member.getPointsBalance(), member.getLifetimePoints());

    // The bill was over 1000, so at the Silver rate of one point per ringgit
    // they should have crossed into Gold.
    runner.checkEquals("and the tier moved up", Member.GOLD, member.getTier());

    PointTransaction earned = data.getTransactionList().search(
        txn -> booking.getBookingId().equals(txn.getBookingId()));
    runner.check("the award is in the ledger", earned != null);
    runner.checkEquals("with the running balance recorded",
        member.getPointsBalance(), earned.getBalanceAfter());

    // They were told about it.
    ListInterface<entity.Notification> messages = data.getNotificationList().filter(
        note -> member.getMemberId().equals(note.getMemberId()));
    runner.check("the member was notified", messages.getNumberOfEntries() >= 2);
    runner.check("including about the tier upgrade",
        messages.countIf(note ->
            entity.Notification.TIER_UPGRADE.equals(note.getType())) >= 1);

    // --- Spending them --------------------------------------------
    int beforeSpend = member.getPointsBalance();
    int lifetimeBefore = member.getLifetimePoints();

    ServiceResult<Redemption> requested =
        service.requestRedemption(member.getMemberId(), "RW001");
    runner.check("they can request a reward", requested.isSuccess());

    Redemption redemption = requested.getValue();
    runner.checkEquals("nothing is taken until it is approved", beforeSpend,
        member.getPointsBalance());

    while (data.getPendingRedemptions().contains(redemption)) {
      service.processNextRedemption(MANAGER);
    }

    runner.checkEquals("it is approved", Redemption.APPROVED, redemption.getStatus());
    runner.checkEquals("and the points are taken",
        beforeSpend - redemption.getPointsUsed(), member.getPointsBalance());

    // The rule that protects a member's standing.
    runner.checkEquals("spending does not touch the lifetime total",
        lifetimeBefore, member.getLifetimePoints());
    runner.checkEquals("so the tier is unchanged", Member.GOLD, member.getTier());

    // --- Using it on a second stay --------------------------------
    Booking second = new Booking(data.nextBookingId(), guest.getGuestId(),
        "RT01", to, to.plusDays(2), 1, Booking.PRIORITY_NORMAL,
        Booking.SOURCE_ONLINE, null, 150.00, LocalDateTime.now(), FRONT_DESK);
    data.addBooking(second);

    Room small = data.findRoom("1001");
    small.setOutOfService(false);
    small.setOccupancyStatus(Room.VACANT);
    small.setHousekeepingStatus(Room.READY_FOR_CHECK_IN);

    service.assignRoom(second.getBookingId(), "1001", FRONT_DESK,
        RoomAssignment.REASON_INITIAL);

    Invoice secondInvoice = data.findInvoiceByBooking(second.getBookingId());
    double beforeDiscount = secondInvoice.getTotalAmount();

    ServiceResult<Invoice> discounted = service.applyRedemptionToInvoice(
        redemption.getRedemptionId(), second.getBookingId());
    runner.check("the reward can be used on the next stay", discounted.isSuccess());
    runner.check("and the bill is smaller for it",
        secondInvoice.getTotalAmount() < beforeDiscount);
  }

  // ==================================================================
  // CONSISTENCY
  // ==================================================================

  /**
   * After a lot of activity, the things that must always be true still are.
   *
   * These are invariants rather than individual results: no room in two places
   * at once, no bill disagreeing with its payments, no member's balance
   * disagreeing with its ledger.
   */
  private void testSystemStaysConsistentUnderLoad() {
    runner.suite("The system stays consistent after a busy day");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    // Run a day's worth of activity.
    for (int i = 1; i <= 12; i++) {
      Guest guest = service.findOrCreateGuest("8001" + String.format("%02d", i)
          + "-14-70" + String.format("%02d", i), "Load Guest " + i,
          "012-70000" + String.format("%02d", i), "");

      boolean urgent = (i % 4 == 0);
      WalkInRegistration reg = new WalkInRegistration(data.nextRegistrationId(),
          guest.getGuestId(), LocalDateTime.now().minusMinutes(120 - i * 5),
          urgent ? WalkInRegistration.PRIORITY_URGENT
                 : WalkInRegistration.PRIORITY_NORMAL,
          urgent ? "Medical or emergency situation" : null,
          (i % 2 == 0) ? "RT01" : "RT02", 1 + (i % 3));

      data.getRegistrationList().add(reg);
      data.getWaitingList().enqueue(reg, reg.getPriority());
    }

    // Serve half of them.
    for (int i = 1; i <= 6; i++) {
      ServiceResult<WalkInRegistration> called = service.serveNextGuest(FRONT_DESK);
      if (called.isFailure()) {
        break;
      }

      WalkInRegistration reg = called.getValue();
      LocalDate from = LocalDate.now();
      ServiceResult<Booking> booked = service.convertRegistrationToBooking(
          reg.getRegId(), from, from.plusDays(reg.getRequestedNights()), 1,
          FRONT_DESK);

      if (booked.isSuccess()) {
        ListInterface<Room> free = service.findAvailableRooms(
            booked.getValue().getTypeId(), from,
            from.plusDays(reg.getRequestedNights()));

        if (!free.isEmpty()) {
          service.assignRoom(booked.getValue().getBookingId(),
              free.getEntry(1).getRoomNo(), FRONT_DESK,
              RoomAssignment.REASON_INITIAL);
        }
      }
    }

    // --- Now check what must always hold --------------------------

    // No room may be given to two live bookings over the same dates.
    ListInterface<Booking> bookings = data.getBookingList();
    boolean doubleBooked = false;

    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking a = bookings.getEntry(i);
      if (!a.holdsRoom() || a.getRoomNo() == null) {
        continue;
      }

      for (int j = i + 1; j <= bookings.getNumberOfEntries(); j++) {
        Booking b = bookings.getEntry(j);
        if (!b.holdsRoom() || !a.getRoomNo().equals(b.getRoomNo())) {
          continue;
        }
        if (a.overlaps(b.getCheckInDate(), b.getCheckOutDate())) {
          doubleBooked = true;
        }
      }
    }
    runner.check("no room is double-booked over the same dates", !doubleBooked);

    // No live booking may hold a room that housekeeping says is dirty.
    boolean dirtyRoomSold = false;
    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking booking = bookings.getEntry(i);
      if (!Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())
          || booking.getRoomNo() == null) {
        continue;
      }

      Room room = data.findRoom(booking.getRoomNo());
      if (room != null && Room.DIRTY.equals(room.getHousekeepingStatus())) {
        dirtyRoomSold = true;
      }
    }
    runner.check("no guest is checked into a dirty room", !dirtyRoomSold);

    // Every bill must agree with the payments taken against it.
    ListInterface<Invoice> invoices = data.getInvoiceList();
    boolean billMismatch = false;
    for (int i = 1; i <= invoices.getNumberOfEntries(); i++) {
      Invoice invoice = invoices.getEntry(i);
      double fromPayments = service.sumPayments(invoice.getInvoiceId());
      if (Math.abs(fromPayments - invoice.getAmountPaid()) > 0.005) {
        billMismatch = true;
      }
    }
    runner.check("every bill agrees with its payments", !billMismatch);

    // Every member's balance must agree with the last row of their ledger.
    ListInterface<Member> members = data.getMemberList();
    boolean ledgerMismatch = false;
    for (int i = 1; i <= members.getNumberOfEntries(); i++) {
      Member member = members.getEntry(i);
      ListInterface<PointTransaction> ledger = data.getTransactionList().filter(
          txn -> member.getMemberId().equals(txn.getMemberId()));

      if (!ledger.isEmpty()) {
        PointTransaction last = ledger.getEntry(ledger.getNumberOfEntries());
        if (last.getBalanceAfter() != member.getPointsBalance()) {
          ledgerMismatch = true;
        }
      }
    }
    runner.check("every member's balance agrees with their ledger", !ledgerMismatch);

    // A guest cannot be waiting and being served at the same time.
    ListInterface<WalkInRegistration> queued =
        data.getWaitingList().toServiceOrder();
    boolean queueMismatch = false;
    for (int i = 1; i <= queued.getNumberOfEntries(); i++) {
      if (!queued.getEntry(i).isWaiting()) {
        queueMismatch = true;
      }
    }
    runner.check("everyone in the queue is actually waiting", !queueMismatch);

    // Every waiting registration must be in the queue, and no others.
    int waitingRecords = data.getRegistrationList().countIf(
        WalkInRegistration::isWaiting);
    runner.checkEquals("the queue holds exactly the waiting registrations",
        waitingRecords, data.getWaitingList().getNumberOfEntries());

    // Every ID must be unique.
    runner.check("booking IDs are unique", allUnique(data, "booking"));
    runner.check("guest IDs are unique", allUnique(data, "guest"));

    // Everything must survive being written and read back.
    data.saveAll();
    ResortData reloaded = new ResortData();
    runner.checkEquals("bookings survive a save and reload",
        data.getBookingList().getNumberOfEntries(),
        reloaded.getBookingList().getNumberOfEntries());
    runner.checkEquals("guests survive too", data.getGuestList().getNumberOfEntries(),
        reloaded.getGuestList().getNumberOfEntries());
    runner.checkEquals("and the queue is rebuilt to the same size",
        data.getWaitingList().getNumberOfEntries(),
        reloaded.getWaitingList().getNumberOfEntries());
  }

  /** Whether every ID in a table appears exactly once. */
  private boolean allUnique(ResortData data, String table) {
    if ("booking".equals(table)) {
      ListInterface<Booking> bookings = data.getBookingList();
      for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
        final String id = bookings.getEntry(i).getBookingId();
        if (bookings.countIf(b -> id.equals(b.getBookingId())) != 1) {
          return false;
        }
      }
      return true;
    }

    ListInterface<Guest> guests = data.getGuestList();
    for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
      final String id = guests.getEntry(i).getGuestId();
      if (guests.countIf(g -> id.equals(g.getGuestId())) != 1) {
        return false;
      }
    }
    return true;
  }

  /** The very first run, with no data files at all. */
  private void testStartupFromNothing() {
    runner.suite("The first run, with no data files");

    ResortData wiped = new ResortData();
    wiped.deleteAllFiles();

    ResortData data = new ResortData();

    runner.check("staff are seeded", !data.getStaffList().isEmpty());
    runner.check("room types are seeded", !data.getRoomTypeList().isEmpty());
    runner.check("rooms are seeded", !data.getRoomList().isEmpty());
    runner.check("guests are seeded", !data.getGuestList().isEmpty());
    runner.check("registrations are seeded", !data.getRegistrationList().isEmpty());
    runner.check("bookings are seeded", !data.getBookingList().isEmpty());
    runner.check("invoices are seeded", !data.getInvoiceList().isEmpty());
    runner.check("cleaning tasks are seeded", !data.getTaskList().isEmpty());
    runner.check("members are seeded", !data.getMemberList().isEmpty());
    runner.check("rewards are seeded", !data.getRewardList().isEmpty());

    // The seeded data must be internally consistent, or every screen built on
    // it starts out wrong.
    ListInterface<Booking> bookings = data.getBookingList();
    boolean danglingGuest = false;
    boolean danglingRoom = false;

    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking booking = bookings.getEntry(i);
      if (data.findGuest(booking.getGuestId()) == null) {
        danglingGuest = true;
      }
      if (booking.getRoomNo() != null && data.findRoom(booking.getRoomNo()) == null) {
        danglingRoom = true;
      }
    }
    runner.check("every seeded booking points at a real guest", !danglingGuest);
    runner.check("and at a real room", !danglingRoom);

    ListInterface<Member> members = data.getMemberList();
    boolean danglingMember = false;
    for (int i = 1; i <= members.getNumberOfEntries(); i++) {
      if (data.findGuest(members.getEntry(i).getGuestId()) == null) {
        danglingMember = true;
      }
    }
    runner.check("every seeded member points at a real guest", !danglingMember);

    ListInterface<HousekeepingTask> tasks = data.getTaskList();
    boolean danglingTaskRoom = false;
    for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
      if (data.findRoom(tasks.getEntry(i).getRoomNo()) == null) {
        danglingTaskRoom = true;
      }
    }
    runner.check("every seeded task points at a real room", !danglingTaskRoom);

    // The seed must obey the same rules the running system enforces, or every
    // screen built on it starts out showing an impossible state.
    boolean seededDirtyOccupied = false;
    boolean seededOccupiedWithoutGuest = false;

    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking booking = bookings.getEntry(i);
      if (!Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())
          || booking.getRoomNo() == null) {
        continue;
      }

      Room room = data.findRoom(booking.getRoomNo());
      if (room == null) {
        continue;
      }
      if (Room.DIRTY.equals(room.getHousekeepingStatus())) {
        seededDirtyOccupied = true;
      }
      if (!Room.OCCUPIED.equals(room.getOccupancyStatus())) {
        seededOccupiedWithoutGuest = true;
      }
    }

    runner.check("no seeded guest is checked into a dirty room",
        !seededDirtyOccupied);
    runner.check("every seeded checked-in booking has its room marked occupied",
        !seededOccupiedWithoutGuest);

    // The queues must have been rebuilt from the seeded records.
    runner.check("the waiting queue is populated",
        data.getWaitingList().getNumberOfEntries() > 0);
    runner.check("and so is the cleaning queue",
        data.getCleaningQueue().getNumberOfEntries() > 0);

    // The seed must include something in flight, or the demonstration has
    // nothing interesting to show.
    Booking urgentPending = data.getBookingList().search(
        booking -> Booking.STATUS_PENDING.equals(booking.getBookingStatus())
            && booking.isUrgent());
    runner.check("an urgent booking is waiting on a room, ready to demonstrate",
        urgentPending != null);

    if (urgentPending != null) {
      HousekeepingTask waitingOn = data.getTaskList().search(
          task -> urgentPending.getBookingId().equals(task.getReservedForBookingId()));
      runner.check("and a room is being cleaned for it", waitingOn != null);

      if (waitingOn != null) {
        runner.checkEquals("in the urgent lane", HousekeepingTask.PRIORITY_URGENT,
            waitingOn.getPriority());
      }
    }

    // A second start must load rather than seed again.
    int guestsFirstRun = data.getGuestList().getNumberOfEntries();
    ResortData secondRun = new ResortData();
    runner.checkEquals("a second start loads the same data, it does not re-seed",
        guestsFirstRun, secondRun.getGuestList().getNumberOfEntries());
  }
}
