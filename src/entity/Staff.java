package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * One member of resort staff. Shared master - every module records who
 * performed an action by holding a staffId.
 *
 * @author Wong Chee Yan
 */
public class Staff implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String ROLE_FRONT_DESK = "FRONT_DESK";
  public static final String ROLE_HOUSEKEEPER = "HOUSEKEEPER";
  public static final String ROLE_SUPERVISOR = "SUPERVISOR";
  public static final String ROLE_MANAGER = "MANAGER";

  public static final String SHIFT_MORNING = "MORNING";
  public static final String SHIFT_EVENING = "EVENING";
  public static final String SHIFT_NIGHT = "NIGHT";

  private String staffId;
  private String fullName;
  private String role;
  private String shift;
  private String contactNumber;
  private boolean active;

  public Staff() {
  }

  public Staff(String staffId, String fullName, String role, String shift,
      String contactNumber, boolean active) {
    this.staffId = staffId;
    this.fullName = fullName;
    this.role = role;
    this.shift = shift;
    this.contactNumber = contactNumber;
    this.active = active;
  }

  public String getStaffId() {
    return staffId;
  }

  public void setStaffId(String staffId) {
    this.staffId = staffId;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getShift() {
    return shift;
  }

  public void setShift(String shift) {
    this.shift = shift;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public void setContactNumber(String contactNumber) {
    this.contactNumber = contactNumber;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(staffId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.staffId, ((Staff) obj).staffId);
  }

  @Override
  public String toString() {
    return String.format("%-6s %-26s %-12s %-8s %-14s %s",
        staffId, fullName, role, shift, contactNumber, active ? "ACTIVE" : "INACTIVE");
  }
}
