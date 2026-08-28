package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.LoyaltyRewardsUI;
import entity.Booking;
import entity.Guest;
import entity.Member;
import entity.Notification;
import entity.PointTransaction;
import entity.Redemption;
import entity.Reward;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import utility.MessageUI;

/**
 * Loyalty & Rewards - the reason a guest comes back.
 *
 * Points are earned from stays the front desk has completed and spent on
 * rewards, which can then be taken off a live bill. That makes this module the
 * end of the guest journey and the start of the next one.
 *
 * Two rules shape everything here. Tier comes from lifetime points rather than
 * the spendable balance, so redeeming never costs a member their standing. And
 * points are deducted only when a request is approved, so a refusal costs the
 * member nothing - which is why eligibility is checked when the request is
 * processed rather than when it is made.
 *
 * @author Ivan Wong
 */
public class LoyaltyRewardsMaintenance {

  private final LoyaltyRewardsUI ui = new LoyaltyRewardsUI();
  private final ResortData data;
  private final ResortService service;
  private final String staffId;

  public LoyaltyRewardsMaintenance(ResortService service, String staffId) {
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
          runMemberMenu();
          break;
        case 2:
          runPointsMenu();
          break;
        case 3:
          runRewardMenu();
          break;
        case 4:
          runNotificationMenu();
          break;
        case 5:
          runReportMenu();
          break;
        default:
          break;
      }
    } while (choice != 0);

    data.saveLoyalty();
  }

  private void runMemberMenu() {
    int choice;
    do {
      choice = ui.getMemberMenuChoice();
      switch (choice) {
        case 1:
          enrolMember();
          break;
        case 2:
          searchMember();
          break;
        case 3:
          displayAllMembers();
          break;
        case 4:
          displayMembersByPoints();
          break;
        case 5:
          displayExpiringPoints();
          break;
        case 6:
          displayNearNextTier();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runPointsMenu() {
    int choice;
    do {
      choice = ui.getPointsMenuChoice();
      switch (choice) {
        case 1:
          awardPointsForStay();
          break;
        case 2:
          adjustPoints();
          break;
        case 3:
          expirePoints();
          break;
        case 4:
          displayLedger();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runRewardMenu() {
    int choice;
    do {
      choice = ui.getRewardMenuChoice();
      switch (choice) {
        case 1:
          displayCatalogue();
          break;
        case 2:
          addNewReward();
          break;
        case 3:
          deleteRewardMenu();
          break;
        case 4:
          restockReward();
          break;
        case 5:
          requestRedemption();
          break;
        case 6:
          processNextRedemption();
          break;
        case 7:
          displayPendingQueue();
          break;
        case 8:
          displayRedemptionHistory();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runNotificationMenu() {
    int choice;
    do {
      choice = ui.getNotificationMenuChoice();
      switch (choice) {
        case 1:
          displayNotifications();
          break;
        case 2:
          displayPromotion();
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
          membershipReport();
          break;
        case 2:
          redemptionReport();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  // ==================================================================
  // MEMBERS
  // ==================================================================

  /**
   * Signs an existing guest up to the programme.
   *
   * A membership attaches to a guest record rather than creating a person of
   * its own, so a member and the guest who books rooms are always the same
   * individual.
   */
  private void enrolMember() {
    ui.startAction("ENROL A NEW MEMBER");

    ListInterface<Guest> notMembers = data.getGuestList().filter(
        guest -> data.findMemberByGuest(guest.getGuestId()) == null);

    if (notMembers.isEmpty()) {
      ui.displayMessage("  Every guest on record is already a member.");
      ui.pause();
      return;
    }

    ui.displaySectionHeading("Guests who are not yet members");
    ui.displayTableHeading(String.format("  %-7s %-28s %-16s %s",
        "GUEST", "NAME", "IC / PASSPORT", "CONTACT"));

    for (int i = 1; i <= notMembers.getNumberOfEntries(); i++) {
      Guest guest = notMembers.getEntry(i);
      System.out.printf("  %-7s %-28s %-16s %s%n",
          guest.getGuestId(), guest.getFullName(),
          guest.getIcPassportNo(), guest.getContactNumber());
    }
    ui.displayThinRule();

    String guestId = ui.inputGuestId();
    if (guestId == null) {
      return;
    }

    ServiceResult<Member> result = service.enrolMember(guestId);
    if (result.isFailure()) {
      ui.displayError(result.getMessage());
      ui.pause();
      return;
    }

    ui.displaySuccess(result.getMessage());
    ui.displayMember(result.getValue(), data.findGuest(guestId));
    ui.pause();
  }

  private void searchMember() {
    ui.startAction("SEARCH BY MEMBER ID");

    Member member = promptForMember();
    if (member == null) {
      return;
    }

    ui.displayMember(member, data.findGuest(member.getGuestId()));

    // Their recent activity says more than the balance on its own.
    ListInterface<PointTransaction> recent = ledgerFor(member.getMemberId());
    if (!recent.isEmpty()) {
      ui.displayLedger(recent, member);
    }
    ui.pause();
  }

  private void displayAllMembers() {
    ui.startAction("ALL MEMBERS");
    // The tree walk gives them in ID order without a sort.
    ui.displayMemberList(data.getMembersSorted(), data, "There are no members.");
    ui.pause();
  }

  private void displayMembersByPoints() {
    ui.startAction("MEMBERS BY POINTS BALANCE");

    ListInterface<Member> sorted = copyOf(data.getMemberList());
    sorted.sort(Comparator.comparingInt(Member::getPointsBalance).reversed());

    ui.displayMemberList(sorted, data, "There are no members.");
    ui.pause();
  }

  /**
   * Members whose points are about to be lost.
   *
   * The whole point of finding them is to tell them in time to spend the
   * points, so a notification is offered here rather than only in a report.
   */
  private void displayExpiringPoints() {
    ui.startAction("MEMBERS WITH POINTS EXPIRING SOON");

    LocalDate today = LocalDate.now();
    ListInterface<Member> expiring = data.getMemberList().filter(
        member -> member.hasExpiringPoints(today));

    if (!ui.displayMemberList(expiring, data,
        "No member has points expiring in the next " + Member.EXPIRING_SOON_DAYS
            + " days.")) {
      ui.pause();
      return;
    }

    ui.displayMessage("");
    if (!ui.confirm("Send each of them an expiry warning?")) {
      ui.pause();
      return;
    }

    for (int i = 1; i <= expiring.getNumberOfEntries(); i++) {
      Member member = expiring.getEntry(i);
      long days = ChronoUnit.DAYS.between(today, member.getPointsExpiryDate());

      service.raiseNotification(member.getMemberId(), Notification.POINTS_EXPIRING,
          String.format("Your %d points expire in %d day(s) - on %s.",
              member.getPointsBalance(), days, member.getPointsExpiryDate()),
          null);
    }

    data.saveLoyalty();
    ui.displaySuccess(expiring.getNumberOfEntries() + " warning(s) sent.");
    ui.pause();
  }

  /** Members who could be nudged over the line into the next tier. */
  private void displayNearNextTier() {
    ui.startAction("MEMBERS CLOSE TO THE NEXT TIER");

    final int within = 500;
    ListInterface<Member> near = data.getMemberList().filter(member -> {
      String next = member.getNextTier();
      return next != null && member.getPointsToNextTier() <= within;
    });

    if (near.isEmpty()) {
      ui.displayMessage("  No member is within " + within
          + " lifetime points of the next tier.");
      ui.pause();
      return;
    }

    near.sort(Comparator.comparingInt(Member::getPointsToNextTier));

    ui.displaySectionHeading("Within " + within + " points of the next tier");
    ui.displayTableHeading(String.format("  %-7s %-24s %-9s %11s %-9s %s",
        "MEMBER", "GUEST", "TIER", "LIFETIME", "NEXT", "SHORT BY"));

    for (int i = 1; i <= near.getNumberOfEntries(); i++) {
      Member member = near.getEntry(i);
      Guest guest = data.findGuest(member.getGuestId());

      System.out.printf("  %-7s %-24s %-9s %11d %-9s %d%n",
          member.getMemberId(), guest == null ? "-" : guest.getFullName(),
          member.getTier(), member.getLifetimePoints(),
          member.getNextTier(), member.getPointsToNextTier());
    }
    ui.displayThinRule();
    ui.pause();
  }

  // ==================================================================
  // POINTS
  // ==================================================================

  /**
   * Awards the points a completed stay earned.
   *
   * Normally this happens by itself when the front desk checks a guest out.
   * This is here for a stay that was settled before the guest joined the
   * programme, or one that needs putting right by hand.
   */
  private void awardPointsForStay() {
    ui.startAction("AWARD POINTS FOR A STAY");

    ListInterface<Booking> settled = data.getBookingList().filter(booking -> {
      if (!Booking.STATUS_CHECKED_OUT.equals(booking.getBookingStatus())) {
        return false;
      }
      entity.Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());
      return invoice != null && invoice.isSettled();
    });

    if (settled.isEmpty()) {
      ui.displayMessage("  There is no completed, settled stay to award points for.");
      ui.pause();
      return;
    }

    ui.displaySectionHeading("Completed stays");
    ui.displayTableHeading(String.format("  %-8s %-24s %-8s %10s  %s",
        "BOOKING", "GUEST", "MEMBER", "BILL", "POINTS AWARDED?"));

    for (int i = 1; i <= settled.getNumberOfEntries(); i++) {
      Booking booking = settled.getEntry(i);
      Guest guest = data.findGuest(booking.getGuestId());
      Member member = data.findMemberByGuest(booking.getGuestId());
      entity.Invoice invoice = data.findInvoiceByBooking(booking.getBookingId());

      PointTransaction awarded = data.getTransactionList().search(
          txn -> booking.getBookingId().equals(txn.getBookingId())
              && PointTransaction.EARN.equals(txn.getTxnType()));

      System.out.printf("  %-8s %-24s %-8s %10.2f  %s%n",
          booking.getBookingId(),
          guest == null ? "-" : guest.getFullName(),
          member == null ? "not a member" : member.getMemberId(),
          invoice == null ? 0.0 : invoice.getTotalAmount(),
          awarded == null ? "no" : "yes (" + awarded.getPoints() + ")");
    }
    ui.displayThinRule();

    String bookingId = ui.inputBookingId();
    if (bookingId == null) {
      return;
    }

    ServiceResult<PointTransaction> result = service.awardPointsForStay(bookingId);
    if (result.isFailure()) {
      ui.displayError(result.getMessage());
      ui.pause();
      return;
    }

    ui.displaySuccess(result.getMessage());

    Member member = data.findMember(result.getValue().getMemberId());
    if (member != null) {
      ui.displayMember(member, data.findGuest(member.getGuestId()));
    }
    ui.pause();
  }

  /**
   * Changes a member's points by hand.
   *
   * Written to the ledger like any other movement, with the reason recorded -
   * an adjustment that left no trace would make the balance unexplainable.
   */
  private void adjustPoints() {
    ui.startAction("ADJUST A MEMBER'S POINTS");

    Member member = promptForMember();
    if (member == null) {
      return;
    }

    ui.displayMember(member, data.findGuest(member.getGuestId()));

    int adjustment = ui.inputPointsAdjustment();
    if (adjustment == MessageUI.CANCELLED_INT) {
      ui.displayMessage("  Adjustment cancelled.");
      ui.pause();
      return;
    }

    if (adjustment < 0 && member.getPointsBalance() + adjustment < 0) {
      ui.displayError("That would take the balance below zero. The member has only "
          + member.getPointsBalance() + " points.");
      ui.pause();
      return;
    }

    String reason = ui.inputAdjustmentReason();
    if (reason == null) {
      ui.displayMessage("  Adjustment cancelled.");
      ui.pause();
      return;
    }

    if (!ui.confirm(String.format("Apply %+d points to %s?",
        adjustment, member.getMemberId()))) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    String previousTier = member.getTier();
    member.setPointsBalance(member.getPointsBalance() + adjustment);

    // Only points added count towards the lifetime total: taking points away
    // must not cost the member the tier they earned.
    if (adjustment > 0) {
      member.setLifetimePoints(member.getLifetimePoints() + adjustment);
    }

    data.getTransactionList().add(new PointTransaction(data.nextTransactionId(),
        member.getMemberId(), null, PointTransaction.ADJUST, adjustment,
        member.getPointsBalance(), LocalDate.now(), reason));

    if (member.refreshTier()) {
      service.raiseNotification(member.getMemberId(), Notification.TIER_UPGRADE,
          "Congratulations - you are now " + member.getTier() + ".", null);
      ui.displaySuccess("Tier upgraded from " + previousTier
          + " to " + member.getTier() + ".");
    }

    data.saveLoyalty();
    ui.displaySuccess(String.format("%+d points applied. Balance is now %d.",
        adjustment, member.getPointsBalance()));
    ui.pause();
  }

  private void expirePoints() {
    ui.startAction("EXPIRE POINTS PAST THEIR DATE");

    LocalDate today = LocalDate.now();
    ListInterface<Member> overdue = data.getMemberList().filter(
        member -> member.getPointsExpiryDate() != null
            && member.getPointsExpiryDate().isBefore(today)
            && member.getPointsBalance() > 0);

    if (overdue.isEmpty()) {
      ui.displayMessage("  No member has points past their expiry date.");
      ui.pause();
      return;
    }

    ui.displayMemberList(overdue, data, "");
    ui.displayMessage("");
    ui.displayMessage("  These members' points have passed their expiry date.");
    ui.displayMessage("");

    if (!ui.confirm("Expire them now?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    ServiceResult<Integer> result = service.expireOverduePoints();
    ui.displaySuccess(result.getMessage());
    ui.displayMessage("  Each affected member has been notified.");
    ui.pause();
  }

  private void displayLedger() {
    ui.startAction("A MEMBER'S POINT LEDGER");

    Member member = promptForMember();
    if (member == null) {
      return;
    }

    ui.displayMember(member, data.findGuest(member.getGuestId()));
    ui.displayLedger(ledgerFor(member.getMemberId()), member);
    ui.pause();
  }

  /** One member's ledger rows, oldest first. */
  private ListInterface<PointTransaction> ledgerFor(String memberId) {
    ListInterface<PointTransaction> txns = data.getTransactionList().filter(
        txn -> memberId.equals(txn.getMemberId()));
    txns.sort(Comparator.comparing(PointTransaction::getTxnDate));
    return txns;
  }

  // ==================================================================
  // REWARDS
  // ==================================================================

  private void displayCatalogue() {
    ui.startAction("REWARD CATALOGUE");
    ui.displayRewardCatalogue(data.getRewardList(), null);
    ui.pause();
  }

  /**
   * Adds a new reward to the catalogue when its ID is not already in use.
   *
   * @param newReward the reward to add; its ID is generated if left blank
   * @return true if the reward was added and saved
   */
  public boolean addReward(Reward newReward) {
    if (newReward == null || newReward.getRewardName() == null
        || newReward.getRewardName().isBlank()) {
      return false;
    }
    if (newReward.getPointsRequired() <= 0) {
      return false;
    }
    if (newReward.getCategory() == null || newReward.getCategory().isBlank()) {
      return false;
    }
    if (newReward.getMinimumTier() == null || newReward.getMinimumTier().isBlank()) {
      return false;
    }
    if (newReward.getStockQuantity() < 0) {
      return false;
    }

    if (newReward.getRewardId() == null || newReward.getRewardId().isBlank()) {
      newReward.setRewardId(data.nextRewardId());
    }

    if (data.findReward(newReward.getRewardId()) != null) {
      return false;
    }

    data.getRewardList().add(newReward);
    data.saveLoyalty();
    return true;
  }

  /**
   * Removes a reward from the catalogue by ID.
   *
   * @param rewardId the reward to delete, e.g. RW001
   * @return true if the reward was found and removed
   */
  public boolean deleteReward(String rewardId) {
    if (rewardId == null || rewardId.isBlank()) {
      return false;
    }

    Reward reward = data.findReward(rewardId);
    if (reward == null) {
      return false;
    }

    data.getRewardList().removeEntry(reward);
    data.saveLoyalty();
    return true;
  }

  /**
   * Adds stock to an existing reward and saves the updated catalogue.
   *
   * @param rewardId the reward to restock, e.g. RW001
   * @param amount how many units to add
   * @return true if the reward was found and restocked
   */
  public boolean restockReward(String rewardId, int amount) {
    if (rewardId == null || amount <= 0) {
      return false;
    }

    Reward reward = data.findReward(rewardId);
    if (reward == null) {
      return false;
    }

    reward.addQuantity(amount);
    data.saveLoyalty();
    return true;
  }

  /** Collects a new reward from the user and adds it to the catalogue. */
  private void addNewReward() {
    ui.startAction("ADD A NEW REWARD");

    Reward draft = ui.promptAddNewReward();
    if (draft == null) {
      ui.displayMessage("  Add reward cancelled.");
      ui.pause();
      return;
    }

    if (addReward(draft)) {
      ui.displaySuccess("Reward " + draft.getRewardId() + " - "
          + draft.getRewardName() + " added with "
          + draft.getStockQuantity() + " in stock.");
    } else {
      ui.displayError("Could not add reward. The ID may already exist or the "
          + "details were invalid.");
    }
    ui.pause();
  }

  /**
   * Asks for a reward ID and finds it, re-asking until one names a reward.
   *
   * @return the reward, or null if the user typed 0 to quit
   */
  private Reward promptForReward() {
    while (true) {
      String rewardId = ui.inputRewardId();
      if (rewardId == null) {
        return null;
      }

      Reward reward = data.findReward(rewardId);
      if (reward != null) {
        return reward;
      }

      ui.displayError("Reward " + rewardId
          + " was not found. Enter another ID, or 0 to go back.");
    }
  }

  /** Prompts for a reward ID and removes it from the catalogue. */
  private void deleteRewardMenu() {
    ui.startAction("DELETE A REWARD");
    ui.displayRewardCatalogue(data.getRewardList(), null);

    Reward reward = promptForReward();
    if (reward == null) {
      ui.displayMessage("  Delete reward cancelled.");
      ui.pause();
      return;
    }
    String rewardId = reward.getRewardId();

    if (hasPendingRedemptionFor(rewardId)) {
      ui.displayError("Reward " + rewardId + " cannot be deleted while a "
          + "redemption request for it is still pending.");
      ui.pause();
      return;
    }

    if (!ui.confirm("Delete " + rewardId + " - " + reward.getRewardName() + "?")) {
      ui.displayMessage("  Delete cancelled.");
      ui.pause();
      return;
    }

    if (deleteReward(rewardId)) {
      ui.displaySuccess("Reward " + rewardId + " - " + reward.getRewardName()
          + " has been removed.");
    } else {
      ui.displayError("Could not delete reward " + rewardId + ".");
    }
    ui.pause();
  }

  /**
   * Prompts for a reward and a quantity to add, then updates the catalogue.
   */
  private void restockReward() {
    ui.startAction("RESTOCK A REWARD");
    ui.displayRewardCatalogue(data.getRewardList(), null);

    Reward reward = promptForReward();
    if (reward == null) {
      ui.displayMessage("  Restock cancelled.");
      ui.pause();
      return;
    }

    ui.displayMessage("");
    MessageUI.displayField("Reward", reward.getRewardName());
    MessageUI.displayField("Current quantity", String.valueOf(reward.getStockQuantity()));
    ui.displayMessage("");

    Integer amount = ui.inputRestockQuantity();
    if (amount == null) {
      ui.displayMessage("  Restock cancelled.");
      ui.pause();
      return;
    }

    if (restockReward(reward.getRewardId(), amount)) {
      ui.displaySuccess("Restocked " + reward.getRewardId() + " - " + reward.getRewardName()
          + " by " + amount + ". New quantity: " + reward.getStockQuantity() + ".");
    } else {
      ui.displayError("Restock failed.");
    }
    ui.pause();
  }

  private boolean hasPendingRedemptionFor(String rewardId) {
    ListInterface<Redemption> pending = data.getPendingRedemptions();
    for (int position = 1; position <= pending.getNumberOfEntries(); position++) {
      if (rewardId.equals(pending.getEntry(position).getRewardId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Puts a redemption request into the queue.
   *
   * Nothing is deducted here and eligibility is not decided here either. The
   * request simply joins the line, and the rules are applied when it reaches
   * the front - which is what makes a refusal recorded and reviewable.
   */
  private void requestRedemption() {
    ui.startAction("REQUEST A REDEMPTION");

    Member member = promptForMember();
    if (member == null) {
      return;
    }

    ui.displayMember(member, data.findGuest(member.getGuestId()));

    String rewardId = ui.inputReward(data.getRewardList(), member);
    if (rewardId == null) {
      ui.displayMessage("  Request cancelled.");
      ui.pause();
      return;
    }

    Reward reward = data.findReward(rewardId);
    if (!ui.confirm("Request " + reward.getRewardName() + " for "
        + reward.getPointsRequired() + " points?")) {
      ui.displayMessage("  Request cancelled.");
      ui.pause();
      return;
    }

    ServiceResult<Redemption> result =
        service.requestRedemption(member.getMemberId(), rewardId);

    if (result.isFailure()) {
      ui.displayError(result.getMessage());
      ui.pause();
      return;
    }

    ui.displaySuccess(result.getMessage());
    ui.displayMessage("");
    ui.displayMessage("  No points have been taken yet - they are deducted only");
    ui.displayMessage("  if the request is approved when it reaches the front.");
    ui.displayMessage("  Requests waiting: " + data.getPendingRedemptions()
        .getNumberOfEntries());
    ui.pause();
  }

  /**
   * Decides the oldest waiting request.
   *
   * Whoever asked first is dealt with first - there is no urgent lane here,
   * because no redemption is more pressing than another.
   */
  private void processNextRedemption() {
    ui.startAction("PROCESS THE NEXT REQUEST");

    ListInterface<Redemption> pending = data.getPendingRedemptions();
    if (pending.isEmpty()) {
      ui.displayError("There are no requests waiting.");
      ui.pause();
      return;
    }

    Redemption next = pending.getEntry(1);
    Member member = data.findMember(next.getMemberId());
    Reward reward = data.findReward(next.getRewardId());
    Guest guest = (member == null) ? null : data.findGuest(member.getGuestId());

    ui.displayMessage("  Next request in the queue:");
    ui.displayMessage("");
    MessageUI.displayField("Redemption ID", next.getRedemptionId());
    MessageUI.displayField("Member", next.getMemberId()
        + (guest == null ? "" : "  (" + guest.getFullName() + ")"));
    MessageUI.displayField("Reward", reward == null ? next.getRewardId()
        : reward.getRewardName());
    MessageUI.displayField("Points required", String.valueOf(next.getPointsUsed()));
    MessageUI.displayField("Requested on", String.valueOf(next.getRequestDate()));

    if (member != null) {
      MessageUI.displayField("Member balance", String.valueOf(member.getPointsBalance()));
      MessageUI.displayField("Member tier", member.getTier());
    }
    if (reward != null) {
      MessageUI.displayField("Minimum tier", reward.getMinimumTier());
      MessageUI.displayField("Stock left", String.valueOf(reward.getStockQuantity()));
    }

    ui.displayMessage("");
    if (!ui.confirm("Process this request?")) {
      ui.displayMessage("  It stays at the front of the queue.");
      ui.pause();
      return;
    }

    ServiceResult<Redemption> result = service.processNextRedemption(staffId);
    Redemption processed = result.getValue();

    if (processed != null && Redemption.APPROVED.equals(processed.getStatus())) {
      ui.displaySuccess(result.getMessage());

      if (member != null) {
        ui.displayMessage("  " + member.getMemberId() + " now has "
            + member.getPointsBalance() + " points.");
      }

      // An approved reward is worth real money off a bill, so the chance to
      // apply it is offered while the guest is still here.
      if (reward != null && reward.getCashValue() > 0) {
        offerToApplyToBill(processed, member, reward);
      }
    } else {
      ui.displayError(result.getMessage());
      ui.displayMessage("  No points have been deducted.");
    }
    ui.pause();
  }

  /** Offers to take an approved reward off the guest's live bill. */
  private void offerToApplyToBill(Redemption redemption, Member member, Reward reward) {
    if (member == null) {
      return;
    }

    ListInterface<Booking> live = data.getBookingList().filter(
        booking -> member.getGuestId().equals(booking.getGuestId())
            && (Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())
                || Booking.STATUS_CONFIRMED.equals(booking.getBookingStatus())));

    if (live.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  This member has no live booking to apply the reward to.");
      ui.displayMessage("  It can be applied from Front Desk on their next stay.");
      return;
    }

    ui.displayMessage("");
    ui.displayMessage(String.format("  This reward is worth RM%.2f off a bill.",
        reward.getCashValue()));

    Booking booking = live.getEntry(1);
    if (!ui.confirm("Apply it to booking " + booking.getBookingId() + " now?")) {
      return;
    }

    ServiceResult<entity.Invoice> applied = service.applyRedemptionToInvoice(
        redemption.getRedemptionId(), booking.getBookingId());

    if (applied.isSuccess()) {
      ui.displaySuccess(applied.getMessage());
    } else {
      ui.displayError(applied.getMessage());
    }
  }

  private void displayPendingQueue() {
    ui.startAction("PENDING REDEMPTION QUEUE");

    ListInterface<Redemption> pending = data.getPendingRedemptions();
    if (pending.isEmpty()) {
      ui.displayMessage("  No request is waiting.");
      ui.pause();
      return;
    }

    ui.displaySectionHeading("Waiting, in the order they will be processed");
    ui.displayTableHeading(String.format("  %-4s %-8s %-7s %-26s %7s  %s",
        "POS", "REDEEM", "MEMBER", "REWARD", "POINTS", "REQUESTED"));

    for (int i = 1; i <= pending.getNumberOfEntries(); i++) {
      Redemption redemption = pending.getEntry(i);
      Reward reward = data.findReward(redemption.getRewardId());

      System.out.printf("  %-4d %-8s %-7s %-26s %7d  %s%n",
          i, redemption.getRedemptionId(), redemption.getMemberId(),
          reward == null ? redemption.getRewardId() : reward.getRewardName(),
          redemption.getPointsUsed(), redemption.getRequestDate());
    }
    ui.displayThinRule();
    ui.displayMessage("  First requested, first processed. There is no priority lane.");
    ui.pause();
  }

  private void displayRedemptionHistory() {
    ui.startAction("REDEMPTION HISTORY");

    ListInterface<Redemption> history = copyOfRedemptions(data.getRedemptionList());
    history.sort(Comparator.comparing(Redemption::getRequestDate).reversed());

    ui.displayRedemptionList(history, data, "No redemption has ever been requested.");
    ui.pause();
  }

  // ==================================================================
  // NOTIFICATIONS
  // ==================================================================

  private void displayNotifications() {
    ui.startAction("A MEMBER'S NOTIFICATIONS");

    Member member = promptForMember();
    if (member == null) {
      return;
    }

    final String memberId = member.getMemberId();
    ListInterface<Notification> notifications = data.getNotificationList().filter(
        notification -> memberId.equals(notification.getMemberId()));
    notifications.sort(Comparator.comparing(Notification::getCreatedDate).reversed());

    ui.displayNotifications(notifications, memberId);

    if (!notifications.isEmpty() && notifications.countIf(n -> !n.isRead()) > 0) {
      ui.displayMessage("");
      if (ui.confirm("Mark them all as read?")) {
        for (int i = 1; i <= notifications.getNumberOfEntries(); i++) {
          notifications.getEntry(i).setRead(true);
        }
        data.saveLoyalty();
        ui.displaySuccess("All notifications marked as read.");
      }
    }
    ui.pause();
  }

  /**
   * Shows the offer that suits a particular member.
   *
   * Built from their tier, their balance and how long they have been a member,
   * so a long-standing Diamond member is not shown the same welcome offer as
   * somebody who joined last week.
   */
  private void displayPromotion() {
    ui.startAction("PERSONALISED PROMOTION");

    Member member = promptForMember();
    if (member == null) {
      return;
    }

    Guest guest = data.findGuest(member.getGuestId());
    ui.displayMember(member, guest);

    long monthsAMember = ChronoUnit.MONTHS.between(member.getJoinDate(), LocalDate.now());

    ui.displaySectionHeading("Offer for "
        + (guest == null ? member.getMemberId() : guest.getFullName()));

    switch (member.getTier()) {
      case Member.DIAMOND:
        ui.displayMessage("  Diamond signature experience:");
        ui.displayMessage("  A night in the Executive Villa, and a private dining");
        ui.displayMessage("  experience for two.");
        break;

      case Member.PLATINUM:
        ui.displayMessage("  Premium Platinum offer:");
        ui.displayMessage("  A complimentary suite upgrade on your next stay, and");
        ui.displayMessage("  lounge access for the duration.");
        break;

      case Member.GOLD:
        ui.displayMessage("  Enhanced Gold offer:");
        ui.displayMessage("  A spa session at half the usual points, and late");
        ui.displayMessage("  check-out on request.");
        break;

      default:
        ui.displayMessage("  Silver welcome offer:");
        ui.displayMessage("  Double points on your next stay, and a welcome");
        ui.displayMessage("  fruit basket on arrival.");
        break;
    }

    String nextTier = member.getNextTier();
    if (nextTier != null) {
      ui.displayMessage("");
      ui.displayMessage("  You are " + member.getPointsToNextTier()
          + " lifetime points from " + nextTier + ".");
    }

    if (monthsAMember >= 12) {
      ui.displayMessage("");
      ui.displayMessage("  Thank you for " + monthsAMember
          + " months with us - a loyalty bonus applies to your next booking.");
    }

    // Rewards they could actually take today, rather than the whole catalogue.
    ListInterface<Reward> affordable = data.getRewardList().filter(reward ->
        reward.isAvailable()
            && reward.getPointsRequired() <= member.getPointsBalance()
            && Member.tierRank(member.getTier())
                >= Member.tierRank(reward.getMinimumTier()));

    if (!affordable.isEmpty()) {
      ui.displaySectionHeading("You could redeem right now");
      for (int i = 1; i <= affordable.getNumberOfEntries(); i++) {
        Reward reward = affordable.getEntry(i);
        System.out.printf("    %-30s %d points%n",
            reward.getRewardName(), reward.getPointsRequired());
      }
    }

    ui.displayMessage("");
    if (ui.confirm("Send this promotion to the member?")) {
      service.raiseNotification(member.getMemberId(), Notification.PROMOTION,
          "A " + member.getTier() + " offer is waiting for you.", null);
      data.saveLoyalty();
      ui.displaySuccess("Promotion sent.");
    }
    ui.pause();
  }

  // ==================================================================
  // REPORTS
  // ==================================================================

  /** Who the members are and how the tiers are distributed. */
  private void membershipReport() {
    ui.displayReportHeader("MEMBERSHIP & TIER PERFORMANCE REPORT");

    ListInterface<Member> members = data.getMemberList();
    if (members.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  There are no members to analyse.");
      ui.pause();
      return;
    }

    int total = members.getNumberOfEntries();
    int totalBalance = 0;
    int totalLifetime = 0;
    Member highest = null;

    for (int i = 1; i <= total; i++) {
      Member member = members.getEntry(i);
      totalBalance += member.getPointsBalance();
      totalLifetime += member.getLifetimePoints();

      if (highest == null || member.getPointsBalance() > highest.getPointsBalance()) {
        highest = member;
      }
    }

    ui.displaySectionHeading("Membership");
    ui.displayReportLine("Total members", String.valueOf(total));
    ui.displayReportLine("Active",
        String.valueOf(members.countIf(m -> Member.ACTIVE.equals(m.getStatus()))));
    ui.displayReportLine("Points outstanding", String.valueOf(totalBalance));
    ui.displayReportLine("Average balance", String.valueOf(totalBalance / total));
    ui.displayReportLine("Lifetime points issued", String.valueOf(totalLifetime));

    if (highest != null) {
      Guest guest = data.findGuest(highest.getGuestId());
      ui.displayReportLine("Highest balance", String.format("%s  (%s, %d points)",
          guest == null ? "-" : guest.getFullName(),
          highest.getMemberId(), highest.getPointsBalance()));
    }

    displayTierBreakdown(members);

    LocalDate today = LocalDate.now();
    ui.displaySectionHeading("Attention needed");
    ui.displayReportLine("Points expiring within " + Member.EXPIRING_SOON_DAYS + " days",
        String.valueOf(members.countIf(m -> m.hasExpiringPoints(today))));
    ui.displayReportLine("Within 500 points of the next tier",
        String.valueOf(members.countIf(m -> m.getNextTier() != null
            && m.getPointsToNextTier() <= 500)));
    ui.displayReportLine("Joined in the last 30 days",
        String.valueOf(members.countIf(
            m -> ChronoUnit.DAYS.between(m.getJoinDate(), today) <= 30)));

    ui.displayReportFooter();
  }

  /** How the members split across the four tiers. */
  private void displayTierBreakdown(ListInterface<Member> members) {
    String[] tiers = {Member.SILVER, Member.GOLD, Member.PLATINUM, Member.DIAMOND};
    String[] labels = new String[tiers.length];
    double[] values = new double[tiers.length];

    for (int i = 0; i < tiers.length; i++) {
      final String tier = tiers[i];
      labels[i] = tier.substring(0, Math.min(4, tier.length()));
      values[i] = members.countIf(member -> tier.equals(member.getTier()));
    }

    ui.displayBarChart("Members by tier", "Members", labels, values);

    ui.displaySectionHeading("Tier detail");
    for (int i = 0; i < tiers.length; i++) {
      final String tier = tiers[i];
      int count = (int) values[i];

      int tierPoints = 0;
      for (int j = 1; j <= members.getNumberOfEntries(); j++) {
        Member member = members.getEntry(j);
        if (tier.equals(member.getTier())) {
          tierPoints += member.getPointsBalance();
        }
      }

      ui.displayReportLine(tier, String.format("%d member(s)  (%.1f%%)  %d points held",
          count, (count * 100.0) / members.getNumberOfEntries(), tierPoints));
    }
  }

  /** How redemptions are going - approved, refused, and why. */
  private void redemptionReport() {
    ui.displayReportHeader("REDEMPTION ANALYSIS REPORT");

    ListInterface<Redemption> all = data.getRedemptionList();
    if (all.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  No redemption has ever been requested.");
      ui.pause();
      return;
    }

    int total = all.getNumberOfEntries();
    int approved = all.countIf(r -> Redemption.APPROVED.equals(r.getStatus()));
    int rejected = all.countIf(r -> Redemption.REJECTED.equals(r.getStatus()));
    int pending = all.countIf(Redemption::isPending);

    int pointsRedeemed = 0;
    int pointsPending = 0;
    for (int i = 1; i <= total; i++) {
      Redemption redemption = all.getEntry(i);
      if (Redemption.APPROVED.equals(redemption.getStatus())) {
        pointsRedeemed += redemption.getPointsUsed();
      } else if (redemption.isPending()) {
        pointsPending += redemption.getPointsUsed();
      }
    }

    ui.displaySectionHeading("Requests");
    ui.displayReportLine("Total requests", String.valueOf(total));
    ui.displayReportLine("Approved", approved + percentOf(approved, total));
    ui.displayReportLine("Rejected", rejected + percentOf(rejected, total));
    ui.displayReportLine("Still waiting", pending + percentOf(pending, total));

    int decided = approved + rejected;
    ui.displayReportLine("Approval rate", decided == 0 ? "-"
        : String.format("%.1f%%", (approved * 100.0) / decided));

    ui.displaySectionHeading("Points");
    ui.displayReportLine("Points redeemed", String.valueOf(pointsRedeemed));
    ui.displayReportLine("Points committed but not yet decided",
        String.valueOf(pointsPending));

    displayRewardPopularity(all);
    displayRejectionReasons(all);

    ui.displayReportFooter();
  }

  /** Which rewards people actually want. */
  private void displayRewardPopularity(ListInterface<Redemption> all) {
    ListInterface<Reward> rewards = data.getRewardList();
    String[] labels = new String[rewards.getNumberOfEntries()];
    double[] values = new double[rewards.getNumberOfEntries()];

    Reward most = null;
    Reward least = null;
    int highest = -1;
    int lowest = Integer.MAX_VALUE;

    for (int i = 1; i <= rewards.getNumberOfEntries(); i++) {
      Reward reward = rewards.getEntry(i);
      final String rewardId = reward.getRewardId();
      int count = all.countIf(r -> rewardId.equals(r.getRewardId()));

      labels[i - 1] = rewardId;
      values[i - 1] = count;

      if (count > highest) {
        highest = count;
        most = reward;
      }
      if (count < lowest) {
        lowest = count;
        least = reward;
      }
    }

    ui.displayBarChart("Requests by reward", "Requests", labels, values);

    if (most != null) {
      ui.displayReportLine("Most requested",
          most.getRewardName() + "  (" + highest + ")");
    }
    if (least != null) {
      ui.displayReportLine("Least requested",
          least.getRewardName() + "  (" + lowest + ")");
    }
  }

  /**
   * Why requests were refused.
   *
   * Worth reporting: a pattern of refusals for one reason suggests the rule
   * itself, or how it is explained to members, needs looking at.
   */
  private void displayRejectionReasons(ListInterface<Redemption> all) {
    ListInterface<Redemption> rejected = all.filter(
        r -> Redemption.REJECTED.equals(r.getStatus()));

    if (rejected.isEmpty()) {
      return;
    }

    ui.displaySectionHeading("Why requests were refused");
    ui.displayTableHeading(String.format("  %-8s %-7s %-24s %s",
        "REDEEM", "MEMBER", "REWARD", "REASON"));

    for (int i = 1; i <= rejected.getNumberOfEntries(); i++) {
      Redemption redemption = rejected.getEntry(i);
      Reward reward = data.findReward(redemption.getRewardId());

      System.out.printf("  %-8s %-7s %-24s %s%n",
          redemption.getRedemptionId(), redemption.getMemberId(),
          reward == null ? redemption.getRewardId() : reward.getRewardName(),
          redemption.getRejectReason() == null ? "-" : redemption.getRejectReason());
    }
    ui.displayThinRule();
  }

  // ==================================================================
  // HELPERS
  // ==================================================================

  /**
   * Asks for a member ID and finds them.
   *
   * An ID that names no member is re-asked rather than ending the action, so a
   * typo costs one line instead of sending the user back to the menu.
   *
   * @return the member, or null if the user typed 0 to quit
   */
  private Member promptForMember() {
    while (true) {
      String memberId = ui.inputMemberId();
      if (memberId == null) {
        return null;
      }

      Member member = data.findMember(memberId);
      if (member != null) {
        return member;
      }

      ui.displayError("No member with ID " + memberId
          + ". Enter another ID, or 0 to go back.");
    }
  }

  private ListInterface<Member> copyOf(ListInterface<Member> source) {
    ListInterface<Member> copy = new ArrayList<>();
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      copy.add(source.getEntry(i));
    }
    return copy;
  }

  private ListInterface<Redemption> copyOfRedemptions(ListInterface<Redemption> source) {
    ListInterface<Redemption> copy = new ArrayList<>();
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      copy.add(source.getEntry(i));
    }
    return copy;
  }

  private String percentOf(int part, int whole) {
    if (whole == 0) {
      return "";
    }
    return String.format("  (%.1f%%)", (part * 100.0) / whole);
  }
}
