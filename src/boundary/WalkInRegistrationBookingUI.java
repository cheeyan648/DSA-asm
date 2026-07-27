package boundary;

import java.util.Scanner;
import utility.MessageUI;

/**
 *
 * @author Kat Tan
 */
public class WalkInRegistrationBookingUI {

  // Shared with every other UI class - see MessageUI.scanner for why.
  private Scanner scanner = MessageUI.scanner;

  // Landing menu for this module. Only "0. Back" is wired up so far - the
  // developer implementing this module should fill in cases 1-4 in
  // WalkInRegistrationBookingMaintenance.runWalkInRegistrationBooking() and
  // can extend this menu with more options if needed.
  public int getMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nWALK-IN REGISTRATION & STANDARD BOOKING");
    System.out.println("1. Register walk-in guest (join queue)");
    System.out.println("2. Insert priority walk-in guest (jump queue)");
    System.out.println("3. Serve next guest (remove from front)");
    System.out.println("4. Display queue");
    System.out.println("0. Back to main menu");

    // Keeps re-prompting (without clearing) until a valid 0-4 is entered, so
    // the user can see the error message and correct their input.
    return MessageUI.readMenuChoice(scanner, 4, "go back to the main menu");
  }

  // TODO: developer to implement Walk-In Registration & Standard Booking UI
  //
  // This is a queue-style module (FIFO): guests join the back of the line via
  // add(), and are served from the front via remove(1). Build the menu/input
  // methods below; leave the actual queue logic to the Maintenance class.
  //
  // Suggested methods to add (getMenuChoice() above is already done):
  //   1. inputWalkInGuest()
  //      - Prompt for and read guestId, name, contactNumber.
  //      - Return a new entity.WalkInGuest(guestId, name, contactNumber, false).
  //      - For the priority variant, set priority = true instead.
  //
  //   2. displayGuest(WalkInGuest guest)
  //      - Print a single guest's details (or "No guest" if null, e.g. after
  //        serving from an empty queue).
  //
  //   3. displayAllGuests(ListInterface<WalkInGuest> list)
  //      - Use list.getIterator() to walk the queue front-to-back and print
  //        each entry, numbering positions 1..n so the user can see queue order.
  //
  //   4. displayMessage(String message) / displayGuestServedMessage(...)
  //      - Small helper(s) for confirmation/error text, following the style
  //        of utility.MessageUI.
}
