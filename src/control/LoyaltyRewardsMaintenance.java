package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.LoyaltyRewardsUI;
import entity.Member;
// import java.time.LocalDate; // uncomment when implementing findExpiringPoints(LocalDate cutoff)

/**
 *
 * @author Kat Tan
 */
public class LoyaltyRewardsMaintenance {

  private LoyaltyRewardsUI loyaltyRewardsUI = new LoyaltyRewardsUI();
  private ListInterface<Member> memberList = new ArrayList<>();

  // TODO: developer to implement Loyalty & Rewards logic
  //
  // REQUIRED ADT: List (adt/ListInterface + adt/ArrayList), already wired up
  // as memberList above. This module must use the List ADT so it satisfies
  // the assignment's List ADT requirement - do not remove it.
  //
  // Pattern: General member storage on top of the List ADT.
  //   - Registering a member -> memberList.add(member)
  //   - Lookup               -> memberList.getEntry(position) / memberList.contains(member)
  //
  // OPTIONAL ADTs available in the adt package - use only what genuinely
  // helps your design, on top of the required List ADT above:
  //   - adt/ArrayStack (adt/StackInterface) / adt/ArrayQueue
  //     (adt/QueueInterface): this module doesn't naturally need FIFO/LIFO
  //     ordering, but either could support a side feature if you add one
  //     (e.g. a queue of pending redemption requests, or a stack of recent
  //     point-earning transactions to undo).
  //   - A BST is also an option later if you want faster lookup by
  //     memberId than a linear scan/contains().
  //   - The List ADT above must stay as the core structure regardless of
  //     what else you add.
  //
  // Suggested methods to add:
  //   1. registerMember()
  //      - Get a new Member from loyaltyRewardsUI.inputMember().
  //      - memberList.add(member).
  //      - Show a confirmation message via the UI.
  //
  //   2. isMemberRegistered(Member member)
  //      - return memberList.contains(member);
  //      - Note: relies on Member.equals() comparing by memberId (already
  //        implemented in entity.Member).
  //
  //   3. sortByPoints()
  //      - Add-on method: implement your own sort algorithm (e.g. selection
  //        sort or insertion sort) over memberList, ordering members by
  //        getPoints() descending (highest-value members first).
  //      - Walk the list with getEntry(i)/replace(i, member) to reorder in
  //        place, since ListInterface has no built-in sort.
  //
  //   4. findExpiringPoints(LocalDate cutoff)
  //      - Add-on method: a filter/search pass over memberList.
  //      - Use memberList.getIterator() (or getEntry() in a loop) to collect
  //        every Member whose getPointsExpiryDate() is on/before cutoff into
  //        a new ListInterface<Member> to return.
  //
  //   5. runLoyaltyRewards()
  //      - The loop below already handles the landing menu and "0. Back"
  //        (returns to TARUMTResortUI). Fill in cases 1-4 to call the
  //        methods above as you implement them.
  public void runLoyaltyRewards() {
    int choice = 0;
    do {
      choice = loyaltyRewardsUI.getMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
        case 2:
        case 3:
        case 4:
          // TODO: developer to call registerMember() / isMemberRegistered()
          // / sortByPoints() / findExpiringPoints() here based on choice
          break;
      }
    } while (choice != 0);
  }
}
