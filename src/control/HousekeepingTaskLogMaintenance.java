package control;

import adt.ArrayList;
import adt.ListInterface;
import adt.ArrayStack;
import adt.Condition;
import adt.StackInterface;
import boundary.HousekeepingTaskLogUI;
import dao.HousekeepingTaskDAO;
import dao.HousekeepingTaskInitializer;
import entity.HousekeepingTask;
import utility.MessageUI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.Comparator;

/**
 * @author Kat Tan
 */
public class HousekeepingTaskLogMaintenance {

  private static final int ROWS_PER_PAGE = 20;

  /** Loads saved tasks and prepares the rollback history. */
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

  /** Runs the Housekeeping Task Log menu until the user exits it. */
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
          runReportsMenu();
          break;
        case 0:
          break;
      }
    } while (choice != 0);
    housekeepingTaskDAO.saveToFile(taskLog, nextTaskNumber);
  }

  /** Creates the next unique task ID. */
  private String generateUniqueTaskId() {
    String taskId = String.format("HT%04d", nextTaskNumber);
    nextTaskNumber++;
    return taskId;
  }

  /** Finds the newest status record for the selected room. */
  private HousekeepingTask findLatestTaskByRoom(String roomNumber) {
    for (int i = taskLog.getNumberOfEntries(); i >= 1; i--) {
      HousekeepingTask task = taskLog.getEntry(i);
      if (task.getRoomNumber().equals(roomNumber)) {
        return task;
      }
    }
    return null;
  }

  /** Removes a task record from the task log by its ID. */
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

  /** Runs the submenu that lets the user choose a housekeeping report. */
  private void runReportsMenu() {
    int choice;
    do {
      choice = housekeepingTaskLogUI.getReportMenuChoice();
      switch (choice) {
        case 1:
          generateRoomStatusReport();
          break;
        case 2:
          generateHousekeepingActivityReport();
          break;
        case 0:
          break;
      }
    } while (choice != 0);
  }

  /**
   * Checks whether a new status follows the required housekeeping workflow.
   */
  private boolean isValidStatusTransition(String currentStatus, String newStatus) {
    if (currentStatus == null) {
      return true;
    }

    switch (currentStatus) {
      case "Dirty":
        return newStatus.equals("Cleaning In Progress");
      case "Cleaning In Progress":
        return newStatus.equals("Inspected");
      case "Inspected":
        return newStatus.equals("Ready for Check-In");
      case "Ready for Check-In":
        return newStatus.equals("Dirty");
      default:
        return false;
    }
  }

  /**
   * Explains the next status that is allowed for a room.
   */
  private String getExpectedNextStatus(String currentStatus) {
    if (currentStatus.equals("Ready for Check-In")) {
      return "Dirty";
    }
    if (currentStatus.equals("Dirty")) {
      return "Cleaning In Progress";
    }
    if (currentStatus.equals("Cleaning In Progress")) {
      return "Inspected";
    }
    if (currentStatus.equals("Inspected")) {
      return "Ready for Check-In";
    }
    return null;
  }

  /** Records the next valid housekeeping status for a room. */
  public void logStatusUpdate() {
    while (true) {
      housekeepingTaskLogUI.displayLogStatusUpdateHeader();

      // Step 1: Input room number
      String roomNumber = housekeepingTaskLogUI.inputRoomNumber();

      // null means the user entered 0 to cancel.
      if (roomNumber == null) {
        housekeepingTaskLogUI.displayMessage("Operation cancelled.");
        return;
      }

      // Step 2: Search latest housekeeping record
      HousekeepingTask currentTask = findLatestTaskByRoom(roomNumber);

      String newStatus;

      // Step 3: Existing rooms automatically move to the next status.
      if (currentTask != null) {
        housekeepingTaskLogUI.displayCurrentStatus(roomNumber, currentTask.getStatus());
        newStatus = getExpectedNextStatus(currentTask.getStatus());
        if (newStatus == null) {
          housekeepingTaskLogUI.displayMessage("Unable to determine the next status for this room.");
          return;
        }

        int updateChoice = housekeepingTaskLogUI.getUpdateChoice(newStatus);
        if (updateChoice == 0) {
          housekeepingTaskLogUI.displayMessage("Operation cancelled.");
          return;
        }
      } else {
        // A new room may be recorded at any current status.
        housekeepingTaskLogUI.displayNoExistingRecord(roomNumber);
        newStatus = housekeepingTaskLogUI.inputStatus();

        if (newStatus == null) {
          housekeepingTaskLogUI.displayMessage("Operation cancelled.");
          return;
        }
      }

      // Reject an update that does not actually change anything. Logging the
      // status a room is already in would add a duplicate entry that clutters
      // the task log and distorts the activity report.
      if (currentTask != null && newStatus.equals(currentTask.getStatus())) {
        housekeepingTaskLogUI.displayMessage(
            "Room " + roomNumber + " is already marked as \"" + newStatus + "\"."
                + "\nNo update logged - please choose a different status.");

        int retryChoice = housekeepingTaskLogUI.getAfterUpdateChoice();
        if (retryChoice == 0) {
          return;
        }
        continue;
      }

      String currentStatus = currentTask == null ? null : currentTask.getStatus();
      if (!isValidStatusTransition(currentStatus, newStatus)) {
        housekeepingTaskLogUI.displayMessage(
            "Invalid status transition. The next status must be: "
                + getExpectedNextStatus(currentStatus) + ".");

        int retryChoice = housekeepingTaskLogUI.getAfterUpdateChoice();
        if (retryChoice == 0) {
          return;
        }
        continue;
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

  /** Removes the most recently logged status update after confirmation. */
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

  /** Displays every saved housekeeping task in chronological order. */
  public void displayTaskLog() {
    if (taskLog.getNumberOfEntries() == 0) {
      housekeepingTaskLogUI.displayEmptyTaskLog();
      housekeepingTaskLogUI.pressEnterToContinue();
      return;
    }

    int totalRecords = taskLog.getNumberOfEntries();
    int totalPages = (totalRecords + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
    int currentPage = 1;

    while (true) {
      int firstRow = (currentPage - 1) * ROWS_PER_PAGE + 1;
      int lastRow = Math.min(currentPage * ROWS_PER_PAGE, totalRecords);

      housekeepingTaskLogUI.displayTaskLogHeader();
      for (int i = firstRow; i <= lastRow; i++) {
        HousekeepingTask task = taskLog.getEntry(i);
        housekeepingTaskLogUI.displayTaskLogRow(
            task.getTaskId(),
            task.getRoomNumber(),
            task.getStatus(),
            task.getTimestamp());
      }
      housekeepingTaskLogUI.displayTaskLogFooter(totalRecords);
      housekeepingTaskLogUI.displayPageInfo(firstRow, lastRow, totalRecords, currentPage, totalPages);

      if (totalPages == 1) {
        housekeepingTaskLogUI.pressEnterToContinue();
        return;
      }

      int nextPage = housekeepingTaskLogUI.getPageChoice(currentPage, totalPages);
      if (nextPage == 0) {
        return;
      }
      currentPage = nextPage;
    }
  }

  /** Searches for the first housekeeping task that matches the given Task ID using the List ADT search operation. */
  private HousekeepingTask searchTaskById(String taskId) {
    return taskLog.search(new Condition<HousekeepingTask>() {
      @Override
      public boolean isSatisfiedBy(HousekeepingTask task) {
        return task.getTaskId().equalsIgnoreCase(taskId);
      }
    });
  }

  /** Searches all housekeeping tasks for a given room number using the List ADT filter operation. */
  private ListInterface<HousekeepingTask> searchTasksByRoom(String roomNumber) {
    return taskLog.filter(new Condition<HousekeepingTask>() {
      @Override
      public boolean isSatisfiedBy(HousekeepingTask task) {
        return task.getRoomNumber().equals(roomNumber);
      }
    });
  }

  /** Searches all housekeeping tasks for a given status using the List ADT filter operation. */
  private ListInterface<HousekeepingTask> searchTasksByStatus(String status) {
    return taskLog.filter(new Condition<HousekeepingTask>() {
      @Override
      public boolean isSatisfiedBy(HousekeepingTask task) {
        return status.equalsIgnoreCase("ALL")
            || task.getStatus().equalsIgnoreCase(status);
      }
    });
  }

  /** Counts housekeeping tasks with the selected status using the List ADT countIf operation. */
  private int countStatusInList(ListInterface<HousekeepingTask> taskList, String status) {
    return taskList.countIf(new Condition<HousekeepingTask>() {
      @Override
      public boolean isSatisfiedBy(HousekeepingTask task) {
        return task.getStatus().equalsIgnoreCase(status);
      }
    });
  }

  /** Searches task records by task ID, room number, or status. */
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
          // Step 1: Validate that the entered Task ID exists using the List ADT search operation.
          while (true) {
            searchTaskId = housekeepingTaskLogUI.inputSearchTaskId();

            // null means the user entered 0 to cancel.
            if (searchTaskId == null) {
              break;
            }

            if (searchTaskById(searchTaskId) == null) {
              housekeepingTaskLogUI.displayMessage(
                  "No housekeeping task record found for Task ID " + searchTaskId
                      + ". Please enter another Task ID.");
              continue;
            }

            break;
          }

          if (searchTaskId == null) {
            continue;
          }

          searchDescription = "Task ID = " + searchTaskId;
          break;
        case 2:
          // Step 2: Validate that the entered Room Number exists using the List ADT filter operation.
          while (true) {
            searchRoomNumber = housekeepingTaskLogUI.inputRoomNumber();

            // null means the user entered 0 to cancel.
            if (searchRoomNumber == null) {
              break;
            }

            if (searchTasksByRoom(searchRoomNumber).getNumberOfEntries() == 0) {
              housekeepingTaskLogUI.displayMessage(
                  "No housekeeping task records found for Room Number " + searchRoomNumber
                      + ". Please enter another Room Number.");
              continue;
            }

            break;
          }

          if (searchRoomNumber == null) {
            continue;
          }

          searchDescription = "Room Number = " + searchRoomNumber;
          break;
        case 3:
          searchStatus = housekeepingTaskLogUI.inputSearchStatus();
          if (searchStatus == null) {
            continue;
          }
          searchDescription = searchStatus.equalsIgnoreCase("ALL")
              ? "Status = All Statuses"
              : "Status = " + searchStatus;
          break;
      }

      ListInterface<HousekeepingTask> searchResults;

      // Apply the selected search using the List ADT search/filter operations.
      if (searchChoice == 1) {
        searchResults = new ArrayList<>();
        HousekeepingTask task = searchTaskById(searchTaskId);
        if (task != null) {
          searchResults.add(task);
        }
      } else if (searchChoice == 2) {
        searchResults = searchTasksByRoom(searchRoomNumber);

        // Sort room search results by date so the latest status is identified from the last record.
        searchResults.sort(Comparator.comparing(HousekeepingTask::getTimestamp));
      } else {
        searchResults = searchTasksByStatus(searchStatus);
      }

      int totalFound = searchResults.getNumberOfEntries();
      int totalPages = (totalFound + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
      int currentPage = 1;

      // Display search results using the same pagination style as the task log and reports.
      while (true) {
        int firstRow = (currentPage - 1) * ROWS_PER_PAGE + 1;
        int lastRow = Math.min(currentPage * ROWS_PER_PAGE, totalFound);

        housekeepingTaskLogUI.displaySearchResultHeader(searchDescription);

        for (int i = firstRow; i <= lastRow; i++) {
          HousekeepingTask task = searchResults.getEntry(i);
          housekeepingTaskLogUI.displayTaskLogRow(
              task.getTaskId(),
              task.getRoomNumber(),
              task.getStatus(),
              task.getTimestamp());
        }

        if (totalFound == 0) {
          if (searchChoice == 2) {
            housekeepingTaskLogUI.displayMessage(
                "No housekeeping task records found for Room Number " + searchRoomNumber + ".");
          } else if (searchChoice == 3) {
            housekeepingTaskLogUI.displayMessage(
                "No housekeeping task records found for Status " + searchStatus + ".");
          } else {
            housekeepingTaskLogUI.displayMessage(
                "No housekeeping task record found for Task ID " + searchTaskId + ".");
          }
        }

        housekeepingTaskLogUI.displaySearchResultFooter(totalFound);

        // Do not display pagination information when no search records are found.
        if (totalFound == 0) {
          break;
        }

        housekeepingTaskLogUI.displayPageInfo(firstRow, lastRow, totalFound, currentPage, totalPages);

        if (totalPages <= 1) {
          break;
        }

        int nextPage = housekeepingTaskLogUI.getPageChoice(currentPage, totalPages);
        if (nextPage == 0) {
          break;
        }
        currentPage = nextPage;
      }

      // Display the latest status once after all search pages have been viewed.
      if (searchChoice == 2 && totalFound > 0) {
        HousekeepingTask currentTask = searchResults.getEntry(totalFound);
        housekeepingTaskLogUI.displayCurrentRoomStatus(
            currentTask.getRoomNumber(),
            currentTask.getStatus(),
            currentTask.getTimestamp());
      }

      int nextChoice = housekeepingTaskLogUI.getAfterSearchChoice();

      if (nextChoice == 0) {
        return;
      }
    }
  }

  /** Builds a list containing only the latest task for each room. */
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

  /** Checks whether a task matches the selected status filter. */

  /** Checks whether a task matches the selected room filter. */

  /** Sorts room records by room number using the List ADT sort operation. */
  private void sortRoomList(ListInterface<HousekeepingTask> roomList, int sortChoice) {
    // Ascending: room number, then latest timestamp, then Task ID.
    if (sortChoice == 1) {
      roomList.sort(
          Comparator.comparingInt((HousekeepingTask task) -> Integer.parseInt(task.getRoomNumber()))
              .thenComparing(HousekeepingTask::getTimestamp)
              .thenComparing(HousekeepingTask::getTaskId));
    }
    // Descending: room number, then latest timestamp, then Task ID.
    else {
      roomList.sort(
          Comparator.comparingInt((HousekeepingTask task) -> Integer.parseInt(task.getRoomNumber()))
              .reversed()
              .thenComparing(HousekeepingTask::getTimestamp, Comparator.reverseOrder())
              .thenComparing(HousekeepingTask::getTaskId, Comparator.reverseOrder()));
    }
  }

  /** Generates a report showing the latest status of matching rooms. */
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
        boolean validSpecificRoom = false;
        while (!validSpecificRoom) {
          specificRoom = housekeepingTaskLogUI.inputRoomNumber();
          // null means the user entered 0 to cancel.
          if (specificRoom == null) {
            return;
          }

          // Step 4a: Validate that the selected Room Number exists using the List ADT filter operation.
          ListInterface<HousekeepingTask> roomRecords = searchTasksByRoom(specificRoom);
          if (roomRecords.getNumberOfEntries() == 0) {
            housekeepingTaskLogUI.displayMessage(
                "No housekeeping records found for Room Number " + specificRoom
                    + ". Please enter another Room Number.");
          } else {
            validSpecificRoom = true;
          }
        }
      }

      // Room Number Range
      else if (roomFilterChoice == 3) {
        boolean validRange = false;
        while (!validRange) {
          Integer startRoomInput = housekeepingTaskLogUI.inputRoomRange("Enter Starting Room Number: ");
          if (startRoomInput == null) {
            return;
          }
          Integer endRoomInput = housekeepingTaskLogUI.inputRoomRange("Enter Ending Room Number: ");
          if (endRoomInput == null) {
            return;
          }
          startRoom = startRoomInput;
          endRoom = endRoomInput;
          if (startRoom <= endRoom) {
            // Step 4b: Validate that the selected Room Number Range contains existing records using the List ADT filter operation.
            final int selectedStartRoom = startRoom;
            final int selectedEndRoom = endRoom;
            ListInterface<HousekeepingTask> rangeRecords = taskLog.filter(new Condition<HousekeepingTask>() {
              @Override
              public boolean isSatisfiedBy(HousekeepingTask task) {
                int roomNumber = Integer.parseInt(task.getRoomNumber());
                return roomNumber >= selectedStartRoom && roomNumber <= selectedEndRoom;
              }
            });

            if (rangeRecords.getNumberOfEntries() == 0) {
              housekeepingTaskLogUI.displayMessage(
                  "No housekeeping records found for Room Range " + startRoom + " - " + endRoom
                      + ". Please enter another Room Range.");
            } else {
              validRange = true;
            }
          } else {
            housekeepingTaskLogUI
                .displayMessage("Starting room number cannot be greater " + "than ending room number.");
          }
        }
      }

      // Step 5: Apply Status Filter using the List ADT filter operation.
      ListInterface<HousekeepingTask> filteredRoomList = currentRoomList;
      if (!statusFilter.equals("ALL")) {
        filteredRoomList = filteredRoomList.filter(new Condition<HousekeepingTask>() {
          @Override
          public boolean isSatisfiedBy(HousekeepingTask task) {
            return task.getStatus().equalsIgnoreCase(statusFilter);
          }
        });
      }

      // Step 6: Apply Room Filter using the List ADT filter operation.
      if (roomFilterChoice == 2) {
        final String selectedRoom = specificRoom;
        filteredRoomList = filteredRoomList.filter(new Condition<HousekeepingTask>() {
          @Override
          public boolean isSatisfiedBy(HousekeepingTask task) {
            return task.getRoomNumber().equals(selectedRoom);
          }
        });
      } else if (roomFilterChoice == 3) {
        final int selectedStartRoom = startRoom;
        final int selectedEndRoom = endRoom;
        filteredRoomList = filteredRoomList.filter(new Condition<HousekeepingTask>() {
          @Override
          public boolean isSatisfiedBy(HousekeepingTask task) {
            int roomNumber = Integer.parseInt(task.getRoomNumber());
            return roomNumber >= selectedStartRoom && roomNumber <= selectedEndRoom;
          }
        });
      }

      // Step 7: Sort Order
      int sortChoice = housekeepingTaskLogUI.getRoomSortChoice();
      if (sortChoice == 0) {
        return;
      }

      // Step 8: Sort using the List ADT sort operation.
      sortRoomList(filteredRoomList, sortChoice);

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

      // Step 10: Handle No Matching Records
      if (filteredRoomList.getNumberOfEntries() == 0) {
        housekeepingTaskLogUI.displayRoomStatusReportHeader(statusDescription, roomDescription, sortDescription);
        housekeepingTaskLogUI.displayNoMatchingRooms();
        housekeepingTaskLogUI.displayRoomStatusReportFooter(0);
        housekeepingTaskLogUI.displayRoomStatusReportEnd();
        int nextChoice = housekeepingTaskLogUI.getAfterRoomStatusReportChoice();
        if (nextChoice == 0) {
          return;
        }
        // User selected Generate Another Report
        continue;
      }

      // Step 11: Count current statuses for the report summary using the List ADT countIf operation.
      int dirtyCount = countStatusInList(filteredRoomList, "Dirty");
      int cleaningCount = countStatusInList(filteredRoomList, "Cleaning In Progress");
      int inspectedCount = countStatusInList(filteredRoomList, "Inspected");
      int readyCount = countStatusInList(filteredRoomList, "Ready for Check-In");

      int totalRooms = filteredRoomList.getNumberOfEntries();
      int totalPages = (totalRooms + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
      int currentPage = 1;
      while (true) {
        int firstRow = (currentPage - 1) * ROWS_PER_PAGE + 1;
        int lastRow = Math.min(currentPage * ROWS_PER_PAGE, totalRooms);

        housekeepingTaskLogUI.displayRoomStatusReportHeader(statusDescription, roomDescription, sortDescription);
        for (int i = firstRow; i <= lastRow; i++) {
          HousekeepingTask task = filteredRoomList.getEntry(i);
          housekeepingTaskLogUI.displayRoomStatusReportRow(task.getRoomNumber(), task.getStatus(), task.getTaskId(),
              task.getTimestamp());
        }
        housekeepingTaskLogUI.displayRoomStatusReportFooter(totalRooms);
        housekeepingTaskLogUI.displayPageInfo(firstRow, lastRow, totalRooms, currentPage, totalPages);

        if (statusFilter.equals("ALL")) {
          housekeepingTaskLogUI.displayRoomStatusSummary(dirtyCount, cleaningCount, inspectedCount, readyCount, totalRooms);
        }
        housekeepingTaskLogUI.displayRoomStatusReportEnd();

        if (totalPages == 1) {
          break;
        }
        int nextPage = housekeepingTaskLogUI.getPageChoice(currentPage, totalPages);
        if (nextPage == 0) {
          break;
        }
        currentPage = nextPage;
      }

      // Step 14: Generate Another / Back
      int nextChoice = housekeepingTaskLogUI.getAfterRoomStatusReportChoice();

      if (nextChoice == 0) {
        return;
      }
    }
  }

  /** Checks whether a task timestamp matches the selected date filter. */

  /** Sorts activity records by timestamp using the List ADT sort operation. */
  private void sortActivityList(ListInterface<HousekeepingTask> activityList, int sortChoice) {
    // Oldest to Newest: timestamp, then room number, then Task ID.
    if (sortChoice == 1) {
      activityList.sort(
          Comparator.comparing(HousekeepingTask::getTimestamp)
              .thenComparingInt(task -> Integer.parseInt(task.getRoomNumber()))
              .thenComparing(HousekeepingTask::getTaskId));
    }
    // Newest to Oldest: timestamp, then room number, then Task ID.
    else {
      activityList.sort(
          Comparator.comparing(HousekeepingTask::getTimestamp, Comparator.reverseOrder())
              .thenComparingInt(task -> Integer.parseInt(task.getRoomNumber()))
              .thenComparing(HousekeepingTask::getTaskId));
    }
  }

  /** Counts the different room numbers in an activity list. */
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

  /** Generates a filtered report of housekeeping status updates. */
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
        if (specificDate == null) {
          return;
        }
      }

      // Date Range
      else if (dateFilterChoice == 3) {
        boolean validDateRange = false;
        while (!validDateRange) {
          startDate = housekeepingTaskLogUI.inputActivityDate("Enter Start Date (dd/MM/yyyy): ");
          if (startDate == null) {
            return;
          }
          endDate = housekeepingTaskLogUI.inputActivityDate("Enter End Date (dd/MM/yyyy): ");
          if (endDate == null) {
            return;
          }
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
        boolean validSpecificRoom = false;
        while (!validSpecificRoom) {
          specificRoom = housekeepingTaskLogUI.inputRoomNumber();

          // null means the user entered 0 to cancel.
          if (specificRoom == null) {
            return;
          }

          // Validate that the selected Room Number exists.
          ListInterface<HousekeepingTask> roomRecords = searchTasksByRoom(specificRoom);
          if (roomRecords.getNumberOfEntries() == 0) {
            housekeepingTaskLogUI.displayMessage(
                "No housekeeping records found for Room Number " + specificRoom
                    + ". Please enter another Room Number.");
          } else {
            validSpecificRoom = true;
          }
        }
      }

      // Room Number Range
      else if (roomFilterChoice == 3) {
        boolean validRoomRange = false;
        while (!validRoomRange) {
          Integer startRoomInput = housekeepingTaskLogUI.inputRoomRange("Enter Starting Room Number: ");
          if (startRoomInput == null) {
            return;
          }
          Integer endRoomInput = housekeepingTaskLogUI.inputRoomRange("Enter Ending Room Number: ");
          if (endRoomInput == null) {
            return;
          }
          startRoom = startRoomInput;
          endRoom = endRoomInput;
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

      // Step 6: Apply Date Filter using the List ADT filter operation.
      ListInterface<HousekeepingTask> filteredActivityList = taskLog;
      if (dateFilterChoice == 2) {
        final LocalDate selectedDate = specificDate;
        filteredActivityList = filteredActivityList.filter(new Condition<HousekeepingTask>() {
          @Override
          public boolean isSatisfiedBy(HousekeepingTask task) {
            return task.getTimestamp().toLocalDate().equals(selectedDate);
          }
        });
      } else if (dateFilterChoice == 3) {
        final LocalDate selectedStartDate = startDate;
        final LocalDate selectedEndDate = endDate;
        filteredActivityList = filteredActivityList.filter(new Condition<HousekeepingTask>() {
          @Override
          public boolean isSatisfiedBy(HousekeepingTask task) {
            LocalDate taskDate = task.getTimestamp().toLocalDate();
            return !taskDate.isBefore(selectedStartDate) && !taskDate.isAfter(selectedEndDate);
          }
        });
      }

      // Step 7: Apply Status Filter using the List ADT filter operation.
      if (!statusFilter.equals("ALL")) {
        filteredActivityList = filteredActivityList.filter(new Condition<HousekeepingTask>() {
          @Override
          public boolean isSatisfiedBy(HousekeepingTask task) {
            return task.getStatus().equalsIgnoreCase(statusFilter);
          }
        });
      }

      // Step 8: Apply Room Filter using the List ADT filter operation.
      if (roomFilterChoice == 2) {
        final String selectedRoom = specificRoom;
        filteredActivityList = filteredActivityList.filter(new Condition<HousekeepingTask>() {
          @Override
          public boolean isSatisfiedBy(HousekeepingTask task) {
            return task.getRoomNumber().equals(selectedRoom);
          }
        });
      } else if (roomFilterChoice == 3) {
        final int selectedStartRoom = startRoom;
        final int selectedEndRoom = endRoom;
        filteredActivityList = filteredActivityList.filter(new Condition<HousekeepingTask>() {
          @Override
          public boolean isSatisfiedBy(HousekeepingTask task) {
            int roomNumber = Integer.parseInt(task.getRoomNumber());
            return roomNumber >= selectedStartRoom && roomNumber <= selectedEndRoom;
          }
        });
      }

      // Step 9: Sort by Date / Time using the List ADT sort operation.
      sortActivityList(filteredActivityList, sortChoice);

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

      // Step 10: No Matching Activities
      if (filteredActivityList.getNumberOfEntries() == 0) {
        housekeepingTaskLogUI.displayActivityReportHeader(dateDescription, statusDescription, roomDescription,
            sortDescription);
        housekeepingTaskLogUI.displayNoMatchingActivities();
        housekeepingTaskLogUI.displayActivityReportFooter(0, 0);
        housekeepingTaskLogUI.displayActivityReportEnd();
        int nextChoice = housekeepingTaskLogUI.getAfterActivityReportChoice();
        if (nextChoice == 0) {
          return;
        }
        // Generate another report
        continue;
      }

      // Step 11: Count Activity Types using the List ADT countIf operation.
      int dirtyCount = countStatusInList(filteredActivityList, "Dirty");
      int cleaningCount = countStatusInList(filteredActivityList, "Cleaning In Progress");
      int inspectedCount = countStatusInList(filteredActivityList, "Inspected");
      int readyCount = countStatusInList(filteredActivityList, "Ready for Check-In");

      // Step 13: Count Unique Rooms Involved
      int roomsInvolved = countUniqueRooms(filteredActivityList);

      int totalActivities = filteredActivityList.getNumberOfEntries();
      int totalPages = (totalActivities + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
      int currentPage = 1;
      while (true) {
        int firstRow = (currentPage - 1) * ROWS_PER_PAGE + 1;
        int lastRow = Math.min(currentPage * ROWS_PER_PAGE, totalActivities);

        housekeepingTaskLogUI.displayActivityReportHeader(dateDescription, statusDescription, roomDescription,
            sortDescription);
        for (int i = firstRow; i <= lastRow; i++) {
          HousekeepingTask task = filteredActivityList.getEntry(i);
          housekeepingTaskLogUI.displayTaskLogRow(task.getTaskId(), task.getRoomNumber(), task.getStatus(),
              task.getTimestamp());
        }
        housekeepingTaskLogUI.displayActivityReportFooter(totalActivities, roomsInvolved);
        housekeepingTaskLogUI.displayPageInfo(firstRow, lastRow, totalActivities, currentPage, totalPages);
        housekeepingTaskLogUI.displayActivitySummary(dirtyCount, cleaningCount, inspectedCount, readyCount);
        housekeepingTaskLogUI.displayActivityReportEnd();

        if (totalPages == 1) {
          break;
        }
        int nextPage = housekeepingTaskLogUI.getPageChoice(currentPage, totalPages);
        if (nextPage == 0) {
          break;
        }
        currentPage = nextPage;
      }

      // Step 16: Generate Another / Back
      int nextChoice = housekeepingTaskLogUI.getAfterActivityReportChoice();
      if (nextChoice == 0) {
        return;
      }
    }
  }

}
