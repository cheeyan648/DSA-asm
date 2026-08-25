package entity;

import java.io.Serializable;
import java.util.Objects;

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

    /**
     * Two billing records refer to the same bill when they carry the same
     * confirmation number, since one booking has exactly one bill. Overridden
     * so the collection ADT's contains, getPosition and removeEntry operations
     * can match a record by identity rather than by object reference.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof BillingRecord)) {
            return false;
        }

        BillingRecord otherRecord = (BillingRecord) other;
        return Objects.equals(confirmationNumber,
                otherRecord.confirmationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confirmationNumber);
    }

    @Override
    public String toString() {
        return String.format(
                "BillingRecord[%s, total=%.2f, paid=%.2f, %s]",
                confirmationNumber, totalBill, amountPaid, getPaymentStatus());
    }
}