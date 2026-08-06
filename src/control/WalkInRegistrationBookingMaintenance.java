package control;

import adt.ArrayStack;
import adt.ListInterface;
import adt.StackInterface;
import boundary.WalkInRegistrationBookingUI;
import dao.WalkInGuestDAO;
import dao.WalkInGuestInitializer;
import entity.WalkInGuest;

/**
 *
 * @author Kat Tan
 */
public class WalkInRegistrationBookingMaintenance {

  private WalkInRegistrationBookingUI walkInRegistrationBookingUI = new WalkInRegistrationBookingUI();
  private WalkInGuestDAO walkInGuestDAO = new WalkInGuestDAO();
  private ListInterface<WalkInGuest> walkInQueue;

  // Add-on feature: remembers registrations made during this session so the
  // most recent one can be undone. A stack is the natural fit - undo always
  // targets the newest registration first (LIFO). This is deliberately NOT
  // persisted: an undo only makes sense for actions taken in the current run,
  // so the history starts empty each time the program is launched.
  private StackInterface<WalkInGuest> registrationHistory = new ArrayStack<>();

  public WalkInRegistrationBookingMaintenance() {
    // Load the saved queue from walkInGuests.dat. If there's nothing saved
    // yet (first run), seed it with the sample guests so there is data to
    // work with, then write that file out.
    walkInQueue = walkInGuestDAO.retrieveFromFile();
    if (walkInQueue.isEmpty()) {
      walkInQueue = new WalkInGuestInitializer().initializeWalkInGuests();
      walkInGuestDAO.saveToFile(walkInQueue);
    }
  }

  // DATA PERSISTENCE: walkInQueue is loaded from walkInGuests.dat in the
  // constructor above and written back by saveToFile() after every action that
  // changes the queue, so nothing is lost even if the program is killed
  // without going through "0. Back".
  //
  // REQUIRED ADT: List (adt/ListInterface + adt/ArrayList), used as walkInQueue
  // above. This module must use the List ADT so it satisfies the assignment's
  // List ADT requirement.
  //
  // Pattern: Queue-like (FIFO) built on top of the List ADT, using 1-based
  // positions.
  //   - Registering a guest  -> walkInQueue.add(guest)                (join back of line)
  //   - Serving next guest   -> walkInQueue.remove(1)                 (always position 1 = front)
  //   - Priority walk-in     -> walkInQueue.add(1, guest)             (jump to front)
  //
  // The Stack ADT (adt/ArrayStack) is used on top of that for the undo feature
  // - see registrationHistory above.

  /**
   * Registers a new walk-in guest at the back of the queue.
   *
   * Rejects a guest ID that is already waiting: WalkInGuest.equals() compares
   * on guestId alone, so the List ADT's contains() is enough to detect it.
   */
  public void registerWalkIn() {
    WalkInGuest guest = walkInRegistrationBookingUI.inputWalkInGuest(false);

    if (guest == null) {
      walkInRegistrationBookingUI.displayMessage("Registration cancelled.");
      return;
    }
    if (walkInQueue.contains(guest)) {
      walkInRegistrationBookingUI.displayMessage(
          "Guest ID " + guest.getGuestId() + " is already in the queue! Registration rejected.");
      return;
    }

    walkInQueue.add(guest);
    registrationHistory.push(guest);
    walkInGuestDAO.saveToFile(walkInQueue);

    walkInRegistrationBookingUI.displayMessage(
        "Guest registered at position " + walkInQueue.getNumberOfEntries() + " of the queue.");
    walkInRegistrationBookingUI.displayGuest(guest);
  }

  /**
   * Inserts an urgent walk-in guest at the very front of the queue, ahead of
   * everyone already waiting (e.g. VIP or complaint escalation).
   *
   * @param guest the priority guest to insert, may be null if the user
   * cancelled at the input prompt
   */
  public void insertPriorityWalkIn(WalkInGuest guest) {
    if (guest == null) {
      walkInRegistrationBookingUI.displayMessage("Priority registration cancelled.");
      return;
    }
    if (walkInQueue.contains(guest)) {
      walkInRegistrationBookingUI.displayMessage(
          "Guest ID " + guest.getGuestId() + " is already in the queue! Registration rejected.");
      return;
    }

    walkInQueue.add(1, guest);
    registrationHistory.push(guest);
    walkInGuestDAO.saveToFile(walkInQueue);

    walkInRegistrationBookingUI.displayMessage(
        "Priority guest inserted at the front of the queue (position 1).");
    walkInRegistrationBookingUI.displayGuest(guest);
  }

  /**
   * Serves the guest at the front of the queue, removing them from it.
   *
   * remove(1) returns null when the list is empty rather than throwing, so a
   * null result is what tells us the queue had no one waiting.
   */
  public void serveNextGuest() {
    WalkInGuest served = walkInQueue.remove(1);

    if (served != null) {
      walkInGuestDAO.saveToFile(walkInQueue);
    }

    walkInRegistrationBookingUI.displayGuestServedMessage(served);

    if (served != null) {
      walkInRegistrationBookingUI.displayMessage(
          "Guests still waiting: " + walkInQueue.getNumberOfEntries());
    }
  }

  /**
   * Shows every guest currently waiting, in service order.
   */
  public void displayQueue() {
    walkInRegistrationBookingUI.displayAllGuests(walkInQueue);
  }

  /**
   * Add-on feature: undoes the most recent registration made in this session by
   * popping it off the history stack and removing it from the queue.
   *
   * The guest may already have been served since being registered, in which
   * case there is nothing left in the queue to remove - that is reported rather
   * than treated as an error.
   */
  public void undoLastRegistration() {
    WalkInGuest lastRegistered = registrationHistory.pop();

    if (lastRegistered == null) {
      walkInRegistrationBookingUI.displayMessage(
          "Nothing to undo - no guests have been registered in this session.");
      return;
    }

    int position = findPosition(lastRegistered);
    if (position == -1) {
      walkInRegistrationBookingUI.displayMessage(
          "Cannot undo - guest " + lastRegistered.getGuestId()
          + " has already been served and is no longer in the queue.");
      return;
    }

    walkInQueue.remove(position);
    walkInGuestDAO.saveToFile(walkInQueue);

    walkInRegistrationBookingUI.displayMessage(
        "Undone - registration for guest " + lastRegistered.getGuestId() + " has been removed.");
    walkInRegistrationBookingUI.displayGuest(lastRegistered);
  }

  /**
   * Finds where a guest currently sits in the queue.
   *
   * @param guest the guest to look for
   * @return the 1-based position of the guest, or -1 if they are not queued
   */
  private int findPosition(WalkInGuest guest) {
    for (int position = 1; position <= walkInQueue.getNumberOfEntries(); position++) {
      if (guest.equals(walkInQueue.getEntry(position))) {
        return position;
      }
    }
    return -1;
  }

  public void runWalkInRegistrationBooking() {
    int choice = 0;
    do {
      choice = walkInRegistrationBookingUI.getMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
          registerWalkIn();
          walkInRegistrationBookingUI.pause();
          break;
        case 2:
          // The priority guest is gathered here because insertPriorityWalkIn()
          // takes the guest as a parameter, so it can also be called directly
          // by other code with a guest that was built elsewhere.
          insertPriorityWalkIn(walkInRegistrationBookingUI.inputWalkInGuest(true));
          walkInRegistrationBookingUI.pause();
          break;
        case 3:
          serveNextGuest();
          walkInRegistrationBookingUI.pause();
          break;
        case 4:
          displayQueue();
          walkInRegistrationBookingUI.pause();
          break;
        case 5:
          undoLastRegistration();
          walkInRegistrationBookingUI.pause();
          break;
      }
    } while (choice != 0);

    // Persist the queue so any changes survive to the next run.
    walkInGuestDAO.saveToFile(walkInQueue);
  }
}
