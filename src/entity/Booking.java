package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A reservation of a room type over a date range - the spine of the system.
 *
 * A booking is recorded before any room is found for it. That is why PENDING
 * exists with a null roomNo: the details are captured at the counter first,
 * and only then does the front desk look for a room. A PENDING booking that is
 * URGENT is exactly what lets housekeeping know a room must be cleaned out of
 * turn.
 *
 * priority is copied from the walk-in registration that produced the booking
 * and never re-decided here, so an urgency granted once at the door travels
 * unchanged through the desk and into housekeeping.
 *
 * @author Lim Yong Le
 */
public class Booking implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_CONFIRMED = "CONFIRMED";
  public static final String STATUS_CHECKED_IN = "CHECKED_IN";
  public static final String STATUS_CHECKED_OUT = "CHECKED_OUT";
  public static final String STATUS_CANCELLED = "CANCELLED";
  public static final String STATUS_NO_SHOW = "NO_SHOW";

  public static final String PRIORITY_URGENT = "URGENT";
  public static final String PRIORITY_NORMAL = "NORMAL";

  public static final String SOURCE_WALK_IN = "WALK_IN";
  public static final String SOURCE_ONLINE = "ONLINE";
  public static final String SOURCE_PHONE = "PHONE";
  public static final String SOURCE_CORPORATE = "CORPORATE";

  private String bookingId;
  /**
   * The eight-digit number given to the guest.  It is deliberately separate
   * from the internal BK identifier so front-desk staff can retrieve a stay
   * from the number a caller has in hand without exposing implementation IDs.
   */
  private String confirmationNumber;
  private String guestId;
  private String typeId;
  private String roomNo;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;
  private int numberOfNights;
  private int numberOfGuests;
  private String bookingStatus;
  private String priority;
  private String source;
  private String regId;
  private double ratePerNight;
  private LocalDateTime createdAt;
  private String createdBy;

  public Booking() {
  }

  public Booking(String bookingId, String guestId, String typeId,
      LocalDate checkInDate, LocalDate checkOutDate, int numberOfGuests,
      String priority, String source, String regId, double ratePerNight,
      LocalDateTime createdAt, String createdBy) {
    this.bookingId = bookingId;
    this.guestId = guestId;
    this.typeId = typeId;
    this.checkInDate = checkInDate;
    this.checkOutDate = checkOutDate;
    this.numberOfNights = calculateNights(checkInDate, checkOutDate);
    this.numberOfGuests = numberOfGuests;
    this.bookingStatus = STATUS_PENDING;
    this.priority = priority;
    this.source = source;
    this.regId = regId;
    this.ratePerNight = ratePerNight;
    this.createdAt = createdAt;
    this.createdBy = createdBy;
  }

  /**
   * The number of nights between two dates.
   *
   * A stay is counted in nights, not days: arriving on the 1st and leaving on
   * the 4th is three nights, so the guest is charged for the nights they
   * actually sleep there.
   */
  public static int calculateNights(LocalDate in, LocalDate out) {
    if (in == null || out == null) {
      return 0;
    }
    long nights = java.time.temporal.ChronoUnit.DAYS.between(in, out);
    return (nights < 0) ? 0 : (int) nights;
  }

  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public String getConfirmationNumber() {
    return confirmationNumber;
  }

  public void setConfirmationNumber(String confirmationNumber) {
    this.confirmationNumber = confirmationNumber;
  }

  public String getGuestId() {
    return guestId;
  }

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public String getTypeId() {
    return typeId;
  }

  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  public String getRoomNo() {
    return roomNo;
  }

  public void setRoomNo(String roomNo) {
    this.roomNo = roomNo;
  }

  public LocalDate getCheckInDate() {
    return checkInDate;
  }

  public void setCheckInDate(LocalDate checkInDate) {
    this.checkInDate = checkInDate;
    this.numberOfNights = calculateNights(checkInDate, checkOutDate);
  }

  public LocalDate getCheckOutDate() {
    return checkOutDate;
  }

  public void setCheckOutDate(LocalDate checkOutDate) {
    this.checkOutDate = checkOutDate;
    this.numberOfNights = calculateNights(checkInDate, checkOutDate);
  }

  public int getNumberOfNights() {
    return numberOfNights;
  }

  public void setNumberOfNights(int numberOfNights) {
    this.numberOfNights = numberOfNights;
  }

  public int getNumberOfGuests() {
    return numberOfGuests;
  }

  public void setNumberOfGuests(int numberOfGuests) {
    this.numberOfGuests = numberOfGuests;
  }

  public String getBookingStatus() {
    return bookingStatus;
  }

  public void setBookingStatus(String bookingStatus) {
    this.bookingStatus = bookingStatus;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getRegId() {
    return regId;
  }

  public void setRegId(String regId) {
    this.regId = regId;
  }

  public double getRatePerNight() {
    return ratePerNight;
  }

  public void setRatePerNight(double ratePerNight) {
    this.ratePerNight = ratePerNight;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public boolean isUrgent() {
    return PRIORITY_URGENT.equals(priority);
  }

  /**
   * Whether this booking still occupies its room in the availability sense.
   *
   * Only confirmed and checked-in bookings block a room. A cancelled or
   * no-show booking must not, or its room would stay unsellable forever.
   */
  public boolean holdsRoom() {
    return STATUS_CONFIRMED.equals(bookingStatus) || STATUS_CHECKED_IN.equals(bookingStatus);
  }

  /**
   * Whether this booking's dates clash with a requested range.
   *
   * Two stays overlap only when each starts before the other ends. Same-day
   * turnover is deliberately not an overlap: a guest leaving on the 4th and
   * another arriving on the 4th can share the room, which is how a hotel
   * actually works.
   *
   * @param requestedIn the arrival date being tested
   * @param requestedOut the departure date being tested
   * @return true if the two ranges cannot both be honoured
   */
  public boolean overlaps(LocalDate requestedIn, LocalDate requestedOut) {
    if (checkInDate == null || checkOutDate == null
        || requestedIn == null || requestedOut == null) {
      return false;
    }
    return requestedIn.isBefore(checkOutDate) && checkInDate.isBefore(requestedOut);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(bookingId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.bookingId, ((Booking) obj).bookingId);
  }

  @Override
  public String toString() {
    return String.format("%-7s %-6s %-6s %-6s %-11s %-11s %4d %-12s %-7s %s",
        bookingId, guestId, typeId, (roomNo == null ? "-" : roomNo),
        checkInDate, checkOutDate, numberOfNights, bookingStatus, priority, source);
  }
}
