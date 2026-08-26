package boundary;

import adt.ListInterface;
import control.ResortData;
import entity.Guest;
import entity.Member;
import entity.Notification;
import entity.PointTransaction;
import entity.Redemption;
import entity.Reward;
import java.util.Scanner;
import utility.MessageUI;

/**
 * Every screen and prompt for Loyalty & Rewards.
 *
 * @author Ivan Wong
 */
public class LoyaltyRewardsUI {

  private final Scanner scanner = MessageUI.scanner;

  // ==================================================================
  // MENUS
  // ==================================================================

  public int getMenuChoice() {
    MessageUI.displayMenuScreen("LOYALTY", null,
        "Main Menu  >  Loyalty & Rewards",
        new String[] {
          "Members (enrol, search, list)",
          "Points (award, expire, ledger)",
          "Rewards & redemptions",
          "Notifications & promotions",
          "Reports"
        },
        "Back to main menu");
    return MessageUI.readMenuChoice(scanner, 5, "go back to the main menu");
  }

  public int getMemberMenuChoice() {
    MessageUI.displayMenuScreen("MEMBERS", null,
        "Main Menu  >  Loyalty & Rewards  >  Members",
        new String[] {
          "Enrol a new member",
          "Search by member ID",
          "Display all members",
          "Display members sorted by points",
          "Members with points expiring soon",
          "Members close to the next tier"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 6, "go back");
  }

  public int getPointsMenuChoice() {
    MessageUI.displayMenuScreen("POINTS", null,
        "Main Menu  >  Loyalty & Rewards  >  Points",
        new String[] {
          "Award points for a completed stay",
          "Adjust a member's points by hand",
          "Expire points that are past their date",
          "View a member's point ledger"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 4, "go back");
  }

  public int getRewardMenuChoice() {
    MessageUI.displayMenuScreen("REWARDS & REDEMPTIONS", null,
        "Main Menu  >  Loyalty & Rewards  >  Rewards",
        new String[] {
          "View the reward catalogue",
          "Request a redemption",
          "Process the next pending request",
          "View the pending queue",
          "View redemption history"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 5, "go back");
  }

  public int getNotificationMenuChoice() {
    MessageUI.displayMenuScreen("NOTIFICATIONS", null,
        "Main Menu  >  Loyalty & Rewards  >  Notifications",
        new String[] {
          "View a member's notifications",
          "View a member's personalised promotion"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  public int getReportMenuChoice() {
    MessageUI.displayMenuScreen("REPORTS", null,
        "Main Menu  >  Loyalty & Rewards  >  Reports",
        new String[] {
          "Membership & Tier Performance Report",
          "Redemption Analysis Report"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  // ==================================================================
  // INPUT
  // ==================================================================

  public String inputMemberId() {
    String id = MessageUI.readIdNumber(scanner, "Member number", "L", 4);
    return MessageUI.isCancelled(id) ? null : id;
  }

  public String inputGuestId() {
    String id = MessageUI.readIdNumber(scanner, "Guest number", "G", 4);
    return MessageUI.isCancelled(id) ? null : id;
  }

  public String inputBookingId() {
    String id = MessageUI.readIdNumber(scanner, "Booking number", "BK", 4);
    return MessageUI.isCancelled(id) ? null : id;
  }

  public int inputPointsAdjustment() {
    MessageUI.displayMessage("");
    MessageUI.displayMessage("  Enter a positive number to add points, negative to take away.");

    while (true) {
      System.out.print("  Points adjustment (0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if ("0".equals(input)) {
        return MessageUI.CANCELLED_INT;
      }
      if (input.isEmpty()) {
        MessageUI.displayError("This cannot be left blank.");
        continue;
      }

      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException notANumber) {
        MessageUI.displayError("Please enter a whole number, e.g. 500 or -200.");
      }
    }
  }

  public String inputAdjustmentReason() {
    String reason = MessageUI.readRequiredText(scanner, "Reason for the adjustment");
    return MessageUI.isCancelled(reason) ? null : reason;
  }

  /**
   * Asks which reward is wanted, listing what each costs and needs.
   *
   * Rewards the member cannot have are still shown, but marked, so the officer
   * can explain why rather than simply not offering them.
   *
   * @param rewards the catalogue
   * @param member who is asking, or null to list without eligibility
   * @return the chosen reward's ID, or null if cancelled
   */
  public String inputReward(ListInterface<Reward> rewards, Member member) {
    displayRewardCatalogue(rewards, member);

    int picked = MessageUI.readInt(scanner, "Reward number", 1,
        rewards.getNumberOfEntries());
    return (picked == MessageUI.CANCELLED_INT) ? null
        : rewards.getEntry(picked).getRewardId();
  }

  public boolean confirm(String question) {
    return MessageUI.confirm(scanner, question);
  }

  /**
   * Asks whether to run the same lookup again with a different value.
   *
   * Sits under a result that is already on screen, so answering no is what
   * ends the action - these screens no longer finish with a pause, because
   * the question doubles as the way out.
   *
   * @param question what running it again would do
   * @return true to go round again
   */
  public boolean confirmAnother(String question) {
    MessageUI.displayBlankLine();
    return MessageUI.confirm(scanner, question);
  }

  /**
   * Pauses under a caller-chosen wording.
   *
   * @param prompt what to tell the user, without its trailing dots
   */
  public void pause(String prompt) {
    MessageUI.pause(scanner, prompt);
  }

  public void pause() {
    MessageUI.pause(scanner);
  }

  // ==================================================================
  // DISPLAY
  // ==================================================================

  public void startAction(String title) {
    MessageUI.startAction(title);
  }

  public void displayMessage(String message) {
    MessageUI.displayMessage(message);
  }

  public void displayError(String message) {
    MessageUI.displayError(message);
  }

  public void displaySuccess(String message) {
    MessageUI.displaySuccess(message);
  }

  public void displaySectionHeading(String title) {
    MessageUI.displaySectionHeading(title);
  }

  /**
   * Shows one member in full.
   *
   * Both point figures are shown because they mean different things: the
   * balance is what can be spent, and the lifetime total is what earned the
   * tier and never falls.
   *
   * @param member the member
   * @param guest who they are
   */
  public void displayMember(Member member, Guest guest) {
    MessageUI.displayBlankLine();
    MessageUI.displayField("Member ID", member.getMemberId());
    MessageUI.displayField("Guest", (guest == null ? "-" : guest.getFullName())
        + " (" + member.getGuestId() + ")");
    MessageUI.displayField("Tier", member.getTier()
        + String.format("  (earns %.2fx points)", member.getMultiplier()));
    MessageUI.displayField("Points balance", String.valueOf(member.getPointsBalance()));
    MessageUI.displayField("Lifetime points", member.getLifetimePoints()
        + "  (this is what sets the tier)");

    String nextTier = member.getNextTier();
    if (nextTier != null) {
      MessageUI.displayField("Next tier", nextTier + " in "
          + member.getPointsToNextTier() + " more lifetime points");
    } else {
      MessageUI.displayField("Next tier", "already at the top tier");
    }

    MessageUI.displayField("Points expire", String.valueOf(member.getPointsExpiryDate()));
    MessageUI.displayField("Joined", String.valueOf(member.getJoinDate()));
    MessageUI.displayField("Status", member.getStatus());
  }

  /**
   * Lists members as a table, a page at a time.
   *
   * @param members the members to show
   * @param data used to turn a guest ID into a name
   * @param emptyMessage what to say when there is nothing to show
   * @return true if anything was shown
   */
  public boolean displayMemberList(ListInterface<Member> members, ResortData data,
      String emptyMessage) {
    if (members.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  " + emptyMessage);
      return false;
    }

    int totalPages = MessageUI.pageCount(members.getNumberOfEntries());
    int shown = 0;

    for (int page = 1; page <= totalPages; page++) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format("  %-7s %-24s %-9s %9s %11s  %s",
          "MEMBER", "GUEST", "TIER", "BALANCE", "LIFETIME", "EXPIRES"));

      int upTo = Math.min(shown + MessageUI.PAGE_SIZE, members.getNumberOfEntries());
      for (int i = shown + 1; i <= upTo; i++) {
        Member member = members.getEntry(i);
        Guest guest = data.findGuest(member.getGuestId());

        System.out.printf("  %-7s %-24s %-9s %9d %11d  %s%n",
            member.getMemberId(),
            guest == null ? "-" : truncate(guest.getFullName(), 24),
            member.getTier(), member.getPointsBalance(),
            member.getLifetimePoints(), member.getPointsExpiryDate());
      }
      shown = upTo;

      MessageUI.displayThinRule();
      System.out.printf("  %d member(s).%n", members.getNumberOfEntries());

      if (!MessageUI.askForNextPage(scanner, page, totalPages)) {
        break;
      }
    }
    return true;
  }

  /**
   * Shows the reward catalogue.
   *
   * @param rewards the catalogue
   * @param member who is looking, or null for a plain listing
   */
  public void displayRewardCatalogue(ListInterface<Reward> rewards, Member member) {
    MessageUI.displayBlankLine();
    if (member != null) {
      System.out.printf("  %s has %d points to spend (%s tier).%n",
          member.getMemberId(), member.getPointsBalance(), member.getTier());
      MessageUI.displayBlankLine();
    }

    MessageUI.displayTableHeading(String.format("  %-4s %-6s %-28s %8s %-9s %6s  %s",
        "NO", "ID", "REWARD", "POINTS", "MIN TIER", "STOCK", "AVAILABLE?"));

    for (int i = 1; i <= rewards.getNumberOfEntries(); i++) {
      Reward reward = rewards.getEntry(i);

      String available;
      if (!reward.isActive()) {
        available = "No (withdrawn)";
      } else if (reward.getStockQuantity() <= 0) {
        available = "No (out of stock)";
      } else if (member == null) {
        available = "Yes";
      } else if (Member.tierRank(member.getTier())
          < Member.tierRank(reward.getMinimumTier())) {
        available = "No (" + reward.getMinimumTier() + " tier needed)";
      } else if (member.getPointsBalance() < reward.getPointsRequired()) {
        available = "No (" + (reward.getPointsRequired() - member.getPointsBalance())
            + " points short)";
      } else {
        available = "Yes";
      }

      System.out.printf("  %-4d %-6s %-28s %8d %-9s %6d  %s%n",
          i, reward.getRewardId(), truncate(reward.getRewardName(), 28),
          reward.getPointsRequired(), reward.getMinimumTier(),
          reward.getStockQuantity(), available);
    }
    MessageUI.displayThinRule();
  }

  /**
   * Lists redemptions as a table.
   *
   * @param redemptions the redemptions to show
   * @param data used to look up each reward's name
   * @param emptyMessage what to say when there is nothing to show
   * @return true if anything was shown
   */
  public boolean displayRedemptionList(ListInterface<Redemption> redemptions,
      ResortData data, String emptyMessage) {
    if (redemptions.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  " + emptyMessage);
      return false;
    }

    int totalPages = MessageUI.pageCount(redemptions.getNumberOfEntries());
    int shown = 0;

    for (int page = 1; page <= totalPages; page++) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format("  %-8s %-7s %-26s %7s %-10s %s",
          "REDEEM", "MEMBER", "REWARD", "POINTS", "STATUS", "REQUESTED"));

      int upTo = Math.min(shown + MessageUI.PAGE_SIZE, redemptions.getNumberOfEntries());
      for (int i = shown + 1; i <= upTo; i++) {
        Redemption redemption = redemptions.getEntry(i);
        Reward reward = data.findReward(redemption.getRewardId());

        System.out.printf("  %-8s %-7s %-26s %7d %-10s %s%n",
            redemption.getRedemptionId(), redemption.getMemberId(),
            reward == null ? redemption.getRewardId()
                : truncate(reward.getRewardName(), 26),
            redemption.getPointsUsed(), redemption.getStatus(),
            redemption.getRequestDate());
      }
      shown = upTo;

      MessageUI.displayThinRule();
      System.out.printf("  %d redemption(s).%n", redemptions.getNumberOfEntries());

      if (!MessageUI.askForNextPage(scanner, page, totalPages)) {
        break;
      }
    }
    return true;
  }

  /**
   * Shows a member's point ledger.
   *
   * The running balance is shown against every row, so a member querying their
   * total can be walked through how it was arrived at.
   *
   * @param txns the ledger rows, oldest first
   * @param member whose ledger it is
   */
  public void displayLedger(ListInterface<PointTransaction> txns, Member member) {
    MessageUI.displaySectionHeading("Point ledger for " + member.getMemberId());

    if (txns.isEmpty()) {
      MessageUI.displayMessage("  This member has no point history.");
      return;
    }

    MessageUI.displayTableHeading(String.format("  %-7s %-11s %-8s %8s %9s  %s",
        "TXN", "DATE", "TYPE", "POINTS", "BALANCE", "DESCRIPTION"));

    for (int i = 1; i <= txns.getNumberOfEntries(); i++) {
      PointTransaction txn = txns.getEntry(i);

      System.out.printf("  %-7s %-11s %-8s %+8d %9d  %s%n",
          txn.getTxnId(), txn.getTxnDate(), txn.getTxnType(),
          txn.getPoints(), txn.getBalanceAfter(),
          txn.getDescription() == null ? "" : txn.getDescription());
    }
    MessageUI.displayThinRule();
    System.out.printf("  Balance now: %d points.%n", member.getPointsBalance());
  }

  /**
   * Shows a member's notifications, newest first.
   *
   * @param notifications the messages
   * @param memberId whose they are
   */
  public void displayNotifications(ListInterface<Notification> notifications,
      String memberId) {
    MessageUI.displaySectionHeading("Notifications for " + memberId);

    if (notifications.isEmpty()) {
      MessageUI.displayMessage("  This member has no notifications.");
      return;
    }

    for (int i = 1; i <= notifications.getNumberOfEntries(); i++) {
      Notification notification = notifications.getEntry(i);

      System.out.printf("  [%s]  %-16s %s%n",
          notification.isRead() ? " " : "*",
          notification.getType(), notification.getCreatedDate());
      System.out.printf("        %s%n", notification.getMessage());
      MessageUI.displayBlankLine();
    }

    MessageUI.displayThinRule();
    int unread = notifications.countIf(n -> !n.isRead());
    System.out.printf("  %d notification(s), %d unread (marked *).%n",
        notifications.getNumberOfEntries(), unread);
  }

  private String truncate(String text, int width) {
    if (text == null) {
      return "-";
    }
    return (text.length() <= width) ? text : text.substring(0, width - 1) + ".";
  }

  // ==================================================================
  // REPORTS
  // ==================================================================

  public void displayReportHeader(String title) {
    MessageUI.beginLongOutput();
    MessageUI.displayBlankLine();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("TARUMT RESORT MANAGEMENT SYSTEM");
    MessageUI.displayBoxCentred(title);
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  Generated: " + java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")));
    MessageUI.displayBoxBottom();
  }

  public void displayReportLine(String label, String value) {
    MessageUI.displayReportLine(label, value);
  }

  public void displayBarChart(String title, String yAxisLabel, String[] labels,
      double[] values) {
    MessageUI.displayBarChart(title, yAxisLabel, labels, values);
  }

  public void displayTableHeading(String heading) {
    MessageUI.displayTableHeading(heading);
  }

  public void displayThinRule() {
    MessageUI.displayThinRule();
  }

  public void displayReportFooter() {
    MessageUI.displayBlankLine();
    MessageUI.displayRule();
    MessageUI.displayMessage("  END OF REPORT");
    MessageUI.displayRule();
    MessageUI.endLongOutput(scanner);
  }
}
