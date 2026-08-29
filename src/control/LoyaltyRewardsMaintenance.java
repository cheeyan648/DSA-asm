package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.LoyaltyRewardsUI;
import entity.Booking;
import entity.Guest;
import entity.Member;
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
 * member nothing - the eligibility rules are checked both when a request is
 * made and again when it is decided, since stock can run out while it waits.
 *
 * COLLECTION ADTs USED
 *   ListInterface<Member> / <Reward> / <Redemption> / <PointTransaction>
 *       The memberships, the catalogue, every redemption and the points
 *       ledger. filter() and search() answer the lookups, sort() orders the
 *       listings and reports, countIf() builds the tier tallies.
 *   ListInterface<Redemption> as a FIFO queue - the pending requests.
 *       Whoever asked first is decided first; there is no urgent lane here
 *       because loyalty has no urgency concept.
 *   TreeInterface<String, Member> - memberId to Member, for fast lookup.
 *
 * INTERACTS WITH
 *   Front-Desk Service. Receives at check-out: bookingId, guestId and the
 *   settled invoice total, from which points are earned. Receives reward
 *   requests carrying memberId, rewardId and the bookingId they belong to.
 *   Returns nothing directly - the decision is written on the redemption,
 *   and the front desk reads it when printing a receipt.
 *
 * @author Ivan Tan Yann Rong
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
          editMember();
          break;
        case 3:
          removeMembership();
          break;
        case 4:
          displayAllMembers();
          break;
        case 5:
          displayMembersByPoints();
          break;
        case 6:
          displayNearNextTier();
          break;
        case 7:
          editTierThresholds();
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
          expirePoints();
          break;
        case 2:
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
          editReward();
          break;
        case 4:
          deleteRewardMenu();
          break;
        case 5:
          restockReward();
          break;
        case 6:
          requestRedemption();
          break;
        case 7:
          processPendingQueue();
          break;
        case 8:
          displayRedemptionHistory();
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

    // The number in the first column is what gets typed, so nobody has to
    // know a guest ID before they can enrol somebody.
    ui.displaySectionHeading("Guests who are not yet members");
    ui.displayTableHeading(String.format("  %-5s %-7s %-26s %-16s %s",
        "NO", "GUEST", "NAME", "IC / PASSPORT", "CONTACT"));

    for (int i = 1; i <= notMembers.getNumberOfEntries(); i++) {
      Guest guest = notMembers.getEntry(i);
      System.out.printf("  [%d]   %-7s %-26s %-16s %s%n",
          i, guest.getGuestId(), guest.getFullName(),
          guest.getIcPassportNo(), guest.getContactNumber());
    }
    ui.displayThinRule();
    System.out.printf("  %d guest(s) not yet enrolled.%n",
        notMembers.getNumberOfEntries());

    int position = ui.inputListPosition(notMembers.getNumberOfEntries(),
        "Number of the guest to enrol");
    if (position < 0) {
      return;
    }
    String guestId = notMembers.getEntry(position).getGuestId();

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
   * Sets what each tier is worth.
   *
   * Tiers are earned from lifetime points, so moving a threshold re-tiers
   * everybody at once - which is shown before it is applied, because raising a
   * bar can demote a guest who did nothing wrong.
   */
  private void editTierThresholds() {
    while (true) {
      ui.startAction("TIER THRESHOLDS");

      ui.displayTierThresholds(data.getMemberList());

      ui.displayMessage("");
      if (!ui.confirm("Change these thresholds?")) {
        return;
      }

      int gold = ui.inputThreshold("GOLD", Member.getGoldThreshold());
      if (gold < 0) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      int platinum = ui.inputThreshold("PLATINUM", Member.getPlatinumThreshold());
      if (platinum < 0) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      int diamond = ui.inputThreshold("DIAMOND", Member.getDiamondThreshold());
      if (diamond < 0) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      if (gold >= platinum || platinum >= diamond) {
        ui.displayError("Each tier must need more points than the one below it.");
        ui.displayMessage("  GOLD < PLATINUM < DIAMOND.");
        ui.pause();
        continue;
      }

      // What this would do to real members, worked out before anything is
      // saved, so the officer sees the demotions rather than discovering them.
      ui.displayRetierPreview(data.getMemberList(), gold, platinum, diamond, data);

      ui.displayMessage("");
      if (!ui.confirm("Apply these thresholds?")) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      Member.setThresholds(gold, platinum, diamond);

      // Everyone is re-tiered from their lifetime points immediately.
      int moved = 0;
      ListInterface<Member> members = data.getMemberList();
      for (int i = 1; i <= members.getNumberOfEntries(); i++) {
        if (members.getEntry(i).refreshTier()) {
          moved++;
        }
      }

      data.saveLoyalty();
      ui.displaySuccess("Thresholds updated. " + moved + " member(s) re-tiered.");
      ui.pause("Press ENTER to accept the change");
      return;
    }
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
   * Changes a member's points by hand.
   *
   * Written to the ledger like any other movement, with the reason recorded -
   * an adjustment that left no trace would make the balance unexplainable.
   */
  /**
   * Corrects a member's own details.
   *
   * Only the guest record behind the membership is editable here - the points,
   * the tier and the lifetime total are all earned, so none of them is offered.
   * A balance that could be typed in would make every report meaningless.
   */
  private void editMember() {
    while (true) {
      ui.startAction("EDIT A MEMBER");

      Member member = promptForMember("Number of the member to edit");
      if (member == null) {
        ui.pause();
        return;
      }

      Guest guest = data.findGuest(member.getGuestId());
      if (guest == null) {
        ui.displayError("That membership's guest record is missing.");
        ui.pause();
        continue;
      }

      ui.displayMember(member, guest);
      ui.displayMessage("");
      ui.displayMessage("  Points, tier and lifetime total are earned and"
          + " cannot be typed in.");
      ui.displayMessage("  Press ENTER on any field to leave it as it is.");

      if (!ui.editGuestFields(guest)) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      ui.displayMessage("");
      if (!ui.confirm("Save these changes to " + guest.getFullName() + "?")) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      data.saveMasters();
      ui.displaySuccess("Member " + member.getMemberId() + " updated.");
      ui.displayMember(member, guest);
      ui.pause("Press ENTER to accept the changes");

      if (!ui.confirmAnother("Edit another member?")) {
        return;
      }
    }
  }

  /**
   * Ends a membership.
   *
   * The guest record stays: they can still book, they simply stop earning.
   * A membership holding points or a request still waiting is refused, so
   * nothing a guest has earned disappears without being dealt with first.
   */
  private void removeMembership() {
    while (true) {
      ui.startAction("REMOVE A MEMBERSHIP");

      Member member = promptForMember("Number of the membership to remove");
      if (member == null) {
        ui.pause();
        return;
      }

      Guest guest = data.findGuest(member.getGuestId());
      ui.displayMember(member, guest);

      ui.displayMessage("");
      if (member.getPointsBalance() > 0) {
        ui.displayMessage("  " + member.getPointsBalance()
            + " points will be forfeited.");
      }
      ui.displayMessage("  The guest record is kept - only the membership ends.");
      ui.displayMessage("  Past transactions stay on record for reporting.");

      if (!ui.confirm("Are you sure you want to remove membership "
          + member.getMemberId() + "?")) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      int forfeited = member.getPointsBalance();
      data.getMemberList().removeEntry(member);
      data.rebuildIndexes();
      data.saveLoyalty();

      ui.displaySuccess("Membership " + member.getMemberId() + " removed.");
      if (forfeited > 0) {
        ui.displayMessage("  " + forfeited + " points forfeited.");
      }
      ui.displayMessage("  " + (guest == null ? "The guest" : guest.getFullName())
          + " can still book, but no longer earns points.");
      ui.pause("Press ENTER to accept the removal");

      if (!ui.confirmAnother("Remove another membership?")) {
        return;
      }
    }
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
  /**
   * Asks which reward to act on, showing the catalogue first.
   *
   * @return the chosen reward, or null if the officer went back
   */
  private Reward promptForReward() {
    return promptForReward("Number of the reward");
  }

  /**
   * Asks which reward to act on, under wording that fits the action.
   *
   * @param prompt what the number is being asked for
   * @return the chosen reward, or null if the officer went back
   */
  private Reward promptForReward(String prompt) {
    return ui.chooseReward(data.getRewardList(), prompt,
        "The reward catalogue is empty.");
  }

  /**
   * Changes what a reward is and what it costs.
   *
   * A catalogue is not fixed: prices move, a service is withdrawn for a
   * season, a name is corrected. Each field is offered with what it holds
   * now, and pressing ENTER keeps it - so one figure can be changed without
   * retyping the rest.
   */
  private void editReward() {
    while (true) {
      ui.startAction("EDIT A REWARD");

      Reward reward = promptForReward("Number of the reward to edit");
      if (reward == null) {
        ui.pause();
        return;
      }

      ui.displayReward(reward);
      ui.displayMessage("");
      ui.displayMessage("  Press ENTER on any field to leave it as it is.");

      ServiceResult<Reward> edited = ui.editRewardFields(reward);
      if (edited.isFailure()) {
        ui.displayMessage("  " + edited.getMessage());
        ui.pause();
        continue;
      }

      ui.displayMessage("");
      if (!ui.confirm("Save these changes to " + reward.getRewardId() + "?")) {
        ui.displayMessage("  Nothing has been changed.");
        ui.pause();
        continue;
      }

      data.saveLoyalty();
      ui.displaySuccess("Reward " + reward.getRewardId() + " updated.");
      ui.displayReward(reward);
      ui.pause("Press ENTER to accept the changes");

      if (!ui.confirmAnother("Edit another reward?")) {
        return;
      }
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
   * Works through the requests waiting for a decision.
   *
   * The whole queue is on screen rather than one request at a time, so the
   * officer can see what is waiting and pick from it - but the list stays in
   * arrival order, because first asked is still first served.
   */
  private void processPendingQueue() {
    while (true) {
      ui.startAction("PROCESS THE PENDING QUEUE");

      ListInterface<Redemption> pending = data.getPendingRedemptions();
      Redemption chosen = ui.choosePendingRedemption(pending, data);
      if (chosen == null) {
        ui.pause();
        return;
      }

      Member member = data.findMember(chosen.getMemberId());
      Reward reward = data.findReward(chosen.getRewardId());
      ui.displayRedemptionForDecision(chosen, member, reward, data);

      ui.displayMessage("");
      if (!ui.confirm("Are you sure you want to approve this request?")) {
        ui.displayMessage("  Nothing has been changed - the request is still"
            + " waiting.");
        ui.pause();
        continue;
      }

      ServiceResult<Redemption> result =
          service.processRedemption(chosen.getRedemptionId(), staffId);

      if (result.isSuccess()) {
        ui.displaySuccess(result.getMessage());
      } else {
        ui.displayError(result.getMessage());
      }
      ui.pause("Press ENTER to go back to the queue");
    }
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
   * Every redemption ever asked for, newest first.
   *
   * The listing answers "what has been redeemed"; picking a row answers "by
   * whom, against which stay" - which is the question the listing cannot fit
   * on one line.
   */
  private void displayRedemptionHistory() {
    while (true) {
      ui.startAction("REDEMPTION HISTORY");

      ListInterface<Redemption> history = copyOfRedemptions(data.getRedemptionList());
      history.sort(Comparator.comparing(Redemption::getRequestDate).reversed());

      Redemption chosen = ui.chooseRedemptionFromHistory(history, data);
      if (chosen == null) {
        ui.pause();
        return;
      }

      // The detail stands on its own screen, so nothing of the listing is
      // left around it to read by mistake.
      ui.clearScreen();
      ui.startAction("REDEMPTION " + chosen.getRedemptionId());
      ui.displayRedemptionDetail(chosen, data);
      ui.pause("Press ENTER to go back to the history");
    }
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
  /**
   * Asks which member to act on, showing them all first.
   *
   * Nobody should have to remember a member ID to use this module, so the
   * list is put on screen and a row number is what gets typed.
   */
  private Member promptForMember() {
    return promptForMember("Number of the member");
  }

  /**
   * Asks which member to act on, under wording that fits the action.
   *
   * @param prompt what the number is being asked for
   * @return the chosen member, or null if the officer went back
   */
  private Member promptForMember(String prompt) {
    ListInterface<Member> members = copyOf(data.getMemberList());
    members.sort(java.util.Comparator.comparing(Member::getMemberId));
    return ui.chooseMember(members, data, prompt, "There are no members yet.");
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
