package boundary;

import adt.ListInterface;
import control.ResortData;
import control.ServiceResult;
import entity.Booking;
import entity.Guest;
import entity.Member;
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
          "Members (enrol, edit, tiers)",
          "Points (expire, ledger)",
          "Rewards & redemptions",
          "Reports"
        },
        "Back to main menu");
    return MessageUI.readMenuChoice(scanner, 4, "go back to the main menu");
  }

  public int getMemberMenuChoice() {
    MessageUI.displayMenuScreen("MEMBERS", null,
        "Main Menu  >  Loyalty & Rewards  >  Members",
        new String[] {
          "Enrol a new member",
          "Edit a member's details",
          "Remove a membership",
          "Display all members",
          "Display members sorted by points",
          "Members close to the next tier",
          "Tier thresholds (points needed for each tier)"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 7, "go back");
  }

  public int getPointsMenuChoice() {
    MessageUI.displayMenuScreen("POINTS", null,
        "Main Menu  >  Loyalty & Rewards  >  Points",
        new String[] {
          "Expire points that are past their date",
          "View a member's point ledger"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  public int getRewardMenuChoice() {
    MessageUI.displayMenuScreen("REWARDS & REDEMPTIONS", null,
        "Main Menu  >  Loyalty & Rewards  >  Rewards",
        new String[] {
          "View the reward catalogue",
          "Add a new reward",
          "Edit a reward",
          "Delete a reward",
          "Restock a reward",
          "Request a redemption",
          "Process the pending queue",
          "View redemption history"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 8, "go back");
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

  /**
   * Asks for a row number from a listing already on screen.
   *
   * @param maximum how many rows there are
   * @param prompt what the number is being asked for
   * @return the 1-based position, or -1 if the officer went back
   */
  public int inputListPosition(int maximum, String prompt) {
    int picked = MessageUI.readInt(scanner, prompt, 1, maximum);
    return (picked == MessageUI.CANCELLED_INT) ? -1 : picked;
  }

  /**
   * Asks for a reward ID such as RW001. C cancels; 0 is not a cancel key here
   * because a typed zero would otherwise be indistinguishable from backing out.
   *
   * @return the normalised ID, or null if cancelled
   */
  public String inputRewardId() {
    while (true) {
      System.out.print("  Reward ID (e.g. RW001, C to cancel): ");
      String input = MessageUI.readLine(scanner, "c");

      if (input.equalsIgnoreCase("c")) {
        return null;
      }
      if (input.isEmpty()) {
        MessageUI.displayError("This cannot be left blank.");
        continue;
      }

      String value = input.toUpperCase();
      if (value.startsWith("RW")) {
        value = value.substring(2);
      }

      if (!MessageUI.isAllDigits(value)) {
        MessageUI.displayError("Please enter a reward ID, e.g. RW001.");
        continue;
      }

      try {
        int number = Integer.parseInt(value);
        if (number <= 0) {
          MessageUI.displayError("The number must be 1 or more.");
          continue;
        }
        return "RW" + String.format("%03d", number);
      } catch (NumberFormatException tooLong) {
        MessageUI.displayError("That number is too large.");
      }
    }
  }

  /**
   * Asks how many units to add back to a reward's stock.
   *
   * @return the amount to add, or null if cancelled
   */
  public Integer inputRestockQuantity() {
    return readIntOrCancel("Quantity to add", 1, 9999);
  }

  /**
   * Collects the details for a new reward. Every prompt uses C to cancel, so
   * a typed 0 can still be a valid stock count or cash value.
   *
   * The reward ID is assigned by the control layer.
   *
   * @return a new Reward without an ID, or null if cancelled
   */
  public Reward promptAddNewReward() {
    MessageUI.displayMessage("  A new reward ID will be assigned automatically.");
    MessageUI.displayBlankLine();

    String name = readTextOrCancel("Reward name");
    if (name == null) {
      return null;
    }

    String category = readChoiceOrCancel("Category", new String[] {
        Reward.CAT_ACTIVITY,
        Reward.CAT_DINING,
        Reward.CAT_SPA,
        Reward.CAT_TRANSPORT,
        Reward.CAT_WELLNESS
    });
    if (category == null) {
      return null;
    }

    Integer pointsRequired = readIntOrCancel("Points required", 1, 99999);
    if (pointsRequired == null) {
      return null;
    }

    String minimumTier = readChoiceOrCancel("Minimum tier", new String[] {
        Member.SILVER,
        Member.GOLD,
        Member.PLATINUM,
        Member.DIAMOND
    });
    if (minimumTier == null) {
      return null;
    }

    Integer stockQuantity = readIntOrCancel("Initial stock quantity", 0, 9999);
    if (stockQuantity == null) {
      return null;
    }

    Double cashValue = inputCashValue();
    if (cashValue == null) {
      return null;
    }

    return new Reward(null, name, category, pointsRequired, minimumTier,
        stockQuantity, true, cashValue);
  }

  /**
   * Reads a non-negative cash value where 0 is valid and C cancels.
   *
   * @return the amount in RM, or null if cancelled
   */
  public Double inputCashValue() {
    while (true) {
      System.out.print("  Cash value off a bill (RM, C to cancel): ");
      String input = MessageUI.readLine(scanner, "c");

      if (input.equalsIgnoreCase("c")) {
        return null;
      }
      if (input.isEmpty()) {
        MessageUI.displayError("This cannot be left blank.");
        continue;
      }

      try {
        double value = Double.parseDouble(input);
        if (value < 0) {
          MessageUI.displayError("An amount cannot be negative.");
          continue;
        }
        return Math.round(value * 100.0) / 100.0;
      } catch (NumberFormatException notANumber) {
        MessageUI.displayError("Please enter an amount, e.g. 150.00");
      }
    }
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

  /**
   * Reads required text where C cancels. 0 is an ordinary value, not a way out.
   *
   * @return the text, or null if cancelled
   */
  private String readTextOrCancel(String prompt) {
    while (true) {
      System.out.print("  " + prompt + " (C to cancel): ");
      String input = MessageUI.readLine(scanner, "c");

      if (input.equalsIgnoreCase("c")) {
        return null;
      }
      if (input.isEmpty()) {
        MessageUI.displayError("This cannot be left blank.");
        continue;
      }
      return input;
    }
  }

  /**
   * Reads a whole number in range where C cancels. 0 is valid when min is 0.
   *
   * @return the number, or null if cancelled
   */
  private Integer readIntOrCancel(String prompt, int min, int max) {
    while (true) {
      System.out.printf("  %s (%d-%d, C to cancel): ", prompt, min, max);
      String input = MessageUI.readLine(scanner, "c");

      if (input.equalsIgnoreCase("c")) {
        return null;
      }
      if (input.isEmpty()) {
        MessageUI.displayError("This cannot be left blank.");
        continue;
      }

      try {
        int value = Integer.parseInt(input);
        if (value < min || value > max) {
          MessageUI.displayError("Please enter a number from " + min + " to " + max + ".");
          continue;
        }
        return value;
      } catch (NumberFormatException notANumber) {
        MessageUI.displayError("Please enter a whole number.");
      }
    }
  }

  /**
   * Asks the user to pick one of a set of values, with C to cancel.
   *
   * @return the chosen value, or null if cancelled
   */
  private String readChoiceOrCancel(String prompt, String[] choices) {
    MessageUI.displayBlankLine();
    for (int i = 0; i < choices.length; i++) {
      System.out.printf("    [%d]  %s%n", i + 1, choices[i]);
    }
    MessageUI.displayBlankLine();

    Integer picked = readIntOrCancel(prompt, 1, choices.length);
    return (picked == null) ? null : choices[picked - 1];
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
  /**
   * Lists the members and takes the one picked by its row number.
   *
   * The officer should not have to know a member ID before they can act on a
   * member, so the table comes first and the number in its first column is
   * what they type. Long lists are paged, and the numbers count from the top
   * of the whole listing rather than the page, so a member keeps the same
   * number whichever page they are read from.
   *
   * @param members the members to offer
   * @param data used to reach each member's guest record
   * @param prompt what the number is being asked for
   * @param emptyMessage what to say when there is nothing to show
   * @return the chosen member, or null if the officer quit
   */
  /**
   * Walks the editable fields of a guest, keeping whatever is left blank.
   *
   * The IC or passport is not offered: it is what the front desk recognises a
   * returning guest by, so changing it here would quietly detach them from
   * their own booking history.
   *
   * @param guest the guest behind the membership
   * @return true if anything was actually changed
   */
  public boolean editGuestFields(Guest guest) {
    MessageUI.displaySectionHeading("Edit " + guest.getGuestId());
    boolean changed = false;

    String name = MessageUI.readOptionalText(scanner,
        "Full name [" + guest.getFullName() + "]");
    if (name != null && !name.isBlank()) {
      guest.setFullName(name.trim());
      changed = true;
    }

    String contact = MessageUI.readOptionalText(scanner,
        "Contact number [" + guest.getContactNumber() + "]");
    if (contact != null && !contact.isBlank()) {
      guest.setContactNumber(contact.trim());
      changed = true;
    }

    String email = MessageUI.readOptionalText(scanner,
        "Email [" + (guest.getEmail() == null || guest.getEmail().isBlank()
            ? "none" : guest.getEmail()) + "]");
    if (email != null && !email.isBlank()) {
      guest.setEmail(email.trim());
      changed = true;
    }

    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  IC / Passport stays " + guest.getIcPassportNo()
        + " - it is how a returning");
    MessageUI.displayMessage("  guest is recognised at the front desk.");

    return changed;
  }

  /** Wipes the screen, so nothing of the last listing is left behind. */
  public void clearScreen() {
    MessageUI.clearScreen();
  }

  /**
   * Lists every redemption and takes the one to look at in detail.
   *
   * @param history the redemptions, newest first
   * @param data used to name the member and reward on each row
   * @return the chosen redemption, or null to go back
   */
  public Redemption chooseRedemptionFromHistory(ListInterface<Redemption> history,
      ResortData data) {
    if (history.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  No redemption has ever been requested.");
      return null;
    }

    int total = history.getNumberOfEntries();
    int totalPages = MessageUI.pageCount(total);
    int page = 1;

    while (true) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format(
          "  %-5s %-8s %-8s %-24s %8s %-10s  %s",
          "NO", "REDEEM", "MEMBER", "REWARD", "POINTS", "STATUS", "REQUESTED"));

      int from = MessageUI.firstRowOnPage(page);
      int upTo = MessageUI.lastRowOnPage(page, total);

      for (int i = from; i <= upTo; i++) {
        Redemption redemption = history.getEntry(i);
        Reward reward = data.findReward(redemption.getRewardId());

        System.out.printf("  [%d]   %-8s %-8s %-24s %8d %-10s  %s%n",
            i, redemption.getRedemptionId(), redemption.getMemberId(),
            truncate(reward == null ? redemption.getRewardId()
                : reward.getRewardName(), 24),
            redemption.getPointsUsed(), redemption.getStatus(),
            redemption.getRequestDate());
      }

      MessageUI.displayThinRule();
      System.out.printf("  %d redemption(s).%n", total);

      if (totalPages == 1) {
        int picked = MessageUI.readInt(scanner,
            "Number of the redemption to view", 1, total);
        return (picked == MessageUI.CANCELLED_INT) ? null : history.getEntry(picked);
      }

      System.out.printf("  Showing %d-%d of %d.%n", from, upTo, total);

      Integer chosen = readPagedSelection(page, totalPages, total, "redemption");
      if (chosen == null) {
        return null;
      }
      if (chosen > 0) {
        return history.getEntry(chosen);
      }
      page = -chosen;
    }
  }

  /**
   * Everything known about one redemption, on its own screen.
   *
   * @param redemption the redemption to show
   * @param data used to reach the member, reward, guest and booking
   */
  public void displayRedemptionDetail(Redemption redemption, ResortData data) {
    Member member = data.findMember(redemption.getMemberId());
    Reward reward = data.findReward(redemption.getRewardId());
    Guest guest = (member == null) ? null : data.findGuest(member.getGuestId());

    MessageUI.displaySectionHeading("Member");
    MessageUI.displayField("Name", guest == null ? "-" : guest.getFullName());
    MessageUI.displayField("Member ID", redemption.getMemberId());
    MessageUI.displayField("IC / Passport", guest == null ? "-"
        : guest.getIcPassportNo());
    MessageUI.displayField("Tier now", member == null ? "-" : member.getTier());
    MessageUI.displayField("Points balance now", member == null ? "-"
        : String.valueOf(member.getPointsBalance()));

    MessageUI.displaySectionHeading("Reward");
    MessageUI.displayField("Reward", reward == null ? redemption.getRewardId()
        : reward.getRewardName());
    MessageUI.displayField("Category", reward == null ? "-" : reward.getCategory());
    MessageUI.displayField("Points spent", String.valueOf(redemption.getPointsUsed()));
    MessageUI.displayField("Value", reward == null ? "-"
        : String.format("RM%.2f", reward.getCashValue()));

    MessageUI.displaySectionHeading("Request");
    MessageUI.displayField("Redemption ID", redemption.getRedemptionId());
    MessageUI.displayField("Status", redemption.getStatus());
    MessageUI.displayField("Requested on", redemption.getRequestDate().toString());
    if (redemption.getProcessedDate() != null) {
      MessageUI.displayField("Decided on", redemption.getProcessedDate().toString());
    }
    if (redemption.getProcessedBy() != null) {
      MessageUI.displayField("Decided by", redemption.getProcessedBy());
    }
    if (redemption.getRejectReason() != null
        && !redemption.getRejectReason().isBlank()) {
      MessageUI.displayField("Reason", redemption.getRejectReason());
    }

    // Which stay it belongs to - the whole point of tying a reward to a
    // booking, and what tells the spa which guest is turning up.
    MessageUI.displaySectionHeading("Stay");
    if (redemption.getBookingId() == null) {
      MessageUI.displayMessage("  Not tied to a particular booking.");
      return;
    }

    Booking booking = data.findBooking(redemption.getBookingId());
    if (booking == null) {
      MessageUI.displayMessage("  Booking " + redemption.getBookingId()
          + " is no longer on record.");
      return;
    }

    MessageUI.displayField("Booking", booking.getBookingId());
    MessageUI.displayField("Confirmation no.", booking.getConfirmationNumber());
    MessageUI.displayField("Room", booking.getRoomNo() == null
        ? "not assigned" : booking.getRoomNo());
    MessageUI.displayField("Check-in", booking.getCheckInDate().toString());
    MessageUI.displayField("Check-out", booking.getCheckOutDate().toString());
    MessageUI.displayField("Nights", booking.getNumberOfNights() + " night(s)");
    MessageUI.displayField("Booking status", booking.getBookingStatus());
  }

  /**
   * Lists the requests waiting for a decision and takes the one picked.
   *
   * Paged like every other long listing, and S is what starts a selection so a
   * bare number can mean the page without being mistaken for a request.
   *
   * @param pending the queue, in arrival order
   * @param data used to name the member and reward on each row
   * @return the chosen request, or null to go back
   */
  public Redemption choosePendingRedemption(ListInterface<Redemption> pending,
      ResortData data) {
    if (pending.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  No request is waiting for a decision.");
      return null;
    }

    int total = pending.getNumberOfEntries();
    int totalPages = MessageUI.pageCount(total);
    int page = 1;

    while (true) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format(
          "  %-5s %-8s %-8s %-26s %8s  %s",
          "NO", "REDEEM", "MEMBER", "REWARD", "POINTS", "REQUESTED"));

      int from = MessageUI.firstRowOnPage(page);
      int upTo = MessageUI.lastRowOnPage(page, total);

      for (int i = from; i <= upTo; i++) {
        Redemption redemption = pending.getEntry(i);
        Reward reward = data.findReward(redemption.getRewardId());

        System.out.printf("  [%d]   %-8s %-8s %-26s %8d  %s%n",
            i, redemption.getRedemptionId(), redemption.getMemberId(),
            truncate(reward == null ? redemption.getRewardId()
                : reward.getRewardName(), 26),
            redemption.getPointsUsed(), redemption.getRequestDate());
      }

      MessageUI.displayThinRule();
      MessageUI.displayMessage("  First asked is first served - work down from"
          + " the top.");

      if (totalPages == 1) {
        int picked = MessageUI.readInt(scanner,
            "Number of the request to decide", 1, total);
        return (picked == MessageUI.CANCELLED_INT) ? null : pending.getEntry(picked);
      }

      System.out.printf("  Showing %d-%d of %d.%n", from, upTo, total);

      Integer chosen = readPagedSelection(page, totalPages, total, "request");
      if (chosen == null) {
        return null;
      }
      if (chosen > 0) {
        return pending.getEntry(chosen);
      }
      page = -chosen;
    }
  }

  /**
   * Reads a paging command or a selection from one prompt.
   *
   * @param page the page on screen now
   * @param totalPages how many there are
   * @param total how many rows altogether
   * @param noun what is being selected, for the wording
   * @return a positive row to select, a negated page to move to, or null to quit
   */
  private Integer readPagedSelection(int page, int totalPages, int total, String noun) {
    while (true) {
      System.out.printf("  Page %d of %d - [N]ext, [P]revious, [1-%d] jump,"
          + " [S]elect, 0 to go back: ", page, totalPages, totalPages);
      String input = MessageUI.readLine(scanner, "0").trim().toLowerCase();

      if (input.equals("0") || input.equals("q")) {
        return null;
      }
      if (input.isEmpty() || input.equals("n")) {
        return -((page >= totalPages) ? 1 : page + 1);
      }
      if (input.equals("p")) {
        return -((page <= 1) ? totalPages : page - 1);
      }
      if (input.equals("s")) {
        int picked = MessageUI.readInt(scanner,
            "Number of the " + noun, 1, total);
        if (picked == MessageUI.CANCELLED_INT) {
          continue;
        }
        return picked;
      }

      try {
        int wanted = Integer.parseInt(input);
        if (wanted >= 1 && wanted <= totalPages) {
          return -wanted;
        }
        MessageUI.displayError("There is no page " + wanted + ". Enter 1 to "
            + totalPages + ", or S to select.");
      } catch (NumberFormatException notANumber) {
        MessageUI.displayError("Enter N, P, a page number, S to select,"
            + " or 0 to go back.");
      }
    }
  }

  /**
   * Shows one request in full, so the decision is made on the facts.
   *
   * @param redemption the request
   * @param member who asked
   * @param reward what they asked for
   * @param data used to reach the booking behind the request
   */
  public void displayRedemptionForDecision(Redemption redemption, Member member,
      Reward reward, ResortData data) {
    MessageUI.displaySectionHeading("Request " + redemption.getRedemptionId());
    MessageUI.displayField("Member", member == null ? redemption.getMemberId()
        : member.getMemberId() + "  (" + member.getTier() + ")");
    MessageUI.displayField("Reward", reward == null ? redemption.getRewardId()
        : reward.getRewardName());
    MessageUI.displayField("Points required",
        String.valueOf(redemption.getPointsUsed()));
    MessageUI.displayField("Member balance", member == null ? "-"
        : String.valueOf(member.getPointsBalance()));
    MessageUI.displayField("Requested on", redemption.getRequestDate().toString());

    if (redemption.getBookingId() != null) {
      Booking booking = data.findBooking(redemption.getBookingId());
      if (booking != null) {
        MessageUI.displayField("For booking", booking.getBookingId()
            + "  (confirmation " + booking.getConfirmationNumber() + ")");
        MessageUI.displayField("Stay", booking.getCheckInDate() + " to "
            + booking.getCheckOutDate());
        MessageUI.displayField("Booking status", booking.getBookingStatus());
      }
    }
  }

  /**
   * Shows what each tier costs and how many members sit in it.
   *
   * @param members every member, counted by tier
   */
  public void displayTierThresholds(ListInterface<Member> members) {
    int[] counts = new int[4];
    for (int i = 1; i <= members.getNumberOfEntries(); i++) {
      switch (members.getEntry(i).getTier()) {
        case Member.DIAMOND: counts[3]++; break;
        case Member.PLATINUM: counts[2]++; break;
        case Member.GOLD: counts[1]++; break;
        default: counts[0]++; break;
      }
    }

    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format("  %-12s %16s  %s",
        "TIER", "LIFETIME POINTS", "MEMBERS"));
    System.out.printf("  %-12s %16s  %d%n", Member.SILVER, "0 (everyone)", counts[0]);
    System.out.printf("  %-12s %16d  %d%n", Member.GOLD,
        Member.getGoldThreshold(), counts[1]);
    System.out.printf("  %-12s %16d  %d%n", Member.PLATINUM,
        Member.getPlatinumThreshold(), counts[2]);
    System.out.printf("  %-12s %16d  %d%n", Member.DIAMOND,
        Member.getDiamondThreshold(), counts[3]);
    MessageUI.displayThinRule();
    MessageUI.displayMessage("  A tier is earned on lifetime points, so these"
        + " apply to everyone at once.");
  }

  /**
   * Asks for one tier's threshold.
   *
   * @param tier which tier it is for
   * @param current what it is now
   * @return the new figure, or -1 if cancelled
   */
  public int inputThreshold(String tier, int current) {
    int value = MessageUI.readInt(scanner,
        "Lifetime points for " + tier + " [" + current + "]", 1, 1000000);
    return (value == MessageUI.CANCELLED_INT) ? -1 : value;
  }

  /**
   * Shows who would move tier if the proposed thresholds were applied.
   *
   * A demotion is the thing worth seeing: a guest can lose a tier through a
   * management decision rather than anything they did, so it is named before
   * the change is saved rather than discovered afterwards.
   *
   * @param members everybody on the books
   * @param gold the proposed GOLD threshold
   * @param platinum the proposed PLATINUM threshold
   * @param diamond the proposed DIAMOND threshold
   */
  public void displayRetierPreview(ListInterface<Member> members, int gold,
      int platinum, int diamond, ResortData data) {
    MessageUI.displaySectionHeading("What this would change");

    int up = 0;
    int down = 0;
    boolean listed = false;

    for (int i = 1; i <= members.getNumberOfEntries(); i++) {
      Member member = members.getEntry(i);
      String now = member.getTier();
      String after = tierUnder(member.getLifetimePoints(), gold, platinum, diamond);
      if (now.equals(after)) {
        continue;
      }

      boolean promotion = Member.tierRank(after) > Member.tierRank(now);
      if (promotion) {
        up++;
      } else {
        down++;
      }

      if (!listed) {
        MessageUI.displayTableHeading(String.format("  %-8s %-22s %10s  %s",
            "MEMBER", "GUEST", "LIFETIME", "TIER"));
        listed = true;
      }
      Guest guest = data.findGuest(member.getGuestId());
      System.out.printf("  %-8s %-22s %10d  %s -> %s%s%n",
          member.getMemberId(),
          truncate(guest == null ? "-" : guest.getFullName(), 22),
          member.getLifetimePoints(), now, after, promotion ? "" : "   (demoted)");
    }

    if (!listed) {
      MessageUI.displayMessage("  Nobody changes tier.");
      return;
    }

    MessageUI.displayThinRule();
    System.out.printf("  %d promoted, %d demoted.%n", up, down);
  }

  /** Which tier a lifetime total would earn under the proposed thresholds. */
  private String tierUnder(int lifetimePoints, int gold, int platinum, int diamond) {
    if (lifetimePoints >= diamond) {
      return Member.DIAMOND;
    }
    if (lifetimePoints >= platinum) {
      return Member.PLATINUM;
    }
    if (lifetimePoints >= gold) {
      return Member.GOLD;
    }
    return Member.SILVER;
  }

  /**
   * Shows one reward in full.
   *
   * @param reward the reward to show
   */
  public void displayReward(Reward reward) {
    MessageUI.displayBlankLine();
    MessageUI.displayField("Reward ID", reward.getRewardId());
    MessageUI.displayField("Name", reward.getRewardName());
    MessageUI.displayField("Category", reward.getCategory());
    MessageUI.displayField("Points required", String.valueOf(reward.getPointsRequired()));
    MessageUI.displayField("Minimum tier", reward.getMinimumTier());
    MessageUI.displayField("Cash value", String.format("RM%.2f", reward.getCashValue()));
    MessageUI.displayField("In stock", String.valueOf(reward.getStockQuantity()));
    MessageUI.displayField("Active", reward.isActive() ? "Yes" : "No");
  }

  /**
   * Walks the editable fields of a reward, keeping whatever is left blank.
   *
   * The reward is changed in place as each answer is given, so a value that
   * fails its own check is refused before it reaches the record.
   *
   * @param reward the reward being edited
   * @return ok when something was changed, or a failure explaining why not
   */
  public ServiceResult<Reward> editRewardFields(Reward reward) {
    MessageUI.displaySectionHeading("Edit " + reward.getRewardId());

    String name = MessageUI.readOptionalText(scanner,
        "Name [" + reward.getRewardName() + "]");
    if (name != null && !name.isBlank()) {
      reward.setRewardName(name.trim());
    }

    String category = inputRewardCategory(reward.getCategory());
    if (category != null) {
      reward.setCategory(category);
    }

    String points = MessageUI.readOptionalText(scanner,
        "Points required [" + reward.getPointsRequired() + "]");
    if (points != null && !points.isBlank()) {
      try {
        int value = Integer.parseInt(points.trim());
        if (value <= 0) {
          return ServiceResult.fail("Points required must be more than zero.");
        }
        reward.setPointsRequired(value);
      } catch (NumberFormatException notANumber) {
        return ServiceResult.fail("Points required must be a whole number.");
      }
    }

    String value = MessageUI.readOptionalText(scanner,
        String.format("Cash value [RM%.2f]", reward.getCashValue()));
    if (value != null && !value.isBlank()) {
      try {
        double amount = Double.parseDouble(value.trim());
        if (amount < 0) {
          return ServiceResult.fail("A cash value cannot be negative.");
        }
        reward.setCashValue(Math.round(amount * 100.0) / 100.0);
      } catch (NumberFormatException notANumber) {
        return ServiceResult.fail("Cash value must be an amount, e.g. 160.00");
      }
    }

    String stock = MessageUI.readOptionalText(scanner,
        "In stock [" + reward.getStockQuantity() + "]");
    if (stock != null && !stock.isBlank()) {
      try {
        int quantity = Integer.parseInt(stock.trim());
        if (quantity < 0) {
          return ServiceResult.fail("Stock cannot be negative.");
        }
        reward.setStockQuantity(quantity);
      } catch (NumberFormatException notANumber) {
        return ServiceResult.fail("Stock must be a whole number.");
      }
    }

    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  Currently "
        + (reward.isActive() ? "on offer." : "withdrawn."));
    if (MessageUI.confirm(scanner, reward.isActive()
        ? "Withdraw it from the catalogue?" : "Put it back on offer?")) {
      reward.setActive(!reward.isActive());
    }

    return ServiceResult.ok("Updated.", reward);
  }

  /**
   * Asks which category a reward belongs to, or keeps the one it has.
   *
   * @param current what it is now
   * @return the chosen category, or null to leave it alone
   */
  private String inputRewardCategory(String current) {
    String[] categories = {
      Reward.CAT_SPA, Reward.CAT_DINING, Reward.CAT_ACTIVITY,
      Reward.CAT_WELLNESS, Reward.CAT_TRANSPORT
    };

    MessageUI.displayMessage("  Category [" + current + "]:");
    for (int i = 0; i < categories.length; i++) {
      MessageUI.displayMessage("     [" + (i + 1) + "]  " + categories[i]);
    }

    String answer = MessageUI.readOptionalText(scanner,
        "Category number (ENTER to keep " + current + ")");
    if (answer == null || answer.isBlank()) {
      return null;
    }

    try {
      int picked = Integer.parseInt(answer.trim());
      if (picked >= 1 && picked <= categories.length) {
        return categories[picked - 1];
      }
    } catch (NumberFormatException notANumber) {
      // fall through - an unreadable answer keeps what is there
    }

    MessageUI.displayError("Category unchanged - " + current + " kept.");
    return null;
  }

  public Member chooseMember(ListInterface<Member> members, ResortData data,
      String prompt, String emptyMessage) {
    if (members.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  " + emptyMessage);
      return null;
    }

    int total = members.getNumberOfEntries();
    int totalPages = MessageUI.pageCount(total);
    int page = 1;

    while (true) {
      MessageUI.displayBlankLine();
      // The lifetime total is not shown when picking a member to act on: what
      // matters here is the balance they can actually spend. Lifetime points
      // only decide the tier, and the tier is already on the row.
      MessageUI.displayTableHeading(String.format(
          "  %-5s %-7s %-24s %-9s %9s  %s",
          "NO", "MEMBER", "GUEST", "TIER", "BALANCE", "EXPIRES"));

      int from = MessageUI.firstRowOnPage(page);
      int upTo = MessageUI.lastRowOnPage(page, total);

      for (int i = from; i <= upTo; i++) {
        Member member = members.getEntry(i);
        Guest guest = data.findGuest(member.getGuestId());

        System.out.printf("  [%d]   %-7s %-24s %-9s %9d  %s%n",
            i, member.getMemberId(),
            guest == null ? "-" : truncate(guest.getFullName(), 24),
            member.getTier(), member.getPointsBalance(),
            member.getPointsExpiryDate());
      }

      MessageUI.displayThinRule();

      if (totalPages == 1) {
        System.out.printf("  %d member(s).%n", total);
        int picked = MessageUI.readInt(scanner, prompt, 1, total);
        return (picked == MessageUI.CANCELLED_INT) ? null : members.getEntry(picked);
      }

      System.out.printf("  Showing %d-%d of %d member(s).%n", from, upTo, total);

      // One prompt does both jobs: a bare number is always a member, and
      // paging is a letter, so the two can never be mistaken for each other.
      while (true) {
        System.out.printf("  %s (1-%d), [N]ext page, [P]revious, 0 to go back: ",
            prompt, total);
        String input = MessageUI.readLine(scanner, "0").trim().toLowerCase();

        if (input.equals("0") || input.equals("q")) {
          return null;
        }
        if (input.isEmpty() || input.equals("n")) {
          page = (page >= totalPages) ? 1 : page + 1;
          break;
        }
        if (input.equals("p")) {
          page = (page <= 1) ? totalPages : page - 1;
          break;
        }

        try {
          int picked = Integer.parseInt(input);
          if (picked < 1 || picked > total) {
            MessageUI.displayError("There is no member " + picked
                + " in that list. Enter 1 to " + total + ".");
            continue;
          }
          return members.getEntry(picked);
        } catch (NumberFormatException notANumber) {
          MessageUI.displayError("Enter a member number, N, P, or 0 to go back.");
        }
      }
    }
  }

  /**
   * Lists the reward catalogue and takes the one picked by its row number.
   *
   * @param rewards the rewards to offer
   * @param prompt what the number is being asked for
   * @param emptyMessage what to say when the catalogue is empty
   * @return the chosen reward, or null if the officer quit
   */
  public Reward chooseReward(ListInterface<Reward> rewards, String prompt,
      String emptyMessage) {
    if (rewards.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  " + emptyMessage);
      return null;
    }

    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format("  %-5s %-8s %-28s %-10s %8s %8s  %s",
        "NO", "REWARD", "NAME", "CATEGORY", "POINTS", "STOCK", "ACTIVE"));

    for (int i = 1; i <= rewards.getNumberOfEntries(); i++) {
      Reward reward = rewards.getEntry(i);
      System.out.printf("  [%d]   %-8s %-28s %-10s %8d %8d  %s%n",
          i, reward.getRewardId(), truncate(reward.getRewardName(), 28),
          truncate(reward.getCategory(), 10), reward.getPointsRequired(),
          reward.getStockQuantity(), reward.isActive() ? "Yes" : "No");
    }

    MessageUI.displayThinRule();
    System.out.printf("  %d reward(s).%n", rewards.getNumberOfEntries());

    int picked = MessageUI.readInt(scanner, prompt, 1, rewards.getNumberOfEntries());
    return (picked == MessageUI.CANCELLED_INT) ? null : rewards.getEntry(picked);
  }

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

    MessageUI.displayTableHeading(String.format(
        "  %-7s %-11s %-8s %8s %9s %-8s  %s",
        "TXN", "DATE", "TYPE", "POINTS", "BALANCE", "BOOKING", "DESCRIPTION"));

    int earned = 0;
    int spent = 0;

    for (int i = 1; i <= txns.getNumberOfEntries(); i++) {
      PointTransaction txn = txns.getEntry(i);

      if (txn.getPoints() > 0) {
        earned += txn.getPoints();
      } else {
        spent += -txn.getPoints();
      }

      System.out.printf("  %-7s %-11s %-8s %+8d %9d %-8s  %s%n",
          txn.getTxnId(), txn.getTxnDate(), txn.getTxnType(),
          txn.getPoints(), txn.getBalanceAfter(),
          txn.getBookingId() == null ? "-" : txn.getBookingId(),
          txn.getDescription() == null ? "" : txn.getDescription());
    }

    // The two totals said plainly, so the balance is not something the reader
    // has to add up themselves from a column of signed figures.
    MessageUI.displayThinRule();
    System.out.printf("  %-24s %+8d points%n", "Total earned", earned);
    System.out.printf("  %-24s %+8d points%n", "Total spent or expired", -spent);
    System.out.printf("  %-24s %8d points%n", "Balance now",
        member.getPointsBalance());
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
