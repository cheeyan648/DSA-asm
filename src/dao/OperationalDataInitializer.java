package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.Booking;
import entity.HousekeepingTask;
import entity.Invoice;
import entity.Payment;
import entity.RoomAssignment;
import entity.RoomStatusLog;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds the working records on the first run - walk-ins, bookings, bills and
 * cleaning tasks.
 *
 * The sample is built as one consistent day rather than as unrelated rows, so
 * the links between the modules can actually be followed: an urgent walk-in
 * that became an urgent booking that made housekeeping clean a room out of
 * turn, and a completed stay whose check-out both dirtied a room and earned
 * loyalty points.
 *
 * Times are anchored to the moment the program is first run rather than to
 * fixed clock times, so waiting times and "today" filters are realistic
 * whenever the system is demonstrated.
 *
 * @author Tan Chee Yan
 */
public class OperationalDataInitializer {

  /**
   * Walk-in registrations covering every status.
   *
   * Two are still waiting so the queue display and its paging have something
   * to show, and the urgent ones arrived after normal guests who are still
   * waiting - which is what makes the two lanes visible at a glance.
   */
  public ListInterface<WalkInRegistration> initializeRegistrations() {
    ListInterface<WalkInRegistration> registrations = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    WalkInRegistration wr1 = new WalkInRegistration("WR0001", "G0004",
        now.minusMinutes(95), WalkInRegistration.PRIORITY_NORMAL, null, "RT02", 3);
    wr1.setStatus(WalkInRegistration.STATUS_BOOKED);
    wr1.setCalledAt(now.minusMinutes(85));
    wr1.setBookedAt(now.minusMinutes(82));
    wr1.setServedBy("ST001");
    wr1.setBookingId("BK0004");
    registrations.add(wr1);

    WalkInRegistration wr2 = new WalkInRegistration("WR0002", "G0006",
        now.minusMinutes(66), WalkInRegistration.PRIORITY_URGENT,
        "Elderly guest, wheelchair user", "RT01", 2);
    wr2.setStatus(WalkInRegistration.STATUS_BOOKED);
    wr2.setCalledAt(now.minusMinutes(65));
    wr2.setBookedAt(now.minusMinutes(63));
    wr2.setServedBy("ST001");
    wr2.setBookingId("BK0005");
    registrations.add(wr2);

    // Still waiting - the normal lane.
    registrations.add(new WalkInRegistration("WR0003", "G0007",
        now.minusMinutes(44), WalkInRegistration.PRIORITY_NORMAL, null, "RT03", 1));
    registrations.add(new WalkInRegistration("WR0004", "G0005",
        now.minusMinutes(29), WalkInRegistration.PRIORITY_NORMAL, null, "RT04", 4));

    WalkInRegistration wr5 = new WalkInRegistration("WR0005", "G0003",
        now.minusMinutes(21), WalkInRegistration.PRIORITY_URGENT,
        "Travelling with infant", "RT02", 2);
    wr5.setStatus(WalkInRegistration.STATUS_BOOKED);
    wr5.setCalledAt(now.minusMinutes(20));
    wr5.setBookedAt(now.minusMinutes(18));
    wr5.setServedBy("ST001");
    wr5.setBookingId("BK0007");
    registrations.add(wr5);

    WalkInRegistration wr6 = new WalkInRegistration("WR0006", "G0002",
        now.minusMinutes(140), WalkInRegistration.PRIORITY_NORMAL, null, "RT05", 2);
    wr6.setStatus(WalkInRegistration.STATUS_CANCELLED);
    wr6.setServedBy("ST002");
    registrations.add(wr6);

    // Extra sample records so a listing runs past one page (PAGE_SIZE = 15).
    WalkInRegistration wr7 = new WalkInRegistration("WR0007", "G0001",
        now.minusMinutes(300), WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 2);
    wr7.setStatus(WalkInRegistration.STATUS_BOOKED);
    wr7.setCalledAt(now.minusMinutes(292));
    wr7.setBookedAt(now.minusMinutes(289));
    wr7.setServedBy("ST001");
    wr7.setBookingId("BK0010");
    registrations.add(wr7);

    WalkInRegistration wr8 = new WalkInRegistration("WR0008", "G0003",
        now.minusMinutes(280), WalkInRegistration.PRIORITY_URGENT,
        "Medical or emergency situation", "RT02", 1);
    wr8.setStatus(WalkInRegistration.STATUS_BOOKED);
    wr8.setCalledAt(now.minusMinutes(279));
    wr8.setBookedAt(now.minusMinutes(276));
    wr8.setServedBy("ST002");
    wr8.setBookingId("BK0011");
    registrations.add(wr8);

    registrations.add(new WalkInRegistration("WR0009", "G0005",
        now.minusMinutes(258), WalkInRegistration.PRIORITY_NORMAL, null, "RT03", 3));

    WalkInRegistration wr10 = new WalkInRegistration("WR0010", "G0007",
        now.minusMinutes(240), WalkInRegistration.PRIORITY_NORMAL, null, "RT04", 2);
    wr10.setStatus(WalkInRegistration.STATUS_NO_SHOW);
    wr10.setCalledAt(now.minusMinutes(210));
    wr10.setServedBy("ST001");
    registrations.add(wr10);

    WalkInRegistration wr11 = new WalkInRegistration("WR0011", "G0004",
        now.minusMinutes(225), WalkInRegistration.PRIORITY_URGENT,
        "Complaint escalation", "RT01", 1);
    wr11.setStatus(WalkInRegistration.STATUS_BOOKED);
    wr11.setCalledAt(now.minusMinutes(224));
    wr11.setBookedAt(now.minusMinutes(221));
    wr11.setServedBy("ST002");
    wr11.setBookingId("BK0012");
    registrations.add(wr11);

    registrations.add(new WalkInRegistration("WR0012", "G0006",
        now.minusMinutes(198), WalkInRegistration.PRIORITY_NORMAL, null, "RT05", 4));

    WalkInRegistration wr13 = new WalkInRegistration("WR0013", "G0002",
        now.minusMinutes(180), WalkInRegistration.PRIORITY_NORMAL, null, "RT02", 2);
    wr13.setStatus(WalkInRegistration.STATUS_CANCELLED);
    wr13.setServedBy("ST001");
    registrations.add(wr13);

    WalkInRegistration wr14 = new WalkInRegistration("WR0014", "G0001",
        now.minusMinutes(165), WalkInRegistration.PRIORITY_URGENT,
        "Travelling with infant or young children", "RT03", 2);
    wr14.setStatus(WalkInRegistration.STATUS_BOOKED);
    wr14.setCalledAt(now.minusMinutes(164));
    wr14.setBookedAt(now.minusMinutes(161));
    wr14.setServedBy("ST002");
    wr14.setBookingId("BK0013");
    registrations.add(wr14);

    registrations.add(new WalkInRegistration("WR0015", "G0003",
        now.minusMinutes(150), WalkInRegistration.PRIORITY_NORMAL, null, "RT04", 1));

    WalkInRegistration wr16 = new WalkInRegistration("WR0016", "G0005",
        now.minusMinutes(120), WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 3);
    wr16.setStatus(WalkInRegistration.STATUS_BOOKED);
    wr16.setCalledAt(now.minusMinutes(112));
    wr16.setBookedAt(now.minusMinutes(109));
    wr16.setServedBy("ST001");
    wr16.setBookingId("BK0014");
    registrations.add(wr16);

    WalkInRegistration wr17 = new WalkInRegistration("WR0017", "G0007",
        now.minusMinutes(105), WalkInRegistration.PRIORITY_URGENT,
        "Elderly or disabled guest", "RT02", 2);
    wr17.setStatus(WalkInRegistration.STATUS_BOOKED);
    wr17.setCalledAt(now.minusMinutes(104));
    wr17.setBookedAt(now.minusMinutes(101));
    wr17.setServedBy("ST002");
    wr17.setBookingId("BK0015");
    registrations.add(wr17);

    registrations.add(new WalkInRegistration("WR0018", "G0004",
        now.minusMinutes(80), WalkInRegistration.PRIORITY_NORMAL, null, "RT05", 2));

    // The day's earlier arrivals, all finished with. They take the listing
    // past a single page, so the paging controls have something to work on
    // from the first run rather than only once the queue has been used.
    addFinished(registrations, "WR0019", "G0001", now.minusMinutes(300),
        WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 2, "ST001", "BK0016");
    addFinished(registrations, "WR0020", "G0002", now.minusMinutes(292),
        WalkInRegistration.PRIORITY_NORMAL, null, "RT03", 1, "ST001", "BK0017");
    addFinished(registrations, "WR0021", "G0003", now.minusMinutes(285),
        WalkInRegistration.PRIORITY_URGENT, "Medical or emergency situation",
        "RT02", 3, "ST002", "BK0018");
    addFinished(registrations, "WR0022", "G0005", now.minusMinutes(277),
        WalkInRegistration.PRIORITY_NORMAL, null, "RT04", 2, "ST001", "BK0019");
    addFinished(registrations, "WR0023", "G0006", now.minusMinutes(268),
        WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 1, "ST002", "BK0020");
    addFinished(registrations, "WR0024", "G0007", now.minusMinutes(260),
        WalkInRegistration.PRIORITY_URGENT, "Travelling with infant",
        "RT02", 2, "ST001", "BK0021");
    addFinished(registrations, "WR0025", "G0001", now.minusMinutes(251),
        WalkInRegistration.PRIORITY_NORMAL, null, "RT03", 4, "ST002", "BK0022");
    addFinished(registrations, "WR0026", "G0004", now.minusMinutes(243),
        WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 1, "ST001", "BK0023");

    // Two guests who gave up waiting, and two who were called but never came
    // forward - so every status appears somewhere in the listing.
    WalkInRegistration wr27 = new WalkInRegistration("WR0027", "G0002",
        now.minusMinutes(236), WalkInRegistration.PRIORITY_NORMAL, null, "RT05", 2);
    wr27.setStatus(WalkInRegistration.STATUS_CANCELLED);
    wr27.setServedBy("ST002");
    registrations.add(wr27);

    WalkInRegistration wr28 = new WalkInRegistration("WR0028", "G0005",
        now.minusMinutes(228), WalkInRegistration.PRIORITY_NORMAL, null, "RT02", 1);
    wr28.setStatus(WalkInRegistration.STATUS_CANCELLED);
    wr28.setServedBy("ST001");
    registrations.add(wr28);

    WalkInRegistration wr29 = new WalkInRegistration("WR0029", "G0003",
        now.minusMinutes(219), WalkInRegistration.PRIORITY_NORMAL, null, "RT04", 2);
    wr29.setStatus(WalkInRegistration.STATUS_NO_SHOW);
    wr29.setCalledAt(now.minusMinutes(205));
    wr29.setServedBy("ST002");
    registrations.add(wr29);

    WalkInRegistration wr30 = new WalkInRegistration("WR0030", "G0006",
        now.minusMinutes(210), WalkInRegistration.PRIORITY_URGENT,
        "Complaint escalation", "RT03", 1);
    wr30.setStatus(WalkInRegistration.STATUS_NO_SHOW);
    wr30.setCalledAt(now.minusMinutes(208));
    wr30.setServedBy("ST001");
    registrations.add(wr30);

    // Still waiting, so the queue itself also spans more than one screen.
    registrations.add(new WalkInRegistration("WR0031", "G0007",
        now.minusMinutes(62), WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 2));
    registrations.add(new WalkInRegistration("WR0032", "G0001",
        now.minusMinutes(54), WalkInRegistration.PRIORITY_NORMAL, null, "RT03", 1));

    return registrations;
  }

  /**
   * Adds one registration that has already become a booking.
   *
   * The finished rows differ only in their values, so they are built through
   * here rather than repeating the same six setter calls each time.
   */
  private void addFinished(ListInterface<WalkInRegistration> registrations,
      String regId, String guestId, LocalDateTime arrived, String priority,
      String reason, String typeId, int nights, String staffId, String bookingId) {
    WalkInRegistration reg = new WalkInRegistration(regId, guestId, arrived,
        priority, reason, typeId, nights);
    reg.setStatus(WalkInRegistration.STATUS_BOOKED);
    reg.setCalledAt(arrived.plusMinutes(6));
    reg.setBookedAt(arrived.plusMinutes(9));
    reg.setServedBy(staffId);
    reg.setBookingId(bookingId);
    registrations.add(reg);
  }

  /** Bookings from every source and at every stage of a stay. */
  public ListInterface<Booking> initializeBookings() {
    ListInterface<Booking> bookings = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    LocalDate today = LocalDate.now();

    Booking bk1 = new Booking("BK0001", "G0001", "RT01",
        today.minusDays(2), today, 2, Booking.PRIORITY_NORMAL,
        Booking.SOURCE_ONLINE, null, 150.00, now.minusDays(6), "ST002");
    bk1.setRoomNo("1001");
    bk1.setBookingStatus(Booking.STATUS_CHECKED_OUT);
    bookings.add(bk1);

    Booking bk2 = new Booking("BK0002", "G0002", "RT03",
        today.minusDays(1), today.plusDays(2), 2, Booking.PRIORITY_NORMAL,
        Booking.SOURCE_ONLINE, null, 234.00, now.minusDays(5), "ST001");
    bk2.setRoomNo("2001");
    bk2.setBookingStatus(Booking.STATUS_CHECKED_IN);
    bookings.add(bk2);

    Booking bk3 = new Booking("BK0003", "G0003", "RT03",
        today.minusDays(4), today.minusDays(1), 3, Booking.PRIORITY_NORMAL,
        Booking.SOURCE_PHONE, null, 260.00, now.minusDays(8), "ST002");
    bk3.setRoomNo("2002");
    bk3.setBookingStatus(Booking.STATUS_CHECKED_OUT);
    bookings.add(bk3);

    Booking bk4 = new Booking("BK0004", "G0004", "RT02",
        today, today.plusDays(3), 2, Booking.PRIORITY_NORMAL,
        Booking.SOURCE_WALK_IN, "WR0001", 180.00, now.minusMinutes(82), "ST001");
    bk4.setRoomNo("1004");
    bk4.setBookingStatus(Booking.STATUS_CONFIRMED);
    bookings.add(bk4);

    Booking bk5 = new Booking("BK0005", "G0006", "RT01",
        today, today.plusDays(2), 1, Booking.PRIORITY_URGENT,
        Booking.SOURCE_WALK_IN, "WR0002", 150.00, now.minusMinutes(63), "ST001");
    bk5.setRoomNo("1002");
    bk5.setBookingStatus(Booking.STATUS_CHECKED_IN);
    bookings.add(bk5);

    Booking bk6 = new Booking("BK0006", "G0005", "RT04",
        today.plusDays(7), today.plusDays(11), 4, Booking.PRIORITY_NORMAL,
        Booking.SOURCE_CORPORATE, null, 399.00, now.minusMinutes(30), "ST002");
    bookings.add(bk6);

    // Urgent and still waiting on room 1003 to be cleaned - the case that
    // shows the whole priority chain in flight.
    Booking bk7 = new Booking("BK0007", "G0003", "RT02",
        today, today.plusDays(2), 2, Booking.PRIORITY_URGENT,
        Booking.SOURCE_WALK_IN, "WR0005", 180.00, now.minusMinutes(18), "ST001");
    bookings.add(bk7);

    return bookings;
  }

  /** Which room each booking took, and when it was given up. */
  public ListInterface<RoomAssignment> initializeAssignments() {
    ListInterface<RoomAssignment> assignments = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    RoomAssignment ra1 = new RoomAssignment("RA0001", "BK0001", "1001",
        now.minusDays(2).withHour(15).withMinute(2), "ST002", RoomAssignment.REASON_INITIAL);
    ra1.setReleasedAt(now.minusHours(3));
    assignments.add(ra1);

    // A guest moved rooms: the first assignment is closed, the second is open.
    RoomAssignment ra2 = new RoomAssignment("RA0002", "BK0002", "2003",
        now.minusDays(1).withHour(14).withMinute(10), "ST001", RoomAssignment.REASON_INITIAL);
    ra2.setReleasedAt(now.minusDays(1).withHour(19).withMinute(45));
    assignments.add(ra2);

    assignments.add(new RoomAssignment("RA0003", "BK0002", "2001",
        now.minusDays(1).withHour(19).withMinute(45), "ST005",
        RoomAssignment.REASON_GUEST_REQUEST));

    RoomAssignment ra4 = new RoomAssignment("RA0004", "BK0003", "2002",
        now.minusDays(4).withHour(14).withMinute(30), "ST002", RoomAssignment.REASON_INITIAL);
    ra4.setReleasedAt(now.minusDays(1).withHour(12).withMinute(5));
    assignments.add(ra4);

    assignments.add(new RoomAssignment("RA0005", "BK0004", "1004",
        now.minusMinutes(82), "ST001", RoomAssignment.REASON_INITIAL));
    assignments.add(new RoomAssignment("RA0006", "BK0005", "1002",
        now.minusMinutes(58), "ST001", RoomAssignment.REASON_INITIAL));

    return assignments;
  }

  /** One bill per booking that has a room. */
  public ListInterface<Invoice> initializeInvoices() {
    ListInterface<Invoice> invoices = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    Invoice inv1 = new Invoice("INV0001", "BK0001", 300.00, now.minusHours(3));
    inv1.setAmountPaid(inv1.getTotalAmount());
    invoices.add(inv1);

    Invoice inv2 = new Invoice("INV0002", "BK0002", 702.00, now.minusDays(1));
    inv2.setDiscountAmount(50.00);
    inv2.setAmountPaid(400.00);
    invoices.add(inv2);

    Invoice inv3 = new Invoice("INV0003", "BK0003", 780.00, now.minusDays(1));
    inv3.setAmountPaid(inv3.getTotalAmount());
    invoices.add(inv3);

    invoices.add(new Invoice("INV0004", "BK0004", 540.00, now.minusMinutes(82)));

    Invoice inv5 = new Invoice("INV0005", "BK0005", 300.00, now.minusMinutes(58));
    inv5.setAmountPaid(150.00);
    invoices.add(inv5);

    return invoices;
  }

  /** The payments those bills were settled with. */
  public ListInterface<Payment> initializePayments() {
    ListInterface<Payment> payments = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    // One bill settled by two payments - a deposit then the balance.
    payments.add(new Payment("PY0001", "INV0001", 200.00, Payment.CARD,
        "APPR-884213", now.minusDays(2).withHour(15).withMinute(2), "ST002"));
    payments.add(new Payment("PY0002", "INV0001", 149.80, Payment.CASH,
        null, now.minusHours(3), "ST001"));
    payments.add(new Payment("PY0003", "INV0002", 400.00, Payment.EWALLET,
        "TNG-5590231", now.minusDays(1).withHour(14).withMinute(10), "ST001"));
    payments.add(new Payment("PY0004", "INV0003", 909.48, Payment.BANK_TRANSFER,
        "MBB-77102934", now.minusDays(1).withHour(12).withMinute(5), "ST002"));
    payments.add(new Payment("PY0005", "INV0005", 150.00, Payment.CASH,
        null, now.minusMinutes(58), "ST001"));

    return payments;
  }

  /**
   * Cleaning tasks at every stage.
   *
   * HK0003 is the one worth reading: it is urgent only because BK0007 is
   * waiting on that room, and it overtook HK0002 which was raised a day
   * earlier but sits in the normal lane.
   */
  public ListInterface<HousekeepingTask> initializeTasks() {
    ListInterface<HousekeepingTask> tasks = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    tasks.add(new HousekeepingTask("HK0001", "1001", HousekeepingTask.TYPE_CHECKOUT_CLEAN,
        "BK0001", now.minusHours(3)));

    HousekeepingTask hk2 = new HousekeepingTask("HK0002", "2002",
        HousekeepingTask.TYPE_CHECKOUT_CLEAN, "BK0003", now.minusDays(1).withHour(12));
    hk2.setStatus(HousekeepingTask.CLEANING_IN_PROGRESS);
    hk2.setAssignedTo("ST003");
    hk2.setStartedAt(now.minusMinutes(75));
    tasks.add(hk2);

    HousekeepingTask hk3 = new HousekeepingTask("HK0003", "1003",
        HousekeepingTask.TYPE_CHECKOUT_CLEAN, null, now.minusHours(4));
    hk3.setStatus(HousekeepingTask.CLEANING_IN_PROGRESS);
    hk3.setPriority(HousekeepingTask.PRIORITY_URGENT);
    hk3.setReservedForBookingId("BK0007");
    hk3.setAssignedTo("ST004");
    hk3.setStartedAt(now.minusMinutes(15));
    hk3.setRemark("Expedited - urgent booking waiting on this room");
    tasks.add(hk3);

    HousekeepingTask hk4 = new HousekeepingTask("HK0004", "2004",
        HousekeepingTask.TYPE_CHECKOUT_CLEAN, null, now.minusHours(5));
    hk4.setStatus(HousekeepingTask.INSPECTED);
    hk4.setAssignedTo("ST003");
    hk4.setStartedAt(now.minusHours(4));
    hk4.setRemark("Awaiting supervisor sign-off");
    tasks.add(hk4);

    // The completed urgent chain: WR0002 -> BK0005 -> HK0005.
    HousekeepingTask hk5 = new HousekeepingTask("HK0005", "1002",
        HousekeepingTask.TYPE_CHECKOUT_CLEAN, null, now.minusHours(6));
    hk5.setStatus(HousekeepingTask.READY_FOR_CHECK_IN);
    hk5.setPriority(HousekeepingTask.PRIORITY_URGENT);
    hk5.setReservedForBookingId("BK0005");
    hk5.setAssignedTo("ST003");
    hk5.setStartedAt(now.minusHours(5));
    hk5.setCompletedAt(now.minusHours(4));
    hk5.setRemark("Expedited - released to front desk");
    tasks.add(hk5);

    HousekeepingTask hk6 = new HousekeepingTask("HK0006", "2003",
        HousekeepingTask.TYPE_STAYOVER_CLEAN, "BK0002", now.minusHours(2));
    hk6.setStatus(HousekeepingTask.READY_FOR_CHECK_IN);
    hk6.setAssignedTo("ST004");
    hk6.setStartedAt(now.minusMinutes(100));
    hk6.setCompletedAt(now.minusMinutes(65));
    tasks.add(hk6);

    HousekeepingTask hk7 = new HousekeepingTask("HK0007", "3001",
        HousekeepingTask.TYPE_MAINTENANCE, null, now.minusDays(6));
    hk7.setStatus(HousekeepingTask.BLOCKED);
    hk7.setAssignedTo("ST004");
    hk7.setStartedAt(now.minusDays(6).plusMinutes(30));
    hk7.setRemark("Awaiting compressor part");
    tasks.add(hk7);

    return tasks;
  }

  /** The status history behind those tasks. */
  public ListInterface<RoomStatusLog> initializeStatusLogs() {
    ListInterface<RoomStatusLog> logs = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    logs.add(new RoomStatusLog("HL0001", "HK0005", "1002", null,
        HousekeepingTask.DIRTY, now.minusHours(6), "ST005", false, "Raised on check-out"));
    logs.add(new RoomStatusLog("HL0002", "HK0005", "1002", HousekeepingTask.DIRTY,
        HousekeepingTask.CLEANING_IN_PROGRESS, now.minusHours(5), "ST003", false, ""));
    logs.add(new RoomStatusLog("HL0003", "HK0005", "1002",
        HousekeepingTask.CLEANING_IN_PROGRESS, HousekeepingTask.INSPECTED,
        now.minusHours(4).minusMinutes(20), "ST003", false, ""));
    logs.add(new RoomStatusLog("HL0004", "HK0005", "1002", HousekeepingTask.INSPECTED,
        HousekeepingTask.READY_FOR_CHECK_IN, now.minusHours(4), "ST005", false,
        "Supervisor sign-off"));

    logs.add(new RoomStatusLog("HL0005", "HK0002", "2002", null,
        HousekeepingTask.DIRTY, now.minusDays(1).withHour(12), "ST005", false,
        "Raised on check-out"));
    logs.add(new RoomStatusLog("HL0006", "HK0002", "2002", HousekeepingTask.DIRTY,
        HousekeepingTask.CLEANING_IN_PROGRESS, now.minusMinutes(75), "ST003", false, ""));

    logs.add(new RoomStatusLog("HL0007", "HK0004", "2004",
        HousekeepingTask.CLEANING_IN_PROGRESS, HousekeepingTask.INSPECTED,
        now.minusHours(3), "ST003", false, "Ready for sign-off"));
    logs.add(new RoomStatusLog("HL0008", "HK0007", "3001",
        HousekeepingTask.CLEANING_IN_PROGRESS, HousekeepingTask.BLOCKED,
        now.minusDays(6).plusHours(1), "ST004", false, "Part not in stock"));

    logs.add(new RoomStatusLog("HL0009", "HK0003", "1003", null,
        HousekeepingTask.DIRTY, now.minusHours(4), "ST005", false, "Raised on check-out"));
    logs.add(new RoomStatusLog("HL0010", "HK0003", "1003", HousekeepingTask.DIRTY,
        HousekeepingTask.CLEANING_IN_PROGRESS, now.minusMinutes(15), "ST004", false,
        "Escalated to URGENT - booking waiting"));

    logs.add(new RoomStatusLog("HL0011", "HK0001", "1001", null,
        HousekeepingTask.DIRTY, now.minusHours(3), "ST001", false,
        "Raised on check-out"));

    return logs;
  }
}
