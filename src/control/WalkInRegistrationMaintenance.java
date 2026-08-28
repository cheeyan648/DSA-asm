package control;

import adt.ArrayList;
import adt.ArrayStack;
import adt.ListInterface;
import adt.StackInterface;
import boundary.WalkInRegistrationUI;
import entity.Guest;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import utility.MessageUI;

/**
 * Walk-In Registration - who is waiting, and whose turn it is.
 *
 * Three ADTs do the work: a List holds every registration and answers the
 * searches, filters and reports; a DualLaneQueue holds only those still
 * waiting and decides who is called next; a Stack remembers this session's
 * registrations so the last one can be undone.
 *
 * @author Tan Chee Yan
 */
public class WalkInRegistrationMaintenance {
  private final WalkInRegistrationUI ui = new WalkInRegistrationUI();
  private final ResortData data;
  private final ResortService service;

  /**
   * Stack ADT - last in, first out, so pop() always returns the newest
   * registration. Not persisted: an undo only makes sense in the session
   * that made the mistake.
   */
  private final StackInterface<WalkInRegistration> undoStack = new ArrayStack<>();

  /** Who is on the desk. Recorded against everything this session does. */
  private final String staffId;

  public WalkInRegistrationMaintenance(ResortService service, String staffId) {
    this.service = service;
    this.data = service.getData();
    this.staffId = staffId;
  }

  // ==================================================================
  // MENU
  // ==================================================================

  public void run() {
    int choice;
    do {
      choice = ui.getMenuChoice();
      switch (choice) {
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
        default:
          break;
      }
    } while (choice != 0);

    data.saveRegistrations();
  }

  private void runRegistrationMenu() {
    int choice;
    do {
      choice = ui.getRegistrationMenuChoice();
      switch (choice) {
        case 1:
          registerGuest(false);
          break;
        case 2:
          registerGuest(true);
          break;
        case 3:
          undoLastRegistration();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runQueueMenu() {
    int choice;
    do {
      choice = ui.getQueueMenuChoice();
      switch (choice) {
        case 1:
          serveNextGuest();
          break;
        case 2:
          cancelWaitingGuest();
          break;
        case 3:
          markNoShow();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runSearchMenu() {
    int choice;
    do {
      choice = ui.getSearchMenuChoice();
      switch (choice) {
        case 1:
          displayQueue();
          ui.pause();
          break;
        case 2:
          searchByRegistrationId();
          break;
        case 3:
          searchByName();
          break;
        case 4:
          filterByStatus();
          ui.pause();
          break;
        case 5:
          filterByPriority();
          ui.pause();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runReportMenu() {
    int choice;
    do {
      choice = ui.getReportMenuChoice();
      switch (choice) {
        case 1:
          queuePerformanceReport();
          break;
        case 2:
          urgencyAuditReport();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  // ==================================================================
  // REGISTRATION
  // ==================================================================

  private void registerGuest(boolean urgent) {
    ui.startAction(urgent ? "REGISTER URGENT WALK-IN" : "REGISTER NORMAL WALK-IN");

    String icPassport = ui.inputIcPassport();
    if (icPassport == null) {
      ui.displayMessage("  Registration cancelled.");
      ui.pause();
      return;
    }

    Guest guest = data.findGuestByIc(icPassport);
    if (guest == null) {
      guest = ui.inputNewGuest(data.nextGuestId(), icPassport);
      if (guest == null) {
        ui.displayMessage("  Registration cancelled.");
        ui.pause();
        return;
      }
      data.addGuest(guest);
      data.saveMasters();
      ui.displaySuccess("New guest record " + guest.getGuestId() + " created.");
    } else {
      ui.displaySuccess("Welcome back, " + guest.getFullName()
          + " (" + guest.getGuestId() + ").");
    }

    // List.search - first entry matching the condition, or null.
    final String guestId = guest.getGuestId();
    WalkInRegistration active = data.getRegistrationList().search(
        reg -> guestId.equals(reg.getGuestId())
            && (reg.isWaiting()
                || WalkInRegistration.STATUS_IN_SERVICE.equals(reg.getStatus())));
    if (active != null) {
      ui.displayError(guest.getFullName() + " already has an active registration ("
          + active.getRegId() + ", " + active.getStatus() + ").");
      ui.pause();
      return;
    }

    String typeId = ui.inputRoomType(data.getRoomTypeList());
    if (typeId == null) {
      ui.displayMessage("  Registration cancelled.");
      ui.pause();
      return;
    }

    int nights = ui.inputNights();
    if (nights < 0) {
      ui.displayMessage("  Registration cancelled.");
      ui.pause();
      return;
    }

    LocalDate checkIn = ui.inputCheckInDate();
    if (checkIn == null) {
      ui.displayMessage("  Registration cancelled.");
      ui.pause();
      return;
    }

    // The departure date is never asked for - it follows from the nights, and
    // is shown here so the officer confirms the stay the guest actually gets.
    ui.displayCalculatedStay(checkIn, nights);

    String reason = null;
    if (urgent) {
      reason = ui.inputUrgencyReason();
      if (reason == null) {
        ui.displayMessage("  Registration cancelled - an urgent case needs a reason.");
        ui.pause();
        return;
      }
    }

    WalkInRegistration reg = new WalkInRegistration(data.nextRegistrationId(),
        guest.getGuestId(), LocalDateTime.now(),
        urgent ? WalkInRegistration.PRIORITY_URGENT : WalkInRegistration.PRIORITY_NORMAL,
        reason, typeId, nights, checkIn);

    // One registration, three ADTs: stored in the List, queued in the lane
    // its priority names, and pushed onto the Stack in case it is undone.
    data.getRegistrationList().add(reg);
    data.getWaitingList().enqueue(reg, reg.getPriority());
    undoStack.push(reg);
    data.saveRegistrations();

    ui.displaySuccess(urgent
        ? "Registered as an URGENT exception - this guest is served next."
        : "Registered. The guest joins the back of the normal queue.");

    RoomType type = data.findRoomType(typeId);
    ui.displayRegistration(reg, guest.getFullName(),
        type == null ? typeId : type.getTypeName());

    int ahead = countAhead(reg);
    ui.displayMessage("");
    ui.displayMessage("  Guests ahead of them: " + ahead);
    ui.pause();
  }

  /**
   * Walks the queue in service order to find a guest's place in it. Worked
   * out on demand rather than stored, because it changes every time anyone
   * else is served.
   */
  private int countAhead(WalkInRegistration reg) {
    ListInterface<WalkInRegistration> order = data.getWaitingList().toServiceOrder();
    for (int i = 1; i <= order.getNumberOfEntries(); i++) {
      if (order.getEntry(i).equals(reg)) {
        return i - 1;
      }
    }
    return 0;
  }

  private void undoLastRegistration() {
    ui.startAction("UNDO LAST REGISTRATION");

    if (undoStack.isEmpty()) {
      ui.displayError("Nothing has been registered in this session to undo.");
      ui.pause();
      return;
    }

    // peek() first - the entry is only popped once the undo is confirmed.
    WalkInRegistration last = undoStack.peek();

    if (!last.isWaiting()) {
      ui.displayError("The last registration (" + last.getRegId() + ") is already "
          + last.getStatus() + " and can no longer be undone.");
      undoStack.pop();
      ui.pause();
      return;
    }

    Guest guest = data.findGuest(last.getGuestId());
    RoomType type = data.findRoomType(last.getRequestedTypeId());

    ui.displayMessage("  This registration will be removed:");
    ui.displayRegistration(last, guest == null ? "-" : guest.getFullName(),
        type == null ? last.getRequestedTypeId() : type.getTypeName());
    ui.displayMessage("");

    if (!ui.confirm("Undo this registration?")) {
      ui.displayMessage("  Undo cancelled - nothing has been changed.");
      ui.pause();
      return;
    }

    // Removed from all three, in the reverse of how it was added.
    undoStack.pop();
    data.getWaitingList().removeEntry(last);
    data.getRegistrationList().removeEntry(last);
    data.saveRegistrations();

    ui.displaySuccess("Registration " + last.getRegId() + " removed.");
    ui.displayMessage("  The guest record has been kept.");
    ui.pause();
  }

  // ==================================================================
  // QUEUE
  // ==================================================================

  private void serveNextGuest() {
    ui.startAction("SERVE NEXT GUEST");

    // peekNext() reads the front of the urgent lane, or the normal lane
    // when urgent is empty - without removing anyone.
    WalkInRegistration next = data.getWaitingList().peekNext();
    if (next == null) {
      ui.displayError("No guests are waiting.");
      ui.pause();
      return;
    }

    Guest guest = data.findGuest(next.getGuestId());
    RoomType type = data.findRoomType(next.getRequestedTypeId());

    ui.displayMessage("  Next in the queue:");
    ui.displayRegistration(next, guest == null ? "-" : guest.getFullName(),
        type == null ? next.getRequestedTypeId() : type.getTypeName());
    ui.displayMessage("");

    if (!ui.confirm("Call this guest to the counter?")) {
      ui.displayMessage("  Cancelled - the guest is still waiting.");
      ui.pause();
      return;
    }

    ServiceResult<WalkInRegistration> called = service.serveNextGuest(staffId);
    if (called.isFailure()) {
      ui.displayError(called.getMessage());
      ui.pause();
      return;
    }

    ui.displaySuccess(called.getMessage());
    ui.displayMessage("  They have left the waiting list and are now IN_SERVICE.");
    ui.displayMessage("  Their booking is made at the Front Desk.");
    ui.displayMessage("");

    reportNextInQueue();
    ui.pause();
  }

  private void reportNextInQueue() {
    int waiting = data.getWaitingList().getNumberOfEntries();
    ui.displayMessage("  " + capitalise(WalkInRegistrationUI.waitingSentence(waiting)) + ".");

    WalkInRegistration upcoming = data.getWaitingList().peekNext();
    if (upcoming == null) {
      ui.displayMessage("  The queue is now empty.");
      return;
    }

    Guest nextGuest = data.findGuest(upcoming.getGuestId());

    ui.displayMessage("  Next to be called: "
        + (nextGuest == null ? "-" : nextGuest.getFullName())
        + ", " + bookingKind(upcoming)
        + ", arrived " + upcoming.getFormattedArrivalTime()
        + ", waited " + upcoming.getFormattedWaitingTime() + ".");
  }

  private String bookingKind(WalkInRegistration reg) {
    return reg.isUrgent() ? "urgent booking" : "normal booking";
  }

  private String capitalise(String sentence) {
    if (sentence == null || sentence.isEmpty()) {
      return sentence;
    }
    return Character.toUpperCase(sentence.charAt(0)) + sentence.substring(1);
  }

  private boolean displayQueue() {
    ui.startAction("CURRENT WALK-IN QUEUE");
    return ui.displayQueue(data.getWaitingList().toServiceOrder(), data,
        data.getWaitingList().getUrgentCount(),
        data.getWaitingList().getNormalCount());
  }

  private void cancelWaitingGuest() {
    ui.startAction("CANCEL A WAITING GUEST");

    ListInterface<WalkInRegistration> queue = data.getWaitingList().toServiceOrder();
    if (!ui.displayQueue(queue, data, data.getWaitingList().getUrgentCount(),
        data.getWaitingList().getNormalCount())) {
      ui.pause();
      return;
    }

    ui.displayMessage("");
    int position = ui.inputQueuePosition(queue.getNumberOfEntries());
    if (position < 0) {
      ui.displayMessage("  Cancelled - nothing has been changed.");
      ui.pause();
      return;
    }

    WalkInRegistration reg = queue.getEntry(position);

    Guest guest = data.findGuest(reg.getGuestId());
    RoomType type = data.findRoomType(reg.getRequestedTypeId());
    ui.displayRegistration(reg, guest == null ? "-" : guest.getFullName(),
        type == null ? reg.getRequestedTypeId() : type.getTypeName());
    ui.displayMessage("");

    if (!ui.confirm("Cancel this guest's registration?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    reg.leaveQueue(WalkInRegistration.STATUS_CANCELLED, LocalDateTime.now());
    reg.setServedBy(staffId);
    data.getWaitingList().removeEntry(reg);
    data.saveRegistrations();

    ui.displaySuccess(reg.getRegId() + " cancelled. "
        + capitalise(WalkInRegistrationUI.waitingSentence(
            data.getWaitingList().getNumberOfEntries())) + ".");
    ui.displayMessage("");
    reportNextInQueue();
    ui.pause();
  }

  private void markNoShow() {
    ui.startAction("MARK A CALLED GUEST AS NO-SHOW");

    // List.filter - a new list of the entries that match, original untouched.
    ListInterface<WalkInRegistration> inService = data.getRegistrationList().filter(
        reg -> WalkInRegistration.STATUS_IN_SERVICE.equals(reg.getStatus()));

    ui.displayMessage("  A guest can only be a no-show once they have been called");
    ui.displayMessage("  to the counter and have not come forward.");

    if (!ui.displayRegistrationList(inService, data,
        "No guest has been called and is still waiting to be dealt with.")) {
      ui.pause();
      return;
    }

    ui.displayMessage("");
    int position = ui.inputListPosition(inService.getNumberOfEntries(),
        "Position to mark as a no-show");
    if (position < 0) {
      ui.displayMessage("  Cancelled - nothing has been changed.");
      ui.pause();
      return;
    }

    WalkInRegistration reg = inService.getEntry(position);

    if (!ui.confirm("Mark " + reg.getRegId() + " as a no-show?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    reg.setStatus(WalkInRegistration.STATUS_NO_SHOW);
    if (reg.getServedAt() == null) {
      reg.setServedAt(LocalDateTime.now());
    }
    data.saveRegistrations();

    ui.displaySuccess(reg.getRegId() + " recorded as a no-show.");
    ui.displayMessage("");
    reportNextInQueue();
    ui.pause();
  }

  // ==================================================================
  // SEARCH AND SORT
  // ==================================================================

  private void searchByRegistrationId() {
    ui.startAction("SEARCH BY REGISTRATION NUMBER");

    while (true) {
      String regId = ui.inputRegistrationId();
      if (regId == null) {
        return;
      }

      WalkInRegistration reg = data.findRegistration(regId);
      if (reg == null) {
        ui.displayError("No registration " + regId
            + ". Enter another number, or 0 to go back.");
        continue;
      }

      showRegistrationDetail(reg);
    }
  }

  private void showRegistrationDetail(WalkInRegistration reg) {
    Guest guest = data.findGuest(reg.getGuestId());
    RoomType type = data.findRoomType(reg.getRequestedTypeId());
    ui.displayRegistration(reg, guest == null ? "-" : guest.getFullName(),
        type == null ? reg.getRequestedTypeId() : type.getTypeName());
  }

  private void searchByName() {
    ui.startAction("SEARCH BY GUEST NAME");

    while (true) {
      String term = ui.inputSearchName();
      if (term == null) {
        return;
      }

      final String lower = term.toLowerCase();
      ListInterface<WalkInRegistration> matches =
          data.getRegistrationList().filter(reg -> {
            Guest guest = data.findGuest(reg.getGuestId());
            return guest != null && guest.getFullName().toLowerCase().contains(lower);
          });

      sortNewestFirst(matches);

      if (ui.displayRegistrationList(matches, data,
          "No guest matched \"" + term + "\".", "SEARCH BY GUEST NAME")) {
        ui.startAction("SEARCH BY GUEST NAME");
      }
    }
  }

  private void filterByStatus() {
    ui.startAction("FILTER BY STATUS");

    String status = ui.inputStatusFilter();
    if (status == null) {
      return;
    }

    ListInterface<WalkInRegistration> matches =
        data.getRegistrationList().filter(reg -> status.equals(reg.getStatus()));

    sortNewestFirst(matches);
    ui.displayRegistrationList(matches, data, "No registration is " + status + ".",
        "FILTER BY STATUS");
  }

  private void filterByPriority() {
    ui.startAction("FILTER BY PRIORITY");

    String priority = ui.inputPriorityFilter();
    if (priority == null) {
      return;
    }

    ListInterface<WalkInRegistration> matches =
        data.getRegistrationList().filter(reg -> priority.equals(reg.getPriority()));

    sortNewestFirst(matches);
    ui.displayRegistrationList(matches, data,
        "No " + priority + " registration found.", "FILTER BY PRIORITY");
  }

  /** List.sort - reorders in place, newest first, nulls pushed to the end. */
  private void sortNewestFirst(ListInterface<WalkInRegistration> list) {
    list.sort(Comparator.comparing(WalkInRegistration::getQueuedAt,
        Comparator.nullsLast(Comparator.reverseOrder())));
  }

  private void runSortMenu() {
    while (true) {
      int choice = ui.getSortChoice();
      if (choice == 0) {
        return;
      }

      ListInterface<WalkInRegistration> sorted = copyOf(data.getRegistrationList());
      String title;

      switch (choice) {
        case 1:
          sorted.sort(Comparator.comparing(WalkInRegistration::getArrivalTime));
          title = "SORTED BY ARRIVAL TIME (EARLIEST FIRST)";
          break;

        case 2:
          sorted.sort((a, b) -> {
            Guest first = data.findGuest(a.getGuestId());
            Guest second = data.findGuest(b.getGuestId());
            String nameA = (first == null) ? "" : first.getFullName();
            String nameB = (second == null) ? "" : second.getFullName();
            return nameA.compareToIgnoreCase(nameB);
          });
          title = "SORTED BY GUEST NAME (A-Z)";
          break;

        case 3:
          sorted.sort(Comparator
              .comparingLong(WalkInRegistration::getWaitingMinutes).reversed());
          title = "SORTED BY WAITING TIME (LONGEST FIRST)";
          break;

        default:
          sorted.sort(Comparator
              .comparing(WalkInRegistration::getStatus)
              .thenComparing(WalkInRegistration::getArrivalTime));
          title = "SORTED BY STATUS, THEN ARRIVAL TIME";
          break;
      }

      ui.displayRegistrationList(sorted, data, "There are no registrations.", title);

      if (MessageUI.pageCount(sorted.getNumberOfEntries()) <= 1) {
        ui.pause();
      }
    }
  }

  /** A copy, so List.sort on a listing cannot reorder the stored records. */
  private ListInterface<WalkInRegistration> copyOf(ListInterface<WalkInRegistration> source) {
    ListInterface<WalkInRegistration> copy = new ArrayList<>();
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      copy.add(source.getEntry(i));
    }
    return copy;
  }

  // ==================================================================
  // REPORTS
  // ==================================================================

  private void queuePerformanceReport() {
    ui.displayPrintedReportHeader("QUEUE PERFORMANCE ANALYSIS REPORT");

    ListInterface<WalkInRegistration> all = data.getRegistrationList();
    if (all.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  There are no registrations to analyse.");
      ui.displayPrintedReportFooter();
      return;
    }

    // List.countIf - counts matches without building an intermediate list.
    int total = all.getNumberOfEntries();
    int urgent = all.countIf(WalkInRegistration::isUrgent);
    int waiting = all.countIf(WalkInRegistration::isWaiting);
    int booked = all.countIf(
        reg -> WalkInRegistration.STATUS_BOOKED.equals(reg.getStatus()));
    int cancelled = all.countIf(
        reg -> WalkInRegistration.STATUS_CANCELLED.equals(reg.getStatus()));
    int noShow = all.countIf(
        reg -> WalkInRegistration.STATUS_NO_SHOW.equals(reg.getStatus()));

    ui.displaySectionHeading("1. ARRIVAL AND SERVICE SUMMARY");
    ui.displayReportLine("Total walk-in guests today", String.valueOf(total));
    ui.displayReportLine("Served", booked + percentOf(booked, total));
    ui.displayReportLine("Still waiting", waiting + percentOf(waiting, total));
    ui.displayReportLine("Cancelled / left", (cancelled + noShow)
        + percentOf(cancelled + noShow, total));

    ui.displaySectionHeading("2. GUEST TYPE BREAKDOWN");
    ui.displayReportLine("Normal walk-ins", (total - urgent) + percentOf(total - urgent, total));
    ui.displayReportLine("Urgent exception cases", urgent + percentOf(urgent, total));

    // Only a called guest has a finished wait to measure.
    ListInterface<WalkInRegistration> called =
        all.filter(reg -> reg.getCalledAt() != null);

    ui.displaySectionHeading("3. WAITING TIME ANALYSIS (SERVED GUESTS)");
    if (called.isEmpty()) {
      ui.displayMessage("  No guest has been called yet.");
    } else {
      long totalWait = 0;
      long shortest = Long.MAX_VALUE;

      for (int i = 1; i <= called.getNumberOfEntries(); i++) {
        long wait = called.getEntry(i).getWaitingMinutes();
        totalWait += wait;
        if (wait < shortest) {
          shortest = wait;
        }
      }

      ListInterface<WalkInRegistration> byWaitDesc = copyOf(called);
      byWaitDesc.sort(Comparator
          .comparingLong(WalkInRegistration::getWaitingMinutes).reversed());
      long longest = byWaitDesc.getEntry(1).getWaitingMinutes();

      int count = called.getNumberOfEntries();
      ui.displayReportLine("Guests served", String.valueOf(count));
      ui.displayReportLine("Average waiting time", (totalWait / count) + " minutes");
      ui.displayReportLine("Longest waiting time", longest + " minutes");
      ui.displayReportLine("Shortest waiting time", shortest + " minutes");

      ui.displaySectionHeading("4. LONGEST WAITS (TOP 3)");
      int topCount = Math.min(3, byWaitDesc.getNumberOfEntries());
      for (int i = 1; i <= topCount; i++) {
        WalkInRegistration reg = byWaitDesc.getEntry(i);
        Guest guest = data.findGuest(reg.getGuestId());
        String name = (guest == null) ? "-" : guest.getFullName();
        ui.displayReportLine(i + ". " + reg.getRegId() + " - " + name,
            reg.getWaitingMinutes() + " minutes ("
                + (reg.isUrgent() ? "URGENT" : "NORMAL") + ")");
      }
    }

    displayArrivalsByHour(all);

    ui.displayPrintedReportFooter();
  }

  private void displayArrivalsByHour(ListInterface<WalkInRegistration> all) {
    // getEntry is 1-based, so the walk runs 1..getNumberOfEntries().
    int[] byHour = new int[24];
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      LocalDateTime arrival = all.getEntry(i).getArrivalTime();
      if (arrival != null) {
        byHour[arrival.getHour()]++;
      }
    }

    int first = -1;
    int last = -1;
    for (int hour = 0; hour < 24; hour++) {
      if (byHour[hour] > 0) {
        if (first < 0) {
          first = hour;
        }
        last = hour;
      }
    }

    if (first < 0) {
      return;
    }

    int busiest = first;
    int highest = byHour[first];
    for (int hour = first; hour <= last; hour++) {
      if (byHour[hour] > highest) {
        highest = byHour[hour];
        busiest = hour;
      }
    }

    ui.displaySectionHeading("5. ARRIVALS BY HOUR (PEAK PERIOD)");
    int barWidth = 60;
    for (int hour = first; hour <= last; hour++) {
      int count = byHour[hour];
      int stars = (highest == 0) ? 0 : (int) Math.round((count * (double) barWidth) / highest);
      System.out.printf("  %02d:00 | %-" + barWidth + "s %d%n",
          hour, "*".repeat(stars), count);
    }
    ui.displayMessage("");
    ui.displayReportLine("Peak arrival hour",
        String.format("%02d:00 - %02d:59 (%d guests)",
            busiest, busiest, byHour[busiest]));
  }

  private void urgencyAuditReport() {
    ui.displayPrintedReportHeader("URGENCY EXCEPTION AUDIT REPORT");

    ListInterface<WalkInRegistration> all = data.getRegistrationList();
    ListInterface<WalkInRegistration> urgent = all.filter(WalkInRegistration::isUrgent);
    int total = all.getNumberOfEntries();
    int urgentCount = urgent.getNumberOfEntries();

    ui.displaySectionHeading("1. OVERRIDE USAGE");
    ui.displayReportLine("Total guests on record", String.valueOf(total));
    ui.displayReportLine("Urgent exceptions granted", urgentCount + percentOf(urgentCount, total));
    ui.displayReportLine("Normal registrations",
        (total - urgentCount) + percentOf(total - urgentCount, total));

    if (urgent.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  No urgency override has been used.");
      ui.displayPrintedReportFooter();
      return;
    }

    displayReasonsGiven(urgent);

    displayWaitSaved(all);

    ui.displaySectionHeading("4. ALL EXCEPTION CASES (BY GUEST NAME)");
    ListInterface<WalkInRegistration> byName = copyOf(urgent);
    byName.sort((a, b) -> {
      Guest first = data.findGuest(a.getGuestId());
      Guest second = data.findGuest(b.getGuestId());
      String nameA = (first == null) ? "" : first.getFullName();
      String nameB = (second == null) ? "" : second.getFullName();
      return nameA.compareToIgnoreCase(nameB);
    });

    for (int i = 1; i <= byName.getNumberOfEntries(); i++) {
      WalkInRegistration reg = byName.getEntry(i);
      Guest guest = data.findGuest(reg.getGuestId());
      String name = (guest == null) ? "-" : guest.getFullName();
      String reason = reg.getUrgencyReason();
      if (reason == null || reason.isBlank()) {
        reason = "(none given)";
      }

      System.out.printf("  %-7s %-24s %-19s %-10s %s%n",
          reg.getGuestId(), truncate(name, 24), reg.getFormattedArrivalTime(),
          reg.getStatus(), reason);
    }

    ui.displayPrintedReportFooter();
  }

  private void displayReasonsGiven(ListInterface<WalkInRegistration> urgent) {
    // Two Lists kept in step as a tally: getPosition finds a reason already
    // seen, and replace() updates its count in place.
    ListInterface<String> reasons = new ArrayList<>();
    ListInterface<Integer> counts = new ArrayList<>();

    for (int i = 1; i <= urgent.getNumberOfEntries(); i++) {
      String reason = urgent.getEntry(i).getUrgencyReason();
      if (reason == null || reason.isBlank()) {
        reason = "(none given)";
      }

      int position = reasons.getPosition(reason);
      if (position < 0) {
        reasons.add(reason);
        counts.add(1);
      } else {
        counts.replace(position, counts.getEntry(position) + 1);
      }
    }

    int highest = 0;
    for (int i = 1; i <= counts.getNumberOfEntries(); i++) {
      highest = Math.max(highest, counts.getEntry(i));
    }

    ui.displaySectionHeading("2. REASONS GIVEN");
    int barWidth = 30;
    for (int i = 1; i <= reasons.getNumberOfEntries(); i++) {
      String reason = reasons.getEntry(i);
      int count = counts.getEntry(i);
      int stars = (highest == 0) ? 0 : (int) Math.round((count * (double) barWidth) / highest);
      System.out.printf("  %-29s | %-" + barWidth + "s %d%n",
          truncateTilde(reason, 29), "*".repeat(stars), count);
    }

    ui.displayMessage("");
    for (int i = 1; i <= reasons.getNumberOfEntries(); i++) {
      ui.displayReportLine("  " + reasons.getEntry(i), counts.getEntry(i) + " case(s)");
    }
  }

  private void displayWaitSaved(ListInterface<WalkInRegistration> all) {
    ListInterface<WalkInRegistration> called = all.filter(reg -> reg.getCalledAt() != null);

    long urgentTotal = 0;
    int urgentCount = 0;
    long normalTotal = 0;
    int normalCount = 0;

    for (int i = 1; i <= called.getNumberOfEntries(); i++) {
      WalkInRegistration reg = called.getEntry(i);
      if (reg.isUrgent()) {
        urgentTotal += reg.getWaitingMinutes();
        urgentCount++;
      } else {
        normalTotal += reg.getWaitingMinutes();
        normalCount++;
      }
    }

    ui.displaySectionHeading("3. IMPACT ON WAITING TIMES");
    if (urgentCount == 0 || normalCount == 0) {
      ui.displayMessage("  Not enough served guests in both lanes to compare yet.");
      return;
    }

    long urgentAverage = urgentTotal / urgentCount;
    long normalAverage = normalTotal / normalCount;

    ui.displayReportLine("Average wait - urgent guests (served)", urgentAverage + " minutes");
    ui.displayReportLine("Average wait - normal guests (served)", normalAverage + " minutes");
    ui.displayReportLine("Time saved by the override",
        (normalAverage - urgentAverage) + " minutes on average");
  }

  // ==================================================================
  // HELPERS
  // ==================================================================

  private String percentOf(int part, int whole) {
    if (whole == 0) {
      return "";
    }
    return String.format("  (%.1f%%)", (part * 100.0) / whole);
  }

  private String truncate(String text, int width) {
    if (text == null) {
      return "-";
    }
    return (text.length() <= width) ? text : text.substring(0, width - 1) + ".";
  }

  private String truncateTilde(String text, int width) {
    if (text == null) {
      return "-";
    }
    return (text.length() <= width) ? text : text.substring(0, width - 1) + "~";
  }
}
