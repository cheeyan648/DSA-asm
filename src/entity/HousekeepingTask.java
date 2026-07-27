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

  public HousekeepingTask() {
  }

  public HousekeepingTask(String taskId, String roomNumber, String status, LocalDateTime timestamp) {
    this.taskId = taskId;
    this.roomNumber = roomNumber;
    this.status = status;
    this.timestamp = timestamp;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(taskId);
  }

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

  @Override
  public String toString() {
    return String.format("%-10s %-10s %-15s %s", taskId, roomNumber, status, timestamp);
  }
}
