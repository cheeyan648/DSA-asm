package boundary;

import java.util.Scanner;
import utility.MessageUI;

/**
 *
 * @author Kat Tan
 */
public class FrontDeskServiceUI {

  // Shared with every other UI class - see MessageUI.scanner for why.
  private Scanner scanner = MessageUI.scanner;

  // Landing menu for this module. Only "0. Back" is wired up so far - the
  // developer implementing this module should fill in cases 1-3 in
  // FrontDeskServiceMaintenance.runFrontDeskService() and can extend this
  // menu with more options if needed.
  public int getMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nFRONT-DESK SERVICE");
    System.out.println("1. Create new booking");
    System.out.println("2. Search booking by confirmation number");
    System.out.println("3. Display all bookings (arrival order)");
    System.out.println("0. Back to main menu");

    // Keeps re-prompting (without clearing) until a valid 0-3 is entered, so
    // the user can see the error message and correct their input.
    return MessageUI.readMenuChoice(scanner, 3, "go back to the main menu");
  }

  // TODO: developer to implement Front-Desk Service UI
  //
  // This module keeps a master List of bookings in arrival order (add()
  // only), paired with a separate BST (non-linear structure, not yet
  // created in adt/) keyed by the 8-digit confirmation number for fast
  // search. Build the menu/input methods below; leave the storage +
  // BST-sync logic to the Maintenance class.
  //
  // Suggested methods to add (getMenuChoice() above is already done):
  //   1. inputBooking()
  //      - Prompt for confirmationNumber (validate it's 8 digits),
  //        guestName, roomNumber, checkInDate, checkOutDate.
  //      - Return a new entity.Booking(confirmationNumber, guestName,
  //        roomNumber, checkInDate, checkOutDate).
  //
  //   2. inputConfirmationNumber()
  //      - Prompt for and return the 8-digit confirmation number to search
  //        for. Keep validation here so the control layer only deals with a
  //        clean String.
  //
  //   3. displayBooking(Booking booking)
  //      - Print one booking's details, or a "not found" message when null.
  //
  //   4. displayAllBookings(ListInterface<Booking> list)
  //      - Use list.getIterator() to print every booking in the order it was
  //        created (arrival order), numbering each row.
}
