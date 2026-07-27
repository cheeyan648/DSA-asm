package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Kat Tan
 */
public class WalkInGuest implements Serializable {
  private String guestId;
  private String name;
  private String contactNumber;
  private boolean priority;

  public WalkInGuest() {
  }

  public WalkInGuest(String guestId, String name, String contactNumber, boolean priority) {
    this.guestId = guestId;
    this.name = name;
    this.contactNumber = contactNumber;
    this.priority = priority;
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

  public boolean isPriority() {
    return priority;
  }

  public void setPriority(boolean priority) {
    this.priority = priority;
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

  @Override
  public String toString() {
    return String.format("%-10s %-30s %-15s %-8s", guestId, name, contactNumber, priority ? "PRIORITY" : "NORMAL");
  }
}
