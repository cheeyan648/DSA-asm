package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One physical room, and the two separate things that can be true of it at
 * once: whether someone is in it, and whether it is clean.
 *
 * Those two are deliberately kept apart. Front-Desk owns occupancyStatus and
 * Housekeeping owns housekeepingStatus, and a room is only sellable when both
 * agree - vacant AND ready. Collapsing them into one status field is what made
 * it possible, in the separate modules, to let a guest into a dirty room.
 *
 * @author Tan Chee Yan
 */
public class Room implements Serializable {

  private static final long serialVersionUID = 2L;

  // Occupancy - written by Front-Desk.
  public static final String VACANT = "VACANT";
  public static final String OCCUPIED = "OCCUPIED";
  public static final String RESERVED = "RESERVED";

  // Housekeeping - written by Housekeeping only.
  public static final String DIRTY = "DIRTY";
  public static final String CLEANING_IN_PROGRESS = "CLEANING_IN_PROGRESS";
  public static final String INSPECTED = "INSPECTED";
  public static final String READY_FOR_CHECK_IN = "READY_FOR_CHECK_IN";
  public static final String BLOCKED = "BLOCKED";

  private String roomNo;
  private String typeId;
  private int floorNo;
  private String occupancyStatus;
  private String housekeepingStatus;
  private boolean outOfService;
  private LocalDateTime lastCleanedAt;
  private String remark;

  public Room() {
  }

  public Room(String roomNo, String typeId, int floorNo, String occupancyStatus,
      String housekeepingStatus, boolean outOfService, LocalDateTime lastCleanedAt,
      String remark) {
    this.roomNo = roomNo;
    this.typeId = typeId;
    this.floorNo = floorNo;
    this.occupancyStatus = occupancyStatus;
    this.housekeepingStatus = housekeepingStatus;
    this.outOfService = outOfService;
    this.lastCleanedAt = lastCleanedAt;
    this.remark = remark;
  }

  public String getRoomNo() {
    return roomNo;
  }

  public void setRoomNo(String roomNo) {
    this.roomNo = roomNo;
  }

  public String getTypeId() {
    return typeId;
  }

  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  public int getFloorNo() {
    return floorNo;
  }

  public void setFloorNo(int floorNo) {
    this.floorNo = floorNo;
  }

  public String getOccupancyStatus() {
    return occupancyStatus;
  }

  public void setOccupancyStatus(String occupancyStatus) {
    this.occupancyStatus = occupancyStatus;
  }

  public String getHousekeepingStatus() {
    return housekeepingStatus;
  }

  public void setHousekeepingStatus(String housekeepingStatus) {
    this.housekeepingStatus = housekeepingStatus;
  }

  public boolean isOutOfService() {
    return outOfService;
  }

  public void setOutOfService(boolean outOfService) {
    this.outOfService = outOfService;
  }

  public LocalDateTime getLastCleanedAt() {
    return lastCleanedAt;
  }

  public void setLastCleanedAt(LocalDateTime lastCleanedAt) {
    this.lastCleanedAt = lastCleanedAt;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  /**
   * Whether this room could be given to a guest right now.
   *
   * Three of the four conditions in the assignable rule live on the room
   * itself and are checked here; the fourth - that no existing booking already
   * covers the requested dates - depends on the dates being asked about, so
   * Front-Desk applies it separately.
   *
   * @return true if the room is in service, empty and cleaned
   */
  public boolean isAssignable() {
    return !outOfService
        && VACANT.equals(occupancyStatus)
        && READY_FOR_CHECK_IN.equals(housekeepingStatus);
  }

  /**
   * Whether this room could be made ready by cleaning it.
   *
   * Used when an urgent guest is waiting and nothing is ready: a room that is
   * merely dirty can still be offered, because housekeeping can be asked to
   * clean it out of turn. A room that is occupied or out of service cannot.
   *
   * @return true if the room only needs cleaning to become assignable
   */
  public boolean isCleanable() {
    return !outOfService
        && VACANT.equals(occupancyStatus)
        && (DIRTY.equals(housekeepingStatus)
            || CLEANING_IN_PROGRESS.equals(housekeepingStatus)
            || INSPECTED.equals(housekeepingStatus));
  }

  /**
   * How close this room is to being ready, lowest first.
   *
   * Lets the front desk pick the room that will be free soonest when several
   * could be cleaned: one already inspected needs only a sign-off, while a
   * dirty one needs the whole cycle.
   *
   * @return 0 inspected, 1 being cleaned, 2 dirty, 3 anything else
   */
  public int getReadinessRank() {
    if (INSPECTED.equals(housekeepingStatus)) {
      return 0;
    }
    if (CLEANING_IN_PROGRESS.equals(housekeepingStatus)) {
      return 1;
    }
    if (DIRTY.equals(housekeepingStatus)) {
      return 2;
    }
    return 3;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(roomNo);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.roomNo, ((Room) obj).roomNo);
  }

  @Override
  public String toString() {
    return String.format("%-6s %-6s %5d  %-10s %-22s %s",
        roomNo, typeId, floorNo, occupancyStatus, housekeepingStatus,
        outOfService ? "OUT OF SERVICE" : "");
  }
}
