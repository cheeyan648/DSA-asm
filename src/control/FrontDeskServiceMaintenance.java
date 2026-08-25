package control;

import adt.ArrayList;
import adt.BookingBST;
import adt.ListInterface;
import boundary.FrontDeskServiceUI;
import dao.BookingDAO;
import dao.BookingInitializer;
import entity.BillingRecord;
import entity.Booking;
import entity.FrontDeskRecord;

import java.time.LocalDate;
import utility.MessageUI;

/**
 * Controls the Front-Desk Service module.
 *
 * @author Yong Le
 */
public class FrontDeskServiceMaintenance {

    /*
     * DAO used for permanent file storage.
     */
    private final BookingDAO bookingDAO =
            new BookingDAO();

    /*
     * Stores the complete Front-Desk records.
     *
     * Each record contains:
     * 1. Booking
     * 2. BillingRecord
     */
    private final ListInterface<FrontDeskRecord> recordList =
            new ArrayList<>();

    /*
     * Boundary class for user input and output.
     */
    private final FrontDeskServiceUI frontDeskServiceUI =
            new FrontDeskServiceUI();

    /*
     * Non-Linear ADT.
     *
     * Booking records are stored according to
     * the 8-digit confirmation number.
     *
     * Used for searching bookings.
     */
    private final BookingBST bookingTree =
            new BookingBST();

    /*
     * Stores booking records for:
     * - displaying all bookings
     * - room availability checking
     * - report generation
     */
    private final ListInterface<Booking> bookingList =
            new ArrayList<>();

    /*
     * Stores billing records.
     */
    private final ListInterface<BillingRecord> billingList =
            new ArrayList<>();

    /**
     * Constructor.
     *
     * Loads previously saved booking data when
     * the Front-Desk module starts.
     */
    public FrontDeskServiceMaintenance() {

        loadBookingData();
    }

    // ============================================================
    // DATA LOADING
    // ============================================================

    /**
     * Loads all saved Front-Desk records from bookings.dat.
     *
     * The loaded records are inserted into:
     * - BookingBST
     * - bookingList
     * - billingList
     * - recordList
     */
    private void loadBookingData() {

        ListInterface<FrontDeskRecord> loadedRecords =
                bookingDAO.loadRecords();

        /*
         * On a new device, there is no bookings.dat yet. Seed the module
         * with sample records and save them as that device's local data file.
         */
        if (loadedRecords.isEmpty()) {
            loadedRecords = new BookingInitializer()
                    .initializeBookings();
            bookingDAO.saveRecords(loadedRecords);
        }

        for (int i = 1;
                i <= loadedRecords.getNumberOfEntries();
                i++) {

            FrontDeskRecord record =
                    loadedRecords.getEntry(i);

            if (record == null) {
                continue;
            }

            Booking booking =
                    record.getBooking();

            BillingRecord billing =
                    record.getBillingRecord();

            if (booking == null
                    || billing == null) {
                continue;
            }

            /*
             * Rebuild the Non-Linear ADT.
             */
            bookingTree.add(booking);

            /*
             * Rebuild the booking list.
             */
            bookingList.add(booking);

            /*
             * Rebuild the billing list.
             */
            billingList.add(billing);

            /*
             * Keep the complete record.
             */
            recordList.add(record);
        }
    }

    // ============================================================
    // CREATE BOOKING
    // ============================================================

    /**
     * Creates a new booking.
     *
     * The method:
     * 1. Gets booking information from UI
     * 2. Checks duplicate confirmation number
     * 3. Checks room availability
     * 4. Gets billing information
     * 5. Stores booking in BST
     * 6. Stores booking in ArrayList
     * 7. Stores billing information
     * 8. Saves complete record to bookings.dat
     */
    public void createBooking() {

        Booking booking =
                frontDeskServiceUI.inputBooking();

        // null means the user entered 0 to cancel at one of the prompts.
        if (booking == null) {
            frontDeskServiceUI.displayMessage(
                    "Booking creation cancelled.");
            return;
        }

        String confirmationNumber =
                booking.getConfirmationNumber();

        /*
         * Search the BST to check whether
         * the confirmation number already exists.
         */
        if (bookingTree.contains(
                confirmationNumber)) {

            frontDeskServiceUI.displayMessage(
                    "A booking with this confirmation number "
                    + "already exists.");

            return;
        }

        /*
         * Check whether the selected room is available
         * during the requested dates.
         */
        if (!isRoomAvailable(
                booking.getRoomNumber(),
                booking.getCheckInDate(),
                booking.getCheckOutDate())) {

            frontDeskServiceUI.displayMessage(
                    "Room is not available for the selected dates.");

            return;
        }

        /*
         * Get billing information.
         */
        double totalBill =
                frontDeskServiceUI
                        .inputNonNegativeAmount(
                                "Total bill (RM): ");

        double amountPaid;

        do {

            amountPaid =
                    frontDeskServiceUI
                            .inputNonNegativeAmount(
                                    "Amount paid (RM): ");

            if (amountPaid > totalBill) {

                frontDeskServiceUI.displayMessage(
                        "Amount paid cannot exceed total bill.");
            }

        } while (amountPaid > totalBill);

        /*
         * Create BillingRecord.
         */
        BillingRecord billingRecord =
                new BillingRecord(
                        confirmationNumber,
                        totalBill,
                        amountPaid);

        /*
         * Add booking to BST.
         */
        bookingTree.add(booking);

        /*
         * Add booking to list.
         */
        bookingList.add(booking);

        /*
         * Add billing record to list.
         */
        billingList.add(billingRecord);

        /*
         * Combine booking and billing information
         * into one FrontDeskRecord.
         */
        FrontDeskRecord record =
                new FrontDeskRecord(
                        booking,
                        billingRecord);

        /*
         * Add complete record to record list.
         */
        recordList.add(record);

        /*
         * Permanently save all records.
         */
        bookingDAO.saveRecords(
                recordList);

        frontDeskServiceUI.displayMessage(
                "Booking created and saved successfully.");
    }

    // ============================================================
    // SEARCH BOOKING
    // ============================================================

    /**
     * Searches for a booking using the confirmation number.
     *
     * Binary Search Tree is used as the Non-Linear ADT.
     *
     * @param confirmationNumber 8-digit confirmation number
     * @return Booking if found, otherwise null
     */
    public Booking searchBookingByConfirmationNumber(
            String confirmationNumber) {

        return bookingTree.search(
                confirmationNumber);
    }

    // ============================================================
    // SEARCH COMPLETE GUEST INFORMATION
    // ============================================================

    /**
     * Searches and displays complete guest information.
     */
    public void searchCompleteGuestInformation() {

        frontDeskServiceUI.displayScreenHeading("SEARCH COMPLETE GUEST INFORMATION");

        String confirmationNumber =
                frontDeskServiceUI
                        .inputConfirmationNumber();

        // null means the user entered 0 to cancel.
        if (confirmationNumber == null) {
            frontDeskServiceUI.displayMessage(
                    "Search cancelled.");
            return;
        }

        /*
         * Search booking using BST.
         */
        Booking booking =
                searchBookingByConfirmationNumber(
                        confirmationNumber);

        if (booking == null) {

            frontDeskServiceUI
                    .displayCompleteGuestInformation(
                            null,
                            0,
                            0);

            return;
        }

        /*
         * Search corresponding billing record.
         */
        BillingRecord billingRecord =
                searchBillingRecord(
                        confirmationNumber);

        if (billingRecord == null) {

            frontDeskServiceUI
                    .displayCompleteGuestInformation(
                            booking,
                            0,
                            0);

            return;
        }

        frontDeskServiceUI
                .displayCompleteGuestInformation(
                        booking,
                        billingRecord.getTotalBill(),
                        billingRecord.getAmountPaid());
    }

    // ============================================================
    // SEARCH BILLING RECORD
    // ============================================================

    /**
     * Performs linear search on the billing list.
     *
     * @param confirmationNumber confirmation number
     * @return BillingRecord if found, otherwise null
     */
    private BillingRecord searchBillingRecord(
            String confirmationNumber) {

        for (int i = 1;
                i <= billingList.getNumberOfEntries();
                i++) {

            BillingRecord billing =
                    billingList.getEntry(i);

            if (billing != null
                    && billing.getConfirmationNumber()
                            .equals(confirmationNumber)) {

                return billing;
            }
        }

        return null;
    }

    // ============================================================
    // SEARCH BILLING DETAILS
    // ============================================================

    /**
     * Searches and displays billing details
     * using confirmation number.
     */
    public void searchBillingDetails() {

        frontDeskServiceUI.displayScreenHeading("SEARCH BILLING DETAILS");

        String confirmationNumber =
                frontDeskServiceUI
                        .inputConfirmationNumber();

        // null means the user entered 0 to cancel.
        if (confirmationNumber == null) {
            frontDeskServiceUI.displayMessage(
                    "Search cancelled.");
            return;
        }

        /*
         * Search booking using BST.
         */
        Booking booking =
                searchBookingByConfirmationNumber(
                        confirmationNumber);

        if (booking == null) {

            frontDeskServiceUI
                    .displayBillingDetails(
                            null,
                            0,
                            0);

            return;
        }

        /*
         * Search billing record.
         */
        BillingRecord billingRecord =
                searchBillingRecord(
                        confirmationNumber);

        if (billingRecord == null) {

            frontDeskServiceUI
                    .displayBillingDetails(
                            booking,
                            0,
                            0);

            return;
        }

        frontDeskServiceUI
                .displayBillingDetails(
                        booking,
                        billingRecord.getTotalBill(),
                        billingRecord.getAmountPaid());
    }

    // ============================================================
    // ROOM AVAILABILITY
    // ============================================================

    /**
     * Checks whether a room is available
     * for the requested date range.
     *
     * @param roomNumber requested room
     * @param requestedCheckIn requested check-in date
     * @param requestedCheckOut requested check-out date
     * @return true if available, otherwise false
     */
    public boolean isRoomAvailable(
            String roomNumber,
            LocalDate requestedCheckIn,
            LocalDate requestedCheckOut) {

        /*
         * Check every existing booking.
         */
        for (int i = 1;
                i <= bookingList.getNumberOfEntries();
                i++) {

            Booking existingBooking =
                    bookingList.getEntry(i);

            if (existingBooking == null) {
                continue;
            }

            /*
             * Check whether the room number is the same.
             */
            boolean sameRoom =
                    existingBooking
                            .getRoomNumber()
                            .equalsIgnoreCase(
                                    roomNumber);

            /*
             * Date overlap condition:
             *
             * requestedCheckIn
             *      < existingCheckOut
             *
             * AND
             *
             * requestedCheckOut
             *      > existingCheckIn
             */
            boolean datesOverlap =
                    requestedCheckIn.isBefore(
                            existingBooking
                                    .getCheckOutDate())
                    &&
                    requestedCheckOut.isAfter(
                            existingBooking
                                    .getCheckInDate());

            if (sameRoom && datesOverlap) {
                return false;
            }
        }

        return true;
    }

    /**
     * User interface for checking room availability.
     */
    public void checkRoomAvailability() {

        frontDeskServiceUI.displayScreenHeading("CHECK ROOM AVAILABILITY");

        String roomNumber =
                frontDeskServiceUI
                        .inputRoomNumber();

        // null means the user entered 0 to cancel.
        if (roomNumber == null) {
            frontDeskServiceUI.displayMessage(
                    "Availability check cancelled.");
            return;
        }

        LocalDate checkInDate =
                frontDeskServiceUI
                        .inputCheckInDate();

        if (checkInDate == null) {
            frontDeskServiceUI.displayMessage(
                    "Availability check cancelled.");
            return;
        }

        LocalDate checkOutDate =
                frontDeskServiceUI
                        .inputCheckOutDate(
                                checkInDate);

        if (checkOutDate == null) {
            frontDeskServiceUI.displayMessage(
                    "Availability check cancelled.");
            return;
        }

        boolean available =
                isRoomAvailable(
                        roomNumber,
                        checkInDate,
                        checkOutDate);

        frontDeskServiceUI
                .displayRoomAvailability(
                        roomNumber,
                        checkInDate,
                        checkOutDate,
                        available);
    }

    // ============================================================
    // DISPLAY ALL BOOKINGS
    // ============================================================

    /**
     * Displays all bookings.
     *
     * Uses the BST inorder traversal.
     */
    public void displayAllBookings() {

        frontDeskServiceUI
                .displayAllBookings(
                        bookingTree.getAllBookings());
    }

    // ============================================================
    // BOOKING REPORT
    // ============================================================

    /**
     * Generates Booking Report.
     *
     * Filtering criteria:
     * 1. Room number
     * 2. Check-in date range
     *
     * Sorting:
     * Check-in date ascending.
     */
    public void generateBookingReport() {

        frontDeskServiceUI.displayScreenHeading("GENERATE BOOKING REPORT");

        /*
         * First filtering criterion.
         */
        String roomNumber =
                frontDeskServiceUI
                        .inputRoomNumber();

        // null means the user entered 0 to cancel.
        if (roomNumber == null) {
            frontDeskServiceUI.displayMessage(
                    "Report cancelled.");
            return;
        }

        /*
         * Second filtering criterion.
         */
        LocalDate startDate =
                frontDeskServiceUI
                        .inputStartDate();

        if (startDate == null) {
            frontDeskServiceUI.displayMessage(
                    "Report cancelled.");
            return;
        }

        LocalDate endDate =
                frontDeskServiceUI
                        .inputEndDate(
                                startDate);

        if (endDate == null) {
            frontDeskServiceUI.displayMessage(
                    "Report cancelled.");
            return;
        }

        /*
         * Temporary list for the report.
         */
        ListInterface<Booking> reportList =
                new ArrayList<>();

        /*
         * Multiple-criteria filtering.
         */
        for (int i = 1;
                i <= bookingList.getNumberOfEntries();
                i++) {

            Booking booking =
                    bookingList.getEntry(i);

            if (booking == null) {
                continue;
            }

            /*
             * Criterion 1:
             * Room number.
             */
            boolean sameRoom =
                    booking.getRoomNumber()
                            .equalsIgnoreCase(
                                    roomNumber);

            /*
             * Criterion 2:
             * Check-in date range.
             */
            boolean withinDateRange =
                    !booking.getCheckInDate()
                            .isBefore(startDate)
                    &&
                    !booking.getCheckInDate()
                            .isAfter(endDate);

            /*
             * Both criteria must be satisfied.
             */
            if (sameRoom
                    && withinDateRange) {

                reportList.add(booking);
            }
        }

        /*
         * Sort the report by check-in date
         * in ascending order.
         */
        sortBookingByCheckInDate(
                reportList);

        /*
         * Display report.
         */
        frontDeskServiceUI
                .displayBookingReport(
                        reportList);
    }

    // ============================================================
    // SELECTION SORT - BOOKING REPORT
    // ============================================================

    /**
     * Selection sort by check-in date ascending.
     */
    private void sortBookingByCheckInDate(
            ListInterface<Booking> list) {

        for (int i = 1;
                i < list.getNumberOfEntries();
                i++) {

            int minimumPosition = i;

            for (int j = i + 1;
                    j <= list.getNumberOfEntries();
                    j++) {

                LocalDate currentDate =
                        list.getEntry(j)
                                .getCheckInDate();

                LocalDate minimumDate =
                        list.getEntry(
                                minimumPosition)
                                .getCheckInDate();

                if (currentDate.isBefore(
                        minimumDate)) {

                    minimumPosition = j;
                }
            }

            /*
             * Swap the elements.
             */
            if (minimumPosition != i) {

                Booking temporary =
                        list.getEntry(i);

                list.replace(
                        i,
                        list.getEntry(
                                minimumPosition));

                list.replace(
                        minimumPosition,
                        temporary);
            }
        }
    }

    // ============================================================
    // OUTSTANDING BILLING REPORT
    // ============================================================

    /**
     * Generates Outstanding Billing Report.
     *
     * Filtering criteria:
     * 1. Outstanding balance > 0
     * 2. Outstanding balance >= minimum amount
     *
     * Sorting:
     * Outstanding balance descending.
     */
    public void generateOutstandingBillingReport() {

        frontDeskServiceUI.displayScreenHeading("GENERATE OUTSTANDING BILLING REPORT");

        /*
         * Get minimum outstanding amount.
         */
        double minimumAmount =
                frontDeskServiceUI
                        .inputMinimumOutstandingAmount();

        // Negative means the user cancelled at the prompt.
        if (minimumAmount == FrontDeskServiceUI.CANCELLED_AMOUNT) {
            frontDeskServiceUI.displayMessage(
                    "Report cancelled.");
            return;
        }

        /*
         * Temporary lists for the report.
         *
         * Both lists use the same index so that
         * Booking and BillingRecord remain matched.
         */
        ListInterface<Booking> filteredBookings =
                new ArrayList<>();

        ListInterface<BillingRecord> filteredBilling =
                new ArrayList<>();

        /*
         * Search billing records.
         */
        for (int i = 1;
                i <= billingList.getNumberOfEntries();
                i++) {

            BillingRecord billing =
                    billingList.getEntry(i);

            if (billing == null) {
                continue;
            }

            /*
             * Criterion 1:
             * There must be an outstanding balance.
             */
            boolean hasOutstandingBalance =
                    billing.getOutstandingBalance()
                            > 0;

            /*
             * Criterion 2:
             * Balance must meet minimum amount.
             */
            boolean meetsMinimumAmount =
                    billing.getOutstandingBalance()
                            >= minimumAmount;

            /*
             * Both criteria must be satisfied.
             */
            if (hasOutstandingBalance
                    && meetsMinimumAmount) {

                /*
                 * Find corresponding booking
                 * using BST search.
                 */
                Booking booking =
                        searchBookingByConfirmationNumber(
                                billing
                                        .getConfirmationNumber());

                if (booking != null) {

                    filteredBookings.add(
                            booking);

                    filteredBilling.add(
                            billing);
                }
            }
        }

        /*
         * Sort by outstanding balance
         * in descending order.
         */
        sortBillingByOutstandingBalance(
                filteredBookings,
                filteredBilling);

        /*
         * Display report.
         */
        frontDeskServiceUI
                .displayOutstandingBillingReport(
                        filteredBookings,
                        filteredBilling);
    }

    // ============================================================
    // SELECTION SORT - BILLING REPORT
    // ============================================================

    /**
     * Selection sort by outstanding balance descending.
     *
     * Booking and BillingRecord are swapped together
     * so their relationship is maintained.
     */
    private void sortBillingByOutstandingBalance(
            ListInterface<Booking> bookingList,
            ListInterface<BillingRecord> billingList) {

        for (int i = 1;
                i < billingList.getNumberOfEntries();
                i++) {

            int maximumPosition = i;

            for (int j = i + 1;
                    j <= billingList.getNumberOfEntries();
                    j++) {

                double currentBalance =
                        billingList
                                .getEntry(j)
                                .getOutstandingBalance();

                double maximumBalance =
                        billingList
                                .getEntry(
                                        maximumPosition)
                                .getOutstandingBalance();

                if (currentBalance
                        > maximumBalance) {

                    maximumPosition = j;
                }
            }

            /*
             * Swap BillingRecord.
             */
            if (maximumPosition != i) {

                BillingRecord temporaryBilling =
                        billingList.getEntry(i);

                billingList.replace(
                        i,
                        billingList.getEntry(
                                maximumPosition));

                billingList.replace(
                        maximumPosition,
                        temporaryBilling);

                /*
                 * Swap the corresponding Booking.
                 */
                Booking temporaryBooking =
                        bookingList.getEntry(i);

                bookingList.replace(
                        i,
                        bookingList.getEntry(
                                maximumPosition));

                bookingList.replace(
                        maximumPosition,
                        temporaryBooking);
            }
        }
    }

    // ============================================================
    // MAIN FRONT-DESK LOOP
    // ============================================================

    /**
     * Runs the Front-Desk Service menu.
     */
    public void runFrontDeskService() {

        int choice;

        do {

            choice =
                    frontDeskServiceUI
                            .getMenuChoice();

            switch (choice) {

                case 1:

                    do {

                        MessageUI.clearScreen();
                        createBooking();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                            == 1);

                    break;

                case 2:

                    do {

                        MessageUI.clearScreen();
                        searchCompleteGuestInformation();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                            == 1);

                    break;

                case 3:

                    do {

                        MessageUI.clearScreen();
                        checkRoomAvailability();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                            == 1);

                    break;

                case 4:

                    do {

                        MessageUI.clearScreen();
                        searchBillingDetails();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                            == 1);

                    break;

                case 5:

                    do {

                        MessageUI.clearScreen();
                        displayAllBookings();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                            == 1);

                    break;

                case 6:

                    do {

                        MessageUI.clearScreen();
                        generateBookingReport();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                            == 1);

                    break;

                case 7:

                    do {

                        MessageUI.clearScreen();
                        generateOutstandingBillingReport();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                            == 1);

                    break;

                case 0:

                    break;

                default:

                    frontDeskServiceUI.displayMessage(
                            "Invalid menu choice.");

                    break;
            }

        } while (choice != 0);
    }
}
