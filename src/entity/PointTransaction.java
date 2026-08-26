package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One movement of points on a member's account.
 *
 * This ledger is the truth about a member's points; Member.pointsBalance is a
 * cached running total kept alongside it so screens do not have to add the
 * whole history up every time. Every earn, redemption, expiry and manual
 * adjustment writes a row here, which is what makes a balance explainable
 * rather than merely asserted.
 *
 * @author Ivan Wong
 */
public class PointTransaction implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String EARN = "EARN";
  public static final String REDEEM = "REDEEM";
  public static final String EXPIRE = "EXPIRE";
  public static final String ADJUST = "ADJUST";

  private String txnId;
  private String memberId;
  private String bookingId;
  private String txnType;
  private int points;
  private int balanceAfter;
  private LocalDate txnDate;
  private String description;

  public PointTransaction() {
  }

  public PointTransaction(String txnId, String memberId, String bookingId, String txnType,
      int points, int balanceAfter, LocalDate txnDate, String description) {
    this.txnId = txnId;
    this.memberId = memberId;
    this.bookingId = bookingId;
    this.txnType = txnType;
    this.points = points;
    this.balanceAfter = balanceAfter;
    this.txnDate = txnDate;
    this.description = description;
  }

  public String getTxnId() {
    return txnId;
  }

  public void setTxnId(String txnId) {
    this.txnId = txnId;
  }

  public String getMemberId() {
    return memberId;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
  }

  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public String getTxnType() {
    return txnType;
  }

  public void setTxnType(String txnType) {
    this.txnType = txnType;
  }

  public int getPoints() {
    return points;
  }

  public void setPoints(int points) {
    this.points = points;
  }

  public int getBalanceAfter() {
    return balanceAfter;
  }

  public void setBalanceAfter(int balanceAfter) {
    this.balanceAfter = balanceAfter;
  }

  public LocalDate getTxnDate() {
    return txnDate;
  }

  public void setTxnDate(LocalDate txnDate) {
    this.txnDate = txnDate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(txnId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.txnId, ((PointTransaction) obj).txnId);
  }

  @Override
  public String toString() {
    return String.format("%-7s %-7s %-8s %+7d %8d  %-11s %s",
        txnId, memberId, txnType, points, balanceAfter, txnDate,
        (description == null ? "" : description));
  }
}
