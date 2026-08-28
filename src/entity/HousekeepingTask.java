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
  public static final String NOT_CLEANED = "NOT_CLEANED";
  public static final String CLEANED = "CLEANED";

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

  // Creates an empty housekeeping task.
  public HousekeepingTask() {
  }

  // Creates a new housekeeping task that starts DIRTY and in the normal lane.
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

  // Adds one failed inspection to this task's count.
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

  // Whether this task is in the urgent lane.
  public boolean isUrgent() {
    return PRIORITY_URGENT.equals(priority);
  }

  /**
   * Whether this task is actual room cleaning, not inspection or maintenance.
   */
  public boolean isCleaningType() {
    return TYPE_CHECKOUT_CLEAN.equals(taskType)
        || TYPE_STAYOVER_CLEAN.equals(taskType)
        || TYPE_DEEP_CLEAN.equals(taskType);
  }

  /** Whether this task is a maintenance job rather than cleaning. */
  public boolean isMaintenanceType() {
    return TYPE_MAINTENANCE.equals(taskType);
  }

  /** Whether this task is a supervisor inspection job. */
  public boolean isInspectionType() {
    return TYPE_INSPECTION.equals(taskType);
  }

  /**
   * Whether this is a daily stayover-service record, not a regular
   * STAYOVER_CLEAN pipeline job.
   */
  public boolean isStayoverService() {
    if (!TYPE_STAYOVER_CLEAN.equals(taskType)) {
      return false;
    }
    if (NOT_CLEANED.equals(status) || CLEANED.equals(status)) {
      return true;
    }
    return remark != null && remark.startsWith("Stayover service");
  }

  /**
   * Whether this task was replaced by a later cleaning job.
   *
   * Used when maintenance is resolved or an inspection fails: the original
   * row stays in the list as history, and a new cleaning task is what enters
   * the queue.
   */
  public boolean isSuperseded() {
    return remark != null && remark.startsWith("Superseded by ");
  }

  /** Whether this task was raised as the next clean after another job. */
  public boolean isFollowOnCleaning() {
    return isCleaningType() && remark != null && remark.startsWith("Follow-on of ");
  }

  /**
   * Whether this task is still live regular housekeeping work.
   *
   * Ready jobs, superseded jobs and daily stayover-service records are not
   * updated through Update Task Status.
   */
  public boolean isActiveWork() {
    return !isStayoverService() && !READY_FOR_CHECK_IN.equals(status)
        && !isSuperseded() && completedAt == null;
  }

  /** Whether this task still needs to be picked up by a housekeeper. */
  public boolean isPendingCleaning() {
    return !isStayoverService() && isCleaningType() && DIRTY.equals(status)
        && completedAt == null && !isSuperseded();
  }

  /**
   * Whether this is current outstanding cleaning work, not finished history.
   *
   * DIRTY is waiting to be picked up; CLEANING_IN_PROGRESS is already being
   * done. Inspected, ready, blocked and inspection/maintenance jobs are not
   * outstanding cleaning.
   */
  public boolean isOutstandingCleaning() {
    return !isStayoverService() && (isPendingCleaning()
        || (isCleaningType() && CLEANING_IN_PROGRESS.equals(status)
            && completedAt == null && !isSuperseded()));
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
   * blocked from any state and returns to DIRTY or CLEANING_IN_PROGRESS, not
   * to INSPECTED or READY.
   *
   * @param from the status the room is at now
   * @param to the status being requested
   * @return true if the workflow permits that move
   */
  public static boolean isValidTransition(String from, String to) {
    return isValidTransition(null, from, to);
  }

  /**
   * Whether a status change is allowed for this kind of task.
   *
   * Cleaning follows the usual pipeline. Daily stayover service uses
   * NOT_CLEANED, CLEANING_IN_PROGRESS and CLEANED. Maintenance never starts
   * cleaning: it can only be blocked or resolved back to DIRTY, after which a
   * separate cleaning task is raised. Inspection is sign-off work, not queue
   * work.
   *
   * @param taskType cleaning, inspection or maintenance, or null for cleaning
   * @param from the status the task is at now
   * @param to the status being requested
   * @return true if that move is allowed
   */
  public static boolean isValidTransition(String taskType, String from, String to) {
    if (from == null || to == null || from.equals(to)) {
      return false;
    }
    if (TYPE_STAYOVER_CLEAN.equals(taskType)
        && (NOT_CLEANED.equals(from) || CLEANED.equals(from)
            || NOT_CLEANED.equals(to) || CLEANED.equals(to))) {
      if (NOT_CLEANED.equals(from) && CLEANING_IN_PROGRESS.equals(to)) {
        return true;
      }
      return CLEANING_IN_PROGRESS.equals(from) && CLEANED.equals(to);
    }
    if (TYPE_MAINTENANCE.equals(taskType)) {
      if (CLEANING_IN_PROGRESS.equals(to) || INSPECTED.equals(to)
          || READY_FOR_CHECK_IN.equals(to)) {
        return false;
      }
      if (BLOCKED.equals(to)) {
        return true;
      }
      return BLOCKED.equals(from) && DIRTY.equals(to);
    }
    if (TYPE_INSPECTION.equals(taskType) && CLEANING_IN_PROGRESS.equals(to)) {
      return false;
    }
    if (BLOCKED.equals(to)) {
      return true;
    }
    if (BLOCKED.equals(from)) {
      return DIRTY.equals(to) || CLEANING_IN_PROGRESS.equals(to);
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

  /** The statuses the UI may offer from this task's type and current status. */
  public static String[] allowedNextStatuses(String taskType, String currentStatus) {
    String[] every = {
      DIRTY, CLEANING_IN_PROGRESS, INSPECTED, READY_FOR_CHECK_IN, BLOCKED
    };
    int count = 0;
    for (String candidate : every) {
      if (isValidTransition(taskType, currentStatus, candidate)) {
        count++;
      }
    }
    String[] allowed = new String[count];
    int next = 0;
    for (String candidate : every) {
      if (isValidTransition(taskType, currentStatus, candidate)) {
        allowed[next++] = candidate;
      }
    }
    return allowed;
  }

  /** Why a transition was refused, for showing to the user. */
  public static String explainInvalidTransition(String from, String to) {
    if (from != null && from.equals(to)) {
      return "The room is already at " + to + ".";
    }
    if (DIRTY.equals(from) && CLEANING_IN_PROGRESS.equals(to)) {
      return "MAINTENANCE is not a cleaning task.";
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
    if (BLOCKED.equals(from) && (INSPECTED.equals(to) || READY_FOR_CHECK_IN.equals(to))) {
      return "A blocked room must be cleaned again before inspection or check-in.";
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
