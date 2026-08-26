package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A message raised for a member - points earned, points about to expire, a
 * tier upgrade, a redemption outcome or a promotion.
 *
 * relatedRefId holds whichever record caused the message - a booking, a
 * redemption or a reward. It is deliberately not a foreign key to any one
 * table, because which table it points at depends on the type.
 *
 * @author Ivan Wong
 */
public class Notification implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String POINTS_EARNED = "POINTS_EARNED";
  public static final String POINTS_EXPIRING = "POINTS_EXPIRING";
  public static final String TIER_UPGRADE = "TIER_UPGRADE";
  public static final String REDEMPTION = "REDEMPTION";
  public static final String PROMOTION = "PROMOTION";

  private String notificationId;
  private String memberId;
  private String type;
  private String message;
  private LocalDate createdDate;
  private boolean read;
  private String relatedRefId;

  public Notification() {
  }

  public Notification(String notificationId, String memberId, String type,
      String message, LocalDate createdDate, String relatedRefId) {
    this.notificationId = notificationId;
    this.memberId = memberId;
    this.type = type;
    this.message = message;
    this.createdDate = createdDate;
    this.relatedRefId = relatedRefId;
    this.read = false;
  }

  public String getNotificationId() {
    return notificationId;
  }

  public void setNotificationId(String notificationId) {
    this.notificationId = notificationId;
  }

  public String getMemberId() {
    return memberId;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public LocalDate getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDate createdDate) {
    this.createdDate = createdDate;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }

  public String getRelatedRefId() {
    return relatedRefId;
  }

  public void setRelatedRefId(String relatedRefId) {
    this.relatedRefId = relatedRefId;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(notificationId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.notificationId, ((Notification) obj).notificationId);
  }

  @Override
  public String toString() {
    return String.format("%-7s %-7s %-16s %-11s %-7s %s",
        notificationId, memberId, type, createdDate, read ? "READ" : "UNREAD", message);
  }
}
