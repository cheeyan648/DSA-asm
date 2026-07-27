package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.WalkInGuest;

/**
 * Generates a small set of sample walk-in guests, used to seed
 * walkInGuests.dat the first time the program runs (or whenever the queue is
 * empty) so there is something to demonstrate with.
 *
 * @author Kat Tan
 */
public class WalkInGuestInitializer {

  public ListInterface<WalkInGuest> initializeWalkInGuests() {
    ListInterface<WalkInGuest> walkInQueue = new ArrayList<>();

    // Queue order matters here - position 1 is the front of the line (served
    // first), so these are listed in arrival order.
    walkInQueue.add(new WalkInGuest("WG1001", "Tan Chee Yan", "012-3456789", false));
    walkInQueue.add(new WalkInGuest("WG1002", "Lim Yong Le", "013-2233445", false));
    walkInQueue.add(new WalkInGuest("WG1003", "Nur Aisyah binti Rahman", "011-98765432", true));
    walkInQueue.add(new WalkInGuest("WG1004", "Ivan Wong", "016-7788990", false));
    walkInQueue.add(new WalkInGuest("WG1005", "Chong Zhi Ying", "014-5566778", false));

    return walkInQueue;
  }
}
