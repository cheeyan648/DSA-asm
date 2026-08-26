package control;

import adt.ListInterface;
import boundary.HousekeepingTaskLogUI;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatusLog;
import entity.RoomType;
import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Housekeeping Task Log - whether a room may be given to anybody.
 *
 * This module is the gatekeeper of room readiness. The front desk can find a
 * room that is free for the dates, but only housekeeping can say it is clean,
 * and a room is sold only when both agree. That division is what stops the
 * system doing the thing the separate modules could: letting a guest into a
 * room somebody has just checked out of.
 *
 * The log is append-only. A rollback adds a compensating row rather than
 * deleting one, because the reports count inspection failures and re-cleaning
 * from this history - removing rows would quietly rewrite figures that have
 * already been reported.
 *
 * @author Chong Zhi Ying
 */
public class HousekeepingTaskLogMaintenance {

  private final HousekeepingTaskLogUI ui = new HousekeepingTaskLogUI();
  private final ResortData data;
  private final ResortService service;
  private final String staffId;

  public HousekeepingTaskLogMaintenance(ResortService service, String staffId) {
    this.service = service;
    this.data = service.getData();
    this.staffId = staffId;
  }

  // ==================================================================
  // ROOM MANAGEMENT
  //
  // Housekeeping owns the rooms themselves, not just the cleaning of them.
  // It is the module that knows whether a room is fit to be sold, so adding
  // one, retiring one and taking one out of service belong here rather than
  // at the desk that is trying to sell it.
  // ==================================================================

  private void runRoomMenu() {
    int choice;
    do {
      choice = ui.getRoomMenuChoice();
      switch (choice) {
        case 1:
          displayRoomList();
          break;
        case 2:
          addRoom();
          break;
        case 3:
          removeRoom();
          break;
        case 4:
          changeServiceState(true);
          break;
        case 5:
          changeServiceState(false);
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  /** Shows every room, and whether each may currently be sold. */
  private void displayRoomList() {
    ui.startAction("ALL ROOMS");
    ui.displayRoomList(data.getRoomList(), data);
    ui.pause();
  }

  /**
   * Adds a room to the resort.
   *
   * The new room is not sellable straight away - it goes onto the cleaning
   * queue to be prepared first, which is the same route every dirty room
   * takes. That is what stops a room being sold the minute it is keyed in.
   */
  private void addRoom() {
    ui.startAction("ADD A ROOM");

    String roomNo = ui.inputNewRoomNo();
    if (roomNo == null) {
      return;
    }

    if (data.findRoom(roomNo) != null) {
      ui.displayError("Room " + roomNo + " already exists.");
      ui.pause();
      return;
    }

    String typeId = ui.inputRoomType(data.getRoomTypeList());
    if (typeId == null) {
      return;
    }

    int floor = ui.inputFloorNumber();
    if (floor < 0) {
      return;
    }

    ServiceResult<Room> added = service.addRoom(roomNo, typeId, floor, staffId);
    if (added.isFailure()) {
      ui.displayError(added.getMessage());
      ui.pause();
      return;
    }

    ui.displaySuccess(added.getMessage());
    ui.displayMessage("  It cannot be sold until it has been cleaned and inspected.");
    ui.pause();
  }

  /**
   * Retires a room.
   *
   * Refused while a guest is in it or a live booking is holding it, because
   * removing the room would leave that booking pointing at a room number that
   * no longer exists.
   */
  private void removeRoom() {
    ui.startAction("REMOVE A ROOM");

    if (!ui.displayRoomList(data.getRoomList(), data)) {
      ui.pause();
      return;
    }

    ui.displayMessage("");
    String roomNo = ui.inputRoomNo();
    if (roomNo == null) {
      return;
    }

    Room room = data.findRoom(roomNo);
    if (room == null) {
      ui.displayError("There is no room " + roomNo + ".");
      ui.pause();
      return;
    }

    ui.displayRoom(room, data);
    ui.displayMessage("");

    if (!ui.confirm("Remove room " + roomNo + " from the resort?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    ServiceResult<Room> removed = service.removeRoom(roomNo);
    if (removed.isFailure()) {
      ui.displayError(removed.getMessage());
    } else {
      ui.displaySuccess(removed.getMessage());
    }
    ui.pause();
  }

  /**
   * Takes a room out of service, or puts it back.
   *
   * @param block true to take it out of service, false to return it
   */
  private void changeServiceState(boolean block) {
    ui.startAction(block ? "TAKE A ROOM OUT OF SERVICE" : "RETURN A ROOM TO SERVICE");

    if (!ui.displayRoomList(data.getRoomList(), data)) {
      ui.pause();
      return;
    }

    ui.displayMessage("");
    String roomNo = ui.inputRoomNo();
    if (roomNo == null) {
      return;
    }

    ServiceResult<Room> changed = service.setRoomOutOfService(roomNo, block, staffId);
    if (changed.isFailure()) {
      ui.displayError(changed.getMessage());
    } else {
      ui.displaySuccess(changed.getMessage());
      if (!block) {
        ui.displayMessage("  It cannot be sold until it has been cleaned again.");
      }
    }
    ui.pause();
  }

  // ==================================================================
  // MENU
  // ==================================================================

  public void run() {
    int choice;
    do {
      choice = ui.getMenuChoice();
      switch (choice) {
        case 1:
          runQueueMenu();
          break;
        case 2:
          updateTaskStatus();
          break;
        case 3:
          rollbackLastUpdate();
          break;
        case 4:
          runRoomMenu();
          break;
        case 5:
          runSearchMenu();
          break;
        case 6:
          runReportMenu();
          break;
        default:
          break;
      }
    } while (choice != 0);

    data.saveHousekeeping();
  }

  private void runQueueMenu() {
    int choice;
    do {
      choice = ui.getQueueMenuChoice();
      switch (choice) {
        case 1:
          takeNextRoom();
          break;
        case 2:
          displayQueue();
          ui.pause();
          break;
        case 3:
          raiseNewTask();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runSearchMenu() {
    int choice;
    do {
      choice = ui.getSearchMenuChoice();
      switch (choice) {
        case 1:
          searchByTaskId();
          break;
        case 2:
          displayRoomHistory();
          break;
        case 3:
          filterByStatus();
          break;
        case 4:
          displayRoomBoard();
          break;
        case 5:
          displayWholeLog();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  private void runReportMenu() {
    int choice;
    do {
      choice = ui.getReportMenuChoice();
      switch (choice) {
        case 1:
          cleaningPerformanceReport();
          break;
        case 2:
          workloadAnalysisReport();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  // ==================================================================
  // CLEANING QUEUE
  // ==================================================================

  /**
   * Takes the next room off the queue and starts cleaning it.
   *
   * The urgent lane is emptied before the normal one, so a room somebody is
   * waiting on is never left behind routine work - however long that routine
   * work has been queued.
   */
  private void takeNextRoom() {
    ui.startAction("TAKE THE NEXT ROOM");

    HousekeepingTask next = data.getCleaningQueue().peekNext();
    if (next == null) {
      ui.displayError("The cleaning queue is empty.");
      ui.pause();
      return;
    }

    ui.displayMessage("  Next room to clean:");
    ui.displayTask(next, data);

    if (next.isUrgent() && next.getReservedForBookingId() != null) {
      ui.displayMessage("");
      ui.displayMessage("  This room is in the URGENT lane because booking "
          + next.getReservedForBookingId() + " is waiting on it.");
    }
    ui.displayMessage("");

    if (!ui.confirm("Start cleaning this room?")) {
      ui.displayMessage("  Cancelled - the room is still in the queue.");
      ui.pause();
      return;
    }

    ServiceResult<HousekeepingTask> result = service.updateTaskStatus(
        next.getTaskId(), HousekeepingTask.CLEANING_IN_PROGRESS, staffId, null);

    if (result.isSuccess()) {
      ui.displaySuccess(result.getMessage());
      ui.displayMessage("  Assigned to " + staffId + ".");
      ui.displayMessage("  Rooms still waiting: "
          + data.getCleaningQueue().getNumberOfEntries());
    } else {
      ui.displayError(result.getMessage());
    }
    ui.pause();
  }

  /** Shows what is waiting to be cleaned, in the order it will be taken. */
  private boolean displayQueue() {
    ui.startAction("CLEANING QUEUE");
    return ui.displayQueue(data.getCleaningQueue().toServiceOrder(), data,
        data.getCleaningQueue().getUrgentCount(),
        data.getCleaningQueue().getNormalCount());
  }

  /**
   * Raises a cleaning task by hand.
   *
   * Most tasks are raised automatically when a guest checks out. This covers
   * the rest - a deep clean, a stayover service, a maintenance job.
   */
  private void raiseNewTask() {
    ui.startAction("RAISE A NEW TASK");

    String roomNo = ui.inputRoomNo();
    if (roomNo == null) {
      return;
    }

    // The room table is checked first, so a mistyped number is reported
    // rather than creating a task against a room that does not exist.
    Room room = data.findRoom(roomNo);
    if (room == null) {
      ui.displayError("Room " + roomNo + " does not exist.");
      ui.pause();
      return;
    }

    HousekeepingTask open = data.findOpenTaskForRoom(roomNo);
    if (open != null) {
      ui.displayError("Room " + roomNo + " already has an open task ("
          + open.getTaskId() + ", " + open.getStatus() + ").");
      ui.pause();
      return;
    }

    String taskType = ui.inputTaskType();
    if (taskType == null) {
      ui.displayMessage("  Cancelled.");
      ui.pause();
      return;
    }

    String remark = ui.inputRemark(false);
    if (remark == null) {
      ui.displayMessage("  Cancelled.");
      ui.pause();
      return;
    }

    HousekeepingTask task = new HousekeepingTask(data.nextTaskId(), roomNo,
        taskType, null, LocalDateTime.now());
    task.setRemark(remark);

    data.getTaskList().add(task);
    service.refreshTaskPriority(task);
    data.getCleaningQueue().enqueue(task, task.getPriority());

    data.getStatusLogList().add(new RoomStatusLog(data.nextStatusLogId(),
        task.getTaskId(), roomNo, null, HousekeepingTask.DIRTY,
        LocalDateTime.now(), staffId, false, "Raised by hand"));

    room.setHousekeepingStatus(Room.DIRTY);
    data.saveHousekeeping();
    data.saveMasters();

    ui.displaySuccess("Task " + task.getTaskId() + " raised for room " + roomNo + ".");
    ui.displayTask(task, data);
    ui.pause();
  }

  // ==================================================================
  // STATUS UPDATES
  // ==================================================================

  /**
   * Moves a task along the cleaning workflow.
   *
   * Only the steps the workflow allows are offered, and reaching ready is what
   * makes the room sellable again - which is the moment this module hands
   * control back to the front desk.
   */
  private void updateTaskStatus() {
    ui.startAction("UPDATE A TASK'S STATUS");

    ListInterface<HousekeepingTask> open = data.getTaskList().filter(
        task -> !HousekeepingTask.READY_FOR_CHECK_IN.equals(task.getStatus()));

    if (!ui.displayTaskList(open, "There is no open task to update.")) {
      ui.pause();
      return;
    }

    String taskId = ui.inputTaskId();
    if (taskId == null) {
      return;
    }

    HousekeepingTask task = data.findTask(taskId);
    if (task == null) {
      ui.displayError("No task with ID " + taskId + ".");
      ui.pause();
      return;
    }

    ui.displayTask(task, data);

    String nextStatus = ui.inputNextStatus(task.getStatus());
    if (nextStatus == null) {
      ui.displayMessage("  Update cancelled.");
      ui.pause();
      return;
    }

    // Blocking a room stops work on it, so the reason has to be recorded.
    String remark = null;
    if (HousekeepingTask.BLOCKED.equals(nextStatus)) {
      remark = ui.inputRemark(true);
      if (remark == null) {
        ui.displayMessage("  Update cancelled - a blocked room needs a reason.");
        ui.pause();
        return;
      }
    } else if (HousekeepingTask.DIRTY.equals(nextStatus)
        && HousekeepingTask.INSPECTED.equals(task.getStatus())) {
      remark = ui.inputRemark(true);
      if (remark == null) {
        ui.displayMessage("  Update cancelled - a failed inspection needs a reason.");
        ui.pause();
        return;
      }
    }

    ServiceResult<HousekeepingTask> result =
        service.updateTaskStatus(taskId, nextStatus, staffId, remark);

    if (result.isFailure()) {
      ui.displayError(result.getMessage());
      ui.pause();
      return;
    }

    ui.displaySuccess(result.getMessage());

    if (HousekeepingTask.DIRTY.equals(nextStatus) && task.getInspectionFailCount() > 0) {
      ui.displayMessage("  The room goes back into the cleaning queue.");
      ui.displayMessage("  Failed inspections for this task: "
          + task.getInspectionFailCount());
    }
    ui.pause();
  }

  /**
   * Undoes the most recent status change.
   *
   * The change being undone stays in the history and a compensating row is
   * added beside it, so a report produced before the rollback and one produced
   * after both remain explainable.
   */
  private void rollbackLastUpdate() {
    ui.startAction("ROLL BACK THE LAST STATUS UPDATE");

    ListInterface<RoomStatusLog> logs = data.getStatusLogList();
    RoomStatusLog latest = null;

    for (int i = logs.getNumberOfEntries(); i >= 1; i--) {
      RoomStatusLog log = logs.getEntry(i);
      if (!log.isRollback() && log.getFromStatus() != null) {
        latest = log;
        break;
      }
    }

    if (latest == null) {
      ui.displayError("There is no status update to roll back.");
      ui.pause();
      return;
    }

    // Shown before it happens, the same way every other irreversible action is.
    ui.displayMessage("  This status update will be undone:");
    ui.displayMessage("");
    utility.MessageUI.displayField("Log ID", latest.getLogId());
    utility.MessageUI.displayField("Task", latest.getTaskId());
    utility.MessageUI.displayField("Room", latest.getRoomNo());
    utility.MessageUI.displayField("Change",
        latest.getFromStatus() + "  ->  " + latest.getToStatus());
    utility.MessageUI.displayField("Made by", latest.getChangedBy());
    utility.MessageUI.displayField("Made at", String.valueOf(latest.getChangedAt()));
    ui.displayMessage("");
    ui.displayMessage("  The room will go back to " + latest.getFromStatus() + ".");
    ui.displayMessage("  The original entry is kept - a rollback row is added beside it.");
    ui.displayMessage("");

    if (!ui.confirm("Roll this update back?")) {
      ui.displayMessage("  Nothing has been changed.");
      ui.pause();
      return;
    }

    ServiceResult<HousekeepingTask> result = service.rollbackLastStatusChange(staffId);

    if (result.isSuccess()) {
      ui.displaySuccess(result.getMessage());
      ui.displayTask(result.getValue(), data);
    } else {
      ui.displayError(result.getMessage());
    }
    ui.pause();
  }

  // ==================================================================
  // SEARCH AND MONITOR
  // ==================================================================

  private void searchByTaskId() {
    ui.startAction("SEARCH BY TASK ID");

    String taskId = ui.inputTaskId();
    if (taskId == null) {
      return;
    }

    HousekeepingTask task = data.findTask(taskId);
    if (task == null) {
      ui.displayError("No task with ID " + taskId + ".");
      ui.pause();
      return;
    }

    ui.displayTask(task, data);

    final String id = taskId;
    ListInterface<RoomStatusLog> logs = data.getStatusLogList().filter(
        log -> id.equals(log.getTaskId()));

    if (!logs.isEmpty()) {
      ui.displayRoomHistory(logs, task.getRoomNo(), task.getStatus());
    }
    ui.pause();
  }

  /**
   * Shows everything that has happened to one room.
   *
   * The history is separate from the room's current status: the log is what
   * happened, and the room record is what is true now.
   */
  private void displayRoomHistory() {
    ui.startAction("A ROOM'S CLEANING HISTORY");

    String roomNo = ui.inputRoomNo();
    if (roomNo == null) {
      return;
    }

    Room room = data.findRoom(roomNo);
    if (room == null) {
      ui.displayError("Room " + roomNo + " does not exist.");
      ui.pause();
      return;
    }

    final String number = roomNo;
    ListInterface<RoomStatusLog> logs = data.getStatusLogList().filter(
        log -> number.equals(log.getRoomNo()));
    logs.sort(Comparator.comparing(RoomStatusLog::getChangedAt));

    ui.displayRoomHistory(logs, roomNo, room.getHousekeepingStatus());

    ListInterface<HousekeepingTask> tasks = data.getTaskList().filter(
        task -> number.equals(task.getRoomNo()));
    ui.displaySectionHeading("Tasks raised for room " + roomNo);
    ui.displayTaskList(tasks, "No task has been raised for this room.");
    ui.pause();
  }

  private void filterByStatus() {
    ui.startAction("FILTER TASKS BY STATUS");

    String status = ui.inputStatusFilter();
    if (status == null) {
      return;
    }

    ListInterface<HousekeepingTask> matches = data.getTaskList().filter(
        task -> status.equals(task.getStatus()));

    ui.displayTaskList(matches, "No task is " + status + ".");
    ui.pause();
  }

  /** The board the front desk relies on - both statuses, side by side. */
  private void displayRoomBoard() {
    ui.startAction("ROOM STATUS BOARD");
    ui.displayRoomBoard(data.getRoomList(), data);
    ui.displayMessage("  A room is sellable only when it is vacant AND cleaned.");

    ListInterface<Room> ready = data.getRoomList().filter(Room::isAssignable);
    ui.displayMessage("");
    ui.displayMessage("  Ready to sell right now: " + ready.getNumberOfEntries()
        + " of " + data.getRoomList().getNumberOfEntries() + " rooms.");
    ui.pause();
  }

  private void displayWholeLog() {
    ui.startAction("THE WHOLE TASK LOG");

    ListInterface<HousekeepingTask> tasks = data.getTaskList();
    ui.displayTaskList(tasks, "The task log is empty.");
    ui.pause();
  }

  // ==================================================================
  // REPORTS
  // ==================================================================

  /** How quickly rooms are being turned around. */
  private void cleaningPerformanceReport() {
    ui.displayReportHeader("CLEANING PERFORMANCE REPORT");

    ListInterface<HousekeepingTask> tasks = data.getTaskList();
    if (tasks.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  There are no tasks to analyse.");
      ui.pause();
      return;
    }

    ListInterface<HousekeepingTask> completed = tasks.filter(
        task -> task.getCleaningDurationMinutes() >= 0);

    ui.displaySectionHeading("Tasks");
    ui.displayReportLine("Total tasks raised", String.valueOf(tasks.getNumberOfEntries()));
    ui.displayReportLine("Completed", String.valueOf(completed.getNumberOfEntries()));
    ui.displayReportLine("Still open",
        String.valueOf(tasks.getNumberOfEntries() - completed.getNumberOfEntries()));
    ui.displayReportLine("Urgent lane",
        String.valueOf(tasks.countIf(HousekeepingTask::isUrgent)));

    ui.displaySectionHeading("Cleaning times");
    if (completed.isEmpty()) {
      ui.displayMessage("  No task has been completed yet.");
    } else {
      long total = 0;
      long fastest = Long.MAX_VALUE;
      long slowest = 0;
      HousekeepingTask fastestTask = null;
      HousekeepingTask slowestTask = null;

      for (int i = 1; i <= completed.getNumberOfEntries(); i++) {
        HousekeepingTask task = completed.getEntry(i);
        long minutes = task.getCleaningDurationMinutes();
        total += minutes;

        if (minutes < fastest) {
          fastest = minutes;
          fastestTask = task;
        }
        if (minutes > slowest) {
          slowest = minutes;
          slowestTask = task;
        }
      }

      int count = completed.getNumberOfEntries();
      ui.displayReportLine("Average cleaning time", (total / count) + " min");
      ui.displayReportLine("Fastest", fastest + " min"
          + (fastestTask == null ? "" : "  (" + fastestTask.getTaskId()
              + ", room " + fastestTask.getRoomNo() + ")"));
      ui.displayReportLine("Slowest", slowest + " min"
          + (slowestTask == null ? "" : "  (" + slowestTask.getTaskId()
              + ", room " + slowestTask.getRoomNo() + ")"));

      displayTimeByType(completed);
    }

    displayCompletionsByHour(completed);

    ui.displayReportFooter();
  }

  /**
   * Average cleaning time per room type.
   *
   * Compared against the standard time each type is supposed to take, so a
   * type consistently over its allowance shows up.
   */
  private void displayTimeByType(ListInterface<HousekeepingTask> completed) {
    ListInterface<RoomType> types = data.getRoomTypeList();
    String[] labels = new String[types.getNumberOfEntries()];
    double[] values = new double[types.getNumberOfEntries()];

    for (int i = 1; i <= types.getNumberOfEntries(); i++) {
      RoomType type = types.getEntry(i);
      labels[i - 1] = type.getTypeId();

      long total = 0;
      int count = 0;
      for (int j = 1; j <= completed.getNumberOfEntries(); j++) {
        HousekeepingTask task = completed.getEntry(j);
        Room room = data.findRoom(task.getRoomNo());
        if (room != null && type.getTypeId().equals(room.getTypeId())) {
          total += task.getCleaningDurationMinutes();
          count++;
        }
      }
      values[i - 1] = (count == 0) ? 0 : (double) total / count;
    }

    ui.displayBarChart("Average cleaning time by room type", "Minutes", labels, values);

    ui.displaySectionHeading("Against the standard time");
    for (int i = 1; i <= types.getNumberOfEntries(); i++) {
      RoomType type = types.getEntry(i);
      double actual = values[i - 1];

      if (actual == 0) {
        ui.displayReportLine(type.getTypeName(), "no completed cleans");
      } else {
        ui.displayReportLine(type.getTypeName(), String.format(
            "%.0f min actual vs %d min standard  (%s)",
            actual, type.getStandardCleanMinutes(),
            actual <= type.getStandardCleanMinutes() ? "on target" : "over"));
      }
    }
  }

  /** When cleaning actually gets finished, to show the busy hours. */
  private void displayCompletionsByHour(ListInterface<HousekeepingTask> completed) {
    if (completed.isEmpty()) {
      return;
    }

    int[] byHour = new int[24];
    for (int i = 1; i <= completed.getNumberOfEntries(); i++) {
      LocalDateTime finished = completed.getEntry(i).getCompletedAt();
      if (finished != null) {
        byHour[finished.getHour()]++;
      }
    }

    int first = -1;
    int last = -1;
    for (int hour = 0; hour < 24; hour++) {
      if (byHour[hour] > 0) {
        if (first < 0) {
          first = hour;
        }
        last = hour;
      }
    }
    if (first < 0) {
      return;
    }

    int span = last - first + 1;
    String[] labels = new String[span];
    double[] values = new double[span];
    int busiest = first;
    int quietest = first;

    for (int i = 0; i < span; i++) {
      int hour = first + i;
      labels[i] = String.format("%02d", hour);
      values[i] = byHour[hour];
      if (byHour[hour] > byHour[busiest]) {
        busiest = hour;
      }
      if (byHour[hour] < byHour[quietest]) {
        quietest = hour;
      }
    }

    ui.displayBarChart("Cleans completed by hour", "Tasks", labels, values);
    ui.displayReportLine("Busiest hour",
        String.format("%02d:00  (%d completed)", busiest, byHour[busiest]));
    ui.displayReportLine("Quietest hour",
        String.format("%02d:00  (%d completed)", quietest, byHour[quietest]));
  }

  /** Which rooms take the most work, and how often cleaning has to be redone. */
  private void workloadAnalysisReport() {
    ui.displayReportHeader("ROOM & WORKLOAD ANALYSIS REPORT");

    ListInterface<HousekeepingTask> tasks = data.getTaskList();
    if (tasks.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  There are no tasks to analyse.");
      ui.pause();
      return;
    }

    ui.displaySectionHeading("Rooms right now");
    ui.displayReportLine("Requiring cleaning",
        String.valueOf(countRoomsAt(Room.DIRTY)));
    ui.displayReportLine("Being cleaned",
        String.valueOf(countRoomsAt(Room.CLEANING_IN_PROGRESS)));
    ui.displayReportLine("Awaiting inspection",
        String.valueOf(countRoomsAt(Room.INSPECTED)));
    ui.displayReportLine("Ready for check-in",
        String.valueOf(countRoomsAt(Room.READY_FOR_CHECK_IN)));
    ui.displayReportLine("Blocked", String.valueOf(countRoomsAt(Room.BLOCKED)));
    ui.displayReportLine("Out of service",
        String.valueOf(data.getRoomList().countIf(Room::isOutOfService)));

    displayTasksPerRoom(tasks);
    displayInspectionQuality(tasks);
    displayOutstandingWorkload();

    ui.displayReportFooter();
  }

  private int countRoomsAt(String status) {
    return data.getRoomList().countIf(room -> status.equals(room.getHousekeepingStatus()));
  }

  /** Which rooms are cleaned most often. */
  private void displayTasksPerRoom(ListInterface<HousekeepingTask> tasks) {
    ListInterface<Room> rooms = data.getRoomList();
    String[] labels = new String[rooms.getNumberOfEntries()];
    double[] values = new double[rooms.getNumberOfEntries()];

    String busiestRoom = "-";
    int highest = 0;

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      final String roomNo = rooms.getEntry(i).getRoomNo();
      int count = tasks.countIf(task -> roomNo.equals(task.getRoomNo()));

      labels[i - 1] = roomNo;
      values[i - 1] = count;

      if (count > highest) {
        highest = count;
        busiestRoom = roomNo;
      }
    }

    ui.displayBarChart("Tasks raised per room", "Tasks", labels, values);
    ui.displayReportLine("Most frequently cleaned room",
        busiestRoom + "  (" + highest + " task(s))");
  }

  /**
   * How often cleaning passes inspection first time.
   *
   * A failure means the room has to be done again, so this figure is really a
   * measure of wasted work.
   */
  private void displayInspectionQuality(ListInterface<HousekeepingTask> tasks) {
    int failures = 0;
    int recleaned = 0;

    for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
      int fails = tasks.getEntry(i).getInspectionFailCount();
      failures += fails;
      if (fails > 0) {
        recleaned++;
      }
    }

    int passes = tasks.countIf(
        task -> HousekeepingTask.READY_FOR_CHECK_IN.equals(task.getStatus()));
    int inspections = passes + failures;

    ui.displaySectionHeading("Inspection quality");
    ui.displayReportLine("Inspections passed", String.valueOf(passes));
    ui.displayReportLine("Inspections failed", String.valueOf(failures));
    ui.displayReportLine("Success rate", inspections == 0 ? "-"
        : String.format("%.1f%%", (passes * 100.0) / inspections));
    ui.displayReportLine("Rooms needing a re-clean", String.valueOf(recleaned));
  }

  /**
   * How much work is still outstanding, in minutes.
   *
   * Estimated from each room type's standard cleaning time, which is what
   * turns a count of open tasks into something a supervisor can staff against.
   */
  private void displayOutstandingWorkload() {
    ListInterface<HousekeepingTask> open = data.getTaskList().filter(
        task -> !HousekeepingTask.READY_FOR_CHECK_IN.equals(task.getStatus())
            && !HousekeepingTask.BLOCKED.equals(task.getStatus()));

    int minutes = 0;
    for (int i = 1; i <= open.getNumberOfEntries(); i++) {
      Room room = data.findRoom(open.getEntry(i).getRoomNo());
      RoomType type = (room == null) ? null : data.findRoomType(room.getTypeId());
      minutes += (type == null) ? 30 : type.getStandardCleanMinutes();
    }

    ui.displaySectionHeading("Outstanding workload");
    ui.displayReportLine("Open tasks", String.valueOf(open.getNumberOfEntries()));
    ui.displayReportLine("  In the urgent lane",
        String.valueOf(data.getCleaningQueue().getUrgentCount()));
    ui.displayReportLine("  In the normal lane",
        String.valueOf(data.getCleaningQueue().getNormalCount()));
    ui.displayReportLine("Estimated work remaining",
        String.format("%d min  (about %.1f hours)", minutes, minutes / 60.0));
  }
}
