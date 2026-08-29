package entity;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * One guest who walked in without a reservation, and their place in the queue.
 *
 * Guests are normally handled in arrival order. A guest may be flagged URGENT
 * by the officer when circumstances require an exception - an elderly or
 * disabled guest, a medical need, a guest travelling with young children, or a
 * complaint escalation - and the reason is recorded so every override can be
 * reviewed afterwards.
 *
 * A registration is only ever in one place. WAITING means the guest is in a
 * lane of the waiting list; the moment the officer calls them, the status
 * becomes IN_SERVICE and calledAt is stamped, and they are no longer in any
 * queue. Nothing can be waiting and served at the same time.
 *
 * @author Wong Chee Yan
 */
public class WalkInRegistration implements Serializable {

  private static final long serialVersionUID = 3L;

  public static final String STATUS_WAITING = "WAITING";
  public static final String STATUS_IN_SERVICE = "IN_SERVICE";
  public static final String STATUS_BOOKED = "BOOKED";
  public static final String STATUS_CANCELLED = "CANCELLED";
  public static final String STATUS_NO_SHOW = "NO_SHOW";

  public static final String PRIORITY_URGENT = "URGENT";
  public static final String PRIORITY_NORMAL = "NORMAL";

  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private String regId;
  private String guestId;
  private LocalDateTime arrivalTime;

  /**
   * When the guest joined the waiting queue, and when they left it.
   *
   * A registration is recorded at both ends of the wait rather than only at
   * the end: queuedAt is stamped as they join the line and never changes, and
   * servedAt is stamped when the status leaves WAITING. Keeping both means the
   * wait can still be measured after the guest has gone, which a single
   * timestamp overwritten in place could not do.
   */
  private LocalDateTime queuedAt;
  private LocalDateTime servedAt;
  private String priority;
  private String urgencyReason;
  private String requestedTypeId;
  private int requestedNights;

  /**
   * The arrival date the guest gave at the desk. The departure date is never
   * stored alongside it - it is always derived from this date plus the nights,
   * so the two can never drift apart if either is later amended.
   */
  private LocalDate requestedCheckInDate;
  private String status;
  private LocalDateTime calledAt;
  private LocalDateTime bookedAt;
  private String servedBy;
  private String bookingId;

  public WalkInRegistration() {
  }

  public WalkInRegistration(String regId, String guestId, LocalDateTime arrivalTime,
      String priority, String urgencyReason, String requestedTypeId, int requestedNights) {
    this(regId, guestId, arrivalTime, priority, urgencyReason, requestedTypeId,
        requestedNights, arrivalTime == null ? null : arrivalTime.toLocalDate());
  }

  public WalkInRegistration(String regId, String guestId, LocalDateTime arrivalTime,
      String priority, String urgencyReason, String requestedTypeId, int requestedNights,
      LocalDate requestedCheckInDate) {
    this.regId = regId;
    this.guestId = guestId;
    this.arrivalTime = arrivalTime;
    this.priority = priority;
    this.urgencyReason = urgencyReason;
    this.requestedTypeId = requestedTypeId;
    this.requestedNights = requestedNights;
    this.requestedCheckInDate = requestedCheckInDate;
    this.status = STATUS_WAITING;
    this.queuedAt = arrivalTime;
  }

  public String getRegId() {
    return regId;
  }

  public void setRegId(String regId) {
    this.regId = regId;
  }

  public String getGuestId() {
    return guestId;
  }

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public LocalDateTime getArrivalTime() {
    return arrivalTime;
  }

  public void setArrivalTime(LocalDateTime arrivalTime) {
    this.arrivalTime = arrivalTime;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getUrgencyReason() {
    return urgencyReason;
  }

  public void setUrgencyReason(String urgencyReason) {
    this.urgencyReason = urgencyReason;
  }

  public String getRequestedTypeId() {
    return requestedTypeId;
  }

  public void setRequestedTypeId(String requestedTypeId) {
    this.requestedTypeId = requestedTypeId;
  }

  public int getRequestedNights() {
    return requestedNights;
  }

  public void setRequestedNights(int requestedNights) {
    this.requestedNights = requestedNights;
  }

  public LocalDate getRequestedCheckInDate() {
    return requestedCheckInDate;
  }

  public void setRequestedCheckInDate(LocalDate requestedCheckInDate) {
    this.requestedCheckInDate = requestedCheckInDate;
  }

  /**
   * The departure date the requested stay works out to.
   *
   * @return check-in plus the nights requested, or null if no date was given
   */
  public LocalDate getRequestedCheckOutDate() {
    return (requestedCheckInDate == null)
        ? null : requestedCheckInDate.plusDays(requestedNights);
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getCalledAt() {
    return calledAt;
  }

  public void setCalledAt(LocalDateTime calledAt) {
    this.calledAt = calledAt;
  }

  public LocalDateTime getBookedAt() {
    return bookedAt;
  }

  public void setBookedAt(LocalDateTime bookedAt) {
    this.bookedAt = bookedAt;
  }

  public String getServedBy() {
    return servedBy;
  }

  public void setServedBy(String servedBy) {
    this.servedBy = servedBy;
  }

  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public LocalDateTime getQueuedAt() {
    return queuedAt;
  }

  public void setQueuedAt(LocalDateTime queuedAt) {
    this.queuedAt = queuedAt;
  }

  public LocalDateTime getServedAt() {
    return servedAt;
  }

  public void setServedAt(LocalDateTime servedAt) {
    this.servedAt = servedAt;
  }

  /**
   * Records that the guest has left the waiting queue.
   *
   * Every way out of the queue comes through here - called to the counter,
   * cancelled, or marked a no-show - so the moment the wait ended is stamped
   * once, in one place, whatever ended it.
   *
   * @param newStatus what the registration becomes
   * @param when the moment they left the queue
   */
  public void leaveQueue(String newStatus, LocalDateTime when) {
    this.status = newStatus;
    this.servedAt = when;
    if (STATUS_IN_SERVICE.equals(newStatus)) {
      this.calledAt = when;
    }
  }

  /** The moment the guest joined the queue, formatted for display. */
  public String getFormattedQueuedAt() {
    return (queuedAt == null) ? "-" : queuedAt.format(TIME_FORMAT);
  }

  /** The moment the guest left the queue, formatted for display. */
  public String getFormattedServedAt() {
    return (servedAt == null) ? "-" : servedAt.format(TIME_FORMAT);
  }

  public boolean isUrgent() {
    return PRIORITY_URGENT.equals(priority);
  }

  public boolean isWaiting() {
    return STATUS_WAITING.equals(status);
  }

  /**
   * How long this guest waited, in whole minutes.
   *
   * For a guest who has been called that is arrival to being called; for one
   * still waiting it is arrival until now, so the figure keeps growing while
   * they wait.
   *
   * @return the wait in minutes, or 0 if the arrival time is unknown
   */
  public long getWaitingMinutes() {
    LocalDateTime start = (queuedAt != null) ? queuedAt : arrivalTime;
    if (start == null) {
      return 0;
    }

    // A finished wait is measured between the two stored stamps; one still
    // running is measured to now, so the figure keeps growing on screen.
    LocalDateTime end = servedAt;
    if (end == null) {
      end = (calledAt != null) ? calledAt : LocalDateTime.now();
    }

    long minutes = Duration.between(start, end).toMinutes();
    return (minutes < 0) ? 0 : minutes;
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
   * The arrival time formatted for display.
   *
   * @return the arrival time as dd/MM/yyyy HH:mm, or "-" if it is unknown
   */
  public String getFormattedArrivalTime() {
    return (arrivalTime == null) ? "-" : arrivalTime.format(TIME_FORMAT);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(regId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.regId, ((WalkInRegistration) obj).regId);
  }

  /**
   * The registration as one table row, without any queue position - the
   * boundary class adds the position and "ahead" columns, because those depend
   * on where the guest sits in a particular list rather than on the guest.
   */
  @Override
  public String toString() {
    return String.format("%-7s %-6s %-8s %-6s %5d  %-11s %s",
        regId, guestId, priority, requestedTypeId, requestedNights, status,
        getFormattedArrivalTime());
  }
}
