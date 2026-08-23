package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 *
 * @author Kat Tan
 */
public class HousekeepingTask implements Serializable {
  private String taskId;
  private String roomNumber;
  private String status;
  private LocalDateTime timestamp;

  /** Creates an empty housekeeping task. */
  public HousekeepingTask() {
  }

  /** Creates a task with its ID, room, status, and update time. */
  public HousekeepingTask(String taskId, String roomNumber, String status, LocalDateTime timestamp) {
    this.taskId = taskId;
    this.roomNumber = roomNumber;
    this.status = status;
    this.timestamp = timestamp;
  }

  /** Returns the task ID. */
  public String getTaskId() {
    return taskId;
  }

  /** Updates the task ID. */
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  /** Returns the room number. */
  public String getRoomNumber() {
    return roomNumber;
  }

  /** Updates the room number. */
  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  /** Returns the housekeeping status. */
  public String getStatus() {
    return status;
  }

  /** Updates the housekeeping status. */
  public void setStatus(String status) {
    this.status = status;
  }

  /** Returns the time when the status was logged. */
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /** Updates the status log time. */
  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  /** Returns a hash code based on the task ID. */
  @Override
  public int hashCode() {
    return Objects.hashCode(taskId);
  }

  /** Compares two tasks by task ID. */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final HousekeepingTask other = (HousekeepingTask) obj;
    return Objects.equals(this.taskId, other.taskId);
  }

  /** Returns a readable text version of this task. */
  @Override
  public String toString() {
    return String.format("%-10s %-10s %-15s %s", taskId, roomNumber, status, timestamp);
  }
}
