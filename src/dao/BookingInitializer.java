package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.BillingRecord;
import entity.Booking;
import entity.FrontDeskRecord;
import java.time.LocalDate;

/**
 * Provides sample booking records when no saved booking data exists yet.
 *
 * @author Yong Le
 */
public class BookingInitializer {

    /**
     * Creates the default bookings used on the first run of the module.
     *
     * @return sample booking and billing records
     */
    public ListInterface<FrontDeskRecord> initializeBookings() {

        ListInterface<FrontDeskRecord> records =
                new ArrayList<>();

        // ============================================================
        // A101
        // Same room with different booking dates
        // ============================================================

        addRecord(records, "10000001", "Tan Chee Yan", "A101",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 4),
                450.00, 450.00);

        addRecord(records, "10000004", "Ivan Wong", "A101",
                LocalDate.of(2026, 9, 6),
                LocalDate.of(2026, 9, 9),
                450.00, 450.00);

        addRecord(records, "10000006", "Daniel Lee", "A101",
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 15),
                500.00, 300.00);

        addRecord(records, "10000007", "Sarah Lim", "A101",
                LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 21),
                480.00, 480.00);

        addRecord(records, "10000008", "Jason Tan", "A101",
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 9, 28),
                520.00, 200.00);


        // ============================================================
        // A102
        // Same room with different booking dates
        // ============================================================

        addRecord(records, "10000002", "Lim Yong Le", "A102",
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 5),
                600.00, 200.00);

        addRecord(records, "10000009", "Michelle Wong", "A102",
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 10),
                550.00, 300.00);

        addRecord(records, "10000010", "Kevin Chong", "A102",
                LocalDate.of(2026, 9, 13),
                LocalDate.of(2026, 9, 16),
                580.00, 580.00);

        addRecord(records, "10000011", "Emily Tan", "A102",
                LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 9, 23),
                620.00, 100.00);


        // ============================================================
        // B201
        // Same room with different booking dates
        // ============================================================

        addRecord(records, "10000003", "Nur Aisyah binti Rahman", "B201",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 8),
                525.00, 0.00);

        addRecord(records, "10000012", "Ahmad Hakim", "B201",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 13),
                550.00, 250.00);

        addRecord(records, "10000013", "Siti Nurhaliza", "B201",
                LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 19),
                575.00, 575.00);

        addRecord(records, "10000014", "Mohd Firdaus", "B201",
                LocalDate.of(2026, 9, 22),
                LocalDate.of(2026, 9, 25),
                600.00, 150.00);


        // ============================================================
        // C301
        // Same room with different booking dates
        // ============================================================

        addRecord(records, "10000005", "Chong Zhi Ying", "C301",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 13),
                750.00, 300.00);

        addRecord(records, "10000015", "Alice Wong", "C301",
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 18),
                720.00, 720.00);

        addRecord(records, "10000016", "Brandon Lim", "C301",
                LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 9, 24),
                900.00, 400.00);

        addRecord(records, "10000017", "Grace Tan", "C301",
                LocalDate.of(2026, 9, 26),
                LocalDate.of(2026, 9, 29),
                680.00, 0.00);


        return records;
    }

    /**
     * Creates and adds one complete booking record.
     */
    private void addRecord(
            ListInterface<FrontDeskRecord> records,
            String confirmationNumber,
            String guestName,
            String roomNumber,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            double totalBill,
            double amountPaid) {

        Booking booking = new Booking(
                confirmationNumber,
                guestName,
                roomNumber,
                checkInDate,
                checkOutDate);

        BillingRecord billingRecord = new BillingRecord(
                confirmationNumber,
                totalBill,
                amountPaid);

        records.add(
                new FrontDeskRecord(
                        booking,
                        billingRecord));
    }
}