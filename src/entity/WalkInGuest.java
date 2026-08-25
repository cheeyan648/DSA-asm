package entity;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * One guest who walked in to the resort without a prior reservation.
 *
 * Guests are normally handled chronologically (arrival order). A guest may be
 * flagged as URGENT by the front-desk officer when circumstances require an
 * exception - an elderly or disabled guest, a medical need, a guest travelling
 * with young children, or a complaint escalation. The reason for that exception
 * is recorded so it can be reviewed later.
 *
 * @author Tan Chee Yan
 */
public class WalkInGuest implements Serializable {

  // Status values a guest moves through. Kept as constants rather than an enum
  // so the existing .dat serialization stays simple.
  public static final String STATUS_WAITING = "WAITING";
  public static final String STATUS_SERVED = "SERVED";
  public static final String STATUS_CANCELLED = "CANCELLED";

  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private String guestId;
  private String name;
  private String contactNumber;
  private boolean urgent;
  private String urgencyReason;
  private LocalDateTime arrivalTime;
  private LocalDateTime servedTime;
  private String status;

  public WalkInGuest() {
  }

  public WalkInGuest(String guestId, String name, String contactNumber,
      boolean urgent, String urgencyReason, LocalDateTime arrivalTime) {
    this.guestId = guestId;
    this.name = name;
    this.contactNumber = contactNumber;
    this.urgent = urgent;
    this.urgencyReason = urgencyReason;
    this.arrivalTime = arrivalTime;
    this.status = STATUS_WAITING;
  }

  public String getGuestId() {
    return guestId;
  }

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public void setContactNumber(String contactNumber) {
    this.contactNumber = contactNumber;
  }

  public boolean isUrgent() {
    return urgent;
  }

  public void setUrgent(boolean urgent) {
    this.urgent = urgent;
  }

  public String getUrgencyReason() {
    return urgencyReason;
  }

  public void setUrgencyReason(String urgencyReason) {
    this.urgencyReason = urgencyReason;
  }

  public LocalDateTime getArrivalTime() {
    return arrivalTime;
  }

  public void setArrivalTime(LocalDateTime arrivalTime) {
    this.arrivalTime = arrivalTime;
  }

  public LocalDateTime getServedTime() {
    return servedTime;
  }

  public void setServedTime(LocalDateTime servedTime) {
    this.servedTime = servedTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * How long this guest waited, in whole minutes.
   *
   * For a guest who has been served that is arrival to service; for one still
   * waiting it is arrival until now, so the figure keeps growing while they
   * wait.
   *
   * @return the wait in minutes, or 0 if the arrival time is unknown
   */
  public long getWaitingMinutes() {
    if (arrivalTime == null) {
      return 0;
    }

    LocalDateTime end = (servedTime != null) ? servedTime : LocalDateTime.now();
    long minutes = Duration.between(arrivalTime, end).toMinutes();
    return (minutes < 0) ? 0 : minutes;
  }

  /**
   * The guest's type as shown to the user.
   *
   * @return "URGENT" for an exception case, otherwise "NORMAL"
   */
  public String getGuestType() {
    return urgent ? "URGENT" : "NORMAL";
  }

  /**
   * The arrival time formatted for display.
   *
   * @return the arrival time as dd/MM HH:mm, or "-" if it is unknown
   */
  public String getFormattedArrivalTime() {
    return (arrivalTime == null) ? "-" : arrivalTime.format(TIME_FORMAT);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(guestId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WalkInGuest other = (WalkInGuest) obj;
    return Objects.equals(this.guestId, other.guestId);
  }

  /**
   * How long this guest waited, written for people rather than as raw minutes.
   *
   * @return e.g. "45m" or "2h 05m", or "-" if the arrival time is unknown
   */
  public String getFormattedWaitingTime() {
    if (arrivalTime == null) {
      return "-";
    }

    long minutes = getWaitingMinutes();
    if (minutes < 60) {
      return minutes + "m";
    }
    return String.format("%dh %02dm", minutes / 60, minutes % 60);
  }

  /**
   * The guest's details as one table row, without any queue position - the
   * boundary class adds the position and "ahead" columns, because those depend
   * on where the guest sits in a particular list rather than on the guest.
   */
  @Override
  public String toString() {
    return String.format("%-9s %-26s %-14s %-8s %-17s %-9s",
        guestId, name, contactNumber, getGuestType(), getFormattedArrivalTime(), status);
  }
}
