package boundary;

import adt.ListInterface;
import entity.LoyaltyNotification;
import entity.Member;
import entity.Redemption;
import entity.Reward;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;
import utility.MessageUI;

/**
 *
 * @author Kat Tan
 */
public class LoyaltyRewardsUI {

  // Shared with every other UI class - see MessageUI.scanner for why.
  private Scanner scanner = MessageUI.scanner;

  public int getMenuChoice() {
    MessageUI.clearScreen();
    System.out.println();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("L O Y A L T Y   &   R E W A R D S");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  Main Menu  >  Loyalty & Rewards");
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(1, "Register new member");
    MessageUI.displayMenuOption(2, "Search member by ID");
    MessageUI.displayMenuOption(3, "Display all members");
    MessageUI.displayMenuOption(4, "Display members sorted by points");
    MessageUI.displayMenuOption(5, "Find members with expiring points");
    MessageUI.displayMenuOption(6, "Accumulate member points");
    MessageUI.displayMenuOption(7, "View rewards");
    MessageUI.displayMenuOption(8, "Request reward redemption");
    MessageUI.displayMenuOption(9, "Process next pending redemption");
    MessageUI.displayMenuOption(10, "View redemption history");
    MessageUI.displayMenuOption(11, "View member notifications");
    MessageUI.displayMenuOption(12, "View personalized promotion");
    MessageUI.displayMenuOption(13, "Management reports");
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(0, "Back to main menu");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxBottom();

    return MessageUI.readMenuChoice(scanner, 13, "go back to the main menu");
  }

  /**
   * Draws the framed title every action in this module starts with, matching
   * the layout used across the rest of the system.
   *
   * @param title the name of the action being started
   */
  private void displayActionHeader(String title) {
    MessageUI.clearScreen();
    System.out.println();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxCentred(title);
    MessageUI.displayBoxBottom();
  }

  /**
   * Draws the framed title every action in this module starts with, matching
   * the layout used across the rest of the system.
   *
   * @param title the name of the action being started
   */
  private void displayActionHeader(String title) {
    MessageUI.clearScreen();
    System.out.println();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxCentred(title);
    MessageUI.displayBoxBottom();
  }

  /**
   * Collects the manual registration details for a new loyalty member.
   * Loyalty ID and join date are assigned by the control class.
   *
   * @return a Member with name and contact only, or null if the user cancels
   */
  public Member inputMember() {
    displayActionHeader("REGISTER NEW MEMBER");

    System.out.println("Enter 0 at any prompt to cancel.");
    System.out.println();

    String memberId = inputMemberId("Member ID (0 to cancel): ");
    if (memberId == null) {
      return null;
    }

    String name = inputRequiredText("Name (0 to cancel): ");
    if (name == null) {
      return null;
    }

    // No "0 to cancel" here: 0 is a valid starting points balance.
    Integer points = inputNonNegativeInt("Points: ");
    if (points == null) {
      return null;
    }

    LocalDate pointsExpiryDate = inputOptionalDate(
        "Points expiry date (YYYY-MM-DD, blank if none, 0 to cancel): ");
    if (pointsExpiryDate == null && !skippedOptionalDate) {
      return null;
    }

    String tier = inputTier();
    if (tier == null) {
      return null;
    }

    LocalDate joinDate = inputDate("Join date (YYYY-MM-DD, 0 to cancel): ");
    if (joinDate == null) {
      return null;
    }

    String contactNumber = inputContactNumber();
    if (contactNumber == null) {
      return null;
    }

    Member member = new Member();
    member.setName(name);
    member.setContactNumber(contactNumber);
    return member;
  }

  /**
   * Reads a member ID for lookup. Returns null when the user enters 0 to cancel.
   */
  public String inputSearchMemberId() {
    displayActionHeader("SEARCH MEMBER");
    return inputMemberId("Enter member ID (0 to cancel): ");
  }

  /**
   * Reads the member ID used by the points accumulation workflow.
   */
  public String inputAccumulateMemberId() {
    displayActionHeader("ACCUMULATE MEMBER POINTS");
    return inputMemberId("Enter member ID (0 to cancel): ");
  }

  /**
   * Reads the number of points earned in the current accumulation action.
   * Returns null when the user enters 0 to cancel.
   */
  public Integer inputEarnedPoints() {
    while (true) {
      System.out.print("Points earned (0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if (input.isEmpty()) {
        System.out.println("Points earned cannot be empty.");
        continue;
      }

      if (input.equals("0")) {
        return null;
      }

      try {
        int value = Integer.parseInt(input);
        if (value > 0) {
          return value;
        }
        System.out.println("Earned points must be greater than zero.");
      } catch (NumberFormatException e) {
        System.out.println("Please enter a valid whole number.");
      }
    }
  }

  public void displayPointsAccumulationResult(Member member, int oldPoints,
      int newPoints, String oldTier, String newTier, boolean upgraded) {
    System.out.println("\nPOINTS ACCUMULATION RESULT");
    System.out.println("==========================");
    System.out.printf("Member ID   : %s%n", member.getMemberId());
    System.out.printf("Name        : %s%n", member.getName());
    System.out.printf("Old points  : %d%n", oldPoints);
    System.out.printf("New points  : %d%n", newPoints);
    System.out.printf("Current tier: %s%n", member.getTier());

    if (upgraded) {
      System.out.println();
      System.out.println("Tier upgrade detected!");
      System.out.printf("Previous tier: %s%n", oldTier);
      System.out.printf("New tier     : %s%n", newTier);
    } else {
      System.out.println();
      System.out.println("No tier upgrade. Current tier remains "
          + member.getTier() + ".");
    }
  }

  /**
   * Reads the cutoff date used by the expiring-points listing.
   */
  public LocalDate inputCutoffDate() {
    displayActionHeader("FIND MEMBERS WITH EXPIRING POINTS");
    System.out.println("Members with points > 0 and an expiry date on or before");
    System.out.println("the cutoff will be listed.");
    return inputDate("Cutoff date (YYYY-MM-DD, 0 to cancel): ");
  }

  public void displayMember(Member member) {
    if (member == null) {
      displayMessage("Member not found.");
      return;
    }

    System.out.println("\nMEMBER PROFILE");
    System.out.println("==============");
    System.out.printf("Member ID          : %s%n", member.getMemberId());
    System.out.printf("Name               : %s%n", member.getName());
    System.out.printf("Points             : %d%n", member.getPoints());
    System.out.printf("Points expiry date : %s%n",
        formatDate(member.getPointsExpiryDate()));
    System.out.printf("Tier               : %s%n", member.getTier());
    System.out.printf("Join date          : %s%n", formatDate(member.getJoinDate()));
    System.out.printf("Contact number     : %s%n", member.getContactNumber());
  }

  public void displayMembers(ListInterface<Member> members, String title) {
    System.out.println();
    System.out.println(title);
    System.out.println("-".repeat(title.length()));

    if (members == null || members.isEmpty()) {
      displayMessage("No members to display.");
      return;
    }

    System.out.printf("%-4s %-10s %-25s %8s %-12s %-10s %-12s %-15s%n",
        "No.", "Member ID", "Name", "Points", "Expiry", "Tier", "Join Date", "Contact");
    System.out.println("-".repeat(110));

    Iterator<Member> iterator = members.getIterator();
    int row = 1;
    while (iterator.hasNext()) {
      Member member = iterator.next();
      System.out.printf("%-4d %-10s %-25s %8d %-12s %-10s %-12s %-15s%n",
          row,
          member.getMemberId(),
          member.getName(),
          member.getPoints(),
          formatDate(member.getPointsExpiryDate()),
          member.getTier(),
          formatDate(member.getJoinDate()),
          member.getContactNumber());
      row++;
    }

    System.out.println("-".repeat(110));
    System.out.println("Total: " + members.getNumberOfEntries() + " member(s)");
  }

  public void displayMessage(String message) {
    System.out.println("\n" + message);
  }

  /**
   * Waits for the user to press Enter so output stays on screen instead of
   * being wiped by the clearScreen() at the top of the next menu. Matches the
   * pause used by the other modules.
   */
  public void pressEnterToContinue() {
    System.out.print("\nPress Enter to continue...");
    // hasNextLine() guards against input being exhausted (e.g. piped input or
    // Ctrl+D), which would otherwise throw NoSuchElementException here.
    if (scanner.hasNextLine()) {
      scanner.nextLine();
    }
  }

  public void displayRegistrationSuccess(Member member) {
    System.out.println();
    System.out.println("========================================");
    System.out.println("       MEMBER REGISTRATION SUCCESS");
    System.out.println("========================================");
    System.out.printf("Loyalty ID   : %s%n", member.getMemberId());
    System.out.printf("Name         : %s%n", member.getName());
    System.out.printf("Contact No.  : %s%n", member.getContactNumber());
    System.out.printf("Join Date    : %s%n", formatDate(member.getJoinDate()));
    System.out.printf("Points       : %d%n", member.getPoints());
    System.out.printf("Tier         : %s%n", member.getTier());
    System.out.println("========================================");
  }

  public void displayDuplicateMemberMessage(String memberId) {
    displayMessage("Registration failed: member ID \"" + memberId
        + "\" is already registered.");
  }

  public void displayMemberExistsMessage(Member member) {
    displayMessage("Member found.");
    displayMember(member);
  }

  public void displayMemberNotFoundMessage(String memberId) {
    displayMessage("No member found with ID \"" + memberId + "\".");
  }

  public String inputPromotionMemberId() {
    displayActionHeader("VIEW PERSONALIZED PROMOTION");
    return inputMemberId("Enter member ID (0 to cancel): ");
  }

  public void displayPersonalizedPromotion(Member member, int membershipMonths,
      String promotionTitle, String promotionDetails, String eligibilityReason) {
    System.out.println("\nPERSONALIZED PROMOTION");
    System.out.println("======================");
    System.out.printf("Member ID           : %s%n", member.getMemberId());
    System.out.printf("Name                : %s%n", member.getName());
    System.out.printf("Tier                : %s%n", member.getTier());
    System.out.printf("Points              : %d%n", member.getPoints());
    System.out.printf("Membership duration : %d month(s)%n", membershipMonths);
    System.out.println();
    System.out.printf("Promotion title     : %s%n", promotionTitle);
    System.out.printf("Promotion details   : %s%n", promotionDetails);
    System.out.println();
    System.out.printf("Eligibility         : %s%n", eligibilityReason);
  }

  public String inputRedemptionMemberId() {
    displayActionHeader("REQUEST REWARD REDEMPTION");
    return inputMemberId("Enter member ID (0 to cancel): ");
  }

  public String inputRewardId() {
    while (true) {
      System.out.print("Enter reward ID (0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if (input.isEmpty()) {
        System.out.println("Reward ID cannot be empty.");
        continue;
      }

      if (input.equals("0")) {
        return null;
      }

      return input;
    }
  }

  public void displayMemberSummaryForRedemption(Member member) {
    System.out.println();
    System.out.printf("Member : %s (%s)%n", member.getMemberId(), member.getName());
    System.out.printf("Points : %d%n", member.getPoints());
  }

  public void displayRewards(ListInterface<Reward> rewards) {
    System.out.println("\nAVAILABLE REWARDS");
    System.out.println("=================");
    if (rewards == null || rewards.isEmpty()) {
      displayMessage("No rewards available.");
      return;
    }

    System.out.printf("%-8s %-30s %12s%n", "Reward ID", "Reward Name", "Points Req.");
    System.out.println("-".repeat(54));

    Iterator<Reward> iterator = rewards.getIterator();
    while (iterator.hasNext()) {
      Reward reward = iterator.next();
      System.out.printf("%-8s %-30s %12d%n",
          reward.getRewardId(),
          reward.getRewardName(),
          reward.getPointsRequired());
    }
    System.out.println("-".repeat(54));
  }

  public void displayInvalidRewardMessage(String rewardId) {
    displayMessage("Reward \"" + rewardId + "\" was not found.");
  }

  public void displayInsufficientPointsMessage(int currentPoints, int requiredPoints) {
    displayMessage("Insufficient points. Member has " + currentPoints
        + " points but this reward requires " + requiredPoints + " points.");
  }

  public void displayRedemptionRequestResult(Redemption redemption, Reward reward,
      Member member) {
    System.out.println("\nREDEMPTION REQUEST CREATED");
    System.out.println("==========================");
    System.out.printf("Redemption ID : %s%n", redemption.getRedemptionId());
    System.out.printf("Member ID     : %s%n", redemption.getMemberId());
    System.out.printf("Reward        : %s (%s)%n", reward.getRewardName(), reward.getRewardId());
    System.out.printf("Points needed : %d%n", redemption.getPointsUsed());
    System.out.printf("Request date  : %s%n", redemption.getRequestDate());
    System.out.printf("Status        : %s%n", redemption.getStatus());
    System.out.println();
    System.out.println("Note: points are not deducted yet. They will be deducted");
    System.out.println("only when this request is successfully processed.");
    System.out.printf("Member points remain: %d%n", member.getPoints());
  }

  public void displayEmptyPendingQueueMessage() {
    displayMessage("No pending redemption requests to process.");
  }

  public void displayProcessRedemptionResult(Redemption redemption, Reward reward,
      Member member, int oldPoints, int newPoints, String message) {
    System.out.println("\nPROCESS PENDING REDEMPTION");
    System.out.println("==========================");
    System.out.printf("Redemption ID : %s%n", redemption.getRedemptionId());
    System.out.printf("Member ID     : %s%n", redemption.getMemberId());
    if (reward != null) {
      System.out.printf("Reward        : %s (%s)%n",
          reward.getRewardName(), reward.getRewardId());
    } else {
      System.out.printf("Reward ID     : %s%n", redemption.getRewardId());
    }
    System.out.printf("Points used   : %d%n", redemption.getPointsUsed());
    System.out.printf("Status        : %s%n", redemption.getStatus());
    System.out.println();
    System.out.println(message);

    if (member != null && Redemption.STATUS_COMPLETED.equals(redemption.getStatus())) {
      System.out.printf("Member points : %d -> %d%n", oldPoints, newPoints);
    } else if (member != null) {
      System.out.printf("Member points : %d (unchanged)%n", member.getPoints());
    }
  }

  public void displayRedemptions(ListInterface<Redemption> redemptions, String title) {
    System.out.println();
    System.out.println(title);
    System.out.println("-".repeat(title.length()));

    if (redemptions == null || redemptions.isEmpty()) {
      displayMessage("No redemption records to display.");
      return;
    }

    System.out.printf("%-4s %-10s %-10s %-8s %8s %-12s %-10s%n",
        "No.", "Redeem ID", "Member ID", "Reward", "Points", "Request Date", "Status");
    System.out.println("-".repeat(78));

    Iterator<Redemption> iterator = redemptions.getIterator();
    int row = 1;
    while (iterator.hasNext()) {
      Redemption redemption = iterator.next();
      System.out.printf("%-4d %-10s %-10s %-8s %8d %-12s %-10s%n",
          row,
          redemption.getRedemptionId(),
          redemption.getMemberId(),
          redemption.getRewardId(),
          redemption.getPointsUsed(),
          formatDate(redemption.getRequestDate()),
          redemption.getStatus());
      row++;
    }

    System.out.println("-".repeat(78));
    System.out.println("Total: " + redemptions.getNumberOfEntries() + " record(s)");
  }

  public String inputNotificationMemberId() {
    displayActionHeader("VIEW MEMBER NOTIFICATIONS");
    return inputMemberId("Enter member ID (0 to cancel): ");
  }

  public int getNotificationFilterChoice() {
    System.out.println("\nNotification filter:");
    System.out.println("1. All notifications");
    System.out.println("2. Unread only");
    System.out.println("3. By notification type");
    System.out.println("0. Cancel");
    return MessageUI.readMenuChoice(scanner, 3, "cancel");
  }

  public String inputNotificationType() {
    while (true) {
      System.out.println("\nNotification type:");
      System.out.println("1. EXPIRING_POINTS");
      System.out.println("2. REDEMPTION");
      System.out.println("3. TIER_UPGRADE");
      System.out.println("0. Cancel");

      int choice = MessageUI.readMenuChoice(scanner, 3, "cancel");
      switch (choice) {
        case 0:
          return null;
        case 1:
          return "EXPIRING_POINTS";
        case 2:
          return "REDEMPTION";
        case 3:
          return "TIER_UPGRADE";
        default:
          return null;
      }
    }
  }

  public void displayNotifications(ListInterface<LoyaltyNotification> notifications,
      String memberId, int filterChoice, String typeFilter) {
    System.out.println("\nMEMBER NOTIFICATIONS");
    System.out.println("====================");
    System.out.printf("Member ID : %s%n", memberId);
    System.out.printf("Filter    : %s%n", describeNotificationFilter(filterChoice, typeFilter));
    System.out.println();

    if (notifications == null || notifications.isEmpty()) {
      displayMessage("No notifications found for this member and filter.");
      return;
    }

    System.out.printf("%-4s %-10s %-18s %-12s %-6s %s%n",
        "No.", "Notify ID", "Type", "Created", "Read", "Message");
    System.out.println("-".repeat(110));

    Iterator<LoyaltyNotification> iterator = notifications.getIterator();
    int row = 1;
    while (iterator.hasNext()) {
      LoyaltyNotification notification = iterator.next();
      System.out.printf("%-4d %-10s %-18s %-12s %-6s %s%n",
          row,
          notification.getNotificationId(),
          notification.getType(),
          formatDate(notification.getCreatedDate()),
          notification.isRead() ? "YES" : "NO",
          notification.getMessage());
      row++;
    }

    System.out.println("-".repeat(110));
    System.out.println("Total: " + notifications.getNumberOfEntries() + " notification(s)");
  }

  private String describeNotificationFilter(int filterChoice, String typeFilter) {
    if (filterChoice == 2) {
      return "Unread only";
    }
    if (filterChoice == 3) {
      return "Type = " + typeFilter;
    }
    return "All";
  }

  private boolean skippedOptionalDate;

  private String inputMemberId(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (input.isEmpty()) {
        System.out.println("Member ID cannot be empty.");
        continue;
      }

      if (input.equals("0")) {
        return null;
      }

      return input;
    }
  }

  private String inputRequiredText(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (input.isEmpty()) {
        System.out.println("This field cannot be empty. Enter 0 to cancel.");
        continue;
      }

      if (input.equals("0")) {
        return null;
      }

      return input;
    }
  }

  private Integer inputNonNegativeInt(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (input.isEmpty()) {
        System.out.println("This field cannot be empty.");
        continue;
      }

      try {
        int value = Integer.parseInt(input);
        if (value >= 0) {
          return value;
        }
        System.out.println("Points cannot be negative.");
      } catch (NumberFormatException e) {
        System.out.println("Please enter a whole number.");
      }
    }
  }

  private String inputTier() {
    while (true) {
      System.out.print(
          "Tier (Silver/Gold/Platinum/Diamond, blank for Silver, 0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if (input.equals("0")) {
        return null;
      }

      if (input.isEmpty()) {
        return "Silver";
      }

      if (input.equalsIgnoreCase("Silver")
          || input.equalsIgnoreCase("Gold")
          || input.equalsIgnoreCase("Platinum")
          || input.equalsIgnoreCase("Diamond")) {
        return capitalizeTier(input);
      }

      System.out.println("Tier must be Silver, Gold, Platinum, or Diamond.");
    }
  }

  private String capitalizeTier(String tier) {
    if (tier.equalsIgnoreCase("Diamond")) {
      return "Diamond";
    }
    if (tier.equalsIgnoreCase("Gold")) {
      return "Gold";
    }
    if (tier.equalsIgnoreCase("Platinum")) {
      return "Platinum";
    }
    return "Silver";
  }

  private LocalDate inputDate(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (input.equals("0")) {
        return null;
      }

      try {
        return LocalDate.parse(input);
      } catch (DateTimeParseException e) {
        System.out.println("Please enter a valid date in YYYY-MM-DD format.");
      }
    }
  }

  private LocalDate inputOptionalDate(String prompt) {
    skippedOptionalDate = false;

    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (input.equals("0")) {
        return null;
      }

      if (input.isEmpty()) {
        skippedOptionalDate = true;
        return null;
      }

      try {
        return LocalDate.parse(input);
      } catch (DateTimeParseException e) {
        System.out.println("Please enter a valid date in YYYY-MM-DD format.");
      }
    }
  }

  private String inputContactNumber() {
    while (true) {
      System.out.print(
          "Contact number (9-10 digits, starts with 0; enter 0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if (input.equals("0")) {
        return null;
      }

      if (input.isEmpty()) {
        System.out.println("Contact number cannot be empty.");
        continue;
      }

      String digitsOnly = input.replaceAll("[^0-9]", "");
      if (digitsOnly.length() < 9 || digitsOnly.length() > 10) {
        System.out.println("Contact number must contain 9 to 10 digits.");
        continue;
      }

      if (!digitsOnly.startsWith("0")) {
        System.out.println("Contact number must start with 0.");
        continue;
      }

      return input;
    }
  }

  private String formatDate(LocalDate date) {
    return date == null ? "-" : date.toString();
  }

  private boolean reportInputCancelled;

  public boolean wasReportInputCancelled() {
    return reportInputCancelled;
  }

  public int getReportMenuChoice() {
    MessageUI.clearScreen();
    System.out.println();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("M A N A G E M E N T   R E P O R T S");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  Main Menu  >  Loyalty & Rewards  >  Management Reports");
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(1, "Loyalty Membership & Tier Performance Report");
    MessageUI.displayMenuOption(2, "Redemption Analysis Report");
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(0, "Back to Loyalty & Rewards menu");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxBottom();

    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  public void displayReportHeader(String reportTitle) {
    System.out.println();
    System.out.println(reportTitle);
    System.out.println("=".repeat(reportTitle.length()));
    System.out.println("Generated on: " + LocalDate.now());
    System.out.println();
  }

  public void displayReportFooter() {
    System.out.println();
    System.out.println("-".repeat(78));
    System.out.println("End of report");
    System.out.println();
  }

  public void displayReportSection(String title) {
    System.out.println(title);
    System.out.println("-".repeat(title.length()));
  }

  public void displayReportLine(String label, String value) {
    System.out.printf("  %-34s: %s%n", label, value);
  }

  public String inputMembershipReportTierFilter() {
    System.out.println("\nTier filter:");
    System.out.println("0. All tiers");
    System.out.println("1. Silver");
    System.out.println("2. Gold");
    System.out.println("3. Platinum");
    System.out.println("4. Diamond");
    System.out.println("9. Cancel report");

    while (true) {
      System.out.print("Enter choice: ");
      // readLine() returns "9" at end of input so a closed console cancels the
      // report instead of throwing NoSuchElementException.
      String input = MessageUI.readLine(scanner, "9");
      if (input.equals("9")) {
        return null;
      }
      try {
        int choice = Integer.parseInt(input);
        switch (choice) {
          case 0:
            return "ALL";
          case 1:
            return "Silver";
          case 2:
            return "Gold";
          case 3:
            return "Platinum";
          case 4:
            return "Diamond";
          default:
            System.out.println("Please enter a number from 0 to 4, or 9 to cancel.");
        }
      } catch (NumberFormatException e) {
        System.out.println("Please enter a valid number.");
      }
    }
  }

  public String inputRedemptionReportStatusFilter() {
    System.out.println("\nStatus filter:");
    System.out.println("0. All statuses");
    System.out.println("1. PENDING");
    System.out.println("2. COMPLETED");
    System.out.println("3. REJECTED");
    System.out.println("9. Cancel report");

    while (true) {
      System.out.print("Enter choice: ");
      // As above: end of input cancels the report rather than crashing.
      String input = MessageUI.readLine(scanner, "9");
      if (input.equals("9")) {
        return null;
      }
      try {
        int choice = Integer.parseInt(input);
        switch (choice) {
          case 0:
            return "ALL";
          case 1:
            return Redemption.STATUS_PENDING;
          case 2:
            return Redemption.STATUS_COMPLETED;
          case 3:
            return Redemption.STATUS_REJECTED;
          default:
            System.out.println("Please enter a number from 0 to 3, or 9 to cancel.");
        }
      } catch (NumberFormatException e) {
        System.out.println("Please enter a valid number.");
      }
    }
  }

  private boolean isReportCancelInput(String input) {
    return input.equalsIgnoreCase("C");
  }

  public Integer inputOptionalReportPoints(String prompt) {
    reportInputCancelled = false;

    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (isReportCancelInput(input)) {
        reportInputCancelled = true;
        return null;
      }

      if (input.isEmpty()) {
        return null;
      }

      try {
        int value = Integer.parseInt(input);
        if (value >= 0) {
          return value;
        }
        System.out.println("Points cannot be negative.");
      } catch (NumberFormatException e) {
        System.out.println("Please enter a valid whole number.");
      }
    }
  }

  public LocalDate inputOptionalReportDate(String prompt) {
    reportInputCancelled = false;

    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (isReportCancelInput(input)) {
        reportInputCancelled = true;
        return null;
      }

      if (input.isEmpty()) {
        return null;
      }

      try {
        return LocalDate.parse(input);
      } catch (DateTimeParseException e) {
        System.out.println("Please enter a valid date in YYYY-MM-DD format.");
      }
    }
  }

  public void displayMembershipTierPerformanceReport(String filterDescription,
      ListInterface<Member> members, int totalMembers, int totalPoints,
      double averagePoints, int silverCount, int goldCount, int platinumCount,
      int diamondCount, int nearNextTierCount, Member highestPointsMember) {
    displayReportSection("FILTER CRITERIA");
    System.out.println("  " + filterDescription);
    System.out.println();

    displayReportSection("MEMBER LISTING");
    if (members == null || members.isEmpty()) {
      displayMessage("No members matched the selected criteria.");
    } else {
      System.out.printf("%-10s %-22s %-10s %8s %-12s %-12s%n",
          "Member ID", "Name", "Tier", "Points", "Join Date", "Expiry");
      System.out.println("-".repeat(86));

      Iterator<Member> iterator = members.getIterator();
      while (iterator.hasNext()) {
        Member member = iterator.next();
        System.out.printf("%-10s %-22s %-10s %8d %-12s %-12s%n",
            member.getMemberId(),
            member.getName(),
            member.getTier(),
            member.getPoints(),
            formatDate(member.getJoinDate()),
            formatDate(member.getPointsExpiryDate()));
      }
      System.out.println("-".repeat(86));
    }

    System.out.println();
    displayReportSection("SUMMARY METRICS");
    displayReportLine("Total matching members", String.valueOf(totalMembers));
    displayReportLine("Total points", String.valueOf(totalPoints));
    displayReportLine("Average points", String.format("%.2f", averagePoints));
    displayReportLine("Silver members", String.valueOf(silverCount));
    displayReportLine("Gold members", String.valueOf(goldCount));
    displayReportLine("Platinum members", String.valueOf(platinumCount));
    displayReportLine("Diamond members", String.valueOf(diamondCount));
    displayReportLine("Members near next tier", String.valueOf(nearNextTierCount));

    if (highestPointsMember == null) {
      displayReportLine("Highest-points member", "-");
    } else {
      displayReportLine("Highest-points member",
          highestPointsMember.getMemberId() + " ("
              + highestPointsMember.getPoints() + " pts)");
    }
  }

  public void displayRedemptionAnalysisReport(String filterDescription,
      ListInterface<Redemption> redemptions, String[] memberNames,
      String[] memberTiers, String[] rewardNames, int totalMatches,
      int completedCount, int pendingCount, int rejectedCount,
      int totalCompletedPoints, double averageCompletedPoints,
      int highestCompletedPoints) {
    displayReportSection("FILTER CRITERIA");
    System.out.println("  " + filterDescription);
    System.out.println();

    displayReportSection("REDEMPTION LISTING");
    if (redemptions == null || redemptions.isEmpty()) {
      displayMessage("No redemptions matched the selected criteria.");
    } else {
      System.out.printf("%-10s %-10s %-18s %-10s %-22s %8s %-12s %-10s%n",
          "Redeem ID", "Member ID", "Member Name", "Tier", "Reward",
          "Points", "Date", "Status");
      System.out.println("-".repeat(112));

      for (int position = 1; position <= redemptions.getNumberOfEntries(); position++) {
        Redemption redemption = redemptions.getEntry(position);
        int index = position - 1;
        System.out.printf("%-10s %-10s %-18s %-10s %-22s %8d %-12s %-10s%n",
            redemption.getRedemptionId(),
            redemption.getMemberId(),
            memberNames[index],
            memberTiers[index],
            rewardNames[index],
            redemption.getPointsUsed(),
            formatDate(redemption.getRequestDate()),
            redemption.getStatus());
      }
      System.out.println("-".repeat(112));
    }

    System.out.println();
    displayReportSection("SUMMARY METRICS");
    displayReportLine("Total matching redemptions", String.valueOf(totalMatches));
    displayReportLine("Completed", String.valueOf(completedCount));
    displayReportLine("Pending", String.valueOf(pendingCount));
    displayReportLine("Rejected", String.valueOf(rejectedCount));
    displayReportLine("Total points successfully redeemed",
        String.valueOf(totalCompletedPoints));
    displayReportLine("Average completed redemption",
        String.format("%.2f", averageCompletedPoints));
    displayReportLine("Highest completed redemption",
        completedCount == 0
            ? "-"
            : String.valueOf(highestCompletedPoints) + " pts");
  }
}
