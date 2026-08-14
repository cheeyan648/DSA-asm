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

    public int getMenuChoice() {
        MessageUI.clearScreen();

        System.out.println("\nHOUSEKEEPING TASK LOG");
        System.out.println("1. Log new task status update");
        System.out.println("2. Rollback last status update");
        System.out.println("3. Display task log");
        System.out.println("4. Search Housekeeping Task");
        System.out.println("5. Generate Room Status Report");
        System.out.println("6. Generate Housekeeping Activity Report");
        System.out.println("0. Back to main menu");

        return MessageUI.readMenuChoice(scanner, 6, "go back to the main menu");
    }

    // ==============================
    // 1. Log New Task Status Update
    // ==============================
    public void displayLogStatusUpdateHeader() {
        MessageUI.clearScreen();
        System.out.println("\nLOG NEW TASK STATUS UPDATE");
    }

    public String inputRoomNumber() {
        while (true) {
            System.out.print("Enter Room Number: ");
            String roomNumber = scanner.nextLine().trim();

            if (roomNumber.isEmpty()) {
                System.out.println("Room number cannot be empty.");
            } else if (!roomNumber.matches("\\d+")) {
                System.out.println("Room number must contain numbers only.");
            } else {
                return roomNumber;
            }
        }
    }

    public void displayCurrentStatus(String roomNumber, String status) {
        System.out.println(
                "\nHousekeeping record found for Room "
                        + roomNumber + ".");
        System.out.println("Current Status: " + status);
    }

    public void displayNoExistingRecord(String roomNumber) {
        System.out.println(
                "\nNo existing housekeeping record found for Room "
                        + roomNumber + ".");
    }

    public int getUpdateChoice() {
        System.out.println("\n1. Update Status");
        System.out.println("0. Cancel");

        return MessageUI.readMenuChoice(scanner, 1, "cancel");
    }

    public String inputStatus() {
        System.out.println("\nSelect New Status:");
        System.out.println("1. Dirty");
        System.out.println("2. Cleaning In Progress");
        System.out.println("3. Inspected");
        System.out.println("4. Ready for Check-In");
        System.out.println("0. Cancel");

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

    public int getAfterUpdateChoice() {

        System.out.println("\nWhat would you like to do next?");
        System.out.println("1. Log Another Status Update");
        System.out.println("0. Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to the Housekeeping Task Log menu");
    }

    // ========================
    // 2. Rollback Last Status
    // ========================
    public void displayRollbackHeader() {
        MessageUI.clearScreen();
        System.out.println("\nROLLBACK LAST STATUS UPDATE");
    }

    public void displayLastStatusUpdate(String taskId, String roomNumber, String status) {

        System.out.println("\nLast Status Update:");
        System.out.println("Task ID     : " + taskId);
        System.out.println("Room Number : " + roomNumber);
        System.out.println("Status      : " + status);
    }

    public void displayPreviousStatus(String previousStatus) {
        if (previousStatus == null) {
            System.out.println(
                    "\nPrevious Status: No previous housekeeping status");
        } else {
            System.out.println(
                    "\nPrevious Status: " + previousStatus);
        }
    }

    public int getRollbackConfirmation() {
        System.out.println("\n1. Confirm Rollback");
        System.out.println("0. Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "cancel");
    }

    public int getAfterRollbackChoice() {
        System.out.println("\nWhat would you like to do next?");
        System.out.println("1. Rollback Another Status Update");
        System.out.println("0. Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to the Housekeeping Task Log menu");
    }

    // ====================
    // 3. Display Task Log
    // ====================
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

    public void displayTaskLogFooter(int totalRecords) {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Total Records: " + totalRecords);
    }

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
    public int getSearchChoice() {
        MessageUI.clearScreen();
        System.out.println("\nSEARCH HOUSEKEEPING TASK");
        System.out.println("1. Search by Task ID");
        System.out.println("2. Search by Room Number");
        System.out.println("3. Search by Status");
        System.out.println("0. Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                3,
                "go back to the Housekeeping Task Log menu");
    }

    public String inputSearchTaskId() {
        while (true) {
            System.out.print("Enter Task ID: ");
            String taskId = scanner.nextLine().trim().toUpperCase();

            if (taskId.isEmpty()) {
                System.out.println("Task ID cannot be empty.");
            } else if (!taskId.matches("HT\\d{4}")) {
                System.out.println("Invalid Task ID format. Example: HT0001");
            } else {
                return taskId;
            }
        }
    }

    public String inputSearchStatus() {
        System.out.println("\nSelect Status:");
        System.out.println("1. Dirty");
        System.out.println("2. Cleaning In Progress");
        System.out.println("3. Inspected");
        System.out.println("4. Ready for Check-In");
        System.out.println("0. Cancel");

        int choice = MessageUI.readMenuChoice(scanner, 4, "cancel");
        switch (choice) {
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

    public void displaySearchResultFooter(int totalFound) {
        System.out.println(
                "--------------------------------------------------------------------------------");
        System.out.println(
                "Total Records Found: " + totalFound);
    }

    public int getAfterSearchChoice() {
        System.out.println("\nWhat would you like to do next?");
        System.out.println("1. Search Again");
        System.out.println("0. Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to the Housekeeping Task Log menu");
    }

    // ===============================
    // 5. Generate Room Status Report
    // ===============================
    public String getRoomStatusFilter() {
        MessageUI.clearScreen();

        System.out.println("\nGENERATE ROOM STATUS REPORT");
        System.out.println("\nFilter by Status:");
        System.out.println("1. All Statuses");
        System.out.println("2. Dirty");
        System.out.println("3. Cleaning In Progress");
        System.out.println("4. Inspected");
        System.out.println("5. Ready for Check-In");
        System.out.println("0. Cancel");

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

    public int getRoomFilterChoice() {
        System.out.println("\nFilter by Room:");
        System.out.println("1. All Rooms");
        System.out.println("2. Specific Room");
        System.out.println("3. Room Number Range");
        System.out.println("0. Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                3,
                "cancel");
    }

    public int inputRoomRange(String prompt) {
        while (true) {

            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {

                System.out.println(
                        "Room number cannot be empty.");

            } else if (!input.matches("\\d+")) {

                System.out.println(
                        "Room number must contain numbers only.");

            } else {

                return Integer.parseInt(input);
            }
        }
    }

    public int getRoomSortChoice() {
        System.out.println("\nSort Room Number By:");
        System.out.println("1. Ascending");
        System.out.println("2. Descending");
        System.out.println("0. Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                2,
                "cancel");
    }

    public void displayRoomStatusReportHeader(String statusFilter, String roomFilter, String sortOrder) {
        MessageUI.clearScreen();

        System.out.println(
                "\n========================================================================================");

        System.out.println(
                "                               ROOM STATUS REPORT");

        System.out.println(
                "========================================================================================");

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

    public void displayRoomStatusReportFooter(
            int totalRooms) {

        System.out.println(
                "----------------------------------------------------------------------------------------");

        System.out.println(
                "Total Rooms: " + totalRooms);
    }

    public void displayRoomStatusSummary(
            int dirtyCount,
            int cleaningCount,
            int inspectedCount,
            int readyCount) {

        System.out.println(
                "\nSTATUS SUMMARY");

        System.out.println(
                "----------------------------------------------------------------------------------------");

        System.out.printf(
                "%-25s : %d%n",
                "Dirty",
                dirtyCount);

        System.out.printf(
                "%-25s : %d%n",
                "Cleaning In Progress",
                cleaningCount);

        System.out.printf(
                "%-25s : %d%n",
                "Inspected",
                inspectedCount);

        System.out.printf(
                "%-25s : %d%n",
                "Ready for Check-In",
                readyCount);
    }

    public void displayNoMatchingRooms() {

        System.out.println(
                "\nNo rooms match the selected report criteria.");
    }

    public int getAfterRoomStatusReportChoice() {

        System.out.println("\nWhat would you like to do next?");
        System.out.println("1. Generate Another Room Status Report");
        System.out.println("0. Back to Housekeeping Task Log Menu");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to the Housekeeping Task Log menu");
    }

    // =========================================
    // 6. Generate Housekeeping Activity Report
    // =========================================
    public int getActivityDateFilterChoice() {
        MessageUI.clearScreen();
        System.out.println("\nGENERATE HOUSEKEEPING ACTIVITY REPORT");
        System.out.println("\nFilter by Date:");
        System.out.println("1. All Dates");
        System.out.println("2. Specific Date");
        System.out.println("3. Date Range");
        System.out.println("0. Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                3,
                "cancel");
    }

    public LocalDate inputActivityDate(String prompt) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("dd/MM/uuuu")
                .withResolverStyle(
                        ResolverStyle.STRICT);

        while (true) {

            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {

                System.out.println(
                        "Date cannot be empty.");

            } else {

                try {

                    return LocalDate.parse(
                            input,
                            formatter);

                } catch (DateTimeParseException e) {

                    System.out.println(
                            "Invalid date. Please use dd/MM/yyyy format.");
                }
            }
        }
    }

    public String getActivityStatusFilter() {
        System.out.println("\nFilter by Status:");
        System.out.println("1. All Statuses");
        System.out.println("2. Dirty");
        System.out.println("3. Cleaning In Progress");
        System.out.println("4. Inspected");
        System.out.println("5. Ready for Check-In");
        System.out.println("0. Cancel");

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

    public int getActivitySortChoice() {
        System.out.println("\nSort Activities By:");
        System.out.println("1. Oldest to Newest");
        System.out.println("2. Newest to Oldest");
        System.out.println("0. Cancel");

        return MessageUI.readMenuChoice(
                scanner,
                2,
                "cancel");
    }

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

    public void displayNoMatchingActivities() {
        System.out.println(
                "\nNo housekeeping activities match "
                        + "the selected report criteria.");
    }

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
    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    public void displayMessage(String message) {
        System.out.println("\n" + message);
    }
}