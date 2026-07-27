package boundary;

import java.util.Scanner;
import utility.MessageUI;

/**
 *
 * @author Kat Tan
 */
public class LoyaltyRewardsUI {

  // Shared with every other UI class - see MessageUI.scanner for why.
  private Scanner scanner = MessageUI.scanner;

  // Landing menu for this module. Only "0. Back" is wired up so far - the
  // developer implementing this module should fill in cases 1-4 in
  // LoyaltyRewardsMaintenance.runLoyaltyRewards() and can extend this menu
  // with more options if needed.
  public int getMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nLOYALTY & REWARDS");
    System.out.println("1. Register new member");
    System.out.println("2. Check if a member exists");
    System.out.println("3. Display members sorted by points");
    System.out.println("4. Find members with expiring points");
    System.out.println("0. Back to main menu");

    // Keeps re-prompting (without clearing) until a valid 0-4 is entered, so
    // the user can see the error message and correct their input.
    return MessageUI.readMenuChoice(scanner, 4, "go back to the main menu");
  }

  // TODO: developer to implement Loyalty & Rewards UI
  //
  // This is general member storage: members are added via add() and looked
  // up with getEntry()/contains(). It also needs a custom sort
  // (sortByPoints) and a filter/search pass (findExpiringPoints). Build the
  // menu/input methods below; leave the sort/filter logic to the
  // Maintenance class.
  //
  // Suggested methods to add (getMenuChoice() above is already done):
  //   1. inputMember()
  //      - Prompt for memberId, name, points, pointsExpiryDate.
  //      - Return a new entity.Member(memberId, name, points, pointsExpiryDate).
  //
  //   2. inputMemberId() / inputCutoffDate()
  //      - Small prompts for the lookup and expiring-points features -
  //        keep these separate from inputMember() so the control layer can
  //        call just what it needs.
  //
  //   3. displayMember(Member member)
  //      - Print one member's details, or a "not found" message when null.
  //
  //   4. displayMembers(ListInterface<Member> list)
  //      - Use list.getIterator() to print every member in whatever order
  //        the list is currently in (e.g. after sortByPoints() reorders it,
  //        or for the filtered expiring-points result).
}
