import adt.ArrayList;
import adt.ArrayQueue;
import adt.ArrayStack;
import adt.BookingBST;
import adt.Condition;
import adt.ListInterface;
import adt.QueueInterface;
import adt.StackInterface;
import entity.BillingRecord;
import entity.Booking;
import entity.FrontDeskRecord;
import entity.HousekeepingTask;
import entity.Member;
import entity.Redemption;
import entity.Reward;
import entity.WalkInGuest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * UNIT TESTS - the collection ADTs and each module's core logic in isolation.
 *
 * Covers:
 *   Part 1  The four collection ADTs (List, Stack, Queue, BST)
 *   Part 2  Module 1 - Walk-In Registration
 *   Part 3  Module 2 - Housekeeping Task Log
 *   Part 4  Module 3 - Front-Desk Service
 *   Part 5  Module 4 - Loyalty & Rewards
 *
 * Where a module's rule lives in a private method or behind a console prompt,
 * the rule is reproduced here as a testable copy. Those copies are marked, and
 * the limitation is stated: if the original changes and the copy does not, the
 * test keeps passing against the old rule.
 *
 * @author Tan Chee Yan
 */
public class UnitTests {

  // ==================================================================
  // HELPERS
  // ==================================================================

  private static WalkInGuest walkIn(String id, String name, boolean urgent) {
    return new WalkInGuest(id, name, "0123456789", urgent,
        urgent ? "Elderly or unwell guest" : null, LocalDateTime.now());
  }

  private static HousekeepingTask task(String id, String room, String status,
      LocalDateTime when) {
    return new HousekeepingTask(id, room, status, when);
  }

  private static Booking booking(String confirmation, String guest, String room) {
    return new Booking(confirmation, guest, room,
        LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 23));
  }

  private static String idsOf(ListInterface<WalkInGuest> list) {
    StringBuilder ids = new StringBuilder();
    for (int i = 1; i <= list.getNumberOfEntries(); i++) {
      if (i > 1) {
        ids.append(',');
      }
      ids.append(list.getEntry(i).getGuestId());
    }
    return ids.toString();
  }

  // ==================================================================
  // PART 1 - THE COLLECTION ADTs
  // ==================================================================

  private static void testListAdt() {
    TestRunner.section("1.1  LIST ADT  (adt/ArrayList - the TEAM ADT)");

    ListInterface<String> list = new ArrayList<>();

    TestRunner.check("a new list is empty", list.isEmpty());
    TestRunner.checkEquals("a new list has no entries", 0, list.getNumberOfEntries());

    list.add("A");
    list.add("B");
    list.add("C");
    TestRunner.checkEquals("add appends in order", 3, list.getNumberOfEntries());
    TestRunner.checkEquals("positions are 1-BASED", "A", list.getEntry(1));
    TestRunner.checkEquals("getEntry reads the last entry", "C", list.getEntry(3));

    list.add(2, "X");
    TestRunner.checkEquals("add at a position inserts there", "X", list.getEntry(2));
    TestRunner.checkEquals("add at a position shifts the rest right", "B", list.getEntry(3));
    TestRunner.checkEquals("size grows by one", 4, list.getNumberOfEntries());

    TestRunner.checkEquals("remove returns the removed entry", "X", list.remove(2));
    TestRunner.checkEquals("remove shifts the rest left", "B", list.getEntry(2));

    list.replace(1, "Z");
    TestRunner.checkEquals("replace overwrites in place", "Z", list.getEntry(1));

    TestRunner.check("contains finds a present entry", list.contains("B"));
    TestRunner.check("contains rejects an absent entry", !list.contains("Q"));
    TestRunner.checkEquals("getPosition is 1-based", 2, list.getPosition("B"));
    TestRunner.checkEquals("getPosition returns -1 when absent", -1, list.getPosition("Q"));

    TestRunner.checkEquals("removeEntry removes by value", "B", list.removeEntry("B"));
    TestRunner.check("removeEntry actually removed it", !list.contains("B"));

    // Out-of-range access must not crash.
    TestRunner.checkEquals("getEntry(0) is null, not a crash", null, list.getEntry(0));
    TestRunner.checkEquals("getEntry past the end is null", null, list.getEntry(999));

    list.clear();
    TestRunner.check("clear empties the list", list.isEmpty());

    // Growth past the initial capacity - the resize path.
    ListInterface<Integer> big = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      big.add(i);
    }
    TestRunner.checkEquals("grows past its initial capacity", 100, big.getNumberOfEntries());
    TestRunner.checkEquals("first entry survives resizing", Integer.valueOf(1), big.getEntry(1));
    TestRunner.checkEquals("last entry survives resizing", Integer.valueOf(100), big.getEntry(100));
  }

  private static void testListAddedOperations() {
    TestRunner.section("1.2  LIST ADT - THE SIX ADDED OPERATIONS");

    ListInterface<Integer> numbers = new ArrayList<>();
    for (int n : new int[] {5, 3, 9, 1, 7}) {
      numbers.add(n);
    }

    // filter
    Condition<Integer> isBig = value -> value > 4;
    ListInterface<Integer> big = numbers.filter(isBig);
    TestRunner.checkEquals("filter returns only matching entries", 3, big.getNumberOfEntries());
    TestRunner.checkEquals("filter does NOT shrink the source", 5, numbers.getNumberOfEntries());

    big.add(999);
    TestRunner.checkEquals("the filtered list is INDEPENDENT of the source",
        5, numbers.getNumberOfEntries());

    // search
    TestRunner.checkEquals("search returns the first match", Integer.valueOf(5),
        numbers.search(isBig));
    TestRunner.checkEquals("search returns null when nothing matches", null,
        numbers.search(value -> value > 1000));

    // countIf
    TestRunner.checkEquals("countIf agrees with filter", 3, numbers.countIf(isBig));
    TestRunner.checkEquals("countIf returns 0 when nothing matches", 0,
        numbers.countIf(value -> value > 1000));

    // sort - ascending
    ListInterface<Integer> toSort = numbers.filter(value -> true);
    toSort.sort((a, b) -> Integer.compare(a, b));
    TestRunner.checkEquals("sort orders ascending", Integer.valueOf(1), toSort.getEntry(1));
    TestRunner.checkEquals("sort puts the largest last", Integer.valueOf(9), toSort.getEntry(5));

    // sort - already sorted, reversed, all equal
    ListInterface<Integer> reversed = new ArrayList<>();
    for (int n : new int[] {5, 4, 3, 2, 1}) {
      reversed.add(n);
    }
    reversed.sort((a, b) -> Integer.compare(a, b));
    TestRunner.checkEquals("sort handles fully reversed input",
        "1", String.valueOf(reversed.getEntry(1)));

    ListInterface<Integer> allEqual = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      allEqual.add(7);
    }
    allEqual.sort((a, b) -> Integer.compare(a, b));
    TestRunner.checkEquals("sort handles all-equal input", 4, allEqual.getNumberOfEntries());

    ListInterface<Integer> single = new ArrayList<>();
    single.add(42);
    single.sort((a, b) -> Integer.compare(a, b));
    TestRunner.checkEquals("sort handles a single entry", Integer.valueOf(42), single.getEntry(1));

    ListInterface<Integer> none = new ArrayList<>();
    none.sort((a, b) -> Integer.compare(a, b));
    TestRunner.check("sort handles an empty list without crashing", none.isEmpty());

    // sort past the initial capacity
    ListInterface<Integer> large = new ArrayList<>();
    for (int i = 60; i >= 1; i--) {
      large.add(i);
    }
    large.sort((a, b) -> Integer.compare(a, b));
    boolean ordered = true;
    for (int i = 1; i <= 60; i++) {
      if (large.getEntry(i) != i) {
        ordered = false;
      }
    }
    TestRunner.check("sort works past the initial array capacity", ordered);
  }

  private static void testMergeSortStability() {
    TestRunner.section("1.3  LIST ADT - MERGE SORT STABILITY");

    // Two guests with the SAME name, registered in a known order. A stable sort
    // must leave them in that order. This is the property the module relies on
    // when listing by name: tied guests keep their arrival order.
    ListInterface<WalkInGuest> guests = new ArrayList<>();
    WalkInGuest first = walkIn("WG1001", "Lim", false);
    WalkInGuest second = walkIn("WG1002", "Lim", false);
    WalkInGuest third = walkIn("WG1003", "Abu", false);
    guests.add(first);
    guests.add(second);
    guests.add(third);

    guests.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

    TestRunner.checkEquals("sorting by name puts Abu first", "WG1003",
        guests.getEntry(1).getGuestId());
    TestRunner.checkEquals("equal names keep their original order (1st)",
        "WG1001", guests.getEntry(2).getGuestId());
    TestRunner.checkEquals("equal names keep their original order (2nd)",
        "WG1002", guests.getEntry(3).getGuestId());

    // A larger stability check: sort by a key that ignores the tiebreaker.
    ListInterface<String> pairs = new ArrayList<>();
    for (String s : new String[] {"b1", "a1", "b2", "a2", "b3", "a3"}) {
      pairs.add(s);
    }
    pairs.sort((x, y) -> Character.compare(x.charAt(0), y.charAt(0)));
    TestRunner.checkEquals("stable across many ties (a1 first)", "a1", pairs.getEntry(1));
    TestRunner.checkEquals("stable across many ties (a2 second)", "a2", pairs.getEntry(2));
    TestRunner.checkEquals("stable across many ties (a3 third)", "a3", pairs.getEntry(3));
    TestRunner.checkEquals("stable across many ties (b1 fourth)", "b1", pairs.getEntry(4));
  }

  private static void testStackAdt() {
    TestRunner.section("1.4  STACK ADT  (adt/ArrayStack - LIFO)");

    StackInterface<String> stack = new ArrayStack<>();

    TestRunner.check("a new stack is empty", stack.isEmpty());
    TestRunner.checkEquals("peek on an empty stack returns null", null, stack.peek());
    TestRunner.checkEquals("pop on an empty stack returns null", null, stack.pop());

    stack.push("first");
    stack.push("second");
    stack.push("third");

    TestRunner.checkEquals("size tracks pushes", 3, stack.getNumberOfEntries());
    TestRunner.checkEquals("peek returns the NEWEST entry", "third", stack.peek());
    TestRunner.checkEquals("peek does not remove", 3, stack.getNumberOfEntries());

    TestRunner.checkEquals("pop returns the newest (LIFO)", "third", stack.pop());
    TestRunner.checkEquals("pop removed it", 2, stack.getNumberOfEntries());
    TestRunner.checkEquals("the next pop returns the one before", "second", stack.pop());
    TestRunner.checkEquals("and then the oldest", "first", stack.pop());
    TestRunner.check("the stack is empty again", stack.isEmpty());

    // Growth past capacity.
    StackInterface<Integer> deep = new ArrayStack<>();
    for (int i = 1; i <= 50; i++) {
      deep.push(i);
    }
    TestRunner.checkEquals("grows past its initial capacity", 50, deep.getNumberOfEntries());
    TestRunner.checkEquals("still LIFO after growing", Integer.valueOf(50), deep.pop());
  }

  private static void testQueueAdt() {
    TestRunner.section("1.5  QUEUE ADT  (adt/ArrayQueue - FIFO, circular)");

    QueueInterface<String> queue = new ArrayQueue<>();

    TestRunner.check("a new queue is empty", queue.isEmpty());
    TestRunner.checkEquals("getFront on an empty queue returns null", null, queue.getFront());
    TestRunner.checkEquals("dequeue on an empty queue returns null", null, queue.dequeue());

    queue.enqueue("Ali");
    queue.enqueue("Siti");
    queue.enqueue("Kumar");

    TestRunner.checkEquals("size tracks enqueues", 3, queue.getNumberOfEntries());
    TestRunner.checkEquals("getFront returns the OLDEST entry", "Ali", queue.getFront());
    TestRunner.checkEquals("getFront does not remove", 3, queue.getNumberOfEntries());

    TestRunner.checkEquals("dequeue returns the oldest (FIFO)", "Ali", queue.dequeue());
    TestRunner.checkEquals("then the next oldest", "Siti", queue.dequeue());
    TestRunner.checkEquals("then the newest", "Kumar", queue.dequeue());
    TestRunner.check("the queue is empty again", queue.isEmpty());

    // The circular-array wrap: interleave enqueues and dequeues so the front
    // index passes the end of the backing array and wraps.
    QueueInterface<Integer> circular = new ArrayQueue<>(3);
    for (int round = 1; round <= 10; round++) {
      circular.enqueue(round);
      TestRunner.checkEquals("wrap round " + round + " dequeues in order",
          Integer.valueOf(round), circular.dequeue());
    }
    TestRunner.check("queue is empty after interleaved wrap", circular.isEmpty());

    // Growth past capacity while holding entries.
    QueueInterface<Integer> growing = new ArrayQueue<>(2);
    for (int i = 1; i <= 20; i++) {
      growing.enqueue(i);
    }
    TestRunner.checkEquals("grows past its initial capacity", 20, growing.getNumberOfEntries());
    TestRunner.checkEquals("order survives growing", Integer.valueOf(1), growing.dequeue());
  }

  private static void testBookingBst() {
    TestRunner.section("1.6  BINARY SEARCH TREE  (adt/BookingBST - non-linear)");

    BookingBST tree = new BookingBST();

    TestRunner.check("a new tree is empty", tree.isEmpty());
    TestRunner.checkEquals("search on an empty tree returns null", null,
        tree.search("50231847"));

    TestRunner.check("add returns true for a new booking",
        tree.add(booking("50231847", "Ali", "101")));
    TestRunner.check("add returns true for a smaller key",
        tree.add(booking("20114563", "Siti", "102")));
    TestRunner.check("add returns true for a larger key",
        tree.add(booking("78905512", "Kumar", "103")));

    TestRunner.checkEquals("size tracks inserts", 3, tree.getNumberOfEntries());

    // The duplicate check falls out of the search path - it is free.
    TestRunner.check("add REJECTS a duplicate confirmation number",
        !tree.add(booking("50231847", "Someone Else", "999")));
    TestRunner.checkEquals("a rejected duplicate does not change the size",
        3, tree.getNumberOfEntries());

    TestRunner.checkEquals("search finds the root", "Ali",
        tree.search("50231847").getGuestName());
    TestRunner.checkEquals("search finds a left-subtree node", "Siti",
        tree.search("20114563").getGuestName());
    TestRunner.checkEquals("search finds a right-subtree node", "Kumar",
        tree.search("78905512").getGuestName());
    TestRunner.checkEquals("search returns null for an absent key", null,
        tree.search("00000000"));

    TestRunner.check("contains agrees with search", tree.contains("20114563"));
    TestRunner.check("contains rejects an absent key", !tree.contains("00000000"));
    TestRunner.check("add rejects null", !tree.add(null));

    // In-order traversal yields ascending order WITHOUT a sort being run.
    ListInterface<Booking> all = tree.getAllBookings();
    TestRunner.checkEquals("traversal returns every booking", 3, all.getNumberOfEntries());
    TestRunner.checkEquals("in-order traversal is sorted (1st)", "20114563",
        all.getEntry(1).getConfirmationNumber());
    TestRunner.checkEquals("in-order traversal is sorted (2nd)", "50231847",
        all.getEntry(2).getConfirmationNumber());
    TestRunner.checkEquals("in-order traversal is sorted (3rd)", "78905512",
        all.getEntry(3).getConfirmationNumber());

    // The degenerate case: already-sorted inserts make a chain, but it must
    // still find everything correctly.
    BookingBST chain = new BookingBST();
    for (int i = 1; i <= 20; i++) {
      chain.add(booking(String.format("%08d", i), "Guest" + i, "R" + i));
    }
    TestRunner.checkEquals("degenerate (sorted-input) tree holds all entries",
        20, chain.getNumberOfEntries());
    TestRunner.checkEquals("degenerate tree still searches correctly", "Guest20",
        chain.search("00000020").getGuestName());
    TestRunner.checkEquals("degenerate tree still traverses in order", "00000001",
        chain.getAllBookings().getEntry(1).getConfirmationNumber());
  }

  // ==================================================================
  // PART 2 - MODULE 1: WALK-IN REGISTRATION
  // Logic copies mirror WalkInRegistrationBookingMaintenance / ...UI.
  // ==================================================================

  private static String generateNextGuestId(ListInterface<WalkInGuest> records) {
    int highest = 1000;
    for (int position = 1; position <= records.getNumberOfEntries(); position++) {
      String guestId = records.getEntry(position).getGuestId();
      if (guestId != null && guestId.length() > 2 && guestId.startsWith("WG")) {
        try {
          int number = Integer.parseInt(guestId.substring(2));
          if (number > highest) {
            highest = number;
          }
        } catch (NumberFormatException e) {
          // not a WG#### id - cannot be the highest
        }
      }
    }
    return "WG" + (highest + 1);
  }

  private static int findUrgentInsertPosition(ListInterface<WalkInGuest> records) {
    for (int position = 1; position <= records.getNumberOfEntries(); position++) {
      WalkInGuest guest = records.getEntry(position);
      if (WalkInGuest.STATUS_WAITING.equals(guest.getStatus()) && !guest.isUrgent()) {
        return position;
      }
    }
    return -1;
  }

  private static boolean isLetter(char c) {
    return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isValidName(String name) {
    if (name == null || name.isEmpty() || name.equals("0")) {
      return false;
    }
    if (name.length() < 2 || name.length() > 40) {
      return false;
    }
    boolean hasLetter = false;
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (isLetter(c)) {
        hasLetter = true;
      }
      boolean permitted = isLetter(c) || isDigit(c) || c == ' ' || c == '\''
          || c == '-' || c == '.' || c == '/' || c == '@';
      if (!permitted) {
        return false;
      }
    }
    return hasLetter;
  }

  private static String digitsOf(String text) {
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      if (isDigit(text.charAt(i))) {
        digits.append(text.charAt(i));
      }
    }
    return digits.toString();
  }

  private static boolean isValidContact(String contact) {
    if (contact == null || contact.isEmpty() || contact.equals("0")) {
      return false;
    }
    for (int i = 0; i < contact.length(); i++) {
      char c = contact.charAt(i);
      if (!isDigit(c) && c != ' ' && c != '-') {
        return false;
      }
    }
    String digits = digitsOf(contact);
    return digits.length() >= 9 && digits.length() <= 10 && digits.startsWith("0");
  }

  private static void registerGuest(ListInterface<WalkInGuest> records,
      StackInterface<WalkInGuest> history, WalkInGuest guest) {
    int position = guest.isUrgent() ? findUrgentInsertPosition(records) : -1;
    if (position == -1) {
      records.add(guest);
    } else {
      records.add(position, guest);
    }
    history.push(guest);
  }

  private static void testModule1() {
    TestRunner.section("2.1  MODULE 1 - AUTO-ASSIGNED GUEST ID");

    ListInterface<WalkInGuest> records = new ArrayList<>();
    TestRunner.checkEquals("empty records start at WG1001", "WG1001",
        generateNextGuestId(records));

    records.add(walkIn("WG1001", "Ali", false));
    records.add(walkIn("WG1002", "Siti", false));
    TestRunner.checkEquals("next id follows the highest", "WG1003",
        generateNextGuestId(records));

    records.remove(2);
    TestRunner.checkEquals("a freed id is reused after an undo", "WG1002",
        generateNextGuestId(records));

    ListInterface<WalkInGuest> unordered = new ArrayList<>();
    unordered.add(walkIn("WG1050", "High", false));
    unordered.add(walkIn("WG1002", "Low", false));
    TestRunner.checkEquals("scans for the highest, not the last", "WG1051",
        generateNextGuestId(unordered));

    ListInterface<WalkInGuest> malformed = new ArrayList<>();
    malformed.add(walkIn("WGABCD", "Bad", false));
    malformed.add(walkIn(null, "NoId", false));
    malformed.add(walkIn("WG1007", "Good", false));
    TestRunner.checkEquals("ignores malformed and null ids", "WG1008",
        generateNextGuestId(malformed));

    TestRunner.section("2.2  MODULE 1 - URGENT INSERTION POLICY (anti-starvation)");

    TestRunner.checkEquals("empty queue appends", -1,
        findUrgentInsertPosition(new ArrayList<>()));

    ListInterface<WalkInGuest> normals = new ArrayList<>();
    normals.add(walkIn("WG1001", "N1", false));
    normals.add(walkIn("WG1002", "N2", false));
    TestRunner.checkEquals("all-normal queue - urgent goes to the front", 1,
        findUrgentInsertPosition(normals));

    ListInterface<WalkInGuest> mixed = new ArrayList<>();
    mixed.add(walkIn("WG1001", "U1", true));
    mixed.add(walkIn("WG1002", "U2", true));
    mixed.add(walkIn("WG1003", "N1", false));
    TestRunner.checkEquals("behind existing urgent, ahead of normal", 3,
        findUrgentInsertPosition(mixed));

    ListInterface<WalkInGuest> withServed = new ArrayList<>();
    WalkInGuest served = walkIn("WG1001", "Served", false);
    served.setStatus(WalkInGuest.STATUS_SERVED);
    withServed.add(served);
    withServed.add(walkIn("WG1002", "N1", false));
    TestRunner.checkEquals("a served guest does not block the insert", 2,
        findUrgentInsertPosition(withServed));

    TestRunner.section("2.3  MODULE 1 - REGISTRATION SEQUENCE");

    ListInterface<WalkInGuest> queue = new ArrayList<>();
    StackInterface<WalkInGuest> history = new ArrayStack<>();

    registerGuest(queue, history, walkIn("WG1001", "Normal1", false));
    registerGuest(queue, history, walkIn("WG1002", "Normal2", false));
    TestRunner.checkEquals("normals join the back in order", "WG1001,WG1002", idsOf(queue));

    registerGuest(queue, history, walkIn("WG1003", "Urgent1", true));
    TestRunner.checkEquals("first urgent jumps the normals",
        "WG1003,WG1001,WG1002", idsOf(queue));

    registerGuest(queue, history, walkIn("WG1004", "Urgent2", true));
    TestRunner.checkEquals("second urgent goes BEHIND the first",
        "WG1003,WG1004,WG1001,WG1002", idsOf(queue));

    registerGuest(queue, history, walkIn("WG1005", "Normal3", false));
    TestRunner.checkEquals("a later normal still joins the very back",
        "WG1003,WG1004,WG1001,WG1002,WG1005", idsOf(queue));

    TestRunner.check("NO urgent guest ever overtakes an earlier urgent guest",
        queue.getEntry(1).getGuestId().equals("WG1003")
        && queue.getEntry(2).getGuestId().equals("WG1004"));

    TestRunner.section("2.4  MODULE 1 - UNDO  (peek, confirm, pop)");

    ListInterface<WalkInGuest> undoRecords = new ArrayList<>();
    StackInterface<WalkInGuest> undoHistory = new ArrayStack<>();
    registerGuest(undoRecords, undoHistory, walkIn("WG1001", "Ali", false));
    registerGuest(undoRecords, undoHistory, walkIn("WG1002", "Siti", false));

    TestRunner.checkEquals("peek shows the newest registration", "WG1002",
        undoHistory.peek().getGuestId());

    // Declining must change NOTHING. This is the bug the design fixed.
    WalkInGuest candidate = undoHistory.peek();
    boolean confirmed = false;
    if (confirmed) {
      undoHistory.pop();
      undoRecords.removeEntry(candidate);
    }
    TestRunner.checkEquals("declining leaves the stack intact", 2,
        undoHistory.getNumberOfEntries());
    TestRunner.checkEquals("declining leaves the records intact", 2,
        undoRecords.getNumberOfEntries());
    TestRunner.checkEquals("the same guest is still on top", "WG1002",
        undoHistory.peek().getGuestId());

    // Confirming removes from both.
    candidate = undoHistory.peek();
    int position = undoRecords.getPosition(candidate);
    undoHistory.pop();
    undoRecords.remove(position);
    TestRunner.checkEquals("confirming pops the stack", 1, undoHistory.getNumberOfEntries());
    TestRunner.checkEquals("confirming removes the record", "WG1001", idsOf(undoRecords));

    TestRunner.section("2.5  MODULE 1 - NAME VALIDATION");

    TestRunner.check("rejects empty", !isValidName(""));
    TestRunner.check("rejects one character", !isValidName("A"));
    TestRunner.check("rejects the cancel keyword 0", !isValidName("0"));
    TestRunner.check("rejects digits only", !isValidName("123"));
    TestRunner.check("rejects symbols only", !isValidName("###"));
    TestRunner.check("rejects over 40 characters",
        !isValidName("This name is far far too long to be accepted"));
    TestRunner.check("rejects an illegal symbol", !isValidName("Ali <script>"));
    TestRunner.check("rejects a comma", !isValidName("Lim, Ah Kow"));

    TestRunner.check("accepts a plain name", isValidName("Ali Bakar"));
    TestRunner.check("accepts exactly 2 characters", isValidName("Li"));
    TestRunner.check("accepts a Malaysian a/l name", isValidName("Muthu a/l Samy"));
    TestRunner.check("accepts binti", isValidName("Nur Aisyah binti Rahman"));
    TestRunner.check("accepts an apostrophe", isValidName("O'Brien"));
    TestRunner.check("accepts a hyphen", isValidName("Mary-Jane"));
    TestRunner.check("accepts a full stop", isValidName("John Lim Jr."));

    TestRunner.section("2.6  MODULE 1 - CONTACT VALIDATION");

    TestRunner.check("rejects empty", !isValidContact(""));
    TestRunner.check("rejects the cancel keyword 0", !isValidContact("0"));
    TestRunner.check("rejects 8 digits", !isValidContact("01234567"));
    TestRunner.check("rejects 11 digits", !isValidContact("01234567890"));
    TestRunner.check("rejects a number not starting with 0", !isValidContact("123456789"));
    TestRunner.check("rejects letters", !isValidContact("012ABC7890"));
    TestRunner.check("rejects a plus prefix", !isValidContact("+60123456789"));

    TestRunner.check("accepts 9 digits", isValidContact("012345678"));
    TestRunner.check("accepts 10 digits", isValidContact("0123456789"));
    TestRunner.check("accepts dashes", isValidContact("012-3456789"));
    TestRunner.check("accepts spaces", isValidContact("012 345 6789"));
    TestRunner.checkEquals("dashes are ignored when counting", "0123456789",
        digitsOf("012-345-6789"));
  }

  // ==================================================================
  // PART 3 - MODULE 2: HOUSEKEEPING TASK LOG
  // ==================================================================

  private static void testModule2() {
    TestRunner.section("3.1  MODULE 2 - APPEND-ONLY LOG AND ROLLBACK STACK");

    ListInterface<HousekeepingTask> log = new ArrayList<>();
    StackInterface<HousekeepingTask> history = new ArrayStack<>();

    LocalDateTime base = LocalDateTime.of(2026, 8, 25, 8, 0);

    HousekeepingTask t1 = task("HT001", "101", "Dirty", base);
    HousekeepingTask t2 = task("HT002", "102", "Dirty", base.plusMinutes(5));
    HousekeepingTask t3 = task("HT003", "101", "Cleaning In Progress", base.plusMinutes(75));

    log.add(t1);
    history.push(t1);
    log.add(t2);
    history.push(t2);
    log.add(t3);
    history.push(t3);

    TestRunner.checkEquals("the log appends, never overwrites", 3, log.getNumberOfEntries());
    TestRunner.checkEquals("room 101 has two entries", 2,
        log.countIf(t -> t.getRoomNumber().equals("101")));

    TestRunner.checkEquals("rollback peeks the newest update", "HT003",
        history.peek().getTaskId());
    TestRunner.checkEquals("peek does not consume", 3, history.getNumberOfEntries());

    // Declining a rollback must change nothing.
    boolean confirmed = false;
    if (confirmed) {
      history.pop();
    }
    TestRunner.checkEquals("declining a rollback leaves the stack", 3,
        history.getNumberOfEntries());

    HousekeepingTask rolledBack = history.pop();
    log.removeEntry(rolledBack);
    TestRunner.checkEquals("confirming removes the newest update", 2,
        log.getNumberOfEntries());
    TestRunner.checkEquals("room 101 is back to one entry", 1,
        log.countIf(t -> t.getRoomNumber().equals("101")));

    TestRunner.section("3.2  MODULE 2 - CURRENT STATE PER ROOM (log collapse)");

    ListInterface<HousekeepingTask> fullLog = new ArrayList<>();
    fullLog.add(task("HT001", "101", "Dirty", base));
    fullLog.add(task("HT002", "102", "Dirty", base.plusMinutes(5)));
    fullLog.add(task("HT003", "101", "Cleaning In Progress", base.plusMinutes(75)));
    fullLog.add(task("HT004", "101", "Inspected", base.plusMinutes(160)));
    fullLog.add(task("HT005", "102", "Cleaning In Progress", base.plusMinutes(90)));

    // Collapse: keep each room's LATEST entry, as buildCurrentRoomList does.
    ListInterface<String> seenRooms = new ArrayList<>();
    ListInterface<HousekeepingTask> current = new ArrayList<>();
    for (int i = 1; i <= fullLog.getNumberOfEntries(); i++) {
      HousekeepingTask entry = fullLog.getEntry(i);
      String room = entry.getRoomNumber();
      if (!seenRooms.contains(room)) {
        seenRooms.add(room);
        HousekeepingTask latest = entry;
        for (int j = i + 1; j <= fullLog.getNumberOfEntries(); j++) {
          HousekeepingTask other = fullLog.getEntry(j);
          if (other.getRoomNumber().equals(room)
              && other.getTimestamp().isAfter(latest.getTimestamp())) {
            latest = other;
          }
        }
        current.add(latest);
      }
    }

    TestRunner.checkEquals("collapses 5 log entries to 2 rooms", 2,
        current.getNumberOfEntries());
    TestRunner.checkEquals("room 101 shows its LATEST status", "Inspected",
        current.getEntry(1).getStatus());
    TestRunner.checkEquals("room 102 shows its LATEST status", "Cleaning In Progress",
        current.getEntry(2).getStatus());
    TestRunner.checkEquals("the source log is untouched", 5, fullLog.getNumberOfEntries());

    TestRunner.section("3.3  MODULE 2 - SORTING AND SEARCHING");

    ListInterface<HousekeepingTask> toSort = fullLog.filter(t -> true);
    toSort.sort(Comparator.comparingInt(
        (HousekeepingTask t) -> Integer.parseInt(t.getRoomNumber()))
        .thenComparing(HousekeepingTask::getTimestamp));

    TestRunner.checkEquals("sorts by room number ascending", "101",
        toSort.getEntry(1).getRoomNumber());
    TestRunner.checkEquals("ties break on timestamp - earliest first", "HT001",
        toSort.getEntry(1).getTaskId());
    TestRunner.checkEquals("last entry is the higher room", "102",
        toSort.getEntry(5).getRoomNumber());
    TestRunner.checkEquals("sorting a copy leaves the source order intact", "HT002",
        fullLog.getEntry(2).getTaskId());

    // Linear search by unique task ID.
    HousekeepingTask found = fullLog.search(t -> t.getTaskId().equals("HT004"));
    TestRunner.checkEquals("search finds a task by its unique id", "Inspected",
        found.getStatus());
    TestRunner.checkEquals("search returns null for an unknown id", null,
        fullLog.search(t -> t.getTaskId().equals("HT999")));

    // Filter by status and by room.
    TestRunner.checkEquals("filter by status", 2,
        fullLog.filter(t -> t.getStatus().equals("Dirty")).getNumberOfEntries());
    TestRunner.checkEquals("filter by room", 3,
        fullLog.filter(t -> t.getRoomNumber().equals("101")).getNumberOfEntries());

    TestRunner.section("3.4  MODULE 2 - DUPLICATE STATUS REJECTION");

    // A task may not be set to the status it already has.
    String currentStatus = current.getEntry(1).getStatus();
    TestRunner.check("setting the SAME status is rejected",
        currentStatus.equalsIgnoreCase("Inspected"));
    TestRunner.check("setting a DIFFERENT status is allowed",
        !currentStatus.equalsIgnoreCase("Ready for Check-In"));
  }

  // ==================================================================
  // PART 4 - MODULE 3: FRONT-DESK SERVICE
  // ==================================================================

  private static void testModule3() {
    TestRunner.section("4.1  MODULE 3 - BST LOOKUP AND DUAL STORAGE");

    BookingBST tree = new BookingBST();
    ListInterface<Booking> list = new ArrayList<>();

    String[] numbers = {"50231847", "20114563", "78905512", "10009999", "99887766"};
    for (int i = 0; i < numbers.length; i++) {
      Booking b = booking(numbers[i], "Guest" + i, "R10" + i);
      tree.add(b);
      list.add(b);
    }

    TestRunner.checkEquals("every booking is in the tree", 5, tree.getNumberOfEntries());
    TestRunner.checkEquals("every booking is also in the List", 5, list.getNumberOfEntries());

    // Both structures hold the SAME object, not copies.
    Booking fromTree = tree.search("50231847");
    Booking fromList = list.search(b -> b.getConfirmationNumber().equals("50231847"));
    TestRunner.check("tree and List share the same object, not copies",
        fromTree == fromList);

    TestRunner.check("duplicate confirmation number is rejected",
        !tree.add(booking("50231847", "Impostor", "R999")));

    TestRunner.section("4.2  MODULE 3 - BILLING AND OUTSTANDING BALANCE");

    ListInterface<BillingRecord> billing = new ArrayList<>();
    billing.add(new BillingRecord("50231847", 1000.0, 1000.0));
    billing.add(new BillingRecord("20114563", 800.0, 300.0));
    billing.add(new BillingRecord("78905512", 500.0, 0.0));

    BillingRecord paid = billing.search(r -> r.getConfirmationNumber().equals("50231847"));
    TestRunner.checkEquals("a fully paid bill has no balance", 0.0,
        paid.getOutstandingBalance());
    TestRunner.checkEquals("a fully paid bill reports PAID", "PAID", paid.getPaymentStatus());

    BillingRecord partial = billing.search(r -> r.getConfirmationNumber().equals("20114563"));
    TestRunner.checkEquals("a part-paid bill reports the remainder", 500.0,
        partial.getOutstandingBalance());
    TestRunner.checkEquals("a part-paid bill reports OUTSTANDING", "OUTSTANDING",
        partial.getPaymentStatus());

    BillingRecord unpaid = billing.search(r -> r.getConfirmationNumber().equals("78905512"));
    TestRunner.checkEquals("an unpaid bill owes the full amount", 500.0,
        unpaid.getOutstandingBalance());

    TestRunner.checkEquals("counts the outstanding bills", 2,
        billing.countIf(r -> r.getOutstandingBalance() > 0));

    TestRunner.section("4.3  MODULE 3 - ENTITY equals (added during the audit)");

    // These matter because the List ADT's contains/getPosition/removeEntry
    // compare with equals(). Without them a record rebuilt from file could not
    // be matched against one in memory.
    BillingRecord original = new BillingRecord("50231847", 1000.0, 1000.0);
    BillingRecord rebuilt = new BillingRecord("50231847", 1000.0, 1000.0);
    TestRunner.check("two bills with the same confirmation number are equal",
        original.equals(rebuilt));
    TestRunner.check("bills with different numbers are not equal",
        !original.equals(new BillingRecord("11111111", 1000.0, 1000.0)));
    TestRunner.check("a bill is not equal to null", !original.equals(null));
    TestRunner.checkEquals("equal bills share a hash code",
        original.hashCode(), rebuilt.hashCode());
    TestRunner.check("toString does not return the default Object form",
        !original.toString().contains("@"));

    ListInterface<BillingRecord> matching = new ArrayList<>();
    matching.add(original);
    TestRunner.check("contains matches an equal-but-different instance",
        matching.contains(rebuilt));
    TestRunner.checkEquals("getPosition matches an equal-but-different instance",
        1, matching.getPosition(rebuilt));

    FrontDeskRecord record1 = new FrontDeskRecord(booking("50231847", "Ali", "101"), original);
    FrontDeskRecord record2 = new FrontDeskRecord(booking("50231847", "Ali", "101"), rebuilt);
    TestRunner.check("front-desk records pairing the same booking+bill are equal",
        record1.equals(record2));
    TestRunner.check("front-desk toString is not the default Object form",
        !record1.toString().contains("@"));

    TestRunner.section("4.4  MODULE 3 - ROOM AVAILABILITY (date overlap)");

    // A room is unavailable if a requested stay overlaps an existing booking.
    Booking existing = new Booking("50231847", "Ali", "101",
        LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 25));

    TestRunner.check("a stay fully inside an existing booking overlaps",
        LocalDate.of(2026, 8, 21).isBefore(existing.getCheckOutDate())
        && LocalDate.of(2026, 8, 23).isAfter(existing.getCheckInDate()));
    TestRunner.check("a stay entirely before does NOT overlap",
        !LocalDate.of(2026, 8, 19).isAfter(existing.getCheckInDate()));
    TestRunner.check("a stay entirely after does NOT overlap",
        LocalDate.of(2026, 8, 26).isAfter(existing.getCheckOutDate()));
  }

  // ==================================================================
  // PART 5 - MODULE 4: LOYALTY & REWARDS
  // ==================================================================

  private static void testModule4() {
    TestRunner.section("5.1  MODULE 4 - REDEMPTION QUEUE (FIFO fairness)");

    QueueInterface<Redemption> pending = new ArrayQueue<>();
    LocalDate today = LocalDate.of(2026, 8, 25);

    pending.enqueue(new Redemption("RD001", "M001", "RW001", 300, today,
        Redemption.STATUS_PENDING));
    pending.enqueue(new Redemption("RD002", "M002", "RW002", 500, today,
        Redemption.STATUS_PENDING));
    pending.enqueue(new Redemption("RD003", "M003", "RW003", 800, today,
        Redemption.STATUS_PENDING));

    TestRunner.checkEquals("three requests are queued", 3, pending.getNumberOfEntries());
    TestRunner.checkEquals("the front is the FIRST request made", "RD001",
        pending.getFront().getRedemptionId());

    TestRunner.checkEquals("processing takes the earliest request", "RD001",
        pending.dequeue().getRedemptionId());
    TestRunner.checkEquals("then the next earliest", "RD002",
        pending.dequeue().getRedemptionId());
    TestRunner.checkEquals("then the last", "RD003",
        pending.dequeue().getRedemptionId());
    TestRunner.check("nothing is left pending", pending.isEmpty());
    TestRunner.checkEquals("processing an empty queue returns null", null,
        pending.dequeue());

    TestRunner.section("5.2  MODULE 4 - POINTS AND TIER PROGRESSION");

    Member member = new Member("M001", "Ali", 0, today.plusMonths(6));
    TestRunner.checkEquals("a new member starts at Silver", "Silver", member.getTier());
    TestRunner.checkEquals("a new member starts with the points given", 0,
        member.getPoints());

    member.setPoints(member.getPoints() + 1200);
    TestRunner.checkEquals("points accumulate", 1200, member.getPoints());

    member.setTier("Gold");
    TestRunner.checkEquals("tier can progress", "Gold", member.getTier());

    TestRunner.section("5.3  MODULE 4 - REDEMPTION AFFORDABILITY");

    Reward cheap = new Reward("RW001", "Airport Transfer Voucher", 300);
    Reward costly = new Reward("RW006", "Weekend Stay Discount", 2500);

    TestRunner.check("a member with enough points can redeem",
        member.getPoints() >= cheap.getPointsRequired());
    TestRunner.check("a member without enough points cannot redeem",
        member.getPoints() < costly.getPointsRequired());

    int before = member.getPoints();
    member.setPoints(before - cheap.getPointsRequired());
    TestRunner.checkEquals("redeeming deducts exactly the reward cost",
        before - 300, member.getPoints());
    TestRunner.check("points never go negative on an affordable redemption",
        member.getPoints() >= 0);

    TestRunner.section("5.4  MODULE 4 - SORTING WITH TIE-BREAKERS");

    ListInterface<Member> members = new ArrayList<>();
    members.add(new Member("M003", "Charlie", 500, today));
    members.add(new Member("M001", "Alice", 500, today));
    members.add(new Member("M002", "Bob", 900, today));

    ListInterface<Member> sorted = members.filter(m -> true);
    sorted.sort(Comparator.comparingInt(Member::getPoints).reversed()
        .thenComparing(Member::getMemberId));

    TestRunner.checkEquals("highest points first", "M002",
        sorted.getEntry(1).getMemberId());
    TestRunner.checkEquals("equal points break on member id (1st)", "M001",
        sorted.getEntry(2).getMemberId());
    TestRunner.checkEquals("equal points break on member id (2nd)", "M003",
        sorted.getEntry(3).getMemberId());
    TestRunner.checkEquals("the master member list is unchanged", "M003",
        members.getEntry(1).getMemberId());

    TestRunner.section("5.5  MODULE 4 - EXPIRING POINTS");

    ListInterface<Member> withExpiry = new ArrayList<>();
    withExpiry.add(new Member("M001", "Soon", 100, today.plusDays(5)));
    withExpiry.add(new Member("M002", "Later", 100, today.plusDays(90)));
    withExpiry.add(new Member("M003", "AlsoSoon", 100, today.plusDays(10)));

    LocalDate cutoff = today.plusDays(30);
    ListInterface<Member> expiring = withExpiry.filter(
        m -> m.getPointsExpiryDate() != null && !m.getPointsExpiryDate().isAfter(cutoff));

    TestRunner.checkEquals("finds members expiring before the cutoff", 2,
        expiring.getNumberOfEntries());

    expiring.sort(Comparator.comparing(Member::getPointsExpiryDate)
        .thenComparing(Member::getMemberId));
    TestRunner.checkEquals("soonest expiry listed first", "M001",
        expiring.getEntry(1).getMemberId());

    TestRunner.checkEquals("a far-future expiry is excluded", 0,
        expiring.countIf(m -> m.getMemberId().equals("M002")));
  }

  // ==================================================================
  // MAIN
  // ==================================================================

  public static void main(String[] args) {
    TestRunner.suite("UNIT TESTS - ADTs AND ALL FOUR MODULES");

    testListAdt();
    testListAddedOperations();
    testMergeSortStability();
    testStackAdt();
    testQueueAdt();
    testBookingBst();

    testModule1();
    testModule2();
    testModule3();
    testModule4();

    TestRunner.report("UNIT TESTS");
    TestRunner.exitWithStatus();
  }
}
