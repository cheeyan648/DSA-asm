package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One person the resort deals with, held once and referred to everywhere.
 *
 * Before this existed the same human could appear as a name on a walk-in
 * record, a different name string on a booking and a third on a membership,
 * with nothing tying them together. Every module now points at this record
 * instead of keeping its own copy of who someone is.
 *
 * Membership is deliberately not stored here - Member points at Guest, not the
 * other way round, so there is one direction of reference and no way for the
 * two to disagree.
 *
 * @author Wong Chee Yan
 */
public class Guest implements Serializable {

  private static final long serialVersionUID = 2L;

  private String guestId;
  private String fullName;
  private String icPassportNo;
  private String contactNumber;
  private String email;
  private LocalDateTime registeredAt;

  public Guest() {
  }

  public Guest(String guestId, String fullName, String icPassportNo,
      String contactNumber, String email, LocalDateTime registeredAt) {
    this.guestId = guestId;
    this.fullName = fullName;
    this.icPassportNo = icPassportNo;
    this.contactNumber = contactNumber;
    this.email = email;
    this.registeredAt = registeredAt;
  }

  public String getGuestId() {
    return guestId;
  }

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getIcPassportNo() {
    return icPassportNo;
  }

  public void setIcPassportNo(String icPassportNo) {
    this.icPassportNo = icPassportNo;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public void setContactNumber(String contactNumber) {
    this.contactNumber = contactNumber;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public LocalDateTime getRegisteredAt() {
    return registeredAt;
  }

  public void setRegisteredAt(LocalDateTime registeredAt) {
    this.registeredAt = registeredAt;
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
    return Objects.equals(this.guestId, ((Guest) obj).guestId);
  }

  @Override
  public String toString() {
    return String.format("%-6s %-26s %-16s %-14s %s",
        guestId, fullName, icPassportNo, contactNumber,
        (email == null || email.isBlank()) ? "-" : email);
  }
}
