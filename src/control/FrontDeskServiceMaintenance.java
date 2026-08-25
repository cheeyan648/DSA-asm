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

    // ============================================================
    // DATA MEMBERS
    // ============================================================

    /**
     * DAO used for permanent file storage.
     */
    private final BookingDAO bookingDAO =
            new BookingDAO();

    /**
     * Stores complete Front-Desk records.
     *
     * Each record contains:
     * 1. Booking
     * 2. BillingRecord
     */
    private final ListInterface<FrontDeskRecord> recordList =
            new ArrayList<>();

    /**
     * Boundary class for user input and output.
     */
    private final FrontDeskServiceUI frontDeskServiceUI =
            new FrontDeskServiceUI();

    /**
     * Non-Linear ADT.
     *
     * Booking records are stored according to
     * the confirmation number.
     *
     * Used for searching bookings.
     */
    private final BookingBST bookingTree =
            new BookingBST();

    /**
     * Stores booking records for:
     * - displaying all bookings
     * - room availability checking
     * - report generation
     */
    private final ListInterface<Booking> bookingList =
            new ArrayList<>();

    /**
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
     * Loads all saved Front-Desk records.
     *
     * If no data exists, sample records are created.
     */
    private void loadBookingData() {

        ListInterface<FrontDeskRecord> loadedRecords =
                bookingDAO.loadRecords();

        /*
         * If no data exists, initialize sample data.
         */
        if (loadedRecords.isEmpty()) {

            loadedRecords =
                    new BookingInitializer()
                            .initializeBookings();

            bookingDAO.saveRecords(
                    loadedRecords);
        }

        /*
         * Rebuild all ADTs.
         */
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
             * Rebuild BST.
             */
            bookingTree.add(booking);

            /*
             * Rebuild booking list.
             */
            bookingList.add(booking);

            /*
             * Rebuild billing list.
             */
            billingList.add(billing);

            /*
             * Rebuild complete record list.
             */
            recordList.add(record);
        }
    }

    // ============================================================
    // ROOM NUMBER VALIDATION
    // ============================================================

    /**
     * Validates a room number.
     *
     * Room number must contain exactly four digits.
     *
     * Valid:
     * 1001
     * 1002
     * 2001
     * 3001
     *
     * Invalid:
     * A101
     * B201
     * 101
     * 10001
     * 20A1
     *
     * @param roomNumber room number to validate
     * @return true if valid, otherwise false
     */
    private boolean isValidRoomNumber(
            String roomNumber) {

        return roomNumber != null
                && roomNumber.matches("\\d{4}");
    }

    // ============================================================
    // CREATE BOOKING
    // ============================================================

    /**
     * Creates a new booking.
     */
    public void createBooking() {

        Booking booking =
                frontDeskServiceUI.inputBooking();

        /*
         * User cancelled.
         */
        if (booking == null) {

            frontDeskServiceUI.displayMessage(
                    "Booking creation cancelled.");

            return;
        }

        /*
         * Additional validation at control level.
         */
        if (!isValidRoomNumber(
                booking.getRoomNumber())) {

            frontDeskServiceUI.displayMessage(
                    "Invalid room number. "
                    + "Room number must contain "
                    + "exactly 4 digits.");

            return;
        }

        String confirmationNumber =
                booking.getConfirmationNumber();

        /*
         * Check duplicate confirmation number.
         */
        if (bookingTree.contains(
                confirmationNumber)) {

            frontDeskServiceUI.displayMessage(
                    "A booking with this "
                    + "confirmation number "
                    + "already exists.");

            return;
        }

        /*
         * Check room availability.
         */
        if (!isRoomAvailable(
                booking.getRoomNumber(),
                booking.getCheckInDate(),
                booking.getCheckOutDate())) {

            frontDeskServiceUI.displayMessage(
                    "Room is not available "
                    + "for the selected dates.");

            return;
        }

        // ========================================================
        // BILLING INPUT
        // ========================================================

        double totalBill =
                frontDeskServiceUI
                        .inputNonNegativeAmount(
                                "Total bill "
                                + "(RM, -1 to cancel): ");

        /*
         * User cancelled.
         */
        if (totalBill
                == FrontDeskServiceUI.CANCELLED_AMOUNT) {

            frontDeskServiceUI.displayMessage(
                    "Booking creation cancelled.");

            return;
        }

        double amountPaid;

        do {

            amountPaid =
                    frontDeskServiceUI
                            .inputNonNegativeAmount(
                                    "Amount paid "
                                    + "(RM, -1 to cancel): ");

            /*
             * User cancelled.
             */
            if (amountPaid
                    == FrontDeskServiceUI.CANCELLED_AMOUNT) {

                frontDeskServiceUI.displayMessage(
                        "Booking creation cancelled.");

                return;
            }

            /*
             * Amount paid cannot exceed bill.
             */
            if (amountPaid > totalBill) {

                frontDeskServiceUI.displayMessage(
                        "Amount paid cannot exceed "
                        + "total bill.");
            }

        } while (amountPaid > totalBill);

        // ========================================================
        // CREATE BILLING RECORD
        // ========================================================

        BillingRecord billingRecord =
                new BillingRecord(
                        confirmationNumber,
                        totalBill,
                        amountPaid);

        // ========================================================
        // UPDATE ADTS
        // ========================================================

        bookingTree.add(booking);

        bookingList.add(booking);

        billingList.add(billingRecord);

        FrontDeskRecord record =
                new FrontDeskRecord(
                        booking,
                        billingRecord);

        recordList.add(record);

        /*
         * Save all records permanently.
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
     * Searches booking using confirmation number.
     *
     * Binary Search Tree is used.
     *
     * @param confirmationNumber confirmation number
     * @return booking if found, otherwise null
     */
    public Booking searchBookingByConfirmationNumber(
            String confirmationNumber) {

        return bookingTree.search(
                confirmationNumber);
    }

    // ============================================================
    // SEARCH GUEST INFORMATION
    // ============================================================

    /**
     * Searches and displays complete guest information.
     */
    public void searchCompleteGuestInformation() {

        frontDeskServiceUI
                .displayScreenHeading(
                        "SEARCH COMPLETE GUEST INFORMATION");

        String confirmationNumber =
                frontDeskServiceUI
                        .inputConfirmationNumber();

        if (confirmationNumber == null) {

            frontDeskServiceUI.displayMessage(
                    "Search cancelled.");

            return;
        }

        Booking booking =
                searchBookingByConfirmationNumber(
                        confirmationNumber);

        if (booking == null) {

            frontDeskServiceUI
                    .displayCompleteGuestInformation(
                            null);

            return;
        }

        frontDeskServiceUI
                .displayCompleteGuestInformation(
                        booking);
    }

    // ============================================================
    // SEARCH BILLING RECORD
    // ============================================================

    /**
     * Performs linear search on billing list.
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
                    && billing
                            .getConfirmationNumber()
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
     * Searches and displays billing details.
     */
    public void searchBillingDetails() {

        frontDeskServiceUI
                .displayScreenHeading(
                        "SEARCH BILLING DETAILS");

        String confirmationNumber =
                frontDeskServiceUI
                        .inputConfirmationNumber();

        if (confirmationNumber == null) {

            frontDeskServiceUI.displayMessage(
                    "Search cancelled.");

            return;
        }

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
     * Checks whether a room is available.
     *
     * @param roomNumber requested room
     * @param requestedCheckIn requested check-in date
     * @param requestedCheckOut requested check-out date
     * @return true if available
     */
    public boolean isRoomAvailable(
            String roomNumber,
            LocalDate requestedCheckIn,
            LocalDate requestedCheckOut) {

        /*
         * Control-level validation.
         */
        if (!isValidRoomNumber(roomNumber)) {
            return false;
        }

        /*
         * Check all existing bookings.
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
             * Check same room.
             */
            boolean sameRoom =
                    existingBooking
                            .getRoomNumber()
                            .equals(roomNumber);

            /*
             * Check date overlap.
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

        frontDeskServiceUI
                .displayScreenHeading(
                        "CHECK ROOM AVAILABILITY");

        String roomNumber =
                frontDeskServiceUI
                        .inputRoomNumber();

        if (roomNumber == null) {

            frontDeskServiceUI.displayMessage(
                    "Availability check cancelled.");

            return;
        }

        /*
         * Additional validation.
         */
        if (!isValidRoomNumber(roomNumber)) {

            frontDeskServiceUI.displayMessage(
                    "Invalid room number.");

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
     * Displays all bookings using BST inorder traversal.
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
     * Generates a functional booking report.
     *
     * User can:
     * 1. Select filter
     * 2. Select sorting field
     * 3. Select ascending / descending
     */
    public void generateBookingReport() {

        int filterChoice =
                frontDeskServiceUI
                        .getBookingReportFilterChoice();

        if (filterChoice == 0) {
            return;
        }

        /*
         * Temporary list.
         */
        ListInterface<Booking> reportList =
                new ArrayList<>();

        String filterDescription;

        // ========================================================
        // FILTER
        // ========================================================

        switch (filterChoice) {

            case 1:

                filterDescription =
                        "All bookings";

                for (int i = 1;
                        i <= bookingList.getNumberOfEntries();
                        i++) {

                    Booking booking =
                            bookingList.getEntry(i);

                    if (booking != null) {
                        reportList.add(booking);
                    }
                }

                break;

            case 2:

                String roomNumber =
                        frontDeskServiceUI
                                .inputRoomNumber();

                if (roomNumber == null) {
                    return;
                }

                filterDescription =
                        "Room " + roomNumber;

                for (int i = 1;
                        i <= bookingList.getNumberOfEntries();
                        i++) {

                    Booking booking =
                            bookingList.getEntry(i);

                    if (booking != null
                            && booking
                                    .getRoomNumber()
                                    .equals(roomNumber)) {

                        reportList.add(booking);
                    }
                }

                break;

            case 3:

                LocalDate startDate =
                        frontDeskServiceUI
                                .inputStartDate();

                if (startDate == null) {
                    return;
                }

                LocalDate endDate =
                        frontDeskServiceUI
                                .inputEndDate(
                                        startDate);

                if (endDate == null) {
                    return;
                }

                filterDescription =
                        startDate
                        + " to "
                        + endDate;

                for (int i = 1;
                        i <= bookingList.getNumberOfEntries();
                        i++) {

                    Booking booking =
                            bookingList.getEntry(i);

                    if (booking == null) {
                        continue;
                    }

                    boolean withinRange =
                            !booking
                                    .getCheckInDate()
                                    .isBefore(startDate)
                            &&
                            !booking
                                    .getCheckInDate()
                                    .isAfter(endDate);

                    if (withinRange) {
                        reportList.add(booking);
                    }
                }

                break;

            default:

                return;
        }

        // ========================================================
        // SORT FIELD
        // ========================================================

        if (reportList.isEmpty()) {

            frontDeskServiceUI
                    .displayBookingReport(
                            reportList,
                            filterDescription,
                            "None",
                            "None");

            return;
        }

        int sortChoice =
                frontDeskServiceUI
                        .getBookingReportSortChoice();

        if (sortChoice == 0) {
            return;
        }

        int orderChoice =
                frontDeskServiceUI
                        .getBookingSortOrderChoice();

        if (orderChoice == 0) {
            return;
        }

        boolean ascending =
                orderChoice == 1;

        String sortDescription;

        switch (sortChoice) {

            case 1:

                sortDescription =
                        "Check-in date";

                sortBookingByCheckInDate(
                        reportList,
                        ascending);

                break;

            case 2:

                sortDescription =
                        "Check-out date";

                sortBookingByCheckOutDate(
                        reportList,
                        ascending);

                break;

            case 3:

                sortDescription =
                        "Room number";

                sortBookingByRoomNumber(
                        reportList,
                        ascending);

                break;

            case 4:

                sortDescription =
                        "Confirmation number";

                sortBookingByConfirmationNumber(
                        reportList,
                        ascending);

                break;

            default:

                return;
        }

        String orderDescription =
                ascending
                        ? "Ascending"
                        : "Descending";

        frontDeskServiceUI
                .displayBookingReport(
                        reportList,
                        filterDescription,
                        sortDescription,
                        orderDescription);
    }

    // ============================================================
    // BOOKING SORT - CHECK IN
    // ============================================================

    private void sortBookingByCheckInDate(
            ListInterface<Booking> list,
            boolean ascending) {

        for (int i = 1;
                i < list.getNumberOfEntries();
                i++) {

            int selectedPosition = i;

            for (int j = i + 1;
                    j <= list.getNumberOfEntries();
                    j++) {

                LocalDate current =
                        list.getEntry(j)
                                .getCheckInDate();

                LocalDate selected =
                        list.getEntry(
                                selectedPosition)
                                .getCheckInDate();

                boolean shouldSelect =
                        ascending
                                ? current.isBefore(selected)
                                : current.isAfter(selected);

                if (shouldSelect) {
                    selectedPosition = j;
                }
            }

            swapBookings(
                    list,
                    i,
                    selectedPosition);
        }
    }

    // ============================================================
    // BOOKING SORT - CHECK OUT
    // ============================================================

    private void sortBookingByCheckOutDate(
            ListInterface<Booking> list,
            boolean ascending) {

        for (int i = 1;
                i < list.getNumberOfEntries();
                i++) {

            int selectedPosition = i;

            for (int j = i + 1;
                    j <= list.getNumberOfEntries();
                    j++) {

                LocalDate current =
                        list.getEntry(j)
                                .getCheckOutDate();

                LocalDate selected =
                        list.getEntry(
                                selectedPosition)
                                .getCheckOutDate();

                boolean shouldSelect =
                        ascending
                                ? current.isBefore(selected)
                                : current.isAfter(selected);

                if (shouldSelect) {
                    selectedPosition = j;
                }
            }

            swapBookings(
                    list,
                    i,
                    selectedPosition);
        }
    }

    // ============================================================
    // BOOKING SORT - ROOM
    // ============================================================

    private void sortBookingByRoomNumber(
            ListInterface<Booking> list,
            boolean ascending) {

        for (int i = 1;
                i < list.getNumberOfEntries();
                i++) {

            int selectedPosition = i;

            for (int j = i + 1;
                    j <= list.getNumberOfEntries();
                    j++) {

                String current =
                        list.getEntry(j)
                                .getRoomNumber();

                String selected =
                        list.getEntry(
                                selectedPosition)
                                .getRoomNumber();

                int comparison =
                        current.compareTo(selected);

                boolean shouldSelect =
                        ascending
                                ? comparison < 0
                                : comparison > 0;

                if (shouldSelect) {
                    selectedPosition = j;
                }
            }

            swapBookings(
                    list,
                    i,
                    selectedPosition);
        }
    }

    // ============================================================
    // BOOKING SORT - CONFIRMATION
    // ============================================================

    private void sortBookingByConfirmationNumber(
            ListInterface<Booking> list,
            boolean ascending) {

        for (int i = 1;
                i < list.getNumberOfEntries();
                i++) {

            int selectedPosition = i;

            for (int j = i + 1;
                    j <= list.getNumberOfEntries();
                    j++) {

                String current =
                        list.getEntry(j)
                                .getConfirmationNumber();

                String selected =
                        list.getEntry(
                                selectedPosition)
                                .getConfirmationNumber();

                int comparison =
                        current.compareTo(selected);

                boolean shouldSelect =
                        ascending
                                ? comparison < 0
                                : comparison > 0;

                if (shouldSelect) {
                    selectedPosition = j;
                }
            }

            swapBookings(
                    list,
                    i,
                    selectedPosition);
        }
    }

    // ============================================================
    // SWAP BOOKINGS
    // ============================================================

    private void swapBookings(
            ListInterface<Booking> list,
            int first,
            int second) {

        if (first == second) {
            return;
        }

        Booking temporary =
                list.getEntry(first);

        list.replace(
                first,
                list.getEntry(second));

        list.replace(
                second,
                temporary);
    }

    // ============================================================
    // BILLING REPORT
    // ============================================================

    /**
     * Generates a functional billing report.
     *
     * User can:
     * 1. Select billing filter
     * 2. Select monetary field
     * 3. Select highest-to-lowest or lowest-to-highest
     */
    public void generateBillingReport() {

        int filterChoice =
                frontDeskServiceUI
                        .getBillingReportFilterChoice();

        if (filterChoice == 0) {
            return;
        }

        ListInterface<Booking> filteredBookings =
                new ArrayList<>();

        ListInterface<BillingRecord> filteredBilling =
                new ArrayList<>();

        String filterDescription;

        // ========================================================
        // FILTER
        // ========================================================

        for (int i = 1;
                i <= billingList.getNumberOfEntries();
                i++) {

            BillingRecord billing =
                    billingList.getEntry(i);

            if (billing == null) {
                continue;
            }

            boolean include = false;

            switch (filterChoice) {

                case 1:

                    /*
                     * All billing records.
                     */
                    include = true;

                    break;

                case 2:

                    /*
                     * Fully paid.
                     */
                    include =
                            billing
                                    .getOutstandingBalance()
                                    <= 0;

                    break;

                case 3:

                    /*
                     * Outstanding.
                     */
                    include =
                            billing
                                    .getOutstandingBalance()
                                    > 0;

                    break;

                default:

                    return;
            }

            if (include) {

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

        switch (filterChoice) {

            case 1:

                filterDescription =
                        "All billing records";

                break;

            case 2:

                filterDescription =
                        "Fully paid";

                break;

            case 3:

                filterDescription =
                        "Outstanding";

                break;

            default:

                return;
        }

        // ========================================================
        // NO RESULT
        // ========================================================

        if (filteredBilling.isEmpty()) {

            frontDeskServiceUI
                    .displayBillingReport(
                            filteredBookings,
                            filteredBilling,
                            filterDescription,
                            "None",
                            "None");

            return;
        }

        // ========================================================
        // SORT FIELD
        // ========================================================

        int sortChoice =
                frontDeskServiceUI
                        .getBillingReportSortChoice();

        if (sortChoice == 0) {
            return;
        }

        int orderChoice =
                frontDeskServiceUI
                        .getBillingSortOrderChoice();

        if (orderChoice == 0) {
            return;
        }

        boolean descending =
                orderChoice == 1;

        String sortDescription;

        switch (sortChoice) {

            case 1:

                sortDescription =
                        "Total bill";

                break;

            case 2:

                sortDescription =
                        "Amount paid";

                break;

            case 3:

                sortDescription =
                        "Outstanding balance";

                break;

            default:

                return;
        }

        sortBillingReport(
                filteredBookings,
                filteredBilling,
                sortChoice,
                descending);

        String orderDescription =
                descending
                        ? "Highest to Lowest"
                        : "Lowest to Highest";

        frontDeskServiceUI
                .displayBillingReport(
                        filteredBookings,
                        filteredBilling,
                        filterDescription,
                        sortDescription,
                        orderDescription);
    }

    // ============================================================
    // BILLING SELECTION SORT
    // ============================================================

    private void sortBillingReport(
            ListInterface<Booking> bookings,
            ListInterface<BillingRecord> billings,
            int sortChoice,
            boolean descending) {

        for (int i = 1;
                i < billings.getNumberOfEntries();
                i++) {

            int selectedPosition = i;

            for (int j = i + 1;
                    j <= billings.getNumberOfEntries();
                    j++) {

                double currentValue =
                        getBillingSortValue(
                                billings.getEntry(j),
                                sortChoice);

                double selectedValue =
                        getBillingSortValue(
                                billings.getEntry(
                                        selectedPosition),
                                sortChoice);

                boolean shouldSelect =
                        descending
                                ? currentValue > selectedValue
                                : currentValue < selectedValue;

                if (shouldSelect) {

                    selectedPosition = j;
                }
            }

            if (selectedPosition != i) {

                /*
                 * Swap BillingRecord.
                 */
                BillingRecord temporaryBilling =
                        billings.getEntry(i);

                billings.replace(
                        i,
                        billings.getEntry(
                                selectedPosition));

                billings.replace(
                        selectedPosition,
                        temporaryBilling);

                /*
                 * Swap corresponding Booking.
                 */
                Booking temporaryBooking =
                        bookings.getEntry(i);

                bookings.replace(
                        i,
                        bookings.getEntry(
                                selectedPosition));

                bookings.replace(
                        selectedPosition,
                        temporaryBooking);
            }
        }
    }

    // ============================================================
    // BILLING SORT VALUE
    // ============================================================

    private double getBillingSortValue(
            BillingRecord billing,
            int sortChoice) {

        switch (sortChoice) {

            case 1:

                return billing.getTotalBill();

            case 2:

                return billing.getAmountPaid();

            case 3:

                return billing.getOutstandingBalance();

            default:

                return 0;
        }
    }

    // ============================================================
    // SEARCH MENU
    // ============================================================

    private void runSearchMenu() {

        int searchChoice;

        do {

            searchChoice =
                    frontDeskServiceUI
                            .getSearchMenuChoice();

            switch (searchChoice) {

                case 1:

                    do {

                        MessageUI.clearScreen();

                        searchCompleteGuestInformation();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                                    == 1);

                    break;

                case 2:

                    do {

                        MessageUI.clearScreen();

                        searchBillingDetails();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                                    == 1);

                    break;

                case 0:

                    break;

                default:

                    frontDeskServiceUI
                            .displayMessage(
                                    "Invalid search choice.");

                    break;
            }

        } while (searchChoice != 0);
    }

    // ============================================================
    // REPORT MENU
    // ============================================================

    private void runReportsMenu() {

        int reportChoice;

        do {

            reportChoice =
                    frontDeskServiceUI
                            .getReportMenuChoice();

            switch (reportChoice) {

                case 1:

                    do {

                        MessageUI.clearScreen();

                        generateBookingReport();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                                    == 1);

                    break;

                case 2:

                    do {

                        MessageUI.clearScreen();

                        generateBillingReport();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                                    == 1);

                    break;

                case 0:

                    break;

                default:

                    frontDeskServiceUI
                            .displayMessage(
                                    "Invalid report choice.");

                    break;
            }

        } while (reportChoice != 0);
    }

    // ============================================================
    // MAIN FRONT DESK LOOP
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

                    runSearchMenu();

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

                        displayAllBookings();

                    } while (
                            frontDeskServiceUI
                                    .getNextActionChoice()
                                    == 1);

                    break;

                case 5:

                    runReportsMenu();

                    break;

                case 0:

                    break;

                default:

                    frontDeskServiceUI
                            .displayMessage(
                                    "Invalid menu choice.");

                    break;
            }

        } while (choice != 0);
    }
}