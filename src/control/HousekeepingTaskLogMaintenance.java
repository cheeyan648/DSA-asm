package control;

import adt.ArrayList;
import adt.ListInterface;
import adt.ArrayStack;
import adt.StackInterface;
import boundary.HousekeepingTaskLogUI;
import dao.HousekeepingTaskDAO;
import dao.HousekeepingTaskInitializer;
import entity.HousekeepingTask;
import utility.MessageUI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

/**
 * @author Kat Tan
 */
public class HousekeepingTaskLogMaintenance {

  public HousekeepingTaskLogMaintenance() {
    // Load saved housekeeping records
    taskLog = housekeepingTaskDAO.retrieveFromFile();

    // Restore next unique Task ID number
    nextTaskNumber = housekeepingTaskDAO.getNextTaskNumber();

    // Seed sample records on first run (when no persisted data exists yet).
    if (taskLog.isEmpty()) {
      taskLog = new HousekeepingTaskInitializer().initializeHousekeepingTasks();
      nextTaskNumber = taskLog.getNumberOfEntries() + 1;
      housekeepingTaskDAO.saveToFile(taskLog, nextTaskNumber);
    }

    // Rebuild rollback stack from saved records
    for (int i = 1; i <= taskLog.getNumberOfEntries(); i++) {
      statusHistory.push(taskLog.getEntry(i));
    }
  }

  private HousekeepingTaskLogUI housekeepingTaskLogUI = new HousekeepingTaskLogUI();

  private HousekeepingTaskDAO housekeepingTaskDAO = new HousekeepingTaskDAO();

  private ListInterface<HousekeepingTask> taskLog;

  private StackInterface<HousekeepingTask> statusHistory = new ArrayStack<>();

  private int nextTaskNumber;

  public void runHousekeepingTaskLog() {
    int choice;
    do {
      choice = housekeepingTaskLogUI.getMenuChoice();
      switch (choice) {
        case 1:
          logStatusUpdate();
          break;
        case 2:
          rollbackLastStatus();
          break;
        case 3:
          displayTaskLog();
          break;
        case 4:
          searchHousekeepingTask();
          break;
        case 5:
          generateRoomStatusReport();
          break;
        case 6:
          generateHousekeepingActivityReport();
          break;
        case 0:
          break;
      }
    } while (choice != 0);
    housekeepingTaskDAO.saveToFile(taskLog, nextTaskNumber);
  }

  private String generateUniqueTaskId() {
    String taskId = String.format("HT%04d", nextTaskNumber);
    nextTaskNumber++;
    return taskId;
  }

  private HousekeepingTask findLatestTaskByRoom(String roomNumber) {
    for (int i = taskLog.getNumberOfEntries(); i >= 1; i--) {
      HousekeepingTask task = taskLog.getEntry(i);
      if (task.getRoomNumber().equals(roomNumber)) {
        return task;
      }
    }
    return null;
  }

  private boolean removeTaskFromLog(String taskId) {
    for (int i = taskLog.getNumberOfEntries(); i >= 1; i--) {
      HousekeepingTask task = taskLog.getEntry(i);
      if (task.getTaskId().equalsIgnoreCase(taskId)) {
        taskLog.remove(i);
        return true;
      }
    }
    return false;
  }

  public void logStatusUpdate() {
    while (true) {
      housekeepingTaskLogUI.displayLogStatusUpdateHeader();

      // Step 1: Input room number
      String roomNumber = housekeepingTaskLogUI.inputRoomNumber();

      // Step 2: Search latest housekeeping record
      HousekeepingTask currentTask = findLatestTaskByRoom(roomNumber);

      // Step 3: If room already has housekeeping record
      if (currentTask != null) {
        housekeepingTaskLogUI.displayCurrentStatus(roomNumber, currentTask.getStatus());
        int updateChoice = housekeepingTaskLogUI.getUpdateChoice();
        // User chooses cancel
        if (updateChoice == 0) {
          housekeepingTaskLogUI.displayMessage("Operation cancelled.");
          return;
        }
      } else {
        // No existing housekeeping record
        housekeepingTaskLogUI.displayNoExistingRecord(roomNumber);
      }

      // Step 4: Select new status
      String newStatus = housekeepingTaskLogUI.inputStatus();

      // Cancel status selection
      if (newStatus == null) {
        housekeepingTaskLogUI.displayMessage("Operation cancelled.");
        return;
      }

      // Step 5: Generate unique Task ID
      String taskId = generateUniqueTaskId();

      // Step 6: Create housekeeping task
      HousekeepingTask newTask = new HousekeepingTask(taskId, roomNumber, newStatus, LocalDateTime.now());

      // Step 7: Add into List ADT
      boolean added = taskLog.add(newTask);

      if (added) {
        statusHistory.push(newTask);
        housekeepingTaskDAO.saveToFile(taskLog, nextTaskNumber);
        MessageUI.clearScreen();
        housekeepingTaskLogUI.displayMessage("Task status update logged successfully." + "\nTask ID: " + taskId
            + "\nRoom Number: " + roomNumber + "\nNew Status: " + newStatus);
        // Step 8: Ask user whether to update again or return to housekeeping menu
        int nextChoice = housekeepingTaskLogUI.getAfterUpdateChoice();
        if (nextChoice == 0) {
          return;
        }
      } else {
        housekeepingTaskLogUI.displayMessage("Unable to log task status update.");
        return;
      }
    }
  }

  public void rollbackLastStatus() {
    while (true) {
      housekeepingTaskLogUI.displayRollbackHeader();

      // Step 1: Check whether there is anything to rollback
      if (statusHistory.isEmpty()) {
        MessageUI.clearScreen();
        housekeepingTaskLogUI.displayMessage("No status updates available to rollback.");
        housekeepingTaskLogUI.pressEnterToContinue();
        return;
      }

      // Step 2: Look at the latest status update without removing it yet
      HousekeepingTask lastTask = statusHistory.peek();

      housekeepingTaskLogUI.displayLastStatusUpdate(
          lastTask.getTaskId(),
          lastTask.getRoomNumber(),
          lastTask.getStatus());

      // Step 3:Find what the previous status for this room would be
      String previousStatus = null;

      for (int i = taskLog.getNumberOfEntries(); i >= 1; i--) {
        HousekeepingTask task = taskLog.getEntry(i);
        if (task.getRoomNumber().equals(lastTask.getRoomNumber())
            && !task.getTaskId().equalsIgnoreCase(lastTask.getTaskId())) {
          previousStatus = task.getStatus();
          break;
        }
      }

      housekeepingTaskLogUI.displayPreviousStatus(previousStatus);

      // Step 4: Confirm rollback
      int confirmation = housekeepingTaskLogUI.getRollbackConfirmation();

      if (confirmation == 0) {
        MessageUI.clearScreen();
        housekeepingTaskLogUI.displayMessage("Rollback cancelled.");
        housekeepingTaskLogUI.pressEnterToContinue();
        return;
      }

      // Step 5:Remove from Stack
      HousekeepingTask rolledBackTask = statusHistory.pop();

      // Step 6:Remove the same task from List ADT
      boolean removed = removeTaskFromLog(rolledBackTask.getTaskId());

      if (!removed) {
        MessageUI.clearScreen();
        housekeepingTaskLogUI.displayMessage("Unable to rollback status update.");
        housekeepingTaskLogUI.pressEnterToContinue();
        return;
      }
      // Save after successful rollback
      housekeepingTaskDAO.saveToFile(taskLog, nextTaskNumber);

      // Step 7:Find the room's current status AFTER rollback
      HousekeepingTask restoredTask = findLatestTaskByRoom(rolledBackTask.getRoomNumber());

      String currentStatus = null;

      if (restoredTask != null) {
        currentStatus = restoredTask.getStatus();
      }

      // Step 8: Show successful rollback
      housekeepingTaskLogUI.displayRollbackSuccess(
          rolledBackTask.getTaskId(),
          rolledBackTask.getRoomNumber(),
          rolledBackTask.getStatus(),
          currentStatus);

      // Step 9:Rollback another or return to menu
      int nextChoice = housekeepingTaskLogUI.getAfterRollbackChoice();

      if (nextChoice == 0) {
        return;
      }
    }
  }

  public void displayTaskLog() {
    if (taskLog.getNumberOfEntries() == 0) {
      housekeepingTaskLogUI.displayEmptyTaskLog();
      housekeepingTaskLogUI.pressEnterToContinue();
      return;
    }

    housekeepingTaskLogUI.displayTaskLogHeader();
    for (int i = 1; i <= taskLog.getNumberOfEntries(); i++) {
      HousekeepingTask task = taskLog.getEntry(i);
      housekeepingTaskLogUI.displayTaskLogRow(
          task.getTaskId(),
          task.getRoomNumber(),
          task.getStatus(),
          task.getTimestamp());
    }
    housekeepingTaskLogUI.displayTaskLogFooter(taskLog.getNumberOfEntries());
    housekeepingTaskLogUI.pressEnterToContinue();
  }

  public void searchHousekeepingTask() {
    while (true) {
      // Check whether there are records to search
      if (taskLog.getNumberOfEntries() == 0) {
        MessageUI.clearScreen();
        housekeepingTaskLogUI.displayMessage("No housekeeping task records available.");
        housekeepingTaskLogUI.pressEnterToContinue();
        return;
      }

      // Display search menu
      int searchChoice = housekeepingTaskLogUI.getSearchChoice();
      if (searchChoice == 0) {
        return;
      }

      String searchTaskId = null;
      String searchRoomNumber = null;
      String searchStatus = null;
      String searchDescription = "";

      // Get search value based on selected criterion
      switch (searchChoice) {
        case 1:
          searchTaskId = housekeepingTaskLogUI.inputSearchTaskId();
          searchDescription = "Task ID = " + searchTaskId;
          break;
        case 2:
          searchRoomNumber = housekeepingTaskLogUI.inputRoomNumber();
          searchDescription = "Room Number = " + searchRoomNumber;
          break;
        case 3:
          searchStatus = housekeepingTaskLogUI.inputSearchStatus();
          if (searchStatus == null) {
            continue;
          }
          searchDescription = "Status = " + searchStatus;
          break;
      }

      housekeepingTaskLogUI.displaySearchResultHeader(searchDescription);
      int totalFound = 0;

      // Linear Search
      for (int i = 1; i <= taskLog.getNumberOfEntries(); i++) {
        HousekeepingTask task = taskLog.getEntry(i);
        boolean match = false;
        switch (searchChoice) {
          case 1:
            match = task.getTaskId().equalsIgnoreCase(searchTaskId);
            break;
          case 2:
            match = task.getRoomNumber().equals(searchRoomNumber);
            break;
          case 3:
            match = task.getStatus().equalsIgnoreCase(searchStatus);
            break;
        }

        if (match) {
          housekeepingTaskLogUI.displayTaskLogRow(task.getTaskId(), task.getRoomNumber(), task.getStatus(),
              task.getTimestamp());
          totalFound++;
          // Task ID is unique, so no need to continue searching
          if (searchChoice == 1) {
            break;
          }
        }
      }

      if (totalFound == 0) {
        housekeepingTaskLogUI.displayMessage("No matching housekeeping task records found.");
      }

      housekeepingTaskLogUI.displaySearchResultFooter(totalFound);
      int nextChoice = housekeepingTaskLogUI.getAfterSearchChoice();

      if (nextChoice == 0) {
        return;
      }
    }
  }

  private ListInterface<HousekeepingTask> buildCurrentRoomList() {
    ListInterface<HousekeepingTask> currentRoomList = new ArrayList<>();
    // Traverse from newest to oldest
    for (int i = taskLog.getNumberOfEntries(); i >= 1; i--) {
      HousekeepingTask task = taskLog.getEntry(i);
      boolean roomExists = false;
      // Linear Search: Check whether this room has already been added into
      // currentRoomList
      for (int j = 1; j <= currentRoomList.getNumberOfEntries(); j++) {
        HousekeepingTask currentRoom = currentRoomList.getEntry(j);
        if (currentRoom.getRoomNumber().equals(task.getRoomNumber())) {
          roomExists = true;
          break;
        }
      }
      if (!roomExists) {
        currentRoomList.add(task);
      }
    }
    return currentRoomList;
  }

  private boolean matchesStatusFilter(HousekeepingTask task, String statusFilter) {
    if (statusFilter.equals("ALL")) {
      return true;
    }
    return task.getStatus()
        .equalsIgnoreCase(statusFilter);
  }

  private boolean matchesRoomFilter(HousekeepingTask task, int roomFilterChoice, String specificRoom, int startRoom,
      int endRoom) {
    switch (roomFilterChoice) {
      case 1:
        // All Rooms
        return true;
      case 2:
        // Specific Room
        return task.getRoomNumber().equals(specificRoom);
      case 3:
        // Room Number Range
        int roomNumber = Integer.parseInt(task.getRoomNumber());
        return roomNumber >= startRoom && roomNumber <= endRoom;
      default:
        return false;
    }
  }

  private void bubbleSortRoomList(ListInterface<HousekeepingTask> roomList, int sortChoice) {
    int numberOfRooms = roomList.getNumberOfEntries();
    for (int i = 1; i < numberOfRooms; i++) {
      boolean swapped = false;

      for (int j = 1; j <= numberOfRooms - i; j++) {
        HousekeepingTask firstTask = roomList.getEntry(j);
        HousekeepingTask secondTask = roomList.getEntry(j + 1);
        int firstRoom = Integer.parseInt(firstTask.getRoomNumber());
        int secondRoom = Integer.parseInt(secondTask.getRoomNumber());
        boolean shouldSwap = false;
        // Ascending
        if (sortChoice == 1 && firstRoom > secondRoom) {
          shouldSwap = true;
        }
        // Descending
        else if (sortChoice == 2 && firstRoom < secondRoom) {
          shouldSwap = true;
        }

        if (shouldSwap) {
          roomList.replace(j, secondTask);
          roomList.replace(j + 1, firstTask);
          swapped = true;
        }
      }
      // No swap means list is already sorted
      if (!swapped) {
        break;
      }
    }
  }

  public void generateRoomStatusReport() {
    while (true) {
      // Step 1: Check available records
      if (taskLog.getNumberOfEntries() == 0) {
        MessageUI.clearScreen();
        housekeepingTaskLogUI.displayMessage("No housekeeping task records available.");
        housekeepingTaskLogUI.pressEnterToContinue();
        return;
      }

      // Step 2: Get latest status of each room
      ListInterface<HousekeepingTask> currentRoomList = buildCurrentRoomList();

      // Step 3: Status Filter
      String statusFilter = housekeepingTaskLogUI.getRoomStatusFilter();
      if (statusFilter == null) {
        return;
      }

      // Step 4: Room Filter
      int roomFilterChoice = housekeepingTaskLogUI.getRoomFilterChoice();
      if (roomFilterChoice == 0) {
        return;
      }

      String specificRoom = null;
      int startRoom = 0;
      int endRoom = 0;

      // Specific Room
      if (roomFilterChoice == 2) {
        specificRoom = housekeepingTaskLogUI.inputRoomNumber();
      }

      // Room Number Range
      else if (roomFilterChoice == 3) {
        boolean validRange = false;
        while (!validRange) {
          startRoom = housekeepingTaskLogUI.inputRoomRange("Enter Starting Room Number: ");
          endRoom = housekeepingTaskLogUI.inputRoomRange("Enter Ending Room Number: ");
          if (startRoom <= endRoom) {
            validRange = true;
          } else {
            housekeepingTaskLogUI
                .displayMessage("Starting room number cannot be greater " + "than ending room number.");
          }
        }
      }

      // Step 5: Apply Multiple Criteria Filters
      ListInterface<HousekeepingTask> filteredRoomList = new ArrayList<>();
      for (int i = 1; i <= currentRoomList.getNumberOfEntries(); i++) {
        HousekeepingTask task = currentRoomList.getEntry(i);
        boolean statusMatch = matchesStatusFilter(task, statusFilter);
        boolean roomMatch = matchesRoomFilter(task, roomFilterChoice, specificRoom, startRoom, endRoom);
        // Both criteria must match
        if (statusMatch && roomMatch) {
          filteredRoomList.add(task);
        }
      }

      // Step 6: Sort Order
      int sortChoice = housekeepingTaskLogUI.getRoomSortChoice();
      if (sortChoice == 0) {
        return;
      }

      // Step 7: Bubble Sort
      bubbleSortRoomList(filteredRoomList, sortChoice);

      // Step 8: Prepare Report Descriptions
      String statusDescription;
      if (statusFilter.equals("ALL")) {
        statusDescription = "All Statuses";
      } else {
        statusDescription = statusFilter;
      }

      String roomDescription = "";
      switch (roomFilterChoice) {
        case 1:
          roomDescription = "All Rooms";
          break;
        case 2:
          roomDescription = "Room " + specificRoom;
          break;
        case 3:
          roomDescription = "Room " + startRoom + " - " + endRoom;
          break;
      }

      String sortDescription;
      if (sortChoice == 1) {
        sortDescription = "Room Number Ascending";
      } else {
        sortDescription = "Room Number Descending";
      }

      // Step 9: Display Report Header
      housekeepingTaskLogUI.displayRoomStatusReportHeader(statusDescription, roomDescription, sortDescription);

      // Step 10: Handle No Matching Records
      if (filteredRoomList.getNumberOfEntries() == 0) {
        housekeepingTaskLogUI.displayNoMatchingRooms();
        housekeepingTaskLogUI.displayRoomStatusReportFooter(0);
        int nextChoice = housekeepingTaskLogUI.getAfterRoomStatusReportChoice();
        if (nextChoice == 0) {
          return;
        }
        // User selected Generate Another Report
        continue;
      }

      // Step 11: Display Report Rows
      int dirtyCount = 0;
      int cleaningCount = 0;
      int inspectedCount = 0;
      int readyCount = 0;
      for (int i = 1; i <= filteredRoomList.getNumberOfEntries(); i++) {
        HousekeepingTask task = filteredRoomList.getEntry(i);
        housekeepingTaskLogUI.displayRoomStatusReportRow(task.getRoomNumber(), task.getStatus(), task.getTaskId(),
            task.getTimestamp());
        // Count current statuses
        switch (task.getStatus()) {
          case "Dirty":
            dirtyCount++;
            break;
          case "Cleaning In Progress":
            cleaningCount++;
            break;
          case "Inspected":
            inspectedCount++;
            break;
          case "Ready for Check-In":
            readyCount++;
            break;
        }
      }

      // Step 12: Report Footer
      housekeepingTaskLogUI.displayRoomStatusReportFooter(filteredRoomList.getNumberOfEntries());

      // Step 13: Status Summary, Only for All Statuses
      if (statusFilter.equals("ALL")) {
        housekeepingTaskLogUI.displayRoomStatusSummary(dirtyCount, cleaningCount, inspectedCount, readyCount);
      }

      // Step 14: Generate Another / Back
      int nextChoice = housekeepingTaskLogUI.getAfterRoomStatusReportChoice();

      if (nextChoice == 0) {
        return;
      }
    }
  }

  private boolean matchesDateFilter(HousekeepingTask task, int dateFilterChoice, LocalDate specificDate,
      LocalDate startDate, LocalDate endDate) {
    LocalDate taskDate = task.getTimestamp().toLocalDate();
    switch (dateFilterChoice) {
      case 1:
        // All Dates
        return true;
      case 2:
        // Specific Date
        return taskDate.equals(specificDate);
      case 3:
        // Date Range
        return !taskDate.isBefore(startDate) && !taskDate.isAfter(endDate);
      default:
        return false;
    }
  }

  private void bubbleSortActivityList(ListInterface<HousekeepingTask> activityList, int sortChoice) {
    int numberOfActivities = activityList.getNumberOfEntries();
    for (int i = 1; i < numberOfActivities; i++) {
      boolean swapped = false;
      for (int j = 1; j <= numberOfActivities - i; j++) {
        HousekeepingTask firstTask = activityList.getEntry(j);
        HousekeepingTask secondTask = activityList.getEntry(j + 1);
        boolean shouldSwap = false;

        // Oldest to Newest
        if (sortChoice == 1 && firstTask.getTimestamp().isAfter(secondTask.getTimestamp())) {
          shouldSwap = true;
        }

        // Newest to Oldest
        else if (sortChoice == 2 && firstTask.getTimestamp().isBefore(secondTask.getTimestamp())) {
          shouldSwap = true;
        }
        if (shouldSwap) {
          activityList.replace(j, secondTask);
          activityList.replace(j + 1, firstTask);
          swapped = true;
        }
      }
      if (!swapped) {
        break;
      }
    }
  }

  private int countUniqueRooms(ListInterface<HousekeepingTask> activityList) {
    ListInterface<String> uniqueRooms = new ArrayList<>();
    for (int i = 1; i <= activityList.getNumberOfEntries(); i++) {
      HousekeepingTask task = activityList.getEntry(i);
      boolean roomExists = false;
      // Linear Search
      for (int j = 1; j <= uniqueRooms.getNumberOfEntries(); j++) {
        String roomNumber = uniqueRooms.getEntry(j);
        if (roomNumber.equals(task.getRoomNumber())) {
          roomExists = true;
          break;
        }
      }
      if (!roomExists) {
        uniqueRooms.add(task.getRoomNumber());
      }
    }
    return uniqueRooms.getNumberOfEntries();
  }

  public void generateHousekeepingActivityReport() {
    while (true) {
      // Step 1: Check Available Activities
      if (taskLog.getNumberOfEntries() == 0) {
        MessageUI.clearScreen();
        housekeepingTaskLogUI.displayMessage("No housekeeping task records available.");
        housekeepingTaskLogUI.pressEnterToContinue();
        return;
      }

      // Step 2: Date Filter
      int dateFilterChoice = housekeepingTaskLogUI.getActivityDateFilterChoice();
      if (dateFilterChoice == 0) {
        return;
      }

      LocalDate specificDate = null;
      LocalDate startDate = null;
      LocalDate endDate = null;

      // Specific Date
      if (dateFilterChoice == 2) {
        specificDate = housekeepingTaskLogUI.inputActivityDate("Enter Date (dd/MM/yyyy): ");
      }

      // Date Range
      else if (dateFilterChoice == 3) {
        boolean validDateRange = false;
        while (!validDateRange) {
          startDate = housekeepingTaskLogUI.inputActivityDate("Enter Start Date (dd/MM/yyyy): ");
          endDate = housekeepingTaskLogUI.inputActivityDate("Enter End Date (dd/MM/yyyy): ");
          if (!startDate.isAfter(endDate)) {
            validDateRange = true;
          } else {
            housekeepingTaskLogUI.displayMessage("Start date cannot be later than end date.");
          }
        }
      }

      // Step 3: Status Filter
      String statusFilter = housekeepingTaskLogUI.getActivityStatusFilter();
      if (statusFilter == null) {
        return;
      }

      // Step 4: Room Filter
      int roomFilterChoice = housekeepingTaskLogUI.getRoomFilterChoice();
      if (roomFilterChoice == 0) {
        return;
      }

      String specificRoom = null;
      int startRoom = 0;
      int endRoom = 0;

      // Specific Room
      if (roomFilterChoice == 2) {
        specificRoom = housekeepingTaskLogUI.inputRoomNumber();
      }

      // Room Number Range
      else if (roomFilterChoice == 3) {
        boolean validRoomRange = false;
        while (!validRoomRange) {
          startRoom = housekeepingTaskLogUI.inputRoomRange("Enter Starting Room Number: ");
          endRoom = housekeepingTaskLogUI.inputRoomRange("Enter Ending Room Number: ");
          if (startRoom <= endRoom) {
            validRoomRange = true;
          } else {
            housekeepingTaskLogUI
                .displayMessage("Starting room number cannot be greater " + "than ending room number.");
          }
        }
      }

      // Step 5: Activity Sort Order
      int sortChoice = housekeepingTaskLogUI.getActivitySortChoice();
      if (sortChoice == 0) {
        return;
      }

      // Step 6: Apply Multiple Criteria Filters
      ListInterface<HousekeepingTask> filteredActivityList = new ArrayList<>();
      for (int i = 1; i <= taskLog.getNumberOfEntries(); i++) {
        HousekeepingTask task = taskLog.getEntry(i);
        boolean dateMatch = matchesDateFilter(task, dateFilterChoice, specificDate, startDate, endDate);
        boolean statusMatch = matchesStatusFilter(task, statusFilter);
        boolean roomMatch = matchesRoomFilter(task, roomFilterChoice, specificRoom, startRoom, endRoom);

        // ALL THREE criteria must match
        if (dateMatch && statusMatch && roomMatch) {
          filteredActivityList.add(task);
        }
      }

      // Step 7: Bubble Sort by Date / Time
      bubbleSortActivityList(filteredActivityList, sortChoice);

      // Step 8: Prepare Report Descriptions
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      String dateDescription = "";
      switch (dateFilterChoice) {
        case 1:
          dateDescription = "All Dates";
          break;
        case 2:
          dateDescription = specificDate.format(dateFormatter);
          break;
        case 3:
          dateDescription = startDate.format(dateFormatter) + " - " + endDate.format(dateFormatter);
          break;
      }

      String statusDescription;
      if (statusFilter.equals("ALL")) {
        statusDescription = "All Statuses";
      } else {
        statusDescription = statusFilter;
      }

      String roomDescription = "";
      switch (roomFilterChoice) {
        case 1:
          roomDescription = "All Rooms";
          break;
        case 2:
          roomDescription = "Room " + specificRoom;
          break;
        case 3:
          roomDescription = "Room " + startRoom + " - " + endRoom;
          break;
      }

      String sortDescription;
      if (sortChoice == 1) {
        sortDescription = "Oldest to Newest";
      } else {
        sortDescription = "Newest to Oldest";
      }

      // Step 9: Display Report Header
      housekeepingTaskLogUI.displayActivityReportHeader(dateDescription, statusDescription, roomDescription,
          sortDescription);

      // Step 10: No Matching Activities
      if (filteredActivityList.getNumberOfEntries() == 0) {
        housekeepingTaskLogUI.displayNoMatchingActivities();
        housekeepingTaskLogUI.displayActivityReportFooter(0, 0);
        int nextChoice = housekeepingTaskLogUI.getAfterActivityReportChoice();
        if (nextChoice == 0) {
          return;
        }
        // Generate another report
        continue;
      }

      // Step 11: Count Activity Types
      int dirtyCount = 0;
      int cleaningCount = 0;
      int inspectedCount = 0;
      int readyCount = 0;

      // Step 12: Display Activity Rows
      for (int i = 1; i <= filteredActivityList.getNumberOfEntries(); i++) {
        HousekeepingTask task = filteredActivityList.getEntry(i);
        housekeepingTaskLogUI.displayTaskLogRow(task.getTaskId(), task.getRoomNumber(), task.getStatus(),
            task.getTimestamp());
        switch (task.getStatus()) {
          case "Dirty":
            dirtyCount++;
            break;
          case "Cleaning In Progress":
            cleaningCount++;
            break;
          case "Inspected":
            inspectedCount++;
            break;
          case "Ready for Check-In":
            readyCount++;
            break;
        }
      }

      // Step 13: Count Unique Rooms Involved
      int roomsInvolved = countUniqueRooms(filteredActivityList);

      // Step 14: Report Footer
      housekeepingTaskLogUI.displayActivityReportFooter(filteredActivityList.getNumberOfEntries(), roomsInvolved);

      // Step 15: Activity Summary
      housekeepingTaskLogUI.displayActivitySummary(dirtyCount, cleaningCount, inspectedCount, readyCount);

      // Step 16: Generate Another / Back
      int nextChoice = housekeepingTaskLogUI.getAfterActivityReportChoice();
      if (nextChoice == 0) {
        return;
      }
    }
  }

}
