package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A member's request to exchange points for a reward.
 *
 * pointsUsed is a snapshot taken when the request is made, so a later change to
 * the reward's price cannot alter a request already in the queue.
 *
 * Points are only ever deducted when a request is approved. A rejected request
 * costs the member nothing, and the check that decides it happens at
 * processing time rather than at request time so the reason for a refusal is
 * recorded and can be reviewed.
 *
 * @author Ivan Tan Yann Rong
 */
public class Redemption implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String PENDING = "PENDING";
  public static final String APPROVED = "APPROVED";
  public static final String REJECTED = "REJECTED";
  public static final String CANCELLED = "CANCELLED";

  private String redemptionId;
  private String memberId;
  private String rewardId;
  private int pointsUsed;
  private LocalDate requestDate;
  private LocalDate processedDate;
  private String status;
  private String rejectReason;
  private String processedBy;
  private String invoiceId;

  /**
   * The stay this reward belongs to.
   *
   * A reward is enjoyed during a particular visit, so the request carries the
   * booking it was asked for - which is what lets a loyalty officer see whose
   * stay they are approving rather than only which member asked.
   */
  private String bookingId;

  public Redemption() {
  }

  public Redemption(String redemptionId, String memberId, String rewardId,
      int pointsUsed, LocalDate requestDate) {
    this.redemptionId = redemptionId;
    this.memberId = memberId;
    this.rewardId = rewardId;
    this.pointsUsed = pointsUsed;
    this.requestDate = requestDate;
    this.status = PENDING;
  }

  public String getRedemptionId() {
    return redemptionId;
  }

  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public void setRedemptionId(String redemptionId) {
    this.redemptionId = redemptionId;
  }

  public String getMemberId() {
    return memberId;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
  }

  public String getRewardId() {
    return rewardId;
  }

  public void setRewardId(String rewardId) {
    this.rewardId = rewardId;
  }

  public int getPointsUsed() {
    return pointsUsed;
  }

  public void setPointsUsed(int pointsUsed) {
    this.pointsUsed = pointsUsed;
  }

  public LocalDate getRequestDate() {
    return requestDate;
  }

  public void setRequestDate(LocalDate requestDate) {
    this.requestDate = requestDate;
  }

  public LocalDate getProcessedDate() {
    return processedDate;
  }

  public void setProcessedDate(LocalDate processedDate) {
    this.processedDate = processedDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }

  public String getProcessedBy() {
    return processedBy;
  }

  public void setProcessedBy(String processedBy) {
    this.processedBy = processedBy;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
  }

  public boolean isPending() {
    return PENDING.equals(status);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(redemptionId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.redemptionId, ((Redemption) obj).redemptionId);
  }

  @Override
  public String toString() {
    return String.format("%-7s %-7s %-6s %7d  %-11s %-10s %s",
        redemptionId, memberId, rewardId, pointsUsed, requestDate, status,
        (processedDate == null ? "-" : processedDate.toString()));
  }
}
