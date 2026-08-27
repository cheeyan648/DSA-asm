package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One recorded change of a room's housekeeping status.
 *
 * Append-only. A rollback does not delete the row it undoes - it adds a
 * compensating row marked isRollback. Deleting would silently change reports
 * that have already been produced, because the analysis counts inspection
 * failures and re-cleaning from this history; adding leaves the trail intact
 * and still restores the room to the right status.
 *
 * @author Kat Tan
 */
public class RoomStatusLog implements Serializable {

  private static final long serialVersionUID = 2L;

  private String logId;
  private String taskId;
  private String roomNo;
  private String fromStatus;
  private String toStatus;
  private LocalDateTime changedAt;
  private String changedBy;
  private boolean rollback;
  private String remark;

  // Creates an empty room-status history row.
  public RoomStatusLog() {
  }

  // Creates one recorded housekeeping status change for a room.
  public RoomStatusLog(String logId, String taskId, String roomNo, String fromStatus,
      String toStatus, LocalDateTime changedAt, String changedBy, boolean rollback,
      String remark) {
    this.logId = logId;
    this.taskId = taskId;
    this.roomNo = roomNo;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.changedAt = changedAt;
    this.changedBy = changedBy;
    this.rollback = rollback;
    this.remark = remark;
  }

  public String getLogId() {
    return logId;
  }

  public void setLogId(String logId) {
    this.logId = logId;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getRoomNo() {
    return roomNo;
  }

  public void setRoomNo(String roomNo) {
    this.roomNo = roomNo;
  }

  public String getFromStatus() {
    return fromStatus;
  }

  public void setFromStatus(String fromStatus) {
    this.fromStatus = fromStatus;
  }

  public String getToStatus() {
    return toStatus;
  }

  public void setToStatus(String toStatus) {
    this.toStatus = toStatus;
  }

  public LocalDateTime getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(LocalDateTime changedAt) {
    this.changedAt = changedAt;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public boolean isRollback() {
    return rollback;
  }

  public void setRollback(boolean rollback) {
    this.rollback = rollback;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(logId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.logId, ((RoomStatusLog) obj).logId);
  }

  @Override
  public String toString() {
    return String.format("%-7s %-7s %-6s %-22s %-22s %s",
        logId, taskId, roomNo,
        (fromStatus == null ? "-" : fromStatus), toStatus, changedAt);
  }
}
