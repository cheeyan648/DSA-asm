package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 *
 * @author Kat Tan
 */
public class LoyaltyNotification implements Serializable {

  private String notificationId;
  private String memberId;
  private String type;
  private String message;
  private LocalDate createdDate;
  private boolean read;

  public LoyaltyNotification() {
  }

  public LoyaltyNotification(String notificationId, String memberId, String type,
      String message, LocalDate createdDate, boolean read) {
    this.notificationId = notificationId;
    this.memberId = memberId;
    this.type = type;
    this.message = message;
    this.createdDate = createdDate;
    this.read = read;
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
    final LoyaltyNotification other = (LoyaltyNotification) obj;
    return Objects.equals(this.notificationId, other.notificationId);
  }

  @Override
  public String toString() {
    return String.format("%-10s %-18s %-10s %-12s %s",
        notificationId, type, memberId, createdDate, read ? "READ" : "UNREAD");
  }
}
