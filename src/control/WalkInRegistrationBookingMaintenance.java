package control;

import adt.ArrayStack;
import adt.Condition;
import adt.ListInterface;
import adt.StackInterface;
import boundary.WalkInRegistrationBookingUI;
import dao.WalkInGuestDAO;
import dao.WalkInGuestInitializer;
import entity.WalkInGuest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Business logic for the Walk-In Registration & Standard Booking module.
 *
 * DATA PERSISTENCE: walkInRecords is loaded from walkInGuests.dat in the
 * constructor and written back by saveToFile() after every action that changes
 * it, so nothing is lost even if the program is killed without going through
 * "0. Back".
 *
 * REQUIRED ADT: List (adt/ListInterface + adt/ArrayList), used as
 * walkInRecords. Guests are held chronologically - the guest waiting longest is
 * nearest the front - which is what this module's "manage incoming standard
 * reservations chronologically" requirement asks for.
 *
 * The List holds every guest ever registered, not just those still waiting.
 * Keeping served and cancelled guests lets the reports analyse what actually
 * happened during the day; the waiting queue itself is derived from it on
 * demand with the List's filter() operation.
 *
 * Pattern: Queue-like (FIFO) behaviour built on top of the List ADT, using
 * 1-based positions.
 *   - Registering a normal guest -> walkInRecords.add(guest)      (join the back)
 *   - Registering an urgent guest -> walkInRecords.add(pos, guest) (ahead of
 *     normal guests, but behind urgent guests already waiting, so an exception
 *     never overtakes an earlier exception)
 *   - Serving the next guest -> the first WAITING entry, marked SERVED
 *
 * The Stack ADT (adt/ArrayStack) sits on top of that for the undo feature - see
 * registrationHistory below.
 *
 * @author Tan Chee Yan
 */
public class WalkInRegistrationBookingMaintenance {

  private WalkInRegistrationBookingUI walkInRegistrationBookingUI = new WalkInRegistrationBookingUI();
  private WalkInGuestDAO walkInGuestDAO = new WalkInGuestDAO();
  private ListInterface<WalkInGuest> walkInRecords;

  // Remembers registrations made during this session so the most recent one can
  // be undone. A stack is the natural fit - undo always targets the newest
  // registration first (LIFO). This is deliberately NOT persisted: an undo only
  // makes sense for actions taken in the current run, so the history starts
  // empty each time the program is launched.
  private StackInterface<WalkInGuest> registrationHistory = new ArrayStack<>();

  public WalkInRegistrationBookingMaintenance() {
    // Load the saved records from walkInGuests.dat. If there's nothing saved
    // yet (first run), seed with the sample guests so there is data to work
    // with, then write that file out.
    walkInRecords = walkInGuestDAO.retrieveFromFile();
    if (walkInRecords.isEmpty()) {
      walkInRecords = new WalkInGuestInitializer().initializeWalkInGuests();
      walkInGuestDAO.saveToFile(walkInRecords);
    }
  }

  // ==================================================================
  // CONDITIONS - reusable tests passed to the List ADT's filter/search/countIf
  // ==================================================================

  private Condition<WalkInGuest> isWaiting() {
    return guest -> WalkInGuest.STATUS_WAITING.equals(guest.getStatus());
  }

  private Condition<WalkInGuest> hasStatus(String status) {
    return guest -> status.equals(guest.getStatus());
  }

  private Condition<WalkInGuest> isUrgent(boolean urgent) {
    return guest -> guest.isUrgent() == urgent;
  }

  private Condition<WalkInGuest> arrivedOn(LocalDate date) {
    return guest -> guest.getArrivalTime() != null
        && guest.getArrivalTime().toLocalDate().equals(date);
  }

  /**
   * Combines two conditions so an entry must satisfy both. Lets the reports
   * express multi-criteria filters (e.g. "urgent AND served AND today")
   * without the List ADT needing to know anything about them.
   */
  private Condition<WalkInGuest> both(Condition<WalkInGuest> first,
      Condition<WalkInGuest> second) {
    return guest -> first.isSatisfiedBy(guest) && second.isSatisfiedBy(guest);
  }

  // ==================================================================
  // COMPARATORS - orderings passed to the List ADT's sort
  // ==================================================================

  private Comparator<WalkInGuest> byArrivalTime() {
    return (first, second) -> {
      if (first.getArrivalTime() == null && second.getArrivalTime() == null) {
        return 0;
      }
      if (first.getArrivalTime() == null) {
        return 1;
      }
      if (second.getArrivalTime() == null) {
        return -1;
      }
      return first.getArrivalTime().compareTo(second.getArrivalTime());
    };
  }

  private Comparator<WalkInGuest> byName() {
    return (first, second) -> first.getName().compareToIgnoreCase(second.getName());
  }

  private Comparator<WalkInGuest> byWaitingTimeDescending() {
    return (first, second) -> Long.compare(second.getWaitingMinutes(), first.getWaitingMinutes());
  }

  /**
   * Service order: urgent exceptions first, and within the same type the guest
   * who arrived earliest goes first. This is the order the queue is actually
   * worked through.
   */
  private Comparator<WalkInGuest> byServiceOrder() {
    return (first, second) -> {
      if (first.isUrgent() != second.isUrgent()) {
        return first.isUrgent() ? -1 : 1;
      }
      return byArrivalTime().compare(first, second);
    };
  }

  // ==================================================================
  // SUBMODULE 1 - GUEST REGISTRATION
  // ==================================================================

  /**
   * Works out the next guest ID to hand out, so the front-desk officer never
   * types one.
   *
   * Scans every record for the highest WG number in use and adds one. Scanning
   * (rather than keeping a counter) means the IDs stay correct even if the
   * .dat file is replaced or an undo removes the newest guest.
   *
   * @return the next free ID, e.g. "WG1012"
   */
  private String generateNextGuestId() {
    int highest = 1000;

    Iterator<WalkInGuest> iterator = walkInRecords.getIterator();
    while (iterator.hasNext()) {
      String guestId = iterator.next().getGuestId();
      if (guestId != null && guestId.length() > 2 && guestId.startsWith("WG")) {
        try {
          int number = Integer.parseInt(guestId.substring(2));
          if (number > highest) {
            highest = number;
          }
        } catch (NumberFormatException e) {
          // An ID that isn't WG#### can't be the highest - skip it.
        }
      }
    }

    return "WG" + (highest + 1);
  }

  /**
   * Registers a new walk-in guest.
   *
   * A normal guest joins the back of the queue. An urgent guest is placed ahead
   * of the normal guests who are waiting, but behind any urgent guests already
   * waiting - so an exception never overtakes an earlier exception.
   *
   * @param urgent true to register an urgent exception case
   */
  public void registerWalkIn(boolean urgent) {
    walkInRegistrationBookingUI.startAction(urgent
        ? "REGISTER URGENT WALK-IN GUEST"
        : "REGISTER NORMAL WALK-IN GUEST");

    String assignedId = generateNextGuestId();
    WalkInGuest guest = walkInRegistrationBookingUI.inputWalkInGuest(urgent, assignedId);

    if (guest == null) {
      walkInRegistrationBookingUI.displayMessage("Registration cancelled.");
      return;
    }

    // The ID is generated, not typed, so a clash should be impossible - but
    // check anyway, because contains() proves it before anything is stored.
    if (walkInRecords.contains(guest)) {
      walkInRegistrationBookingUI.displayMessage(
          "Guest ID " + guest.getGuestId() + " is already in use! Registration rejected.");
      return;
    }

    int insertPosition = urgent ? findUrgentInsertPosition() : -1;

    if (insertPosition == -1) {
      walkInRecords.add(guest);
    } else {
      walkInRecords.add(insertPosition, guest);
    }

    registrationHistory.push(guest);
    walkInGuestDAO.saveToFile(walkInRecords);

    walkInRegistrationBookingUI.displayMessage(urgent
        ? "Urgent guest registered and placed ahead of the normal queue."
        : "Guest registered at the back of the queue.");
    walkInRegistrationBookingUI.displayGuest(guest);
    walkInRegistrationBookingUI.displayMessage(
        "Guests now waiting: " + countWaiting());
  }

  /**
   * Finds where an urgent guest should be inserted: after the last urgent guest
   * who is still waiting, and before the first normal guest who is still
   * waiting.
   *
   * @return the 1-based position to insert at, or -1 to append to the end
   */
  private int findUrgentInsertPosition() {
    for (int position = 1; position <= walkInRecords.getNumberOfEntries(); position++) {
      WalkInGuest guest = walkInRecords.getEntry(position);
      if (WalkInGuest.STATUS_WAITING.equals(guest.getStatus()) && !guest.isUrgent()) {
        return position;
      }
    }
    return -1;
  }

  /**
   * Undoes the most recent registration made in this session by popping it off
   * the history stack and removing it from the records.
   *
   * The guest may already have been served since being registered, in which
   * case undoing would rewrite history that has already happened - that is
   * refused rather than treated as an error.
   */
  public void undoLastRegistration() {
    walkInRegistrationBookingUI.startAction("UNDO LAST REGISTRATION");

    // peek() rather than pop(): the entry must stay on the stack until the user
    // has confirmed, otherwise answering "no" would still consume the undo and
    // the next undo would silently skip a registration.
    WalkInGuest lastRegistered = registrationHistory.peek();

    if (lastRegistered == null) {
      walkInRegistrationBookingUI.displayMessage(
          "Nothing to undo - no guests have been registered in this session.");
      return;
    }

    int position = walkInRecords.getPosition(lastRegistered);
    if (position == -1) {
      registrationHistory.pop();
      walkInRegistrationBookingUI.displayMessage(
          "Cannot undo - guest " + lastRegistered.getGuestId()
          + " is no longer in the records.");
      return;
    }

    WalkInGuest current = walkInRecords.getEntry(position);
    if (!WalkInGuest.STATUS_WAITING.equals(current.getStatus())) {
      registrationHistory.pop();
      walkInRegistrationBookingUI.displayMessage(
          "Cannot undo - guest " + lastRegistered.getGuestId() + " has already been "
          + current.getStatus().toLowerCase() + ".");
      return;
    }

    // Show what will be undone before asking, so the user is confirming
    // something specific rather than a blind yes/no.
    walkInRegistrationBookingUI.displayMessage("This registration will be removed:");
    walkInRegistrationBookingUI.displayGuest(lastRegistered);

    if (!walkInRegistrationBookingUI.confirm(
        "Are you sure you want to undo this registration?")) {
      walkInRegistrationBookingUI.displayMessage("Undo cancelled - nothing has been changed.");
      return;
    }

    registrationHistory.pop();
    walkInRecords.remove(position);
    walkInGuestDAO.saveToFile(walkInRecords);

    walkInRegistrationBookingUI.displayMessage(
        "Undone - registration for guest " + lastRegistered.getGuestId()
        + " (" + lastRegistered.getName() + ") has been removed.");
    walkInRegistrationBookingUI.displayMessage(
        "Guests now waiting: " + countWaiting());
  }

  // ==================================================================
  // SUBMODULE 2 - QUEUE OPERATIONS
  // ==================================================================

  /**
   * Serves the guest at the front of the waiting queue.
   *
   * The guest is not deleted - they are marked SERVED and keep their place in
   * the records, so the reports can still analyse them afterwards.
   */
  public void serveNextGuest() {
    walkInRegistrationBookingUI.startAction("SERVE NEXT GUEST");

    WalkInGuest next = walkInRecords.search(isWaiting());

    if (next == null) {
      walkInRegistrationBookingUI.displayGuestServedMessage(null);
      return;
    }

    // Serving is shown and confirmed before it happens, the same way cancelling
    // and undoing are. Marking a guest SERVED cannot be reversed from within
    // this module, so the officer is told exactly who is at the front of the
    // queue before committing to it.
    walkInRegistrationBookingUI.displayMessage("Next guest in the queue:");
    walkInRegistrationBookingUI.displayGuest(next);

    if (!walkInRegistrationBookingUI.confirm(
        "Serve this guest now?")) {
      walkInRegistrationBookingUI.displayMessage(
          "Serving aborted - the guest is still waiting.");
      return;
    }

    next.setStatus(WalkInGuest.STATUS_SERVED);
    next.setServedTime(java.time.LocalDateTime.now());
    walkInGuestDAO.saveToFile(walkInRecords);

    walkInRegistrationBookingUI.displayGuestServedMessage(next);
    walkInRegistrationBookingUI.displayMessage(
        "Guests still waiting: " + countWaiting());
  }

  /**
   * Shows every guest currently waiting, in the order they will be served.
   */
  public boolean displayQueue() {
    walkInRegistrationBookingUI.startAction("CURRENT WALK-IN QUEUE");

    ListInterface<WalkInGuest> waiting = walkInRecords.filter(isWaiting());

    return walkInRegistrationBookingUI.displayGuestList(waiting,
        "Position 1 is served next:",
        "The walk-in queue is empty. No guests are waiting.");
  }

  /**
   * Removes a waiting guest from the queue - for example a guest who gave up
   * and left before being served.
   *
   * The guest is marked CANCELLED rather than deleted, so they still appear in
   * the reports as someone who was lost.
   */
  public void cancelWaitingGuest() {
    walkInRegistrationBookingUI.startAction("CANCEL A WAITING GUEST");

    ListInterface<WalkInGuest> waiting = walkInRecords.filter(isWaiting());

    if (waiting.isEmpty()) {
      walkInRegistrationBookingUI.displayMessage(
          "The walk-in queue is empty. There is no one to cancel.");
      return;
    }

    // A compact pick list rather than the usual paged table - the user is about
    // to type a position, so a pager between the list and that prompt would
    // capture the keystroke meant for it.
    walkInRegistrationBookingUI.displayGuestPickList(waiting,
        "Guests currently waiting (position 1 is served next):");

    int position = walkInRegistrationBookingUI.inputPositionToCancel(
        waiting.getNumberOfEntries());

    if (position == 0) {
      walkInRegistrationBookingUI.displayMessage("Cancellation aborted.");
      return;
    }

    // The chosen position refers to the filtered waiting list, so take the
    // guest from there. Because filter() copies references (not clones), the
    // object found here is the very same object held in walkInRecords -
    // updating it updates the records too.
    WalkInGuest guest = waiting.getEntry(position);

    if (!walkInRegistrationBookingUI.confirm(
        "Cancel the registration for " + guest.getGuestId() + " - " + guest.getName() + "?")) {
      walkInRegistrationBookingUI.displayMessage("Cancellation aborted.");
      return;
    }

    guest.setStatus(WalkInGuest.STATUS_CANCELLED);
    walkInGuestDAO.saveToFile(walkInRecords);

    walkInRegistrationBookingUI.displayMessage(
        "Guest " + guest.getGuestId() + " has been cancelled and removed from the queue.");
    walkInRegistrationBookingUI.displayMessage(
        "Guests still waiting: " + countWaiting());
  }

  // ==================================================================
  // SUBMODULE 3 - SEARCH & FILTER
  // ==================================================================

  /**
   * Finds one guest by their exact guest ID.
   */
  public void searchByGuestId() {
    walkInRegistrationBookingUI.startAction("SEARCH BY GUEST ID");

    // Keeps asking so several guests can be looked up without going back out
    // to the menu each time. The header stays on screen and the results build
    // up under it, which is what makes comparing two guests possible.
    while (true) {
      final String guestId = walkInRegistrationBookingUI.inputGuestIdToSearch();

      if (guestId == null) {
        return;
      }

      WalkInGuest found = walkInRecords.search(guest -> guestId.equals(guest.getGuestId()));

      if (found == null) {
        walkInRegistrationBookingUI.displayMessage(
            "No guest found with ID " + guestId + ".");
        continue;
      }

      walkInRegistrationBookingUI.displayMessage("Guest found:");
      walkInRegistrationBookingUI.displayGuest(found);

      if (WalkInGuest.STATUS_WAITING.equals(found.getStatus())) {
        ListInterface<WalkInGuest> waiting = walkInRecords.filter(isWaiting());
        walkInRegistrationBookingUI.displayMessage(
            "Currently at position " + waiting.getPosition(found) + " in the queue.");
      }
    }
  }

  /**
   * Finds every guest whose name contains the text entered, so a partial or
   * misremembered name still finds them.
   */
  public void searchByName() {
    walkInRegistrationBookingUI.startAction("SEARCH BY NAME");

    while (true) {
      String text = walkInRegistrationBookingUI.inputNameToSearch();

      if (text == null) {
        return;
      }

      String lowerCaseText = text.toLowerCase();
      ListInterface<WalkInGuest> matches = walkInRecords.filter(
          guest -> guest.getName().toLowerCase().contains(lowerCaseText));

      // A paged listing clears the screen on its way out, so the action title
      // has to be redrawn before the next prompt or it would be lost.
      if (walkInRegistrationBookingUI.displayGuestList(matches,
          "SEARCH RESULTS FOR \"" + text + "\"",
          "No guest found with a name containing \"" + text + "\".")) {
        walkInRegistrationBookingUI.startAction("SEARCH BY NAME");
      }
    }
  }

  /**
   * Lists every guest with the chosen status.
   */
  public boolean filterByStatus() {
    walkInRegistrationBookingUI.startAction("FILTER BY STATUS");

    String status = walkInRegistrationBookingUI.inputStatusFilter();

    if (status == null) {
      walkInRegistrationBookingUI.displayMessage("Filter cancelled.");
      return false;
    }

    ListInterface<WalkInGuest> matches = walkInRecords.filter(hasStatus(status));

    return walkInRegistrationBookingUI.displayGuestList(matches,
        "GUESTS WITH STATUS: " + status,
        "No guests currently have the status " + status + ".");
  }

  /**
   * Lists every guest of the chosen type (urgent or normal).
   */
  public boolean filterByType() {
    walkInRegistrationBookingUI.startAction("FILTER BY GUEST TYPE");

    int choice = walkInRegistrationBookingUI.inputTypeFilter();

    if (choice == 0) {
      walkInRegistrationBookingUI.displayMessage("Filter cancelled.");
      return false;
    }

    boolean urgent = (choice == 1);
    ListInterface<WalkInGuest> matches = walkInRecords.filter(isUrgent(urgent));

    return walkInRegistrationBookingUI.displayGuestList(matches,
        urgent ? "URGENT EXCEPTION CASES" : "NORMAL WALK-IN GUESTS",
        urgent ? "No urgent exception cases recorded." : "No normal walk-in guests recorded.");
  }

  // ==================================================================
  // SUBMODULE 4 - SORTED LISTINGS
  // ==================================================================

  /**
   * Shows all records arranged into a chosen order.
   *
   * The sort is applied to a filtered copy, never to walkInRecords itself, so
   * the chronological order the queue depends on is never disturbed by someone
   * simply looking at a listing.
   *
   * @param comparator the order to arrange the guests into
   * @param heading the title to show above the listing
   */
  private boolean displaySorted(Comparator<WalkInGuest> comparator, String heading) {
    walkInRegistrationBookingUI.startAction(heading);

    // filter() with an always-true condition gives a copy of every record.
    ListInterface<WalkInGuest> copy = walkInRecords.filter(guest -> true);

    if (copy.isEmpty()) {
      walkInRegistrationBookingUI.displayMessage("There are no records to list.");
      return false;
    }

    copy.sort(comparator);

    // The heading is already on screen as the action banner, so the table is
    // labelled with the record count instead of repeating it.
    return walkInRegistrationBookingUI.displayGuestList(copy,
        "All " + copy.getNumberOfEntries() + " records, sorted:",
        "There are no records to list.");
  }

  public boolean displaySortedByArrival() {
    return displaySorted(byArrivalTime(), "ALL RECORDS - BY ARRIVAL TIME (EARLIEST FIRST)");
  }

  public boolean displaySortedByName() {
    return displaySorted(byName(), "ALL RECORDS - BY GUEST NAME (A-Z)");
  }

  public boolean displaySortedByWaitingTime() {
    return displaySorted(byWaitingTimeDescending(), "ALL RECORDS - BY WAITING TIME (LONGEST FIRST)");
  }

  public boolean displaySortedByServiceOrder() {
    return displaySorted(byServiceOrder(), "ALL RECORDS - BY SERVICE ORDER (URGENT FIRST, THEN ARRIVAL)");
  }

  // ==================================================================
  // SUBMODULE 5 - REPORTS
  // ==================================================================

  /**
   * REPORT 1: Queue Performance Analysis.
   *
   * Answers "how well did the desk cope today?" - how many guests arrived, how
   * many were served, how many gave up, how long they waited, and when the busy
   * periods were.
   *
   * Combines filtering (by date, status and type), sorting (longest waits
   * first) and counting over the List ADT.
   */
  public void generateQueuePerformanceReport() {
    LocalDate today = LocalDate.now();
    ListInterface<WalkInGuest> todayRecords = walkInRecords.filter(arrivedOn(today));

    walkInRegistrationBookingUI.displayReportHeader("QUEUE PERFORMANCE ANALYSIS REPORT");

    if (todayRecords.isEmpty()) {
      walkInRegistrationBookingUI.displayMessage("  No walk-in guests arrived today.");
      walkInRegistrationBookingUI.displayReportFooter();
      return;
    }

    int total = todayRecords.getNumberOfEntries();
    int served = todayRecords.countIf(hasStatus(WalkInGuest.STATUS_SERVED));
    int waiting = todayRecords.countIf(hasStatus(WalkInGuest.STATUS_WAITING));
    int cancelled = todayRecords.countIf(hasStatus(WalkInGuest.STATUS_CANCELLED));
    int urgent = todayRecords.countIf(isUrgent(true));
    int normal = todayRecords.countIf(isUrgent(false));

    walkInRegistrationBookingUI.displayReportSection("1. ARRIVAL AND SERVICE SUMMARY");
    walkInRegistrationBookingUI.displayReportLine("Total walk-in guests today", String.valueOf(total));
    walkInRegistrationBookingUI.displayReportLine("Served", served + formatPercentage(served, total));
    walkInRegistrationBookingUI.displayReportLine("Still waiting", waiting + formatPercentage(waiting, total));
    walkInRegistrationBookingUI.displayReportLine("Cancelled / left", cancelled + formatPercentage(cancelled, total));

    walkInRegistrationBookingUI.displayReportSection("2. GUEST TYPE BREAKDOWN");
    walkInRegistrationBookingUI.displayReportLine("Normal walk-ins", normal + formatPercentage(normal, total));
    walkInRegistrationBookingUI.displayReportLine("Urgent exception cases", urgent + formatPercentage(urgent, total));

    // Waiting-time statistics, computed over served guests only - a guest still
    // waiting has no final wait yet, so including them would understate it.
    ListInterface<WalkInGuest> servedToday = todayRecords.filter(
        both(hasStatus(WalkInGuest.STATUS_SERVED), arrivedOn(today)));

    walkInRegistrationBookingUI.displayReportSection("3. WAITING TIME ANALYSIS (SERVED GUESTS)");
    if (servedToday.isEmpty()) {
      walkInRegistrationBookingUI.displayReportLine("No guests served yet today", "-");
    } else {
      long totalWait = 0;
      long longestWait = 0;
      long shortestWait = Long.MAX_VALUE;

      Iterator<WalkInGuest> iterator = servedToday.getIterator();
      while (iterator.hasNext()) {
        long wait = iterator.next().getWaitingMinutes();
        totalWait += wait;
        if (wait > longestWait) {
          longestWait = wait;
        }
        if (wait < shortestWait) {
          shortestWait = wait;
        }
      }

      long averageWait = totalWait / servedToday.getNumberOfEntries();

      walkInRegistrationBookingUI.displayReportLine("Guests served", String.valueOf(servedToday.getNumberOfEntries()));
      walkInRegistrationBookingUI.displayReportLine("Average waiting time", averageWait + " minutes");
      walkInRegistrationBookingUI.displayReportLine("Longest waiting time", longestWait + " minutes");
      walkInRegistrationBookingUI.displayReportLine("Shortest waiting time", shortestWait + " minutes");

      // Sorting puts the worst waits at the top, which is what management
      // needs to see first.
      servedToday.sort(byWaitingTimeDescending());

      walkInRegistrationBookingUI.displayReportSection("4. LONGEST WAITS (TOP 3)");
      Iterator<WalkInGuest> worst = servedToday.getIterator();
      int shown = 0;
      while (worst.hasNext() && shown < 3) {
        WalkInGuest guest = worst.next();
        walkInRegistrationBookingUI.displayReportLine(
            (shown + 1) + ". " + guest.getGuestId() + " - " + guest.getName(),
            guest.getWaitingMinutes() + " minutes (" + guest.getGuestType() + ")");
        shown++;
      }
    }

    walkInRegistrationBookingUI.displayReportSection("5. ARRIVALS BY HOUR (PEAK PERIOD)");
    displayHourlyChart(todayRecords);

    walkInRegistrationBookingUI.displayReportFooter();
  }

  /**
   * Draws the arrivals-per-hour bar chart, covering only the hours the resort
   * actually saw guests so the chart isn't mostly empty columns.
   */
  private void displayHourlyChart(ListInterface<WalkInGuest> records) {
    int[] hourlyCounts = new int[24];

    Iterator<WalkInGuest> iterator = records.getIterator();
    while (iterator.hasNext()) {
      WalkInGuest guest = iterator.next();
      if (guest.getArrivalTime() != null) {
        hourlyCounts[guest.getArrivalTime().getHour()]++;
      }
    }

    int firstHour = -1;
    int lastHour = -1;
    for (int hour = 0; hour < 24; hour++) {
      if (hourlyCounts[hour] > 0) {
        if (firstHour == -1) {
          firstHour = hour;
        }
        lastHour = hour;
      }
    }

    if (firstHour == -1) {
      walkInRegistrationBookingUI.displayMessage("  (no arrivals recorded)");
      return;
    }

    int span = lastHour - firstHour + 1;
    String[] labels = new String[span];
    int[] counts = new int[span];

    for (int i = 0; i < span; i++) {
      int hour = firstHour + i;
      labels[i] = String.format("%02d:00", hour);
      counts[i] = hourlyCounts[hour];
    }

    walkInRegistrationBookingUI.displayBarChart(labels, counts, 40);

    // Name the busiest hour outright so the reader doesn't have to eyeball the
    // chart to find it.
    int peakHour = firstHour;
    for (int hour = firstHour; hour <= lastHour; hour++) {
      if (hourlyCounts[hour] > hourlyCounts[peakHour]) {
        peakHour = hour;
      }
    }

    walkInRegistrationBookingUI.displayBlankLine();
    walkInRegistrationBookingUI.displayReportLine("Peak arrival hour",
        String.format("%02d:00 - %02d:59 (%d guests)", peakHour, peakHour, hourlyCounts[peakHour]));
  }

  /**
   * REPORT 2: Urgency Exception Audit.
   *
   * Every urgent registration lets a guest skip the chronological queue, so
   * management needs to see that the override is being used properly: how often
   * it happens, for what reasons, and whether it is delaying the normal guests.
   *
   * Combines filtering (urgent vs normal, by status), sorting (by name, and by
   * wait) and counting over the List ADT.
   */
  public void generateUrgencyAuditReport() {
    walkInRegistrationBookingUI.displayReportHeader("URGENCY EXCEPTION AUDIT REPORT");

    ListInterface<WalkInGuest> urgentGuests = walkInRecords.filter(isUrgent(true));
    ListInterface<WalkInGuest> normalGuests = walkInRecords.filter(isUrgent(false));

    int totalRecords = walkInRecords.getNumberOfEntries();
    int urgentCount = urgentGuests.getNumberOfEntries();
    int normalCount = normalGuests.getNumberOfEntries();

    walkInRegistrationBookingUI.displayReportSection("1. OVERRIDE USAGE");
    walkInRegistrationBookingUI.displayReportLine("Total guests on record", String.valueOf(totalRecords));
    walkInRegistrationBookingUI.displayReportLine("Urgent exceptions granted",
        urgentCount + formatPercentage(urgentCount, totalRecords));
    walkInRegistrationBookingUI.displayReportLine("Normal registrations",
        normalCount + formatPercentage(normalCount, totalRecords));

    if (urgentCount == 0) {
      walkInRegistrationBookingUI.displayMessage("\n  No urgency exceptions have been granted.");
      walkInRegistrationBookingUI.displayReportFooter();
      return;
    }

    walkInRegistrationBookingUI.displayReportSection("2. REASONS GIVEN");
    displayReasonChart(urgentGuests);

    walkInRegistrationBookingUI.displayReportSection("3. IMPACT ON WAITING TIMES");
    long urgentAverage = averageWaitOf(urgentGuests.filter(hasStatus(WalkInGuest.STATUS_SERVED)));
    long normalAverage = averageWaitOf(normalGuests.filter(hasStatus(WalkInGuest.STATUS_SERVED)));

    walkInRegistrationBookingUI.displayReportLine("Average wait - urgent guests (served)",
        urgentAverage + " minutes");
    walkInRegistrationBookingUI.displayReportLine("Average wait - normal guests (served)",
        normalAverage + " minutes");
    walkInRegistrationBookingUI.displayReportLine("Time saved by the override",
        (normalAverage - urgentAverage) + " minutes on average");

    walkInRegistrationBookingUI.displayReportSection("4. ALL EXCEPTION CASES (BY GUEST NAME)");
    urgentGuests.sort(byName());

    Iterator<WalkInGuest> iterator = urgentGuests.getIterator();
    while (iterator.hasNext()) {
      walkInRegistrationBookingUI.displayExceptionRow(iterator.next());
    }

    walkInRegistrationBookingUI.displayReportFooter();
  }

  /**
   * Groups the urgent guests by the reason given and charts how often each
   * reason was used.
   *
   * Reasons are collected into a List ADT rather than a map, since the Java
   * Collections Framework is not permitted here.
   */
  private void displayReasonChart(ListInterface<WalkInGuest> urgentGuests) {
    ListInterface<String> reasons = new adt.ArrayList<>();
    ListInterface<Integer> tallies = new adt.ArrayList<>();

    Iterator<WalkInGuest> iterator = urgentGuests.getIterator();
    while (iterator.hasNext()) {
      String reason = iterator.next().getUrgencyReason();
      if (reason == null) {
        reason = "(not recorded)";
      }

      int position = reasons.getPosition(reason);
      if (position == -1) {
        reasons.add(reason);
        tallies.add(1);
      } else {
        tallies.replace(position, tallies.getEntry(position) + 1);
      }
    }

    int count = reasons.getNumberOfEntries();
    String[] labels = new String[count];
    int[] counts = new int[count];

    for (int i = 1; i <= count; i++) {
      String reason = reasons.getEntry(i);
      // Trim long reasons so the chart labels stay aligned. The limit is wide
      // enough to keep similar reasons apart - at 10 characters both
      // "Wheelchair / mobility assistance" and "Wheelchair assistance required"
      // would collapse to the same "Wheelchair" label.
      labels[i - 1] = reason.length() > 28 ? reason.substring(0, 27) + "~" : reason;
      counts[i - 1] = tallies.getEntry(i);
    }

    walkInRegistrationBookingUI.displayBarChart(labels, counts, 30);

    walkInRegistrationBookingUI.displayBlankLine();
    for (int i = 1; i <= count; i++) {
      walkInRegistrationBookingUI.displayReportLine(
          "  " + reasons.getEntry(i), tallies.getEntry(i) + " case(s)");
    }
  }

  /**
   * Works out the mean waiting time across a list of guests.
   *
   * @param guests the guests to average over
   * @return the average wait in whole minutes, or 0 if the list is empty
   */
  private long averageWaitOf(ListInterface<WalkInGuest> guests) {
    if (guests.isEmpty()) {
      return 0;
    }

    long total = 0;
    Iterator<WalkInGuest> iterator = guests.getIterator();
    while (iterator.hasNext()) {
      total += iterator.next().getWaitingMinutes();
    }

    return total / guests.getNumberOfEntries();
  }

  /**
   * Formats a count as a percentage of a total, for display beside the count.
   *
   * @return e.g. " (42.9%)", or an empty string when the total is zero
   */
  private String formatPercentage(int count, int total) {
    if (total == 0) {
      return "";
    }
    return String.format(" (%.1f%%)", (double) count / total * 100);
  }

  /**
   * How many guests are still waiting to be served.
   */
  private int countWaiting() {
    return walkInRecords.countIf(isWaiting());
  }

  // ==================================================================
  // MENU DRIVERS
  // ==================================================================

  /**
   * Holds the screen after an action unless the user has already dismissed it.
   *
   * A paged listing ends with the user pressing 0, which is itself a
   * deliberate "I have finished looking" keystroke, and the listing is cleared
   * on the way out. Pausing again there would make leaving a listing take two
   * keystrokes and would leave the finished table on screen. Every other action
   * still pauses, so its result stays readable until the user is ready.
   *
   * @param alreadyDismissed true if the action ended with the user quitting a
   * paged listing
   */
  private void pauseUnlessQuit(boolean alreadyDismissed) {
    if (!alreadyDismissed) {
      walkInRegistrationBookingUI.pause();
    }
  }

  private void runRegistrationMenu() {
    int choice;
    do {
      choice = walkInRegistrationBookingUI.getRegistrationMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
          registerWalkIn(false);
          walkInRegistrationBookingUI.pause();
          break;
        case 2:
          registerWalkIn(true);
          walkInRegistrationBookingUI.pause();
          break;
        case 3:
          undoLastRegistration();
          walkInRegistrationBookingUI.pause();
          break;
      }
    } while (choice != 0);
  }

  private void runQueueMenu() {
    int choice;
    do {
      choice = walkInRegistrationBookingUI.getQueueMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
          serveNextGuest();
          walkInRegistrationBookingUI.pause();
          break;
        case 2:
          pauseUnlessQuit(displayQueue());
          break;
        case 3:
          cancelWaitingGuest();
          walkInRegistrationBookingUI.pause();
          break;
      }
    } while (choice != 0);
  }

  private void runSearchMenu() {
    int choice;
    do {
      choice = walkInRegistrationBookingUI.getSearchMenuChoice();
      switch (choice) {
        case 0:
          break;
        // The two searches keep prompting until the user enters 0, so they
        // need no pause - leaving is already a deliberate 0. The filters run
        // once and pause, unless the listing was already dismissed.
        case 1:
          searchByGuestId();
          break;
        case 2:
          searchByName();
          break;
        case 3:
          pauseUnlessQuit(filterByStatus());
          break;
        case 4:
          pauseUnlessQuit(filterByType());
          break;
      }
    } while (choice != 0);
  }

  private void runSortMenu() {
    int choice;
    do {
      choice = walkInRegistrationBookingUI.getSortMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
          pauseUnlessQuit(displaySortedByArrival());
          break;
        case 2:
          pauseUnlessQuit(displaySortedByName());
          break;
        case 3:
          pauseUnlessQuit(displaySortedByWaitingTime());
          break;
        case 4:
          pauseUnlessQuit(displaySortedByServiceOrder());
          break;
      }
    } while (choice != 0);
  }

  private void runReportMenu() {
    int choice;
    do {
      choice = walkInRegistrationBookingUI.getReportMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
          generateQueuePerformanceReport();
          walkInRegistrationBookingUI.pause();
          break;
        case 2:
          generateUrgencyAuditReport();
          walkInRegistrationBookingUI.pause();
          break;
      }
    } while (choice != 0);
  }

  public void runWalkInRegistrationBooking() {
    int choice;
    do {
      choice = walkInRegistrationBookingUI.getMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
          runRegistrationMenu();
          break;
        case 2:
          runQueueMenu();
          break;
        case 3:
          runSearchMenu();
          break;
        case 4:
          runSortMenu();
          break;
        case 5:
          runReportMenu();
          break;
      }
    } while (choice != 0);

    // Persist the records so any changes survive to the next run.
    walkInGuestDAO.saveToFile(walkInRecords);
  }
}
