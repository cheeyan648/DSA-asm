package control;

import adt.ArrayList;
import adt.ArrayStack;
import adt.BinarySearchTree;
import adt.DualLaneQueue;
import adt.DualLaneQueueInterface;
import adt.HashMap;
import adt.ListInterface;
import adt.MapInterface;
import adt.StackInterface;
import adt.TreeInterface;
import dao.GenericDAO;
import dao.LoyaltyDataInitializer;
import dao.MasterDataInitializer;
import dao.OperationalDataInitializer;
import entity.Booking;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Invoice;
import entity.Member;
import entity.Notification;
import entity.Payment;
import entity.PointTransaction;
import entity.Redemption;
import entity.Reward;
import entity.Room;
import entity.RoomAssignment;
import entity.RoomStatusLog;
import entity.RoomType;
import entity.Staff;
import entity.WalkInRegistration;
import java.util.Comparator;
import utility.IdGenerator;

/**
 * The one place every table is loaded, held and saved.
 *
 * This is what makes the four modules one system. Previously each maintenance
 * class built its own lists in its own constructor, so a room the front desk
 * had just sold was still vacant as far as housekeeping was concerned - the
 * modules could not see each other because they were not looking at the same
 * data. Every module is now handed this same registry.
 *
 * The lookup structures are built alongside the lists rather than instead of
 * them: the list is what is written to file and iterated for reports, while
 * the map and the trees answer the by-key lookups that happen on almost every
 * screen.
 *
 * @author Tan Chee Yan
 */
public class ResortData {

  // ==================================================================
  // TABLES
  // ==================================================================

  private ListInterface<Staff> staffList;
  private ListInterface<RoomType> roomTypeList;
  private ListInterface<Room> roomList;
  private ListInterface<Guest> guestList;

  private ListInterface<WalkInRegistration> registrationList;

  private ListInterface<Booking> bookingList;
  private ListInterface<RoomAssignment> assignmentList;
  private ListInterface<Invoice> invoiceList;
  private ListInterface<Payment> paymentList;

  private ListInterface<HousekeepingTask> taskList;
  private ListInterface<RoomStatusLog> statusLogList;

  private ListInterface<Member> memberList;
  private ListInterface<Reward> rewardList;
  private ListInterface<Redemption> redemptionList;
  private ListInterface<PointTransaction> transactionList;
  private ListInterface<Notification> notificationList;

  // ==================================================================
  // LOOKUP STRUCTURES
  // ==================================================================

  /** roomNo -> Room. Consulted on every availability check. */
  private MapInterface<String, Room> roomIndex;

  /** guestId -> Guest. Consulted whenever a name is shown beside a record. */
  private MapInterface<String, Guest> guestIndex;

  private MapInterface<String, Staff> staffIndex;
  private MapInterface<String, RoomType> roomTypeIndex;

  /** Keyed lookup plus a sorted listing for free from an in-order walk. */
  private TreeInterface<String, Booking> bookingTree;
  /** Fast front-desk lookup by the guest-facing eight-digit confirmation. */
  private TreeInterface<String, Booking> confirmationTree;
  private TreeInterface<String, Member> memberTree;

  // ==================================================================
  // WAITING LINES
  // ==================================================================

  private DualLaneQueueInterface<WalkInRegistration> waitingList;
  private DualLaneQueueInterface<HousekeepingTask> cleaningQueue;

  /**
   * Latest reversible housekeeping status updates, newest on top.
   *
   * In-memory only, like the cleaning queue: the append-only status log is
   * what is saved, and this stack is rebuilt from it on load so a rollback
   * after restart still undoes the same update a live session would.
   */
  private StackInterface<RoomStatusLog> statusRollbackStack;

  /**
   * Redemptions awaiting a decision.
   *
   * Deliberately a single lane, not two: loyalty has no urgency concept, and
   * whoever asked first is served first.
   */
  private ListInterface<Redemption> pendingRedemptions;

  // ==================================================================
  // FILES
  // ==================================================================

  private final GenericDAO<Staff> staffDAO = new GenericDAO<>("staff.dat");
  private final GenericDAO<RoomType> roomTypeDAO = new GenericDAO<>("roomTypes.dat");
  private final GenericDAO<Room> roomDAO = new GenericDAO<>("rooms.dat");
  private final GenericDAO<Guest> guestDAO = new GenericDAO<>("guests.dat");
  private final GenericDAO<WalkInRegistration> registrationDAO =
      new GenericDAO<>("walkInRegistrations.dat");
  private final GenericDAO<Booking> bookingDAO = new GenericDAO<>("bookings.dat");
  private final GenericDAO<RoomAssignment> assignmentDAO =
      new GenericDAO<>("roomAssignments.dat");
  private final GenericDAO<Invoice> invoiceDAO = new GenericDAO<>("invoices.dat");
  private final GenericDAO<Payment> paymentDAO = new GenericDAO<>("payments.dat");
  private final GenericDAO<HousekeepingTask> taskDAO =
      new GenericDAO<>("housekeepingTasks.dat");
  private final GenericDAO<RoomStatusLog> statusLogDAO = new GenericDAO<>("roomStatusLogs.dat");
  private final GenericDAO<Member> memberDAO = new GenericDAO<>("members.dat");
  private final GenericDAO<Reward> rewardDAO = new GenericDAO<>("rewards.dat");
  private final GenericDAO<Redemption> redemptionDAO = new GenericDAO<>("redemptions.dat");
  private final GenericDAO<PointTransaction> transactionDAO =
      new GenericDAO<>("pointTransactions.dat");
  private final GenericDAO<Notification> notificationDAO =
      new GenericDAO<>("notifications.dat");

  public ResortData() {
    loadAll();
  }

  // ==================================================================
  // LOADING
  // ==================================================================

  /**
   * Loads every table, seeding any that has never been written.
   *
   * The order matters: the masters come first because everything else refers
   * to them, and the queues are rebuilt last because they are derived from the
   * records rather than stored separately.
   */
  public final void loadAll() {
    MasterDataInitializer masters = new MasterDataInitializer();
    staffList = loadOrSeed(staffDAO, masters.initializeStaff());
    roomTypeList = loadOrSeed(roomTypeDAO, masters.initializeRoomTypes());
    roomList = loadOrSeed(roomDAO, masters.initializeRooms());
    guestList = loadOrSeed(guestDAO, masters.initializeGuests());

    OperationalDataInitializer operational = new OperationalDataInitializer();
    registrationList = loadOrSeed(registrationDAO, operational.initializeRegistrations());
    bookingList = loadOrSeed(bookingDAO, operational.initializeBookings());
    assignmentList = loadOrSeed(assignmentDAO, operational.initializeAssignments());
    invoiceList = loadOrSeed(invoiceDAO, operational.initializeInvoices());
    paymentList = loadOrSeed(paymentDAO, operational.initializePayments());
    taskList = loadOrSeed(taskDAO, operational.initializeTasks());
    statusLogList = loadOrSeed(statusLogDAO, operational.initializeStatusLogs());

    LoyaltyDataInitializer loyalty = new LoyaltyDataInitializer();
    memberList = loadOrSeed(memberDAO, loyalty.initializeMembers());
    rewardList = loadOrSeed(rewardDAO, loyalty.initializeRewards());
    redemptionList = loadOrSeed(redemptionDAO, loyalty.initializeRedemptions());
    transactionList = loadOrSeed(transactionDAO, loyalty.initializeTransactions());
    notificationList = loadOrSeed(notificationDAO, loyalty.initializeNotifications());

    rebuildIndexes();
    rebuildQueues();
    rebuildStatusRollbackStack();
  }

  /**
   * Reads a table, or seeds and writes it if it has never been saved.
   *
   * @param dao the file this table lives in
   * @param seed the records to start with when the file is absent
   * @return the table's records
   */
  private <T> ListInterface<T> loadOrSeed(GenericDAO<T> dao, ListInterface<T> seed) {
    ListInterface<T> loaded = dao.retrieveFromFile();
    if (loaded.isEmpty()) {
      dao.saveToFile(seed);
      return seed;
    }
    return loaded;
  }

  /**
   * Rebuilds the maps and trees from the lists.
   *
   * The lists are the record of truth and are what get written to file; these
   * structures are derived from them, so they are thrown away and rebuilt
   * rather than maintained in parallel.
   */
  public final void rebuildIndexes() {
    roomIndex = new HashMap<>();
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room room = roomList.getEntry(i);
      roomIndex.put(room.getRoomNo(), room);
    }

    guestIndex = new HashMap<>();
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest guest = guestList.getEntry(i);
      guestIndex.put(guest.getGuestId(), guest);
    }

    staffIndex = new HashMap<>();
    for (int i = 1; i <= staffList.getNumberOfEntries(); i++) {
      Staff staff = staffList.getEntry(i);
      staffIndex.put(staff.getStaffId(), staff);
    }

    roomTypeIndex = new HashMap<>();
    for (int i = 1; i <= roomTypeList.getNumberOfEntries(); i++) {
      RoomType type = roomTypeList.getEntry(i);
      roomTypeIndex.put(type.getTypeId(), type);
    }

    bookingTree = new BinarySearchTree<>();
    confirmationTree = new BinarySearchTree<>();
    int nextConfirmation = 1;
    for (int i = 1; i <= bookingList.getNumberOfEntries(); i++) {
      Booking booking = bookingList.getEntry(i);
      bookingTree.add(booking.getBookingId(), booking);

      // Saved records created before confirmation numbers were introduced are
      // upgraded in memory on load.  This keeps existing demonstrations and
      // persisted data searchable without changing other modules.
      String confirmation = booking.getConfirmationNumber();
      if (!isEightDigitNumber(confirmation) || confirmationTree.contains(confirmation)) {
        do {
          confirmation = String.format("%08d", nextConfirmation++);
        } while (confirmationTree.contains(confirmation));
        booking.setConfirmationNumber(confirmation);
      }
      confirmationTree.add(confirmation, booking);
    }

    memberTree = new BinarySearchTree<>();
    for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
      Member member = memberList.getEntry(i);
      memberTree.add(member.getMemberId(), member);
    }
  }

  /**
   * Rebuilds the waiting lines from the records.
   *
   * A queue is a position in a line, not a fact about a guest, so it is not
   * persisted - it is reconstructed at startup from whoever is still waiting.
   * Entries are sorted by arrival before being enqueued so the order that was
   * in force when the system was last closed is restored exactly.
   */
  public final void rebuildQueues() {
    waitingList = new DualLaneQueue<>();

    ListInterface<WalkInRegistration> waiting =
        registrationList.filter(reg -> reg.isWaiting());
    waiting.sort(Comparator.comparing(WalkInRegistration::getArrivalTime));
    for (int i = 1; i <= waiting.getNumberOfEntries(); i++) {
      WalkInRegistration reg = waiting.getEntry(i);
      waitingList.enqueue(reg, reg.getPriority());
    }

    cleaningQueue = new DualLaneQueue<>();
    ListInterface<HousekeepingTask> pending =
        taskList.filter(task -> task.isPendingCleaning());
    pending.sort(Comparator.comparing(HousekeepingTask::getCreatedAt));
    for (int i = 1; i <= pending.getNumberOfEntries(); i++) {
      HousekeepingTask task = pending.getEntry(i);
      cleaningQueue.enqueue(task, task.getPriority());
    }

    pendingRedemptions = new ArrayList<>();
    ListInterface<Redemption> awaiting =
        redemptionList.filter(redemption -> redemption.isPending());
    awaiting.sort(Comparator.comparing(Redemption::getRequestDate));
    for (int i = 1; i <= awaiting.getNumberOfEntries(); i++) {
      pendingRedemptions.add(awaiting.getEntry(i));
    }
  }

  /**
   * Starts the housekeeping rollback stack empty for this session.
   *
   * Historical status logs stay on file for reports. Only updates made after
   * the application has started are pushed for rollback.
   */
  public final void rebuildStatusRollbackStack() {
    statusRollbackStack = new ArrayStack<>();
  }

  /**
   * Whether a compensating row undoes the update currently on top of the
   * rollback stack.
   *
   * Uses the existing remark convention ("Rollback of {logId}") and, when
   * that is absent, the existing from/to statuses on the same task. No extra
   * fields are required.
   */
  private boolean rollbackUndoesTop(RoomStatusLog rollback, RoomStatusLog top) {
    if (rollback == null || top == null) {
      return false;
    }

    String remark = rollback.getRemark();
    if (remark != null && !remark.isBlank()) {
      String expected = "Rollback of " + top.getLogId();
      if (expected.equals(remark)) {
        return true;
      }
      if (remark.startsWith("Rollback of ")) {
        return false;
      }
    }

    return top.getTaskId() != null && top.getTaskId().equals(rollback.getTaskId())
        && top.getToStatus() != null && top.getToStatus().equals(rollback.getFromStatus())
        && top.getFromStatus() != null
        && top.getFromStatus().equals(rollback.getToStatus());
  }

  // ==================================================================
  // SAVING
  // ==================================================================

  /** Writes every table back to its file. */
  public void saveAll() {
    saveMasters();
    saveRegistrations();
    saveFrontDesk();
    saveHousekeeping();
    saveLoyalty();
  }

  public void saveMasters() {
    staffDAO.saveToFile(staffList);
    roomTypeDAO.saveToFile(roomTypeList);
    roomDAO.saveToFile(roomList);
    guestDAO.saveToFile(guestList);
  }

  public void saveRegistrations() {
    registrationDAO.saveToFile(registrationList);
  }

  public void saveFrontDesk() {
    bookingDAO.saveToFile(bookingList);
    assignmentDAO.saveToFile(assignmentList);
    invoiceDAO.saveToFile(invoiceList);
    paymentDAO.saveToFile(paymentList);
  }

  public void saveHousekeeping() {
    taskDAO.saveToFile(taskList);
    statusLogDAO.saveToFile(statusLogList);
  }

  public void saveLoyalty() {
    memberDAO.saveToFile(memberList);
    rewardDAO.saveToFile(rewardList);
    redemptionDAO.saveToFile(redemptionList);
    transactionDAO.saveToFile(transactionList);
    notificationDAO.saveToFile(notificationList);
  }

  /** Deletes every data file. Used by the tests to start from nothing. */
  public void deleteAllFiles() {
    staffDAO.deleteFile();
    roomTypeDAO.deleteFile();
    roomDAO.deleteFile();
    guestDAO.deleteFile();
    registrationDAO.deleteFile();
    bookingDAO.deleteFile();
    assignmentDAO.deleteFile();
    invoiceDAO.deleteFile();
    paymentDAO.deleteFile();
    taskDAO.deleteFile();
    statusLogDAO.deleteFile();
    memberDAO.deleteFile();
    rewardDAO.deleteFile();
    redemptionDAO.deleteFile();
    transactionDAO.deleteFile();
    notificationDAO.deleteFile();
  }

  // ==================================================================
  // TABLE ACCESS
  // ==================================================================

  public ListInterface<Staff> getStaffList() {
    return staffList;
  }

  public ListInterface<RoomType> getRoomTypeList() {
    return roomTypeList;
  }

  public ListInterface<Room> getRoomList() {
    return roomList;
  }

  public ListInterface<Guest> getGuestList() {
    return guestList;
  }

  public ListInterface<WalkInRegistration> getRegistrationList() {
    return registrationList;
  }

  public ListInterface<Booking> getBookingList() {
    return bookingList;
  }

  public ListInterface<RoomAssignment> getAssignmentList() {
    return assignmentList;
  }

  public ListInterface<Invoice> getInvoiceList() {
    return invoiceList;
  }

  public ListInterface<Payment> getPaymentList() {
    return paymentList;
  }

  public ListInterface<HousekeepingTask> getTaskList() {
    return taskList;
  }

  public ListInterface<RoomStatusLog> getStatusLogList() {
    return statusLogList;
  }

  public ListInterface<Member> getMemberList() {
    return memberList;
  }

  public ListInterface<Reward> getRewardList() {
    return rewardList;
  }

  public ListInterface<Redemption> getRedemptionList() {
    return redemptionList;
  }

  public ListInterface<PointTransaction> getTransactionList() {
    return transactionList;
  }

  public ListInterface<Notification> getNotificationList() {
    return notificationList;
  }

  public DualLaneQueueInterface<WalkInRegistration> getWaitingList() {
    return waitingList;
  }

  public DualLaneQueueInterface<HousekeepingTask> getCleaningQueue() {
    return cleaningQueue;
  }

  public StackInterface<RoomStatusLog> getStatusRollbackStack() {
    return statusRollbackStack;
  }

  public ListInterface<Redemption> getPendingRedemptions() {
    return pendingRedemptions;
  }

  // ==================================================================
  // LOOKUPS
  // ==================================================================

  public Room findRoom(String roomNo) {
    return roomIndex.get(roomNo);
  }

  public Guest findGuest(String guestId) {
    return guestIndex.get(guestId);
  }

  public Staff findStaff(String staffId) {
    return staffIndex.get(staffId);
  }

  public RoomType findRoomType(String typeId) {
    return roomTypeIndex.get(typeId);
  }

  public Booking findBooking(String bookingId) {
    return bookingTree.search(bookingId);
  }

  /**
   * Retrieves a booking directly from its guest-facing confirmation number.
   * The binary search tree avoids scanning all booking records during a busy
   * front-desk call.
   */
  public Booking findBookingByConfirmation(String confirmationNumber) {
    return confirmationTree.search(confirmationNumber);
  }

  public Member findMember(String memberId) {
    return memberTree.search(memberId);
  }

  /** Bookings in ID order, straight from the tree - no sorting needed. */
  public ListInterface<Booking> getBookingsSorted() {
    return bookingTree.getAllInOrder();
  }

  /** Members in ID order, straight from the tree. */
  public ListInterface<Member> getMembersSorted() {
    return memberTree.getAllInOrder();
  }

  /**
   * Finds a guest by their IC or passport number.
   *
   * This is how a returning guest is recognised at the counter. Matching on
   * the identity document rather than the name is what stops the same person
   * being created twice because they gave their name differently.
   */
  public Guest findGuestByIc(String icPassportNo) {
    if (icPassportNo == null || icPassportNo.isBlank()) {
      return null;
    }

    // Dashes and spaces are punctuation, not part of the number: an IC typed
    // as 970818-14-5266 and one stored as 970818145266 are the same document,
    // so both sides are stripped before they are compared.
    final String wanted = normaliseIc(icPassportNo);
    return guestList.search(
        guest -> wanted.equalsIgnoreCase(normaliseIc(guest.getIcPassportNo())));
  }

  /** An IC or passport number reduced to just its letters and digits. */
  private static String normaliseIc(String icPassportNo) {
    return (icPassportNo == null) ? "" : icPassportNo.replace("-", "").replace(" ", "");
  }

  /** The membership held by a guest, or null if they are not a member. */
  public Member findMemberByGuest(String guestId) {
    if (guestId == null) {
      return null;
    }
    return memberList.search(member -> guestId.equals(member.getGuestId()));
  }

  public WalkInRegistration findRegistration(String regId) {
    if (regId == null) {
      return null;
    }
    return registrationList.search(reg -> regId.equals(reg.getRegId()));
  }

  public HousekeepingTask findTask(String taskId) {
    if (taskId == null) {
      return null;
    }
    return taskList.search(task -> taskId.equals(task.getTaskId()));
  }

  public Reward findReward(String rewardId) {
    if (rewardId == null) {
      return null;
    }
    return rewardList.search(reward -> rewardId.equals(reward.getRewardId()));
  }

  public Redemption findRedemption(String redemptionId) {
    if (redemptionId == null) {
      return null;
    }
    return redemptionList.search(r -> redemptionId.equals(r.getRedemptionId()));
  }

  /** The bill for a booking, or null if no room has been assigned yet. */
  public Invoice findInvoiceByBooking(String bookingId) {
    if (bookingId == null) {
      return null;
    }
    return invoiceList.search(invoice -> bookingId.equals(invoice.getBookingId()));
  }

  public Invoice findInvoice(String invoiceId) {
    if (invoiceId == null) {
      return null;
    }
    return invoiceList.search(invoice -> invoiceId.equals(invoice.getInvoiceId()));
  }

  /** The assignment row still open for a booking, if it holds a room. */
  public RoomAssignment findOpenAssignment(String bookingId) {
    if (bookingId == null) {
      return null;
    }
    return assignmentList.search(
        assignment -> bookingId.equals(assignment.getBookingId()) && assignment.isOpen());
  }

  /** The newest cleaning task raised for a room, whatever its status. */
  public HousekeepingTask findLatestTaskForRoom(String roomNo) {
    if (roomNo == null) {
      return null;
    }
    HousekeepingTask latest = null;
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      HousekeepingTask task = taskList.getEntry(i);
      if (roomNo.equals(task.getRoomNo())
          && (latest == null || task.getCreatedAt().isAfter(latest.getCreatedAt()))) {
        latest = task;
      }
    }
    return latest;
  }

  /** The open cleaning task for a room - one that has not reached ready. */
  public HousekeepingTask findOpenTaskForRoom(String roomNo) {
    if (roomNo == null) {
      return null;
    }
    return taskList.search(task -> roomNo.equals(task.getRoomNo())
        && task.isActiveWork());
  }

  // ==================================================================
  // ID GENERATION
  // ==================================================================

  /**
   * Issues the next ID for a table.
   *
   * The IDs already in use are collected and handed to the generator, which
   * takes the highest rather than the count - so an ID is never reissued after
   * a record is removed.
   */
  private <T> String nextId(String prefix, int digits, ListInterface<T> records,
      java.util.function.Function<T, String> idOf) {
    ListInterface<String> ids = new ArrayList<>();
    for (int i = 1; i <= records.getNumberOfEntries(); i++) {
      ids.add(idOf.apply(records.getEntry(i)));
    }
    return IdGenerator.next(prefix, digits, ids);
  }

  public String nextGuestId() {
    return nextId("G", 4, guestList, Guest::getGuestId);
  }

  public String nextRegistrationId() {
    return nextId("WR", 4, registrationList, WalkInRegistration::getRegId);
  }

  public String nextBookingId() {
    return nextId("BK", 4, bookingList, Booking::getBookingId);
  }

  /** Issues the next unused eight-digit confirmation number. */
  public String nextConfirmationNumber() {
    ListInterface<String> confirmations = new ArrayList<>();
    for (int i = 1; i <= bookingList.getNumberOfEntries(); i++) {
      String confirmation = bookingList.getEntry(i).getConfirmationNumber();
      if (isEightDigitNumber(confirmation)) {
        confirmations.add(confirmation);
      }
    }
    return IdGenerator.next("", 8, confirmations);
  }

  public String nextAssignmentId() {
    return nextId("RA", 4, assignmentList, RoomAssignment::getAssignmentId);
  }

  public String nextInvoiceId() {
    return nextId("INV", 4, invoiceList, Invoice::getInvoiceId);
  }

  public String nextPaymentId() {
    return nextId("PY", 4, paymentList, Payment::getPaymentId);
  }

  public String nextTaskId() {
    return nextId("HK", 4, taskList, HousekeepingTask::getTaskId);
  }

  public String nextStatusLogId() {
    return nextId("HL", 4, statusLogList, RoomStatusLog::getLogId);
  }

  public String nextMemberId() {
    return nextId("L", 4, memberList, Member::getMemberId);
  }

  public String nextRedemptionId() {
    return nextId("RD", 4, redemptionList, Redemption::getRedemptionId);
  }

  public String nextTransactionId() {
    return nextId("PT", 4, transactionList, PointTransaction::getTxnId);
  }

  public String nextNotificationId() {
    return nextId("NT", 4, notificationList, Notification::getNotificationId);
  }

  public String nextRewardId() {
    return nextId("RW", 3, rewardList, Reward::getRewardId);
  }

  // ==================================================================
  // KEEPING THE INDEXES IN STEP
  // ==================================================================

  /** Adds a guest to both the table and the lookup. */
  public void addGuest(Guest guest) {
    guestList.add(guest);
    guestIndex.put(guest.getGuestId(), guest);
  }

  /** Adds a booking to both the table and the tree. */
  public void addBooking(Booking booking) {
    if (!isEightDigitNumber(booking.getConfirmationNumber())
        || confirmationTree.contains(booking.getConfirmationNumber())) {
      booking.setConfirmationNumber(nextConfirmationNumber());
    }
    bookingList.add(booking);
    bookingTree.add(booking.getBookingId(), booking);
    confirmationTree.add(booking.getConfirmationNumber(), booking);
  }

  /** Adds a member to both the table and the tree. */
  public void addMember(Member member) {
    memberList.add(member);
    memberTree.add(member.getMemberId(), member);
  }

  /** Removes a booking from both the table and the tree. */
  public void removeBooking(Booking booking) {
    bookingList.removeEntry(booking);
    bookingTree.remove(booking.getBookingId());
    confirmationTree.remove(booking.getConfirmationNumber());
  }

  private boolean isEightDigitNumber(String value) {
    if (value == null || value.length() != 8) {
      return false;
    }
    for (int i = 0; i < value.length(); i++) {
      if (!Character.isDigit(value.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
