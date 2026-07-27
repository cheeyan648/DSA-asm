package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 *
 * @author Kat Tan
 */
public class Booking implements Serializable {
  private String confirmationNumber;
  private String guestName;
  private String roomNumber;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;

  public Booking() {
  }

  public Booking(String confirmationNumber, String guestName, String roomNumber,
      LocalDate checkInDate, LocalDate checkOutDate) {
    this.confirmationNumber = confirmationNumber;
    this.guestName = guestName;
    this.roomNumber = roomNumber;
    this.checkInDate = checkInDate;
    this.checkOutDate = checkOutDate;
  }

  public String getConfirmationNumber() {
    return confirmationNumber;
  }

  public void setConfirmationNumber(String confirmationNumber) {
    this.confirmationNumber = confirmationNumber;
  }

  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  public LocalDate getCheckInDate() {
    return checkInDate;
  }

  public void setCheckInDate(LocalDate checkInDate) {
    this.checkInDate = checkInDate;
  }

  public LocalDate getCheckOutDate() {
    return checkOutDate;
  }

  public void setCheckOutDate(LocalDate checkOutDate) {
    this.checkOutDate = checkOutDate;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(confirmationNumber);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Booking other = (Booking) obj;
    return Objects.equals(this.confirmationNumber, other.confirmationNumber);
  }

  @Override
  public String toString() {
    return String.format("%-10s %-20s %-8s %-12s %-12s",
        confirmationNumber, guestName, roomNumber, checkInDate, checkOutDate);
  }
}
