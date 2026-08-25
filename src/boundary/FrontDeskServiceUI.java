package boundary;

import adt.ListInterface;
import entity.BillingRecord;
import entity.Booking;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;
import utility.MessageUI;

/**
 * Handles all input and output for the Front-Desk Service module.
 *
 * @author Kat Tan
 */
public class FrontDeskServiceUI {

    // ============================================================
    // CONSTANTS
    // ============================================================

    /*
     * Standard booking table format.
     *
     * Confirm No. : 12 characters
     * Guest Name  : 28 characters
     * Room        : 10 characters
     * Check-in    : 12 characters
     * Check-out   : 12 characters
     */
    private static final String BOOKING_FORMAT =
            "%-12s %-28s %-10s %-12s %-12s%n";

    /**
     * Returned by inputNonNegativeAmount()
     * when the user cancels.
     */
    public static final double CANCELLED_AMOUNT = -1;

    private final Scanner scanner =
            MessageUI.scanner;

    // ============================================================
    // MAIN MENU
    // ============================================================

    /**
     * Displays the Front-Desk Service main menu.
     *
     * @return selected menu option
     */
    public int getMenuChoice() {

        MessageUI.clearScreen();
        System.out.println();

        MessageUI.displayBoxTop();
        MessageUI.displayBoxBlank();

        MessageUI.displayBoxCentred(
                "F R O N T - D E S K   S E R V I C E");

        MessageUI.displayBoxBlank();
        MessageUI.displayBoxDivider();

        MessageUI.displayBoxLine(
                "  Main Menu  >  Front-Desk Service");

        MessageUI.displayBoxDivider();
        MessageUI.displayBoxBlank();

        MessageUI.displayMenuOption(
                1,
                "Create new booking");

        MessageUI.displayMenuOption(
                2,
                "Search information");

        MessageUI.displayMenuOption(
                3,
                "Check room availability");

        MessageUI.displayMenuOption(
                4,
                "Display all bookings");

        MessageUI.displayMenuOption(
                5,
                "Reports");

        MessageUI.displayBoxBlank();

        MessageUI.displayMenuOption(
                0,
                "Back to main menu");

        MessageUI.displayBoxBlank();
        MessageUI.displayBoxBottom();

        return MessageUI.readMenuChoice(
                scanner,
                5,
                "go back to the main menu");
    }

    // ============================================================
    // SEARCH MENU
    // ============================================================

    /**
     * Displays the Search Information submenu.
     *
     * @return selected search option
     */
    public int getSearchMenuChoice() {

        MessageUI.clearScreen();
        System.out.println();

        MessageUI.displayBoxTop();
        MessageUI.displayBoxBlank();

        MessageUI.displayBoxCentred(
                "S E A R C H   I N F O R M A T I O N");

        MessageUI.displayBoxBlank();
        MessageUI.displayBoxDivider();

        MessageUI.displayBoxLine(
                "  Main Menu  >  Front-Desk Service"
                + "  >  Search Information");

        MessageUI.displayBoxDivider();
        MessageUI.displayBoxBlank();

        MessageUI.displayMenuOption(
                1,
                "Search guest information");

        MessageUI.displayMenuOption(
                2,
                "Search billing details");

        MessageUI.displayBoxBlank();

        MessageUI.displayMenuOption(
                0,
                "Back to Front-Desk Service");

        MessageUI.displayBoxBlank();
        MessageUI.displayBoxBottom();

        return MessageUI.readMenuChoice(
                scanner,
                2,
                "go back to Front-Desk Service");
    }

    // ============================================================
    // REPORT MENU
    // ============================================================

    /**
     * Displays the Reports submenu.
     *
     * @return selected report option
     */
    public int getReportMenuChoice() {

        MessageUI.clearScreen();
        System.out.println();

        MessageUI.displayBoxTop();
        MessageUI.displayBoxBlank();

        MessageUI.displayBoxCentred(
                "R E P O R T S");

        MessageUI.displayBoxBlank();
        MessageUI.displayBoxDivider();

        MessageUI.displayBoxLine(
                "  Main Menu  >  Front-Desk Service"
                + "  >  Reports");

        MessageUI.displayBoxDivider();
        MessageUI.displayBoxBlank();

        MessageUI.displayMenuOption(
                1,
                "Booking Report");

        MessageUI.displayMenuOption(
                2,
                "Billing Report");

        MessageUI.displayBoxBlank();

        MessageUI.displayMenuOption(
                0,
                "Back to Front-Desk Service");

        MessageUI.displayBoxBlank();
        MessageUI.displayBoxBottom();

        return MessageUI.readMenuChoice(
                scanner,
                2,
                "go back to Front-Desk Service");
    }

    // ============================================================
    // BOOKING REPORT FILTER
    // ============================================================

    /**
     * Displays booking report filter options.
     *
     * @return selected filter option
     */
    public int getBookingReportFilterChoice() {

        displayActionHeader(
                "BOOKING REPORT - FILTER");

        System.out.println();
        System.out.println(
                "  Filter bookings by:");
        System.out.println();

        MessageUI.displayMenuOption(
                1,
                "All bookings");

        MessageUI.displayMenuOption(
                2,
                "Room number");

        MessageUI.displayMenuOption(
                3,
                "Check-in date range");

        System.out.println();

        MessageUI.displayMenuOption(
                0,
                "Back");

        return MessageUI.readMenuChoice(
                scanner,
                3,
                "go back to Reports");
    }

    // ============================================================
    // BILLING REPORT FILTER
    // ============================================================

    /**
     * Displays billing report filter options.
     *
     * @return selected filter option
     */
    public int getBillingReportFilterChoice() {

        displayActionHeader(
                "BILLING REPORT - FILTER");

        System.out.println();
        System.out.println(
                "  Filter billing records by:");
        System.out.println();

        MessageUI.displayMenuOption(
                1,
                "All billing records");

        MessageUI.displayMenuOption(
                2,
                "Fully paid records");

        MessageUI.displayMenuOption(
                3,
                "Outstanding records");

        System.out.println();

        MessageUI.displayMenuOption(
                0,
                "Back");

        return MessageUI.readMenuChoice(
                scanner,
                3,
                "go back to Reports");
    }

    // ============================================================
    // BOOKING REPORT SORT
    // ============================================================

    /**
     * Displays booking report sorting options.
     *
     * @return selected sorting field
     */
    public int getBookingReportSortChoice() {

        displayActionHeader(
                "BOOKING REPORT - SORT");

        System.out.println();
        System.out.println(
                "  Sort bookings by:");
        System.out.println();

        MessageUI.displayMenuOption(
                1,
                "Check-in date");

        MessageUI.displayMenuOption(
                2,
                "Check-out date");

        MessageUI.displayMenuOption(
                3,
                "Room number");

        MessageUI.displayMenuOption(
                4,
                "Confirmation number");

        System.out.println();

        MessageUI.displayMenuOption(
                0,
                "Back");

        return MessageUI.readMenuChoice(
                scanner,
                4,
                "go back to Booking Report");
    }

    /**
     * Displays booking sorting order options.
     *
     * @return selected sorting order
     */
    public int getBookingSortOrderChoice() {

        displayActionHeader(
                "BOOKING REPORT - SORT ORDER");

        System.out.println();
        System.out.println(
                "  Sort order:");
        System.out.println();

        MessageUI.displayMenuOption(
                1,
                "Ascending");

        MessageUI.displayMenuOption(
                2,
                "Descending");

        System.out.println();

        MessageUI.displayMenuOption(
                0,
                "Back");

        return MessageUI.readMenuChoice(
                scanner,
                2,
                "go back to Booking Report");
    }

    // ============================================================
    // BILLING REPORT SORT
    // ============================================================

    /**
     * Displays billing report sorting options.
     *
     * @return selected sorting field
     */
    public int getBillingReportSortChoice() {

        displayActionHeader(
                "BILLING REPORT - SORT");

        System.out.println();
        System.out.println(
                "  Sort billing records by:");
        System.out.println();

        MessageUI.displayMenuOption(
                1,
                "Total bill");

        MessageUI.displayMenuOption(
                2,
                "Amount paid");

        MessageUI.displayMenuOption(
                3,
                "Outstanding balance");

        System.out.println();

        MessageUI.displayMenuOption(
                0,
                "Back");

        return MessageUI.readMenuChoice(
                scanner,
                3,
                "go back to Billing Report");
    }

    /**
     * Displays billing sorting order options.
     *
     * @return selected sorting order
     */
    public int getBillingSortOrderChoice() {

        displayActionHeader(
                "BILLING REPORT - SORT ORDER");

        System.out.println();
        System.out.println(
                "  Sort order:");
        System.out.println();

        MessageUI.displayMenuOption(
                1,
                "Highest to Lowest");

        MessageUI.displayMenuOption(
                2,
                "Lowest to Highest");

        System.out.println();

        MessageUI.displayMenuOption(
                0,
                "Back");

        return MessageUI.readMenuChoice(
                scanner,
                2,
                "go back to Billing Report");
    }

    // ============================================================
    // ACTION HEADER
    // ============================================================

    /**
     * Displays a framed action header.
     *
     * @param title action title
     */
    private void displayActionHeader(
            String title) {

        MessageUI.clearScreen();
        System.out.println();

        MessageUI.displayBoxTop();

        MessageUI.displayBoxCentred(
                title);

        MessageUI.displayBoxBottom();
    }

    // ============================================================
    // NEXT ACTION
    // ============================================================

    /**
     * Displays the next action menu.
     *
     * @return selected option
     */
    public int getNextActionChoice() {

        System.out.println();

        System.out.println(
                "  [1]  Continue with the same task");

        System.out.println(
                "  [0]  Back to Front-Desk Service");

        return MessageUI.readMenuChoice(
                scanner,
                1,
                "go back to Front-Desk Service");
    }

    // ============================================================
    // BOOKING INPUT
    // ============================================================

    /**
     * Collects all information required to create a booking.
     *
     * @return Booking object or null if cancelled
     */
    public Booking inputBooking() {

        displayActionHeader(
                "CREATE NEW BOOKING");

        System.out.println(
                "Enter 0 at any prompt to cancel.");

        System.out.println();

        String confirmationNumber =
                inputConfirmationNumber();

        if (confirmationNumber == null) {
            return null;
        }

        String guestName =
                inputRequiredText(
                        "Guest name (0 to cancel): ");

        if (guestName == null) {
            return null;
        }

        String roomNumber =
                inputRoomNumber();

        if (roomNumber == null) {
            return null;
        }

        LocalDate checkInDate =
                inputCheckInDate();

        if (checkInDate == null) {
            return null;
        }

        LocalDate checkOutDate =
                inputCheckOutDate(
                        checkInDate);

        if (checkOutDate == null) {
            return null;
        }

        return new Booking(
                confirmationNumber,
                guestName,
                roomNumber,
                checkInDate,
                checkOutDate);
    }

    // ============================================================
    // CONFIRMATION NUMBER
    // ============================================================

    /**
     * Reads an 8-digit confirmation number.
     *
     * @return confirmation number or null if cancelled
     */
    public String inputConfirmationNumber() {

        while (true) {

            System.out.print(
                    "Confirmation number "
                    + "(8 digits, 0 to cancel): ");

            String confirmationNumber =
                    MessageUI.readLine(scanner);

            if (confirmationNumber.equals("0")) {
                return null;
            }

            if (confirmationNumber.matches(
                    "\\d{8}")) {

                return confirmationNumber;
            }

            System.out.println(
                    "Confirmation number must contain "
                    + "exactly 8 digits.");
        }
    }

    // ============================================================
    // ROOM NUMBER
    // ============================================================

    /**
     * Reads a room number.
     *
     * Room number must contain exactly four digits.
     *
     * Valid examples:
     * 1001
     * 1002
     * 2001
     * 3001
     *
     * Invalid examples:
     * A101
     * 20A1
     * 101
     * 10001
     *
     * @return room number or null if cancelled
     */
    public String inputRoomNumber() {

        while (true) {

            System.out.print(
                    "Room number "
                    + "(4 digits, 0 to cancel): ");

            String roomNumber =
                    MessageUI.readLine(scanner);

            /*
             * User cancellation.
             */
            if (roomNumber.equals("0")) {
                return null;
            }

            /*
             * Empty input.
             */
            if (roomNumber.isEmpty()) {

                System.out.println(
                        "Room number cannot be empty.");

                continue;
            }

            /*
             * Room number must contain
             * exactly four digits.
             */
            if (!roomNumber.matches(
                    "\\d{4}")) {

                System.out.println(
                        "Invalid room number. "
                        + "Room number must contain "
                        + "exactly 4 digits.");

                continue;
            }

            return roomNumber;
        }
    }

    // ============================================================
    // DATE INPUT
    // ============================================================

    /**
     * Reads check-in date.
     *
     * @return check-in date or null if cancelled
     */
    public LocalDate inputCheckInDate() {

        return inputDate(
                "Check-in date "
                + "(YYYY-MM-DD, 0 to cancel): ");
    }

    /**
     * Reads check-out date.
     *
     * Check-out must be after check-in.
     *
     * @param checkInDate check-in date
     * @return check-out date or null if cancelled
     */
    public LocalDate inputCheckOutDate(
            LocalDate checkInDate) {

        while (true) {

            LocalDate checkOutDate =
                    inputDate(
                            "Check-out date "
                            + "(YYYY-MM-DD, 0 to cancel): ");

            if (checkOutDate == null) {
                return null;
            }

            if (checkOutDate.isAfter(
                    checkInDate)) {

                return checkOutDate;
            }

            System.out.println(
                    "Check-out date must be after "
                    + "the check-in date.");
        }
    }

    /**
     * Reads a LocalDate.
     *
     * @param prompt input prompt
     * @return LocalDate or null if cancelled
     */
    private LocalDate inputDate(
            String prompt) {

        while (true) {

            System.out.print(prompt);

            String input =
                    MessageUI.readLine(scanner);

            if (input.equals("0")) {
                return null;
            }

            try {

                return LocalDate.parse(input);

            } catch (DateTimeParseException e) {

                System.out.println(
                        "Please enter a valid date "
                        + "in YYYY-MM-DD format.");
            }
        }
    }

    /**
     * Reads report start date.
     *
     * @return start date or null
     */
    public LocalDate inputStartDate() {

        return inputDate(
                "Start date "
                + "(YYYY-MM-DD, 0 to cancel): ");
    }

    /**
     * Reads report end date.
     *
     * @param startDate report start date
     * @return end date or null
     */
    public LocalDate inputEndDate(
            LocalDate startDate) {

        while (true) {

            LocalDate endDate =
                    inputDate(
                            "End date "
                            + "(YYYY-MM-DD, 0 to cancel): ");

            if (endDate == null) {
                return null;
            }

            if (!endDate.isBefore(
                    startDate)) {

                return endDate;
            }

            System.out.println(
                    "End date must be after "
                    + "or equal to the start date.");
        }
    }

    // ============================================================
    // AMOUNT INPUT
    // ============================================================

    /**
     * Reads a non-negative monetary amount.
     *
     * @param prompt input prompt
     * @return amount or CANCELLED_AMOUNT
     */
    public double inputNonNegativeAmount(
            String prompt) {

        while (true) {

            System.out.print(prompt);

            String input =
                    MessageUI.readLine(scanner);

            if (input.equals("-1")) {
                return CANCELLED_AMOUNT;
            }

            if (input.isEmpty()) {

                System.out.println(
                        "Amount cannot be empty.");

                continue;
            }

            /*
             * Accept numbers with up to
             * two decimal places.
             */
            if (!input.matches(
                    "\\d+(\\.\\d{1,2})?")) {

                System.out.println(
                        "Please enter a valid amount "
                        + "(e.g. 100 or 100.50).");

                continue;
            }

            try {

                double amount =
                        Double.parseDouble(input);

                if (Double.isFinite(amount)
                        && amount >= 0) {

                    return amount;
                }

                System.out.println(
                        "Amount cannot be negative.");

            } catch (NumberFormatException e) {

                System.out.println(
                        "Please enter a valid amount.");
            }
        }
    }

    /**
     * Reads minimum outstanding amount.
     *
     * @return minimum amount
     */
    public double inputMinimumOutstandingAmount() {

        return inputNonNegativeAmount(
                "Minimum outstanding amount "
                + "(RM, -1 to cancel): ");
    }

    /**
     * Reads required text.
     *
     * @param prompt input prompt
     * @return text or null if cancelled
     */
    private String inputRequiredText(
            String prompt) {

        while (true) {

            System.out.print(prompt);

            String input =
                    MessageUI.readLine(scanner);

            if (input.equals("0")) {
                return null;
            }

            if (!input.isEmpty()) {
                return input;
            }

            System.out.println(
                    "This field cannot be empty.");
        }
    }

    // ============================================================
    // GUEST INFORMATION
    // ============================================================

    /**
     * Displays complete guest information.
     *
     * @param booking booking information
     */
    public void displayCompleteGuestInformation(
            Booking booking) {

        if (booking == null) {

            System.out.println(
                    "\nBooking not found.");

            return;
        }

        displayActionHeader(
                "GUEST INFORMATION");

        System.out.printf(
                "%-22s: %s%n",
                "Confirmation number",
                booking.getConfirmationNumber());

        System.out.printf(
                "%-22s: %s%n",
                "Guest name",
                booking.getGuestName());

        System.out.printf(
                "%-22s: %s%n",
                "Room number",
                booking.getRoomNumber());

        System.out.printf(
                "%-22s: %s%n",
                "Check-in date",
                booking.getCheckInDate());

        System.out.printf(
                "%-22s: %s%n",
                "Check-out date",
                booking.getCheckOutDate());
    }

    // ============================================================
    // BILLING DETAILS
    // ============================================================

    /**
     * Displays billing details.
     *
     * @param booking booking information
     * @param totalBill total bill
     * @param amountPaid amount paid
     */
    public void displayBillingDetails(
            Booking booking,
            double totalBill,
            double amountPaid) {

        if (booking == null) {

            System.out.println(
                    "\nBooking not found.");

            return;
        }

        double outstandingBalance =
                totalBill - amountPaid;

        String paymentStatus =
                outstandingBalance <= 0
                        ? "PAID"
                        : "OUTSTANDING";

        displayActionHeader(
                "BILLING DETAILS");

        System.out.printf(
                "%-22s: %s%n",
                "Confirmation number",
                booking.getConfirmationNumber());

        System.out.printf(
                "%-22s: %s%n",
                "Guest name",
                booking.getGuestName());

        System.out.printf(
                "%-22s: RM %.2f%n",
                "Total bill",
                totalBill);

        System.out.printf(
                "%-22s: RM %.2f%n",
                "Amount paid",
                amountPaid);

        System.out.printf(
                "%-22s: RM %.2f%n",
                "Outstanding balance",
                outstandingBalance);

        System.out.printf(
                "%-22s: %s%n",
                "Payment status",
                paymentStatus);
    }

    // ============================================================
    // ROOM AVAILABILITY
    // ============================================================

    /**
     * Displays room availability result.
     *
     * @param roomNumber room number
     * @param checkInDate check-in date
     * @param checkOutDate check-out date
     * @param available availability status
     */
    public void displayRoomAvailability(
            String roomNumber,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            boolean available) {

        displayActionHeader(
                "ROOM AVAILABILITY RESULT");

        System.out.printf(
                "%-18s: %s%n",
                "Room number",
                roomNumber);

        System.out.printf(
                "%-18s: %s%n",
                "Check-in date",
                checkInDate);

        System.out.printf(
                "%-18s: %s%n",
                "Check-out date",
                checkOutDate);

        System.out.printf(
                "%-18s: %s%n",
                "Status",
                available
                        ? "AVAILABLE"
                        : "NOT AVAILABLE");
    }

    // ============================================================
    // DISPLAY ALL BOOKINGS
    // ============================================================

    /**
     * Displays all bookings.
     *
     * @param list booking list
     */
    public void displayAllBookings(
            ListInterface<Booking> list) {

        if (list.isEmpty()) {

            System.out.println(
                    "\nNo bookings found.");

            return;
        }

        displayActionHeader(
                "ALL BOOKINGS");

        System.out.println(
                "================================================================================");

        System.out.printf(
                "%-5s",
                "No.");

        displayBookingHeader();

        System.out.println(
                "--------------------------------------------------------------------------------");

        Iterator<Booking> iterator =
                list.getIterator();

        int number = 1;

        while (iterator.hasNext()) {

            System.out.printf(
                    "%-5d",
                    number++);

            displayBookingRow(
                    iterator.next());
        }

        System.out.println(
                "================================================================================");

        System.out.println(
                "Total Bookings : "
                + list.getNumberOfEntries());
    }

    /**
     * Displays booking table header.
     */
    private void displayBookingHeader() {

        System.out.printf(
                BOOKING_FORMAT,
                "Confirm No.",
                "Guest Name",
                "Room",
                "Check-in",
                "Check-out");
    }

    /**
     * Displays one booking row.
     *
     * @param booking booking record
     */
    private void displayBookingRow(
            Booking booking) {

        String guestName =
                formatGuestName(
                        booking.getGuestName(),
                        28);

        System.out.printf(
                BOOKING_FORMAT,
                booking.getConfirmationNumber(),
                guestName,
                booking.getRoomNumber(),
                booking.getCheckInDate(),
                booking.getCheckOutDate());
    }

    // ============================================================
    // BOOKING REPORT
    // ============================================================

    /**
     * Displays the generated booking report.
     *
     * @param reportList filtered and sorted bookings
     * @param filterDescription selected filter
     * @param sortDescription selected sorting field
     * @param orderDescription selected sorting order
     */
    public void displayBookingReport(
            ListInterface<Booking> reportList,
            String filterDescription,
            String sortDescription,
            String orderDescription) {

        displayActionHeader(
                "BOOKING REPORT");

        System.out.println(
                "Filter : "
                + filterDescription);

        System.out.println(
                "Sort   : "
                + sortDescription);

        System.out.println(
                "Order  : "
                + orderDescription);

        System.out.println();

        if (reportList.isEmpty()) {

            System.out.println(
                    "No booking records found.");

            return;
        }

        System.out.println(
                "========================================================================================");

        System.out.printf(
                "%-5s %-12s %-28s %-10s %-12s %-12s%n",
                "No.",
                "Confirm No.",
                "Guest Name",
                "Room",
                "Check-in",
                "Check-out");

        System.out.println(
                "----------------------------------------------------------------------------------------");

        long totalNights = 0;

        for (int i = 1;
                i <= reportList.getNumberOfEntries();
                i++) {

            Booking booking =
                    reportList.getEntry(i);

            String guestName =
                    formatGuestName(
                            booking.getGuestName(),
                            28);

            long nights =
                    booking.getCheckInDate()
                            .until(
                                    booking.getCheckOutDate())
                            .getDays();

            totalNights += nights;

            System.out.printf(
                    "%-5d %-12s %-28s %-10s %-12s %-12s%n",
                    i,
                    booking.getConfirmationNumber(),
                    guestName,
                    booking.getRoomNumber(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate());
        }

        System.out.println(
                "========================================================================================");

        System.out.println(
                "Total Bookings : "
                + reportList.getNumberOfEntries());

        System.out.println(
                "Total Nights   : "
                + totalNights);
    }

    // ============================================================
    // BILLING REPORT
    // ============================================================

    /**
     * Displays the generated billing report.
     *
     * @param bookingList booking records
     * @param billingList billing records
     * @param filterDescription selected filter
     * @param sortDescription selected sorting field
     * @param orderDescription selected sorting order
     */
    public void displayBillingReport(
            ListInterface<Booking> bookingList,
            ListInterface<BillingRecord> billingList,
            String filterDescription,
            String sortDescription,
            String orderDescription) {

        displayActionHeader(
                "BILLING REPORT");

        System.out.println(
                "Filter : "
                + filterDescription);

        System.out.println(
                "Sort   : "
                + sortDescription);

        System.out.println(
                "Order  : "
                + orderDescription);

        System.out.println();

        if (bookingList.isEmpty()) {

            System.out.println(
                    "No billing records found.");

            return;
        }

        System.out.println(
                "==============================================================================================");

        System.out.printf(
                "%-5s %-12s %-28s %-14s %-14s %-14s%n",
                "No.",
                "Confirm No.",
                "Guest Name",
                "Total Bill",
                "Paid",
                "Balance");

        System.out.println(
                "----------------------------------------------------------------------------------------------");

        double totalBilling = 0;
        double totalPaid = 0;
        double totalOutstanding = 0;

        for (int i = 1;
                i <= bookingList.getNumberOfEntries();
                i++) {

            Booking booking =
                    bookingList.getEntry(i);

            BillingRecord billing =
                    billingList.getEntry(i);

            String guestName =
                    formatGuestName(
                            booking.getGuestName(),
                            28);

            double totalBill =
                    billing.getTotalBill();

            double amountPaid =
                    billing.getAmountPaid();

            double outstanding =
                    billing.getOutstandingBalance();

            totalBilling += totalBill;
            totalPaid += amountPaid;
            totalOutstanding += outstanding;

            System.out.printf(
                    "%-5d %-12s %-28s "
                    + "RM %-10.2f "
                    + "RM %-10.2f "
                    + "RM %-10.2f%n",
                    i,
                    booking.getConfirmationNumber(),
                    guestName,
                    totalBill,
                    amountPaid,
                    outstanding);
        }

        double averageBill =
                totalBilling
                / bookingList.getNumberOfEntries();

        System.out.println(
                "==============================================================================================");

        System.out.printf(
                "Total Records       : %d%n",
                bookingList.getNumberOfEntries());

        System.out.printf(
                "Total Billing       : RM %.2f%n",
                totalBilling);

        System.out.printf(
                "Total Paid          : RM %.2f%n",
                totalPaid);

        System.out.printf(
                "Total Outstanding   : RM %.2f%n",
                totalOutstanding);

        System.out.printf(
                "Average Bill        : RM %.2f%n",
                averageBill);

        System.out.println(
                "==============================================================================================");
    }

    // ============================================================
    // FORMAT GUEST NAME
    // ============================================================

    /**
     * Keeps guest names within the table width.
     *
     * Long names are shortened with "...".
     *
     * Example:
     *
     * Nur Aisyah binti Rahman
     *
     * will remain unchanged if it fits.
     *
     * Very long names will become:
     *
     * Nur Aisyah binti Rah...
     *
     * @param guestName guest name
     * @param maxLength maximum display width
     * @return formatted guest name
     */
    private String formatGuestName(
            String guestName,
            int maxLength) {

        if (guestName == null) {
            return "";
        }

        if (guestName.length() <= maxLength) {
            return guestName;
        }

        return guestName.substring(
                0,
                maxLength - 3)
                + "...";
    }

    // ============================================================
    // GENERAL MESSAGE
    // ============================================================

    /**
     * Displays a general message.
     *
     * @param message message to display
     */
    public void displayMessage(
            String message) {

        System.out.println(
                "\n" + message);
    }

    /**
     * Displays a simple screen heading.
     *
     * @param title heading title
     */
    public void displayScreenHeading(
            String title) {

        System.out.println(
                "\n" + title);

        System.out.println(
                "=".repeat(
                        title.length()));
    }
}