package test;

import adt.ArrayList;
import adt.ArrayQueue;
import adt.ArrayStack;
import adt.BinarySearchTree;
import adt.DualLaneQueue;
import adt.DualLaneQueueInterface;
import adt.HashMap;
import adt.ListInterface;
import adt.MapInterface;
import adt.QueueInterface;
import adt.StackInterface;
import adt.TreeInterface;
import entity.Booking;
import entity.HousekeepingTask;
import entity.Invoice;
import entity.Member;
import entity.Payment;
import entity.Room;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import utility.IdGenerator;

/**
 * Unit tests - each ADT and entity checked on its own, with no file access and
 * no other class involved.
 *
 * The cases chosen are the ones where a mistake would be silent: the boundary
 * where a rule flips over, the empty collection, the value that is one either
 * side of a threshold. Checking that a list can hold three things proves very
 * little; checking that a tier is worked out from lifetime points rather than
 * the spendable balance proves something that could otherwise go wrong for
 * months without anybody noticing.
 *
 * @author Tan Chee Yan
 */
public class UnitTest {

  private final TestRunner runner = new TestRunner();

  public static void main(String[] args) {
    UnitTest tests = new UnitTest();
    boolean allPassed = tests.runAll();
    System.exit(allPassed ? 0 : 1);
  }

  public boolean runAll() {
    System.out.println();
    System.out.println("=".repeat(76));
    System.out.println("  UNIT TESTS - individual ADTs and entities");
    System.out.println("=".repeat(76));

    testArrayList();
    testArrayQueue();
    testArrayStack();
    testDualLaneQueue();
    testHashMap();
    testBinarySearchTree();
    testIdGenerator();
    testRoom();
    testBooking();
    testInvoice();
    testMember();
    testHousekeepingTask();
    testWalkInRegistration();
    testPayment();

    return runner.report("UNIT TEST SUMMARY");
  }

  // ==================================================================
  // ADTs
  // ==================================================================

  private void testArrayList() {
    runner.suite("ArrayList");

    ListInterface<String> list = new ArrayList<>();
    runner.check("a new list is empty", list.isEmpty());
    runner.checkEquals("a new list has no entries", 0, list.getNumberOfEntries());

    list.add("alpha");
    list.add("beta");
    list.add("gamma");
    runner.checkEquals("three entries after three adds", 3, list.getNumberOfEntries());
    runner.checkEquals("positions are 1-based", "alpha", list.getEntry(1));
    runner.checkEquals("the last entry is at getNumberOfEntries()",
        "gamma", list.getEntry(3));

    // Reading outside the list must give nothing rather than throw, because
    // the callers use a null return to mean "not there".
    runner.checkEquals("position 0 returns null", null, list.getEntry(0));
    runner.checkEquals("a position past the end returns null", null, list.getEntry(4));

    list.add(2, "inserted");
    runner.checkEquals("inserting shifts the rest up", "inserted", list.getEntry(2));
    runner.checkEquals("beta moved to position 3", "beta", list.getEntry(3));

    runner.check("contains finds an entry", list.contains("gamma"));
    runner.check("contains rejects one that is absent", !list.contains("delta"));
    runner.checkEquals("getPosition is 1-based", 1, list.getPosition("alpha"));
    runner.checkEquals("getPosition returns -1 when absent", -1, list.getPosition("delta"));

    list.removeEntry("inserted");
    runner.checkEquals("removeEntry shortens the list", 3, list.getNumberOfEntries());
    runner.checkEquals("the gap closes up", "beta", list.getEntry(2));

    ListInterface<String> matching = list.filter(entry -> entry.startsWith("b"));
    runner.checkEquals("filter returns only what matches", 1,
        matching.getNumberOfEntries());
    runner.checkEquals("the original is untouched by filter", 3,
        list.getNumberOfEntries());

    runner.checkEquals("search finds the first match", "beta",
        list.search(entry -> entry.length() == 4));
    runner.checkEquals("search returns null when nothing matches", null,
        list.search(entry -> entry.isEmpty()));
    runner.checkEquals("countIf counts every match", 3,
        list.countIf(entry -> entry.contains("a")));
    runner.checkEquals("countIf counts a narrower match", 1,
        list.countIf(entry -> entry.startsWith("g")));
    runner.checkEquals("countIf returns zero when nothing matches", 0,
        list.countIf(entry -> entry.startsWith("z")));

    list.sort(Comparator.reverseOrder());
    runner.checkEquals("sort reorders in place", "gamma", list.getEntry(1));

    list.clear();
    runner.check("clear empties the list", list.isEmpty());
  }

  private void testArrayQueue() {
    runner.suite("ArrayQueue");

    QueueInterface<String> queue = new ArrayQueue<>();
    runner.check("a new queue is empty", queue.isEmpty());
    runner.checkEquals("dequeue on an empty queue returns null", null, queue.dequeue());
    runner.checkEquals("getFront on an empty queue returns null", null, queue.getFront());

    queue.enqueue("first");
    queue.enqueue("second");
    queue.enqueue("third");
    runner.checkEquals("three entries after three enqueues", 3,
        queue.getNumberOfEntries());

    runner.checkEquals("getFront shows the front without removing it",
        "first", queue.getFront());
    runner.checkEquals("still three after getFront", 3, queue.getNumberOfEntries());

    runner.checkEquals("dequeue takes from the front", "first", queue.dequeue());
    runner.checkEquals("dequeue is first in, first out", "second", queue.dequeue());
    runner.checkEquals("one left", 1, queue.getNumberOfEntries());
  }

  private void testArrayStack() {
    runner.suite("ArrayStack");

    StackInterface<String> stack = new ArrayStack<>();
    runner.check("a new stack is empty", stack.isEmpty());
    runner.checkEquals("pop on an empty stack returns null", null, stack.pop());

    stack.push("oldest");
    stack.push("middle");
    stack.push("newest");

    runner.checkEquals("peek shows the top without removing it", "newest", stack.peek());
    runner.checkEquals("still three after peek", 3, stack.getNumberOfEntries());

    runner.checkEquals("pop takes the newest", "newest", stack.pop());
    runner.checkEquals("pop is last in, first out", "middle", stack.pop());
    runner.checkEquals("one left", 1, stack.getNumberOfEntries());
  }

  /**
   * The lane rules are the heart of the whole system's fairness, so they are
   * tested harder than anything else here.
   */
  private void testDualLaneQueue() {
    runner.suite("DualLaneQueue - the urgent and normal lanes");

    DualLaneQueueInterface<String> queue = new DualLaneQueue<>();
    runner.check("a new queue is empty", queue.isEmpty());
    runner.checkEquals("next() on an empty queue returns null", null, queue.next());

    queue.enqueue("normal-1", DualLaneQueueInterface.NORMAL);
    queue.enqueue("normal-2", DualLaneQueueInterface.NORMAL);
    queue.enqueue("urgent-1", DualLaneQueueInterface.URGENT);
    queue.enqueue("normal-3", DualLaneQueueInterface.NORMAL);
    queue.enqueue("urgent-2", DualLaneQueueInterface.URGENT);

    runner.checkEquals("the urgent lane holds two", 2, queue.getUrgentCount());
    runner.checkEquals("the normal lane holds three", 3, queue.getNormalCount());
    runner.checkEquals("five altogether", 5, queue.getNumberOfEntries());

    // The rule that matters: urgent-1 arrived third but is served first,
    // and urgent-2 arrived last but is served second.
    runner.checkEquals("the urgent lane goes first even though it arrived later",
        "urgent-1", queue.next());
    runner.checkEquals("the whole urgent lane is drained before any normal",
        "urgent-2", queue.next());
    runner.checkEquals("then the normal lane, in arrival order",
        "normal-1", queue.next());
    runner.checkEquals("still in arrival order within the lane",
        "normal-2", queue.next());

    // Within a lane it is strictly first come first served - nothing is
    // promoted for having waited.
    queue.enqueue("urgent-3", DualLaneQueueInterface.URGENT);
    runner.checkEquals("a new urgent entry still overtakes a waiting normal one",
        "urgent-3", queue.peekNext());

    runner.check("removeEntry finds an entry in the normal lane",
        queue.removeEntry("normal-3"));
    runner.check("removeEntry returns false for something absent",
        !queue.removeEntry("never-added"));

    queue.clear();
    runner.check("clear empties both lanes", queue.isEmpty());

    // Anything not exactly URGENT must be treated as normal, so a typo can
    // never grant a queue jump by accident.
    DualLaneQueueInterface<String> guard = new DualLaneQueue<>();
    guard.enqueue("typo", "URGNET");
    guard.enqueue("blank", "");
    guard.enqueue("nothing", null);
    runner.checkEquals("a misspelled priority does not reach the urgent lane",
        0, guard.getUrgentCount());
    runner.checkEquals("it goes to the normal lane instead", 3, guard.getNormalCount());

    // Listing the queue must not disturb it.
    DualLaneQueueInterface<String> order = new DualLaneQueue<>();
    order.enqueue("n1", DualLaneQueueInterface.NORMAL);
    order.enqueue("u1", DualLaneQueueInterface.URGENT);
    order.enqueue("n2", DualLaneQueueInterface.NORMAL);

    ListInterface<String> listed = order.toServiceOrder();
    runner.checkEquals("toServiceOrder lists everything", 3, listed.getNumberOfEntries());
    runner.checkEquals("urgent comes first in the listing", "u1", listed.getEntry(1));
    runner.checkEquals("then normal, in arrival order", "n1", listed.getEntry(2));
    runner.checkEquals("the queue is left untouched by listing it",
        3, order.getNumberOfEntries());
    runner.checkEquals("and still returns the same entry next", "u1", order.next());
  }

  private void testHashMap() {
    runner.suite("HashMap");

    MapInterface<String, String> map = new HashMap<>();
    runner.check("a new map is empty", map.isEmpty());
    runner.checkEquals("get on a missing key returns null", null, map.get("absent"));
    runner.check("containsKey is false for a missing key", !map.containsKey("absent"));

    map.put("1001", "Standard Twin");
    map.put("1002", "Deluxe King");
    runner.checkEquals("two entries", 2, map.getNumberOfEntries());
    runner.checkEquals("get returns what was put", "Standard Twin", map.get("1001"));
    runner.check("containsKey is true for a stored key", map.containsKey("1002"));

    String replaced = map.put("1001", "Family Suite");
    runner.checkEquals("putting an existing key returns the old value",
        "Standard Twin", replaced);
    runner.checkEquals("the value is replaced", "Family Suite", map.get("1001"));
    runner.checkEquals("replacing does not add an entry", 2, map.getNumberOfEntries());

    runner.checkEquals("remove returns the value removed", "Deluxe King",
        map.remove("1002"));
    runner.checkEquals("one entry left", 1, map.getNumberOfEntries());
    runner.checkEquals("remove on a missing key returns null", null, map.remove("9999"));

    // Enough entries to force at least one rehash, since a bucket position
    // depends on the table size and everything must survive being moved.
    MapInterface<String, Integer> big = new HashMap<>();
    for (int i = 1; i <= 200; i++) {
      big.put("key" + i, i);
    }
    runner.checkEquals("200 entries survive rehashing", 200, big.getNumberOfEntries());
    runner.checkEquals("an early entry is still findable", Integer.valueOf(1),
        big.get("key1"));
    runner.checkEquals("a late entry is findable", Integer.valueOf(200),
        big.get("key200"));
    runner.checkEquals("getKeys returns every key", 200,
        big.getKeys().getNumberOfEntries());
    runner.checkEquals("getValues returns every value", 200,
        big.getValues().getNumberOfEntries());
  }

  private void testBinarySearchTree() {
    runner.suite("BinarySearchTree");

    TreeInterface<String, String> tree = new BinarySearchTree<>();
    runner.check("a new tree is empty", tree.isEmpty());
    runner.checkEquals("search on an empty tree returns null", null, tree.search("BK0001"));

    tree.add("BK0003", "third");
    tree.add("BK0001", "first");
    tree.add("BK0005", "fifth");
    tree.add("BK0002", "second");
    tree.add("BK0004", "fourth");

    runner.checkEquals("five entries", 5, tree.getNumberOfEntries());
    runner.checkEquals("search finds a value by key", "first", tree.search("BK0001"));
    runner.check("contains is true for a stored key", tree.contains("BK0004"));
    runner.check("contains is false for a missing key", !tree.contains("BK0099"));

    // The point of using a tree: the sorted listing costs nothing extra.
    ListInterface<String> inOrder = tree.getAllInOrder();
    runner.checkEquals("an in-order walk gives everything", 5,
        inOrder.getNumberOfEntries());
    runner.checkEquals("sorted, first", "first", inOrder.getEntry(1));
    runner.checkEquals("sorted, second", "second", inOrder.getEntry(2));
    runner.checkEquals("sorted, last", "fifth", inOrder.getEntry(5));

    String replaced = tree.add("BK0001", "replaced");
    runner.checkEquals("adding an existing key returns the old value",
        "first", replaced);
    runner.checkEquals("the value is replaced", "replaced", tree.search("BK0001"));
    runner.checkEquals("replacing does not add an entry", 5, tree.getNumberOfEntries());

    // Removing a node with two children is the case that can corrupt the
    // ordering if the successor is chosen wrongly.
    tree.remove("BK0003");
    runner.checkEquals("four left after removing a two-child node",
        4, tree.getNumberOfEntries());
    runner.check("the removed key is gone", !tree.contains("BK0003"));

    ListInterface<String> afterRemoval = tree.getAllInOrder();
    runner.checkEquals("everything else survives", 4, afterRemoval.getNumberOfEntries());
    runner.checkEquals("the order is still correct after removal",
        "replaced", afterRemoval.getEntry(1));
    runner.checkEquals("and at the end", "fifth", afterRemoval.getEntry(4));
  }

  private void testIdGenerator() {
    runner.suite("IdGenerator");

    ListInterface<String> ids = new ArrayList<>();
    runner.checkEquals("the first ID in an empty series", "BK0001",
        IdGenerator.next("BK", 4, ids));

    ids.add("BK0001");
    ids.add("BK0002");
    ids.add("BK0003");
    runner.checkEquals("the next ID follows the highest", "BK0004",
        IdGenerator.next("BK", 4, ids));

    // The whole reason for taking the highest rather than counting: after a
    // deletion, counting would reissue an ID that is already in use.
    ids.removeEntry("BK0002");
    runner.checkEquals("a deletion does not cause an ID to be reused",
        "BK0004", IdGenerator.next("BK", 4, ids));

    ids.add("BK9999");
    runner.checkEquals("the highest wins wherever it sits", "BK10000",
        IdGenerator.next("BK", 4, ids));

    // A malformed ID must be skipped rather than stopping new IDs entirely.
    ListInterface<String> messy = new ArrayList<>();
    messy.add("BK0007");
    messy.add("BKxxxx");
    messy.add("OTHER1");
    messy.add(null);
    runner.checkEquals("malformed and foreign IDs are ignored", "BK0008",
        IdGenerator.next("BK", 4, messy));

    runner.checkEquals("format pads with leading zeros", "HK0042",
        IdGenerator.format("HK", 4, 42));
    runner.checkEquals("a three-digit series", "RW007",
        IdGenerator.format("RW", 3, 7));
  }

  // ==================================================================
  // ENTITIES
  // ==================================================================

  /**
   * A room is sellable only when three separate things are true, and the two
   * statuses are owned by two different modules - so this is exactly where the
   * old system let a guest into a dirty room.
   */
  private void testRoom() {
    runner.suite("Room - the assignable rule");

    Room ready = new Room("1005", "RT03", 10, Room.VACANT,
        Room.READY_FOR_CHECK_IN, false, LocalDateTime.now(), "");
    runner.check("vacant, cleaned and in service is assignable", ready.isAssignable());

    Room dirty = new Room("1001", "RT01", 10, Room.VACANT,
        Room.DIRTY, false, LocalDateTime.now(), "");
    runner.check("a vacant but dirty room is NOT assignable", !dirty.isAssignable());
    runner.check("but it can be made ready by cleaning it", dirty.isCleanable());

    Room occupied = new Room("1002", "RT01", 10, Room.OCCUPIED,
        Room.READY_FOR_CHECK_IN, false, LocalDateTime.now(), "");
    runner.check("a cleaned but occupied room is NOT assignable",
        !occupied.isAssignable());
    runner.check("and it cannot be cleaned for somebody either",
        !occupied.isCleanable());

    Room outOfService = new Room("3005", "RT03", 30, Room.VACANT,
        Room.READY_FOR_CHECK_IN, true, LocalDateTime.now(), "");
    runner.check("out of service beats everything else", !outOfService.isAssignable());
    runner.check("and cannot be cleaned into use", !outOfService.isCleanable());

    Room inspected = new Room("2004", "RT02", 20, Room.VACANT,
        Room.INSPECTED, false, LocalDateTime.now(), "");
    runner.check("inspected is not yet ready to sell", !inspected.isAssignable());
    runner.check("but it is cleanable", inspected.isCleanable());

    // Closest to ready first, so the front desk offers the room that will
    // free up soonest.
    runner.checkEquals("inspected ranks closest to ready", 0, inspected.getReadinessRank());
    runner.checkEquals("dirty ranks furthest", 2, dirty.getReadinessRank());
    runner.check("inspected sorts before dirty",
        inspected.getReadinessRank() < dirty.getReadinessRank());
  }

  private void testBooking() {
    runner.suite("Booking - nights and date overlap");

    LocalDate first = LocalDate.of(2026, 9, 1);
    LocalDate fourth = LocalDate.of(2026, 9, 4);

    runner.checkEquals("1st to 4th is three nights", 3,
        Booking.calculateNights(first, fourth));
    runner.checkEquals("the same day is no nights", 0,
        Booking.calculateNights(first, first));
    runner.checkEquals("a null date gives zero nights", 0,
        Booking.calculateNights(null, fourth));

    Booking booking = new Booking("BK0001", "G0001", "RT01", first, fourth, 2,
        Booking.PRIORITY_NORMAL, Booking.SOURCE_ONLINE, null, 150.00,
        LocalDateTime.now(), "ST001");

    runner.checkEquals("nights are worked out on construction", 3,
        booking.getNumberOfNights());
    runner.checkEquals("a new booking starts PENDING", Booking.STATUS_PENDING,
        booking.getBookingStatus());
    runner.checkEquals("with no room", null, booking.getRoomNo());
    booking.setConfirmationNumber("12345678");
    runner.checkEquals("stores an eight-digit confirmation number", "12345678",
        booking.getConfirmationNumber());

    runner.check("a stay inside the range overlaps",
        booking.overlaps(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 3)));
    runner.check("a stay straddling the start overlaps",
        booking.overlaps(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 2)));
    runner.check("a stay straddling the end overlaps",
        booking.overlaps(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 6)));
    runner.check("an identical stay overlaps", booking.overlaps(first, fourth));

    // Same-day turnover is how a hotel actually works: one guest leaves on
    // the 4th and another arrives the same day.
    runner.check("arriving the day the last guest leaves is NOT an overlap",
        !booking.overlaps(fourth, LocalDate.of(2026, 9, 7)));
    runner.check("leaving the day the next guest arrives is NOT an overlap",
        !booking.overlaps(LocalDate.of(2026, 8, 28), first));
    runner.check("a stay entirely afterwards does not overlap",
        !booking.overlaps(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12)));

    // Only a live booking blocks its room, or a cancelled one would make the
    // room unsellable forever.
    booking.setBookingStatus(Booking.STATUS_CONFIRMED);
    runner.check("a confirmed booking holds its room", booking.holdsRoom());
    booking.setBookingStatus(Booking.STATUS_CHECKED_IN);
    runner.check("a checked-in booking holds its room", booking.holdsRoom());
    booking.setBookingStatus(Booking.STATUS_CANCELLED);
    runner.check("a cancelled booking does NOT hold its room", !booking.holdsRoom());
    booking.setBookingStatus(Booking.STATUS_CHECKED_OUT);
    runner.check("a checked-out booking does NOT hold its room", !booking.holdsRoom());
  }

  private void testInvoice() {
    runner.suite("Invoice - the bill");

    // 300 room + 30 service (10%) + 19.80 tax (6% of 330) = 349.80
    Invoice invoice = new Invoice("INV0001", "BK0001", 300.00, LocalDateTime.now());

    runner.checkAmount("service charge is 10% of the room charge",
        30.00, invoice.getServiceCharge());
    runner.checkAmount("tax is 6% of room plus service",
        19.80, invoice.getTaxAmount());
    runner.checkAmount("the total is room, service and tax",
        349.80, invoice.getTotalAmount());
    runner.checkEquals("a new bill is unpaid", Invoice.UNPAID, invoice.getPaymentStatus());
    runner.checkAmount("everything is outstanding", 349.80,
        invoice.getOutstandingBalance());
    runner.check("and it is not settled", !invoice.isSettled());

    invoice.setAmountPaid(150.00);
    runner.checkEquals("part payment makes it PARTIAL", Invoice.PARTIAL,
        invoice.getPaymentStatus());
    runner.checkAmount("the balance drops", 199.80, invoice.getOutstandingBalance());
    runner.check("still not settled", !invoice.isSettled());

    invoice.setAmountPaid(349.80);
    runner.checkEquals("paying in full makes it PAID", Invoice.PAID,
        invoice.getPaymentStatus());
    runner.checkAmount("nothing outstanding", 0.00, invoice.getOutstandingBalance());
    runner.check("and it is settled", invoice.isSettled());

    // A discount changes the total, so what was a settled bill becomes
    // overpaid rather than staying exactly equal.
    Invoice discounted = new Invoice("INV0002", "BK0002", 300.00, LocalDateTime.now());
    discounted.setDiscountAmount(50.00);
    runner.checkAmount("a discount comes off the total", 299.80,
        discounted.getTotalAmount());
    runner.checkAmount("the charges themselves are unchanged", 30.00,
        discounted.getServiceCharge());

    // A discount bigger than the bill must not produce a negative total.
    Invoice overDiscounted = new Invoice("INV0003", "BK0003", 100.00,
        LocalDateTime.now());
    overDiscounted.setDiscountAmount(1000.00);
    runner.checkAmount("the total floors at zero", 0.00,
        overDiscounted.getTotalAmount());
    runner.check("and a zero bill counts as settled", overDiscounted.isSettled());

    // Repeated rounded payments can land a fraction of a sen away from the
    // total, which must not leave a settled bill showing as PARTIAL.
    Invoice rounding = new Invoice("INV0004", "BK0004", 33.33, LocalDateTime.now());
    rounding.setAmountPaid(rounding.getTotalAmount());
    runner.checkEquals("a bill paid to the sen is PAID", Invoice.PAID,
        rounding.getPaymentStatus());
  }

  /**
   * The tier rules decide what a member is entitled to, so the boundaries are
   * checked on both sides.
   */
  private void testMember() {
    runner.suite("Member - tiers and points");

    runner.checkEquals("0 points is Silver", Member.SILVER, Member.tierFor(0));
    runner.checkEquals("999 is still Silver", Member.SILVER, Member.tierFor(999));
    runner.checkEquals("1000 is Gold", Member.GOLD, Member.tierFor(1000));
    runner.checkEquals("4999 is still Gold", Member.GOLD, Member.tierFor(4999));
    runner.checkEquals("5000 is Platinum", Member.PLATINUM, Member.tierFor(5000));
    runner.checkEquals("14999 is still Platinum", Member.PLATINUM, Member.tierFor(14999));
    runner.checkEquals("15000 is Diamond", Member.DIAMOND, Member.tierFor(15000));

    runner.checkEquals("Silver earns at the base rate", 1.00,
        Member.multiplierFor(Member.SILVER));
    runner.checkEquals("Gold earns more", 1.25, Member.multiplierFor(Member.GOLD));
    runner.checkEquals("Platinum more again", 1.50,
        Member.multiplierFor(Member.PLATINUM));
    runner.checkEquals("Diamond earns double", 2.00,
        Member.multiplierFor(Member.DIAMOND));

    runner.check("Gold outranks Silver",
        Member.tierRank(Member.GOLD) > Member.tierRank(Member.SILVER));
    runner.check("Diamond outranks Platinum",
        Member.tierRank(Member.DIAMOND) > Member.tierRank(Member.PLATINUM));

    Member member = new Member("L0001", "G0001", LocalDate.now().minusMonths(6));
    runner.checkEquals("a new member starts Silver", Member.SILVER, member.getTier());
    runner.checkEquals("with no points", 0, member.getPointsBalance());
    runner.checkEquals("and no lifetime points", 0, member.getLifetimePoints());

    member.setLifetimePoints(1200);
    runner.check("refreshTier reports a change", member.refreshTier());
    runner.checkEquals("and moves them up", Member.GOLD, member.getTier());
    runner.check("refreshing again reports no change", !member.refreshTier());

    // The rule that matters most: spending points must never cost a member
    // the tier they earned.
    member.setPointsBalance(0);
    member.refreshTier();
    runner.checkEquals("spending every point does not demote them",
        Member.GOLD, member.getTier());
    runner.checkEquals("because the tier comes from lifetime points",
        1200, member.getLifetimePoints());

    member.setLifetimePoints(4500);
    member.refreshTier();
    runner.checkEquals("Platinum is the next tier up from Gold",
        Member.PLATINUM, member.getNextTier());
    runner.checkEquals("and they are 500 points short", 500,
        member.getPointsToNextTier());

    member.setLifetimePoints(20000);
    member.refreshTier();
    runner.checkEquals("a Diamond member has no next tier", null, member.getNextTier());
    runner.checkEquals("and nothing to earn towards it", 0,
        member.getPointsToNextTier());

    // A balance cannot go below zero however it is set.
    member.setPointsBalance(-500);
    runner.checkEquals("a negative balance is floored at zero", 0,
        member.getPointsBalance());

    LocalDate today = LocalDate.now();
    Member expiring = new Member("L0002", "G0002", Member.SILVER, 500, 500,
        today.plusDays(10), today.minusMonths(3), Member.ACTIVE);
    runner.check("points expiring in 10 days are expiring soon",
        expiring.hasExpiringPoints(today));

    Member notExpiring = new Member("L0003", "G0003", Member.SILVER, 500, 500,
        today.plusDays(90), today.minusMonths(3), Member.ACTIVE);
    runner.check("points expiring in 90 days are not",
        !notExpiring.hasExpiringPoints(today));

    // A member with nothing to lose should not be warned.
    Member noPoints = new Member("L0004", "G0004", Member.SILVER, 0, 0,
        today.plusDays(10), today.minusMonths(3), Member.ACTIVE);
    runner.check("a member with no points is not warned about expiry",
        !noPoints.hasExpiringPoints(today));
  }

  /**
   * The cleaning workflow is what stops a room being sold before it is clean,
   * so every refused step is checked as carefully as every allowed one.
   */
  private void testHousekeepingTask() {
    runner.suite("HousekeepingTask - the cleaning workflow");

    runner.check("dirty to cleaning is allowed",
        HousekeepingTask.isValidTransition(
            HousekeepingTask.DIRTY, HousekeepingTask.CLEANING_IN_PROGRESS));
    runner.check("cleaning to inspected is allowed",
        HousekeepingTask.isValidTransition(
            HousekeepingTask.CLEANING_IN_PROGRESS, HousekeepingTask.INSPECTED));
    runner.check("inspected to ready is allowed",
        HousekeepingTask.isValidTransition(
            HousekeepingTask.INSPECTED, HousekeepingTask.READY_FOR_CHECK_IN));
    runner.check("a failed inspection sends it back to dirty",
        HousekeepingTask.isValidTransition(
            HousekeepingTask.INSPECTED, HousekeepingTask.DIRTY));
    runner.check("a fresh check-out dirties a ready room",
        HousekeepingTask.isValidTransition(
            HousekeepingTask.READY_FOR_CHECK_IN, HousekeepingTask.DIRTY));

    // The refusals are the point: each of these would let a guest into a room
    // that has not actually been cleaned or checked.
    runner.check("a room cannot be inspected without being cleaned",
        !HousekeepingTask.isValidTransition(
            HousekeepingTask.DIRTY, HousekeepingTask.INSPECTED));
    runner.check("a dirty room cannot jump straight to ready",
        !HousekeepingTask.isValidTransition(
            HousekeepingTask.DIRTY, HousekeepingTask.READY_FOR_CHECK_IN));
    runner.check("inspection cannot be skipped",
        !HousekeepingTask.isValidTransition(
            HousekeepingTask.CLEANING_IN_PROGRESS, HousekeepingTask.READY_FOR_CHECK_IN));
    runner.check("a ready room cannot move backwards to inspected",
        !HousekeepingTask.isValidTransition(
            HousekeepingTask.READY_FOR_CHECK_IN, HousekeepingTask.INSPECTED));
    runner.check("the same status twice is refused as a duplicate",
        !HousekeepingTask.isValidTransition(
            HousekeepingTask.DIRTY, HousekeepingTask.DIRTY));
    runner.check("a null status is refused",
        !HousekeepingTask.isValidTransition(null, HousekeepingTask.DIRTY));

    runner.check("any status can be blocked",
        HousekeepingTask.isValidTransition(
            HousekeepingTask.CLEANING_IN_PROGRESS, HousekeepingTask.BLOCKED));
    runner.check("a blocked room can go back to being cleaned",
        HousekeepingTask.isValidTransition(
            HousekeepingTask.BLOCKED, HousekeepingTask.CLEANING_IN_PROGRESS));
    runner.check("a blocked room cannot skip to inspected",
        !HousekeepingTask.isValidTransition(
            HousekeepingTask.BLOCKED, HousekeepingTask.INSPECTED));
    runner.check("a blocked room cannot skip to ready",
        !HousekeepingTask.isValidTransition(
            HousekeepingTask.BLOCKED, HousekeepingTask.READY_FOR_CHECK_IN));

    HousekeepingTask task = new HousekeepingTask("HK0001", "1001",
        HousekeepingTask.TYPE_CHECKOUT_CLEAN, "BK0001", LocalDateTime.now());

    runner.checkEquals("a new task starts dirty", HousekeepingTask.DIRTY,
        task.getStatus());
    runner.checkEquals("and in the normal lane", HousekeepingTask.PRIORITY_NORMAL,
        task.getPriority());
    runner.check("a dirty task is waiting to be picked up", task.isPendingCleaning());
    runner.check("and it is outstanding cleaning", task.isOutstandingCleaning());
    task.setStatus(HousekeepingTask.CLEANING_IN_PROGRESS);
    runner.check("work in progress is still outstanding", task.isOutstandingCleaning());
    runner.check("but it is no longer waiting in the queue", !task.isPendingCleaning());
    task.setStatus(HousekeepingTask.INSPECTED);
    runner.check("inspected is not outstanding cleaning", !task.isOutstandingCleaning());
    task.setStatus(HousekeepingTask.READY_FOR_CHECK_IN);
    runner.check("a finished clean is not outstanding", !task.isOutstandingCleaning());
    task.setStatus(HousekeepingTask.DIRTY);
    task.setTaskType(HousekeepingTask.TYPE_INSPECTION);
    runner.check("an inspection job is not outstanding cleaning",
        !task.isOutstandingCleaning());
    task.setTaskType(HousekeepingTask.TYPE_MAINTENANCE);
    runner.check("maintenance is not outstanding cleaning", !task.isOutstandingCleaning());
    runner.check("and a maintenance job is never waiting in the cleaning queue",
        !task.isPendingCleaning());
    runner.check("maintenance cannot start cleaning from DIRTY",
        !HousekeepingTask.isValidTransition(HousekeepingTask.TYPE_MAINTENANCE,
            HousekeepingTask.DIRTY, HousekeepingTask.CLEANING_IN_PROGRESS));
    runner.check("maintenance can be marked BLOCKED",
        HousekeepingTask.isValidTransition(HousekeepingTask.TYPE_MAINTENANCE,
            HousekeepingTask.DIRTY, HousekeepingTask.BLOCKED));
    runner.check("resolved maintenance returns the room to DIRTY",
        HousekeepingTask.isValidTransition(HousekeepingTask.TYPE_MAINTENANCE,
            HousekeepingTask.BLOCKED, HousekeepingTask.DIRTY));
    runner.check("maintenance cannot resume as CLEANING_IN_PROGRESS",
        !HousekeepingTask.isValidTransition(HousekeepingTask.TYPE_MAINTENANCE,
            HousekeepingTask.BLOCKED, HousekeepingTask.CLEANING_IN_PROGRESS));

    String[] maintenanceChoices = HousekeepingTask.allowedNextStatuses(
        HousekeepingTask.TYPE_MAINTENANCE, HousekeepingTask.DIRTY);
    boolean offeredCleaning = false;
    boolean offeredBlocked = false;
    for (int i = 0; i < maintenanceChoices.length; i++) {
      if (HousekeepingTask.CLEANING_IN_PROGRESS.equals(maintenanceChoices[i])) {
        offeredCleaning = true;
      }
      if (HousekeepingTask.BLOCKED.equals(maintenanceChoices[i])) {
        offeredBlocked = true;
      }
    }
    runner.check("the UI does not offer CLEANING_IN_PROGRESS for MAINTENANCE",
        !offeredCleaning);
    runner.check("the UI does offer BLOCKED for MAINTENANCE", offeredBlocked);

    runner.check("an inspection job is not started from the cleaning queue",
        !HousekeepingTask.isValidTransition(HousekeepingTask.TYPE_INSPECTION,
            HousekeepingTask.DIRTY, HousekeepingTask.CLEANING_IN_PROGRESS));

    task.setTaskType(HousekeepingTask.TYPE_CHECKOUT_CLEAN);
    runner.checkEquals("an unfinished task has no duration", -1L,
        task.getCleaningDurationMinutes());

    LocalDateTime started = LocalDateTime.now().minusMinutes(45);
    task.setStartedAt(started);
    task.setCompletedAt(started.plusMinutes(45));
    runner.checkEquals("a finished task reports how long it took", 45L,
        task.getCleaningDurationMinutes());

    task.incrementInspectionFailCount();
    task.incrementInspectionFailCount();
    runner.checkEquals("failures are counted", 2, task.getInspectionFailCount());

    // Undoing a failure must take it back out of the count, or the success
    // rate would keep punishing a mistake that has been reversed.
    task.decrementInspectionFailCount();
    runner.checkEquals("a rollback takes one back off", 1,
        task.getInspectionFailCount());
    task.decrementInspectionFailCount();
    task.decrementInspectionFailCount();
    runner.checkEquals("the count cannot go negative", 0,
        task.getInspectionFailCount());
  }

  private void testWalkInRegistration() {
    runner.suite("WalkInRegistration - waiting time");

    LocalDateTime arrived = LocalDateTime.now().minusMinutes(45);
    WalkInRegistration reg = new WalkInRegistration("WR0001", "G0001", arrived,
        WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 2);

    runner.checkEquals("a new registration is waiting",
        WalkInRegistration.STATUS_WAITING, reg.getStatus());
    runner.check("and isWaiting agrees", reg.isWaiting());
    runner.check("a normal registration is not urgent", !reg.isUrgent());

    // A guest still waiting has a wait that keeps growing.
    runner.check("the wait so far is about 45 minutes",
        reg.getWaitingMinutes() >= 45 && reg.getWaitingMinutes() <= 46);
    runner.checkEquals("shown in minutes under an hour", "45m",
        reg.getFormattedWaitingTime());

    // Once called, the wait is fixed at the moment they were called.
    reg.setCalledAt(arrived.plusMinutes(20));
    reg.setStatus(WalkInRegistration.STATUS_IN_SERVICE);
    runner.checkEquals("a called guest's wait stops at the call", 20L,
        reg.getWaitingMinutes());
    runner.check("and they are no longer waiting", !reg.isWaiting());

    reg.setCalledAt(arrived.plusMinutes(125));
    runner.checkEquals("over an hour is shown as hours and minutes", "2h 05m",
        reg.getFormattedWaitingTime());

    WalkInRegistration urgent = new WalkInRegistration("WR0002", "G0002",
        LocalDateTime.now(), WalkInRegistration.PRIORITY_URGENT,
        "Travelling with infant", "RT02", 1);
    runner.check("an urgent registration is urgent", urgent.isUrgent());
    runner.checkEquals("and carries its reason", "Travelling with infant",
        urgent.getUrgencyReason());

    WalkInRegistration noTime = new WalkInRegistration();
    runner.checkEquals("no arrival time means no wait", 0L, noTime.getWaitingMinutes());
    runner.checkEquals("shown as a dash", "-", noTime.getFormattedWaitingTime());
    runner.checkEquals("and so is the arrival", "-", noTime.getFormattedArrivalTime());
  }

  private void testPayment() {
    runner.suite("Payment");

    runner.check("a card payment needs a reference",
        Payment.requiresReference(Payment.CARD));
    runner.check("an e-wallet payment needs a reference",
        Payment.requiresReference(Payment.EWALLET));
    runner.check("a bank transfer needs a reference",
        Payment.requiresReference(Payment.BANK_TRANSFER));

    // Cash leaves no trace of its own, so there is nothing to record.
    runner.check("cash does not need a reference",
        !Payment.requiresReference(Payment.CASH));

    Payment payment = new Payment("PY0001", "INV0001", 200.00, Payment.CARD,
        "APPR-884213", LocalDateTime.now(), "ST002");
    runner.checkAmount("the amount is kept", 200.00, payment.getAmount());
    runner.checkEquals("and the reference", "APPR-884213", payment.getReference());
    runner.checkEquals("and who took it", "ST002", payment.getReceivedBy());
  }
}
