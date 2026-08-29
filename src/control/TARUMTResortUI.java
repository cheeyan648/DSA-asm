package control;

import adt.ListInterface;
import entity.Booking;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Invoice;
import entity.Member;
import entity.Room;
import entity.Staff;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.util.Scanner;
import utility.MessageUI;

/**
 * The way into the system, and the thing that makes it one system.
 *
 * Every table is loaded once here and the same registry is handed to all four
 * modules. That single decision is what integrates them: previously each
 * module built its own copy of the data in its own constructor, so a room the
 * front desk had just sold was still vacant as far as housekeeping knew, and a
 * guest served at the walk-in counter never became a booking.
 */
public class TARUMTResortUI {

  private final Scanner scanner = MessageUI.scanner;

  /** The one copy of every table, shared by all four modules. */
  private final ResortData data;

  /** The operations that span more than one module. */
  private final ResortService service;

  private final WalkInRegistrationMaintenance walkIn;
  private final FrontDeskServiceMaintenance frontDesk;
  private final HousekeepingTaskLogMaintenance housekeeping;
  private final LoyaltyRewardsMaintenance loyalty;

  /** Who is signed in. Recorded against everything done this session. */
  private String staffId;

  public TARUMTResortUI() {
    data = new ResortData();
    service = new ResortService(data);

    staffId = onDutyStaffId();

    walkIn = new WalkInRegistrationMaintenance(service, staffId);
    frontDesk = new FrontDeskServiceMaintenance(service, staffId);
    housekeeping = new HousekeepingTaskLogMaintenance(service, staffId);
    loyalty = new LoyaltyRewardsMaintenance(service, staffId);
  }

  /**
   * The staff member every action this session is recorded against.
   *
   * Actions still have to name somebody - the reports say who granted an
   * urgency override or signed off an inspection, and a blank there would make
   * them worthless - but the system no longer stops to ask on the way in. The
   * officer rostered first is taken as the one on duty, and the home screen
   * shows who that is.
   *
   * @return the staff ID on duty, or "-" if no staff are on record
   */
  private String onDutyStaffId() {
    ListInterface<Staff> staffList = data.getStaffList();
    if (staffList.isEmpty()) {
      return "-";
    }
    return staffList.getEntry(1).getStaffId();
  }

  // ==================================================================
  // MAIN MENU
  // ==================================================================

  /**
   * Draws the home page, with a live summary of what needs attention.
   *
   * The counts come from all four modules at once, which is only possible
   * because they now share their data - and they are the quickest way to show
   * that the system is genuinely joined up.
   */
  public int getMenuChoice() {
    MessageUI.clearScreen();
    MessageUI.displayBlankLine();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred(
        "TUNKU ABDUL RAHMAN UNIVERSITY OF MANAGEMENT AND TECHNOLOGY");
    MessageUI.displayBoxBlank();

    MessageUI.displayBanner("TARUMT");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("R E S O R T   M A N A G E M E N T   S Y S T E M");
    MessageUI.displayBoxBlank();

    MessageUI.displayBoxDivider();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxLine("  MAIN MENU");
    MessageUI.displayBoxLine("  Select a subsystem to continue.");
    MessageUI.displayBoxBlank();

    MessageUI.displaySubsystemOption(1, "Walk-In Registration & Standard Booking",
        "Who is waiting, and whose turn it is");
    MessageUI.displaySubsystemOption(2, "Housekeeping Task Log",
        "Whether a room may be given to anybody");
    MessageUI.displaySubsystemOption(3, "Front-Desk Service",
        "Bookings, rooms and the money");
    MessageUI.displaySubsystemOption(4, "Loyalty & Rewards",
        "Points, tiers and redemptions");

    MessageUI.displayMenuOption(0, "Quit the system");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxBottom();

    return MessageUI.readMenuChoice(scanner, 4, "exit");
  }

  /**
   * Draws the closing screen, framed to match the home page so the system
   * opens and closes the same way.
   */
  private void displayExitScreen() {
    MessageUI.clearScreen();
    MessageUI.displayBlankLine();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("THANK YOU FOR USING");
    MessageUI.displayBoxBlank();
    MessageUI.displayBanner("TARUMT");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("R E S O R T   M A N A G E M E N T   S Y S T E M");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  All records have been saved.");
    MessageUI.displayBoxCentred("Have a pleasant day.");
    MessageUI.displayBoxBottom();
    MessageUI.displayBlankLine();
  }

  // ==================================================================
  // RESORT OVERVIEW
  // ==================================================================

  /**
   * Everything happening across the resort, on one screen.
   *
   * This exists because no single module can show it. Each section is drawn
   * from a different module's data, and the last one traces a guest all the way
   * through - which is the clearest demonstration that the four are joined.
   */
  private void displayOverview() {
    MessageUI.clearScreen();
    MessageUI.displayBlankLine();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("RESORT OVERVIEW");
    MessageUI.displayBoxCentred("Every module, right now");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxBottom();

    displayFrontDoor();
    displayRoomSummary();
    displayMoneySummary();
    displayLoyaltySummary();
    displayGuestJourney();

    MessageUI.displayBlankLine();
    MessageUI.displayRule();
    MessageUI.displayMessage("  END OF OVERVIEW");
    MessageUI.displayRule();
    MessageUI.pause(scanner);
  }

  /** Who is at the door and who is waiting to be cleaned for. */
  private void displayFrontDoor() {
    MessageUI.displaySectionHeading("At the front door  (Walk-In Registration)");

    int waiting = data.getWaitingList().getNumberOfEntries();
    MessageUI.displayReportLine("Guests waiting", String.valueOf(waiting));
    MessageUI.displayReportLine("  In the urgent lane",
        String.valueOf(data.getWaitingList().getUrgentCount()));
    MessageUI.displayReportLine("  In the normal lane",
        String.valueOf(data.getWaitingList().getNormalCount()));

    WalkInRegistration next = data.getWaitingList().peekNext();
    if (next != null) {
      Guest guest = data.findGuest(next.getGuestId());
      MessageUI.displayReportLine("Next to be called", String.format("%s  (%s, waited %s)",
          guest == null ? "-" : guest.getFullName(),
          next.getPriority(), next.getFormattedWaitingTime()));
    }
  }

  /** What housekeeping has outstanding and what the front desk can sell. */
  private void displayRoomSummary() {
    MessageUI.displaySectionHeading("Rooms  (Housekeeping and Front Desk together)");

    ListInterface<Room> rooms = data.getRoomList();
    MessageUI.displayReportLine("Rooms in the resort",
        String.valueOf(rooms.getNumberOfEntries()));
    MessageUI.displayReportLine("Sellable right now  (vacant AND cleaned)",
        String.valueOf(rooms.countIf(Room::isAssignable)));
    MessageUI.displayReportLine("Occupied",
        String.valueOf(rooms.countIf(r -> Room.OCCUPIED.equals(r.getOccupancyStatus()))));
    MessageUI.displayReportLine("Waiting to be cleaned",
        String.valueOf(data.getCleaningQueue().getNumberOfEntries())
            + "  (" + data.getCleaningQueue().getUrgentCount() + " urgent)");
    MessageUI.displayReportLine("Out of service",
        String.valueOf(rooms.countIf(Room::isOutOfService)));

    HousekeepingTask nextClean = data.getCleaningQueue().peekNext();
    if (nextClean != null) {
      MessageUI.displayReportLine("Next room to clean", String.format("%s  (%s lane%s)",
          nextClean.getRoomNo(), nextClean.getPriority(),
          nextClean.getReservedForBookingId() == null ? ""
              : ", " + nextClean.getReservedForBookingId() + " waiting on it"));
    }
  }

  /** What has been billed and what is still owed. */
  private void displayMoneySummary() {
    MessageUI.displaySectionHeading("Money  (Front Desk)");

    ListInterface<Invoice> invoices = data.getInvoiceList();
    double billed = 0;
    double collected = 0;

    for (int i = 1; i <= invoices.getNumberOfEntries(); i++) {
      billed += invoices.getEntry(i).getTotalAmount();
      collected += invoices.getEntry(i).getAmountPaid();
    }

    MessageUI.displayReportLine("Invoices raised",
        String.valueOf(invoices.getNumberOfEntries()));
    MessageUI.displayReportLine("Total billed", String.format("RM%.2f", billed));
    MessageUI.displayReportLine("Total collected", String.format("RM%.2f", collected));
    MessageUI.displayReportLine("Outstanding", String.format("RM%.2f", billed - collected));
    MessageUI.displayReportLine("Unsettled invoices",
        String.valueOf(invoices.countIf(invoice -> !invoice.isSettled())));
  }

  /** Where the loyalty programme stands. */
  private void displayLoyaltySummary() {
    MessageUI.displaySectionHeading("Loyalty  (Loyalty & Rewards)");

    ListInterface<Member> members = data.getMemberList();
    int points = 0;
    for (int i = 1; i <= members.getNumberOfEntries(); i++) {
      points += members.getEntry(i).getPointsBalance();
    }

    MessageUI.displayReportLine("Members", String.valueOf(members.getNumberOfEntries()));
    MessageUI.displayReportLine("Points outstanding", String.valueOf(points));
    MessageUI.displayReportLine("Redemptions awaiting a decision",
        String.valueOf(data.getPendingRedemptions().getNumberOfEntries()));

    LocalDate today = LocalDate.now();
    MessageUI.displayReportLine("Members with points expiring soon",
        String.valueOf(members.countIf(member -> member.hasExpiringPoints(today))));
  }

  /**
   * Traces one guest through every module.
   *
   * A booking that came from a walk-in is followed back to the queue it came
   * from and forward to the room being cleaned for it - which is the whole
   * integration in a single paragraph.
   */
  private void displayGuestJourney() {
    MessageUI.displaySectionHeading("A guest's journey, end to end");

    // The most interesting case is one still in flight - a walk-in booking
    // that has not yet got its room.
    Booking inFlight = data.getBookingList().search(
        booking -> Booking.SOURCE_WALK_IN.equals(booking.getSource())
            && Booking.STATUS_PENDING.equals(booking.getBookingStatus()));

    final Booking traced = (inFlight != null) ? inFlight
        : data.getBookingList().search(
            booking -> Booking.SOURCE_WALK_IN.equals(booking.getSource()));

    if (traced == null) {
      MessageUI.displayMessage("  No walk-in has become a booking yet.");
      return;
    }

    Guest guest = data.findGuest(traced.getGuestId());
    MessageUI.displayMessage("  Following " + (guest == null ? traced.getGuestId()
        : guest.getFullName()) + ":");
    MessageUI.displayBlankLine();

    WalkInRegistration reg = data.findRegistration(traced.getRegId());
    if (reg != null) {
      MessageUI.displayMessage(String.format(
          "    [M1]  Walked in at %s, %s lane, waited %s",
          reg.getFormattedArrivalTime(), reg.getPriority(),
          reg.getFormattedWaitingTime()));
      if (reg.isUrgent()) {
        MessageUI.displayMessage("          Urgency reason: " + reg.getUrgencyReason());
      }
      MessageUI.displayMessage("    [M1]  Called to the counter, status " + reg.getStatus());
    }

    MessageUI.displayMessage(String.format(
        "    [M3]  Became booking %s  (%s, %s priority)",
        traced.getBookingId(), traced.getBookingStatus(), traced.getPriority()));

    if (traced.getRoomNo() == null) {
      MessageUI.displayMessage("    [M3]  No room assigned yet - waiting on housekeeping");

      HousekeepingTask waitingOn = data.getTaskList().search(
          task -> traced.getBookingId().equals(task.getReservedForBookingId()));

      if (waitingOn != null) {
        MessageUI.displayMessage(String.format(
            "    [M2]  Room %s is being cleaned for it - %s lane, currently %s",
            waitingOn.getRoomNo(), waitingOn.getPriority(), waitingOn.getStatus()));
        MessageUI.displayMessage(
            "    [M2]  The booking is URGENT, which is why this room jumped the queue");
      }
    } else {
      MessageUI.displayMessage("    [M3]  Room " + traced.getRoomNo() + " assigned");

      Invoice invoice = data.findInvoiceByBooking(traced.getBookingId());
      if (invoice != null) {
        MessageUI.displayMessage(String.format(
            "    [M3]  Invoice %s - RM%.2f, %s",
            invoice.getInvoiceId(), invoice.getTotalAmount(), invoice.getPaymentStatus()));
      }
    }

    Member member = data.findMemberByGuest(traced.getGuestId());
    if (member != null) {
      MessageUI.displayMessage(String.format(
          "    [M4]  Loyalty member %s, %s tier, %d points",
          member.getMemberId(), member.getTier(), member.getPointsBalance()));
    } else {
      MessageUI.displayMessage("    [M4]  Not a loyalty member");
    }
  }

  // ==================================================================
  // RUNNING
  // ==================================================================

  public void runTARUMTResort() {
    int choice;
    do {
      choice = getMenuChoice();
      switch (choice) {
        case 0:
          // Everything is written before the screen is drawn, so nothing is
          // lost if the window is closed as soon as it appears.
          data.saveAll();
          displayExitScreen();
          break;
        case 1:
          walkIn.run();
          break;
        case 2:
          housekeeping.run();
          break;
        case 3:
          frontDesk.run();
          break;
        case 4:
          loyalty.run();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  public static void main(String[] args) {
    TARUMTResortUI tarumtResortUI = new TARUMTResortUI();
    tarumtResortUI.runTARUMTResort();
  }
}
