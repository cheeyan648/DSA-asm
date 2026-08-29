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
import entity.Reward;
import entity.Room;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The whole guest journey, in the order it actually happens.
 *
 * Where the other suites check one rule at a time, this one walks a guest from
 * the door to the cleaned room and asserts the handover at every boundary: the
 * walk-in queue orders by urgency, the front desk books only who was served,
 * the loyalty module decides the rewards the desk requested, and housekeeping
 * receives the room the desk released. Each stage prints what was done and what
 * came back, so the run doubles as a record of how the system behaves.
 *
 * @author Tan Chee Yan
 */
public class WorkflowTest {

  private final TestRunner runner = new TestRunner();

  private static final String FRONT_DESK = "ST001";
  private static final String LOYALTY = "ST006";

  public static void main(String[] args) {
    WorkflowTest tests = new WorkflowTest();
    System.exit(tests.runAll() ? 0 : 1);
  }

  public boolean runAll() {
    System.out.println();
    System.out.println("=".repeat(76));
    System.out.println("  WORKFLOW TEST - one guest, door to cleaned room");
    System.out.println("=".repeat(76));

    stage1RegistrationOrdersByUrgency();
    stage2OnlyServedGuestsCanBeBooked();
    stage3MembershipOfferedOnceOnly();
    stage4PaymentAndConfirmationNumber();
    stage5RewardRequestedByConfirmation();
    stage6LoyaltyDecidesTheRequest();
    stage7CheckOutAwardsPoints();
    stage8CheckOutFeedsHousekeeping();
    stage9LedgerRecordsBothDirections();
    stage10SeedDataIsConsistent();

    return runner.report("WORKFLOW TEST SUMMARY");
  }

  private ResortData freshData() {
    ResortData data = new ResortData();
    data.deleteAllFiles();
    return new ResortData();
  }

  /** Puts a room of the given type beyond doubt: vacant, clean, in service. */
  private Room readyRoomOfType(ResortData data, String typeId) {
    ListInterface<Room> rooms = data.getRoomList();
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (typeId.equals(room.getTypeId())) {
        room.setOccupancyStatus(Room.VACANT);
        room.setHousekeepingStatus(Room.READY_FOR_CHECK_IN);
        room.setOutOfService(false);
        return room;
      }
    }
    return null;
  }

  /** Joins the walk-in queue, in the lane the priority names. */
  private WalkInRegistration join(ResortData data, String guestId, String priority,
      String typeId, int nights) {
    WalkInRegistration reg = new WalkInRegistration(data.nextRegistrationId(),
        guestId, LocalDateTime.now(), priority,
        WalkInRegistration.PRIORITY_URGENT.equals(priority) ? "Medical need" : null,
        typeId, nights, LocalDate.now());
    data.getRegistrationList().add(reg);
    data.getWaitingList().enqueue(reg, reg.getPriority());
    return reg;
  }

  private void say(String line) {
    System.out.println("      " + line);
  }

  // ==================================================================
  // STAGE 1 - WALK-IN REGISTRATION
  // ==================================================================

  /** An urgent guest is called before normals, however late they arrived. */
  private void stage1RegistrationOrdersByUrgency() {
    runner.suite("Stage 1  Registration queues by priority, not arrival");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    // Drain whatever the seed left waiting, so the order under test is ours.
    while (service.serveNextGuest(FRONT_DESK).isSuccess()) {
      continue;
    }

    WalkInRegistration first = join(data, "G0001", WalkInRegistration.PRIORITY_NORMAL,
        "RT01", 2);
    WalkInRegistration second = join(data, "G0002", WalkInRegistration.PRIORITY_NORMAL,
        "RT01", 2);
    WalkInRegistration late = join(data, "G0003", WalkInRegistration.PRIORITY_URGENT,
        "RT01", 2);

    say("IN : " + first.getRegId() + " NORMAL, " + second.getRegId()
        + " NORMAL, then " + late.getRegId() + " URGENT joins last");

    WalkInRegistration next = data.getWaitingList().peekNext();
    say("OUT: next to be called is " + next.getRegId() + " ("
        + next.getPriority() + ")");

    runner.checkEquals("the urgent guest is called first despite joining last",
        late.getRegId(), next.getRegId());

    ServiceResult<WalkInRegistration> called = service.serveNextGuest(FRONT_DESK);
    runner.check("calling them succeeds", called.isSuccess());
    runner.checkEquals("and they leave the queue as IN_SERVICE",
        WalkInRegistration.STATUS_IN_SERVICE, late.getStatus());
    runner.check("the queue no longer holds them",
        !data.getWaitingList().toServiceOrder().contains(late));

    // Registration keeps calling people through without waiting on bookings.
    runner.check("a second guest can be called straight away",
        service.serveNextGuest(FRONT_DESK).isSuccess());
    say("OUT: two guests now IN_SERVICE, both waiting at the front desk");
  }

  // ==================================================================
  // STAGE 2 - THE HANDOVER TO THE FRONT DESK
  // ==================================================================

  /** Only a guest who reached the counter can become a booking. */
  private void stage2OnlyServedGuestsCanBeBooked() {
    runner.suite("Stage 2  Front desk books only who registration served");

    ResortData data = freshData();
    ResortService service = new ResortService(data);
    while (service.serveNextGuest(FRONT_DESK).isSuccess()) {
      continue;
    }

    WalkInRegistration waiting = join(data, "G0001",
        WalkInRegistration.PRIORITY_NORMAL, "RT03", 2);
    LocalDate from = LocalDate.now();

    say("IN : try to book " + waiting.getRegId() + " while still WAITING");
    ServiceResult<Booking> tooEarly = service.convertRegistrationToBooking(
        waiting.getRegId(), from, from.plusDays(2), 2, FRONT_DESK);
    say("OUT: " + tooEarly.getMessage());

    runner.check("a guest still in the queue cannot be booked", tooEarly.isFailure());

    service.serveNextGuest(FRONT_DESK);
    say("IN : call them, then book " + waiting.getRegId());

    ServiceResult<Booking> booked = service.convertRegistrationToBooking(
        waiting.getRegId(), from, from.plusDays(2), 2, FRONT_DESK);
    say("OUT: " + booked.getMessage());

    runner.check("once served, the booking is created", booked.isSuccess());
    runner.checkEquals("it starts PENDING with no room",
        Booking.STATUS_PENDING, booked.getValue().getBookingStatus());
    runner.checkEquals("and the registration points at the booking",
        booked.getValue().getBookingId(), waiting.getBookingId());
  }

  // ==================================================================
  // STAGE 3 - MEMBERSHIP
  // ==================================================================

  /** The offer is for people who have no membership - nobody is asked twice. */
  private void stage3MembershipOfferedOnceOnly() {
    runner.suite("Stage 3  Membership offered only to a non-member");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Guest member = null;
    Guest plain = null;
    ListInterface<Guest> guests = data.getGuestList();
    for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
      Guest guest = guests.getEntry(i);
      boolean enrolled = data.findMemberByGuest(guest.getGuestId()) != null;
      if (enrolled && member == null) {
        member = guest;
      }
      if (!enrolled && plain == null) {
        plain = guest;
      }
    }

    runner.check("the sample has both a member and a non-member",
        member != null && plain != null);

    Member existing = data.findMemberByGuest(member.getGuestId());
    say("IN : " + member.getFullName() + " already holds " + existing.getMemberId());
    say("OUT: findMemberByGuest returns a membership, so the offer is skipped");
    runner.check("an existing member is not offered membership again",
        data.findMemberByGuest(member.getGuestId()) != null);

    ServiceResult<Member> twice = service.enrolMember(member.getGuestId());
    say("OUT: enrolling them again -> " + twice.getMessage());
    runner.check("and enrolling a second time is refused", twice.isFailure());

    say("IN : " + plain.getFullName() + " holds no membership");
    ServiceResult<Member> joined = service.enrolMember(plain.getGuestId());
    say("OUT: " + joined.getMessage());

    runner.check("a non-member can join", joined.isSuccess());
    runner.checkEquals("they start at SILVER",
        Member.SILVER, joined.getValue().getTier());
    runner.checkEquals("with nothing earned yet",
        0, joined.getValue().getPointsBalance());
  }

  // ==================================================================
  // STAGE 4 - ROOM, PAYMENT, CONFIRMATION NUMBER
  // ==================================================================

  /** A room is only given when clean, and the bill is settled in full. */
  private void stage4PaymentAndConfirmationNumber() {
    runner.suite("Stage 4  Room, payment in full, and the confirmation code");

    ResortData data = freshData();
    ResortService service = new ResortService(data);
    while (service.serveNextGuest(FRONT_DESK).isSuccess()) {
      continue;
    }

    Room room = readyRoomOfType(data, "RT03");
    WalkInRegistration reg = join(data, "G0001",
        WalkInRegistration.PRIORITY_NORMAL, "RT03", 2);
    service.serveNextGuest(FRONT_DESK);

    LocalDate from = LocalDate.now();
    Booking booking = service.convertRegistrationToBooking(
        reg.getRegId(), from, from.plusDays(2), 2, FRONT_DESK).getValue();

    say("IN : assign room " + room.getRoomNo() + " to " + booking.getBookingId());
    ServiceResult<Booking> assigned = service.assignRoom(booking.getBookingId(),
        room.getRoomNo(), FRONT_DESK, entity.RoomAssignment.REASON_INITIAL);
    say("OUT: " + assigned.getMessage());

    runner.check("the room can be assigned", assigned.isSuccess());
    runner.checkEquals("the booking becomes CONFIRMED",
        Booking.STATUS_CONFIRMED, booking.getBookingStatus());

    Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
    runner.check("a bill is raised with the room", invoice != null);
    runner.checkEquals("and it starts unpaid",
        Invoice.UNPAID, invoice.getPaymentStatus());

    // Half the bill is not a payment.
    double half = invoice.getOutstandingBalance() / 2;
    say("IN : offer RM" + String.format("%.2f", half) + " of RM"
        + String.format("%.2f", invoice.getTotalAmount()));
    ServiceResult<Payment> part = service.recordPayment(invoice.getInvoiceId(),
        half, Payment.CASH, null, FRONT_DESK);
    say("OUT: " + part.getMessage());
    runner.check("a part payment is refused", part.isFailure());

    say("IN : pay the full RM" + String.format("%.2f", invoice.getTotalAmount()));
    ServiceResult<Payment> full = service.recordPayment(invoice.getInvoiceId(),
        invoice.getOutstandingBalance(), Payment.CASH, null, FRONT_DESK);
    say("OUT: " + full.getMessage());

    runner.check("paying in full succeeds", full.isSuccess());
    runner.checkEquals("the bill is settled", Invoice.PAID, invoice.getPaymentStatus());

    String code = booking.getConfirmationNumber();
    say("OUT: confirmation number is " + code);
    runner.check("the confirmation code is eight characters",
        code != null && code.length() == 8);
    runner.check("letters and digits only",
        code.matches("[0-9A-Z]{8}"));
    runner.check("and it finds the booking again",
        data.findBookingByConfirmation(code) == booking);
  }

  // ==================================================================
  // STAGE 5 - REQUESTING A REWARD
  // ==================================================================

  /** The desk requests; nothing is granted and no points move yet. */
  private void stage5RewardRequestedByConfirmation() {
    runner.suite("Stage 5  Reward requested against a live booking");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Booking booking = data.getBookingList().search(candidate ->
        Booking.STATUS_CHECKED_IN.equals(candidate.getBookingStatus())
            && data.findMemberByGuest(candidate.getGuestId()) != null);
    runner.check("the sample has a checked-in member", booking != null);

    Member member = data.findMemberByGuest(booking.getGuestId());
    int before = member.getPointsBalance();

    say("IN : confirmation " + booking.getConfirmationNumber()
        + " -> " + data.findBookingByConfirmation(
            booking.getConfirmationNumber()).getBookingId());
    runner.check("the confirmation code reaches the booking",
        data.findBookingByConfirmation(booking.getConfirmationNumber()) == booking);

    // Something they cannot afford must be refused.
    Reward dear = new Reward("RW099", "Private Yacht Day", Reward.CAT_ACTIVITY,
        before + 5000, Member.SILVER, 5, true, 9999.00);
    data.getRewardList().add(dear);

    say("IN : request " + dear.getRewardName() + " (" + dear.getPointsRequired()
        + " pts) with a balance of " + before);
    ServiceResult<Redemption> tooDear =
        service.requestRedemption(member.getMemberId(), dear.getRewardId());
    say("OUT: " + tooDear.getMessage());
    runner.check("a reward beyond the balance is refused", tooDear.isFailure());

    Reward spa = data.findReward("RW001");
    say("IN : request " + spa.getRewardName() + " (" + spa.getPointsRequired()
        + " pts)");
    ServiceResult<Redemption> asked =
        service.requestRedemption(member.getMemberId(), spa.getRewardId());
    say("OUT: " + asked.getMessage());

    runner.check("an affordable reward can be requested", asked.isSuccess());
    runner.checkEquals("it waits as PENDING",
        Redemption.PENDING, asked.getValue().getStatus());
    runner.checkEquals("and no points have moved yet",
        before, member.getPointsBalance());

    runner.check("it is in the queue for loyalty to decide",
        data.getPendingRedemptions().contains(asked.getValue()));
  }

  // ==================================================================
  // STAGE 6 - LOYALTY DECIDES
  // ==================================================================

  /** Approval takes the points; a refusal costs the member nothing. */
  private void stage6LoyaltyDecidesTheRequest() {
    runner.suite("Stage 6  Loyalty approves or rejects, both go on record");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Booking booking = data.getBookingList().search(candidate ->
        Booking.STATUS_CHECKED_IN.equals(candidate.getBookingStatus())
            && data.findMemberByGuest(candidate.getGuestId()) != null);
    Member member = data.findMemberByGuest(booking.getGuestId());

    Reward spa = data.findReward("RW001");
    Redemption request = service.requestRedemption(
        member.getMemberId(), spa.getRewardId()).getValue();
    request.setBookingId(booking.getBookingId());

    int before = member.getPointsBalance();
    say("IN : approve " + request.getRedemptionId() + " for "
        + spa.getRewardName() + " (" + spa.getPointsRequired() + " pts)");
    say("     member balance before = " + before);

    ServiceResult<Redemption> decided =
        service.processRedemption(request.getRedemptionId(), LOYALTY);
    say("OUT: " + decided.getMessage());
    say("     member balance after  = " + member.getPointsBalance());

    runner.check("the request can be decided", decided.isSuccess());
    runner.checkEquals("it becomes APPROVED",
        Redemption.APPROVED, request.getStatus());
    runner.checkEquals("the points come off the balance",
        before - spa.getPointsRequired(), member.getPointsBalance());
    runner.checkEquals("and it records who decided it",
        LOYALTY, request.getProcessedBy());

    runner.check("it has left the pending queue",
        !data.getPendingRedemptions().contains(request));
    runner.check("but stays in the redemption history",
        data.getRedemptionList().contains(request));

    // A request that could never be granted is turned away at the counter,
    // so it never reaches the loyalty officer's queue at all.
    Member silver = data.findMember("L0003");
    int silverBefore = silver.getPointsBalance();
    int queueBefore = data.getPendingRedemptions().getNumberOfEntries();

    say("IN : SILVER member asks for a GOLD-only reward");
    ServiceResult<Redemption> blocked = service.requestRedemption("L0003", "RW003");
    say("OUT: " + blocked.getMessage());

    runner.check("the request is refused at the desk", blocked.isFailure());
    runner.check("the reason names the tier",
        blocked.getMessage().contains("tier"));
    runner.checkEquals("it costs the member no points",
        silverBefore, silver.getPointsBalance());
    runner.checkEquals("and nothing was added to the queue",
        queueBefore, data.getPendingRedemptions().getNumberOfEntries());

    // A refusal at approval time is still possible - stock can run out while a
    // request waits - and that refusal must cost nothing either.
    Reward last = data.findReward("RW004");
    Redemption waiting = service.requestRedemption("L0003",
        last.getRewardId()).getValue();
    last.setStockQuantity(0);

    say("IN : the reward sells out while the request is waiting");
    ServiceResult<Redemption> rejection =
        service.processRedemption(waiting.getRedemptionId(), LOYALTY);
    say("OUT: " + rejection.getMessage());

    runner.checkEquals("the request is REJECTED",
        Redemption.REJECTED, waiting.getStatus());
    runner.checkEquals("the refusal costs no points",
        silverBefore, silver.getPointsBalance());
    runner.check("a reason is recorded", waiting.getRejectReason() != null);
    runner.check("and the refusal is in the history too",
        data.getRedemptionList().contains(waiting));
  }

  // ==================================================================
  // STAGE 7 - CHECK-OUT AWARDS POINTS
  // ==================================================================

  /** Points are earned from the settled bill, and enrol a guest who has none. */
  private void stage7CheckOutAwardsPoints() {
    runner.suite("Stage 7  Check-out awards points, enrolling if needed");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Booking memberStay = data.getBookingList().search(candidate ->
        Booking.STATUS_CHECKED_IN.equals(candidate.getBookingStatus())
            && data.findMemberByGuest(candidate.getGuestId()) != null);
    Member member = data.findMemberByGuest(memberStay.getGuestId());
    Invoice bill = data.findInvoiceByBooking(memberStay.getBookingId());

    int before = member.getPointsBalance();
    int expected = (int) Math.round(bill.getTotalAmount()
        * ResortService.POINTS_PER_RINGGIT * member.getMultiplier());

    say("IN : check out " + memberStay.getBookingId() + ", bill RM"
        + String.format("%.2f", bill.getTotalAmount()));
    say("     " + member.getTier() + " multiplier x" + member.getMultiplier());

    ServiceResult<Booking> out = service.checkOut(memberStay.getBookingId(),
        FRONT_DESK, false);
    say("OUT: " + out.getMessage());

    runner.check("the stay closes", out.isSuccess());
    runner.checkEquals("the booking is CHECKED_OUT",
        Booking.STATUS_CHECKED_OUT, memberStay.getBookingStatus());
    runner.checkEquals("points are the bill times the tier multiplier",
        before + expected, member.getPointsBalance());

    // A guest with no membership is enrolled by the stay rather than losing it.
    Booking plainStay = data.getBookingList().search(candidate ->
        Booking.STATUS_CHECKED_IN.equals(candidate.getBookingStatus())
            && data.findMemberByGuest(candidate.getGuestId()) == null);

    if (plainStay != null) {
      String guestId = plainStay.getGuestId();
      say("IN : check out " + plainStay.getBookingId() + " for a non-member");

      ServiceResult<Booking> second = service.checkOut(plainStay.getBookingId(),
          FRONT_DESK, false);
      say("OUT: " + second.getMessage());

      Member enrolled = data.findMemberByGuest(guestId);
      runner.check("the guest is enrolled by their stay", enrolled != null);
      runner.check("and the points are on the new membership",
          enrolled != null && enrolled.getPointsBalance() > 0);
    }
  }

  // ==================================================================
  // STAGE 8 - THE ROOM GOES TO HOUSEKEEPING
  // ==================================================================

  /** Check-out dirties the room and queues it in the lane the desk chose. */
  private void stage8CheckOutFeedsHousekeeping() {
    runner.suite("Stage 8  Check-out hands the room to housekeeping");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Booking stay = data.getBookingList().search(candidate ->
        Booking.STATUS_CHECKED_IN.equals(candidate.getBookingStatus()));
    String roomNo = stay.getRoomNo();
    Room room = data.findRoom(roomNo);

    int queueBefore = data.getCleaningQueue().getNumberOfEntries();
    int urgentBefore = data.getCleaningQueue().getUrgentCount();

    say("IN : check out " + stay.getBookingId() + " from room " + roomNo
        + ", marked URGENT for cleaning");
    say("     cleaning queue before = " + queueBefore + " ("
        + urgentBefore + " urgent)");

    ServiceResult<Booking> out = service.checkOut(stay.getBookingId(),
        FRONT_DESK, true);
    say("OUT: " + out.getMessage());

    runner.check("the stay closes", out.isSuccess());
    runner.checkEquals("the room is vacant again", Room.VACANT,
        room.getOccupancyStatus());
    runner.checkEquals("but dirty", Room.DIRTY, room.getHousekeepingStatus());
    runner.check("so it cannot be sold in the meantime",
        !service.isRoomAvailable(roomNo, LocalDate.now(),
            LocalDate.now().plusDays(1), null));

    HousekeepingTask raised = data.findOpenTaskForRoom(roomNo);
    runner.check("a cleaning task was raised without being asked for",
        raised != null);
    runner.checkEquals("in the lane the front desk chose",
        HousekeepingTask.PRIORITY_URGENT, raised.getPriority());
    runner.checkEquals("the cleaning queue grew by one",
        queueBefore + 1, data.getCleaningQueue().getNumberOfEntries());
    runner.checkEquals("and the urgent lane is one longer",
        urgentBefore + 1, data.getCleaningQueue().getUrgentCount());

    say("     cleaning queue after  = "
        + data.getCleaningQueue().getNumberOfEntries() + " ("
        + data.getCleaningQueue().getUrgentCount() + " urgent)");

    // Housekeeping takes it, and the room comes back sellable.
    say("IN : housekeeping cleans " + roomNo + " through to ready");
    service.updateTaskStatus(raised.getTaskId(),
        HousekeepingTask.CLEANING_IN_PROGRESS, "ST003", null);
    service.updateTaskStatus(raised.getTaskId(),
        HousekeepingTask.INSPECTED, "ST003", null);
    ServiceResult<HousekeepingTask> done = service.updateTaskStatus(
        raised.getTaskId(), HousekeepingTask.READY_FOR_CHECK_IN, "ST005", null);
    say("OUT: " + done.getMessage());

    runner.check("the room can be cleaned through to ready", done.isSuccess());
    runner.checkEquals("and it is ready again", Room.READY_FOR_CHECK_IN,
        room.getHousekeepingStatus());
    runner.check("so the front desk can sell it to the next guest",
        service.isRoomAvailable(roomNo, LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(32), null));
  }

  // ==================================================================
  // STAGE 9 - THE LEDGER
  // ==================================================================

  /** Every movement of points is written down, both ways. */
  private void stage9LedgerRecordsBothDirections() {
    runner.suite("Stage 9  The ledger records earning and spending");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    Booking stay = data.getBookingList().search(candidate ->
        Booking.STATUS_CHECKED_IN.equals(candidate.getBookingStatus())
            && data.findMemberByGuest(candidate.getGuestId()) != null);
    Member member = data.findMemberByGuest(stay.getGuestId());

    int rowsBefore = countLedgerRows(data, member.getMemberId());

    // Spend, then earn.
    Reward spa = data.findReward("RW001");
    Redemption request = service.requestRedemption(
        member.getMemberId(), spa.getRewardId()).getValue();
    request.setBookingId(stay.getBookingId());
    service.processRedemption(request.getRedemptionId(), LOYALTY);
    service.checkOut(stay.getBookingId(), FRONT_DESK, false);

    int rowsAfter = countLedgerRows(data, member.getMemberId());
    say("IN : one redemption approved, then one check-out");
    say("OUT: ledger rows " + rowsBefore + " -> " + rowsAfter);

    runner.checkEquals("two rows were written", rowsBefore + 2, rowsAfter);

    PointTransaction spent = findRow(data, member.getMemberId(),
        PointTransaction.REDEEM, request.getRedemptionId());
    runner.check("the spend is on record", spent != null);
    runner.check("recorded as a negative amount",
        spent != null && spent.getPoints() < 0);
    runner.checkEquals("naming the stay it was spent on",
        stay.getBookingId(), spent == null ? null : spent.getBookingId());

    PointTransaction earned = findRow(data, member.getMemberId(),
        PointTransaction.EARN, stay.getBookingId());
    runner.check("the earning is on record", earned != null);
    runner.check("recorded as a positive amount",
        earned != null && earned.getPoints() > 0);

    // The running balance on the newest row must match the member.
    runner.checkEquals("and the ledger agrees with the balance",
        member.getPointsBalance(),
        earned == null ? -1 : earned.getBalanceAfter());

    say("     spend " + (spent == null ? "?" : spent.getPoints())
        + ", earn +" + (earned == null ? "?" : earned.getPoints())
        + ", balance now " + member.getPointsBalance());
  }

  // ==================================================================
  // STAGE 10 - THE SAMPLE DATA ITSELF
  // ==================================================================

  /**
   * The seed must describe a state the system would actually allow.
   *
   * A sample that contradicts the rules teaches the wrong thing and hides
   * real defects: a room shown as both sold and waiting to be cleaned would
   * let a guest be refused check-in for a room that is theirs.
   */
  private void stage10SeedDataIsConsistent() {
    runner.suite("Stage 10  The sample data obeys its own rules");

    ResortData data = freshData();
    ResortService service = new ResortService(data);

    ListInterface<Booking> live = data.getBookingList().filter(booking ->
        booking.getRoomNo() != null
            && (Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus())
                || Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())));

    say("IN : " + live.getNumberOfEntries() + " booking(s) hold a room");

    int notReady = 0;
    int stillQueued = 0;
    for (int i = 1; i <= live.getNumberOfEntries(); i++) {
      Booking booking = live.getEntry(i);
      Room room = data.findRoom(booking.getRoomNo());

      if (room == null || !Room.READY_FOR_CHECK_IN.equals(room.getHousekeepingStatus())) {
        notReady++;
        say("     " + booking.getBookingId() + " holds " + booking.getRoomNo()
            + " which is " + (room == null ? "missing" : room.getHousekeepingStatus()));
      }
      if (data.findOpenTaskForRoom(booking.getRoomNo()) != null) {
        stillQueued++;
        say("     " + booking.getBookingId() + " holds " + booking.getRoomNo()
            + " which still has an open cleaning task");
      }
    }

    runner.checkEquals("every sold room is cleaned and ready", 0, notReady);
    runner.checkEquals("and none is still waiting to be cleaned", 0, stillQueued);

    // A room the records call mid-clean must have the task that says so.
    ListInterface<Room> rooms = data.getRoomList();
    int orphaned = 0;
    int reservedForNobody = 0;

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);

      if (Room.CLEANING_IN_PROGRESS.equals(room.getHousekeepingStatus())
          && data.findOpenTaskForRoom(room.getRoomNo()) == null) {
        orphaned++;
        say("     room " + room.getRoomNo() + " is mid-clean with no task");
      }

      if (Room.RESERVED.equals(room.getOccupancyStatus())) {
        Booking holder = data.getBookingList().search(booking ->
            room.getRoomNo().equals(booking.getRoomNo())
                && (Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus())
                    || Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())));
        if (holder == null) {
          reservedForNobody++;
          say("     room " + room.getRoomNo() + " is RESERVED for nobody");
        }
      }
    }

    runner.checkEquals("a room being cleaned has the task that says so",
        0, orphaned);
    runner.checkEquals("and no room is held for a booking that does not exist",
        0, reservedForNobody);

    // A sold room must be safe from being withdrawn under the guest.
    Booking held = live.isEmpty() ? null : live.getEntry(1);
    if (held != null) {
      say("IN : try to withdraw " + held.getRoomNo() + ", held by "
          + held.getBookingId());
      ServiceResult<Room> blocked =
          service.setRoomOutOfService(held.getRoomNo(), true, FRONT_DESK);
      say("OUT: " + blocked.getMessage());

      runner.check("a sold room cannot be taken out of service",
          blocked.isFailure());
      runner.check("nor removed altogether",
          service.removeRoom(held.getRoomNo()).isFailure());
    }
  }

  private int countLedgerRows(ResortData data, String memberId) {
    return data.getTransactionList().countIf(
        txn -> memberId.equals(txn.getMemberId()));
  }

  private PointTransaction findRow(ResortData data, String memberId, String type,
      String mentions) {
    return data.getTransactionList().search(txn ->
        memberId.equals(txn.getMemberId())
            && type.equals(txn.getTxnType())
            && ((txn.getDescription() != null
                    && txn.getDescription().contains(mentions))
                || mentions.equals(txn.getBookingId())));
  }
}
