package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.HousekeepingTaskLogUI;
import entity.HousekeepingTask;

/**
 *
 * @author Kat Tan
 */
public class HousekeepingTaskLogMaintenance {

  private HousekeepingTaskLogUI housekeepingTaskLogUI = new HousekeepingTaskLogUI();
  private ListInterface<HousekeepingTask> taskLog = new ArrayList<>();

  // TODO: developer to implement Housekeeping Task Log logic
  //
  // REQUIRED ADT: List (adt/ListInterface + adt/ArrayList), already wired up
  // as taskLog above. This module must use the List ADT so it satisfies the
  // assignment's List ADT requirement - do not remove it.
  //
  // Pattern: Sequential log with rollback, built on top of the List ADT.
  //   - Logging a status update -> taskLog.add(task)                      (append to end)
  //   - Undoing the last entry  -> taskLog.remove(taskLog.getNumberOfEntries())
  //                                (always the LAST position, never any other)
  //
  // OPTIONAL ADTs available in the adt package - use only what genuinely
  // helps your design, on top of the required List ADT above:
  //   - adt/ArrayStack (adt/StackInterface): true LIFO (push/pop). Good fit
  //     here since "undo last action" is a classic stack use case - pop()
  //     instead of remove(getNumberOfEntries()). Could replace or sit
  //     alongside taskLog if you want a dedicated undo-history stack.
  //   - adt/ArrayQueue (adt/QueueInterface): true FIFO (enqueue/dequeue).
  //     Not a natural fit for rollback, but could be used if you add a
  //     feature like a pending-tasks queue (e.g. tasks waiting to be
  //     assigned to a housekeeper).
  //   - Any other ADT you build yourself (e.g. a BST) is also fine if a
  //     feature calls for it. The List ADT above must stay as the core
  //     structure regardless of what else you add.
  //
  // Suggested methods to add:
  //   1. logStatusUpdate()
  //      - Get a new HousekeepingTask from
  //        housekeepingTaskLogUI.inputHousekeepingTask().
  //      - taskLog.add(task) to append it as the newest log entry.
  //      - Show a confirmation message via the UI.
  //
  //   2. rollbackLastStatus()
  //      - Add-on method: wraps the "remove the last position" logic into
  //        one clean call so callers never have to compute the index
  //        themselves.
  //      - if (!taskLog.isEmpty()) {
  //          HousekeepingTask removed = taskLog.remove(taskLog.getNumberOfEntries());
  //          ... show removed via the UI ...
  //        } else {
  //          ... show "log is empty, nothing to roll back" ...
  //        }
  //
  //   3. displayLog()
  //      - Pass taskLog to housekeepingTaskLogUI.displayAllTasks(...) so it
  //        can iterate with taskLog.getIterator() and print entries oldest
  //        to newest.
  //
  //   4. runHousekeepingTaskLog()
  //      - The loop below already handles the landing menu and "0. Back"
  //        (returns to TARUMTResortUI). Fill in cases 1-3 to call the
  //        methods above as you implement them.
  public void runHousekeepingTaskLog() {
    int choice = 0;
    do {
      choice = housekeepingTaskLogUI.getMenuChoice();
      switch (choice) {
        case 0:
          break;
        case 1:
        case 2:
        case 3:
          // TODO: developer to call logStatusUpdate() / rollbackLastStatus()
          // / displayLog() here based on choice
          break;
      }
    } while (choice != 0);
  }
}
