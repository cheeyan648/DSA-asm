package control;

import adt.ListInterface;
import boundary.HousekeepingTaskLogUI;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatusLog;
import entity.RoomType;
import java.time.LocalDateTime;
import utility.MessageUI;

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

  // Creates the Housekeeping module using the shared resort data and the staff on duty.
  public HousekeepingTaskLogMaintenance(ResortService service, String staffId) {
    this.service = service;
    this.data = service.getData();
    this.staffId = staffId;
  }

  // ==================================================================
  // MENU
  // ==================================================================

  // Runs the Housekeeping main menu until the user goes back.
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
          runSearchMenu();
          break;
        case 5:
          runReportMenu();
          break;
        default:
          break;
      }
    } while (choice != 0);

    data.saveHousekeeping();
  }

  // Shows the cleaning-queue menu: take the next room, display the queue, or raise a task.
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

  // Shows the search-and-monitor menu for task records, filters and the status board.
  private void runSearchMenu() {
    int choice;
    do {
      choice = ui.getSearchMenuChoice();
      switch (choice) {
        case 1:
          viewAndSearchTaskRecords();
          break;
        case 2:
          filterByStatus();
          break;
        case 3:
          displayRoomBoard();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  // Shows the report menu for cleaning performance and room workload analysis.
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
   * Takes the next room off the cleaning queue and starts cleaning it.
   *
   * This is the only Housekeeping action that moves a DIRTY cleaning task
   * to CLEANING_IN_PROGRESS. The urgent lane is emptied before the normal
   * one, so a room somebody is waiting on is never left behind routine work.
   */
  private void takeNextRoom() {
    while (true) {
      ui.startAction("TAKE THE NEXT ROOM");

      HousekeepingTask next = data.getCleaningQueue().peekNext();
      if (next == null) {
        ui.displayError("No cleaning task is currently waiting in the queue.");
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
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

      if (result.isFailure()) {
        ui.displayError(result.getMessage());
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      ui.displaySuccess(result.getMessage());
      ui.displayMessage("  Assigned to " + staffId + ".");
      ui.displayMessage("  Rooms still waiting: "
          + data.getCleaningQueue().getNumberOfEntries());
      if (!ui.confirmDoAnother("Do you want to take another room?")) {
        return;
      }
    }
  }

  /** Shows what is waiting to be cleaned, in the order it will be taken. */
  private boolean displayQueue() {
    ui.startAction("CLEANING QUEUE");
    return ui.displayQueue(data.getCleaningQueue().toServiceOrder(), data,
        data.getCleaningQueue().getUrgentCount(),
        data.getCleaningQueue().getNormalCount());
  }

  /**
   * Raises a new housekeeping task by hand.
   *
   * A new Task ID is created here only. Inspection is not offered: it is a
   * status of an existing cleaning task, updated under Update Task Status.
   * STAYOVER_CLEAN is not offered.
   */
  private void raiseNewTask() {
    while (true) {
      if (!raiseOneNewTask()) {
        return;
      }
      if (!ui.confirmDoAnother("Do you want to raise another task?")) {
        return;
      }
    }
  }

  /**
   * Collects one Raise New Task attempt.
   *
   * After a valid room is chosen, only the task types that room may take
   * are offered. Returns true after a task is created so the caller can
   * ask whether to raise another. Returns false when the user cancels.
   */
  private boolean raiseOneNewTask() {
    while (true) {
      ui.startAction("RAISE A NEW TASK");
      // Shown before the prompt so the number typed is an informed choice
      // rather than a guess at which rooms exist and which are already busy.
      ui.displayRoomsForNewTask(data.getRoomList(), data);

      String roomNo = ui.inputRoomNo();
      if (roomNo == null) {
        return false;
      }

      // The room table is checked first, so a mistyped number is reported
      // rather than creating a task against a room that does not exist.
      Room room = data.findRoom(roomNo);
      if (room == null) {
        ui.displayError("Room not found.");
        if (!ui.confirmTryAgain()) {
          return false;
        }
        continue;
      }

      HousekeepingTask openForRoom = data.findOpenTaskForRoom(roomNo);
      String[] types = availableRaiseTaskTypes(room, openForRoom);
      if (types.length == 0) {
        if (openForRoom != null) {
          ui.displayError("Room " + roomNo + " already has an open task ("
              + openForRoom.getTaskId() + ", " + openForRoom.getStatus() + ").");
        } else {
          ui.displayError("No task type can be raised for this room.");
        }
        if (!ui.confirmSelectAnotherRoom()) {
          return false;
        }
        continue;
      }

      ui.clearScreen();
      ui.displaySelectedRoomForNewTask(room, data);
      String taskType = ui.inputTaskType(types);
      if (taskType == null) {
        continue;
      }

      if (HousekeepingTask.TYPE_MAINTENANCE.equals(taskType)
          && Room.OCCUPIED.equals(room.getOccupancyStatus())) {
        ui.displayError("Room " + roomNo + " has a guest in it and cannot be blocked.");
        if (!ui.confirmSelectAnotherRoom()) {
          return false;
        }
        continue;
      }

      String remark = ui.inputRemark(false);
      if (remark == null) {
        ui.displayMessage("  Cancelled.");
        ui.pause();
        return false;
      }

      HousekeepingTask task = new HousekeepingTask(data.nextTaskId(), roomNo,
          taskType, null, LocalDateTime.now());
      task.setRemark(remark);

      String toStatus = HousekeepingTask.DIRTY;
      if (HousekeepingTask.TYPE_MAINTENANCE.equals(taskType)) {
        // Records BLOCKED and matches the existing Room out-of-service flag.
        // Front Desk still returns the room to service.
        toStatus = HousekeepingTask.BLOCKED;
        task.setStatus(toStatus);
        room.setHousekeepingStatus(Room.BLOCKED);
        room.setOutOfService(true);
      } else {
        room.setHousekeepingStatus(Room.DIRTY);
      }

      data.getTaskList().add(task);
      service.refreshTaskPriority(task);
      service.enqueueIfNeedsCleaning(task);

      data.getStatusLogList().add(new RoomStatusLog(data.nextStatusLogId(),
          task.getTaskId(), roomNo, null, toStatus,
          LocalDateTime.now(), staffId, false, "Raised by hand"));

      data.saveHousekeeping();
      data.saveMasters();

      ui.displaySuccess("Task " + task.getTaskId() + " created successfully.");
      ui.displayTask(task, data);
      return true;
    }
  }

  /**
   * The task types Raise New Task may offer for this room.
   *
   * Only CHECKOUT_CLEAN, DEEP_CLEAN and MAINTENANCE can be raised by hand.
   * INSPECTION is a status of an existing cleaning task, not a new task.
   * STAYOVER_CLEAN is never offered.
   */
  private String[] availableRaiseTaskTypes(Room room, HousekeepingTask open) {
    if (open != null) {
      return new String[0];
    }

    boolean canMaintain = !Room.OCCUPIED.equals(room.getOccupancyStatus());
    int count = canMaintain ? 3 : 2;
    String[] types = new String[count];
    types[0] = HousekeepingTask.TYPE_CHECKOUT_CLEAN;
    types[1] = HousekeepingTask.TYPE_DEEP_CLEAN;
    if (canMaintain) {
      types[2] = HousekeepingTask.TYPE_MAINTENANCE;
    }
    return types;
  }

  // ==================================================================
  // STATUS UPDATES
  // ==================================================================

  /**
   * Updates the status of an existing open task after cleaning has started.
   *
   * DIRTY cleaning tasks are not started here; they must be taken from the
   * queue. Completed READY_FOR_CHECK_IN tasks are not offered. The Task ID
   * and Task Type stay the same.
   */
  private void updateTaskStatus() {
    while (true) {
      ui.startAction("UPDATE A TASK'S STATUS");

      ListInterface<HousekeepingTask> open = data.getTaskList().filter(
          HousekeepingTask::isActiveWork);

      if (open.isEmpty()) {
        ui.displayError("There is no open task to update.");
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      ui.displayTaskList(open, "There is no open task to update.");

      HousekeepingTask task = null;
      String taskId = null;
      while (task == null) {
        taskId = ui.inputTaskId();
        if (taskId == null) {
          return;
        }

        task = data.findTask(taskId);
        if (task == null) {
          ui.displayError("Task not found.");
          if (!ui.confirmTryAgain()) {
            return;
          }
          ui.startAction("UPDATE A TASK'S STATUS");
          ui.displayTaskList(open, "There is no open task to update.");
          continue;
        }

        if (!isUpdatableTask(task)) {
          ui.displayTaskAlreadyCompleted();
          task = null;
          if (!ui.confirmTryAgain()) {
            return;
          }
          ui.startAction("UPDATE A TASK'S STATUS");
          ui.displayTaskList(open, "There is no open task to update.");
          continue;
        }

        if (mustStartFromCleaningQueue(task)) {
          ui.displayMustStartFromQueue();
          task = null;
          if (!ui.confirmTryAgain()) {
            return;
          }
          ui.startAction("UPDATE A TASK'S STATUS");
          ui.displayTaskList(open, "There is no open task to update.");
        }
      }

      ui.startAction("UPDATE A TASK'S STATUS");
      ui.displayTask(task, data);

      String nextStatus = ui.inputNextStatus(task);
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

      String fromStatus = task.getStatus();
      if (HousekeepingTask.CLEANING_IN_PROGRESS.equals(nextStatus)
          && mustStartFromCleaningQueue(task)) {
        ui.displayMustStartFromQueue();
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      ServiceResult<HousekeepingTask> result =
          service.updateTaskStatus(taskId, nextStatus, staffId, remark);

      if (result.isFailure()) {
        if (!HousekeepingTask.isValidTransition(task.getTaskType(), fromStatus, nextStatus)) {
          ui.displayError("Invalid status transition.");
        } else {
          ui.displayError(result.getMessage());
        }
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      ui.displaySuccess("Task " + taskId + " updated successfully.");
      ui.displayMessage("  " + result.getMessage());

      if (HousekeepingTask.DIRTY.equals(nextStatus)
          && HousekeepingTask.INSPECTED.equals(fromStatus)) {
        ui.displayMessage("  The room stays DIRTY. A new cleaning task is queued.");
        if (task.getInspectionFailCount() > 0) {
          ui.displayMessage("  Failed inspections for this task: "
              + task.getInspectionFailCount());
        }
      }
      if (!ui.confirmDoAnother("Do you want to update another task?")) {
        return;
      }
    }
  }

  /**
   * Whether Update Task Status may change this task.
   *
   * READY_FOR_CHECK_IN and superseded jobs are history. They can still be
   * searched, but they are not updated again.
   */
  private boolean isUpdatableTask(HousekeepingTask task) {
    return task != null && task.isActiveWork();
  }

  /**
   * Whether this cleaning task is still waiting and must be started from
   * the queue rather than Update Task Status.
   */
  private boolean mustStartFromCleaningQueue(HousekeepingTask task) {
    return task != null && task.isCleaningType()
        && HousekeepingTask.DIRTY.equals(task.getStatus());
  }

  /**
   * Undoes the most recent status change.
   *
   * The change being undone stays in the history and a compensating row is
   * added beside it, so a report produced before the rollback and one produced
   * after both remain explainable.
   */
  private void rollbackLastUpdate() {
    while (true) {
      ui.startAction("ROLL BACK THE LAST STATUS UPDATE");

      if (data.getStatusRollbackStack().isEmpty()) {
        ui.displayError("No status change is available for rollback.");
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      RoomStatusLog latest = data.getStatusRollbackStack().peek();

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

      if (result.isFailure()) {
        ui.displayError(result.getMessage());
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      ui.displaySuccess("Last status change was rolled back.");
      ui.displayTask(result.getValue(), data);
      if (!ui.confirmDoAnother("Do you want to rollback another status change?")) {
        return;
      }
    }
  }

  // ==================================================================
  // SEARCH AND MONITOR
  // ==================================================================

  /**
   * Shows the task list a page at a time, then searches by Task ID or room.
   *
   * Pagination (Next, Previous, jump, Search, 0) is offered on every page.
   * Pressing S prints SEARCH under the current page without clearing.
   * A match then clears and jumps to the page that holds that task.
   */
  private void viewAndSearchTaskRecords() {
    ListInterface<HousekeepingTask> tasks = data.getTaskList();
    int page = 1;

    while (true) {
      ui.startAction("VIEW & SEARCH TASK RECORDS");

      if (tasks.isEmpty()) {
        ui.displayError("No matching tasks found.");
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      int totalPages = MessageUI.pageCount(tasks.getNumberOfEntries());
      if (page > totalPages) {
        page = totalPages;
      }

      ui.displaySearchRecordsPage(tasks, page);
      int command = ui.readSearchBrowseCommand(page, totalPages);

      if (command == HousekeepingTaskLogUI.SEARCH_BROWSE_QUIT) {
        return;
      }

      if (command == HousekeepingTaskLogUI.SEARCH_BROWSE_SEARCH) {
        page = searchFromCurrentBrowse(tasks, page);
        continue;
      }

      page = command;
    }
  }

  /**
   * Prints SEARCH under the current page, then jumps if a match is found.
   *
   * Lookup uses the complete task list. The screen is not cleared until a
   * match is found. 0 at the prompt returns to the same records page.
   *
   * @param tasks every housekeeping task
   * @param currentPage the page shown before Search was chosen
   * @return the records page to show next
   */
  private int searchFromCurrentBrowse(ListInterface<HousekeepingTask> tasks,
      int currentPage) {
    ui.startSearchSection();
    String query = ui.inputTaskIdOrRoomNo();
    if (query == null) {
      return currentPage;
    }

    if (query.startsWith("HK")) {
      HousekeepingTask found = findTaskById(query);
      if (found == null) {
        retrySearchAfterNotFound();
        return currentPage;
      }
      int page = pageContaining(tasks, found);
      showFoundTaskOnPage(tasks, found, page);
      return page;
    }

    Room room = data.findRoom(query);
    if (room != null) {
      ListInterface<HousekeepingTask> forRoom = data.getTaskList().filter(
          task -> query.equals(task.getRoomNo()));
      if (forRoom.isEmpty()) {
        retrySearchAfterNotFound();
        return currentPage;
      }
      int page = pageContaining(tasks, forRoom.getEntry(1));
      showFoundRoomOnPage(tasks, query, forRoom, page);
      return page;
    }

    HousekeepingTask asTask = findTaskById("HK" + query);
    if (asTask == null) {
      retrySearchAfterNotFound();
      return currentPage;
    }
    int page = pageContaining(tasks, asTask);
    showFoundTaskOnPage(tasks, asTask, page);
    return page;
  }

  /**
   * Shows not-found on the same screen, then waits for Try Again or Exit.
   *
   * Does not clear first, so the records and the error stay readable.
   */
  private void retrySearchAfterNotFound() {
    ui.displayError("No matching Task ID or Room number found.");
    ui.confirmTryAgain();
  }

  /**
   * Works out which records page holds a task, using its 1-based List position.
   */
  private int pageContaining(ListInterface<HousekeepingTask> tasks,
      HousekeepingTask task) {
    int position = tasks.getPosition(task);
    if (position < 1) {
      return 1;
    }
    return ((position - 1) / MessageUI.PAGE_SIZE) + 1;
  }

  /** Clears, jumps to the page that holds the task, then offers details. */
  private void showFoundTaskOnPage(ListInterface<HousekeepingTask> tasks,
      HousekeepingTask found, int page) {
    ui.startAction("VIEW & SEARCH TASK RECORDS");
    ui.displaySearchRecordsPage(tasks, page);
    ui.displayFound(found.getTaskId());
    if (!ui.confirmOption("View details")) {
      return;
    }
    ui.startAction("VIEW & SEARCH TASK RECORDS");
    ui.displayTaskDetails(found, data);
    ui.pause();
  }

  /**
   * Clears, jumps to the page of the room's first task, then uses the
   * existing room-history selection.
   */
  private void showFoundRoomOnPage(ListInterface<HousekeepingTask> tasks,
      String roomNo, ListInterface<HousekeepingTask> forRoom, int page) {
    ui.startAction("VIEW & SEARCH TASK RECORDS");
    ui.displaySearchRecordsPage(tasks, page);
    ui.displayFound("Room " + roomNo + " found.");
    ui.displayRoomTaskHistory(forRoom, roomNo);
    HousekeepingTask chosen = ui.selectTaskFromList(forRoom, roomNo);
    if (chosen == null) {
      return;
    }
    ui.startAction("VIEW & SEARCH TASK RECORDS");
    ui.displayTaskDetails(chosen, data);
    ui.pause();
  }

  /** Finds one task by its HK ID using the List ADT search. */
  private HousekeepingTask findTaskById(String taskId) {
    return data.getTaskList().search(task -> taskId.equals(task.getTaskId()));
  }

  // Filters the task list by housekeeping status using the List ADT.
  private void filterByStatus() {
    while (true) {
      ui.startAction("FILTER TASKS BY STATUS");

      String status = ui.inputStatusFilter();
      if (status == null) {
        return;
      }

      ListInterface<HousekeepingTask> matches = data.getTaskList().filter(
          task -> status.equals(task.getStatus()));

      if (matches.isEmpty()) {
        ui.displayError("No matching tasks found.");
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      ui.displayTaskList(matches, "No task is " + status + ".");
      if (!ui.confirmDoAnother("Do you want to filter again?")) {
        return;
      }
    }
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

  // ==================================================================
  // REPORTS
  // ==================================================================

  /**
   * Shows how efficiently housekeeping is completing cleaning tasks.
   *
   * Ends with ENTER or 0, then returns to the reports menu. The report is
   * not offered again.
   */
  private void cleaningPerformanceReport() {
    ui.clearScreen();
    ui.displayReportHeader("CLEANING PERFORMANCE REPORT");

    ListInterface<HousekeepingTask> tasks = data.getTaskList().filter(
        HousekeepingTask::isCleaningType);
    if (tasks.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  There are no tasks to analyse.");
      ui.displayReportFooter();
      return;
    }

    ListInterface<HousekeepingTask> completed = tasks.filter(
        task -> task.getCleaningDurationMinutes() >= 0);

    ui.displaySectionHeading("Overview");
    ui.displayReportLine("Total cleaning tasks",
        tasks.getNumberOfEntries() + " tasks");
    ui.displayReportLine("Completed cleaning tasks",
        completed.getNumberOfEntries() + " tasks");
    ui.displayReportLine("Open cleaning tasks",
        (tasks.getNumberOfEntries() - completed.getNumberOfEntries()) + " tasks");
    ui.displayReportLine("Average cleaning time",
        completed.isEmpty() ? "N/A" : averageMinutes(completed) + " min");

    displayCleaningTimeSummary(completed);
    displayTimeByType(completed);
    displayAverageTimeByHour(completed);
    displayCleaningConclusions(completed);

    ui.displayReportFooter();
  }

  // Shows fastest and slowest completed cleaning times.
  private void displayCleaningTimeSummary(ListInterface<HousekeepingTask> completed) {
    ui.displaySectionHeading("Fastest and slowest cleaning time");
    if (completed.isEmpty()) {
      ui.displayMessage("  Fastest cleaning time: N/A");
      ui.displayMessage("  Slowest cleaning time: N/A");
      ui.displayMessage("  No completed tasks yet.");
      return;
    }

    HousekeepingTask fastestTask = completed.getEntry(1);
    HousekeepingTask slowestTask = completed.getEntry(1);
    for (int i = 2; i <= completed.getNumberOfEntries(); i++) {
      HousekeepingTask task = completed.getEntry(i);
      long minutes = task.getCleaningDurationMinutes();
      if (minutes < fastestTask.getCleaningDurationMinutes()) {
        fastestTask = task;
      }
      if (minutes > slowestTask.getCleaningDurationMinutes()) {
        slowestTask = task;
      }
    }

    ui.displayReportLine("Fastest cleaning time",
        fastestTask.getCleaningDurationMinutes() + " min  ("
            + fastestTask.getTaskId() + ", Room " + fastestTask.getRoomNo() + ")");
    ui.displayReportLine("Slowest cleaning time",
        slowestTask.getCleaningDurationMinutes() + " min  ("
            + slowestTask.getTaskId() + ", Room " + slowestTask.getRoomNo() + ")");
  }

  // Average cleaning time per room type, shown as a table.
  private void displayTimeByType(ListInterface<HousekeepingTask> completed) {
    ListInterface<RoomType> types = data.getRoomTypeList();
    ui.displaySectionHeading("Cleaning performance by room type");
    if (completed.isEmpty() || types.isEmpty()) {
      ui.displayMessage("  No completed cleaning data yet.");
      return;
    }

    ui.displayTableHeading(String.format("  %-12s %s", "Room Type", "Average Time"));
    for (int i = 1; i <= types.getNumberOfEntries(); i++) {
      RoomType type = types.getEntry(i);
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
      String shown = (count == 0) ? "N/A" : String.format("%.2f min", (double) total / count);
      System.out.printf("  %-12s %s%n", type.getTypeId(), shown);
    }
    ui.displayThinRule();
  }

  // Average cleaning duration by the hour work started.
  private void displayAverageTimeByHour(ListInterface<HousekeepingTask> completed) {
    ui.displaySectionHeading("Cleaning performance by time/hour");
    if (completed.isEmpty()) {
      ui.displayMessage("  No completed cleaning data yet.");
      return;
    }

    long[] totalMinutes = new long[24];
    int[] count = new int[24];

    for (int i = 1; i <= completed.getNumberOfEntries(); i++) {
      HousekeepingTask task = completed.getEntry(i);
      LocalDateTime started = task.getStartedAt();
      if (started == null) {
        continue;
      }
      int hour = started.getHour();
      totalMinutes[hour] += task.getCleaningDurationMinutes();
      count[hour]++;
    }

    int first = -1;
    int last = -1;
    for (int hour = 0; hour < 24; hour++) {
      if (count[hour] > 0) {
        if (first < 0) {
          first = hour;
        }
        last = hour;
      }
    }
    if (first < 0) {
      ui.displayMessage("  No start-time data yet.");
      return;
    }

    ui.displayTableHeading(String.format("  %-12s %s", "Hour", "Average Minutes"));
    for (int hour = first; hour <= last; hour++) {
      String shown = (count[hour] == 0)
          ? "N/A"
          : String.format("%.0f", (double) totalMinutes[hour] / count[hour]);
      System.out.printf("  %-12s %s%n", String.format("%02d:00", hour), shown);
    }
    ui.displayThinRule();
  }

  // Displays a short conclusion from the calculated cleaning performance data.
  private void displayCleaningConclusions(ListInterface<HousekeepingTask> completed) {
    ui.displaySectionHeading("What this means");
    if (completed.isEmpty()) {
      ui.displayMessage("  There is not enough completed cleaning data to judge efficiency.");
      return;
    }
    ui.displayMessage("  Housekeeping is completing cleaning in "
        + averageMinutes(completed) + " minutes on average.");
  }

  // Calculates the average cleaning time in minutes for the given completed tasks.
  private long averageMinutes(ListInterface<HousekeepingTask> tasks) {
    if (tasks.isEmpty()) {
      return 0;
    }
    long total = 0;
    for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
      total += tasks.getEntry(i).getCleaningDurationMinutes();
    }
    return total / tasks.getNumberOfEntries();
  }

  /**
   * Which rooms take the most work, and how often cleaning has to be redone.
   *
   * Ends with ENTER or 0, then returns to the reports menu. The report is
   * not offered again.
   */
  private void workloadAnalysisReport() {
    ui.clearScreen();
    ui.displayReportHeader("ROOM & WORKLOAD ANALYSIS REPORT");

    ui.displaySectionHeading("Room status overview");
    ui.displayReportLine("Requiring cleaning", roomCount(countRoomsAt(Room.DIRTY)));
    ui.displayReportLine("Cleaning in progress",
        roomCount(countRoomsAt(Room.CLEANING_IN_PROGRESS)));
    ui.displayReportLine("Awaiting inspection",
        roomCount(countRoomsAt(Room.INSPECTED)));
    ui.displayReportLine("Ready for check-in",
        roomCount(countRoomsAt(Room.READY_FOR_CHECK_IN)));
    ui.displayReportLine("Blocked", roomCount(countRoomsAt(Room.BLOCKED)));
    ui.displayReportLine("Out of service",
        roomCount(data.getRoomList().countIf(Room::isOutOfService)));

    ListInterface<HousekeepingTask> tasks = data.getTaskList();
    if (tasks.isEmpty()) {
      ui.displayMessage("");
      ui.displayMessage("  There are no tasks to analyse.");
      ui.displayReportFooter();
      return;
    }

    displayOutstandingWorkload();
    displayTasksPerRoom(tasks);
    displayInspectionQuality(tasks);
    displayMaintenanceAndReclean(tasks);
    displayWorkloadConclusions(tasks);

    ui.displayReportFooter();
  }

  // Counts how many rooms currently have the given housekeeping status.
  private int countRoomsAt(String status) {
    return data.getRoomList().countIf(room -> status.equals(room.getHousekeepingStatus()));
  }

  // Formats a room count with the word "room" or "rooms".
  private String roomCount(int count) {
    return count + (count == 1 ? " room" : " rooms");
  }

  // Shows how often each room has been cleaned, plus re-clean counts.
  private void displayTasksPerRoom(ListInterface<HousekeepingTask> tasks) {
    ui.displaySectionHeading("Cleaning frequency by room");

    ListInterface<Room> rooms = data.getRoomList();
    String mostRecleaned = null;
    int mostFails = 0;
    ListInterface<Room> roomsWithCleans = data.getRoomList().filter(room ->
        tasks.countIf(task -> room.getRoomNo().equals(task.getRoomNo())
            && task.isCleaningType()
            && task.getCleaningDurationMinutes() >= 0) > 0);

    if (roomsWithCleans.isEmpty()) {
      ui.displayMessage("  No completed cleaning history yet.");
    } else {
      ui.displayTableHeading(String.format("  %-12s %s", "Room", "Completed cleans"));
      for (int i = 1; i <= roomsWithCleans.getNumberOfEntries(); i++) {
        final String roomNo = roomsWithCleans.getEntry(i).getRoomNo();
        int completedHere = tasks.countIf(task -> roomNo.equals(task.getRoomNo())
            && task.isCleaningType()
            && task.getCleaningDurationMinutes() >= 0);
        System.out.printf("  %-12s %d%n", roomNo, completedHere);
      }
      ui.displayThinRule();
    }

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      final String roomNo = rooms.getEntry(i).getRoomNo();
      int fails = 0;
      for (int j = 1; j <= tasks.getNumberOfEntries(); j++) {
        HousekeepingTask task = tasks.getEntry(j);
        if (roomNo.equals(task.getRoomNo()) && task.isCleaningType()) {
          fails += task.getInspectionFailCount();
        }
      }
      if (fails > mostFails) {
        mostFails = fails;
        mostRecleaned = roomNo;
      }
    }
    ui.displayReportLine("Most re-cleaned",
        mostFails == 0 ? "N/A"
            : mostRecleaned + "  (" + mostFails + " re-clean events)");
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
      HousekeepingTask task = tasks.getEntry(i);
      if (!task.isCleaningType()) {
        continue;
      }
      int fails = task.getInspectionFailCount();
      failures += fails;
      if (fails > 0) {
        recleaned++;
      }
    }

    int passes = tasks.countIf(
        task -> task.isCleaningType()
            && HousekeepingTask.READY_FOR_CHECK_IN.equals(task.getStatus()));
    int inspections = passes + failures;

    ui.displaySectionHeading("Inspection pass / fail");
    ui.displayReportLine("Inspections passed", passes + " inspections");
    ui.displayReportLine("Inspections failed", failures + " inspections");
    ui.displayReportLine("Pass rate", inspections == 0 ? "N/A"
        : String.format("%.1f%%", (passes * 100.0) / inspections));
    ui.displayReportLine("Re-cleaning",
        tasks.getNumberOfEntries() == 0 ? "N/A"
            : recleaned + " tasks needed at least one re-clean");

    if (inspections > 0) {
      ui.displayBarChart("Inspection Results  (PASS vs FAIL)", "Inspections",
          new String[] { "PASS", "FAIL" },
          new double[] { passes, failures });
    }
  }

  /**
   * Maintenance jobs and re-cleans raised after a failed inspection.
   *
   * Both counts come from the List ADT so the extra work is visible in the
   * same report as the rest of the room workload.
   */
  private void displayMaintenanceAndReclean(ListInterface<HousekeepingTask> tasks) {
    int maintenance = tasks.countIf(HousekeepingTask::isMaintenanceType);
    int recleans = tasks.countIf(HousekeepingTask::isFollowOnCleaning);
    int roomsNeedingReclean = data.getRoomList().countIf(room ->
        tasks.countIf(task -> room.getRoomNo().equals(task.getRoomNo())
            && task.isFollowOnCleaning()) > 0);

    ui.displaySectionHeading("Maintenance and re-cleaning");
    ui.displayReportLine("Maintenance tasks", maintenance + " tasks");
    ui.displayReportLine("Re-clean tasks", recleans + " tasks");
    ui.displayReportLine("Rooms requiring re-cleaning",
        roomsNeedingReclean + (roomsNeedingReclean == 1 ? " room" : " rooms"));
  }

  /**
   * How much work is still outstanding, in minutes.
   *
   * Estimated from each room type's standard cleaning time, which is what
   * turns a count of open tasks into something a supervisor can staff against.
   */
  private void displayOutstandingWorkload() {
    ListInterface<HousekeepingTask> outstanding = data.getTaskList().filter(
        HousekeepingTask::isOutstandingCleaning);

    int minutes = 0;
    int urgentMinutes = 0;
    int normalMinutes = 0;
    int urgentOpen = outstanding.countIf(HousekeepingTask::isUrgent);
    int normalOpen = outstanding.getNumberOfEntries() - urgentOpen;
    for (int i = 1; i <= outstanding.getNumberOfEntries(); i++) {
      HousekeepingTask task = outstanding.getEntry(i);
      int estimate = estimatedMinutesFor(task);
      minutes += estimate;
      if (task.isUrgent()) {
        urgentMinutes += estimate;
      } else {
        normalMinutes += estimate;
      }
    }

    ui.displaySectionHeading("Outstanding cleaning workload");
    ui.displayReportLine("Outstanding cleaning tasks",
        outstanding.getNumberOfEntries() + " tasks");
    ui.displayReportLine("Estimated remaining cleaning time",
        minutes + " minutes");

    ui.displaySectionHeading("URGENT vs NORMAL workload");
    ui.displayReportLine("URGENT tasks", urgentOpen + " tasks");
    ui.displayReportLine("URGENT estimated time", urgentMinutes + " minutes");
    ui.displayReportLine("NORMAL tasks", normalOpen + " tasks");
    ui.displayReportLine("NORMAL estimated time", normalMinutes + " minutes");
  }

  // Estimates remaining cleaning minutes from the room type's standard cleaning time.
  private int estimatedMinutesFor(HousekeepingTask task) {
    Room room = data.findRoom(task.getRoomNo());
    RoomType type = (room == null) ? null : data.findRoomType(room.getTypeId());
    return (type == null) ? 30 : type.getStandardCleanMinutes();
  }

  // Displays a short conclusion from the current outstanding workload data.
  private void displayWorkloadConclusions(ListInterface<HousekeepingTask> tasks) {
    ui.displaySectionHeading("What this means");

    ListInterface<HousekeepingTask> outstanding = tasks.filter(
        HousekeepingTask::isOutstandingCleaning);
    int urgentOpen = outstanding.countIf(HousekeepingTask::isUrgent);

    int failures = 0;
    for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
      HousekeepingTask task = tasks.getEntry(i);
      if (task.isCleaningType()) {
        failures += task.getInspectionFailCount();
      }
    }
    int passes = tasks.countIf(
        task -> task.isCleaningType()
            && HousekeepingTask.READY_FOR_CHECK_IN.equals(task.getStatus()));
    int inspections = passes + failures;

    if (outstanding.isEmpty() && inspections == 0) {
      ui.displayMessage("  There is no outstanding cleaning workload to report.");
      return;
    }

    String meaning = "  There "
        + (outstanding.getNumberOfEntries() == 1 ? "is " : "are ")
        + outstanding.getNumberOfEntries()
        + (outstanding.getNumberOfEntries() == 1
            ? " outstanding cleaning task"
            : " outstanding cleaning tasks")
        + " (" + urgentOpen + " urgent).";
    if (inspections > 0) {
      meaning += " Inspection pass rate is "
          + String.format("%.1f%%", (passes * 100.0) / inspections) + ".";
    }
    ui.displayMessage(meaning);
  }
}
