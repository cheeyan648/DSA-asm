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

/**
 * Walk-In Registration - who is waiting, and whose turn it is.
 *
 * This module answers one question: which guest should be served next, and is
 * that fair. It knows nothing about rooms, dates or money; the moment a guest
 * is called it hands them to the front desk, which is the module that turns a
 * person at the counter into a booking.
 *
 * The fairness problem it exists to solve: serving strictly in arrival order
 * is unfair to an elderly guest or one with a medical need, but letting staff
 * reorder the queue freely invites abuse. The answer is to allow the exception
 * and force a reason to be recorded, so every override can be reviewed.
 *
 * @author Tan Chee Yan
 */
public class WalkInRegistrationMaintenance {

  private final WalkInRegistrationUI ui = new WalkInRegistrationUI();
  private final ResortData data;
  private final ResortService service;

  /**
   * The registrations made in this session, newest on top.
   *
   * Not saved to file: an undo only makes sense to the officer who made the
   * mistake, in the session they made it. Restoring it on the next run would
   * let somebody undo a registration made hours earlier by somebody else.
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
          displayQueue();
          ui.pause();
          break;
        case 3:
          cancelWaitingGuest();
          break;
        case 4:
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
          searchByRegistrationId();
          break;
        case 2:
          searchByName();
          break;
        case 3:
          filterByStatus();
          break;
        case 4:
          filterByPriority();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runSortMenu() {
    int choice;
    do {
      choice = ui.getSortMenuChoice();
      if (choice >= 1 && choice <= 4) {
        displaySorted(choice);
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

  /**
   * Registers a guest who has just walked in.
   *
   * The guest is looked up by their identity document first, so a returning
   * visitor keeps the record - and the loyalty membership - they already have,
   * rather than becoming a second version of themselves.
   *
   * @param urgent whether this is an exception case that jumps the queue
   */
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

    // One person cannot hold two places in the line at once.
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
        reason, typeId, nights);

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
   * How many guests will be called before this one.
   *
   * Worked out from the queue rather than stored, because it changes every
   * time anyone else is served.
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

  /**
   * Takes back the most recent registration made in this session.
   *
   * Only a guest who is still waiting can be undone. Once they have been
   * called they are no longer in the queue and the front desk may already have
   * started work on their booking, so removing the record would leave that
   * work pointing at nothing.
   */
  private void undoLastRegistration() {
    ui.startAction("UNDO LAST REGISTRATION");

    if (undoStack.isEmpty()) {
      ui.displayError("Nothing has been registered in this session to undo.");
      ui.pause();
      return;
    }

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

    undoStack.pop();
    data.getWaitingList().removeEntry(last);
    data.getRegistrationList().removeEntry(last);
    data.saveRegistrations();

    // The guest record is deliberately kept: the person still exists even if
    // this registration was keyed in by mistake.
    ui.displaySuccess("Registration " + last.getRegId() + " removed.");
    ui.displayMessage("  The guest record has been kept.");
    ui.pause();
  }

  // ==================================================================
  // QUEUE
  // ==================================================================

  /**
   * Calls the next guest and hands them to the front desk.
   *
   * This is the handover that makes the two modules one system: the guest
   * leaves the queue here, and a booking is created for them there, carrying
   * the urgency they were granted at the door.
   */
  private void serveNextGuest() {
    ui.startAction("SERVE NEXT GUEST");

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

    // Calling a guest cannot be undone from this module, so it is confirmed
    // before it happens.
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
    ui.displayMessage("  They have left the waiting list.");
    ui.displayMessage("  Guests still waiting: "
        + data.getWaitingList().getNumberOfEntries());
    ui.displayMessage("");

    if (!ui.confirm("Create their booking now?")) {
      ui.displayMessage("  The guest stays IN_SERVICE and can be booked in from Front Desk.");
      ui.pause();
      return;
    }

    createBookingForGuest(called.getValue());
    ui.pause();
  }

  /**
   * Turns a called guest into a booking, and finds them a room.
   *
   * Everything from here on belongs to the front desk and housekeeping - this
   * module only starts it off, then reports back what happened.
   */
  private void createBookingForGuest(WalkInRegistration reg) {
    LocalDate checkIn = LocalDate.now();
    LocalDate checkOut = checkIn.plusDays(reg.getRequestedNights());

    RoomType type = data.findRoomType(reg.getRequestedTypeId());
    int maxGuests = (type == null) ? 4 : type.getMaxOccupancy();

    int guests = utility.MessageUI.readInt(utility.MessageUI.scanner,
        "Number of guests staying", 1, maxGuests);
    if (guests == utility.MessageUI.CANCELLED_INT) {
      ui.displayMessage("  Booking cancelled. The guest remains IN_SERVICE.");
      return;
    }

    ServiceResult<entity.Booking> booked = service.convertRegistrationToBooking(
        reg.getRegId(), checkIn, checkOut, guests, staffId);
    if (booked.isFailure()) {
      ui.displayError(booked.getMessage());
      return;
    }

    ui.displaySuccess(booked.getMessage());
    entity.Booking booking = booked.getValue();

    // Housekeeping decides what may be given out; the front desk only asks.
    ListInterface<entity.Room> ready = service.findAvailableRooms(
        booking.getTypeId(), checkIn, checkOut);

    if (!ready.isEmpty()) {
      entity.Room room = ready.getEntry(1);
      ServiceResult<entity.Booking> assigned = service.assignRoom(
          booking.getBookingId(), room.getRoomNo(), staffId,
          entity.RoomAssignment.REASON_INITIAL);

      if (assigned.isSuccess()) {
        ui.displaySuccess(assigned.getMessage());
      } else {
        ui.displayError(assigned.getMessage());
      }
      return;
    }

    // Nothing is ready. An urgent guest can have a room cleaned out of turn
    // rather than being turned away.
    ui.displayMessage("");
    ui.displayError("No " + reg.getRequestedTypeId()
        + " room is ready for check-in right now.");

    if (booking.isUrgent()) {
      ServiceResult<entity.HousekeepingTask> expedited =
          service.requestUrgentCleaning(booking.getBookingId(), staffId);

      if (expedited.isSuccess()) {
        ui.displaySuccess(expedited.getMessage());
        ui.displayMessage("  The booking stays PENDING until the room is ready.");
      } else {
        ui.displayError(expedited.getMessage());
      }
      return;
    }

    ListInterface<entity.Room> cleanable = service.findCleanableRooms(
        booking.getTypeId(), checkIn, checkOut);
    if (cleanable.isEmpty()) {
      ui.displayMessage("  No room of that type can be made ready. Offer another type.");
    } else {
      ui.displayMessage("  " + cleanable.getNumberOfEntries()
          + " room(s) of that type are being cleaned. The booking stays PENDING.");
    }
  }

  /** Shows who is waiting, in the order they will be called. */
  private boolean displayQueue() {
    ui.startAction("CURRENT WALK-IN QUEUE");
    return ui.displayQueue(data.getWaitingList().toServiceOrder(), data,
        data.getWaitingList().getUrgentCount(),
        data.getWaitingList().getNormalCount());
  }

  /** Removes a guest who has given up waiting and left. */
  private void cancelWaitingGuest() {
    ui.startAction("CANCEL A WAITING GUEST");

    if (!displayQueueRows()) {
      ui.pause();
      return;
    }

    String regId = ui.inputRegistrationId();
    if (regId == null) {
      ui.displayMessage("  Cancelled.");
      ui.pause();
      return;
    }

    WalkInRegistration reg = data.findRegistration(regId);
    if (reg == null) {
      ui.displayError("No registration with ID " + regId + ".");
      ui.pause();
      return;
    }
    if (!reg.isWaiting()) {
      ui.displayError("Only a WAITING guest can be cancelled - " + regId
          + " is " + reg.getStatus() + ".");
      ui.pause();
      return;
    }

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

    reg.setStatus(WalkInRegistration.STATUS_CANCELLED);
    reg.setServedBy(staffId);
    data.getWaitingList().removeEntry(reg);
    data.saveRegistrations();

    ui.displaySuccess(regId + " cancelled. Guests still waiting: "
        + data.getWaitingList().getNumberOfEntries());
    ui.pause();
  }

  /**
   * Records that a guest who was called never came forward.
   *
   * Kept separate from a cancellation: a guest who left before being called
   * and one who was called and did not appear are different things, and the
   * queue report counts them separately.
   */
  private void markNoShow() {
    ui.startAction("MARK A CALLED GUEST AS NO-SHOW");

    ListInterface<WalkInRegistration> inService = data.getRegistrationList().filter(
        reg -> WalkInRegistration.STATUS_IN_SERVICE.equals(reg.getStatus()));

    if (!ui.displayRegistrationList(inService, data,
        "No guest has been called and is still waiting to be dealt with.")) {
      ui.pause();
      return;
    }

    String regId = ui.inputRegistrationId();
    if (regId == null) {
      ui.displayMessage("  Cancelled.");
      ui.pause();
      return;
    }

    WalkInRegistration reg = data.findRegistration(regId);
    if (reg == null || !WalkInRegistration.STATUS_IN_SERVICE.equals(reg.getStatus())) {
      ui.displayError("Only a guest who has been called can be marked as a no-show.");
      ui.pause();
      return;
    }

    if (!ui.confirm("Mark " + regId + " as a no-show?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    reg.setStatus(WalkInRegistration.STATUS_NO_SHOW);
    data.saveRegistrations();

    // They are not put back in the queue: they had their turn.
    ui.displaySuccess(regId + " recorded as a no-show.");
    ui.pause();
  }

  /** Draws the queue table without the action header. */
  private boolean displayQueueRows() {
    return ui.displayQueue(data.getWaitingList().toServiceOrder(), data,
        data.getWaitingList().getUrgentCount(),
        data.getWaitingList().getNormalCount());
  }

  // ==================================================================
  // SEARCH AND SORT
  // ==================================================================

  private void searchByRegistrationId() {
    ui.startAction("SEARCH BY REGISTRATION ID");

    String regId = ui.inputRegistrationId();
    if (regId == null) {
      return;
    }

    WalkInRegistration reg = data.findRegistration(regId);
    if (reg == null) {
      ui.displayError("No registration with ID " + regId + ".");
      ui.pause();
      return;
    }

    Guest guest = data.findGuest(reg.getGuestId());
    RoomType type = data.findRoomType(reg.getRequestedTypeId());
    ui.displayRegistration(reg, guest == null ? "-" : guest.getFullName(),
        type == null ? reg.getRequestedTypeId() : type.getTypeName());
    ui.pause();
  }

  /** Finds registrations by part of the guest's name. */
  private void searchByName() {
    ui.startAction("SEARCH BY GUEST NAME");

    String term = ui.inputSearchName();
    if (term == null) {
      return;
    }

    final String lower = term.toLowerCase();
    ListInterface<WalkInRegistration> matches = data.getRegistrationList().filter(reg -> {
      Guest guest = data.findGuest(reg.getGuestId());
      return guest != null && guest.getFullName().toLowerCase().contains(lower);
    });

    ui.displayRegistrationList(matches, data, "No guest matched \"" + term + "\".");
    ui.pause();
  }

  private void filterByStatus() {
    ui.startAction("FILTER BY STATUS");

    String status = ui.inputStatusFilter();
    if (status == null) {
      return;
    }

    ListInterface<WalkInRegistration> matches =
        data.getRegistrationList().filter(reg -> status.equals(reg.getStatus()));
    ui.displayRegistrationList(matches, data, "No registration is " + status + ".");
    ui.pause();
  }

  private void filterByPriority() {
    ui.startAction("FILTER BY PRIORITY");

    String priority = ui.inputPriorityFilter();
    if (priority == null) {
      return;
    }

    ListInterface<WalkInRegistration> matches =
        data.getRegistrationList().filter(reg -> priority.equals(reg.getPriority()));
    ui.displayRegistrationList(matches, data, "No " + priority + " registration found.");
    ui.pause();
  }

  /**
   * Lists the registrations in a chosen order.
   *
   * The fourth option is the one worth reading: it sorts by the order guests
   * will actually be called, which is not the same as arrival order, and shows
   * why at a glance.
   */
  private void displaySorted(int choice) {
    ListInterface<WalkInRegistration> sorted = copyOf(data.getRegistrationList());
    String title;

    switch (choice) {
      case 1:
        sorted.sort(Comparator.comparing(WalkInRegistration::getArrivalTime));
        title = "SORTED BY ARRIVAL TIME";
        break;

      case 2:
        sorted.sort((a, b) -> {
          Guest first = data.findGuest(a.getGuestId());
          Guest second = data.findGuest(b.getGuestId());
          String nameA = (first == null) ? "" : first.getFullName();
          String nameB = (second == null) ? "" : second.getFullName();
          return nameA.compareToIgnoreCase(nameB);
        });
        title = "SORTED BY GUEST NAME";
        break;

      case 3:
        sorted.sort(Comparator.comparingLong(WalkInRegistration::getWaitingMinutes).reversed());
        title = "SORTED BY WAITING TIME";
        break;

      default:
        // Urgent before normal, and earliest arrival within each - exactly
        // what the queue itself will do.
        sorted = data.getRegistrationList().filter(reg -> reg.isWaiting());
        sorted.sort(Comparator
            .comparing((WalkInRegistration reg) -> reg.isUrgent() ? 0 : 1)
            .thenComparing(WalkInRegistration::getArrivalTime));
        title = "SORTED BY SERVICE ORDER";
        break;
    }

    ui.startAction(title);
    ui.displayRegistrationList(sorted, data, "There are no registrations.");
    ui.pause();
  }

  /** A copy of a list, so sorting a listing does not reorder the records. */
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

  /**
   * How the queue has performed - how many were served, how long they waited,
   * and when the busy hours were.
   */
  private void queuePerformanceReport() {
    ui.displayReportHeader("QUEUE PERFORMANCE ANALYSIS REPORT");

    ListInterface<WalkInRegistration> all = data.getRegistrationList();
    if (all.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  There are no registrations to analyse.");
      ui.pause();
      return;
    }

    int total = all.getNumberOfEntries();
    int urgent = all.countIf(WalkInRegistration::isUrgent);
    int waiting = all.countIf(WalkInRegistration::isWaiting);
    int booked = all.countIf(
        reg -> WalkInRegistration.STATUS_BOOKED.equals(reg.getStatus()));
    int cancelled = all.countIf(
        reg -> WalkInRegistration.STATUS_CANCELLED.equals(reg.getStatus()));
    int noShow = all.countIf(
        reg -> WalkInRegistration.STATUS_NO_SHOW.equals(reg.getStatus()));

    ui.displaySectionHeading("Registrations");
    ui.displayReportLine("Total registrations", String.valueOf(total));
    ui.displayReportLine("  Urgent", urgent + percentOf(urgent, total));
    ui.displayReportLine("  Normal", (total - urgent) + percentOf(total - urgent, total));
    ui.displayReportLine("Still waiting", String.valueOf(waiting));
    ui.displayReportLine("Became a booking", String.valueOf(booked));
    ui.displayReportLine("Cancelled", String.valueOf(cancelled));
    ui.displayReportLine("No-show", String.valueOf(noShow));

    // Only guests who have been called have a finished wait to measure.
    ListInterface<WalkInRegistration> called =
        all.filter(reg -> reg.getCalledAt() != null);

    ui.displaySectionHeading("Waiting times");
    if (called.isEmpty()) {
      ui.displayMessage("  No guest has been called yet.");
    } else {
      long totalWait = 0;
      long longest = 0;
      long shortest = Long.MAX_VALUE;
      WalkInRegistration longestWaiter = null;

      long urgentWait = 0;
      int urgentCalled = 0;
      long normalWait = 0;
      int normalCalled = 0;

      for (int i = 1; i <= called.getNumberOfEntries(); i++) {
        WalkInRegistration reg = called.getEntry(i);
        long wait = reg.getWaitingMinutes();
        totalWait += wait;

        if (wait > longest) {
          longest = wait;
          longestWaiter = reg;
        }
        if (wait < shortest) {
          shortest = wait;
        }
        if (reg.isUrgent()) {
          urgentWait += wait;
          urgentCalled++;
        } else {
          normalWait += wait;
          normalCalled++;
        }
      }

      int count = called.getNumberOfEntries();
      ui.displayReportLine("Guests called", String.valueOf(count));
      ui.displayReportLine("Average wait", (totalWait / count) + " min");
      ui.displayReportLine("  Urgent lane average",
          urgentCalled == 0 ? "-" : (urgentWait / urgentCalled) + " min");
      ui.displayReportLine("  Normal lane average",
          normalCalled == 0 ? "-" : (normalWait / normalCalled) + " min");
      ui.displayReportLine("Longest wait", longest + " min"
          + (longestWaiter == null ? "" : "  (" + longestWaiter.getRegId() + ")"));
      ui.displayReportLine("Shortest wait", shortest + " min");
    }

    ui.displaySectionHeading("Outcome");
    ui.displayReportLine("Conversion rate (registration to booking)",
        percentValue(booked, total));

    displayArrivalsByHour(all);

    ui.displayReportFooter();
    ui.pause();
  }

  /**
   * Charts when guests arrive, so the busy hours can be staffed.
   *
   * Only the hours that actually saw an arrival are charted - a full
   * twenty-four-column chart would be mostly empty and much harder to read.
   */
  private void displayArrivalsByHour(ListInterface<WalkInRegistration> all) {
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

    int span = last - first + 1;
    String[] labels = new String[span];
    double[] values = new double[span];
    int busiest = first;

    for (int i = 0; i < span; i++) {
      int hour = first + i;
      labels[i] = String.format("%02d", hour);
      values[i] = byHour[hour];
      if (byHour[hour] > byHour[busiest]) {
        busiest = hour;
      }
    }

    ui.displayBarChart("Arrivals by hour", "Guests", labels, values);
    ui.displayReportLine("Peak arrival hour",
        String.format("%02d:00  (%d guests)", busiest, byHour[busiest]));
  }

  /**
   * Every use of the urgent flag, and who granted it.
   *
   * An urgent guest is served ahead of people who have waited longer, so this
   * report exists to make that power reviewable rather than invisible.
   */
  private void urgencyAuditReport() {
    ui.displayReportHeader("URGENCY EXCEPTION AUDIT REPORT");

    ListInterface<WalkInRegistration> all = data.getRegistrationList();
    ListInterface<WalkInRegistration> urgent = all.filter(WalkInRegistration::isUrgent);

    ui.displaySectionHeading("Summary");
    ui.displayReportLine("Total registrations", String.valueOf(all.getNumberOfEntries()));
    ui.displayReportLine("Urgent exceptions", String.valueOf(urgent.getNumberOfEntries()));
    ui.displayReportLine("Urgent share",
        percentValue(urgent.getNumberOfEntries(), all.getNumberOfEntries()));

    if (urgent.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  No urgency override has been used.");
      ui.displayReportFooter();
      ui.pause();
      return;
    }

    ui.displaySectionHeading("Every urgent registration");
    utility.MessageUI.displayTableHeading(String.format("  %-7s %-22s %-8s %-9s %s",
        "REG ID", "GUEST", "BY", "WAITED", "REASON"));

    int incomplete = 0;
    for (int i = 1; i <= urgent.getNumberOfEntries(); i++) {
      WalkInRegistration reg = urgent.getEntry(i);
      Guest guest = data.findGuest(reg.getGuestId());
      String reason = reg.getUrgencyReason();

      // A blank or one-word reason cannot be reviewed, so it is flagged.
      if (reason == null || reason.isBlank() || !reason.trim().contains(" ")) {
        incomplete++;
        reason = (reason == null || reason.isBlank()) ? "(none given)" : reason + "  <-- vague";
      }

      System.out.printf("  %-7s %-22s %-8s %-9s %s%n",
          reg.getRegId(),
          guest == null ? "-" : truncate(guest.getFullName(), 22),
          reg.getServedBy() == null ? "-" : reg.getServedBy(),
          reg.getFormattedWaitingTime(), reason);
    }
    utility.MessageUI.displayThinRule();

    displayUrgencyByOfficer(urgent);

    ui.displaySectionHeading("Data quality");
    ui.displayReportLine("Incomplete or vague reasons", String.valueOf(incomplete));

    displayWaitSaved(all);

    ui.displayReportFooter();
    ui.pause();
  }

  /**
   * How many overrides each officer has granted.
   *
   * A single officer well above the others is worth a supervisor's attention,
   * which is the whole point of counting them.
   */
  private void displayUrgencyByOfficer(ListInterface<WalkInRegistration> urgent) {
    ListInterface<String> officers = new ArrayList<>();
    ListInterface<Integer> counts = new ArrayList<>();

    for (int i = 1; i <= urgent.getNumberOfEntries(); i++) {
      String officer = urgent.getEntry(i).getServedBy();
      if (officer == null) {
        officer = "(not recorded)";
      }

      int position = officers.getPosition(officer);
      if (position < 0) {
        officers.add(officer);
        counts.add(1);
      } else {
        counts.replace(position, counts.getEntry(position) + 1);
      }
    }

    if (officers.isEmpty()) {
      return;
    }

    String[] labels = new String[officers.getNumberOfEntries()];
    double[] values = new double[officers.getNumberOfEntries()];
    for (int i = 1; i <= officers.getNumberOfEntries(); i++) {
      labels[i - 1] = officers.getEntry(i);
      values[i - 1] = counts.getEntry(i);
    }

    ui.displayBarChart("Urgent registrations by officer", "Count", labels, values);
  }

  /**
   * How much time the urgent lane actually saved the guests who used it.
   *
   * Comparing the two averages is what turns "we granted five overrides" into
   * a statement about their effect.
   */
  private void displayWaitSaved(ListInterface<WalkInRegistration> all) {
    ListInterface<WalkInRegistration> called = all.filter(reg -> reg.getCalledAt() != null);
    if (called.isEmpty()) {
      return;
    }

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

    if (urgentCount == 0 || normalCount == 0) {
      return;
    }

    long urgentAverage = urgentTotal / urgentCount;
    long normalAverage = normalTotal / normalCount;

    ui.displaySectionHeading("Effect of the override");
    ui.displayReportLine("Average wait, urgent lane", urgentAverage + " min");
    ui.displayReportLine("Average wait, normal lane", normalAverage + " min");
    ui.displayReportLine("Time saved by the override",
        (normalAverage - urgentAverage) + " min");
  }

  // ==================================================================
  // HELPERS
  // ==================================================================

  /** A count with its share in brackets, e.g. "3  (42.9%)". */
  private String percentOf(int part, int whole) {
    if (whole == 0) {
      return "";
    }
    return String.format("  (%.1f%%)", (part * 100.0) / whole);
  }

  /** A share on its own, e.g. "42.9%". */
  private String percentValue(int part, int whole) {
    if (whole == 0) {
      return "-";
    }
    return String.format("%.1f%%", (part * 100.0) / whole);
  }

  private String truncate(String text, int width) {
    if (text == null) {
      return "-";
    }
    return (text.length() <= width) ? text : text.substring(0, width - 1) + ".";
  }
}
