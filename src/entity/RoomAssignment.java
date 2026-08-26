package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A record of which physical room a booking occupied, and for how long.
 *
 * Kept as its own history rather than only as a field on the booking, because
 * a guest can be moved: a room change closes one row (releasedAt is stamped)
 * and opens another, so the reason for every move survives. Booking.roomNo
 * always mirrors whichever row is still open.
 *
 * @author Lim Yong Le
 */
public class RoomAssignment implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String REASON_INITIAL = "INITIAL";
  public static final String REASON_UPGRADE = "UPGRADE";
  public static final String REASON_GUEST_REQUEST = "GUEST_REQUEST";
  public static final String REASON_MAINTENANCE = "MAINTENANCE";

  private String assignmentId;
  private String bookingId;
  private String roomNo;
  private LocalDateTime assignedAt;
  private LocalDateTime releasedAt;
  private String assignedBy;
  private String reason;

  public RoomAssignment() {
  }

  public RoomAssignment(String assignmentId, String bookingId, String roomNo,
      LocalDateTime assignedAt, String assignedBy, String reason) {
    this.assignmentId = assignmentId;
    this.bookingId = bookingId;
    this.roomNo = roomNo;
    this.assignedAt = assignedAt;
    this.assignedBy = assignedBy;
    this.reason = reason;
  }

  public String getAssignmentId() {
    return assignmentId;
  }

  public void setAssignmentId(String assignmentId) {
    this.assignmentId = assignmentId;
  }

  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public String getRoomNo() {
    return roomNo;
  }

  public void setRoomNo(String roomNo) {
    this.roomNo = roomNo;
  }

  public LocalDateTime getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(LocalDateTime assignedAt) {
    this.assignedAt = assignedAt;
  }

  public LocalDateTime getReleasedAt() {
    return releasedAt;
  }

  public void setReleasedAt(LocalDateTime releasedAt) {
    this.releasedAt = releasedAt;
  }

  public String getAssignedBy() {
    return assignedBy;
  }

  public void setAssignedBy(String assignedBy) {
    this.assignedBy = assignedBy;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  /** Whether the booking still holds this room. */
  public boolean isOpen() {
    return releasedAt == null;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(assignmentId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.assignmentId, ((RoomAssignment) obj).assignmentId);
  }

  @Override
  public String toString() {
    return String.format("%-7s %-7s %-6s %-17s %-17s %s",
        assignmentId, bookingId, roomNo, assignedAt,
        (releasedAt == null ? "-" : releasedAt.toString()), reason);
  }
}
