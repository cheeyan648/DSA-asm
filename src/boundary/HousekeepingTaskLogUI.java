package boundary;

import adt.ListInterface;
import control.ResortData;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatusLog;
import entity.RoomType;
import java.util.Comparator;
import java.util.Scanner;
import utility.MessageUI;

/**
 * Every screen and prompt for the Housekeeping Task Log.
 *
 * @author Chong Zhi Ying
 */
public class HousekeepingTaskLogUI {

  private final Scanner scanner = MessageUI.scanner;

  // ==================================================================
  // MENUS
  // ==================================================================

  // Shows the Housekeeping main menu and returns the chosen option.
  public int getMenuChoice() {
    MessageUI.displayMenuScreen("HOUSEKEEPING", null,
        "Main Menu  >  Housekeeping Task Log",
        new String[] {
          "Cleaning queue (take the next room)",
          "Update a task's status",
          "Roll back the last status update",
          "Search & monitor",
          "Reports"
        },
        "Back to main menu");
    return MessageUI.readMenuChoice(scanner, 5, "go back to the main menu");
  }

  // Shows the cleaning-queue menu and returns the chosen option.
  public int getQueueMenuChoice() {
    MessageUI.displayMenuScreen("CLEANING QUEUE", null,
        "Main Menu  >  Housekeeping  >  Cleaning Queue",
        new String[] {
          "Take the next room (urgent lane first)",
          "Display the cleaning queue",
          "Raise a new task for a room"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  // Shows the search-and-monitor menu and returns the chosen option.
  public int getSearchMenuChoice() {
    MessageUI.displayMenuScreen("SEARCH & MONITOR", null,
        "Main Menu  >  Housekeeping  >  Search & Monitor",
        new String[] {
          "View & Search Task Records",
          "Filter tasks by status",
          "Room status board"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  // Shows the Housekeeping reports menu and returns the chosen option.
  public int getReportMenuChoice() {
    MessageUI.displayMenuScreen("REPORTS", null,
        "Main Menu  >  Housekeeping  >  Reports",
        new String[] {
          "Cleaning Performance Report",
          "Room & Workload Analysis Report"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  // ==================================================================
  // INPUT
  // ==================================================================

  /**
   * Asks for a task number from a list.
   *
   * The stored ID is still HK plus four digits. The user types the number
   * only; 0 cancels. An invalid number is not retried silently - the caller
   * is given Try Again or Exit.
   *
   * @return the full task ID, or null if cancelled
   */
  public String inputTaskId() {
    return inputTaskId("Task number");
  }

  /**
   * Asks for a task ID using a caller-chosen prompt.
   *
   * @param prompt what to ask for, without the cancel hint
   * @return the full task ID, or null if cancelled
   */
  public String inputTaskId(String prompt) {
    while (true) {
      System.out.print("  " + prompt + " (0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if (MessageUI.isCancelKey(input)) {
        return null;
      }

      String value = input.toUpperCase();
      if (value.startsWith("HK")) {
        value = value.substring(2);
      }

      if (!input.isEmpty() && MessageUI.isAllDigits(value)) {
        try {
          int number = Integer.parseInt(value);
          if (number > 0) {
            return "HK" + String.format("%04d", number);
          }
        } catch (NumberFormatException tooLong) {
          // same as MessageUI.readIdNumber: a number that will not fit
        }
      }

      displayError("Invalid task number.");
      if (!confirmTryAgain()) {
        return null;
      }
    }
  }

  /**
   * Asks for a Task ID (HK0028) or a room number (1007) from the same prompt.
   *
   * 0 cancels. HK plus digits is treated as a Task ID; digits only are treated
   * as a room number. Invalid format is given Try Again or Exit on this
   * screen, without clearing the records already shown.
   *
   * @return "HK0001" for a task ID, a four-digit room number, or null if cancelled
   */
  public String inputTaskIdOrRoomNo() {
    while (true) {
      System.out.print("  Enter Task ID or Room number (0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if (MessageUI.isCancelKey(input)) {
        return null;
      }

      String value = input.toUpperCase();
      if (value.startsWith("HK")) {
        String digits = value.substring(2);
        if (!digits.isEmpty() && MessageUI.isAllDigits(digits)) {
          try {
            int number = Integer.parseInt(digits);
            if (number > 0) {
              return "HK" + String.format("%04d", number);
            }
          } catch (NumberFormatException tooLong) {
            // a number that will not fit in an int
          }
        }
      } else if (!value.isEmpty() && MessageUI.isAllDigits(value)) {
        try {
          int number = Integer.parseInt(value);
          if (number > 0) {
            return String.format("%04d", number);
          }
        } catch (NumberFormatException tooLong) {
          // a number that will not fit in an int
        }
      }

      displayError("Invalid Task ID or Room number.");
      if (!confirmTryAgain()) {
        return null;
      }
      // Stay on this screen so the task records remain visible.
    }
  }

  /**
   * Asks for a room number, or returns null if the user cancels.
   *
   * 0 cancels. An invalid number is given Try Again or Exit rather than
   * being retried silently.
   *
   * @return the four-digit room number, or null if cancelled
   */
  public String inputRoomNo() {
    while (true) {
      System.out.print("  Enter room number (0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if (MessageUI.isCancelKey(input)) {
        return null;
      }

      String value = input.toUpperCase();
      if (!input.isEmpty() && MessageUI.isAllDigits(value)) {
        try {
          int number = Integer.parseInt(value);
          if (number > 0) {
            return String.format("%04d", number);
          }
        } catch (NumberFormatException tooLong) {
          // a number that will not fit in an int
        }
      }

      displayError("Invalid room number.");
      if (!confirmTryAgain()) {
        return null;
      }
    }
  }

  /**
   * Asks which status this existing task should move to.
   *
   * Only the steps this task type may take from where it is now are offered.
   * The Task ID does not change. Inspection is the INSPECTED status of this
   * task, not a new task. A READY_FOR_CHECK_IN task has no next status.
   *
   * @param task the task being updated
   * @return the chosen status, or null if cancelled or nothing is possible
   */
  public String inputNextStatus(HousekeepingTask task) {
    String currentStatus = task.getStatus();
    if (HousekeepingTask.READY_FOR_CHECK_IN.equals(currentStatus)) {
      displayTaskAlreadyCompleted();
      return null;
    }

    String[] allowed = nextStatusesForUpdate(task.getTaskType(), currentStatus);

    if (allowed.length == 0) {
      displayError("Invalid status transition.");
      return null;
    }

    String[] choices = new String[allowed.length];
    for (int i = 0; i < allowed.length; i++) {
      choices[i] = describeStatus(allowed[i], currentStatus, task.getTaskType());
    }

    MessageUI.displayMessage("");
    MessageUI.displayField("Current task type", task.getTaskType());
    MessageUI.displayField("Current status", currentStatus);

    String picked = MessageUI.readChoice(scanner, "Next status", choices);
    if (MessageUI.isCancelled(picked)) {
      return null;
    }

    for (int i = 0; i < allowed.length; i++) {
      if (choices[i].equals(picked)) {
        return allowed[i];
      }
    }
    return null;
  }

  /**
   * Next statuses Update Task Status may offer.
   *
   * CLEANING_IN_PROGRESS is left out: starting a DIRTY task belongs to
   * Take the next room on the cleaning queue.
   */
  private String[] nextStatusesForUpdate(String taskType, String currentStatus) {
    String[] every = HousekeepingTask.allowedNextStatuses(taskType, currentStatus);
    int count = 0;
    for (int i = 0; i < every.length; i++) {
      if (!HousekeepingTask.CLEANING_IN_PROGRESS.equals(every[i])) {
        count++;
      }
    }
    String[] allowed = new String[count];
    int next = 0;
    for (int i = 0; i < every.length; i++) {
      if (!HousekeepingTask.CLEANING_IN_PROGRESS.equals(every[i])) {
        allowed[next] = every[i];
        next++;
      }
    }
    return allowed;
  }

  /** Explains what a status change means, so the choice is not just a name. */
  private String describeStatus(String status, String from, String taskType) {
    if (HousekeepingTask.TYPE_MAINTENANCE.equals(taskType)) {
      if (HousekeepingTask.BLOCKED.equals(status)) {
        return status + "  (BLOCKED / Maintenance in Progress)";
      }
      if (HousekeepingTask.DIRTY.equals(status)) {
        return status + "  (maintenance completed - room remains DIRTY)";
      }
    }
    switch (status) {
      case HousekeepingTask.CLEANING_IN_PROGRESS:
        return status + "  (start cleaning)";
      case HousekeepingTask.INSPECTED:
        return status + "  (this task - cleaning finished, awaiting sign-off)";
      case HousekeepingTask.READY_FOR_CHECK_IN:
        return status + "  (inspection PASSED - room becomes sellable)";
      case HousekeepingTask.DIRTY:
        return HousekeepingTask.INSPECTED.equals(from)
            ? status + "  (inspection FAILED - raise a re-clean through the queue)"
            : status + "  (needs cleaning)";
      case HousekeepingTask.BLOCKED:
        return status + "  (cannot proceed - a reason is required)";
      default:
        return status;
    }
  }

  // Asks which housekeeping status to filter the task list by.
  public String inputStatusFilter() {
    String[] statuses = {
      HousekeepingTask.DIRTY,
      HousekeepingTask.CLEANING_IN_PROGRESS,
      HousekeepingTask.INSPECTED,
      HousekeepingTask.READY_FOR_CHECK_IN,
      HousekeepingTask.BLOCKED
    };

    while (true) {
      MessageUI.displayBlankLine();
      for (int i = 0; i < statuses.length; i++) {
        System.out.printf("    [%d]  %s%n", i + 1, statuses[i]);
      }
      MessageUI.displayBlankLine();
      System.out.print("  Status (1-" + statuses.length + ", 0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if (MessageUI.isCancelKey(input)) {
        return null;
      }

      try {
        int picked = Integer.parseInt(input);
        if (picked >= 1 && picked <= statuses.length) {
          return statuses[picked - 1];
        }
      } catch (NumberFormatException notANumber) {
        // fall through to the invalid-option message
      }

      displayError("Invalid filter option.");
      if (!confirmTryAgain()) {
        return null;
      }
    }
  }

  /**
   * Shows the chosen room before the Raise New Task type menu.
   *
   * @param room the room already selected
   * @param data used to look up the room type name
   */
  public void displaySelectedRoomForNewTask(Room room, ResortData data) {
    RoomType type = data.findRoomType(room.getTypeId());
    String typeName = (type == null) ? room.getTypeId() : type.getTypeName();
    MessageUI.displayBlankLine();
    System.out.println("Room " + room.getRoomNo() + " - " + typeName);
    System.out.println("Current Status: " + room.getHousekeepingStatus());
  }

  /**
   * Asks which task type to raise from the types this room may take.
   *
   * INSPECTION and STAYOVER_CLEAN are never in the list. 0 returns to room
   * selection. Invalid input is given Try Again or Exit rather than being
   * retried silently.
   *
   * @param available the types already filtered for this room
   * @return the chosen type, or null if cancelled or Exit
   */
  public String inputTaskType(String[] available) {
    while (true) {
      displaySectionHeading("AVAILABLE TASK TYPES");
      for (int i = 0; i < available.length; i++) {
        System.out.printf("  [%d] %s%n", i + 1, available[i]);
      }
      System.out.println("  [0] Cancel");
      MessageUI.displayBlankLine();
      System.out.print("  Task type: ");
      String input = MessageUI.readLine(scanner);

      if (MessageUI.isCancelKey(input)) {
        return null;
      }

      try {
        int picked = Integer.parseInt(input);
        if (picked >= 1 && picked <= available.length) {
          return available[picked - 1];
        }
      } catch (NumberFormatException notANumber) {
        // fall through
      }

      displayError("Invalid Task Type.");
      if (!confirmTryAgain()) {
        return null;
      }
    }
  }

  // Asks for a remark, required when blocking a room or failing an inspection.
  public String inputRemark(boolean required) {
    String remark = required
        ? MessageUI.readRequiredText(scanner, "Reason")
        : MessageUI.readOptionalText(scanner, "Remark");
    return MessageUI.isCancelled(remark) ? null : remark;
  }

  // Asks a yes/no confirmation question.
  public boolean confirm(String question) {
    return MessageUI.confirm(scanner, question);
  }

  /**
   * Asks [1] Try Again / [0] Exit after invalid input or a not-found result.
   *
   * @return true to retry the same input
   */
  public boolean confirmTryAgain() {
    return confirmOption("Try Again");
  }

  /**
   * Asks [1] Select Another Room / [0] Exit when a room cannot take a new task.
   *
   * @return true to pick a different room
   */
  public boolean confirmSelectAnotherRoom() {
    return confirmOption("Select Another Room");
  }

  /**
   * Asks whether to repeat the same operation after a success.
   *
   * @param question e.g. "Do you want to raise another task?"
   * @return true if the user chose Yes
   */
  public boolean confirmDoAnother(String question) {
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  " + question);
    return confirmOption("Yes");
  }

  /**
   * Shows a [1] option / [0] Exit choice and returns true if 1 was chosen.
   *
   * @param optionLabel the text shown next to [1]
   * @return true if the user chose 1
   */
  public boolean confirmOption(String optionLabel) {
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  [1] " + optionLabel);
    MessageUI.displayMessage("  [0] Exit");
    MessageUI.displayBlankLine();
    return MessageUI.readMenuChoice(scanner, 1, "exit") == 1;
  }

  /**
   * Pauses under a caller-chosen wording.
   *
   * @param prompt what to tell the user, without its trailing dots
   */
  public void pause(String prompt) {
    MessageUI.pause(scanner, prompt);
  }

  // Waits for the user to press Enter before leaving the current screen.
  public void pause() {
    MessageUI.pause(scanner);
  }

  // ==================================================================
  // DISPLAY
  // ==================================================================

  /**
   * Clears the console before a major Housekeeping screen.
   *
   * Reuses the shared helper, then scrolls and clears once more so a terminal
   * that ignores ANSI codes does not leave the previous page on screen.
   */
  public void clearScreen() {
    MessageUI.clearScreen();
    for (int i = 0; i < 40; i++) {
      System.out.println();
    }
    MessageUI.clearScreen();
  }

  // Starts a Housekeeping action screen with the given title.
  public void startAction(String title) {
    clearScreen();
    MessageUI.startAction(title);
  }

  // Displays a plain message on the Housekeeping screen.
  public void displayMessage(String message) {
    MessageUI.displayMessage(message);
  }

  // Displays an error message on the Housekeeping screen.
  public void displayError(String message) {
    System.out.println("  [ERROR] " + message);
  }

  /**
   * Tells the user a DIRTY cleaning task must be started from the queue.
   */
  public void displayMustStartFromQueue() {
    System.out.println("  [!] This task is waiting in the cleaning queue.");
    System.out.println("      Please start the task from the Cleaning Queue first.");
  }

  /**
   * Tells the user a finished task cannot be updated again.
   */
  public void displayTaskAlreadyCompleted() {
    System.out.println("  [!] This task is already completed and cannot be updated.");
  }

  // Displays a success message on the Housekeeping screen.
  public void displaySuccess(String message) {
    MessageUI.displaySuccess(message);
  }

  // Displays a section heading on the Housekeeping screen.
  public void displaySectionHeading(String title) {
    MessageUI.displaySectionHeading(title);
  }

  /**
   * Shows one task in full.
   *
   * @param task the task
   * @param data used to look up the room
   */
  public void displayTask(HousekeepingTask task, ResortData data) {
    Room room = data.findRoom(task.getRoomNo());
    RoomType type = (room == null) ? null : data.findRoomType(room.getTypeId());

    MessageUI.displayBlankLine();
    MessageUI.displayField("Task ID", task.getTaskId());
    MessageUI.displayField("Room", task.getRoomNo()
        + (type == null ? "" : "  (" + type.getTypeName() + ")"));
    MessageUI.displayField("Task type", task.getTaskType());
    MessageUI.displayField("Status", task.getStatus());
    MessageUI.displayField("Priority", task.getPriority());

    if (task.getReservedForBookingId() != null) {
      MessageUI.displayField("Reserved for booking", task.getReservedForBookingId()
          + "  (this is what sets the lane)");
    }
    if (task.getBookingId() != null) {
      MessageUI.displayField("Raised by check-out of", task.getBookingId());
    }

    MessageUI.displayField("Assigned to",
        task.getAssignedTo() == null ? "not yet assigned" : task.getAssignedTo());
    MessageUI.displayField("Raised", String.valueOf(task.getCreatedAt()));

    if (task.getStartedAt() != null) {
      MessageUI.displayField("Started", String.valueOf(task.getStartedAt()));
    }
    if (task.getCompletedAt() != null) {
      MessageUI.displayField("Completed", String.valueOf(task.getCompletedAt()));
      MessageUI.displayField("Took", task.getCleaningDurationMinutes() + " min");
    }
    if (task.getInspectionFailCount() > 0) {
      MessageUI.displayField("Failed inspections",
          String.valueOf(task.getInspectionFailCount()));
    }
    if (task.getRemark() != null && !task.getRemark().isBlank()) {
      MessageUI.displayField("Remark", task.getRemark());
    }
  }

  /**
   * Shows one task as a labelled details block for View & Search.
   *
   * Existing HousekeepingTask fields are printed first. The status-log rows
   * already stored for this Task ID are then listed underneath.
   *
   * @param task the task to show
   * @param data used to look up the room type and status history
   */
  public void displayTaskDetails(HousekeepingTask task, ResortData data) {
    Room room = data.findRoom(task.getRoomNo());
    RoomType type = (room == null) ? null : data.findRoomType(room.getTypeId());

    displaySectionHeading("TASK DETAILS");
    MessageUI.displayField("Task ID", task.getTaskId());
    MessageUI.displayField("Room", task.getRoomNo());
    MessageUI.displayField("Room Type", type == null ? "-" : type.getTypeName());
    MessageUI.displayField("Task Type", task.getTaskType());
    MessageUI.displayField("Priority", task.getPriority());
    MessageUI.displayField("Status", task.getStatus());
    MessageUI.displayField("Assigned To",
        task.getAssignedTo() == null ? "-" : task.getAssignedTo());
    MessageUI.displayField("Raised By",
        task.getBookingId() == null ? "-" : task.getBookingId());
    MessageUI.displayField("Raised", String.valueOf(task.getCreatedAt()));
    MessageUI.displayField("Started",
        task.getStartedAt() == null ? "-" : String.valueOf(task.getStartedAt()));
    MessageUI.displayField("Completed",
        task.getCompletedAt() == null ? "-" : String.valueOf(task.getCompletedAt()));
    if (task.getReservedForBookingId() != null) {
      MessageUI.displayField("Reserved for booking", task.getReservedForBookingId());
    }
    if (task.getInspectionFailCount() > 0) {
      MessageUI.displayField("Failed inspections",
          String.valueOf(task.getInspectionFailCount()));
    }
    if (task.getRemark() != null && !task.getRemark().isBlank()) {
      MessageUI.displayField("Remark", task.getRemark());
    }
    MessageUI.displayThinRule();
    displayTaskStatusHistory(task.getTaskId(), data);
  }

  /**
   * Lists the existing RoomStatusLog rows for one Task ID, oldest first.
   *
   * Uses a filtered copy of the shared status-log List so stored history is
   * not changed. A missing FROM status is shown as "-".
   */
  private void displayTaskStatusHistory(String taskId, ResortData data) {
    ListInterface<RoomStatusLog> logs = data.getStatusLogList().filter(
        log -> taskId.equals(log.getTaskId()));
    logs.sort(Comparator.comparing(RoomStatusLog::getChangedAt,
        Comparator.nullsLast(Comparator.naturalOrder())));

    displaySectionHeading("STATUS HISTORY");
    if (logs.isEmpty()) {
      MessageUI.displayMessage("  No status history available.");
      return;
    }

    MessageUI.displayTableHeading(String.format("  %-7s %-24s %s",
        "LOG", "FROM", "TO"));
    for (int i = 1; i <= logs.getNumberOfEntries(); i++) {
      RoomStatusLog log = logs.getEntry(i);
      System.out.printf("  %-7s %-24s %s%n",
          log.getLogId(),
          log.getFromStatus() == null ? "-" : log.getFromStatus(),
          log.getToStatus() == null ? "-" : log.getToStatus());
    }
    MessageUI.displayThinRule();
  }

  /**
   * Prints SEARCH under the records already on screen.
   *
   * Does not clear the screen. The current page stays visible while the
   * user types a Task ID or room number.
   */
  public void startSearchSection() {
    MessageUI.displayBlankLine();
    MessageUI.displayThinRule();
    System.out.println("SEARCH");
  }

  /** Notes that a searched task or room was found on the page just shown. */
  public void displayFound(String message) {
    MessageUI.displayBlankLine();
    System.out.println("  [FOUND] " + message);
  }

  /** Returned by readSearchBrowseCommand when the user wants to leave. */
  public static final int SEARCH_BROWSE_QUIT = 0;

  /** Returned by readSearchBrowseCommand when the user wants to search. */
  public static final int SEARCH_BROWSE_SEARCH = -1;

  /**
   * Shows one page of housekeeping task records from the shared List ADT.
   *
   * @param tasks every housekeeping task
   * @param page the page to show, counting from 1
   */
  public void displaySearchRecordsPage(ListInterface<HousekeepingTask> tasks, int page) {
    int total = tasks.getNumberOfEntries();
    int from = MessageUI.firstRowOnPage(page);
    int to = MessageUI.lastRowOnPage(page, total);
    int totalPages = MessageUI.pageCount(total);

    displaySectionHeading("HOUSEKEEPING TASK RECORDS");
    MessageUI.displayTableHeading(String.format("  %-4s %-7s %-6s %-16s %-7s %s",
        "NO", "TASK", "ROOM", "TASK TYPE", "LANE", "STATUS"));

    for (int i = from; i <= to; i++) {
      HousekeepingTask task = tasks.getEntry(i);
      System.out.printf("  %-4d %-7s %-6s %-16s %-7s %s%n",
          i, task.getTaskId(), task.getRoomNo(), task.getTaskType(),
          task.getPriority(), task.getStatus());
    }
    MessageUI.displayThinRule();
    System.out.printf("  %d task(s).  Page %d of %d.%n", total, page, totalPages);
  }

  /**
   * Reads Next / Previous / jump / Search / 0 for the task-record listing.
   *
   * A page number shows that page and returns it so the listing can be
   * redrawn. Search is a separate command so it is never mixed with jumping.
   *
   * @param page the page currently on screen
   * @param totalPages how many pages there are
   * @return the page to show, SEARCH_BROWSE_SEARCH, or SEARCH_BROWSE_QUIT
   */
  public int readSearchBrowseCommand(int page, int totalPages) {
    MessageUI.displayThinRule();
    while (true) {
      System.out.printf("  Page %d of %d%n", page, totalPages);
      System.out.printf("  [N]ext, [P]revious, [1-%d] Jump, [S]earch, 0 to go back: ",
          totalPages);
      String input = MessageUI.readLine(scanner, "0").trim();
      String lowered = input.toLowerCase();

      if (MessageUI.isCancelKey(input) || lowered.equals("q") || lowered.equals("quit")) {
        return SEARCH_BROWSE_QUIT;
      }

      if (lowered.equals("s") || lowered.equals("search")) {
        return SEARCH_BROWSE_SEARCH;
      }

      if (input.isEmpty() || lowered.equals("n") || lowered.equals("next")) {
        if (page >= totalPages) {
          displayError("Already on the last page.");
          continue;
        }
        return page + 1;
      }

      if (lowered.equals("p") || lowered.equals("prev") || lowered.equals("previous")) {
        if (page <= 1) {
          displayError("Already on the first page.");
          continue;
        }
        return page - 1;
      }

      if (MessageUI.isAllDigits(input)) {
        try {
          int wanted = Integer.parseInt(input);
          if (wanted >= 1 && wanted <= totalPages) {
            return wanted;
          }
          displayError("Invalid page number.");
          if (!confirmTryAgain()) {
            return SEARCH_BROWSE_QUIT;
          }
          return page;
        } catch (NumberFormatException tooLong) {
          displayError("Invalid page number.");
          if (!confirmTryAgain()) {
            return SEARCH_BROWSE_QUIT;
          }
          return page;
        }
      }

      displayError("Invalid choice.");
      if (!confirmTryAgain()) {
        return SEARCH_BROWSE_QUIT;
      }
      return page;
    }
  }

  /**
   * Lists the housekeeping tasks already raised for one room.
   *
   * @param tasks the tasks for that room
   * @param roomNo which room
   */
  public void displayRoomTaskHistory(ListInterface<HousekeepingTask> tasks, String roomNo) {
    displaySectionHeading("ROOM " + roomNo + " - HOUSEKEEPING HISTORY");
    MessageUI.displayTableHeading(String.format("  %-4s %-7s %-16s %s",
        "NO", "TASK", "TASK TYPE", "STATUS"));

    for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
      HousekeepingTask task = tasks.getEntry(i);
      System.out.printf("  %-4d %-7s %-16s %s%n",
          i, task.getTaskId(), task.getTaskType(), task.getStatus());
    }
    MessageUI.displayThinRule();
  }

  /**
   * Asks the user to pick one task from a numbered room-history list.
   *
   * A single match is returned without asking. 0 cancels. After an invalid
   * choice the history is redrawn on a clean screen.
   *
   * @param tasks the tasks shown on screen
   * @param roomNo the room whose history is being shown
   * @return the chosen task, or null if cancelled
   */
  public HousekeepingTask selectTaskFromList(ListInterface<HousekeepingTask> tasks,
      String roomNo) {
    if (tasks.getNumberOfEntries() == 1) {
      return tasks.getEntry(1);
    }

    while (true) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  Select a task for details:");
      for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
        System.out.printf("  [%d] %s%n", i, tasks.getEntry(i).getTaskId());
      }
      MessageUI.displayMessage("  [0] Exit");
      MessageUI.displayBlankLine();

      System.out.print("  Choice: ");
      String input = MessageUI.readLine(scanner);
      if (MessageUI.isCancelKey(input)) {
        return null;
      }

      try {
        int picked = Integer.parseInt(input);
        if (picked >= 1 && picked <= tasks.getNumberOfEntries()) {
          return tasks.getEntry(picked);
        }
      } catch (NumberFormatException notANumber) {
        // fall through
      }

      displayError("Invalid choice.");
      if (!confirmTryAgain()) {
        return null;
      }
      startAction("VIEW & SEARCH TASK RECORDS");
      displayRoomTaskHistory(tasks, roomNo);
    }
  }

  /**
   * Lists tasks as a table, a page at a time.
   *
   * @param tasks the tasks to show
   * @param emptyMessage what to say when there is nothing to show
   * @return true if anything was shown
   */
  public boolean displayTaskList(ListInterface<HousekeepingTask> tasks,
      String emptyMessage) {
    if (tasks.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  " + emptyMessage);
      return false;
    }

    int totalPages = MessageUI.pageCount(tasks.getNumberOfEntries());
    int shown = 0;

    for (int page = 1; page <= totalPages; page++) {
      MessageUI.displayBlankLine();
      MessageUI.displayTableHeading(String.format("  %-7s %-6s %-16s %-7s %-22s %s",
          "TASK", "ROOM", "TASK TYPE", "LANE", "STATUS", "RAISED"));

      int upTo = Math.min(shown + MessageUI.PAGE_SIZE, tasks.getNumberOfEntries());
      for (int i = shown + 1; i <= upTo; i++) {
        HousekeepingTask task = tasks.getEntry(i);

        System.out.printf("  %-7s %-6s %-16s %-7s %-22s %s%n",
            task.getTaskId(), task.getRoomNo(), task.getTaskType(),
            task.getPriority(), task.getStatus(),
            task.getCreatedAt().toLocalDate());
      }
      shown = upTo;

      MessageUI.displayThinRule();
      System.out.printf("  %d task(s).%n", tasks.getNumberOfEntries());

      if (!MessageUI.askForNextPage(scanner, page, totalPages)) {
        break;
      }
    }
    return true;
  }

  /**
   * Shows the cleaning queue in the order rooms will actually be taken.
   *
   * @param serviceOrder the waiting tasks, urgent lane first
   * @param data used to look up each room
   * @param urgentCount how many are in the urgent lane
   * @param normalCount how many are in the normal lane
   * @return true if anything is waiting
   */
  public boolean displayQueue(ListInterface<HousekeepingTask> serviceOrder,
      ResortData data, int urgentCount, int normalCount) {
    if (serviceOrder.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  The cleaning queue is empty. No room is waiting.");
      return false;
    }

    MessageUI.displayBlankLine();
    System.out.printf("  Urgent: %d     Normal: %d     Total: %d%n",
        urgentCount, normalCount, serviceOrder.getNumberOfEntries());
    MessageUI.displayBlankLine();

    MessageUI.displayTableHeading(String.format("  %-4s %-7s %-6s %-7s %-16s %s",
        "POS", "TASK", "ROOM", "LANE", "TASK TYPE", "WAITING ON"));

    for (int i = 1; i <= serviceOrder.getNumberOfEntries(); i++) {
      HousekeepingTask task = serviceOrder.getEntry(i);

      System.out.printf("  %-4d %-7s %-6s %-7s %-16s %s%n",
          i, task.getTaskId(), task.getRoomNo(), task.getPriority(),
          task.getTaskType(),
          task.getReservedForBookingId() == null ? "-" : task.getReservedForBookingId());
    }
    MessageUI.displayThinRule();
    MessageUI.displayMessage("  Position 1 is taken next. The urgent lane is drained first.");
    return true;
  }

  /**
   * Shows a room's cleaning history, oldest first.
   *
   * @param logs the status changes for that room
   * @param roomNo which room
   * @param currentStatus what the room is now
   */
  public void displayRoomHistory(ListInterface<RoomStatusLog> logs, String roomNo,
      String currentStatus) {
    MessageUI.displaySectionHeading("Room " + roomNo + " - currently " + currentStatus);

    if (logs.isEmpty()) {
      MessageUI.displayMessage("  This room has no recorded history.");
      return;
    }

    MessageUI.displayTableHeading(String.format("  %-7s %-7s %-22s %-22s %-7s %s",
        "LOG", "TASK", "FROM", "TO", "BY", "WHEN"));

    for (int i = 1; i <= logs.getNumberOfEntries(); i++) {
      RoomStatusLog log = logs.getEntry(i);

      System.out.printf("  %-7s %-7s %-22s %-22s %-7s %s%s%n",
          log.getLogId(), log.getTaskId(),
          log.getFromStatus() == null ? "-" : log.getFromStatus(),
          log.getToStatus(),
          log.getChangedBy() == null ? "-" : log.getChangedBy(),
          log.getChangedAt().toLocalDate(),
          log.isRollback() ? "  (rollback)" : "");
    }
    MessageUI.displayThinRule();
    MessageUI.displayMessage("  " + logs.getNumberOfEntries() + " status change(s).");
  }

  /**
   * Shows every room with both statuses and whether it is sellable.
   *
   * @param rooms the rooms
   * @param data used to look up each room's type and open task
   */
  public void displayRoomBoard(ListInterface<Room> rooms, ResortData data) {
    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format("  %-6s %-6s %-10s %-22s %-7s %s",
        "ROOM", "TYPE", "OCCUPANCY", "HOUSEKEEPING", "LANE", "SELLABLE?"));

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      HousekeepingTask open = data.findOpenTaskForRoom(room.getRoomNo());

      String lane = (open == null) ? "-" : open.getPriority();
      String sellable;
      if (room.isOutOfService()) {
        sellable = "No (out of service)";
      } else if (!Room.VACANT.equals(room.getOccupancyStatus())) {
        sellable = "No (in use)";
      } else if (!Room.READY_FOR_CHECK_IN.equals(room.getHousekeepingStatus())) {
        sellable = "No (not cleaned)";
      } else {
        sellable = "Yes";
      }

      System.out.printf("  %-6s %-6s %-10s %-22s %-7s %s%n",
          room.getRoomNo(), room.getTypeId(), room.getOccupancyStatus(),
          room.getHousekeepingStatus(), lane, sellable);
    }
    MessageUI.displayThinRule();
  }

  /**
   * Lists the rooms before a task number is asked for.
   *
   * The action column comes from the same open-task lookup the raise
   * validation uses, so a room shown as able to take a task is one the
   * validation will actually accept.
   *
   * @param rooms the rooms, taken from the shared room list
   * @param data used to look up each room's type and open task
   */
  public void displayRoomsForNewTask(ListInterface<Room> rooms, ResortData data) {
    if (rooms.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  There are no rooms on file.");
      return;
    }

    MessageUI.displaySectionHeading("Rooms");
    MessageUI.displayTableHeading(String.format("  %-6s %-18s %-22s %s",
        "ROOM", "ROOM TYPE", "STATUS", "ACTION"));

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      RoomType type = data.findRoomType(room.getTypeId());
      HousekeepingTask open = data.findOpenTaskForRoom(room.getRoomNo());

      System.out.printf("  %-6s %-18s %-22s %s%n",
          room.getRoomNo(),
          (type == null) ? room.getTypeId() : type.getTypeName(),
          room.getHousekeepingStatus(),
          describeRaiseAction(room, open));
    }
    MessageUI.displayThinRule();
    MessageUI.displayMessage("  A room that already has an open task cannot take another one.");
    MessageUI.displayBlankLine();
  }

  /** Short note on whether a new task may be raised against a room. */
  private String describeRaiseAction(Room room, HousekeepingTask open) {
    if (open != null && open.isMaintenanceType()) {
      return "Maintenance";
    }
    if (open != null) {
      return "Has open task (" + open.getTaskId() + ")";
    }
    if (room.isOutOfService() || Room.BLOCKED.equals(room.getHousekeepingStatus())) {
      return "Blocked";
    }
    return "Can raise";
  }

  // ==================================================================
  // REPORTS
  // ==================================================================

  // Displays the boxed header at the top of a Housekeeping report.
  public void displayReportHeader(String title) {
    MessageUI.beginLongOutput();
    MessageUI.displayBlankLine();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("TARUMT RESORT MANAGEMENT SYSTEM");
    MessageUI.displayBoxCentred(title);
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  Generated: " + java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")));
    MessageUI.displayBoxBottom();
  }

  // Displays one labelled value on a Housekeeping report.
  public void displayReportLine(String label, String value) {
    MessageUI.displayReportLine(label, value);
  }

  // Draws a text bar chart for a Housekeeping report.
  public void displayBarChart(String title, String yAxisLabel, String[] labels,
      double[] values) {
    MessageUI.displayBarChart(title, yAxisLabel, labels, values);
  }

  // Displays a table heading line on a Housekeeping report.
  public void displayTableHeading(String heading) {
    MessageUI.displayTableHeading(heading);
  }

  // Draws a thin divider line on a Housekeeping report.
  public void displayThinRule() {
    MessageUI.displayThinRule();
  }

  // Displays the end of a Housekeeping report and waits for the user to leave.
  public void displayReportFooter() {
    MessageUI.displayBlankLine();
    MessageUI.displayRule();
    MessageUI.displayMessage("  END OF REPORT");
    MessageUI.displayRule();
    MessageUI.endLongOutput(scanner);
  }
}
