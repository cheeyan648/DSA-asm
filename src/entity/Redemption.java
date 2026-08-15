package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 *
 * @author Kat Tan
 */
public class Redemption implements Serializable {

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_COMPLETED = "COMPLETED";
  public static final String STATUS_REJECTED = "REJECTED";

  private String redemptionId;
  private String memberId;
  private String rewardId;
  private int pointsUsed;
  private LocalDate requestDate;
  private String status;

  public Redemption() {
  }

  public Redemption(String redemptionId, String memberId, String rewardId,
      int pointsUsed, LocalDate requestDate, String status) {
    this.redemptionId = redemptionId;
    this.memberId = memberId;
    this.rewardId = rewardId;
    this.pointsUsed = pointsUsed;
    this.requestDate = requestDate;
    this.status = status;
  }

  public String getRedemptionId() {
    return redemptionId;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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
    final Redemption other = (Redemption) obj;
    return Objects.equals(this.redemptionId, other.redemptionId);
  }

  @Override
  public String toString() {
    return String.format("%-10s %-10s %-8s %8d %-12s %-10s",
        redemptionId, memberId, rewardId, pointsUsed, requestDate, status);
  }
}
