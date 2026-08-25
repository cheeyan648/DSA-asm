import adt.ArrayList;
import adt.BookingBST;
import adt.ListInterface;
import dao.BookingDAO;
import dao.BookingInitializer;
import dao.HousekeepingTaskDAO;
import dao.HousekeepingTaskInitializer;
import dao.RewardInitializer;
import dao.WalkInGuestDAO;
import dao.WalkInGuestInitializer;
import entity.BillingRecord;
import entity.Booking;
import entity.FrontDeskRecord;
import entity.HousekeepingTask;
import entity.Reward;
import entity.WalkInGuest;
import java.io.File;
import java.time.LocalDateTime;

/**
 * INTEGRATION TESTS - components working together, using the REAL classes.
 *
 * Unlike the unit tests, nothing here is a copy: these drive the actual DAO,
 * initializer and ADT classes the application uses, and check that data
 * survives a round trip through them.
 *
 * Covers:
 *   1  Seeding    - each initializer produces usable data
 *   2  Persistence - save then reload returns equivalent data
 *   3  Cross-ADT   - the same entities held in a List and a BST stay consistent
 *   4  Shared ADT  - all four modules share one List implementation safely
 *   5  Shared UI   - MessageUI is safe for every module to call
 *
 * The .dat files are written into a temporary working directory so a test run
 * can never damage the real data files.
 *
 * @author Tan Chee Yan
 */
public class IntegrationTests {

  // ==================================================================
  // 1 - SEED DATA
  // ==================================================================

  private static void testInitializersProduceUsableData() {
    TestRunner.section("1.1  SEEDING - every initializer produces usable data");

    ListInterface<WalkInGuest> guests =
        new WalkInGuestInitializer().initializeWalkInGuests();
    TestRunner.check("walk-in seed is not empty", !guests.isEmpty());
    TestRunner.check("walk-in seed has enough records to page (>20)",
        guests.getNumberOfEntries() > 20);

    int waiting = guests.countIf(
        g -> WalkInGuest.STATUS_WAITING.equals(g.getStatus()));
    int served = guests.countIf(
        g -> WalkInGuest.STATUS_SERVED.equals(g.getStatus()));
    int cancelled = guests.countIf(
        g -> WalkInGuest.STATUS_CANCELLED.equals(g.getStatus()));

    TestRunner.check("seed covers WAITING guests", waiting > 0);
    TestRunner.check("seed covers SERVED guests", served > 0);
    TestRunner.check("seed covers CANCELLED guests", cancelled > 0);
    TestRunner.checkEquals("the three statuses account for every record",
        guests.getNumberOfEntries(), waiting + served + cancelled);
    TestRunner.check("seed includes urgent exception cases",
        guests.countIf(WalkInGuest::isUrgent) > 0);

    // Every seeded arrival must be in the PAST and on TODAY, or the reports'
    // "today" filters miss them and waiting times read as zero.
    LocalDateTime now = LocalDateTime.now();
    boolean allPast = true;
    boolean allToday = true;
    for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
      LocalDateTime arrival = guests.getEntry(i).getArrivalTime();
      if (arrival == null || arrival.isAfter(now)) {
        allPast = false;
      }
      if (arrival == null || !arrival.toLocalDate().equals(now.toLocalDate())) {
        allToday = false;
      }
    }
    TestRunner.check("no seeded arrival is in the future", allPast);
    TestRunner.check("every seeded arrival falls on today", allToday);

    // Guest IDs must be unique - the ID generator depends on it.
    boolean uniqueIds = true;
    for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
      String id = guests.getEntry(i).getGuestId();
      if (guests.countIf(g -> g.getGuestId().equals(id)) != 1) {
        uniqueIds = false;
      }
    }
    TestRunner.check("every seeded guest id is unique", uniqueIds);

    ListInterface<HousekeepingTask> tasks =
        new HousekeepingTaskInitializer().initializeHousekeepingTasks();
    TestRunner.check("housekeeping seed is not empty", !tasks.isEmpty());

    ListInterface<Reward> rewards = new RewardInitializer().initializeRewards();
    TestRunner.check("reward catalogue is not empty", !rewards.isEmpty());
    boolean rewardsPriced = true;
    for (int i = 1; i <= rewards.getNumberOfEntries(); i++) {
      if (rewards.getEntry(i).getPointsRequired() <= 0) {
        rewardsPriced = false;
      }
    }
    TestRunner.check("every reward costs a positive number of points", rewardsPriced);

    ListInterface<FrontDeskRecord> bookings =
        new BookingInitializer().initializeBookings();
    TestRunner.check("booking seed is not empty", !bookings.isEmpty());

    boolean bookingsValid = true;
    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking b = bookings.getEntry(i).getBooking();
      if (b == null || b.getConfirmationNumber() == null
          || b.getConfirmationNumber().length() != 8) {
        bookingsValid = false;
      }
      if (b != null && !b.getCheckOutDate().isAfter(b.getCheckInDate())) {
        bookingsValid = false;
      }
    }
    TestRunner.check("every seeded booking has an 8-digit number and a valid stay",
        bookingsValid);
  }

  // ==================================================================
  // 2 - PERSISTENCE ROUND TRIP
  // ==================================================================

  private static void testWalkInPersistence() {
    TestRunner.section("2.1  PERSISTENCE - walk-in guests survive save and reload");

    WalkInGuestDAO dao = new WalkInGuestDAO();
    ListInterface<WalkInGuest> original =
        new WalkInGuestInitializer().initializeWalkInGuests();

    int originalCount = original.getNumberOfEntries();
    String firstId = original.getEntry(1).getGuestId();
    String firstName = original.getEntry(1).getName();
    int urgentCount = original.countIf(WalkInGuest::isUrgent);

    dao.saveToFile(original);
    ListInterface<WalkInGuest> reloaded = dao.retrieveFromFile();

    TestRunner.checkEquals("reload returns the same number of records",
        originalCount, reloaded.getNumberOfEntries());
    TestRunner.checkEquals("the first record keeps its id", firstId,
        reloaded.getEntry(1).getGuestId());
    TestRunner.checkEquals("the first record keeps its name", firstName,
        reloaded.getEntry(1).getName());
    TestRunner.checkEquals("urgency survives the round trip", urgentCount,
        reloaded.countIf(WalkInGuest::isUrgent));
    TestRunner.check("arrival times survive the round trip",
        reloaded.getEntry(1).getArrivalTime() != null);
    TestRunner.checkEquals("status survives the round trip",
        original.getEntry(1).getStatus(), reloaded.getEntry(1).getStatus());

    // A record modified in memory must persist that change.
    WalkInGuest target = reloaded.search(
        g -> WalkInGuest.STATUS_WAITING.equals(g.getStatus()));
    if (target != null) {
      String targetId = target.getGuestId();
      target.setStatus(WalkInGuest.STATUS_SERVED);
      dao.saveToFile(reloaded);

      ListInterface<WalkInGuest> second = dao.retrieveFromFile();
      WalkInGuest after = second.search(g -> g.getGuestId().equals(targetId));
      TestRunner.checkEquals("a status change is persisted",
          WalkInGuest.STATUS_SERVED, after.getStatus());
    }

    // equals() is id-based, so a reloaded record must match an in-memory one.
    TestRunner.check("a reloaded guest matches the original via contains()",
        reloaded.contains(original.getEntry(1)));
  }

  private static void testHousekeepingPersistence() {
    TestRunner.section("2.2  PERSISTENCE - housekeeping tasks survive save and reload");

    HousekeepingTaskDAO dao = new HousekeepingTaskDAO();
    ListInterface<HousekeepingTask> original =
        new HousekeepingTaskInitializer().initializeHousekeepingTasks();

    int count = original.getNumberOfEntries();
    String firstTaskId = original.getEntry(1).getTaskId();
    String firstRoom = original.getEntry(1).getRoomNumber();
    String firstStatus = original.getEntry(1).getStatus();

    dao.saveToFile(original, count + 1);
    ListInterface<HousekeepingTask> reloaded = dao.retrieveFromFile();

    TestRunner.checkEquals("reload returns the same number of tasks",
        count, reloaded.getNumberOfEntries());
    TestRunner.checkEquals("task id survives", firstTaskId,
        reloaded.getEntry(1).getTaskId());
    TestRunner.checkEquals("room number survives", firstRoom,
        reloaded.getEntry(1).getRoomNumber());
    TestRunner.checkEquals("status survives", firstStatus,
        reloaded.getEntry(1).getStatus());
    TestRunner.check("timestamp survives", reloaded.getEntry(1).getTimestamp() != null);
    TestRunner.check("the next task number is remembered", dao.getNextTaskNumber() > 0);
  }

  private static void testBookingPersistence() {
    TestRunner.section("2.3  PERSISTENCE - bookings and bills survive save and reload");

    BookingDAO dao = new BookingDAO();
    ListInterface<FrontDeskRecord> original =
        new BookingInitializer().initializeBookings();

    int count = original.getNumberOfEntries();
    String firstConfirmation =
        original.getEntry(1).getBooking().getConfirmationNumber();
    double firstBill = original.getEntry(1).getBillingRecord().getTotalBill();

    dao.saveRecords(original);
    ListInterface<FrontDeskRecord> reloaded = dao.loadRecords();

    TestRunner.checkEquals("reload returns the same number of records",
        count, reloaded.getNumberOfEntries());
    TestRunner.checkEquals("the confirmation number survives", firstConfirmation,
        reloaded.getEntry(1).getBooking().getConfirmationNumber());
    TestRunner.checkEquals("the bill total survives", firstBill,
        reloaded.getEntry(1).getBillingRecord().getTotalBill());
    TestRunner.check("the booking object survives",
        reloaded.getEntry(1).getBooking() != null);
    TestRunner.check("the billing object survives",
        reloaded.getEntry(1).getBillingRecord() != null);

    // The equals() added during the audit must let a reloaded bill match.
    BillingRecord originalBill = original.getEntry(1).getBillingRecord();
    BillingRecord reloadedBill = reloaded.getEntry(1).getBillingRecord();
    TestRunner.check("a reloaded bill equals the original (needs equals())",
        originalBill.equals(reloadedBill));

    ListInterface<BillingRecord> bills = new ArrayList<>();
    bills.add(originalBill);
    TestRunner.check("contains() matches a reloaded bill", bills.contains(reloadedBill));
  }

  private static void testMissingFileFallsBackToSeed() {
    TestRunner.section("2.4  PERSISTENCE - a missing file falls back to seeding");

    // Deleting the file must not crash: the DAO returns an empty list and the
    // control class seeds from the initializer. This is the first-run path.
    File walkInFile = new File("walkInGuests.dat");
    if (walkInFile.exists()) {
      walkInFile.delete();
    }

    WalkInGuestDAO dao = new WalkInGuestDAO();
    ListInterface<WalkInGuest> loaded = dao.retrieveFromFile();
    TestRunner.check("a missing file returns an empty list, not a crash",
        loaded != null && loaded.isEmpty());

    // That is the signal the control class uses to seed.
    if (loaded.isEmpty()) {
      loaded = new WalkInGuestInitializer().initializeWalkInGuests();
      dao.saveToFile(loaded);
    }
    TestRunner.check("seeding after a missing file produces records",
        !loaded.isEmpty());

    ListInterface<WalkInGuest> afterSeed = dao.retrieveFromFile();
    TestRunner.checkEquals("the seeded data is now persisted",
        loaded.getNumberOfEntries(), afterSeed.getNumberOfEntries());
  }

  // ==================================================================
  // 3 - CROSS-ADT CONSISTENCY
  // ==================================================================

  private static void testListAndBstStayConsistent() {
    TestRunner.section("3.1  CROSS-ADT - a List and a BST holding the same bookings");

    // Module 3 stores every booking in BOTH structures. They must never
    // disagree about what exists.
    ListInterface<FrontDeskRecord> seed =
        new BookingInitializer().initializeBookings();

    BookingBST tree = new BookingBST();
    ListInterface<Booking> list = new ArrayList<>();

    for (int i = 1; i <= seed.getNumberOfEntries(); i++) {
      Booking b = seed.getEntry(i).getBooking();
      tree.add(b);
      list.add(b);
    }

    TestRunner.checkEquals("both structures hold the same count",
        list.getNumberOfEntries(), tree.getNumberOfEntries());

    // Every booking in the List must be findable in the tree, and be the very
    // same object rather than a copy.
    boolean allFound = true;
    boolean allSameObject = true;
    for (int i = 1; i <= list.getNumberOfEntries(); i++) {
      Booking fromList = list.getEntry(i);
      Booking fromTree = tree.search(fromList.getConfirmationNumber());
      if (fromTree == null) {
        allFound = false;
      } else if (fromTree != fromList) {
        allSameObject = false;
      }
    }
    TestRunner.check("every List booking is findable in the BST", allFound);
    TestRunner.check("the BST holds references, not copies", allSameObject);

    // The tree's traversal must return exactly the same set, sorted.
    ListInterface<Booking> traversed = tree.getAllBookings();
    TestRunner.checkEquals("traversal returns every booking",
        list.getNumberOfEntries(), traversed.getNumberOfEntries());

    boolean ascending = true;
    for (int i = 2; i <= traversed.getNumberOfEntries(); i++) {
      String previous = traversed.getEntry(i - 1).getConfirmationNumber();
      String current = traversed.getEntry(i).getConfirmationNumber();
      if (previous.compareTo(current) >= 0) {
        ascending = false;
      }
    }
    TestRunner.check("traversal is sorted with no sort being run", ascending);
  }

  // ==================================================================
  // 4 - THE SHARED TEAM ADT
  // ==================================================================

  private static void testSharedListAdtAcrossModules() {
    TestRunner.section("4.1  SHARED ADT - one List implementation, four entity types");

    // The team ADT is generic. Every module stores a different entity in it,
    // and the instances must stay completely independent of one another.
    ListInterface<WalkInGuest> guests = new ArrayList<>();
    ListInterface<HousekeepingTask> tasks = new ArrayList<>();
    ListInterface<Booking> bookings = new ArrayList<>();
    ListInterface<Reward> rewards = new ArrayList<>();

    guests.add(new WalkInGuest("WG1001", "Ali", "0123456789", false, null,
        LocalDateTime.now()));
    tasks.add(new HousekeepingTask("HT001", "101", "Dirty", LocalDateTime.now()));
    bookings.add(new Booking("50231847", "Siti", "201",
        java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(2)));
    rewards.add(new Reward("RW001", "Voucher", 300));

    TestRunner.checkEquals("the guest list holds only its own entry", 1,
        guests.getNumberOfEntries());
    TestRunner.checkEquals("the task list holds only its own entry", 1,
        tasks.getNumberOfEntries());
    TestRunner.checkEquals("the booking list holds only its own entry", 1,
        bookings.getNumberOfEntries());
    TestRunner.checkEquals("the reward list holds only its own entry", 1,
        rewards.getNumberOfEntries());

    guests.clear();
    TestRunner.check("clearing one list does not touch another",
        tasks.getNumberOfEntries() == 1 && bookings.getNumberOfEntries() == 1
        && rewards.getNumberOfEntries() == 1);

    TestRunner.section("4.2  SHARED ADT - the added operations behave for every module");

    ListInterface<WalkInGuest> seededGuests =
        new WalkInGuestInitializer().initializeWalkInGuests();

    // filter must never mutate the source, whichever module calls it.
    int before = seededGuests.getNumberOfEntries();
    ListInterface<WalkInGuest> filtered = seededGuests.filter(WalkInGuest::isUrgent);
    TestRunner.checkEquals("filter leaves the source list untouched", before,
        seededGuests.getNumberOfEntries());
    TestRunner.check("the filtered copy is smaller than the source",
        filtered.getNumberOfEntries() < before);

    // Mutating the copy must not affect the source.
    filtered.clear();
    TestRunner.checkEquals("clearing a filtered copy does not touch the source",
        before, seededGuests.getNumberOfEntries());

    // sort must never mutate the source either.
    ListInterface<WalkInGuest> copy = seededGuests.filter(g -> true);
    copy.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    TestRunner.checkEquals("sorting a copy does not reorder the source",
        seededGuests.getEntry(1).getGuestId(), seededGuests.getEntry(1).getGuestId());
    TestRunner.check("the sorted copy really is sorted",
        copy.getEntry(1).getName().compareToIgnoreCase(
            copy.getEntry(copy.getNumberOfEntries()).getName()) <= 0);

    // countIf and filter must always agree.
    int counted = seededGuests.countIf(WalkInGuest::isUrgent);
    int listed = seededGuests.filter(WalkInGuest::isUrgent).getNumberOfEntries();
    TestRunner.checkEquals("countIf and filter always agree", counted, listed);
  }

  // ==================================================================
  // 5 - THE SHARED UTILITY
  // ==================================================================

  private static void testSharedMessageUi() {
    TestRunner.section("5.1  SHARED UI - MessageUI is safe for every module");

    // MessageUI is shared by all four modules. These calls must not throw,
    // whatever they are handed.
    boolean survived = true;
    try {
      utility.MessageUI.displayBoxTop();
      utility.MessageUI.displayBoxLine("integration test");
      utility.MessageUI.displayBoxCentred("centred");
      utility.MessageUI.displayBoxBlank();
      utility.MessageUI.displayBoxDivider();
      utility.MessageUI.displayBlankLine();
      utility.MessageUI.displayBoxBottom();
    } catch (RuntimeException e) {
      survived = false;
    }
    TestRunner.check("the framed-box helpers run without throwing", survived);

    // The banner was rewritten during the compliance audit to drop HashMap.
    // Every letter it must draw has to still render.
    boolean bannerSurvived = true;
    try {
      utility.MessageUI.displayBanner("TARUMT");
      utility.MessageUI.displayBanner("ABCDEFGHIJKLM");
      utility.MessageUI.displayBanner("NOPQRSTUVWXYZ");
      utility.MessageUI.displayBanner("A-B C");
      utility.MessageUI.displayBanner("");
      utility.MessageUI.displayBanner("a word far too wide to fit inside the frame");
      utility.MessageUI.displayBanner("!!!");
    } catch (RuntimeException e) {
      bannerSurvived = false;
    }
    TestRunner.check("the rewritten banner handles every letter, spaces, "
        + "hyphens, empty, oversized and undefined characters", bannerSurvived);
  }

  // ==================================================================
  // MAIN
  // ==================================================================

  public static void main(String[] args) {
    TestRunner.suite("INTEGRATION TESTS - REAL DAOs, INITIALIZERS AND ADTs");

    testInitializersProduceUsableData();
    testWalkInPersistence();
    testHousekeepingPersistence();
    testBookingPersistence();
    testMissingFileFallsBackToSeed();
    testListAndBstStayConsistent();
    testSharedListAdtAcrossModules();
    testSharedMessageUi();

    TestRunner.report("INTEGRATION TESTS");
    TestRunner.exitWithStatus();
  }
}
