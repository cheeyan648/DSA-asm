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
 * Integration tests - what happens where two or more modules meet.
 *
 * These are the checks that could not exist before the modules shared their
 * data. Each one asserts that an action in one module actually changed
 * something in another: that checking a guest out really did dirty the room
 * and really did award the points, and that the front desk really is refused a
 * room housekeeping has not finished with.
 *
 * Every test runs against its own data files in a scratch directory, so a run
 * never touches the real records and never depends on what a previous run left
 * behind.
 *
 * @author Tan Chee Yan
 */
public class IntegrationTest {

  private final TestRunner runner = new TestRunner();

  /** Whoever is notionally on the desk for these tests. */
  private static final String STAFF = "ST001";

  public static void main(String[] args) {
    IntegrationTest tests = new IntegrationTest();
    boolean allPassed = tests.runAll();
    System.exit(allPassed ? 0 : 1);
  }

  public boolean runAll() {
    System.out.println();
    System.out.println("=".repeat(76));
    System.out.println("  INTEGRATION TESTS - where the modules meet");
    System.out.println("=".repeat(76));

    testSharedDataIsOneCopy();
    testRoomAvailabilityNeedsBothModules();
    testWalkInBecomesBooking();
    testUrgencyTravelsAcrossModules();
    testCheckOutTriggersHousekeepingAndLoyalty();
    testCheckOutRefusedWithUnpaidBill();
    testCleaningReleasesRoomBackToFrontDesk();
    testFailedInspectionKeepsRoomUnsellable();
    testRollbackRestoresRoomAndKeepsHistory();
    testRedemptionDiscountsALiveBill();
    testPointsAreNotAwardedTwice();
    testCancelledBookingReleasesItsRoom();
    testPersistenceRoundTrip();

    return runner.report("INTEGRATION TEST SUMMARY");
  }

  /** A registry backed by its own throwaway files. */
  private ResortData freshData() {
    ResortData data = new ResortData();
    data.deleteAllFiles();
    data = new ResortData();
    return data;
  }

  // ==================================================================
  // THE SHARED REGISTRY
  // ==================================================================

  /**
   * The change that makes everything else possible.
   *
   * Before this, each module built its own lists, so a room one module updated
   * was unchanged as far as the others were concerned.
   */
  private void testSharedDataIsOneCopy() {
    runner.suite("The four modules share one copy of the data");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    runner.check("the service holds the same registry it was given",
        service.getData() == data);

    // A change made through one path must be visible through every other.
    Room room = data.findRoom("1005");
    room.setHousekeepingStatus(Room.DIRTY);

    Room sameRoomViaList = data.getRoomList().search(
        candidate -> "1005".equals(candidate.getRoomNo()));

    runner.check("the map and the list hold the same object",
        room == sameRoomViaList);
    runner.checkEquals("so a change through one is seen through the other",
        Room.DIRTY, sameRoomViaList.getHousekeepingStatus());

    Booking booking = data.findBooking("BK0001");
    Booking sameBookingViaList = data.getBookingList().search(
        candidate -> "BK0001".equals(candidate.getBookingId()));
    runner.check("the tree and the list hold the same booking",
        booking == sameBookingViaList);
  }

  // ==================================================================
  // FRONT DESK AND HOUSEKEEPING
  // ==================================================================

  /**
   * The defect the separate modules had: the front desk could sell a room
   * housekeeping had not finished cleaning.
   */
  private void testRoomAvailabilityNeedsBothModules() {
    runner.suite("A room is sellable only when both modules agree");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    LocalDate from = LocalDate.now().plusDays(30);
    LocalDate to = from.plusDays(2);

    Room room = data.findRoom("1005");
    room.setOccupancyStatus(Room.VACANT);
    room.setHousekeepingStatus(Room.READY_FOR_CHECK_IN);
    runner.check("a vacant, cleaned room is available",
        service.isRoomAvailable("1005", from, to, null));

    // Housekeeping's word alone takes it off the market.
    room.setHousekeepingStatus(Room.DIRTY);
    runner.check("housekeeping marking it dirty makes it unavailable",
        !service.isRoomAvailable("1005", from, to, null));

    room.setHousekeepingStatus(Room.CLEANING_IN_PROGRESS);
    runner.check("a room being cleaned is still unavailable",
        !service.isRoomAvailable("1005", from, to, null));

    room.setHousekeepingStatus(Room.INSPECTED);
    runner.check("inspected but not signed off is still unavailable",
        !service.isRoomAvailable("1005", from, to, null));

    room.setHousekeepingStatus(Room.READY_FOR_CHECK_IN);
    runner.check("only READY_FOR_CHECK_IN makes it available again",
        service.isRoomAvailable("1005", from, to, null));

    // And the front desk's own concern - somebody else's booking.
    Booking clash = new Booking("BKTEST1", "G0001", "RT03", from, to, 2,
        Booking.PRIORITY_NORMAL, Booking.SOURCE_ONLINE, null, 260.00,
        LocalDateTime.now(), STAFF);
    clash.setRoomNo("1005");
    clash.setBookingStatus(Booking.STATUS_CONFIRMED);
    data.addBooking(clash);

    runner.check("a clean room already booked for those dates is unavailable",
        !service.isRoomAvailable("1005", from, to, null));
    runner.check("but it is available for dates that do not clash",
        service.isRoomAvailable("1005", to.plusDays(1), to.plusDays(3), null));

    // A cancelled booking must stop blocking its room.
    clash.setBookingStatus(Booking.STATUS_CANCELLED);
    runner.check("a cancelled booking no longer blocks the room",
        service.isRoomAvailable("1005", from, to, null));
  }

  // ==================================================================
  // WALK-IN TO BOOKING
  // ==================================================================

  /** The handover from M1 to M3 - a guest at the counter becomes a stay. */
  private void testWalkInBecomesBooking() {
    runner.suite("A served walk-in becomes a booking  (M1 to M3)");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    int waitingBefore = data.getWaitingList().getNumberOfEntries();
    runner.check("there are guests waiting to start with", waitingBefore > 0);

    ServiceResult<WalkInRegistration> called = service.serveNextGuest(STAFF);
    runner.check("a guest can be called", called.isSuccess());

    WalkInRegistration reg = called.getValue();
    runner.checkEquals("they are now IN_SERVICE",
        WalkInRegistration.STATUS_IN_SERVICE, reg.getStatus());
    runner.check("their call time is stamped", reg.getCalledAt() != null);
    runner.checkEquals("and who called them is recorded", STAFF, reg.getServedBy());
    runner.checkEquals("they have left the queue", waitingBefore - 1,
        data.getWaitingList().getNumberOfEntries());

    // A guest being served must not still appear as waiting.
    ListInterface<WalkInRegistration> stillWaiting =
        data.getWaitingList().toServiceOrder();
    runner.check("and cannot be found in the waiting list any more",
        !stillWaiting.contains(reg));

    LocalDate from = LocalDate.now();
    LocalDate to = from.plusDays(reg.getRequestedNights());

    ServiceResult<Booking> booked = service.convertRegistrationToBooking(
        reg.getRegId(), from, to, 1, STAFF);
    runner.check("the registration becomes a booking", booked.isSuccess());

    Booking booking = booked.getValue();
    runner.checkEquals("the booking starts PENDING with no room",
        Booking.STATUS_PENDING, booking.getBookingStatus());
    runner.checkEquals("no room is assigned yet", null, booking.getRoomNo());
    runner.checkEquals("its source is WALK_IN", Booking.SOURCE_WALK_IN,
        booking.getSource());
    runner.checkEquals("it points back at the registration", reg.getRegId(),
        booking.getRegId());
    runner.checkEquals("it carries the same guest", reg.getGuestId(),
        booking.getGuestId());

    // And the link is written back the other way, which is what closes the
    // loop the separate modules left open.
    runner.checkEquals("the registration is now BOOKED",
        WalkInRegistration.STATUS_BOOKED, reg.getStatus());
    runner.checkEquals("and points forward at its booking",
        booking.getBookingId(), reg.getBookingId());

    // A guest cannot be booked in twice.
    ServiceResult<Booking> again = service.convertRegistrationToBooking(
        reg.getRegId(), from, to, 1, STAFF);
    runner.check("the same registration cannot be booked twice", again.isFailure());
  }

  /**
   * The priority chain - an urgency granted at the door reaches housekeeping
   * unchanged, across three modules.
   */
  private void testUrgencyTravelsAcrossModules() {
    runner.suite("Urgency travels M1 to M3 to M2 unchanged");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    // Put an urgent guest at the front of the queue.
    WalkInRegistration urgent = new WalkInRegistration("WR9001", "G0001",
        LocalDateTime.now(), WalkInRegistration.PRIORITY_URGENT,
        "Medical or emergency situation", "RT02", 2);
    data.getRegistrationList().add(urgent);
    data.getWaitingList().enqueue(urgent, urgent.getPriority());

    ServiceResult<WalkInRegistration> called = service.serveNextGuest(STAFF);
    runner.checkEquals("the urgent guest is called first, whoever else is waiting",
        "WR9001", called.getValue().getRegId());

    LocalDate from = LocalDate.now();
    ServiceResult<Booking> booked = service.convertRegistrationToBooking(
        "WR9001", from, from.plusDays(2), 2, STAFF);
    runner.check("the booking is created", booked.isSuccess());

    Booking booking = booked.getValue();
    runner.checkEquals("the booking inherits URGENT rather than deciding again",
        Booking.PRIORITY_URGENT, booking.getPriority());
    runner.check("and isUrgent agrees", booking.isUrgent());

    // Make sure nothing of that type is ready, so the cleaning route is taken.
    ListInterface<Room> rooms = data.getRoomList();
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if ("RT02".equals(room.getTypeId())) {
        room.setOccupancyStatus(Room.VACANT);
        room.setHousekeepingStatus(Room.DIRTY);
      }
    }

    runner.check("no RT02 room is ready",
        service.findAvailableRooms("RT02", from, from.plusDays(2)).isEmpty());
    runner.check("but some could be cleaned",
        !service.findCleanableRooms("RT02", from, from.plusDays(2)).isEmpty());

    ServiceResult<HousekeepingTask> expedited =
        service.requestUrgentCleaning(booking.getBookingId(), STAFF);
    runner.check("housekeeping is asked to prepare one", expedited.isSuccess());

    HousekeepingTask task = expedited.getValue();
    runner.checkEquals("the task is reserved for that booking",
        booking.getBookingId(), task.getReservedForBookingId());
    runner.checkEquals("which puts it in the URGENT lane",
        HousekeepingTask.PRIORITY_URGENT, task.getPriority());

    // The urgency is derived, not typed - so cancelling the booking must
    // demote the task on its own.
    booking.setBookingStatus(Booking.STATUS_CANCELLED);
    service.refreshTaskPriority(task);
    runner.checkEquals("cancelling the booking drops the task to the normal lane",
        HousekeepingTask.PRIORITY_NORMAL, task.getPriority());
    runner.check("but the cleaning is not cancelled - the room is still dirty",
        !HousekeepingTask.READY_FOR_CHECK_IN.equals(task.getStatus()));
  }

  // ==================================================================
  // CHECK-OUT AND ITS CONSEQUENCES
  // ==================================================================

  /**
   * One action in M3 with consequences in M2 and M4 - the clearest single
   * demonstration that the modules are joined.
   */
  private void testCheckOutTriggersHousekeepingAndLoyalty() {
    runner.suite("Checking out dirties the room AND awards points  (M3 to M2, M4)");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    // BK0002 is checked in, in room 2001, for a guest who is a member.
    Booking booking = data.findBooking("BK0002");
    runner.checkEquals("the guest is checked in", Booking.STATUS_CHECKED_IN,
        booking.getBookingStatus());

    Member member = data.findMemberByGuest(booking.getGuestId());
    runner.check("and is a loyalty member", member != null);

    int pointsBefore = member.getPointsBalance();
    int tasksBefore = data.getTaskList().getNumberOfEntries();
    String roomNo = booking.getRoomNo();

    // Settle the bill first, since check-out is refused otherwise.
    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    service.recordPayment(invoice.getInvoiceId(), invoice.getOutstandingBalance(),
        Payment.CASH, null, STAFF);
    runner.check("the bill is settled", invoice.isSettled());

    ServiceResult<Booking> out = service.checkOut(booking.getBookingId(), STAFF);
    runner.check("the guest can check out", out.isSuccess());
    runner.checkEquals("the booking is CHECKED_OUT", Booking.STATUS_CHECKED_OUT,
        booking.getBookingStatus());

    // What M2 should now see.
    Room room = data.findRoom(roomNo);
    runner.checkEquals("the room is vacant again", Room.VACANT,
        room.getOccupancyStatus());
    runner.checkEquals("and has been marked dirty", Room.DIRTY,
        room.getHousekeepingStatus());
    runner.check("so it cannot be sold to anybody", !room.isAssignable());

    runner.checkEquals("a cleaning task was raised automatically",
        tasksBefore + 1, data.getTaskList().getNumberOfEntries());

    HousekeepingTask raised = data.findOpenTaskForRoom(roomNo);
    runner.check("the task exists for that room", raised != null);
    runner.checkEquals("it is a check-out clean", HousekeepingTask.TYPE_CHECKOUT_CLEAN,
        raised.getTaskType());
    runner.checkEquals("and points back at the booking that caused it",
        booking.getBookingId(), raised.getBookingId());
    runner.check("it is waiting in the cleaning queue",
        data.getCleaningQueue().toServiceOrder().contains(raised));

    // What M4 should now see.
    runner.check("the member's points went up", member.getPointsBalance() > pointsBefore);

    PointTransaction earned = data.getTransactionList().search(
        txn -> booking.getBookingId().equals(txn.getBookingId())
            && PointTransaction.EARN.equals(txn.getTxnType()));
    runner.check("a ledger row records the award", earned != null);
    runner.checkEquals("the balance matches the ledger", member.getPointsBalance(),
        earned.getBalanceAfter());

    // The points must reflect the tier the member was on.
    int expected = (int) Math.round(invoice.getTotalAmount()
        * ResortService.POINTS_PER_RINGGIT * Member.multiplierFor(member.getTier()));
    runner.check("the points are the bill times the tier multiplier",
        Math.abs(earned.getPoints() - expected) <= 1);

    // The assignment history is closed off.
    RoomAssignment open = data.findOpenAssignment(booking.getBookingId());
    runner.checkEquals("the room assignment is closed", null, open);
  }

  /** Money first: once the guest has gone there is nobody to collect from. */
  private void testCheckOutRefusedWithUnpaidBill() {
    runner.suite("Check-out is refused while the bill is unpaid");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Booking booking = data.findBooking("BK0002");
    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    runner.check("the bill starts unsettled", !invoice.isSettled());

    ServiceResult<Booking> refused = service.checkOut(booking.getBookingId(), STAFF);
    runner.check("check-out is refused", refused.isFailure());
    runner.check("and says how much is owed",
        refused.getMessage().contains("outstanding"));
    runner.checkEquals("the guest is still checked in", Booking.STATUS_CHECKED_IN,
        booking.getBookingStatus());

    // The room must not have been released by a refused check-out.
    Room room = data.findRoom(booking.getRoomNo());
    runner.checkEquals("and the room is still occupied", Room.OCCUPIED,
        room.getOccupancyStatus());

    // A part payment is not enough.
    service.recordPayment(invoice.getInvoiceId(), 10.00, Payment.CASH, null, STAFF);
    runner.checkEquals("a part payment leaves it PARTIAL", Invoice.PARTIAL,
        invoice.getPaymentStatus());
    runner.check("and check-out is still refused",
        service.checkOut(booking.getBookingId(), STAFF).isFailure());

    // Overpaying is refused too.
    ServiceResult<Payment> tooMuch = service.recordPayment(invoice.getInvoiceId(),
        invoice.getOutstandingBalance() + 100.00, Payment.CASH, null, STAFF);
    runner.check("paying more than is owed is refused", tooMuch.isFailure());

    // Settling it releases the check-out.
    service.recordPayment(invoice.getInvoiceId(), invoice.getOutstandingBalance(),
        Payment.CASH, null, STAFF);
    runner.check("once settled, check-out succeeds",
        service.checkOut(booking.getBookingId(), STAFF).isSuccess());
  }

  /** The other direction - M2 hands a room back to M3. */
  private void testCleaningReleasesRoomBackToFrontDesk() {
    runner.suite("Finishing the cleaning releases the room  (M2 to M3)");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    LocalDate from = LocalDate.now().plusDays(40);
    LocalDate to = from.plusDays(2);

    // Room 1001 starts dirty with task HK0001 against it.
    HousekeepingTask task = data.findOpenTaskForRoom("1001");
    runner.check("room 1001 has an open task", task != null);

    Room room = data.findRoom("1001");
    room.setOccupancyStatus(Room.VACANT);
    runner.check("a dirty room cannot be sold",
        !service.isRoomAvailable("1001", from, to, null));

    ServiceResult<HousekeepingTask> started = service.updateTaskStatus(
        task.getTaskId(), HousekeepingTask.CLEANING_IN_PROGRESS, "ST003", null);
    runner.check("cleaning can be started", started.isSuccess());
    runner.checkEquals("the room follows the task", Room.CLEANING_IN_PROGRESS,
        room.getHousekeepingStatus());
    runner.check("it is still not sellable",
        !service.isRoomAvailable("1001", from, to, null));
    runner.check("and it has left the cleaning queue",
        !data.getCleaningQueue().toServiceOrder().contains(task));

    ServiceResult<HousekeepingTask> inspected = service.updateTaskStatus(
        task.getTaskId(), HousekeepingTask.INSPECTED, "ST003", null);
    runner.check("cleaning can be finished", inspected.isSuccess());
    runner.check("awaiting sign-off is still not sellable",
        !service.isRoomAvailable("1001", from, to, null));

    ServiceResult<HousekeepingTask> ready = service.updateTaskStatus(
        task.getTaskId(), HousekeepingTask.READY_FOR_CHECK_IN, "ST005", null);
    runner.check("the supervisor can sign it off", ready.isSuccess());
    runner.checkEquals("the room is now ready", Room.READY_FOR_CHECK_IN,
        room.getHousekeepingStatus());
    runner.check("the completion time is stamped", task.getCompletedAt() != null);
    runner.check("the room's last-cleaned time is stamped",
        room.getLastCleanedAt() != null);

    // The whole point of the exercise.
    runner.check("and the front desk can sell it again",
        service.isRoomAvailable("1001", from, to, null));
  }

  /** A failed inspection must put the room back, not let it through. */
  private void testFailedInspectionKeepsRoomUnsellable() {
    runner.suite("A failed inspection sends the room back to be cleaned");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    HousekeepingTask task = data.findOpenTaskForRoom("1001");
    Room room = data.findRoom("1001");
    room.setOccupancyStatus(Room.VACANT);

    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.CLEANING_IN_PROGRESS, "ST003", null);
    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.INSPECTED, "ST003", null);

    int failuresBefore = task.getInspectionFailCount();

    ServiceResult<HousekeepingTask> failed = service.updateTaskStatus(
        task.getTaskId(), HousekeepingTask.DIRTY, "ST005", "Bathroom not done");
    runner.check("an inspection can be failed", failed.isSuccess());

    runner.checkEquals("the room goes back to dirty", Room.DIRTY,
        room.getHousekeepingStatus());
    runner.checkEquals("the failure is counted", failuresBefore + 1,
        task.getInspectionFailCount());
    runner.check("the room goes back into the cleaning queue",
        data.getCleaningQueue().toServiceOrder().contains(task));

    LocalDate from = LocalDate.now().plusDays(40);
    runner.check("and it certainly cannot be sold",
        !service.isRoomAvailable("1001", from, from.plusDays(2), null));

    // The workflow must refuse the shortcut.
    ServiceResult<HousekeepingTask> shortcut = service.updateTaskStatus(
        task.getTaskId(), HousekeepingTask.READY_FOR_CHECK_IN, "ST005", null);
    runner.check("a dirty room cannot jump straight to ready", shortcut.isFailure());
    runner.check("and the refusal explains why",
        shortcut.getMessage().toLowerCase().contains("skip")
            || shortcut.getMessage().toLowerCase().contains("valid"));
  }

  /**
   * A rollback must restore the room without erasing what happened, or reports
   * already produced would silently stop adding up.
   */
  private void testRollbackRestoresRoomAndKeepsHistory() {
    runner.suite("Rollback restores the room and keeps the history");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    HousekeepingTask task = data.findOpenTaskForRoom("1001");
    Room room = data.findRoom("1001");

    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.CLEANING_IN_PROGRESS, "ST003", null);
    runner.checkEquals("the room is being cleaned", HousekeepingTask.CLEANING_IN_PROGRESS,
        task.getStatus());

    int logsBefore = data.getStatusLogList().getNumberOfEntries();

    ServiceResult<HousekeepingTask> rolled = service.rollbackLastStatusChange("ST005");
    runner.check("the last change can be rolled back", rolled.isSuccess());

    runner.checkEquals("the task goes back to dirty", HousekeepingTask.DIRTY,
        task.getStatus());
    runner.checkEquals("and so does the room", Room.DIRTY,
        room.getHousekeepingStatus());
    runner.check("the room is back in the cleaning queue",
        data.getCleaningQueue().toServiceOrder().contains(task));

    // The history grows rather than shrinking.
    runner.checkEquals("a compensating row is added, not a row removed",
        logsBefore + 1, data.getStatusLogList().getNumberOfEntries());

    entity.RoomStatusLog newest = data.getStatusLogList()
        .getEntry(data.getStatusLogList().getNumberOfEntries());
    runner.check("and it is marked as a rollback", newest.isRollback());
    runner.checkEquals("recording what was undone",
        HousekeepingTask.CLEANING_IN_PROGRESS, newest.getFromStatus());
    runner.checkEquals("and what it went back to", HousekeepingTask.DIRTY,
        newest.getToStatus());

    // A rolled-back failure should stop counting against the room.
    HousekeepingTask other = data.findOpenTaskForRoom("2002");
    if (other != null) {
      service.updateTaskStatus(other.getTaskId(),
          HousekeepingTask.INSPECTED, "ST003", null);
      service.updateTaskStatus(other.getTaskId(),
          HousekeepingTask.DIRTY, "ST005", "Missed the bathroom");

      int failures = other.getInspectionFailCount();
      runner.check("the failure was counted", failures > 0);

      service.rollbackLastStatusChange("ST005");
      runner.checkEquals("rolling it back takes the failure off the count",
          failures - 1, other.getInspectionFailCount());
    }
  }

  // ==================================================================
  // LOYALTY AND BILLING
  // ==================================================================

  /** M4 reaching into M3 - a reward becomes real money off a real bill. */
  private void testRedemptionDiscountsALiveBill() {
    runner.suite("An approved reward comes off a live bill  (M4 to M3)");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    // BK0002 is checked in for member L0002.
    Booking booking = data.findBooking("BK0002");
    Member member = data.findMemberByGuest(booking.getGuestId());
    runner.check("the guest is a member", member != null);

    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    double totalBefore = invoice.getTotalAmount();
    int pointsBefore = member.getPointsBalance();

    // Request something they can afford and are senior enough for.
    ServiceResult<Redemption> requested =
        service.requestRedemption(member.getMemberId(), "RW002");
    runner.check("a redemption can be requested", requested.isSuccess());

    Redemption redemption = requested.getValue();
    runner.checkEquals("it starts PENDING", Redemption.PENDING, redemption.getStatus());
    runner.checkEquals("and no points have been taken yet", pointsBefore,
        member.getPointsBalance());

    // Work through the queue until this one is decided, since others may be
    // ahead of it. Those others may belong to this member too, so what is
    // checked afterwards is the total taken, not this request alone.
    int approvedForThisMember = 0;
    while (data.getPendingRedemptions().contains(redemption)) {
      ServiceResult<Redemption> processed = service.processNextRedemption("ST006");
      Redemption decided = processed.getValue();

      if (decided != null && Redemption.APPROVED.equals(decided.getStatus())
          && member.getMemberId().equals(decided.getMemberId())) {
        approvedForThisMember += decided.getPointsUsed();
      }
    }

    runner.checkEquals("it ends up approved", Redemption.APPROVED,
        redemption.getStatus());
    runner.checkEquals("and every approved request took its points",
        pointsBefore - approvedForThisMember, member.getPointsBalance());
    runner.check("including this one", approvedForThisMember >= redemption.getPointsUsed());

    PointTransaction spent = data.getTransactionList().search(
        txn -> PointTransaction.REDEEM.equals(txn.getTxnType())
            && txn.getDescription() != null
            && txn.getDescription().contains(redemption.getRedemptionId()));
    runner.check("a ledger row records the spend", spent != null);

    ServiceResult<Invoice> applied = service.applyRedemptionToInvoice(
        redemption.getRedemptionId(), booking.getBookingId());
    runner.check("the reward can be applied to the bill", applied.isSuccess());

    runner.check("the bill total drops", invoice.getTotalAmount() < totalBefore);
    runner.checkEquals("and the redemption records which bill",
        invoice.getInvoiceId(), redemption.getInvoiceId());

    // It must not be possible to spend the same reward twice.
    ServiceResult<Invoice> twice = service.applyRedemptionToInvoice(
        redemption.getRedemptionId(), booking.getBookingId());
    runner.check("the same reward cannot be applied twice", twice.isFailure());

    // A refusal must cost the member nothing. L0003 is Silver, and RW003
    // needs Gold.
    Member silver = data.findMember("L0003");
    int silverPoints = silver.getPointsBalance();

    ServiceResult<Redemption> tooJunior =
        service.requestRedemption("L0003", "RW003");
    runner.check("a request above the member's tier is still accepted",
        tooJunior.isSuccess());

    Redemption rejected = tooJunior.getValue();
    while (data.getPendingRedemptions().contains(rejected)) {
      service.processNextRedemption("ST006");
    }

    runner.checkEquals("but it is rejected when processed", Redemption.REJECTED,
        rejected.getStatus());
    runner.checkEquals("no points are taken for a rejection", silverPoints,
        silver.getPointsBalance());
    runner.check("and the reason is recorded", rejected.getRejectReason() != null);
  }

  /** Awarding twice for one stay would be a real defect. */
  private void testPointsAreNotAwardedTwice() {
    runner.suite("Points are awarded once per stay");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Booking booking = data.findBooking("BK0002");
    Member member = data.findMemberByGuest(booking.getGuestId());

    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    service.recordPayment(invoice.getInvoiceId(), invoice.getOutstandingBalance(),
        Payment.CASH, null, STAFF);

    ServiceResult<PointTransaction> first =
        service.awardPointsForStay(booking.getBookingId());
    runner.check("points are awarded the first time", first.isSuccess());

    int afterFirst = member.getPointsBalance();

    ServiceResult<PointTransaction> second =
        service.awardPointsForStay(booking.getBookingId());
    runner.check("a second award is refused", second.isFailure());
    runner.check("and says so plainly",
        second.getMessage().toLowerCase().contains("already"));
    runner.checkEquals("the balance is unchanged", afterFirst,
        member.getPointsBalance());

    // A stay by somebody who is not a member earns nothing.
    Booking notMember = data.getBookingList().search(
        candidate -> data.findMemberByGuest(candidate.getGuestId()) == null);
    if (notMember != null) {
      ServiceResult<PointTransaction> none =
          service.awardPointsForStay(notMember.getBookingId());
      runner.check("a non-member earns no points", none.isFailure());
    }
  }

  /** A cancelled booking must give everything back. */
  private void testCancelledBookingReleasesItsRoom() {
    runner.suite("Cancelling gives the room back");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    LocalDate from = LocalDate.now().plusDays(50);
    LocalDate to = from.plusDays(2);

    Room room = data.findRoom("1005");
    room.setOccupancyStatus(Room.VACANT);
    room.setHousekeepingStatus(Room.READY_FOR_CHECK_IN);

    Booking booking = new Booking("BKTEST2", "G0001", "RT03", from, to, 2,
        Booking.PRIORITY_NORMAL, Booking.SOURCE_ONLINE, null, 260.00,
        LocalDateTime.now(), STAFF);
    data.addBooking(booking);

    ServiceResult<Booking> assigned = service.assignRoom("BKTEST2", "1005",
        STAFF, RoomAssignment.REASON_INITIAL);
    runner.check("the room can be assigned", assigned.isSuccess());
    runner.checkEquals("the booking is confirmed", Booking.STATUS_CONFIRMED,
        booking.getBookingStatus());
    runner.checkEquals("the room is held", Room.RESERVED, room.getOccupancyStatus());

    Invoice invoice = data.findInvoiceByBooking("BKTEST2");
    runner.check("a bill is raised on assignment", invoice != null);
    runner.checkAmount("priced from the nights booked", 260.00 * 2,
        invoice.getRoomCharge());

    runner.check("the room is no longer available to anybody else",
        !service.isRoomAvailable("1005", from, to, null));

    // Cancelling by hand, the way the front desk does it.
    booking.setBookingStatus(Booking.STATUS_CANCELLED);
    room.setOccupancyStatus(Room.VACANT);

    runner.check("once cancelled the room is available again",
        service.isRoomAvailable("1005", from, to, null));
  }

  // ==================================================================
  // PERSISTENCE
  // ==================================================================

  /**
   * The data must survive a restart, and the queues must be rebuilt to the
   * same order they were in.
   */
  private void testPersistenceRoundTrip() {
    runner.suite("Everything survives a restart");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    // Make a change in each module.
    ServiceResult<WalkInRegistration> called = service.serveNextGuest(STAFF);
    String calledRegId = called.getValue().getRegId();

    Member member = data.findMember("L0001");
    member.setPointsBalance(member.getPointsBalance() + 1234);

    Room room = data.findRoom("1005");
    room.setHousekeepingStatus(Room.DIRTY);

    int guestsBefore = data.getGuestList().getNumberOfEntries();
    Guest added = service.findOrCreateGuest("991231-14-9999", "Test Guest",
        "012-0000000", "test@example.com");

    int waitingBefore = data.getWaitingList().getNumberOfEntries();
    int urgentBefore = data.getWaitingList().getUrgentCount();
    int cleaningBefore = data.getCleaningQueue().getNumberOfEntries();
    int pendingBefore = data.getPendingRedemptions().getNumberOfEntries();

    data.saveAll();

    // Reload from the files, as a restart would.
    ResortData reloaded = new ResortData();

    runner.checkEquals("the guest count survives", guestsBefore + 1,
        reloaded.getGuestList().getNumberOfEntries());
    runner.check("the new guest is found by their document",
        reloaded.findGuestByIc("991231-14-9999") != null);
    runner.checkEquals("with the right ID", added.getGuestId(),
        reloaded.findGuestByIc("991231-14-9999").getGuestId());

    runner.checkEquals("the member's points survive", member.getPointsBalance(),
        reloaded.findMember("L0001").getPointsBalance());
    runner.checkEquals("the room's housekeeping status survives", Room.DIRTY,
        reloaded.findRoom("1005").getHousekeepingStatus());
    runner.checkEquals("the called guest is still IN_SERVICE",
        WalkInRegistration.STATUS_IN_SERVICE,
        reloaded.findRegistration(calledRegId).getStatus());

    // The queues are not saved - they are rebuilt from the records, and must
    // come back the same.
    runner.checkEquals("the waiting queue is rebuilt to the same size",
        waitingBefore, reloaded.getWaitingList().getNumberOfEntries());
    runner.checkEquals("with the same urgent count", urgentBefore,
        reloaded.getWaitingList().getUrgentCount());
    runner.checkEquals("the cleaning queue is rebuilt", cleaningBefore,
        reloaded.getCleaningQueue().getNumberOfEntries());
    runner.checkEquals("and the pending redemptions", pendingBefore,
        reloaded.getPendingRedemptions().getNumberOfEntries());

    // A guest who has been called must not reappear in the queue.
    runner.check("a called guest does not come back into the queue",
        !reloaded.getWaitingList().toServiceOrder()
            .contains(reloaded.findRegistration(calledRegId)));

    // The lookups must be rebuilt too, not just the lists.
    runner.check("the booking tree is rebuilt", reloaded.findBooking("BK0001") != null);
    runner.check("the room map is rebuilt", reloaded.findRoom("1001") != null);
    runner.check("the member tree is rebuilt", reloaded.findMember("L0002") != null);
    runner.checkEquals("bookings still come back in sorted order",
        reloaded.getBookingList().getNumberOfEntries(),
        reloaded.getBookingsSorted().getNumberOfEntries());

    // A new ID must not collide with one already issued.
    String nextGuest = reloaded.nextGuestId();
    runner.check("the next guest ID is unused",
        reloaded.findGuest(nextGuest) == null);
  }
}
