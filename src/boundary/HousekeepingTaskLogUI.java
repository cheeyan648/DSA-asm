package boundary;

import java.util.Scanner;
import utility.MessageUI;

/**
 *
 * @author Kat Tan
 */
public class HousekeepingTaskLogUI {

  // Shared with every other UI class - see MessageUI.scanner for why.
  private Scanner scanner = MessageUI.scanner;

  // Landing menu for this module. Only "0. Back" is wired up so far - the
  // developer implementing this module should fill in cases 1-3 in
  // HousekeepingTaskLogMaintenance.runHousekeepingTaskLog() and can extend
  // this menu with more options if needed.
  public int getMenuChoice() {
    MessageUI.clearScreen();
    System.out.println("\nHOUSEKEEPING TASK LOG");
    System.out.println("1. Log new task status update");
    System.out.println("2. Rollback last status update");
    System.out.println("3. Display task log");
    System.out.println("0. Back to main menu");

    // Keeps re-prompting (without clearing) until a valid 0-3 is entered, so
    // the user can see the error message and correct their input.
    return MessageUI.readMenuChoice(scanner, 3, "go back to the main menu");
  }

  // TODO: developer to implement Housekeeping Task Log UI
  //
  // This is a sequential log with rollback: every status update is appended
  // via add(), and a mistaken entry is undone by removing the LAST position.
  // Build the menu/input methods below; leave the log/rollback logic to the
  // Maintenance class.
  //
  // Suggested methods to add (getMenuChoice() above is already done):
  //   1. inputHousekeepingTask()
  //      - Prompt for taskId, roomNumber, status (e.g. "CLEANING",
  //        "INSPECTED", "DONE").
  //      - Return a new entity.HousekeepingTask(taskId, roomNumber, status,
  //        java.time.LocalDateTime.now()).
  //
  //   2. displayTask(HousekeepingTask task)
  //      - Print one log entry, or a "nothing to rollback" message when null
  //        (e.g. rollback attempted on an empty log).
  //
  //   3. displayAllTasks(ListInterface<HousekeepingTask> list)
  //      - Use list.getIterator() to print every entry in the order they
  //        were logged (oldest first), numbering each row.
  //
  //   4. displayMessage(String message)
  //      - Small helper for confirmation/error text, following the style of
  //        utility.MessageUI.
}
