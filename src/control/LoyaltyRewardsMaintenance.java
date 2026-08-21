package control;

import adt.ArrayList;
import adt.ArrayQueue;
import adt.Condition;
import adt.ListInterface;
import adt.QueueInterface;
import boundary.LoyaltyRewardsUI;
import dao.RewardInitializer;
import entity.LoyaltyNotification;
import entity.Member;
import entity.Redemption;
import entity.Reward;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/**
 * Loyalty & Rewards control class.
 *
 * Redemption points rule: member points are deducted only when a pending
 * redemption is successfully processed (not when the request is created).
 * Rejected redemptions never reduce member points.
 *
 * @author Kat Tan
 */
public class LoyaltyRewardsMaintenance {

  public static final String TIER_SILVER = "Silver";
  public static final String TIER_GOLD = "Gold";
  public static final String TIER_PLATINUM = "Platinum";
  public static final String TIER_DIAMOND = "Diamond";

  public static final int GOLD_THRESHOLD = 1000;
  public static final int PLATINUM_THRESHOLD = 5000;
  public static final int DIAMOND_THRESHOLD = 10000;

  public static final int HIGH_SILVER_POINTS = 750;
  public static final int HIGH_GOLD_POINTS = 2500;
  public static final int HIGH_PLATINUM_POINTS = 7500;
  public static final int HIGH_DIAMOND_POINTS = 12000;

  public static final int LONG_MEMBER_MONTHS_SILVER = 12;
  public static final int LONG_MEMBER_MONTHS_GOLD = 6;
  public static final int LONG_MEMBER_MONTHS_PLATINUM = 24;
  public static final int LONG_MEMBER_MONTHS_DIAMOND = 36;

  public static final int NEAR_NEXT_TIER_POINTS = 100;

  public static final String REPORT_ALL_TIERS = "ALL";
  public static final String REPORT_ALL_STATUSES = "ALL";

  public static final String TYPE_EXPIRING_POINTS = "EXPIRING_POINTS";
  public static final String TYPE_REDEMPTION = "REDEMPTION";
  public static final String TYPE_TIER_UPGRADE = "TIER_UPGRADE";

  private static final String REDEMPTION_EVENT_SUBMITTED = "SUBMITTED";
  private static final String REDEMPTION_EVENT_COMPLETED = "COMPLETED";
  private static final String REDEMPTION_EVENT_REJECTED = "REJECTED";

  private LoyaltyRewardsUI loyaltyRewardsUI = new LoyaltyRewardsUI();
  private ListInterface<Member> memberList = new ArrayList<>();
  private ListInterface<Reward> rewardList = new ArrayList<>();
  private ListInterface<Redemption> redemptionHistory = new ArrayList<>();
  private ListInterface<LoyaltyNotification> notificationList = new ArrayList<>();
  private QueueInterface<Redemption> pendingRedemptionQueue = new ArrayQueue<>();

  private int nextRedemptionNumber = 1;
  private int nextNotificationNumber = 1;

  public LoyaltyRewardsMaintenance() {
    rewardList = new RewardInitializer().initializeRewards();
  }

  public void runLoyaltyRewards() {
    int choice = 0;
    do {
      choice = loyaltyRewardsUI.getMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
          registerMember();
          break;
        case 2:
          searchMemberById();
          break;
        case 3:
          displayAllMembers();
          break;
        case 4:
          displayMembersSortedByPoints();
          break;
        case 5:
          displayExpiringPoints();
          break;
        case 6:
          accumulateMemberPoints();
          break;
        case 7:
          viewRewards();
          break;
        case 8:
          requestRewardRedemption();
          break;
        case 9:
          processNextPendingRedemption();
          break;
        case 10:
          viewRedemptionHistory();
          break;
        case 11:
          viewMemberNotifications();
          break;
        case 12:
          viewPersonalizedPromotion();
          break;
        case 13:
          runReportMenu();
          break;
      }
    } while (choice != 0);
  }

  /**
   * Registers a new member with an auto-generated loyalty ID and today's join
   * date after collecting name and contact details from the user.
   */
  public void registerMember() {
    Member member = loyaltyRewardsUI.inputMember();
    if (member == null) {
      loyaltyRewardsUI.displayMessage("Registration cancelled.");
      return;
    }

    member.setMemberId(generateNextMemberId());
    member.setJoinDate(LocalDate.now());
    member.setPoints(0);
    member.setTier(TIER_SILVER);
    member.setPointsExpiryDate(null);
    member.setTier(resolveTierAfterPoints(member.getTier(), member.getPoints()));

    if (findMemberById(member.getMemberId()) != null) {
      loyaltyRewardsUI.displayDuplicateMemberMessage(member.getMemberId());
      return;
    }

    memberList.add(member);
    loyaltyRewardsUI.displayRegistrationSuccess(member);
  }

  /**
   * Scans existing member IDs and returns the next sequential loyalty ID in
   * the format L0001, L0002, and so on. Uses the highest numeric suffix found,
   * not the current list size, so deleted records cannot cause duplicates.
   */
  private String generateNextMemberId() {
    int highestNumber = 0;

    for (int position = 1; position <= memberList.getNumberOfEntries(); position++) {
      Member existingMember = memberList.getEntry(position);
      if (existingMember == null || existingMember.getMemberId() == null) {
        continue;
      }

      String memberId = existingMember.getMemberId().trim();
      if (memberId.length() < 2 || !memberId.substring(0, 1).equalsIgnoreCase("L")) {
        continue;
      }

      try {
        int numericPart = Integer.parseInt(memberId.substring(1));
        if (numericPart > highestNumber) {
          highestNumber = numericPart;
        }
      } catch (NumberFormatException e) {
        // Ignore IDs that do not follow the L#### format.
      }
    }

    return String.format("L%04d", highestNumber + 1);
  }

  /**
   * Adds earned points to an existing member, recalculates tier, and reports
   * whether a tier upgrade occurred.
   */
  public void accumulateMemberPoints() {
    String memberId = loyaltyRewardsUI.inputAccumulateMemberId();
    if (memberId == null) {
      loyaltyRewardsUI.displayMessage("Points accumulation cancelled.");
      return;
    }

    Member member = findMemberById(memberId);
    if (member == null) {
      loyaltyRewardsUI.displayMemberNotFoundMessage(memberId);
      return;
    }

    Integer earnedPoints = loyaltyRewardsUI.inputEarnedPoints();
    if (earnedPoints == null) {
      loyaltyRewardsUI.displayMessage("Points accumulation cancelled.");
      return;
    }

    if (earnedPoints <= 0) {
      loyaltyRewardsUI.displayMessage(
          "Earned points must be greater than zero.");
      return;
    }

    int oldPoints = member.getPoints();
    int newPoints = oldPoints + earnedPoints;
    String oldTier = member.getTier();

    member.setPoints(newPoints);
    TierProgressionResult progression = checkTierProgression(member, newPoints);

    if (progression.isUpgraded()) {
      createTierUpgradeNotification(member,
          progression.getOldTier(), progression.getNewTier());
    }

    loyaltyRewardsUI.displayPointsAccumulationResult(
        member, oldPoints, newPoints, oldTier,
        progression.getNewTier(), progression.isUpgraded());
  }

  /**
   * Determines the tier a member qualifies for based on total points alone.
   */
  public String determineTier(int points) {
    if (points >= DIAMOND_THRESHOLD) {
      return TIER_DIAMOND;
    }
    if (points >= PLATINUM_THRESHOLD) {
      return TIER_PLATINUM;
    }
    if (points >= GOLD_THRESHOLD) {
      return TIER_GOLD;
    }
    return TIER_SILVER;
  }

  /**
   * Applies tier progression after points change. Members are upgraded when
   * they reach a higher threshold, but are never downgraded automatically.
   */
  public TierProgressionResult checkTierProgression(Member member, int newPoints) {
    String oldTier = member.getTier() == null ? TIER_SILVER : member.getTier();
    String resolvedTier = resolveTierAfterPoints(oldTier, newPoints);

    member.setTier(resolvedTier);

    boolean upgraded = getTierRank(resolvedTier) > getTierRank(oldTier);
    return new TierProgressionResult(oldTier, resolvedTier, upgraded);
  }

  /**
   * Looks up a member by ID and displays the full profile.
   */
  public void searchMemberById() {
    String memberId = loyaltyRewardsUI.inputSearchMemberId();
    if (memberId == null) {
      loyaltyRewardsUI.displayMessage("Search cancelled.");
      return;
    }

    Member member = findMemberById(memberId);
    if (member == null) {
      loyaltyRewardsUI.displayMemberNotFoundMessage(memberId);
      return;
    }

    loyaltyRewardsUI.displayMemberExistsMessage(member);
  }

  /**
   * Displays every member in registration order without changing memberList.
   */
  public void displayAllMembers() {
    loyaltyRewardsUI.displayMembers(copyMemberList(), "ALL MEMBERS");
  }

  /**
   * Displays a points-descending listing without reordering the master list.
   */
  public void displayMembersSortedByPoints() {
    ListInterface<Member> sortedMembers = copyMemberList();
    sortedMembers.sort(byPointsDescendingThenMemberId());
    loyaltyRewardsUI.displayMembers(sortedMembers,
        "MEMBERS SORTED BY POINTS (HIGHEST FIRST)");
  }

  /**
   * Lists members whose points expire on or before the selected cutoff date.
   */
  public void displayExpiringPoints() {
    LocalDate cutoff = loyaltyRewardsUI.inputCutoffDate();
    if (cutoff == null) {
      loyaltyRewardsUI.displayMessage("Search cancelled.");
      return;
    }

    ListInterface<Member> expiringMembers =
        findExpiringPoints(cutoff);

    for (int position = 1; position <= expiringMembers.getNumberOfEntries(); position++) {
      createExpiringPointsNotificationIfAbsent(expiringMembers.getEntry(position));
    }

    ListInterface<Member> sortedExpiring = copyList(expiringMembers);
    sortedExpiring.sort(byExpiryDateAscendingThenMemberId());

    loyaltyRewardsUI.displayMembers(sortedExpiring,
        "MEMBERS WITH POINTS EXPIRING ON OR BEFORE " + cutoff);
  }

  /**
   * Returns members whose points expire on or before the cutoff date and who
   * still hold a positive points balance.
   */
  public ListInterface<Member> findExpiringPoints(LocalDate cutoff) {
    return memberList.filter(expiringOnOrBefore(cutoff));
  }

  /**
   * Uses the List ADT's linear search to find the first member with the given
   * member ID.
   */
  public Member findMemberById(String memberId) {
    if (memberId == null) {
      return null;
    }

    String trimmedId = memberId.trim();
    return memberList.search(hasMemberId(trimmedId));
  }

  /**
   * Checks whether a member ID is already registered.
   */
  public boolean isMemberRegistered(String memberId) {
    return findMemberById(memberId) != null;
  }

  /**
   * Displays the reward catalogue.
   */
  public void viewRewards() {
    loyaltyRewardsUI.displayRewards(rewardList);
  }

  /**
   * Creates a pending redemption request without deducting member points.
   * Points are deducted later during FIFO processing when the request is
   * successfully completed.
   */
  public void requestRewardRedemption() {
    String memberId = loyaltyRewardsUI.inputRedemptionMemberId();
    if (memberId == null) {
      loyaltyRewardsUI.displayMessage("Redemption request cancelled.");
      return;
    }

    Member member = findMemberById(memberId);
    if (member == null) {
      loyaltyRewardsUI.displayMemberNotFoundMessage(memberId);
      return;
    }

    loyaltyRewardsUI.displayMemberSummaryForRedemption(member);
    loyaltyRewardsUI.displayRewards(rewardList);

    String rewardId = loyaltyRewardsUI.inputRewardId();
    if (rewardId == null) {
      loyaltyRewardsUI.displayMessage("Redemption request cancelled.");
      return;
    }

    Reward reward = findRewardById(rewardId);
    if (reward == null) {
      loyaltyRewardsUI.displayInvalidRewardMessage(rewardId);
      return;
    }

    if (member.getPoints() < reward.getPointsRequired()) {
      loyaltyRewardsUI.displayInsufficientPointsMessage(
          member.getPoints(), reward.getPointsRequired());
      return;
    }

    Redemption redemption = new Redemption(
        generateRedemptionId(),
        member.getMemberId(),
        reward.getRewardId(),
        reward.getPointsRequired(),
        LocalDate.now(),
        Redemption.STATUS_PENDING);

    redemptionHistory.add(redemption);
    pendingRedemptionQueue.enqueue(redemption);

    createRedemptionNotification(
        member.getMemberId(),
        redemption.getRedemptionId(),
        REDEMPTION_EVENT_SUBMITTED,
        String.format("[%s:%s] Redemption request submitted for %s (%d points). "
            + "Status: PENDING.",
            redemption.getRedemptionId(), REDEMPTION_EVENT_SUBMITTED,
            reward.getRewardName(), redemption.getPointsUsed()));

    loyaltyRewardsUI.displayRedemptionRequestResult(redemption, reward, member);
  }

  /**
   * Processes the front pending redemption request in FIFO order.
   */
  public void processNextPendingRedemption() {
    if (pendingRedemptionQueue.isEmpty()) {
      loyaltyRewardsUI.displayEmptyPendingQueueMessage();
      return;
    }

    Redemption redemption = pendingRedemptionQueue.dequeue();
    Reward reward = findRewardById(redemption.getRewardId());
    Member member = findMemberById(redemption.getMemberId());

    if (member == null) {
      redemption.setStatus(Redemption.STATUS_REJECTED);
      createRedemptionNotification(
          redemption.getMemberId(),
          redemption.getRedemptionId(),
          REDEMPTION_EVENT_REJECTED,
          String.format("[%s:%s] Redemption rejected because the member no longer exists.",
              redemption.getRedemptionId(), REDEMPTION_EVENT_REJECTED));
      loyaltyRewardsUI.displayProcessRedemptionResult(
          redemption, reward, null, 0, 0,
          "Redemption rejected: member no longer exists.");
      return;
    }

    if (reward == null) {
      redemption.setStatus(Redemption.STATUS_REJECTED);
      createRedemptionNotification(
          member.getMemberId(),
          redemption.getRedemptionId(),
          REDEMPTION_EVENT_REJECTED,
          String.format("[%s:%s] Redemption rejected because the reward no longer exists.",
              redemption.getRedemptionId(), REDEMPTION_EVENT_REJECTED));
      loyaltyRewardsUI.displayProcessRedemptionResult(
          redemption, null, member, member.getPoints(), member.getPoints(),
          "Redemption rejected: reward no longer exists.");
      return;
    }

    if (member.getPoints() < redemption.getPointsUsed()) {
      redemption.setStatus(Redemption.STATUS_REJECTED);
      createRedemptionNotification(
          member.getMemberId(),
          redemption.getRedemptionId(),
          REDEMPTION_EVENT_REJECTED,
          String.format("[%s:%s] Redemption rejected due to insufficient points "
              + "at processing time. No points were deducted.",
              redemption.getRedemptionId(), REDEMPTION_EVENT_REJECTED));
      loyaltyRewardsUI.displayProcessRedemptionResult(
          redemption, reward, member, member.getPoints(), member.getPoints(),
          "Redemption rejected: insufficient points at processing time.");
      return;
    }

    int oldPoints = member.getPoints();
    int newPoints = oldPoints - redemption.getPointsUsed();
    member.setPoints(newPoints);
    redemption.setStatus(Redemption.STATUS_COMPLETED);

    createRedemptionNotification(
        member.getMemberId(),
        redemption.getRedemptionId(),
        REDEMPTION_EVENT_COMPLETED,
        String.format("[%s:%s] Redemption completed for %s. %d points deducted.",
            redemption.getRedemptionId(), REDEMPTION_EVENT_COMPLETED,
            reward.getRewardName(), redemption.getPointsUsed()));

    loyaltyRewardsUI.displayProcessRedemptionResult(
        redemption, reward, member, oldPoints, newPoints,
        "Redemption completed successfully. Points deducted.");
  }

  /**
   * Displays every redemption record, including pending, completed, and
   * rejected requests.
   */
  public void viewRedemptionHistory() {
    ListInterface<Redemption> sortedHistory = copyRedemptionHistory();
    sortedHistory.sort(byRedemptionIdAscending());
    loyaltyRewardsUI.displayRedemptions(sortedHistory, "REDEMPTION HISTORY");
  }

  /**
   * Displays notifications for a member with optional unread/type filters.
   */
  public void viewMemberNotifications() {
    String memberId = loyaltyRewardsUI.inputNotificationMemberId();
    if (memberId == null) {
      loyaltyRewardsUI.displayMessage("Notification view cancelled.");
      return;
    }

    Member member = findMemberById(memberId);
    if (member == null) {
      loyaltyRewardsUI.displayMemberNotFoundMessage(memberId);
      return;
    }

    int filterChoice = loyaltyRewardsUI.getNotificationFilterChoice();
    if (filterChoice == 0) {
      loyaltyRewardsUI.displayMessage("Notification view cancelled.");
      return;
    }

    String typeFilter = null;
    if (filterChoice == 3) {
      typeFilter = loyaltyRewardsUI.inputNotificationType();
      if (typeFilter == null) {
        loyaltyRewardsUI.displayMessage("Notification view cancelled.");
        return;
      }
    }

    ListInterface<LoyaltyNotification> notifications =
        getMemberNotifications(memberId, filterChoice, typeFilter);
    notifications.sort(byNotificationIdAscending());

    loyaltyRewardsUI.displayNotifications(
        notifications, memberId, filterChoice, typeFilter);
  }

  /**
   * Looks up a member and determines the promotion tailored to that member's
   * tier, points balance, and membership duration.
   *
   * @return the personalized promotion result, or null if the member was not
   *         found
   */
  public PersonalizedPromotionResult getPersonalizedPromotion(String memberId) {
    Member member = findMemberById(memberId);
    if (member == null) {
      return null;
    }
    return determinePersonalizedPromotion(member);
  }

  /**
   * Prompts for a member ID and displays the promotion selected for that member.
   */
  public void viewPersonalizedPromotion() {
    String memberId = loyaltyRewardsUI.inputPromotionMemberId();
    if (memberId == null) {
      loyaltyRewardsUI.displayMessage("Personalized promotion view cancelled.");
      return;
    }

    PersonalizedPromotionResult promotion = getPersonalizedPromotion(memberId);
    if (promotion == null) {
      loyaltyRewardsUI.displayMemberNotFoundMessage(memberId);
      return;
    }

    loyaltyRewardsUI.displayPersonalizedPromotion(
        promotion.getMember(),
        promotion.getMembershipMonths(),
        promotion.getPromotionTitle(),
        promotion.getPromotionDetails(),
        promotion.getEligibilityReason());
  }

  /**
   * Selects a promotion using the member's tier as the primary rule and points
   * or membership duration as secondary personalization criteria.
   */
  public PersonalizedPromotionResult determinePersonalizedPromotion(Member member) {
    int membershipMonths = getMembershipMonths(member);
    String tier = member.getTier() == null ? TIER_SILVER : member.getTier();
    int points = member.getPoints();

    if (TIER_DIAMOND.equalsIgnoreCase(tier)) {
      return buildDiamondPromotion(member, points, membershipMonths);
    }
    if (TIER_PLATINUM.equalsIgnoreCase(tier)) {
      return buildPlatinumPromotion(member, points, membershipMonths);
    }
    if (TIER_GOLD.equalsIgnoreCase(tier)) {
      return buildGoldPromotion(member, points, membershipMonths);
    }
    return buildSilverPromotion(member, points, membershipMonths);
  }

  private PersonalizedPromotionResult buildSilverPromotion(
      Member member, int points, int membershipMonths) {
    if (points >= HIGH_SILVER_POINTS) {
      return new PersonalizedPromotionResult(
          member,
          membershipMonths,
          "Silver Milestone Bonus",
          "Earn 100 bonus points on your next resort stay.",
          "Selected because the member has at least " + HIGH_SILVER_POINTS
              + " points within the Silver tier.");
    }
    if (membershipMonths >= LONG_MEMBER_MONTHS_SILVER) {
      return new PersonalizedPromotionResult(
          member,
          membershipMonths,
          "Silver Loyalty Dining Offer",
          "Enjoy 10% off food and beverage during your next visit.",
          "Selected because the member has been registered for at least "
              + LONG_MEMBER_MONTHS_SILVER + " months.");
    }
    return new PersonalizedPromotionResult(
        member,
        membershipMonths,
        "Standard Silver Welcome Offer",
        "Earn double loyalty points on weekday check-ins.",
        "Selected as the standard Silver-tier offer for newer members.");
  }

  private PersonalizedPromotionResult buildGoldPromotion(
      Member member, int points, int membershipMonths) {
    if (points >= HIGH_GOLD_POINTS) {
      return new PersonalizedPromotionResult(
          member,
          membershipMonths,
          "Gold Premium Room Upgrade",
          "Receive a complimentary one-category room upgrade on your next booking.",
          "Selected because the member has at least " + HIGH_GOLD_POINTS
              + " Gold-tier points.");
    }
    if (membershipMonths >= LONG_MEMBER_MONTHS_GOLD) {
      return new PersonalizedPromotionResult(
          member,
          membershipMonths,
          "Gold Member Spa Appreciation",
          "Receive 15% off any spa treatment this month.",
          "Selected because the member has been registered for at least "
              + LONG_MEMBER_MONTHS_GOLD + " months.");
    }
    return new PersonalizedPromotionResult(
        member,
        membershipMonths,
        "Enhanced Gold Member Offer",
        "Enjoy free late checkout until 2:00 PM on your next stay.",
        "Selected as the standard enhanced offer for Gold members.");
  }

  private PersonalizedPromotionResult buildPlatinumPromotion(
      Member member, int points, int membershipMonths) {
    if (points >= HIGH_PLATINUM_POINTS) {
      return new PersonalizedPromotionResult(
          member,
          membershipMonths,
          "Platinum Elite Dining Package",
          "Receive one complimentary dinner buffet for two.",
          "Selected because the member has at least " + HIGH_PLATINUM_POINTS
              + " Platinum-tier points.");
    }
    if (membershipMonths >= LONG_MEMBER_MONTHS_PLATINUM) {
      return new PersonalizedPromotionResult(
          member,
          membershipMonths,
          "Platinum Anniversary Reward",
          "Receive 500 bonus loyalty points as a long-membership thank-you.",
          "Selected because the member has been registered for at least "
              + LONG_MEMBER_MONTHS_PLATINUM + " months.");
    }
    return new PersonalizedPromotionResult(
        member,
        membershipMonths,
        "Premium Platinum Priority Offer",
        "Receive priority early check-in and a welcome amenity on arrival.",
        "Selected as the standard premium offer for Platinum members.");
  }

  private PersonalizedPromotionResult buildDiamondPromotion(
      Member member, int points, int membershipMonths) {
    if (points >= HIGH_DIAMOND_POINTS) {
      return new PersonalizedPromotionResult(
          member,
          membershipMonths,
          "Diamond Signature Suite Experience",
          "Receive one complimentary suite-category night subject to availability.",
          "Selected because the member has at least " + HIGH_DIAMOND_POINTS
              + " Diamond-tier points.");
    }
    if (membershipMonths >= LONG_MEMBER_MONTHS_DIAMOND) {
      return new PersonalizedPromotionResult(
          member,
          membershipMonths,
          "Diamond Legacy Concierge Privilege",
          "Receive a dedicated concierge contact line for all future bookings.",
          "Selected because the member has been registered for at least "
              + LONG_MEMBER_MONTHS_DIAMOND + " months.");
    }
    return new PersonalizedPromotionResult(
        member,
        membershipMonths,
        "Exclusive Diamond Transfer Offer",
        "Receive one complimentary airport transfer during your next stay.",
        "Selected as the standard exclusive offer for Diamond members.");
  }

  private int getMembershipMonths(Member member) {
    if (member.getJoinDate() == null) {
      return 0;
    }
    return (int) ChronoUnit.MONTHS.between(member.getJoinDate(), LocalDate.now());
  }

  private void createTierUpgradeNotification(Member member, String oldTier, String newTier) {
    String transitionToken = oldTier + "->" + newTier;
    if (hasTierUpgradeNotification(member.getMemberId(), transitionToken)) {
      return;
    }

    String message = String.format(
        "[%s] Congratulations! Your tier has been upgraded from %s to %s.",
        transitionToken, oldTier, newTier);
    addNotification(member.getMemberId(), TYPE_TIER_UPGRADE, message);
  }

  private void createRedemptionNotification(String memberId, String redemptionId,
      String eventToken, String message) {
    if (hasRedemptionEventNotification(redemptionId, eventToken)) {
      return;
    }
    addNotification(memberId, TYPE_REDEMPTION, message);
  }

  private void createExpiringPointsNotificationIfAbsent(Member member) {
    if (member == null || member.getPoints() <= 0
        || member.getPointsExpiryDate() == null) {
      return;
    }

    LocalDate expiryDate = member.getPointsExpiryDate();
    if (hasExpiringPointsNotification(member.getMemberId(), expiryDate)) {
      return;
    }

    String message = String.format(
        "[EXPIRY:%s] Warning: you have %d points expiring on %s. "
        + "Please redeem or use them before they expire.",
        expiryDate, member.getPoints(), expiryDate);
    addNotification(member.getMemberId(), TYPE_EXPIRING_POINTS, message);
  }

  private void addNotification(String memberId, String type, String message) {
    LoyaltyNotification notification = new LoyaltyNotification(
        generateNotificationId(),
        memberId,
        type,
        message,
        LocalDate.now(),
        false);
    notificationList.add(notification);
  }

  private ListInterface<LoyaltyNotification> getMemberNotifications(
      String memberId, int filterChoice, String typeFilter) {
    ListInterface<LoyaltyNotification> filtered = new ArrayList<>();

    for (int position = 1; position <= notificationList.getNumberOfEntries(); position++) {
      LoyaltyNotification notification = notificationList.getEntry(position);
      if (notification == null
          || !memberId.equalsIgnoreCase(notification.getMemberId())) {
        continue;
      }

      if (filterChoice == 2 && notification.isRead()) {
        continue;
      }

      if (filterChoice == 3 && (typeFilter == null
          || !typeFilter.equalsIgnoreCase(notification.getType()))) {
        continue;
      }

      filtered.add(notification);
    }

    return filtered;
  }

  private boolean hasTierUpgradeNotification(String memberId, String transitionToken) {
    return notificationList.search(notification ->
        TYPE_TIER_UPGRADE.equals(notification.getType())
            && memberId.equalsIgnoreCase(notification.getMemberId())
            && notification.getMessage().contains("[" + transitionToken + "]")) != null;
  }

  private boolean hasRedemptionEventNotification(String redemptionId, String eventToken) {
    return notificationList.search(notification ->
        TYPE_REDEMPTION.equals(notification.getType())
            && notification.getMessage().contains(
                "[" + redemptionId + ":" + eventToken + "]")) != null;
  }

  private boolean hasExpiringPointsNotification(String memberId, LocalDate expiryDate) {
    return notificationList.search(notification ->
        TYPE_EXPIRING_POINTS.equals(notification.getType())
            && memberId.equalsIgnoreCase(notification.getMemberId())
            && notification.getMessage().contains("[EXPIRY:" + expiryDate + "]")) != null;
  }

  private String generateNotificationId() {
    String notificationId = String.format("NT%04d", nextNotificationNumber);
    nextNotificationNumber++;
    return notificationId;
  }

  private Comparator<LoyaltyNotification> byNotificationIdAscending() {
    return (first, second) ->
        first.getNotificationId().compareToIgnoreCase(second.getNotificationId());
  }

  /**
   * Uses the List ADT's linear search to find a reward by ID.
   */
  public Reward findRewardById(String rewardId) {
    if (rewardId == null) {
      return null;
    }
    return rewardList.search(hasRewardId(rewardId.trim()));
  }

  private String generateRedemptionId() {
    String redemptionId = String.format("RD%04d", nextRedemptionNumber);
    nextRedemptionNumber++;
    return redemptionId;
  }

  private Condition<Reward> hasRewardId(String rewardId) {
    return reward -> reward.getRewardId() != null
        && reward.getRewardId().equalsIgnoreCase(rewardId);
  }

  private Comparator<Redemption> byRedemptionIdAscending() {
    return (first, second) ->
        first.getRedemptionId().compareToIgnoreCase(second.getRedemptionId());
  }

  private ListInterface<Redemption> copyRedemptionHistory() {
    ListInterface<Redemption> copy = new ArrayList<>();
    for (int position = 1; position <= redemptionHistory.getNumberOfEntries(); position++) {
      copy.add(redemptionHistory.getEntry(position));
    }
    return copy;
  }

  private Condition<Member> hasMemberId(String memberId) {
    return member -> member.getMemberId() != null
        && member.getMemberId().equalsIgnoreCase(memberId);
  }

  private Condition<Member> expiringOnOrBefore(LocalDate cutoff) {
    return member -> member.getPointsExpiryDate() != null
        && member.getPoints() > 0
        && !member.getPointsExpiryDate().isAfter(cutoff);
  }

  private Comparator<Member> byPointsDescendingThenMemberId() {
    return (first, second) -> {
      int pointsCompare = Integer.compare(second.getPoints(), first.getPoints());
      if (pointsCompare != 0) {
        return pointsCompare;
      }
      return first.getMemberId().compareToIgnoreCase(second.getMemberId());
    };
  }

  private Comparator<Member> byExpiryDateAscendingThenMemberId() {
    return (first, second) -> {
      int dateCompare =
          first.getPointsExpiryDate().compareTo(second.getPointsExpiryDate());
      if (dateCompare != 0) {
        return dateCompare;
      }
      return first.getMemberId().compareToIgnoreCase(second.getMemberId());
    };
  }

  private ListInterface<Member> copyMemberList() {
    return copyList(memberList);
  }

  private ListInterface<Member> copyList(ListInterface<Member> source) {
    ListInterface<Member> copy = new ArrayList<>();
    for (int position = 1; position <= source.getNumberOfEntries(); position++) {
      copy.add(source.getEntry(position));
    }
    return copy;
  }

  private void runReportMenu() {
    int choice;
    do {
      choice = loyaltyRewardsUI.getReportMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
          generateMembershipTierPerformanceReport();
          break;
        case 2:
          generateRedemptionAnalysisReport();
          break;
      }
    } while (choice != 0);
  }

  /**
   * REPORT 1: Loyalty Membership & Tier Performance Report.
   *
   * Combines multiple filter criteria, sorts with the List ADT merge sort, and
   * prints management summary metrics.
   */
  public void generateMembershipTierPerformanceReport() {
    loyaltyRewardsUI.displayReportHeader("LOYALTY MEMBERSHIP & TIER PERFORMANCE REPORT");

    String tierFilter = loyaltyRewardsUI.inputMembershipReportTierFilter();
    if (tierFilter == null) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    Integer minPoints = loyaltyRewardsUI.inputOptionalReportPoints(
        "Minimum points (blank for none, C to cancel): ");
    if (minPoints == null && loyaltyRewardsUI.wasReportInputCancelled()) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    Integer maxPoints = loyaltyRewardsUI.inputOptionalReportPoints(
        "Maximum points (blank for none, C to cancel): ");
    if (maxPoints == null && loyaltyRewardsUI.wasReportInputCancelled()) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    if (minPoints != null && maxPoints != null && minPoints > maxPoints) {
      loyaltyRewardsUI.displayMessage(
          "Minimum points cannot be greater than maximum points.");
      loyaltyRewardsUI.displayReportFooter();
      return;
    }

    LocalDate joinStart = loyaltyRewardsUI.inputOptionalReportDate(
        "Join date on/after (YYYY-MM-DD, blank to skip, C to cancel): ");
    if (joinStart == null && loyaltyRewardsUI.wasReportInputCancelled()) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    LocalDate joinEnd = loyaltyRewardsUI.inputOptionalReportDate(
        "Join date on/before (YYYY-MM-DD, blank to skip, C to cancel): ");
    if (joinEnd == null && loyaltyRewardsUI.wasReportInputCancelled()) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    if (joinStart != null && joinEnd != null && joinStart.isAfter(joinEnd)) {
      loyaltyRewardsUI.displayMessage("Join start date cannot be after join end date.");
      loyaltyRewardsUI.displayReportFooter();
      return;
    }

    LocalDate expiryBefore = loyaltyRewardsUI.inputOptionalReportDate(
        "Points expiry on/before (YYYY-MM-DD, blank to skip, C to cancel): ");
    if (expiryBefore == null && loyaltyRewardsUI.wasReportInputCancelled()) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    ListInterface<Member> filteredMembers = filterMembersForReport(
        tierFilter, minPoints, maxPoints, joinStart, joinEnd, expiryBefore);
    filteredMembers.sort(byPointsDescendingThenMemberId());

    MembershipReportSummary summary = buildMembershipReportSummary(filteredMembers);
    String filterDescription = buildMembershipReportFilterDescription(
        tierFilter, minPoints, maxPoints, joinStart, joinEnd, expiryBefore);

    loyaltyRewardsUI.displayMembershipTierPerformanceReport(
        filterDescription,
        filteredMembers,
        summary.getTotalMembers(),
        summary.getTotalPoints(),
        summary.getAveragePoints(),
        summary.getSilverCount(),
        summary.getGoldCount(),
        summary.getPlatinumCount(),
        summary.getDiamondCount(),
        summary.getNearNextTierCount(),
        summary.getHighestPointsMember());
    loyaltyRewardsUI.displayReportFooter();
  }

  /**
   * REPORT 2: Redemption Analysis Report.
   */
  public void generateRedemptionAnalysisReport() {
    loyaltyRewardsUI.displayReportHeader("REDEMPTION ANALYSIS REPORT");

    String statusFilter = loyaltyRewardsUI.inputRedemptionReportStatusFilter();
    if (statusFilter == null) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    Integer minPoints = loyaltyRewardsUI.inputOptionalReportPoints(
        "Minimum points redeemed (blank for none, C to cancel): ");
    if (minPoints == null && loyaltyRewardsUI.wasReportInputCancelled()) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    Integer maxPoints = loyaltyRewardsUI.inputOptionalReportPoints(
        "Maximum points redeemed (blank for none, C to cancel): ");
    if (maxPoints == null && loyaltyRewardsUI.wasReportInputCancelled()) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    if (minPoints != null && maxPoints != null && minPoints > maxPoints) {
      loyaltyRewardsUI.displayMessage(
          "Minimum points cannot be greater than maximum points.");
      loyaltyRewardsUI.displayReportFooter();
      return;
    }

    String tierFilter = loyaltyRewardsUI.inputMembershipReportTierFilter();
    if (tierFilter == null) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    LocalDate startDate = loyaltyRewardsUI.inputOptionalReportDate(
        "Request date on/after (YYYY-MM-DD, blank to skip, C to cancel): ");
    if (startDate == null && loyaltyRewardsUI.wasReportInputCancelled()) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    LocalDate endDate = loyaltyRewardsUI.inputOptionalReportDate(
        "Request date on/before (YYYY-MM-DD, blank to skip, C to cancel): ");
    if (endDate == null && loyaltyRewardsUI.wasReportInputCancelled()) {
      loyaltyRewardsUI.displayMessage("Report cancelled.");
      return;
    }

    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      loyaltyRewardsUI.displayMessage("Start date cannot be after end date.");
      loyaltyRewardsUI.displayReportFooter();
      return;
    }

    ListInterface<RedemptionReportRow> filteredRows = filterRedemptionsForReport(
        statusFilter, minPoints, maxPoints, tierFilter, startDate, endDate);
    filteredRows.sort(byRedemptionPointsDescendingThenId());

    RedemptionReportSummary summary = buildRedemptionReportSummary(filteredRows);
    String filterDescription = buildRedemptionReportFilterDescription(
        statusFilter, minPoints, maxPoints, tierFilter, startDate, endDate);

    ListInterface<Redemption> reportRedemptions = new ArrayList<>();
    String[] memberNames = new String[filteredRows.getNumberOfEntries()];
    String[] memberTiers = new String[filteredRows.getNumberOfEntries()];
    String[] rewardNames = new String[filteredRows.getNumberOfEntries()];

    for (int position = 1; position <= filteredRows.getNumberOfEntries(); position++) {
      RedemptionReportRow row = filteredRows.getEntry(position);
      reportRedemptions.add(row.getRedemption());
      int index = position - 1;
      memberNames[index] = row.getMemberName();
      memberTiers[index] = row.getMemberTier();
      rewardNames[index] = row.getRewardName();
    }

    loyaltyRewardsUI.displayRedemptionAnalysisReport(
        filterDescription,
        reportRedemptions,
        memberNames,
        memberTiers,
        rewardNames,
        summary.getTotalMatches(),
        summary.getCompletedCount(),
        summary.getPendingCount(),
        summary.getRejectedCount(),
        summary.getTotalCompletedPoints(),
        summary.getAverageCompletedPoints(),
        summary.getHighestCompletedPoints());
    loyaltyRewardsUI.displayReportFooter();
  }

  private ListInterface<Member> filterMembersForReport(String tierFilter,
      Integer minPoints, Integer maxPoints, LocalDate joinStart,
      LocalDate joinEnd, LocalDate expiryBefore) {
    return memberList.filter(member -> matchesMembershipReportCriteria(
        member, tierFilter, minPoints, maxPoints, joinStart, joinEnd, expiryBefore));
  }

  private boolean matchesMembershipReportCriteria(Member member, String tierFilter,
      Integer minPoints, Integer maxPoints, LocalDate joinStart,
      LocalDate joinEnd, LocalDate expiryBefore) {
    if (member == null) {
      return false;
    }

    if (!REPORT_ALL_TIERS.equalsIgnoreCase(tierFilter)) {
      String memberTier = member.getTier() == null ? TIER_SILVER : member.getTier();
      if (!tierFilter.equalsIgnoreCase(memberTier)) {
        return false;
      }
    }

    if (minPoints != null && member.getPoints() < minPoints) {
      return false;
    }

    if (maxPoints != null && member.getPoints() > maxPoints) {
      return false;
    }

    if (joinStart != null && (member.getJoinDate() == null
        || member.getJoinDate().isBefore(joinStart))) {
      return false;
    }

    if (joinEnd != null && (member.getJoinDate() == null
        || member.getJoinDate().isAfter(joinEnd))) {
      return false;
    }

    if (expiryBefore != null) {
      if (member.getPointsExpiryDate() == null
          || member.getPointsExpiryDate().isAfter(expiryBefore)) {
        return false;
      }
    }

    return true;
  }

  private MembershipReportSummary buildMembershipReportSummary(
      ListInterface<Member> members) {
    int totalMembers = members.getNumberOfEntries();
    int totalPoints = 0;
    Member highestPointsMember = null;

    for (int position = 1; position <= totalMembers; position++) {
      Member member = members.getEntry(position);
      totalPoints += member.getPoints();
      if (highestPointsMember == null
          || member.getPoints() > highestPointsMember.getPoints()
          || (member.getPoints() == highestPointsMember.getPoints()
              && member.getMemberId().compareToIgnoreCase(
                  highestPointsMember.getMemberId()) < 0)) {
        highestPointsMember = member;
      }
    }

    double averagePoints = totalMembers == 0 ? 0.0 : (double) totalPoints / totalMembers;

    return new MembershipReportSummary(
        totalMembers,
        totalPoints,
        averagePoints,
        members.countIf(hasTier(TIER_SILVER)),
        members.countIf(hasTier(TIER_GOLD)),
        members.countIf(hasTier(TIER_PLATINUM)),
        members.countIf(hasTier(TIER_DIAMOND)),
        highestPointsMember,
        countMembersNearNextTier(members));
  }

  private ListInterface<RedemptionReportRow> filterRedemptionsForReport(
      String statusFilter, Integer minPoints, Integer maxPoints,
      String tierFilter, LocalDate startDate, LocalDate endDate) {
    ListInterface<RedemptionReportRow> rows = new ArrayList<>();

    for (int position = 1; position <= redemptionHistory.getNumberOfEntries(); position++) {
      Redemption redemption = redemptionHistory.getEntry(position);
      Member member = findMemberById(redemption.getMemberId());
      Reward reward = findRewardById(redemption.getRewardId());

      if (!matchesRedemptionReportCriteria(redemption, member, statusFilter,
          minPoints, maxPoints, tierFilter, startDate, endDate)) {
        continue;
      }

      String memberName = member == null ? "(unknown)" : member.getName();
      String memberTier = member == null || member.getTier() == null
          ? "-" : member.getTier();
      String rewardName = reward == null ? redemption.getRewardId() : reward.getRewardName();

      rows.add(new RedemptionReportRow(
          redemption, memberName, memberTier, rewardName));
    }

    return rows;
  }

  private boolean matchesRedemptionReportCriteria(Redemption redemption, Member member,
      String statusFilter, Integer minPoints, Integer maxPoints,
      String tierFilter, LocalDate startDate, LocalDate endDate) {
    if (redemption == null) {
      return false;
    }

    if (!REPORT_ALL_STATUSES.equalsIgnoreCase(statusFilter)
        && !statusFilter.equalsIgnoreCase(redemption.getStatus())) {
      return false;
    }

    if (minPoints != null && redemption.getPointsUsed() < minPoints) {
      return false;
    }

    if (maxPoints != null && redemption.getPointsUsed() > maxPoints) {
      return false;
    }

    if (!REPORT_ALL_TIERS.equalsIgnoreCase(tierFilter)) {
      if (member == null || member.getTier() == null
          || !tierFilter.equalsIgnoreCase(member.getTier())) {
        return false;
      }
    }

    if (startDate != null && (redemption.getRequestDate() == null
        || redemption.getRequestDate().isBefore(startDate))) {
      return false;
    }

    if (endDate != null && (redemption.getRequestDate() == null
        || redemption.getRequestDate().isAfter(endDate))) {
      return false;
    }

    return true;
  }

  private RedemptionReportSummary buildRedemptionReportSummary(
      ListInterface<RedemptionReportRow> rows) {
    int totalMatches = rows.getNumberOfEntries();
    int completedCount = 0;
    int pendingCount = 0;
    int rejectedCount = 0;
    int totalCompletedPoints = 0;
    int highestCompletedPoints = 0;

    for (int position = 1; position <= totalMatches; position++) {
      Redemption redemption = rows.getEntry(position).getRedemption();
      String status = redemption.getStatus();

      if (Redemption.STATUS_COMPLETED.equalsIgnoreCase(status)) {
        completedCount++;
        totalCompletedPoints += redemption.getPointsUsed();
        if (redemption.getPointsUsed() > highestCompletedPoints) {
          highestCompletedPoints = redemption.getPointsUsed();
        }
      } else if (Redemption.STATUS_PENDING.equalsIgnoreCase(status)) {
        pendingCount++;
      } else if (Redemption.STATUS_REJECTED.equalsIgnoreCase(status)) {
        rejectedCount++;
      }
    }

    double averageCompleted = completedCount == 0
        ? 0.0 : (double) totalCompletedPoints / completedCount;

    return new RedemptionReportSummary(
        totalMatches,
        completedCount,
        pendingCount,
        rejectedCount,
        totalCompletedPoints,
        averageCompleted,
        highestCompletedPoints);
  }

  private int countMembersNearNextTier(ListInterface<Member> members) {
    int count = 0;
    for (int position = 1; position <= members.getNumberOfEntries(); position++) {
      if (isNearNextTier(members.getEntry(position))) {
        count++;
      }
    }
    return count;
  }

  private boolean isNearNextTier(Member member) {
    int points = member.getPoints();
    String tier = member.getTier() == null ? TIER_SILVER : member.getTier();

    if (TIER_SILVER.equalsIgnoreCase(tier)) {
      return points >= GOLD_THRESHOLD - NEAR_NEXT_TIER_POINTS
          && points < GOLD_THRESHOLD;
    }
    if (TIER_GOLD.equalsIgnoreCase(tier)) {
      return points >= PLATINUM_THRESHOLD - NEAR_NEXT_TIER_POINTS
          && points < PLATINUM_THRESHOLD;
    }
    if (TIER_PLATINUM.equalsIgnoreCase(tier)) {
      return points >= DIAMOND_THRESHOLD - NEAR_NEXT_TIER_POINTS
          && points < DIAMOND_THRESHOLD;
    }
    return false;
  }

  private Condition<Member> hasTier(String tier) {
    return member -> tier.equalsIgnoreCase(
        member.getTier() == null ? TIER_SILVER : member.getTier());
  }

  private Comparator<RedemptionReportRow> byRedemptionPointsDescendingThenId() {
    return (first, second) -> {
      int pointsCompare = Integer.compare(
          second.getRedemption().getPointsUsed(),
          first.getRedemption().getPointsUsed());
      if (pointsCompare != 0) {
        return pointsCompare;
      }
      return first.getRedemption().getRedemptionId()
          .compareToIgnoreCase(second.getRedemption().getRedemptionId());
    };
  }

  private String buildMembershipReportFilterDescription(String tierFilter,
      Integer minPoints, Integer maxPoints, LocalDate joinStart,
      LocalDate joinEnd, LocalDate expiryBefore) {
    StringBuilder description = new StringBuilder();
    description.append("Tier = ")
        .append(REPORT_ALL_TIERS.equalsIgnoreCase(tierFilter) ? "All" : tierFilter);

    if (minPoints != null) {
      description.append(", Min points = ").append(minPoints);
    }
    if (maxPoints != null) {
      description.append(", Max points = ").append(maxPoints);
    }
    if (joinStart != null) {
      description.append(", Join on/after = ").append(joinStart);
    }
    if (joinEnd != null) {
      description.append(", Join on/before = ").append(joinEnd);
    }
    if (expiryBefore != null) {
      description.append(", Expiry on/before = ").append(expiryBefore);
    }
    return description.toString();
  }

  private String buildRedemptionReportFilterDescription(String statusFilter,
      Integer minPoints, Integer maxPoints, String tierFilter,
      LocalDate startDate, LocalDate endDate) {
    StringBuilder description = new StringBuilder();
    description.append("Status = ")
        .append(REPORT_ALL_STATUSES.equalsIgnoreCase(statusFilter) ? "All" : statusFilter);

    if (minPoints != null) {
      description.append(", Min points = ").append(minPoints);
    }
    if (maxPoints != null) {
      description.append(", Max points = ").append(maxPoints);
    }
    if (!REPORT_ALL_TIERS.equalsIgnoreCase(tierFilter)) {
      description.append(", Member tier = ").append(tierFilter);
    }
    if (startDate != null) {
      description.append(", Request on/after = ").append(startDate);
    }
    if (endDate != null) {
      description.append(", Request on/before = ").append(endDate);
    }
    return description.toString();
  }

  /**
   * Keeps the higher of the member's current tier and the tier their points
   * qualify for, so automatic tier handling never downgrades a member.
   */
  private String resolveTierAfterPoints(String currentTier, int points) {
    String normalizedCurrent =
        currentTier == null ? TIER_SILVER : currentTier;
    String qualifiedTier = determineTier(points);

    if (getTierRank(qualifiedTier) > getTierRank(normalizedCurrent)) {
      return qualifiedTier;
    }
    return normalizedCurrent;
  }

  private int getTierRank(String tier) {
    if (TIER_DIAMOND.equalsIgnoreCase(tier)) {
      return 4;
    }
    if (TIER_PLATINUM.equalsIgnoreCase(tier)) {
      return 3;
    }
    if (TIER_GOLD.equalsIgnoreCase(tier)) {
      return 2;
    }
    return 1;
  }

  /**
   * Simple result object used when reporting tier changes after points are
   * accumulated.
   */
  public static class TierProgressionResult {
    private final String oldTier;
    private final String newTier;
    private final boolean upgraded;

    public TierProgressionResult(String oldTier, String newTier, boolean upgraded) {
      this.oldTier = oldTier;
      this.newTier = newTier;
      this.upgraded = upgraded;
    }

    public String getOldTier() {
      return oldTier;
    }

    public String getNewTier() {
      return newTier;
    }

    public boolean isUpgraded() {
      return upgraded;
    }
  }

  /**
   * Result object returned when a promotion is selected for a member.
   */
  public static class PersonalizedPromotionResult {
    private final Member member;
    private final int membershipMonths;
    private final String promotionTitle;
    private final String promotionDetails;
    private final String eligibilityReason;

    public PersonalizedPromotionResult(Member member, int membershipMonths,
        String promotionTitle, String promotionDetails, String eligibilityReason) {
      this.member = member;
      this.membershipMonths = membershipMonths;
      this.promotionTitle = promotionTitle;
      this.promotionDetails = promotionDetails;
      this.eligibilityReason = eligibilityReason;
    }

    public Member getMember() {
      return member;
    }

    public int getMembershipMonths() {
      return membershipMonths;
    }

    public String getPromotionTitle() {
      return promotionTitle;
    }

    public String getPromotionDetails() {
      return promotionDetails;
    }

    public String getEligibilityReason() {
      return eligibilityReason;
    }
  }

  /**
   * Summary metrics for the membership and tier performance report.
   */
  public static class MembershipReportSummary {
    private final int totalMembers;
    private final int totalPoints;
    private final double averagePoints;
    private final int silverCount;
    private final int goldCount;
    private final int platinumCount;
    private final int diamondCount;
    private final Member highestPointsMember;
    private final int nearNextTierCount;

    public MembershipReportSummary(int totalMembers, int totalPoints,
        double averagePoints, int silverCount, int goldCount, int platinumCount,
        int diamondCount, Member highestPointsMember, int nearNextTierCount) {
      this.totalMembers = totalMembers;
      this.totalPoints = totalPoints;
      this.averagePoints = averagePoints;
      this.silverCount = silverCount;
      this.goldCount = goldCount;
      this.platinumCount = platinumCount;
      this.diamondCount = diamondCount;
      this.highestPointsMember = highestPointsMember;
      this.nearNextTierCount = nearNextTierCount;
    }

    public int getTotalMembers() {
      return totalMembers;
    }

    public int getTotalPoints() {
      return totalPoints;
    }

    public double getAveragePoints() {
      return averagePoints;
    }

    public int getSilverCount() {
      return silverCount;
    }

    public int getGoldCount() {
      return goldCount;
    }

    public int getPlatinumCount() {
      return platinumCount;
    }

    public int getDiamondCount() {
      return diamondCount;
    }

    public Member getHighestPointsMember() {
      return highestPointsMember;
    }

    public int getNearNextTierCount() {
      return nearNextTierCount;
    }
  }

  /**
   * Enriched redemption row used by the redemption analysis report.
   */
  public static class RedemptionReportRow {
    private final Redemption redemption;
    private final String memberName;
    private final String memberTier;
    private final String rewardName;

    public RedemptionReportRow(Redemption redemption, String memberName,
        String memberTier, String rewardName) {
      this.redemption = redemption;
      this.memberName = memberName;
      this.memberTier = memberTier;
      this.rewardName = rewardName;
    }

    public Redemption getRedemption() {
      return redemption;
    }

    public String getMemberName() {
      return memberName;
    }

    public String getMemberTier() {
      return memberTier;
    }

    public String getRewardName() {
      return rewardName;
    }
  }

  /**
   * Summary metrics for the redemption analysis report.
   */
  public static class RedemptionReportSummary {
    private final int totalMatches;
    private final int completedCount;
    private final int pendingCount;
    private final int rejectedCount;
    private final int totalCompletedPoints;
    private final double averageCompletedPoints;
    private final int highestCompletedPoints;

    public RedemptionReportSummary(int totalMatches, int completedCount,
        int pendingCount, int rejectedCount, int totalCompletedPoints,
        double averageCompletedPoints, int highestCompletedPoints) {
      this.totalMatches = totalMatches;
      this.completedCount = completedCount;
      this.pendingCount = pendingCount;
      this.rejectedCount = rejectedCount;
      this.totalCompletedPoints = totalCompletedPoints;
      this.averageCompletedPoints = averageCompletedPoints;
      this.highestCompletedPoints = highestCompletedPoints;
    }

    public int getTotalMatches() {
      return totalMatches;
    }

    public int getCompletedCount() {
      return completedCount;
    }

    public int getPendingCount() {
      return pendingCount;
    }

    public int getRejectedCount() {
      return rejectedCount;
    }

    public int getTotalCompletedPoints() {
      return totalCompletedPoints;
    }

    public double getAverageCompletedPoints() {
      return averageCompletedPoints;
    }

    public int getHighestCompletedPoints() {
      return highestCompletedPoints;
    }
  }
}
