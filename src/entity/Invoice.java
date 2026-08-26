package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The bill for one booking.
 *
 * amountPaid is a cached total of the payments made against this invoice - the
 * Payment records are the truth, and this field is recalculated from them each
 * time one is taken. The outstanding balance is derived rather than stored, so
 * it can never drift out of step with the two figures it comes from.
 *
 * @author Lim Yong Le
 */
public class Invoice implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String UNPAID = "UNPAID";
  public static final String PARTIAL = "PARTIAL";
  public static final String PAID = "PAID";
  public static final String REFUNDED = "REFUNDED";

  public static final double SERVICE_CHARGE_RATE = 0.10;
  public static final double TAX_RATE = 0.06;

  private String invoiceId;
  private String bookingId;
  private double roomCharge;
  private double serviceCharge;
  private double taxAmount;
  private double discountAmount;
  private double totalAmount;
  private double amountPaid;
  private String paymentStatus;
  private LocalDateTime issuedAt;

  public Invoice() {
  }

  public Invoice(String invoiceId, String bookingId, double roomCharge,
      LocalDateTime issuedAt) {
    this.invoiceId = invoiceId;
    this.bookingId = bookingId;
    this.roomCharge = roomCharge;
    this.discountAmount = 0.0;
    this.amountPaid = 0.0;
    this.issuedAt = issuedAt;
    recalculate();
  }

  /**
   * Recomputes the charges that depend on the room charge and discount.
   *
   * Service charge is a percentage of the room charge, tax applies to both,
   * and any discount comes off the end. The total is floored at zero so a
   * discount larger than the bill cannot produce a negative amount owed.
   */
  public final void recalculate() {
    serviceCharge = round(roomCharge * SERVICE_CHARGE_RATE);
    taxAmount = round((roomCharge + serviceCharge) * TAX_RATE);
    double gross = roomCharge + serviceCharge + taxAmount;
    totalAmount = round(Math.max(0.0, gross - discountAmount));
    updatePaymentStatus();
  }

  /** Rounds money to sen, so displayed and stored figures always agree. */
  private static double round(double amount) {
    return Math.round(amount * 100.0) / 100.0;
  }

  /**
   * Sets the payment status from what has actually been paid.
   *
   * Compared with a small tolerance rather than exactly, because repeated
   * addition of rounded payments can land a fraction of a sen away from the
   * total and would otherwise leave a settled bill showing as PARTIAL.
   */
  private void updatePaymentStatus() {
    if (REFUNDED.equals(paymentStatus)) {
      return;
    }
    if (amountPaid <= 0.005) {
      paymentStatus = UNPAID;
    } else if (amountPaid + 0.005 >= totalAmount) {
      paymentStatus = PAID;
    } else {
      paymentStatus = PARTIAL;
    }
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
  }

  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public double getRoomCharge() {
    return roomCharge;
  }

  public void setRoomCharge(double roomCharge) {
    this.roomCharge = roomCharge;
    recalculate();
  }

  public double getServiceCharge() {
    return serviceCharge;
  }

  public double getTaxAmount() {
    return taxAmount;
  }

  public double getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(double discountAmount) {
    this.discountAmount = Math.max(0.0, discountAmount);
    recalculate();
  }

  public double getTotalAmount() {
    return totalAmount;
  }

  public double getAmountPaid() {
    return amountPaid;
  }

  public void setAmountPaid(double amountPaid) {
    this.amountPaid = round(amountPaid);
    updatePaymentStatus();
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(String paymentStatus) {
    this.paymentStatus = paymentStatus;
  }

  public LocalDateTime getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(LocalDateTime issuedAt) {
    this.issuedAt = issuedAt;
  }

  /** What is still owed. Derived, so it cannot disagree with the figures. */
  public double getOutstandingBalance() {
    return round(Math.max(0.0, totalAmount - amountPaid));
  }

  public boolean isSettled() {
    return getOutstandingBalance() <= 0.005;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(invoiceId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.invoiceId, ((Invoice) obj).invoiceId);
  }

  @Override
  public String toString() {
    return String.format("%-8s %-7s %10.2f %10.2f %10.2f %-9s",
        invoiceId, bookingId, totalAmount, amountPaid, getOutstandingBalance(), paymentStatus);
  }
}
