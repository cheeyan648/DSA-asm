package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.HousekeepingTaskLogUI;
import entity.Booking;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatusLog;
import entity.RoomType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import utility.MessageUI;

/**
 * Manages housekeeping tasks and room readiness.
 * Handles task processing, status updates, queue management, rollback,
 * and cleaning performance reports.
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
          runOperationsMenu();
          break;
        case 2:
          runUpdateMenu();
          break;
        case 3:
          rollbackLastUpdate();
          break;
        case 4:
          runSearchAndMonitorMenu();
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

  // Shows Housekeeping Operations: view queue, take next room, or raise a task.
  private void runOperationsMenu() {
    int choice;
    do {
      choice = ui.getOperationsMenuChoice();
      switch (choice) {
        case 1:
          displayQueue();
          ui.pause();
          break;
        case 2:
          takeNextRoom();
          break;
        case 3:
          raiseNewTask();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  // Shows Update Task Status: original normal cleaning, or today's stayover.
  private void runUpdateMenu() {
    int choice;
    do {
      choice = ui.getUpdateMenuChoice();
      switch (choice) {
        case 1:
          updateTaskStatus();
          break;
        case 2:
          updateStayoverToday();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  // Shows Search & Monitor: original normal search, or stayover monitor.
  private void runSearchAndMonitorMenu() {
    int choice;
    do {
      choice = ui.getSearchAndMonitorMenuChoice();
      switch (choice) {
        case 1:
          runSearchMenu();
          break;
        case 2:
          runStayoverCleaning();
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
  // Takes the next room off the cleaning queue and starts cleaning it.
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

      displayNextRoomToTake(next);

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

  // Displays the next queued room and any urgent booking waiting on it.
  private void displayNextRoomToTake(HousekeepingTask next) {
    ui.displayMessage("  Next room to clean:");
    ui.displayTask(next, data);

    if (next.isUrgent() && next.getReservedForBookingId() != null) {
      ui.displayMessage("");
      ui.displayMessage("  This room is in the URGENT lane because booking "
          + next.getReservedForBookingId() + " is waiting on it.");
    }
    ui.displayMessage("");
  }

  // Displays the cleaning queue.
  private boolean displayQueue() {
    ui.startAction("CLEANING QUEUE");
    return ui.displayQueue(data.getCleaningQueue().toServiceOrder(), data,
        data.getCleaningQueue().getUrgentCount(),
        data.getCleaningQueue().getNormalCount());
  }

  // ==================================================================
  // STAYOVER CLEANING
  // ==================================================================
  // Runs Stayover Cleaning Monitor: Search or Filter by Status.
  private void runStayoverCleaning() {
    int choice;
    do {
      choice = ui.getStayoverMonitorChoice();
      switch (choice) {
        case 1:
          searchStayoverRecords();
          break;
        case 2:
          filterStayoverByStatus();
          break;
        default:
          break;
      }
    } while (choice != 0);
  }

  // Searches Stayover Cleaning records by date/time or Room ID.
  private void searchStayoverRecords() {
    while (true) {
      ui.startAction("STAYOVER CLEANING MONITOR");
      ListInterface<HousekeepingTask> records = collectAllStayoverRecords();
      ui.displayStayoverCleaning(records, null);

      String query = ui.inputStayoverSearch();
      if (query == null) {
        return;
      }

      ListInterface<HousekeepingTask> matches = matchStayoverSearch(records, query);
      if (matches.isEmpty()) {
        ui.displayStayoverNotFound();
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      ui.startAction("STAYOVER CLEANING MONITOR");
      ui.displayStayoverCleaning(matches, null);
      if (!ui.confirmDoAnother("Do you want to search again?")) {
        return;
      }
    }
  }

  // Filters Stayover Cleaning records by status.
  private void filterStayoverByStatus() {
    while (true) {
      ui.startAction("STAYOVER CLEANING MONITOR");
      String status = ui.inputStayoverStatusFilter();
      if (status == null) {
        return;
      }

      ListInterface<HousekeepingTask> matches = collectAllStayoverRecords().filter(
          task -> status.equals(task.getStatus()));
      if (matches.isEmpty()) {
        ui.displayStayoverNotFound();
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      ui.displayStayoverCleaning(matches, null);
      if (!ui.confirmDoAnother("Do you want to filter again?")) {
        return;
      }
    }
  }

  // Matches Stayover records by Room ID or by stay date/time.
  private ListInterface<HousekeepingTask> matchStayoverSearch(
      ListInterface<HousekeepingTask> records, String query) {
    ListInterface<HousekeepingTask> matches = new ArrayList<>();
    if (query == null || query.isBlank()) {
      return matches;
    }

    String roomNo = parseStayoverRoomId(query);
    LocalDateTime dateTime = parseStayoverDateTime(query);
    LocalDate date = (dateTime == null) ? parseStayoverDate(query) : null;

    for (int i = 1; i <= records.getNumberOfEntries(); i++) {
      HousekeepingTask task = records.getEntry(i);
      if (roomNo != null && roomNo.equals(task.getRoomNo())) {
        matches.add(task);
        continue;
      }
      if (task.getCreatedAt() == null) {
        continue;
      }
      if (date != null && date.equals(task.getCreatedAt().toLocalDate())) {
        matches.add(task);
        continue;
      }
      if (dateTime != null && dateTime.equals(task.getCreatedAt().withSecond(0).withNano(0))) {
        matches.add(task);
      }
    }
    return matches;
  }

  // Interprets digits-only input as a four-digit Room ID.
  private String parseStayoverRoomId(String query) {
    String value = query.trim();
    if (value.isEmpty()) {
      return null;
    }
    for (int i = 0; i < value.length(); i++) {
      if (!Character.isDigit(value.charAt(i))) {
        return null;
      }
    }
    try {
      int number = Integer.parseInt(value);
      if (number > 0) {
        return String.format("%04d", number);
      }
    } catch (NumberFormatException tooLong) {
      return null;
    }
    return null;
  }

  private static final DateTimeFormatter[] STAYOVER_DATE_FORMATS = {
    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    DateTimeFormatter.ofPattern("d/M/yyyy"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
  };

  private static final DateTimeFormatter[] STAYOVER_DATE_TIME_FORMATS = {
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
    DateTimeFormatter.ofPattern("d/M/yyyy HH:mm"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  };

  // Parses a Stayover search value as a date.
  private LocalDate parseStayoverDate(String query) {
    for (int i = 0; i < STAYOVER_DATE_FORMATS.length; i++) {
      try {
        return LocalDate.parse(query.trim(), STAYOVER_DATE_FORMATS[i]);
      } catch (DateTimeParseException ignored) {
        // try the next format
      }
    }
    return null;
  }

  // Parses a Stayover search value as a date and time.
  private LocalDateTime parseStayoverDateTime(String query) {
    for (int i = 0; i < STAYOVER_DATE_TIME_FORMATS.length; i++) {
      try {
        return LocalDateTime.parse(query.trim(), STAYOVER_DATE_TIME_FORMATS[i]);
      } catch (DateTimeParseException ignored) {
        // try the next format
      }
    }
    return null;
  }

  // Updates today's stayover rooms from real Booking data for the current date.
  private void updateStayoverToday() {
    while (true) {
      ui.startAction("STAYOVER CLEANING");
      LocalDate today = LocalDate.now();
      ListInterface<HousekeepingTask> records = collectStayoverRecordsForDate(today);
      ui.displayTodayStayoverCleaning(records, today);
      if (records.isEmpty()) {
        ui.pause();
        return;
      }
      if (!updateStayoverFromList(records)) {
        return;
      }
    }
  }

  // Updates a stayover record from the currently displayed Booking-based list.
  private boolean updateStayoverFromList(ListInterface<HousekeepingTask> records) {
    while (true) {
      if (records.isEmpty()) {
        ui.displayError("No stayover cleaning records to update.");
        ui.pause();
        return false;
      }

      HousekeepingTask task = ui.inputStayoverRecordSelection(records);
      if (task == null) {
        return false;
      }

      if (HousekeepingTask.CLEANED.equals(task.getStatus())) {
        ui.displayStayoverAlreadyCompleted();
        continue;
      }

      String nextStatus = ui.inputStayoverNextStatus(task);
      if (nextStatus == null) {
        return true;
      }

      applyStayoverStatusUpdate(task, nextStatus);
      ui.displaySuccess("Stayover cleaning updated successfully.");
      ui.displayTask(task, data);

      if (HousekeepingTask.CLEANING_IN_PROGRESS.equals(task.getStatus())
          && ui.confirmContinueStayoverCleaning()) {
        applyStayoverStatusUpdate(task, HousekeepingTask.CLEANED);
        ui.displaySuccess("Stayover cleaning updated successfully.");
        ui.displayTask(task, data);
      }
      return true;
    }
  }

  // Builds stayover records for every eligible stay date from Booking data.
  private ListInterface<HousekeepingTask> collectAllStayoverRecords() {
    ListInterface<HousekeepingTask> records = new ArrayList<>();
    ListInterface<LocalDate> dates = collectStayoverDatesFromBookings();
    for (int i = 1; i <= dates.getNumberOfEntries(); i++) {
      ListInterface<HousekeepingTask> day = collectStayoverRecordsForDate(dates.getEntry(i));
      for (int j = 1; j <= day.getNumberOfEntries(); j++) {
        addStayoverIfAbsent(records, day.getEntry(j));
      }
    }

    ListInterface<HousekeepingTask> stored = data.getTaskList().filter(
        HousekeepingTask::isStayoverService);
    for (int i = 1; i <= stored.getNumberOfEntries(); i++) {
      addStayoverIfAbsent(records, stored.getEntry(i));
    }

    records.sort((left, right) -> {
      LocalDate leftDate = left.getCreatedAt() == null
          ? LocalDate.MIN : left.getCreatedAt().toLocalDate();
      LocalDate rightDate = right.getCreatedAt() == null
          ? LocalDate.MIN : right.getCreatedAt().toLocalDate();
      int byDate = leftDate.compareTo(rightDate);
      if (byDate != 0) {
        return byDate;
      }
      return left.getRoomNo().compareTo(right.getRoomNo());
    });
    return records;
  }

  // Adds a Stayover record if its Task ID is not already in the list.
  private void addStayoverIfAbsent(ListInterface<HousekeepingTask> records,
      HousekeepingTask task) {
    if (records.search(existing -> existing.getTaskId().equals(task.getTaskId())) == null) {
      records.add(task);
    }
  }

  // Collects every in-house stay date from existing CHECKED_IN Booking records.
  private ListInterface<LocalDate> collectStayoverDatesFromBookings() {
    ListInterface<LocalDate> dates = new ArrayList<>();
    ListInterface<Booking> bookings = data.getBookingList();
    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking booking = bookings.getEntry(i);
      if (!Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())) {
        continue;
      }
      if (booking.getRoomNo() == null || booking.getRoomNo().isBlank()) {
        continue;
      }
      LocalDate checkIn = booking.getCheckInDate();
      LocalDate checkOut = booking.getCheckOutDate();
      if (checkIn == null || checkOut == null) {
        continue;
      }
      for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
        final LocalDate stayDate = date;
        if (dates.search(existing -> existing.equals(stayDate)) == null) {
          dates.add(stayDate);
        }
      }
    }
    dates.sort(LocalDate::compareTo);
    return dates;
  }

  // Builds the stayover list for a date from existing Booking records.
  private ListInterface<HousekeepingTask> collectStayoverRecordsForDate(LocalDate date) {
    ListInterface<HousekeepingTask> records = new ArrayList<>();
    boolean createdAny = false;
    ListInterface<Booking> bookings = data.getBookingList();

    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Booking booking = bookings.getEntry(i);
      if (!isEligibleStayoverBooking(booking, date)) {
        continue;
      }

      String roomNo = booking.getRoomNo();
      if (findStayoverRecordInList(records, roomNo) != null) {
        continue;
      }

      HousekeepingTask existing = findStayoverServiceForRoomDate(roomNo, date);
      if (existing != null) {
        if (existing.getBookingId() == null) {
          existing.setBookingId(booking.getBookingId());
        }
        records.add(existing);
      } else {
        records.add(createStayoverRecord(booking, date));
        createdAny = true;
      }
    }

    records.sort((left, right) -> left.getRoomNo().compareTo(right.getRoomNo()));
    if (createdAny) {
      data.saveHousekeeping();
    }
    return records;
  }

  // Whether this Booking is an in-house stay on the selected date.
  private boolean isEligibleStayoverBooking(Booking booking, LocalDate date) {
    if (booking == null || date == null) {
      return false;
    }
    if (!Booking.STATUS_CHECKED_IN.equals(booking.getBookingStatus())) {
      return false;
    }
    String roomNo = booking.getRoomNo();
    if (roomNo == null || roomNo.isBlank()) {
      return false;
    }
    LocalDate checkIn = booking.getCheckInDate();
    LocalDate checkOut = booking.getCheckOutDate();
    if (checkIn == null || checkOut == null) {
      return false;
    }
    return !date.isBefore(checkIn) && date.isBefore(checkOut);
  }

  // Finds the stayover-service record already stored for this room and date.
  private HousekeepingTask findStayoverServiceForRoomDate(String roomNo, LocalDate date) {
    return data.getTaskList().search(task ->
        task.isStayoverService()
            && roomNo.equals(task.getRoomNo())
            && task.getCreatedAt() != null
            && date.equals(task.getCreatedAt().toLocalDate()));
  }

  // Finds a stayover record in a displayed list by room ID, preferring one not yet cleaned.
  private HousekeepingTask findStayoverRecordInList(ListInterface<HousekeepingTask> records,
      String roomNo) {
    HousekeepingTask open = records.search(task ->
        roomNo.equals(task.getRoomNo())
            && !HousekeepingTask.CLEANED.equals(task.getStatus()));
    if (open != null) {
      return open;
    }
    return records.search(task -> roomNo.equals(task.getRoomNo()));
  }

  // Creates one stayover-service record from an eligible Booking, without enqueueing it.
  private HousekeepingTask createStayoverRecord(Booking booking, LocalDate date) {
    HousekeepingTask task = new HousekeepingTask(data.nextTaskId(), booking.getRoomNo(),
        HousekeepingTask.TYPE_STAYOVER_CLEAN, booking.getBookingId(), date.atStartOfDay());
    task.setStatus(HousekeepingTask.NOT_CLEANED);
    task.setRemark("Stayover service");
    data.getTaskList().add(task);
    data.getStatusLogList().add(new RoomStatusLog(data.nextStatusLogId(),
        task.getTaskId(), task.getRoomNo(), null, HousekeepingTask.NOT_CLEANED,
        LocalDateTime.now(), staffId, false, "Stayover service"));
    return task;
  }

  // Moves a stayover-service record to the next stayover status and records history.
  private void applyStayoverStatusUpdate(HousekeepingTask task, String toStatus) {
    String fromStatus = task.getStatus();
    if (!HousekeepingTask.isValidTransition(task.getTaskType(), fromStatus, toStatus)) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    task.setStatus(toStatus);

    if (HousekeepingTask.CLEANING_IN_PROGRESS.equals(toStatus)) {
      if (task.getStartedAt() == null) {
        task.setStartedAt(now);
      }
      task.setAssignedTo(staffId);
    }
    if (HousekeepingTask.CLEANED.equals(toStatus)) {
      task.setCompletedAt(now);
    }

    RoomStatusLog log = new RoomStatusLog(data.nextStatusLogId(),
        task.getTaskId(), task.getRoomNo(), fromStatus, toStatus,
        now, staffId, false, "Stayover service");
    data.getStatusLogList().add(log);
    if (fromStatus != null) {
      data.getStatusRollbackStack().push(log);
    }
    data.saveHousekeeping();
  }

  // Raises a new housekeeping task.
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

  // Collects one Raise New Task attempt and stores it if valid.
  private boolean raiseOneNewTask() {
    while (true) {
      ui.startAction("RAISE A NEW TASK");
      // Shown before the prompt so the number typed is an informed choice
      // rather than a guess at which rooms exist and which are already busy.
      ListInterface<Room> eligible = ui.displayRoomsForNewTask(data.getRoomList(), data);
      if (eligible.isEmpty()) {
        ui.pause();
        return false;
      }

      String roomNo = ui.inputRaiseRoomSelection(eligible);
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

      HousekeepingTask task = storeRaisedTask(room, roomNo, taskType, remark);
      ui.displaySuccess("Task " + task.getTaskId() + " created successfully.");
      ui.displayTask(task, data);
      return true;
    }
  }

  // Creates and stores a manually raised housekeeping task.
  private HousekeepingTask storeRaisedTask(Room room, String roomNo, String taskType,
      String remark) {
    HousekeepingTask task = new HousekeepingTask(data.nextTaskId(), roomNo,
        taskType, null, LocalDateTime.now());
    task.setRemark(remark);

    String toStatus = HousekeepingTask.DIRTY;
    if (HousekeepingTask.TYPE_MAINTENANCE.equals(taskType)) {
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
    return task;
  }

  // The task types Raise New Task may offer for this room.
  private String[] availableRaiseTaskTypes(Room room, HousekeepingTask open) {
    if (open != null) {
      return new String[0];
    }

    boolean canMaintain = !Room.OCCUPIED.equals(room.getOccupancyStatus());
    int count = canMaintain ? 2 : 1;
    String[] types = new String[count];
    types[0] = HousekeepingTask.TYPE_DEEP_CLEAN;
    if (canMaintain) {
      types[1] = HousekeepingTask.TYPE_MAINTENANCE;
    }
    return types;
  }

  // ==================================================================
  // STATUS UPDATES
  // ==================================================================
  // Updates the selected Normal Cleaning task status.
  private void updateTaskStatus() {
    while (true) {
      ui.startAction("UPDATE A TASK'S STATUS");

      ListInterface<HousekeepingTask> open = listNormalCleaningUpdatableTasks();
      if (open.isEmpty()) {
        ui.displayError("There is no open task to update.");
        if (!ui.confirmTryAgain()) {
          return;
        }
        continue;
      }

      ui.displayNormalCleaningUpdateList(open);

      HousekeepingTask task = selectNormalCleaningTask(open);
      if (task == null) {
        return;
      }

      ui.startAction("UPDATE A TASK'S STATUS");
      ui.displayTask(task, data);

      if (!continueNormalCleaningUpdates(task)) {
        return;
      }
    }
  }

  // Lists Normal Cleaning tasks that may be updated without using the queue.
  private ListInterface<HousekeepingTask> listNormalCleaningUpdatableTasks() {
    return data.getTaskList().filter(task ->
        isUpdatableTask(task) && !mustStartFromCleaningQueue(task)
            && !HousekeepingTask.DIRTY.equals(task.getStatus()));
  }

  // Selects a Normal Cleaning task by displayed number.
  private HousekeepingTask selectNormalCleaningTask(ListInterface<HousekeepingTask> open) {
    HousekeepingTask task = null;
    while (task == null) {
      task = ui.inputNormalCleaningTaskSelection(open);
      if (task == null) {
        return null;
      }

      if (!isUpdatableTask(task)) {
        ui.displayTaskAlreadyCompleted();
        task = null;
        if (!ui.confirmTryAgain()) {
          return null;
        }
        ui.startAction("UPDATE A TASK'S STATUS");
        ui.displayNormalCleaningUpdateList(open);
        continue;
      }

      if (mustStartFromCleaningQueue(task)) {
        ui.displayMustStartFromQueue();
        task = null;
        if (!ui.confirmTryAgain()) {
          return null;
        }
        ui.startAction("UPDATE A TASK'S STATUS");
        ui.displayNormalCleaningUpdateList(open);
      }
    }
    return task;
  }

  // Continues the same Normal Cleaning task through its legal next statuses.
  private boolean continueNormalCleaningUpdates(HousekeepingTask task) {
    String taskId = task.getTaskId();
    boolean continuingSameTask = false;
    while (true) {
      if (continuingSameTask) {
        ui.startAction("UPDATE A TASK'S STATUS");
        ui.displayTask(task, data);
      }

      String nextStatus = ui.inputNextStatus(task);
      if (nextStatus == null) {
        if (!continuingSameTask) {
          ui.displayMessage("  Update cancelled.");
          ui.pause();
          return false;
        }
        return true;
      }

      String remark = null;
      if (HousekeepingTask.BLOCKED.equals(nextStatus)) {
        remark = ui.inputRemark(true);
        if (remark == null) {
          ui.displayMessage("  Update cancelled - a blocked room needs a reason.");
          ui.pause();
          return false;
        }
      } else if (HousekeepingTask.DIRTY.equals(nextStatus)
          && HousekeepingTask.INSPECTED.equals(task.getStatus())) {
        remark = ui.inputRemark(true);
        if (remark == null) {
          ui.displayMessage("  Update cancelled - a failed inspection needs a reason.");
          ui.pause();
          return false;
        }
      }

      String fromStatus = task.getStatus();
      if (HousekeepingTask.CLEANING_IN_PROGRESS.equals(nextStatus)
          && mustStartFromCleaningQueue(task)) {
        ui.displayMustStartFromQueue();
        if (!ui.confirmTryAgain()) {
          return false;
        }
        return true;
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
          return false;
        }
        return true;
      }

      ui.displaySuccess("Task " + taskId + " updated successfully.");
      ui.displayMessage("  " + result.getMessage());
      announceInspectionFailureFollowOn(nextStatus, fromStatus, task);

      if (!isUpdatableTask(task) || !ui.hasFurtherUpdateStatus(task)) {
        ui.pause();
        return true;
      }
      if (!ui.confirmContinueSameTask(task)) {
        return true;
      }
      continuingSameTask = true;
    }
  }

  // Notes that a failed inspection queued a new cleaning task.
  private void announceInspectionFailureFollowOn(String nextStatus, String fromStatus,
      HousekeepingTask task) {
    if (HousekeepingTask.DIRTY.equals(nextStatus)
        && HousekeepingTask.INSPECTED.equals(fromStatus)) {
      ui.displayMessage("  The room stays DIRTY. A new cleaning task is queued.");
      if (task.getInspectionFailCount() > 0) {
        ui.displayMessage("  Failed inspections for this task: "
            + task.getInspectionFailCount());
      }
    }
  }

  // Whether Update Task Status may change this task.
  private boolean isUpdatableTask(HousekeepingTask task) {
    return task != null && task.isActiveWork();
  }

  // Whether this DIRTY cleaning task must be started from the cleaning queue.
  private boolean mustStartFromCleaningQueue(HousekeepingTask task) {
    return task != null && task.isCleaningType()
        && HousekeepingTask.DIRTY.equals(task.getStatus());
  }

  // Rolls back the last status update.
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
      displayRollbackPreview(latest);

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

  // Displays the status update that will be rolled back.
  private void displayRollbackPreview(RoomStatusLog latest) {
    ui.displayMessage("  This status update will be undone:");
    ui.displayMessage("");
    MessageUI.displayField("Log ID", latest.getLogId());
    MessageUI.displayField("Task", latest.getTaskId());
    MessageUI.displayField("Room", latest.getRoomNo());
    MessageUI.displayField("Change",
        latest.getFromStatus() + "  ->  " + latest.getToStatus());
    MessageUI.displayField("Made by", latest.getChangedBy());
    MessageUI.displayField("Made at", String.valueOf(latest.getChangedAt()));
    ui.displayMessage("");
    ui.displayMessage("  The room will go back to " + latest.getFromStatus() + ".");
    ui.displayMessage("  The original entry is kept - a rollback row is added beside it.");
    ui.displayMessage("");
  }

  // ==================================================================
  // SEARCH AND MONITOR
  // ==================================================================
  // Shows and searches stored housekeeping task records.
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

  // Searches the current browse list by Task ID or Room ID.
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

  // Shows not-found and asks whether to try again.
  private void retrySearchAfterNotFound() {
    ui.displayError("No matching Task ID or Room number found.");
    ui.confirmTryAgain();
  }

  // Works out which records page holds a task.
  private int pageContaining(ListInterface<HousekeepingTask> tasks,
      HousekeepingTask task) {
    int position = tasks.getPosition(task);
    if (position < 1) {
      return 1;
    }
    return ((position - 1) / MessageUI.PAGE_SIZE) + 1;
  }

  // Jumps to the page that holds the found task.
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

  // Jumps to the page of the room's first task, then offers room history.
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

  // Finds one task by its Task ID.
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

  // Displays the room occupancy and housekeeping board.
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
  // Generates the cleaning performance report.
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
    displayAverageTimeByPeriod(completed);

    ui.displayReportFooter();
  }

  // Shows fastest and slowest completed cleaning times.
  private void displayCleaningTimeSummary(ListInterface<HousekeepingTask> completed) {
    ui.displaySectionHeading("Fastest and Slowest Cleaning Time");
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
    ui.displaySectionHeading("Cleaning Performance by Room Type");
    if (completed.isEmpty() || types.isEmpty()) {
      ui.displayMessage("  No completed cleaning data yet.");
      return;
    }

    ui.displayTableHeading(String.format("  %-18s %s", "Room Type", "Average Time"));
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
      System.out.printf("  %-18s %s%n", type.getTypeName(), shown);
    }
    ui.displayThinRule();
  }

  // Displays average cleaning time by morning, afternoon and evening.
  private void displayAverageTimeByPeriod(ListInterface<HousekeepingTask> completed) {
    ui.displaySectionHeading("Cleaning Performance by Time Period");

    String[] names = { "Morning", "Afternoon", "Evening" };
    String[] ranges = { "06:00 - 11:59", "12:00 - 17:59", "18:00 - 23:59" };
    long[] totalMinutes = new long[3];
    int[] count = new int[3];

    for (int i = 1; i <= completed.getNumberOfEntries(); i++) {
      HousekeepingTask task = completed.getEntry(i);
      LocalDateTime started = task.getStartedAt();
      if (started == null) {
        continue;
      }
      int period = timePeriodIndex(started.getHour());
      if (period < 0) {
        continue;
      }
      totalMinutes[period] += task.getCleaningDurationMinutes();
      count[period]++;
    }

    double[] averages = new double[3];
    boolean[] hasData = new boolean[3];
    ui.displayTableHeading(String.format("  %-16s %-18s %s",
        "Time Period", "Time Range", "Average Time"));
    for (int i = 0; i < 3; i++) {
      hasData[i] = count[i] > 0;
      averages[i] = hasData[i] ? (double) totalMinutes[i] / count[i] : Double.NaN;
      String shown = hasData[i] ? String.format("%.0f min", averages[i]) : "N/A";
      System.out.printf("  %-16s %-18s %s%n", names[i], ranges[i], shown);
    }
    ui.displayThinRule();

    ui.displayTimePeriodBarChart(names, ranges, averages, hasData);
    displayFastestCleaningPeriod(names, ranges, averages, hasData);
  }

  // Which report time period a start hour belongs to.
  private int timePeriodIndex(int hour) {
    if (hour >= 6 && hour <= 11) {
      return 0;
    }
    if (hour >= 12 && hour <= 17) {
      return 1;
    }
    if (hour >= 18 && hour <= 23) {
      return 2;
    }
    return -1;
  }

  // Names the period with the lowest average cleaning time among those with data.
  private void displayFastestCleaningPeriod(String[] names, String[] ranges,
      double[] averages, boolean[] hasData) {
    ui.displaySectionHeading("Fastest Cleaning Period");
    int fastest = -1;
    for (int i = 0; i < 3; i++) {
      if (!hasData[i]) {
        continue;
      }
      if (fastest < 0 || averages[i] < averages[fastest]) {
        fastest = i;
      }
    }
    if (fastest < 0) {
      ui.displayMessage("  Fastest cleaning period: N/A");
      return;
    }
    ui.displayReportLine("Fastest cleaning period",
        names[fastest] + "  (" + ranges[fastest] + ")");
    ui.displayReportLine("Average cleaning time",
        String.format("%.0f min", averages[fastest]));
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

  // Generates the room and workload analysis report.
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
        completedCleansForRoom(tasks, room.getRoomNo()) > 0);

    if (roomsWithCleans.isEmpty()) {
      ui.displayMessage("  No completed cleaning history yet.");
    } else {
      ui.displayTableHeading(String.format("  %-12s %s", "Room", "Completed cleans"));
      for (int i = 1; i <= roomsWithCleans.getNumberOfEntries(); i++) {
        String roomNo = roomsWithCleans.getEntry(i).getRoomNo();
        System.out.printf("  %-12s %d%n", roomNo, completedCleansForRoom(tasks, roomNo));
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

  // Counts completed cleaning tasks for one room.
  private int completedCleansForRoom(ListInterface<HousekeepingTask> tasks, String roomNo) {
    return tasks.countIf(task -> roomNo.equals(task.getRoomNo())
        && task.isCleaningType()
        && task.getCleaningDurationMinutes() >= 0);
  }

  // Displays inspection pass and fail totals from stored tasks.
  private void displayInspectionQuality(ListInterface<HousekeepingTask> tasks) {
    int failures = inspectionFailTotal(tasks);
    int recleaned = 0;
    for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
      HousekeepingTask task = tasks.getEntry(i);
      if (task.isCleaningType() && task.getInspectionFailCount() > 0) {
        recleaned++;
      }
    }

    int passes = inspectionPassCount(tasks);
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

  // Counts inspection failures recorded on cleaning tasks.
  private int inspectionFailTotal(ListInterface<HousekeepingTask> tasks) {
    int failures = 0;
    for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
      HousekeepingTask task = tasks.getEntry(i);
      if (task.isCleaningType()) {
        failures += task.getInspectionFailCount();
      }
    }
    return failures;
  }

  // Counts cleaning tasks that reached READY_FOR_CHECK_IN.
  private int inspectionPassCount(ListInterface<HousekeepingTask> tasks) {
    return tasks.countIf(task -> task.isCleaningType()
        && HousekeepingTask.READY_FOR_CHECK_IN.equals(task.getStatus()));
  }

  // Displays maintenance and re-clean task counts.
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

  // Displays outstanding cleaning workload in minutes.
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

    int failures = inspectionFailTotal(tasks);
    int passes = inspectionPassCount(tasks);
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
