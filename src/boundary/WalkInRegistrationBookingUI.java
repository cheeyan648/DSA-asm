package boundary;

import adt.ListInterface;
import entity.WalkInGuest;
import java.util.Iterator;
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
  // developer implementing this module should fill in cases 1-5 in
  // WalkInRegistrationBookingMaintenance.runWalkInRegistrationBooking() and
  // can extend this menu with more options if needed.
  public int getMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nWALK-IN REGISTRATION & STANDARD BOOKING");
    System.out.println("1. Register walk-in guest (join queue)");
    System.out.println("2. Insert priority walk-in guest (jump queue)");
    System.out.println("3. Serve next guest (remove from front)");
    System.out.println("4. Display queue");
    System.out.println("5. Undo last registration");
    System.out.println("0. Back to main menu");

    // Keeps re-prompting (without clearing) until a valid 0-5 is entered, so
    // the user can see the error message and correct their input.
    return MessageUI.readMenuChoice(scanner, 5, "go back to the main menu");
  }

  /**
   * Prompts for the details of a new walk-in guest.
   *
   * Every prompt re-asks on empty input, and the guest ID / contact number are
   * checked against a basic format so obviously bad data never reaches the
   * queue. Entering "0" at the guest ID prompt abandons the registration and
   * returns null, as does running out of input, so the caller must always
   * null-check the result.
   *
   * @param priority true to register the guest as a priority (queue-jumping)
   * walk-in, false for a normal walk-in joining the back of the line
   * @return the new guest, or null if the user cancelled
   */
  public WalkInGuest inputWalkInGuest(boolean priority) {
    System.out.println(priority
        ? "\nINSERT PRIORITY WALK-IN GUEST"
        : "\nREGISTER WALK-IN GUEST");
    System.out.println("(enter 0 as the guest ID number to cancel)");

    String guestId = inputGuestId();
    if (guestId == null) {
      return null;
    }

    String name = inputName();
    if (name == null) {
      return null;
    }

    String contactNumber = inputContactNumber();
    if (contactNumber == null) {
      return null;
    }

    return new WalkInGuest(guestId, name, contactNumber, priority);
  }

  /**
   * Reads a guest ID, re-prompting until it is non-empty and well formed.
   *
   * The user only types the 4-digit number - the "WG" prefix is added here, so
   * entering 1006 produces WG1006. Typing the prefix anyway still works, so
   * both "1006" and "WG1006" are accepted.
   *
   * @return the full guest ID including the WG prefix, or null if the user
   * entered 0 to cancel
   */
  private String inputGuestId() {
    while (true) {
      System.out.print("Enter guest ID number (e.g. 1006): ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String guestId = scanner.nextLine().trim().toUpperCase();

      if (guestId.isEmpty()) {
        displayMessage("Guest ID cannot be empty!");
        continue;
      }
      if (guestId.equals("0")) {
        return null;
      }

      // Tolerate a typed-out prefix so old-style input still works.
      if (guestId.startsWith("WG")) {
        guestId = guestId.substring(2).trim();
      }

      if (!guestId.matches("\\d{4}")) {
        displayMessage("Invalid guest ID! Enter a 4-digit number, e.g. 1006.");
        continue;
      }

      return "WG" + guestId;
    }
  }

  /**
   * Reads a guest name, re-prompting until it is non-empty.
   *
   * @return the trimmed name
   */
  private String inputName() {
    while (true) {
      System.out.print("Enter guest name           : ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String name = scanner.nextLine().trim();

      if (name.isEmpty()) {
        displayMessage("Name cannot be empty!");
        continue;
      }

      return name;
    }
  }

  /**
   * Reads a contact number, re-prompting until it is non-empty and looks like a
   * Malaysian mobile number (digits, with optional dashes or spaces).
   *
   * @return the trimmed contact number
   */
  private String inputContactNumber() {
    while (true) {
      System.out.print("Enter contact number       : ");
      if (!scanner.hasNextLine()) {
        return null;
      }
      String contactNumber = scanner.nextLine().trim();

      if (contactNumber.isEmpty()) {
        displayMessage("Contact number cannot be empty!");
        continue;
      }
      if (!contactNumber.matches("[0-9][0-9\\- ]{6,14}[0-9]")) {
        displayMessage("Invalid contact number! Use digits only, e.g. 012-3456789.");
        continue;
      }

      return contactNumber;
    }
  }

  /**
   * Prints one guest's details, or a "no guest" notice when there is none (for
   * example after trying to serve from an empty queue).
   *
   * @param guest the guest to display, may be null
   */
  public void displayGuest(WalkInGuest guest) {
    if (guest == null) {
      System.out.println("\nNo guest to display.");
      return;
    }

    // No position column here - a single guest has no queue position to show.
    System.out.println();
    System.out.printf("%-10s %-30s %-15s %-8s%n", "Guest ID", "Name", "Contact", "Type");
    System.out.println("-".repeat(66));
    System.out.println(guest);
  }

  /**
   * Prints the whole queue in service order, numbering each entry so the user
   * can see who is at the front. Walks the list with its iterator rather than
   * indexing, so the display works for any ListInterface implementation.
   *
   * @param list the walk-in queue, position 1 being the front of the line
   */
  public void displayAllGuests(ListInterface<WalkInGuest> list) {
    if (list == null || list.isEmpty()) {
      System.out.println("\nThe walk-in queue is empty. No guests are waiting.");
      return;
    }

    System.out.println("\nWALK-IN QUEUE (position 1 is served next)");
    System.out.println(headerLine());

    Iterator<WalkInGuest> iterator = list.getIterator();
    int position = 1;
    while (iterator.hasNext()) {
      System.out.printf("%-4d%s%n", position, iterator.next());
      position++;
    }

    System.out.println("\nTotal guests waiting: " + list.getNumberOfEntries());
  }

  /**
   * Announces the guest who has just been taken off the front of the queue.
   *
   * @param guest the guest now being served, or null if the queue was empty
   */
  public void displayGuestServedMessage(WalkInGuest guest) {
    if (guest == null) {
      displayMessage("The walk-in queue is empty - there is no one to serve.");
      return;
    }

    System.out.println("\nNOW SERVING");
    displayGuest(guest);
  }

  /**
   * Prints a one-line confirmation or error, in the style of utility.MessageUI.
   *
   * @param message the text to show
   */
  public void displayMessage(String message) {
    System.out.println("\n" + message);
  }

  /**
   * Waits for the user to press Enter. Called after each action so the output
   * stays on screen instead of being wiped by the clearScreen() at the top of
   * the next getMenuChoice().
   */
  public void pause() {
    System.out.print("\nPress Enter to continue...");
    // hasNextLine() guards against input being exhausted (e.g. piped input or
    // Ctrl+D), which would otherwise throw NoSuchElementException here.
    if (scanner.hasNextLine()) {
      scanner.nextLine();
    }
  }

  /**
   * Column headings matching the layout of WalkInGuest.toString().
   */
  private String headerLine() {
    return String.format("%-4s%-10s %-30s %-15s %-8s%n", "No.", "Guest ID", "Name", "Contact", "Type")
        + "-".repeat(70);
  }
}
