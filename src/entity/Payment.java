package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One payment taken against an invoice.
 *
 * An invoice can have several: a deposit at check-in, a part payment during
 * the stay, and the settlement at check-out. Keeping them as separate records
 * rather than only updating a running total on the invoice means each one
 * keeps its own method, reference and time, which is what makes the takings
 * auditable.
 *
 * @author Tew Yong Le
 */
public class Payment implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String CASH = "CASH";
  public static final String CARD = "CARD";
  public static final String EWALLET = "EWALLET";
  public static final String BANK_TRANSFER = "BANK_TRANSFER";

  private String paymentId;
  private String invoiceId;
  private double amount;
  private String method;
  private String reference;
  private LocalDateTime paidAt;
  private String receivedBy;

  public Payment() {
  }

  public Payment(String paymentId, String invoiceId, double amount, String method,
      String reference, LocalDateTime paidAt, String receivedBy) {
    this.paymentId = paymentId;
    this.invoiceId = invoiceId;
    this.amount = amount;
    this.method = method;
    this.reference = reference;
    this.paidAt = paidAt;
    this.receivedBy = receivedBy;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(String paymentId) {
    this.paymentId = paymentId;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(LocalDateTime paidAt) {
    this.paidAt = paidAt;
  }

  public String getReceivedBy() {
    return receivedBy;
  }

  public void setReceivedBy(String receivedBy) {
    this.receivedBy = receivedBy;
  }

  /**
   * Whether this method needs a reference recorded.
   *
   * Cash leaves no trace of its own, so nothing can be recorded for it. Every
   * other method produces an approval code or transaction number, and that is
   * what makes the payment traceable afterwards.
   */
  public static boolean requiresReference(String method) {
    return !CASH.equals(method);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(paymentId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.paymentId, ((Payment) obj).paymentId);
  }

  @Override
  public String toString() {
    return String.format("%-7s %-8s %10.2f %-14s %-16s %s",
        paymentId, invoiceId, amount, method,
        (reference == null || reference.isBlank()) ? "-" : reference, paidAt);
  }
}
