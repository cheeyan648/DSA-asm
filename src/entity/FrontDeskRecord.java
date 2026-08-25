package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Combines booking and billing information
 * for permanent file storage.
 *
 * @author Yong Le
 */
public class FrontDeskRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Booking booking;
    private BillingRecord billingRecord;

    public FrontDeskRecord(Booking booking,
            BillingRecord billingRecord) {

        this.booking = booking;
        this.billingRecord = billingRecord;
    }

    public Booking getBooking() {
        return booking;
    }

    public BillingRecord getBillingRecord() {
        return billingRecord;
    }

    /**
     * Two front-desk records are the same record when they pair the same
     * booking with the same bill. Overridden so the collection ADT's contains,
     * getPosition and removeEntry operations can match a record by identity
     * rather than by object reference.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof FrontDeskRecord)) {
            return false;
        }

        FrontDeskRecord otherRecord = (FrontDeskRecord) other;
        return Objects.equals(booking, otherRecord.booking)
                && Objects.equals(billingRecord, otherRecord.billingRecord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(booking, billingRecord);
    }

    @Override
    public String toString() {
        return String.format("FrontDeskRecord[%s | %s]", booking, billingRecord);
    }
}