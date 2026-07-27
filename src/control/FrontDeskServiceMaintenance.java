package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.FrontDeskServiceUI;
import entity.Booking;

/**
 *
 * @author Kat Tan
 */
public class FrontDeskServiceMaintenance {

  private FrontDeskServiceUI frontDeskServiceUI = new FrontDeskServiceUI();
  private ListInterface<Booking> bookingList = new ArrayList<>();
  // TODO: add a BST field here once it exists, e.g.:
  //   private BSTInterface<String, Booking> bookingIndex = new BinarySearchTree<>();
  // keyed by confirmationNumber, for O(log n) search instead of scanning the List.

  // TODO: developer to implement Front-Desk Service logic
  //
  // REQUIRED ADT: List (adt/ListInterface + adt/ArrayList), already wired up
  // as bookingList above. This module must use the List ADT so it satisfies
  // the assignment's List ADT requirement - do not remove it.
  //
  // Pattern: Master storage (List, arrival order) + secondary BST index
  // (confirmation number -> Booking) for fast search. The List itself is
  // never searched directly - that's what the BST is for.
  //
  // OPTIONAL ADTs available in the adt package - use only what genuinely
  // helps your design, on top of the required List ADT above:
  //   - A BST is the natural fit for this module's fast-search requirement
  //     (confirmation number lookup), but it doesn't exist in adt/ yet -
  //     you'd need to create BSTInterface/BinarySearchTree yourself if you
  //     want it. If that's too much for now, a plain linear scan over
  //     bookingList with getIterator() also works, just slower.
  //   - adt/ArrayStack (adt/StackInterface) / adt/ArrayQueue
  //     (adt/QueueInterface): not a natural fit for searching, but either
  //     could support a side feature if you add one (e.g. a stack of
  //     recently-viewed bookings, or a queue of check-ins to process).
  //   - The List ADT above must stay as the core structure regardless of
  //     what else you add.
  //
  // Suggested methods to add:
  //   1. createBooking()
  //      - Get a new Booking from frontDeskServiceUI.inputBooking().
  //      - bookingList.add(booking) to record it in arrival order.
  //      - syncToIndex(booking) to also insert it into the BST.
  //      - Show a confirmation message via the UI.
  //
  //   2. syncToIndex(Booking booking)
  //      - Add-on method: inserts a reference into the BST every time a
  //        booking is added to bookingList, keyed by
  //        booking.getConfirmationNumber(). Keeps the two structures
  //        consistent without repeating the insert logic at every call site.
  //
  //   3. searchBookingByConfirmationNumber(String confirmationNumber)
  //      - Look up the BST directly (NOT bookingList) - this is the whole
  //        point of pairing a BST with the List: O(log n) search instead of
  //        an O(n) scan.
  //      - Return the matching Booking, or null if not found.
  //
  //   4. displayAllBookings()
  //      - Pass bookingList to frontDeskServiceUI.displayAllBookings(...) so
  //        it can iterate with bookingList.getIterator() in arrival order.
  //
  //   5. runFrontDeskService()
  //      - The loop below already handles the landing menu and "0. Back"
  //        (returns to TARUMTResortUI). Fill in cases 1-3 to call the
  //        methods above as you implement them.
  public void runFrontDeskService() {
    int choice = 0;
    do {
      choice = frontDeskServiceUI.getMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
        case 2:
        case 3:
          // TODO: developer to call createBooking() /
          // searchBookingByConfirmationNumber() / displayAllBookings() here
          // based on choice
          break;
      }
    } while (choice != 0);
  }
}
