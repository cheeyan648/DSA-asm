package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.WalkInRegistrationBookingUI;
import entity.WalkInGuest;

/**
 *
 * @author Kat Tan
 */
public class WalkInRegistrationBookingMaintenance {

  private WalkInRegistrationBookingUI walkInRegistrationBookingUI = new WalkInRegistrationBookingUI();
  private ListInterface<WalkInGuest> walkInQueue = new ArrayList<>();

  // TODO: developer to implement Walk-In Registration & Standard Booking logic
  //
  // REQUIRED ADT: List (adt/ListInterface + adt/ArrayList), already wired up
  // as walkInQueue above. This module must use the List ADT so it satisfies
  // the assignment's List ADT requirement - do not remove it.
  //
  // Pattern: Queue-like (FIFO) built on top of the List ADT.
  //   - Registering a guest  -> walkInQueue.add(guest)                (join back of line)
  //   - Serving next guest   -> walkInQueue.remove(1)                 (always position 1 = front)
  //   - Priority walk-in     -> walkInQueue.add(1, guest)             (jump to front)
  //
  // OPTIONAL ADTs available in the adt package - use only what genuinely
  // helps your design, on top of the required List ADT above:
  //   - adt/ArrayQueue (adt/QueueInterface): true FIFO (enqueue/dequeue).
  //     Good fit here since walk-ins are naturally a queue; could replace
  //     or sit alongside walkInQueue if you want the enqueue/dequeue
  //     vocabulary instead of List's add/remove(1).
  //   - adt/ArrayStack (adt/StackInterface): true LIFO (push/pop). Not a
  //     natural fit for this module's FIFO ordering, but could be used for
  //     something like an "undo last registration" feature if you add one.
  //   - Any other ADT you build yourself (e.g. a BST) is also fine if a
  //     feature calls for it. The List ADT above must stay as the core
  //     structure regardless of what else you add.
  //
  // Suggested methods to add:
  //   1. registerWalkIn()
  //      - Get a new WalkInGuest from walkInRegistrationBookingUI.inputWalkInGuest().
  //      - walkInQueue.add(guest) to enqueue at the back.
  //      - Show a confirmation message via the UI.
  //
  //   2. insertPriorityWalkIn(WalkInGuest guest)
  //      - Add-on method for urgent edge cases (e.g. VIP, complaint escalation).
  //      - walkInQueue.add(1, guest) inserts at the very front of the queue,
  //        ahead of everyone already waiting.
  //
  //   3. serveNextGuest()
  //      - WalkInGuest served = walkInQueue.remove(1);
  //      - Pass the result to the UI to display ("now serving: ...") or an
  //        empty-queue message if remove(1) returned null.
  //
  //   4. displayQueue()
  //      - Pass walkInQueue to walkInRegistrationBookingUI.displayAllGuests(...)
  //        so it can iterate with walkInQueue.getIterator() and print guests
  //        in arrival order.
  //
  //   5. runWalkInRegistrationBooking()
  //      - The loop below already handles the landing menu and "0. Back"
  //        (returns to TARUMTResortUI). Fill in cases 1-4 to call the
  //        methods above as you implement them.
  public void runWalkInRegistrationBooking() {
    int choice = 0;
    do {
      choice = walkInRegistrationBookingUI.getMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
        case 2:
        case 3:
        case 4:
          // TODO: developer to call registerWalkIn() / insertPriorityWalkIn()
          // / serveNextGuest() / displayQueue() here based on choice
          break;
      }
    } while (choice != 0);
  }
}
