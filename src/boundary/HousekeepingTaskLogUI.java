package boundary;

import adt.ArrayList;
import adt.ListInterface;
import control.ResortData;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatusLog;
import entity.RoomType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
          "Housekeeping Operations",
          "Update Task Status",
          "Rollback",
          "Search & Monitor",
          "Reports"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 5, "go back");
  }

  // Shows Housekeeping Operations: view queue, take next room, or raise a task.
  public int getOperationsMenuChoice() {
    MessageUI.displayMenuScreen("HOUSEKEEPING OPERATIONS", null,
        "Main Menu  >  Housekeeping  >  Housekeeping Operations",
        new String[] {
          "View Cleaning Queue",
          "Take Next Room",
          "Raise New Task for a Room"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  // Shows Update Task Status: original normal cleaning or stayover cleaning.
  public int getUpdateMenuChoice() {
    MessageUI.displayMenuScreen("UPDATE TASK STATUS", null,
        "Main Menu  >  Housekeeping  >  Update Task Status",
        new String[] {
          "Normal Cleaning",
          "Stayover Cleaning"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  // Shows Search & Monitor: original normal search or stayover monitor.
  public int getSearchAndMonitorMenuChoice() {
    MessageUI.displayMenuScreen("SEARCH & MONITOR", null,
        "Main Menu  >  Housekeeping  >  Search & Monitor",
        new String[] {
          "Normal Cleaning",
          "Stayover Cleaning"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
  }

  // Shows the original search-and-monitor menu for task records, filters and the status board.
  public int getSearchMenuChoice() {
    MessageUI.displayMenuScreen("NORMAL CLEANING", null,
        "Main Menu  >  Housekeeping  >  Search & Monitor  >  Normal Cleaning",
        new String[] {
          "View & Search Task Records",
          "Filter tasks by status",
          "Room status board"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 3, "go back");
  }

  // Shows Stayover Cleaning Monitor: Search or Filter by Status.
  public int getStayoverMonitorChoice() {
    MessageUI.displayMenuScreen("STAYOVER CLEANING MONITOR", null,
        "Main Menu  >  Housekeeping  >  Search & Monitor  >  Stayover Cleaning",
        new String[] {
          "Search",
          "Filter by Status"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 2, "go back");
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

  // Asks for a task ID.
  public String inputTaskId() {
    return inputTaskId("Task number");
  }

  // Asks for a task ID using a caller-chosen prompt.
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

  // Asks for a Task ID or a Room ID.
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

  // Asks for a numeric Room ID.
  public String inputRoomNo() {
    while (true) {
      System.out.println("  Room ID (e.g. 1234):");
      System.out.print("  > ");
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

  // Asks for a Stayover search value: date/time or Room ID.
  public String inputStayoverSearch() {
    System.out.print("  Enter Date/Time or Room ID (0 to cancel): ");
    String input = MessageUI.readLine(scanner);
    if (MessageUI.isCancelKey(input)) {
      return null;
    }
    return input.trim();
  }

  // Asks which Stayover status to filter by.
  public String inputStayoverStatusFilter() {
    return inputStatusFromList(new String[] {
      HousekeepingTask.NOT_CLEANED,
      HousekeepingTask.CLEANING_IN_PROGRESS,
      HousekeepingTask.CLEANED
    });
  }

  // Asks whether to continue this Stayover Cleaning to CLEANED.
  public boolean confirmContinueStayoverCleaning() {
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  Continue this Stayover Cleaning?");
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  [1] Yes - Continue to " + HousekeepingTask.CLEANED);
    MessageUI.displayMessage("  [0] Back");
    MessageUI.displayBlankLine();
    return MessageUI.readMenuChoice(scanner, 1, "go back") == 1;
  }

  // Notes that a Stayover search or filter found no records.
  public void displayStayoverNotFound() {
    System.out.println("  [!] No matching Stayover record found.");
  }

  // Asks for a stayover date as dd/MM/yyyy.
  public LocalDate inputStayoverDate() {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    while (true) {
      System.out.print("  Enter date (dd/MM/yyyy, 0 to cancel): ");
      String input = MessageUI.readLine(scanner);
      if (MessageUI.isCancelKey(input)) {
        return null;
      }
      try {
        return LocalDate.parse(input, format);
      } catch (DateTimeParseException badDate) {
        System.out.println("  [!] Invalid date.");
        if (!confirmTryAgain()) {
          return null;
        }
      }
    }
  }

  // Selects a displayed Stayover row by number.
  public HousekeepingTask inputStayoverRecordSelection(ListInterface<HousekeepingTask> records) {
    return selectTaskByDisplayedNo(records);
  }

  // Asks whether to update this Stayover record to its next status.
  public String inputStayoverNextStatus(HousekeepingTask task) {
    String currentStatus = task.getStatus();
    String nextStatus;
    if (HousekeepingTask.NOT_CLEANED.equals(currentStatus)) {
      nextStatus = HousekeepingTask.CLEANING_IN_PROGRESS;
    } else if (HousekeepingTask.CLEANING_IN_PROGRESS.equals(currentStatus)) {
      nextStatus = HousekeepingTask.CLEANED;
    } else {
      displayStayoverAlreadyCompleted();
      return null;
    }

    MessageUI.displayBlankLine();
    MessageUI.displayField("Room", task.getRoomNo());
    MessageUI.displayField("Booking",
        task.getBookingId() == null ? "-" : task.getBookingId());
    MessageUI.displayField("Current Status", currentStatus);
    MessageUI.displayField("Next Status", nextStatus);
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  [1] Update to " + nextStatus);
    MessageUI.displayMessage("  [0] Cancel");
    MessageUI.displayBlankLine();
    return MessageUI.readMenuChoice(scanner, 1, "cancel") == 1 ? nextStatus : null;
  }

  // Asks which status this existing task should move to.
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

  // Next statuses Update Task Status may offer.
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

  // Whether Update Task Status still has a legal next step for this task.
  public boolean hasFurtherUpdateStatus(HousekeepingTask task) {
    if (task == null || HousekeepingTask.READY_FOR_CHECK_IN.equals(task.getStatus())) {
      return false;
    }
    return nextStatusesForUpdate(task.getTaskType(), task.getStatus()).length > 0;
  }

  // Asks whether to keep updating the same task after a successful change.
  public boolean confirmContinueSameTask(HousekeepingTask task) {
    String[] allowed = nextStatusesForUpdate(task.getTaskType(), task.getStatus());
    String target = HousekeepingTask.READY_FOR_CHECK_IN;
    boolean hasReady = false;
    for (int i = 0; i < allowed.length; i++) {
      if (HousekeepingTask.READY_FOR_CHECK_IN.equals(allowed[i])) {
        hasReady = true;
        break;
      }
    }
    if (!hasReady && allowed.length > 0) {
      target = allowed[0];
    }

    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  Continue this cleaning process?");
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  [1] Yes - Continue to " + target);
    MessageUI.displayMessage("  [0] No - Back");
    MessageUI.displayBlankLine();
    return MessageUI.readMenuChoice(scanner, 1, "go back") == 1;
  }

  // Explains what a status change means.
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
    return inputStatusFromList(new String[] {
      HousekeepingTask.DIRTY,
      HousekeepingTask.CLEANING_IN_PROGRESS,
      HousekeepingTask.INSPECTED,
      HousekeepingTask.READY_FOR_CHECK_IN,
      HousekeepingTask.BLOCKED
    });
  }

  // Asks the user to pick a status from a numbered list.
  private String inputStatusFromList(String[] statuses) {
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

  // Displays the selected room.
  public void displaySelectedRoomForNewTask(Room room, ResortData data) {
    RoomType type = data.findRoomType(room.getTypeId());
    String typeName = (type == null) ? room.getTypeId() : type.getTypeName();
    MessageUI.displayBlankLine();
    System.out.println("Room " + room.getRoomNo() + " - " + typeName);
    System.out.println("Current Status    : " + room.getHousekeepingStatus());
    System.out.println("Occupancy Status  : " + room.getOccupancyStatus());
  }

  // Asks which task type to raise for the selected room.
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

  // Asks [1] Try Again / [0] Exit.
  public boolean confirmTryAgain() {
    return confirmOption("Try Again");
  }

  // Asks [1] Select Another Room / [0] Exit.
  public boolean confirmSelectAnotherRoom() {
    return confirmOption("Select Another Room");
  }

  // Asks whether to repeat the same operation after a success.
  public boolean confirmDoAnother(String question) {
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  " + question);
    return confirmOption("Yes");
  }

  // Shows a [1] option / [0] Exit choice.
  public boolean confirmOption(String optionLabel) {
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  [1] " + optionLabel);
    MessageUI.displayMessage("  [0] Exit");
    MessageUI.displayBlankLine();
    return MessageUI.readMenuChoice(scanner, 1, "exit") == 1;
  }

  // Pauses under a caller-chosen wording.
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

  // Clears the console before a major Housekeeping screen.
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

  // Tells the user a DIRTY cleaning task must be started from the queue.
  public void displayMustStartFromQueue() {
    System.out.println("  [!] This task is waiting in the cleaning queue.");
    System.out.println("      Please start the task from the Cleaning Queue first.");
  }

  // Tells the user a finished task cannot be updated again.
  public void displayTaskAlreadyCompleted() {
    System.out.println("  [!] This task is already completed and cannot be updated.");
  }

  // Tells the user this stayover room is already CLEANED.
  public void displayStayoverAlreadyCompleted() {
    System.out.println("  [OK] This Stayover Cleaning is already completed.");
  }

  // Displays a success message on the Housekeeping screen.
  public void displaySuccess(String message) {
    MessageUI.displaySuccess(message);
  }

  // Displays a section heading on the Housekeeping screen.
  public void displaySectionHeading(String title) {
    MessageUI.displaySectionHeading(title);
  }

  // Displays the selected task.
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
      if (task.isStayoverService()) {
        MessageUI.displayField("Booking ID", task.getBookingId());
      } else {
        MessageUI.displayField("Raised by check-out of", task.getBookingId());
      }
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

  // Displays full task details including status history.
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

  // Displays status history for one task.
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

  // Prints SEARCH under the records already on screen.
  public void startSearchSection() {
    MessageUI.displayBlankLine();
    MessageUI.displayThinRule();
    System.out.println("SEARCH");
  }

  // Notes that a searched task or room was found.
  public void displayFound(String message) {
    MessageUI.displayBlankLine();
    System.out.println("  [FOUND] " + message);
  }

  // Returned when the user wants to leave the search browse screen.
  public static final int SEARCH_BROWSE_QUIT = 0;

  // Returned when the user wants to search from the browse screen.
  public static final int SEARCH_BROWSE_SEARCH = -1;

  // Shows one page of housekeeping task records.
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

  // Reads Next / Previous / jump / Search / 0 for the task-record listing.
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

  // Lists the housekeeping tasks already raised for one room.
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

  // Asks the user to pick one task from a numbered room-history list.
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

  // Lists updatable Normal Cleaning tasks with a selection number.
  public void displayNormalCleaningUpdateList(ListInterface<HousekeepingTask> tasks) {
    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format("  %-4s %-7s %-6s %-16s %-7s %s",
        "NO", "TASK", "ROOM", "TASK TYPE", "LANE", "STATUS"));

    for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
      HousekeepingTask task = tasks.getEntry(i);
      System.out.printf("  %-4d %-7s %-6s %-16s %-7s %s%n",
          i, task.getTaskId(), task.getRoomNo(), task.getTaskType(),
          task.getPriority(), task.getStatus());
    }
    MessageUI.displayThinRule();
    MessageUI.displayBlankLine();
  }

  // Selects a displayed Normal Cleaning row by number.
  public HousekeepingTask inputNormalCleaningTaskSelection(ListInterface<HousekeepingTask> tasks) {
    return selectTaskByDisplayedNo(tasks);
  }

  // Asks for a displayed row number and returns that task.
  private HousekeepingTask selectTaskByDisplayedNo(ListInterface<HousekeepingTask> tasks) {
    while (true) {
      System.out.println("  Select No. (0 to cancel):");
      System.out.print("  > ");
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
        // fall through to invalid selection
      }

      System.out.println("  [!] Invalid selection.");
    }
  }

  // Lists tasks as a table, a page at a time.
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

  // Displays the cleaning queue.
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

  // Displays today's Stayover Cleaning list.
  public void displayTodayStayoverCleaning(ListInterface<HousekeepingTask> records,
      LocalDate today) {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    displaySectionHeading("TODAY'S STAYOVER CLEANING");
    MessageUI.displayField("Date", today.format(format));
    if (records.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  No eligible stayover rooms for today.");
      return;
    }
    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format("  %-4s %-8s %-12s %s",
        "NO", "ROOM ID", "BOOKING ID", "STATUS"));
    for (int i = 1; i <= records.getNumberOfEntries(); i++) {
      HousekeepingTask task = records.getEntry(i);
      System.out.printf("  %-4d %-8s %-12s %s%n",
          i, task.getRoomNo(),
          task.getBookingId() == null ? "-" : task.getBookingId(),
          task.getStatus());
    }
    MessageUI.displayThinRule();
  }

  // Displays Stayover Cleaning records for Search & Monitor.
  public void displayStayoverCleaning(ListInterface<HousekeepingTask> records) {
    // No heading or date-filter line: the screen title above already says
    // what this is, and the rows carry their own dates.
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    if (records.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  No stayover cleaning records found.");
      return;
    }
    MessageUI.displayBlankLine();
    MessageUI.displayTableHeading(String.format("  %-4s %-10s %-8s %-12s %s",
        "NO", "DATE", "ROOM ID", "BOOKING ID", "STATUS"));
    for (int i = 1; i <= records.getNumberOfEntries(); i++) {
      HousekeepingTask task = records.getEntry(i);
      String date = task.getCreatedAt() == null
          ? "-" : task.getCreatedAt().toLocalDate().format(format);
      System.out.printf("  %-4d %-10s %-8s %-12s %s%n",
          i, date, task.getRoomNo(),
          task.getBookingId() == null ? "-" : task.getBookingId(),
          task.getStatus());
    }
    MessageUI.displayThinRule();
  }

  // Displays a room's cleaning history.
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

  // Displays every room with occupancy, housekeeping status, and sellability.
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

  // Displays rooms that can take a new housekeeping task.
  public ListInterface<Room> displayRoomsForNewTask(ListInterface<Room> rooms, ResortData data) {
    ListInterface<Room> eligible = new ArrayList<>();
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      HousekeepingTask open = data.findOpenTaskForRoom(room.getRoomNo());
      if ("Can raise".equals(describeRaiseAction(room, open))) {
        eligible.add(room);
      }
    }

    if (eligible.isEmpty()) {
      MessageUI.displayBlankLine();
      MessageUI.displayMessage("  No room can take a new task right now.");
      return eligible;
    }

    MessageUI.displaySectionHeading("Rooms");
    MessageUI.displayTableHeading(String.format("  %-4s %-6s %-18s %s",
        "NO", "ROOM", "ROOM TYPE", "ACTION"));

    for (int i = 1; i <= eligible.getNumberOfEntries(); i++) {
      Room room = eligible.getEntry(i);
      RoomType type = data.findRoomType(room.getTypeId());
      System.out.printf("  %-4d %-6s %-18s %s%n",
          i,
          room.getRoomNo(),
          (type == null) ? room.getTypeId() : type.getTypeName(),
          "Can raise");
    }
    MessageUI.displayThinRule();
    MessageUI.displayBlankLine();
    return eligible;
  }

  // Asks for a displayed room row number and returns that Room ID.
  public String inputRaiseRoomSelection(ListInterface<Room> eligible) {
    while (true) {
      System.out.print("  Room No. (0 to cancel): ");
      String input = MessageUI.readLine(scanner);

      if (MessageUI.isCancelKey(input)) {
        return null;
      }

      try {
        int picked = Integer.parseInt(input);
        if (picked >= 1 && picked <= eligible.getNumberOfEntries()) {
          return eligible.getEntry(picked).getRoomNo();
        }
      } catch (NumberFormatException notANumber) {
        // fall through to invalid selection
      }

      System.out.println("  [!] Invalid selection.");
      if (!confirmTryAgain()) {
        return null;
      }
    }
  }

  // Short note on whether a new task may be raised against a room.
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

  // Draws the morning / afternoon / evening cleaning-time chart.
  public void displayTimePeriodBarChart(String[] names, String[] ranges,
      double[] averages, boolean[] hasData) {
    MessageUI.displayMessage("  Average Cleaning Time (minutes)");

    double highest = 0;
    for (int i = 0; i < averages.length; i++) {
      if (hasData[i] && averages[i] > highest) {
        highest = averages[i];
      }
    }
    if (highest <= 0) {
      MessageUI.displayMessage("  No completed cleaning in these time periods.");
      return;
    }

    int chartHeight = MessageUI.CHART_HEIGHT;
    for (int row = chartHeight; row >= 1; row--) {
      StringBuilder line = new StringBuilder("  |");
      for (int i = 0; i < averages.length; i++) {
        if (!hasData[i]) {
          line.append("       ");
          continue;
        }
        int barHeight = (int) Math.round((averages[i] / highest) * chartHeight);
        line.append(barHeight >= row ? "   #   " : "       ");
      }
      System.out.println(line);
    }

    StringBuilder axis = new StringBuilder("  +");
    for (int i = 0; i < averages.length; i++) {
      axis.append("-------");
    }
    System.out.println(axis);

    System.out.printf("   %-7s%-7s%s%n", "Morn", "Aftn", "Eve");
    MessageUI.displayBlankLine();
    MessageUI.displayMessage("  X-axis:");
    for (int i = 0; i < names.length; i++) {
      String value = hasData[i] ? String.format("%.0f min", averages[i]) : "N/A";
      MessageUI.displayMessage("    " + names[i] + " (" + ranges[i].replace(" ", "")
          + ")  " + value);
    }
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
