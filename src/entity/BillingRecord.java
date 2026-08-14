package entity;

import java.io.Serializable;

/**
 * Stores billing information for a booking.
 *
 * @author Yong Le
 */
public class BillingRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private String confirmationNumber;
    private double totalBill;
    private double amountPaid;

    public BillingRecord(String confirmationNumber,
            double totalBill,
            double amountPaid) {

        this.confirmationNumber = confirmationNumber;
        this.totalBill = totalBill;
        this.amountPaid = amountPaid;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public double getTotalBill() {
        return totalBill;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public double getOutstandingBalance() {
        return totalBill - amountPaid;
    }

    public String getPaymentStatus() {
        if (getOutstandingBalance() <= 0) {
            return "PAID";
        }

        return "OUTSTANDING";
    }
}