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
     * Room numbers use 4-digit numeric values.
     *
     * @return sample booking and billing records
     */
    public ListInterface<FrontDeskRecord> initializeBookings() {

        ListInterface<FrontDeskRecord> records =
                new ArrayList<>();

        // ============================================================
        // ROOM 1001
        // ============================================================

        addRecord(records, "10000001", "Tan Chee Yan", "1001",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 4),
                450.00, 450.00);

        addRecord(records, "10000004", "Ivan Wong", "1001",
                LocalDate.of(2026, 9, 6),
                LocalDate.of(2026, 9, 9),
                450.00, 450.00);

        addRecord(records, "10000006", "Daniel Lee", "1001",
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 15),
                500.00, 300.00);

        addRecord(records, "10000007", "Sarah Lim", "1001",
                LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 21),
                480.00, 480.00);

        addRecord(records, "10000008", "Jason Tan", "1001",
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 9, 28),
                520.00, 200.00);

        addRecord(records, "10000018", "William Chong", "1001",
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 5),
                550.00, 100.00);

        addRecord(records, "10000019", "Jessica Lim", "1001",
                LocalDate.of(2026, 10, 8),
                LocalDate.of(2026, 10, 12),
                680.00, 680.00);

        // ============================================================
        // ROOM 1002
        // ============================================================

        addRecord(records, "10000002", "Lim Yong Le", "1002",
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 5),
                600.00, 200.00);

        addRecord(records, "10000009", "Michelle Wong", "1002",
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 10),
                550.00, 300.00);

        addRecord(records, "10000010", "Kevin Chong", "1002",
                LocalDate.of(2026, 9, 13),
                LocalDate.of(2026, 9, 16),
                580.00, 580.00);

        addRecord(records, "10000011", "Emily Tan", "1002",
                LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 9, 23),
                620.00, 100.00);

        addRecord(records, "10000020", "Nicholas Lee", "1002",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 4),
                720.00, 500.00);

        addRecord(records, "10000021", "Olivia Wong", "1002",
                LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 9),
                490.00, 490.00);

        addRecord(records, "10000022", "Aaron Tan", "1002",
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 16),
                850.00, 350.00);

        // ============================================================
        // ROOM 2001
        // ============================================================

        addRecord(records, "10000003",
                "Nur Aisyah binti Rahman",
                "2001",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 8),
                525.00, 0.00);

        addRecord(records, "10000012", "Ahmad Hakim", "2001",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 13),
                550.00, 250.00);

        addRecord(records, "10000013", "Siti Nurhaliza", "2001",
                LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 19),
                575.00, 575.00);

        addRecord(records, "10000014", "Mohd Firdaus", "2001",
                LocalDate.of(2026, 9, 22),
                LocalDate.of(2026, 9, 25),
                600.00, 150.00);

        addRecord(records, "10000023", "Nur Izzati", "2001",
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 6),
                700.00, 0.00);

        addRecord(records, "10000024", "Hafiz Rahman", "2001",
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 13),
                480.00, 300.00);

        addRecord(records, "10000025", "Farah Nadia", "2001",
                LocalDate.of(2026, 10, 18),
                LocalDate.of(2026, 10, 22),
                780.00, 780.00);

        // ============================================================
        // ROOM 3001
        // ============================================================

        addRecord(records, "10000005", "Chong Zhi Ying", "3001",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 13),
                750.00, 300.00);

        addRecord(records, "10000015", "Alice Wong", "3001",
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 18),
                720.00, 720.00);

        addRecord(records, "10000016", "Brandon Lim", "3001",
                LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 9, 24),
                900.00, 400.00);

        addRecord(records, "10000017", "Grace Tan", "3001",
                LocalDate.of(2026, 9, 26),
                LocalDate.of(2026, 9, 29),
                680.00, 0.00);

        addRecord(records, "10000026", "Marcus Lim", "3001",
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 7),
                950.00, 500.00);

        addRecord(records, "10000027", "Sophia Tan", "3001",
                LocalDate.of(2026, 10, 11),
                LocalDate.of(2026, 10, 15),
                1100.00, 1100.00);

        addRecord(records, "10000028", "Jason Wong", "3001",
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 24),
                1250.00, 500.00);

        // ============================================================
        // ROOM 4001
        // ============================================================

        addRecord(records, "10000029", "Ethan Wong", "4001",
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 6),
                300.00, 300.00);

        addRecord(records, "10000030", "Chloe Lim", "4001",
                LocalDate.of(2026, 9, 9),
                LocalDate.of(2026, 9, 12),
                420.00, 200.00);

        addRecord(records, "10000031", "Ryan Tan", "4001",
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 18),
                390.00, 0.00);

        addRecord(records, "10000032", "Emma Lee", "4001",
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 24),
                450.00, 450.00);

        addRecord(records, "10000033", "Lucas Wong", "4001",
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 5),
                520.00, 300.00);

        // ============================================================
        // ROOM 5001
        // Higher-priced room
        // ============================================================

        addRecord(records, "10000034", "Benjamin Tan", "5001",
                LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 8),
                1500.00, 1000.00);

        addRecord(records, "10000035", "Isabella Lim", "5001",
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 16),
                1800.00, 1800.00);

        addRecord(records, "10000036", "Nathan Lee", "5001",
                LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 9, 24),
                1350.00, 500.00);

        addRecord(records, "10000037", "Mia Wong", "5001",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                2000.00, 0.00);

        addRecord(records, "10000038", "Oliver Tan", "5001",
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 15),
                2250.00, 1500.00);

        // ============================================================
        // ROOM 6001
        // ============================================================

        addRecord(records, "10000039", "Lucas Lim", "6001",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 7),
                280.00, 280.00);

        addRecord(records, "10000040", "Amelia Wong", "6001",
                LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 14),
                360.00, 100.00);

        addRecord(records, "10000041", "Henry Tan", "6001",
                LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 22),
                480.00, 200.00);

        addRecord(records, "10000042", "Ella Lee", "6001",
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 6),
                390.00, 390.00);

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

        Booking booking =
                new Booking(
                        confirmationNumber,
                        guestName,
                        roomNumber,
                        checkInDate,
                        checkOutDate);

        BillingRecord billingRecord =
                new BillingRecord(
                        confirmationNumber,
                        totalBill,
                        amountPaid);

        records.add(
                new FrontDeskRecord(
                        booking,
                        billingRecord));
    }
}