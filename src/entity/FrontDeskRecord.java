package entity;

import java.io.Serializable;

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
}