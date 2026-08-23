package boundary;

import java.util.Scanner;
import utility.MessageUI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 *
 * @author Kat Tan
 */
public class HousekeepingTaskLogUI {

    private Scanner scanner = MessageUI.scanner;
    private static final int MIN_ROOM_NUMBER = 1;
    private static final int MAX_ROOM_NUMBER = 9999;
    private static final DateTimeFormatter REPORT_TIMESTAMP
            = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, hh:mm a");

    /** Displays the housekeeping menu and reads the user's choice. */
    public int getMenuChoice() {
        MessageUI.clearScreen();
        System.out.println();

        MessageUI.displayBoxTop();
        MessageUI.displayBoxBlank();
        MessageUI.displayBoxCentred("H O U S E K E E P I N G   T A S K   L O G");
        MessageUI.displayBoxBlank();
        MessageUI.displayBoxDivider();
        MessageUI.displayBoxLine("  Main Menu  >  Housekeeping Task Log");
        MessageUI.displayBoxDivider();
        MessageUI.displayBoxBlank();
        MessageUI.displayMenuOption(1, "Log new task status update");
        MessageUI.displayMenuOption(2, "Rollback last status update");
        MessageUI.displayMenuOption(3, "Display task log");
        MessageUI.displayMenuOption(4, "Search Housekeeping Task");
        MessageUI.displayMenuOption(5, "Reports");
        MessageUI.displayBoxBlank();
        MessageUI.displayMenuOption(0, "Back to main menu");
        MessageUI.displayBoxBlank();
        MessageUI.displayBoxBottom();

        return MessageUI.readMenuChoice(scanner, 5, "go back to the main menu");
    }

    /**
     * Draws the framed title every action in this module starts with, matching
     * the layout used across the rest of the system.
     *
     * @param title the name of the action being started
     */
    /** Displays a common heading for a housekeeping action. */
    private void displayActionHeader(String title) {
        MessageUI.clearScreen();
        System.out.println();
        MessageUI.displayBoxTop();
        MessageUI.displayBoxCentred(title);
        MessageUI.displayBoxBottom();
    }

    // ==============================
    // 1. Log New Task Status Update
    // ==============================
    /** Displays the heading for a new status update. */
    public void displayLogStatusUpdateHeader() {
        displayActionHeader("LOG NEW TASK STATUS UPDATE");
    }

    /**
     * @return the room number, or null if the user enters 0 to cancel
     */
    public String inputRoomNumber() {
        while (true) {
            System.out.print("Enter Room Number (0 to cancel): ");
            String roomNumber = MessageUI.readLine(scanner);

            if (roomNumber.equals("0")) {
                return null;
            }

            if (roomNumber.isEmpty()) {
                System.out.println("Room number cannot be empty.");
            } else if (!roomNumber.matches("\\d+")) {
                System.out.println("Room number must contain numbers only.");
            } else {
                try {
                    int parsedRoomNumber = Integer.parseInt(roomNumber);
                    if (parsedRoomNumber < MIN_ROOM_NUMBER || parsedRoomNumber > MAX_ROOM_NUMBER) {
                        System.out.println("Room number must be between 1 and 9999.");
                    } else {
                        return String.valueOf(parsedRoomNumber);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Room number must be between 1 and 9999.");
                }
            }
        }
    }

    /** Shows the current status of a room. */
    public void displayCurrentStatus(String roomNumber, String status) {
        System.out.println(
                "\nHousekeeping record found for Room "
                        + roomNumber + ".");
        System.out.println("Current Status: " + status);
    }

    /** Informs the user that a room has no previous task record. */
    public void displayNoExistingRecord(String roomNumber) {
        System.out.println(
                "\nNo existing housekeeping record found for Room "
                        + roomNumber + ".");
    }

    /** Reads whether the user wants to apply the next room status. */
    public int getUpdateChoice(String nextStatus) {
        System.out.println("\n  [1]  Update Status to " + nextStatus);
        System.out.println("  [0]  Cancel");

        return MessageUI.readMenuChoice(scanner, 1, "cancel");
    }

    /** Displays available statuses and returns the selected status. */
    public String inputStatus() {
        System.out.println("\nSelect New Status:");
        System.out.println("  [1]  Dirty");
        System.out.println("  [2]  Cleaning In Progress");
        System.out.println("  [3]  Inspected");
        System.out.println("  [4]  Ready for Check-In");
        System.out.println("  [0]  Cancel");

        int statusChoice = MessageUI.readMenuChoice(scanner, 4, "cancel");

        switch (statusChoice) {
            case 1:
                return "Dirty";
            case 2:
                return "Cleaning In Progress";
            case 3:
                return "Inspected";
            case 4:
                return "Ready for Check-In";
            case 0:
                return null;
            default:
                return null;
        }
    }

    /** Reads whether the user wants to log another update. */
    public int getAfterUpdateChoice() {

        System.out.println("\nWhat would you like to do next?");
        System.out.println("  [1]  Log Another Status Update");
        System.out.println("  [0]  Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to the Housekeeping Task Log menu");
    }

    // ========================
    // 2. Rollback Last Status
    // ========================
    /** Displays the heading for the rollback action. */
    public void displayRollbackHeader() {
        displayActionHeader("ROLLBACK LAST STATUS UPDATE");
    }

    /** Shows the task that will be rolled back. */
    public void displayLastStatusUpdate(String taskId, String roomNumber, String status) {

        System.out.println("\nLast Status Update:");
        System.out.println("Task ID     : " + taskId);
        System.out.println("Room Number : " + roomNumber);
        System.out.println("Status      : " + status);
    }

    /** Shows the status that a room will have after rollback. */
    public void displayPreviousStatus(String previousStatus) {
        if (previousStatus == null) {
            System.out.println(
                    "\nPrevious Status: No previous housekeeping status");
        } else {
            System.out.println(
                    "\nPrevious Status: " + previousStatus);
        }
    }

    /** Reads the user's rollback confirmation. */
    public int getRollbackConfirmation() {
        System.out.println("\n  [1]  Confirm Rollback");
        System.out.println("  [0]  Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "cancel");
    }

    /** Reads whether the user wants to perform another rollback. */
    public int getAfterRollbackChoice() {
        System.out.println("\nWhat would you like to do next?");
        System.out.println("  [1]  Rollback Another Status Update");
        System.out.println("  [0]  Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to the Housekeeping Task Log menu");
    }

    // ====================
    // 3. Display Task Log
    // ====================
    /** Displays the details of a successful rollback. */
    public void displayRollbackSuccess(String taskId, String roomNumber, String rolledBackStatus,
            String currentStatus) {
        MessageUI.clearScreen();

        System.out.println("\nStatus rollback successful.");
        System.out.println("\nRolled Back:");
        System.out.println("Task ID     : " + taskId);
        System.out.println("Room Number : " + roomNumber);
        System.out.println("Status      : " + rolledBackStatus);

        if (currentStatus == null) {
            System.out.println("\nCurrent Status: No housekeeping status");
        } else {
            System.out.println("\nCurrent Status: " + currentStatus);
        }
    }

    /** Displays the column headings for the task log. */
    public void displayTaskLogHeader() {
        MessageUI.clearScreen();
        System.out.println("\n================================================================================");
        System.out.println("                           HOUSEKEEPING TASK LOG");
        System.out.println("================================================================================");
        System.out.printf(
                "%-12s %-15s %-24s %-20s%n",
                "Task ID",
                "Room Number",
                "Status",
                "Date / Time");
        System.out.println("--------------------------------------------------------------------------------");
    }

    /** Displays one task record in a table row. */
    public void displayTaskLogRow(String taskId, String roomNumber, String status, LocalDateTime timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedDateTime = timestamp.format(formatter);
        System.out.printf(
                "%-12s %-15s %-24s %-20s%n",
                taskId,
                roomNumber,
                status,
                formattedDateTime);
    }

    /** Displays the total number of task records. */
    public void displayTaskLogFooter(int totalRecords) {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Total Records: " + totalRecords);
    }

    /** Displays a message when the task log has no records. */
    public void displayEmptyTaskLog() {
        MessageUI.clearScreen();
        System.out.println("\n================================================================================");
        System.out.println("                           HOUSEKEEPING TASK LOG");
        System.out.println("================================================================================");
        System.out.println("\nNo housekeeping task records available.");
    }

    // ===========================
    // 4. Search Housekeeping Task
    // ===========================
    /** Displays search options and reads the selected search type. */
    public int getSearchChoice() {
        displayActionHeader("SEARCH HOUSEKEEPING TASK");
        System.out.println("  [1]  Search by Task ID");
        System.out.println("  [2]  Search by Room Number");
        System.out.println("  [3]  Search by Status");
        System.out.println("  [0]  Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                3,
                "go back to the Housekeeping Task Log menu");
    }

    /**
     * @return the task ID, or null if the user enters 0 to cancel
     */
    public String inputSearchTaskId() {
        while (true) {
            System.out.print("Enter Task ID (0 to cancel): ");
            String taskId = MessageUI.readLine(scanner).toUpperCase();

            if (taskId.equals("0")) {
                return null;
            }

            if (taskId.isEmpty()) {
                System.out.println("Task ID cannot be empty.");
            } else if (!taskId.matches("HT\\d{4,}")) {
                System.out.println("Invalid Task ID format. Example: HT0001");
            } else {
                return taskId;
            }
        }
    }

    /** Displays status options and reads a status for searching. */
    public String inputSearchStatus() {
        System.out.println("\nSelect Status:");
        System.out.println("  [1]  All Statuses");
        System.out.println("  [2]  Dirty");
        System.out.println("  [3]  Cleaning In Progress");
        System.out.println("  [4]  Inspected");
        System.out.println("  [5]  Ready for Check-In");
        System.out.println("  [0]  Cancel");

        int choice = MessageUI.readMenuChoice(scanner, 5, "cancel");
        switch (choice) {
            case 1:
                return "ALL";
            case 2:
                return "Dirty";
            case 3:
                return "Cleaning In Progress";
            case 4:
                return "Inspected";
            case 5:
                return "Ready for Check-In";
            case 0:
                return null;
            default:
                return null;
        }
    }

    /** Displays the heading and criteria for search results. */
    public void displaySearchResultHeader(String searchDescription) {
        MessageUI.clearScreen();

        System.out.println(
                "\n================================================================================");

        System.out.println(
                "                         HOUSEKEEPING SEARCH RESULTS");

        System.out.println(
                "================================================================================");

        System.out.println(
                "Search Criteria: " + searchDescription);

        System.out.println(
                "--------------------------------------------------------------------------------");

        System.out.printf(
                "%-12s %-15s %-24s %-20s%n",
                "Task ID",
                "Room Number",
                "Status",
                "Date / Time");

        System.out.println(
                "--------------------------------------------------------------------------------");
    }

    /** Displays the current status of a searched room. */
    public void displayCurrentRoomStatus(String roomNumber, String status, LocalDateTime timestamp) {
        System.out.println("\nCURRENT ROOM STATUS");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Room Number : " + roomNumber);
        System.out.println("Current Status : " + status);
        System.out.println("Last Updated : " + timestamp.format(REPORT_TIMESTAMP));
    }

    /** Displays the number of matching search records. */
    public void displaySearchResultFooter(int totalFound) {
        System.out.println(
                "--------------------------------------------------------------------------------");
        System.out.println(
                "Total Records Found: " + totalFound);
    }

    /** Reads whether the user wants to search again. */
    public int getAfterSearchChoice() {
        System.out.println("\nWhat would you like to do next?");
        System.out.println("  [1]  Search Again");
        System.out.println("  [0]  Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to the Housekeeping Task Log menu");
    }

    // =====================
    // 5. Reports
    // =====================
    /** Displays report options and reads the selected report. */
    public int getReportMenuChoice() {
        displayActionHeader("HOUSEKEEPING REPORTS");
        System.out.println("  [1]  Room Status Report");
        System.out.println("  [2]  Housekeeping Activity Report");
        System.out.println("  [0]  Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                2,
                "go back to the Housekeeping Task Log menu");
    }

    // ===============================
    // 5.1 Generate Room Status Report
    // ===============================
    /** Reads the status filter for the room status report. */
    public String getRoomStatusFilter() {
        displayActionHeader("GENERATE ROOM STATUS REPORT");
        System.out.println("\nFilter by Status:");
        System.out.println("  [1]  All Statuses");
        System.out.println("  [2]  Dirty");
        System.out.println("  [3]  Cleaning In Progress");
        System.out.println("  [4]  Inspected");
        System.out.println("  [5]  Ready for Check-In");
        System.out.println("  [0]  Cancel");

        int choice = MessageUI.readMenuChoice(
                scanner,
                5,
                "cancel");

        switch (choice) {
            case 1:
                return "ALL";
            case 2:
                return "Dirty";
            case 3:
                return "Cleaning In Progress";
            case 4:
                return "Inspected";
            case 5:
                return "Ready for Check-In";
            case 0:
                return null;
            default:
                return null;
        }
    }

    /** Reads the room filter type for a report. */
    public int getRoomFilterChoice() {
        MessageUI.clearScreen();
        System.out.println("\nFilter by Room:");
        System.out.println("  [1]  All Rooms");
        System.out.println("  [2]  Specific Room");
        System.out.println("  [3]  Room Number Range");
        System.out.println("  [0]  Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                3,
                "cancel");
    }

    /**
     * @return a room number for a range filter, or {@code null} when cancelled
     */
    public Integer inputRoomRange(String prompt) {
        while (true) {

            System.out.print(prompt);

            String input = MessageUI.readLine(scanner);

            if (input.equals("0")) {
                return null;
            } else if (input.isEmpty()) {

                System.out.println(
                        "Room number cannot be empty.");

            } else if (!input.matches("\\d+")) {

                System.out.println(
                        "Room number must contain numbers only.");

            } else {
                try {
                    int roomNumber = Integer.parseInt(input);
                    if (roomNumber < MIN_ROOM_NUMBER || roomNumber > MAX_ROOM_NUMBER) {
                        System.out.println("Room number must be between 1 and 9999.");
                    } else {
                        return roomNumber;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Room number must be between 1 and 9999.");
                }
            }
        }
    }

    /** Reads the room number sort order for a report. */
    public int getRoomSortChoice() {
        MessageUI.clearScreen();
        System.out.println("\nSort Room Number By:");
        System.out.println("  [1]  Ascending");
        System.out.println("  [2]  Descending");
        System.out.println("  [0]  Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                2,
                "cancel");
    }

    /** Displays the selected criteria and headings for the room status report. */
    public void displayRoomStatusReportHeader(String statusFilter, String roomFilter, String sortOrder) {
        MessageUI.clearScreen();

        System.out.println(
                "\n========================================================================================");

        System.out.println(
                "                               ROOM STATUS REPORT");

        System.out.println(
                "========================================================================================");

        System.out.println(
                "Generated at: " + LocalDateTime.now().format(REPORT_TIMESTAMP));

        System.out.println(
                "Status Filter : " + statusFilter);

        System.out.println(
                "Room Filter   : " + roomFilter);

        System.out.println(
                "Sort Order    : " + sortOrder);

        System.out.println(
                "----------------------------------------------------------------------------------------");

        System.out.printf(
                "%-15s %-25s %-15s %-20s%n",
                "Room Number",
                "Current Status",
                "Last Task ID",
                "Last Updated");

        System.out.println(
                "----------------------------------------------------------------------------------------");
    }

    /** Displays one room in the room status report. */
    public void displayRoomStatusReportRow(String roomNumber, String status, String taskId, LocalDateTime timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "dd/MM/yyyy HH:mm");

        String formattedDateTime = timestamp.format(formatter);

        System.out.printf(
                "%-15s %-25s %-15s %-20s%n",
                roomNumber,
                status,
                taskId,
                formattedDateTime);
    }

    /** Displays the total number of rooms in the report. */
    public void displayRoomStatusReportFooter(
            int totalRooms) {

        System.out.println(
                "----------------------------------------------------------------------------------------");

        System.out.println(
                "Total Rooms: " + totalRooms);
    }

    /** Displays the closing line for the room status report. */
    public void displayRoomStatusReportEnd() {
        System.out.println(
                "========================================================================================");
        System.out.println(
                "                            END OF ROOM STATUS REPORT");
        System.out.println(
                "========================================================================================");
    }

    /** Displays the totals and percentages for each current room status. */
    public void displayRoomStatusSummary(
            int dirtyCount,
            int cleaningCount,
            int inspectedCount,
            int readyCount,
            int totalRooms) {

        System.out.println(
                "\nSTATUS SUMMARY");

        System.out.println(
                "----------------------------------------------------------------------------------------");

        if (totalRooms <= 0) {
            System.out.println("No rooms available for percentage calculation.");
            return;
        }

        int dirtyPercentage = (int) Math.round(dirtyCount * 100.0 / totalRooms);
        int cleaningPercentage = (int) Math.round(cleaningCount * 100.0 / totalRooms);
        int inspectedPercentage = (int) Math.round(inspectedCount * 100.0 / totalRooms);
        int readyPercentage = (int) Math.round(readyCount * 100.0 / totalRooms);

        System.out.printf(
                "%-25s : %d (%d%%)%n",
                "Dirty",
                dirtyCount,
                dirtyPercentage);

        System.out.printf(
                "%-25s : %d (%d%%)%n",
                "Cleaning In Progress",
                cleaningCount,
                cleaningPercentage);

        System.out.printf(
                "%-25s : %d (%d%%)%n",
                "Inspected",
                inspectedCount,
                inspectedPercentage);

        System.out.printf(
                "%-25s : %d (%d%%)%n",
                "Ready for Check-In",
                readyCount,
                readyPercentage);
    }

    /** Displays a message when no rooms match the report criteria. */
    public void displayNoMatchingRooms() {

        System.out.println(
                "\nNo rooms match the selected report criteria.");
    }

    /** Reads whether the user wants to generate another room report. */
    public int getAfterRoomStatusReportChoice() {

        System.out.println("\nWhat would you like to do next?");
        System.out.println("  [1]  Generate Another Room Status Report");
        System.out.println("  [0]  Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to the Housekeeping Task Log menu");
    }

    // ===========================================
    // 5.2 Generate Housekeeping Activity Report
    // ===========================================
    /** Reads the date filter type for the activity report. */
    public int getActivityDateFilterChoice() {
        displayActionHeader("GENERATE HOUSEKEEPING ACTIVITY REPORT");
        System.out.println("\nFilter by Date:");
        System.out.println("  [1]  All Dates");
        System.out.println("  [2]  Specific Date");
        System.out.println("  [3]  Date Range");
        System.out.println("  [0]  Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                3,
                "cancel");
    }

    /** Reads and validates a date for the activity report. */
    public LocalDate inputActivityDate(String prompt) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("dd/MM/uuuu")
                .withResolverStyle(
                        ResolverStyle.STRICT);

        while (true) {

            System.out.print(prompt + "(0 to cancel): ");

            String input = MessageUI.readLine(scanner);

            if (input.equals("0")) {
                return null;
            } else if (input.isEmpty()) {

                System.out.println(
                        "Date cannot be empty.");

            } else {

                try {

                    LocalDate date = LocalDate.parse(
                            input,
                            formatter);

                    // Reject dates that are later than today.
                    if (date.isAfter(LocalDate.now())) {
                        System.out.println(
                                "Date cannot be in the future.");
                    } else {
                        return date;
                    }

                } catch (DateTimeParseException e) {

                    System.out.println(
                            "Invalid date. Please use dd/MM/yyyy format.");
                }
            }
        }
    }

    /** Reads the status filter for the activity report. */
    public String getActivityStatusFilter() {
        MessageUI.clearScreen();
        System.out.println("\nFilter by Status:");
        System.out.println("  [1]  All Statuses");
        System.out.println("  [2]  Dirty");
        System.out.println("  [3]  Cleaning In Progress");
        System.out.println("  [4]  Inspected");
        System.out.println("  [5]  Ready for Check-In");
        System.out.println("  [0]  Cancel");

        int choice = MessageUI.readMenuChoice(
                scanner,
                5,
                "cancel");

        switch (choice) {

            case 1:
                return "ALL";

            case 2:
                return "Dirty";

            case 3:
                return "Cleaning In Progress";

            case 4:
                return "Inspected";

            case 5:
                return "Ready for Check-In";

            case 0:
                return null;

            default:
                return null;
        }
    }

    /** Reads the timestamp sort order for the activity report. */
    public int getActivitySortChoice() {
        MessageUI.clearScreen();
        System.out.println("\nSort Activities By:");
        System.out.println("  [1]  Oldest to Newest");
        System.out.println("  [2]  Newest to Oldest");
        System.out.println("  [0]  Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                2,
                "cancel");
    }

    /** Displays the selected criteria and headings for the activity report. */
    public void displayActivityReportHeader(
            String dateFilter,
            String statusFilter,
            String roomFilter,
            String sortOrder) {

        MessageUI.clearScreen();

        System.out.println(
                "\n================================================================================================");

        System.out.println(
                "                              HOUSEKEEPING ACTIVITY REPORT");

        System.out.println(
                "================================================================================================");

        System.out.println(
                "Generated at: " + LocalDateTime.now().format(REPORT_TIMESTAMP));

        System.out.println(
                "Date Filter   : " + dateFilter);

        System.out.println(
                "Status Filter : " + statusFilter);

        System.out.println(
                "Room Filter   : " + roomFilter);

        System.out.println(
                "Sort Order    : " + sortOrder);

        System.out.println(
                "------------------------------------------------------------------------------------------------");

        System.out.printf(
                "%-12s %-15s %-25s %-20s%n",
                "Task ID",
                "Room Number",
                "Status Update",
                "Date / Time");

        System.out.println(
                "------------------------------------------------------------------------------------------------");
    }

    /** Displays the activity and room totals for the report. */
    public void displayActivityReportFooter(
            int totalActivities,
            int roomsInvolved) {

        System.out.println(
                "------------------------------------------------------------------------------------------------");

        System.out.println(
                "Total Activities : " + totalActivities);

        System.out.println(
                "Rooms Involved    : " + roomsInvolved);
    }

    /** Displays the current page and the range of records being shown. */
    public void displayPageInfo(int firstRow, int lastRow, int totalRows, int currentPage, int totalPages) {
        System.out.println(
                "Showing " + firstRow + "-" + lastRow + " of " + totalRows
                        + " | Page " + currentPage + " of " + totalPages);
    }

    /** Reads the next, previous, or quit command for a multi-page list. */
    public int getPageChoice(int currentPage, int totalPages) {
        while (true) {
            System.out.print("[N] Next  [P] Previous  [Q] Quit: ");
            if (!scanner.hasNextLine()) {
                return 0;
            }
            String choice = MessageUI.readLine(scanner).toUpperCase();

            if (choice.equals("N")) {
                if (currentPage < totalPages) {
                    return currentPage + 1;
                }
                System.out.println("This is already the last page.");
            } else if (choice.equals("P")) {
                if (currentPage > 1) {
                    return currentPage - 1;
                }
                System.out.println("This is already the first page.");
            } else if (choice.equals("Q")) {
                return 0;
            } else {
                System.out.println("Invalid choice. Please enter N, P, or Q.");
            }
        }
    }

    /** Displays the closing line for the housekeeping activity report. */
    public void displayActivityReportEnd() {
        System.out.println(
                "================================================================================================");
        System.out.println(
                "                        END OF HOUSEKEEPING ACTIVITY REPORT");
        System.out.println(
                "================================================================================================");
    }

    /** Displays the total updates for each status. */
    public void displayActivitySummary(
            int dirtyCount,
            int cleaningCount,
            int inspectedCount,
            int readyCount) {

        System.out.println(
                "\nACTIVITY SUMMARY");

        System.out.println(
                "------------------------------------------------------------------------------------------------");

        System.out.printf(
                "%-30s : %d%n",
                "Dirty Updates",
                dirtyCount);

        System.out.printf(
                "%-30s : %d%n",
                "Cleaning In Progress Updates",
                cleaningCount);

        System.out.printf(
                "%-30s : %d%n",
                "Inspected Updates",
                inspectedCount);

        System.out.printf(
                "%-30s : %d%n",
                "Ready for Check-In Updates",
                readyCount);
    }

    /** Displays a message when no activities match the report criteria. */
    public void displayNoMatchingActivities() {
        System.out.println(
                "\nNo housekeeping activities match "
                        + "the selected report criteria.");
    }

    /** Reads whether the user wants to generate another activity report. */
    public int getAfterActivityReportChoice() {
        System.out.println("\nWhat would you like to do next?");
        System.out.println(
                "1. Generate Another Housekeeping Activity Report");
        System.out.println(
                "0. Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to the Housekeeping Task Log menu");
    }

    // ================
    // Utility Methods
    // ================
    /** Waits for Enter before returning to the previous menu. */
    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        // hasNextLine() guards against input being exhausted (e.g. piped input
        // or Ctrl+D), which would otherwise throw NoSuchElementException here.
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

    /** Displays a general message to the user. */
    public void displayMessage(String message) {
        System.out.println("\n" + message);
    }
}
