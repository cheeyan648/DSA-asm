package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.FrontDeskServiceUI;
import entity.Booking;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Controls the Front-Desk Service module.
 *
 * The 8-digit confirmation number is used as the key in a HashMap. This gives
 * average O(1) lookup time, allowing front-desk agents to retrieve complete
 * guest information without scanning every booking in the list.
 *
 * @author Kat Tan
 */
public class FrontDeskServiceMaintenance {

  private final FrontDeskServiceUI frontDeskServiceUI =
      new FrontDeskServiceUI();

  /* Keeps bookings in insertion order for display purposes. */
  private final ListInterface<Booking> bookingList =
      new ArrayList<>();

  /*
   * Efficient confirmation-number index.
   * Key   : unique 8-digit confirmation number
   * Value : complete Booking object
   * Average lookup time: O(1)
   */
  private final Map<String, Booking> bookingIndex =
      new HashMap<>();

  /* Billing is stored here because the existing Booking entity has no fields
   * for total bill or amount paid, and no non-front-desk file is modified.
   */
  private final Map<String, BillingRecord> billingIndex =
      new HashMap<>();

  /*
   * Groups bookings by room number. Availability checks only inspect bookings
   * for the selected room instead of scanning every resort booking.
   */
  private final Map<String, ListInterface<Booking>> roomBookingIndex =
      new HashMap<>();

  private static class BillingRecord {

    private final double totalBill;
    private final double amountPaid;

    BillingRecord(double totalBill, double amountPaid) {
      this.totalBill = totalBill;
      this.amountPaid = amountPaid;
    }

    double getTotalBill() {
      return totalBill;
    }

    double getAmountPaid() {
      return amountPaid;
    }
  }

  public void createBooking() {
    Booking booking = frontDeskServiceUI.inputBooking();
    String confirmationNumber = booking.getConfirmationNumber();

    /* Average O(1) duplicate check. */
    if (bookingIndex.containsKey(confirmationNumber)) {
      frontDeskServiceUI.displayMessage(
          "A booking with that confirmation number already exists.");
      return;
    }

    if (!isRoomAvailable(
        booking.getRoomNumber(),
        booking.getCheckInDate(),
        booking.getCheckOutDate())) {

      frontDeskServiceUI.displayMessage(
          "Room " + booking.getRoomNumber()
              + " is not available for the selected dates.");
      return;
    }

    double totalBill = frontDeskServiceUI.inputNonNegativeAmount(
        "Total bill (RM): ");

    double amountPaid;
    do {
      amountPaid = frontDeskServiceUI.inputNonNegativeAmount(
          "Amount paid (RM): ");

      if (amountPaid > totalBill) {
        frontDeskServiceUI.displayMessage(
            "Amount paid cannot exceed the total bill.");
      }
    } while (amountPaid > totalBill);

    bookingList.add(booking);
    bookingIndex.put(confirmationNumber, booking);
    billingIndex.put(
        confirmationNumber,
        new BillingRecord(totalBill, amountPaid)
    );
    addToRoomIndex(booking);

    frontDeskServiceUI.displayMessage(
        "Booking created successfully.");
  }

  private void addToRoomIndex(Booking booking) {
    String roomNumber = normalizeRoomNumber(booking.getRoomNumber());

    ListInterface<Booking> roomBookings =
        roomBookingIndex.get(roomNumber);

    if (roomBookings == null) {
      roomBookings = new ArrayList<>();
      roomBookingIndex.put(roomNumber, roomBookings);
    }

    roomBookings.add(booking);
  }

  /**
   * Retrieves a booking directly through its 8-digit confirmation number.
   *
   * @param confirmationNumber unique booking confirmation number
   * @return matching Booking, or null when no booking exists
   */
  public Booking searchBookingByConfirmationNumber(
      String confirmationNumber) {

    return bookingIndex.get(confirmationNumber);
  }

  public void searchCompleteGuestInformation() {
    System.out.println("\nSEARCH COMPLETE GUEST INFORMATION");
    System.out.println("=================================");

    String confirmationNumber =
        frontDeskServiceUI.inputConfirmationNumber();

    Booking booking =
        searchBookingByConfirmationNumber(confirmationNumber);

    if (booking == null) {
      frontDeskServiceUI.displayCompleteGuestInformation(
          null, 0, 0);
      return;
    }

    BillingRecord billingRecord = billingIndex.get(confirmationNumber);

    frontDeskServiceUI.displayCompleteGuestInformation(
        booking,
        billingRecord.getTotalBill(),
        billingRecord.getAmountPaid()
    );
  }

  public void searchBillingDetails() {
    System.out.println("\nSEARCH BILLING DETAILS");
    System.out.println("======================");

    String confirmationNumber =
        frontDeskServiceUI.inputConfirmationNumber();

    Booking booking =
        searchBookingByConfirmationNumber(confirmationNumber);

    if (booking == null) {
      frontDeskServiceUI.displayBillingDetails(null, 0, 0);
      return;
    }

    BillingRecord billingRecord = billingIndex.get(confirmationNumber);

    frontDeskServiceUI.displayBillingDetails(
        booking,
        billingRecord.getTotalBill(),
        billingRecord.getAmountPaid()
    );
  }

  public void checkRoomAvailability() {
    System.out.println("\nCHECK ROOM AVAILABILITY");
    System.out.println("=======================");

    String roomNumber = frontDeskServiceUI.inputRoomNumber();
    LocalDate checkInDate = frontDeskServiceUI.inputCheckInDate();
    LocalDate checkOutDate =
        frontDeskServiceUI.inputCheckOutDate(checkInDate);

    boolean available = isRoomAvailable(
        roomNumber,
        checkInDate,
        checkOutDate
    );

    frontDeskServiceUI.displayRoomAvailability(
        roomNumber,
        checkInDate,
        checkOutDate,
        available
    );
  }

  /**
   * A room is unavailable when the requested date range overlaps an existing
   * booking for that room. A guest may check in on the same date that the
   * previous guest checks out.
   */
  public boolean isRoomAvailable(
      String roomNumber,
      LocalDate requestedCheckIn,
      LocalDate requestedCheckOut) {

    ListInterface<Booking> roomBookings =
        roomBookingIndex.get(normalizeRoomNumber(roomNumber));

    if (roomBookings == null || roomBookings.isEmpty()) {
      return true;
    }

    for (int position = 1;
        position <= roomBookings.getNumberOfEntries();
        position++) {

      Booking existingBooking = roomBookings.getEntry(position);

      boolean datesOverlap =
          requestedCheckIn.isBefore(existingBooking.getCheckOutDate())
          && requestedCheckOut.isAfter(existingBooking.getCheckInDate());

      if (datesOverlap) {
        return false;
      }
    }

    return true;
  }

  private String normalizeRoomNumber(String roomNumber) {
    return roomNumber.trim().toUpperCase();
  }

  public void displayAllBookings() {
    frontDeskServiceUI.displayAllBookings(bookingList);
  }

  public void runFrontDeskService() {
    int choice;

    do {
      choice = frontDeskServiceUI.getMenuChoice();

      switch (choice) {
        case 1:
          do {
            createBooking();
          } while (frontDeskServiceUI.getNextActionChoice() == 1);
          break;

        case 2:
          do {
            searchCompleteGuestInformation();
          } while (frontDeskServiceUI.getNextActionChoice() == 1);
          break;

        case 3:
          do {
            checkRoomAvailability();
          } while (frontDeskServiceUI.getNextActionChoice() == 1);
          break;

        case 4:
          do {
            searchBillingDetails();
          } while (frontDeskServiceUI.getNextActionChoice() == 1);
          break;

        case 5:
          do {
            displayAllBookings();
          } while (frontDeskServiceUI.getNextActionChoice() == 1);
          break;

        case 0:
          break;

        default:
          break;
      }
    } while (choice != 0);
  }
}