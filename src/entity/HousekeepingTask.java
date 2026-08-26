package entity;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One cleaning, inspection or maintenance job on a room.
 *
 * The task's lane is never typed in. It is worked out from whether a booking
 * is waiting on this room and whether that booking is itself urgent, so an
 * urgency granted at the front door is what reaches the housekeeper - nobody
 * can promote their own work by hand. If the waiting booking is cancelled the
 * reservation is cleared and the task drops back to the normal lane; the
 * cleaning still goes ahead, because the room is dirty either way.
 *
 * @author Kat Tan
 */
public class HousekeepingTask implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String DIRTY = "DIRTY";
  public static final String CLEANING_IN_PROGRESS = "CLEANING_IN_PROGRESS";
  public static final String INSPECTED = "INSPECTED";
  public static final String READY_FOR_CHECK_IN = "READY_FOR_CHECK_IN";
  public static final String BLOCKED = "BLOCKED";

  public static final String TYPE_CHECKOUT_CLEAN = "CHECKOUT_CLEAN";
  public static final String TYPE_STAYOVER_CLEAN = "STAYOVER_CLEAN";
  public static final String TYPE_DEEP_CLEAN = "DEEP_CLEAN";
  public static final String TYPE_INSPECTION = "INSPECTION";
  public static final String TYPE_MAINTENANCE = "MAINTENANCE";

  public static final String PRIORITY_URGENT = "URGENT";
  public static final String PRIORITY_NORMAL = "NORMAL";

  private String taskId;
  private String roomNo;
  private String taskType;
  private String priority;
  private String status;
  private String bookingId;
  private String reservedForBookingId;
  private String assignedTo;
  private int inspectionFailCount;
  private LocalDateTime createdAt;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private String remark;

  public HousekeepingTask() {
  }

  public HousekeepingTask(String taskId, String roomNo, String taskType,
      String bookingId, LocalDateTime createdAt) {
    this.taskId = taskId;
    this.roomNo = roomNo;
    this.taskType = taskType;
    this.bookingId = bookingId;
    this.createdAt = createdAt;
    this.status = DIRTY;
    this.priority = PRIORITY_NORMAL;
    this.inspectionFailCount = 0;
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

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public String getReservedForBookingId() {
    return reservedForBookingId;
  }

  public void setReservedForBookingId(String reservedForBookingId) {
    this.reservedForBookingId = reservedForBookingId;
  }

  public String getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(String assignedTo) {
    this.assignedTo = assignedTo;
  }

  public int getInspectionFailCount() {
    return inspectionFailCount;
  }

  public void setInspectionFailCount(int inspectionFailCount) {
    this.inspectionFailCount = inspectionFailCount;
  }

  public void incrementInspectionFailCount() {
    inspectionFailCount++;
  }

  /**
   * Takes back one recorded failure.
   *
   * Used when a rollback undoes a failed inspection: without this the failure
   * would stay in the count and the inspection success rate in the reports
   * would keep punishing a mistake that has been reversed. Floored at zero so
   * a repeated rollback can never push it negative.
   */
  public void decrementInspectionFailCount() {
    if (inspectionFailCount > 0) {
      inspectionFailCount--;
    }
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public boolean isUrgent() {
    return PRIORITY_URGENT.equals(priority);
  }

  /** Whether this task still needs to be picked up by a housekeeper. */
  public boolean isPendingCleaning() {
    return DIRTY.equals(status);
  }

  /**
   * How long the cleaning took, in minutes.
   *
   * @return the duration, or -1 while the task is unfinished
   */
  public long getCleaningDurationMinutes() {
    if (startedAt == null || completedAt == null) {
      return -1;
    }
    return Duration.between(startedAt, completedAt).toMinutes();
  }

  /**
   * Whether a status change is allowed by the cleaning workflow.
   *
   * The pipeline runs dirty, cleaning, inspected, ready, and a failed
   * inspection sends the room back to dirty to be done again. Anything else is
   * refused: a room cannot be inspected without being cleaned, cannot skip
   * inspection to reach ready, and cannot move backwards. A room can be
   * blocked from any state and returns to where it was when unblocked.
   *
   * @param from the status the room is at now
   * @param to the status being requested
   * @return true if the workflow permits that move
   */
  public static boolean isValidTransition(String from, String to) {
    if (from == null || to == null || from.equals(to)) {
      return false;
    }
    if (BLOCKED.equals(to)) {
      return true;
    }
    if (BLOCKED.equals(from)) {
      return DIRTY.equals(to) || CLEANING_IN_PROGRESS.equals(to) || INSPECTED.equals(to);
    }
    switch (from) {
      case DIRTY:
        return CLEANING_IN_PROGRESS.equals(to);
      case CLEANING_IN_PROGRESS:
        return INSPECTED.equals(to);
      case INSPECTED:
        // Passed inspection, or failed it and goes back to be cleaned again.
        return READY_FOR_CHECK_IN.equals(to) || DIRTY.equals(to);
      case READY_FOR_CHECK_IN:
        // Only a fresh check-out dirties a ready room.
        return DIRTY.equals(to);
      default:
        return false;
    }
  }

  /** Why a transition was refused, for showing to the user. */
  public static String explainInvalidTransition(String from, String to) {
    if (from != null && from.equals(to)) {
      return "The room is already at " + to + ".";
    }
    if (DIRTY.equals(from) && INSPECTED.equals(to)) {
      return "Cannot inspect a room that has not been cleaned.";
    }
    if (DIRTY.equals(from) && READY_FOR_CHECK_IN.equals(to)) {
      return "Cannot skip cleaning and inspection.";
    }
    if (CLEANING_IN_PROGRESS.equals(from) && READY_FOR_CHECK_IN.equals(to)) {
      return "Inspection is required before a room is ready.";
    }
    if (READY_FOR_CHECK_IN.equals(from) && INSPECTED.equals(to)) {
      return "Cannot move a ready room backwards.";
    }
    return "That is not a valid step in the cleaning workflow.";
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
    return Objects.equals(this.taskId, ((HousekeepingTask) obj).taskId);
  }

  @Override
  public String toString() {
    return String.format("%-7s %-6s %-16s %-7s %-22s %s",
        taskId, roomNo, taskType, priority, status, createdAt);
  }
}
