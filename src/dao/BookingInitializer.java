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

        addRecord(records, "10000001", "Tan Chee Yan", "A101",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 4), 450.00, 450.00);
        addRecord(records, "10000002", "Lim Yong Le", "A102",
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 5), 600.00, 200.00);
        addRecord(records, "10000003", "Nur Aisyah binti Rahman", "B201",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 8), 525.00, 0.00);
        addRecord(records, "10000004", "Ivan Wong", "A101",
                LocalDate.of(2026, 9, 6),
                LocalDate.of(2026, 9, 9), 450.00, 450.00);
        addRecord(records, "10000005", "Chong Zhi Ying", "C301",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 13), 750.00, 300.00);

        return records;
    }

    private void addRecord(ListInterface<FrontDeskRecord> records,
            String confirmationNumber, String guestName, String roomNumber,
            LocalDate checkInDate, LocalDate checkOutDate,
            double totalBill, double amountPaid) {

        Booking booking = new Booking(confirmationNumber, guestName,
                roomNumber, checkInDate, checkOutDate);
        BillingRecord billingRecord = new BillingRecord(confirmationNumber,
                totalBill, amountPaid);

        records.add(new FrontDeskRecord(booking, billingRecord));
    }
}
