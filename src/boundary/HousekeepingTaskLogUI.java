package boundary;

import adt.ListInterface;
import control.ResortData;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatusLog;
import entity.RoomType;
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

  public int getMenuChoice() {
    MessageUI.displayMenuScreen("HOUSEKEEPING", "T A S K   L O G",
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

  public int getSearchMenuChoice() {
    MessageUI.displayMenuScreen("SEARCH & MONITOR", null,
        "Main Menu  >  Housekeeping  >  Search & Monitor",
        new String[] {
          "Search by task ID",
          "A room's full cleaning history",
          "Filter tasks by status",
          "Room status board",
          "Display the whole task log"
        },
        "Back");
    return MessageUI.readMenuChoice(scanner, 5, "go back");
  }

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

  public String inputTaskId() {
    String id = MessageUI.readRequiredText(scanner, "Task ID (e.g. HK0003)");
    return MessageUI.isCancelled(id) ? null : id;
  }

  public String inputRoomNo() {
    String roomNo = MessageUI.readRequiredText(scanner, "Room number (e.g. 1003)");
    return MessageUI.isCancelled(roomNo) ? null : roomNo;
  }

  /**
   * Asks which status a task should move to.
   *
   * Only the steps the workflow actually permits from where the task is now
   * are offered, so an impossible move cannot be chosen in the first place.
   *
   * @param currentStatus where the task is now
   * @return the chosen status, or null if cancelled or nothing is possible
   */
  public String inputNextStatus(String currentStatus) {
    ListInterface<String> allowed = new adt.ArrayList<>();

    String[] every = {
      HousekeepingTask.DIRTY,
      HousekeepingTask.CLEANING_IN_PROGRESS,
      HousekeepingTask.INSPECTED,
      HousekeepingTask.READY_FOR_CHECK_IN,
      HousekeepingTask.BLOCKED
    };

    for (String candidate : every) {
      if (HousekeepingTask.isValidTransition(currentStatus, candidate)) {
        allowed.add(candidate);
      }
    }

    if (allowed.isEmpty()) {
      MessageUI.displayError("There is no valid next step from " + currentStatus + ".");
      return null;
    }

    String[] choices = new String[allowed.getNumberOfEntries()];
    for (int i = 1; i <= allowed.getNumberOfEntries(); i++) {
      choices[i - 1] = describeStatus(allowed.getEntry(i), currentStatus);
    }

    MessageUI.displayMessage("");
    MessageUI.displayMessage("  The room is currently " + currentStatus + ".");

    String picked = MessageUI.readChoice(scanner, "Next status", choices);
    if (MessageUI.isCancelled(picked)) {
      return null;
    }

    // The label carries an explanation, so the status is taken from the
    // matching entry rather than parsed back out of the text.
    for (int i = 1; i <= allowed.getNumberOfEntries(); i++) {
      if (choices[i - 1].equals(picked)) {
        return allowed.getEntry(i);
      }
    }
    return null;
  }

  /** Explains what a status change means, so the choice is not just a name. */
  private String describeStatus(String status, String from) {
    switch (status) {
      case HousekeepingTask.CLEANING_IN_PROGRESS:
        return status + "  (start cleaning)";
      case HousekeepingTask.INSPECTED:
        return status + "  (cleaning finished, awaiting sign-off)";
      case HousekeepingTask.READY_FOR_CHECK_IN:
        return status + "  (inspection PASSED - room becomes sellable)";
      case HousekeepingTask.DIRTY:
        return HousekeepingTask.INSPECTED.equals(from)
            ? status + "  (inspection FAILED - clean it again)"
            : status + "  (needs cleaning)";
      case HousekeepingTask.BLOCKED:
        return status + "  (cannot proceed - a reason is required)";
      default:
        return status;
    }
  }

  public String inputStatusFilter() {
    String status = MessageUI.readChoice(scanner, "Status", new String[] {
      HousekeepingTask.DIRTY,
      HousekeepingTask.CLEANING_IN_PROGRESS,
      HousekeepingTask.INSPECTED,
      HousekeepingTask.READY_FOR_CHECK_IN,
      HousekeepingTask.BLOCKED
    });
    return MessageUI.isCancelled(status) ? null : status;
  }

  public String inputTaskType() {
    String type = MessageUI.readChoice(scanner, "Task type", new String[] {
      HousekeepingTask.TYPE_CHECKOUT_CLEAN,
      HousekeepingTask.TYPE_STAYOVER_CLEAN,
      HousekeepingTask.TYPE_DEEP_CLEAN,
      HousekeepingTask.TYPE_INSPECTION,
      HousekeepingTask.TYPE_MAINTENANCE
    });
    return MessageUI.isCancelled(type) ? null : type;
  }

  public String inputRemark(boolean required) {
    String remark = required
        ? MessageUI.readRequiredText(scanner, "Reason")
        : MessageUI.readOptionalText(scanner, "Remark");
    return MessageUI.isCancelled(remark) ? null : remark;
  }

  public boolean confirm(String question) {
    return MessageUI.confirm(scanner, question);
  }

  public void pause() {
    MessageUI.pause(scanner);
  }

  // ==================================================================
  // DISPLAY
  // ==================================================================

  public void startAction(String title) {
    MessageUI.startAction(title);
  }

  public void displayMessage(String message) {
    MessageUI.displayMessage(message);
  }

  public void displayError(String message) {
    MessageUI.displayError(message);
  }

  public void displaySuccess(String message) {
    MessageUI.displaySuccess(message);
  }

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
          "TASK", "ROOM", "TYPE", "LANE", "STATUS", "RAISED"));

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
        "POS", "TASK", "ROOM", "LANE", "TYPE", "WAITING ON"));

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

  // ==================================================================
  // REPORTS
  // ==================================================================

  public void displayReportHeader(String title) {
    MessageUI.clearScreen();
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

  public void displayReportLine(String label, String value) {
    MessageUI.displayReportLine(label, value);
  }

  public void displayBarChart(String title, String yAxisLabel, String[] labels,
      double[] values) {
    MessageUI.displayBarChart(title, yAxisLabel, labels, values);
  }

  public void displayTableHeading(String heading) {
    MessageUI.displayTableHeading(heading);
  }

  public void displayThinRule() {
    MessageUI.displayThinRule();
  }

  public void displayReportFooter() {
    MessageUI.displayBlankLine();
    MessageUI.displayRule();
    MessageUI.displayMessage("  END OF REPORT");
    MessageUI.displayRule();
  }
}
