package boundary;

import adt.ListInterface;
import entity.BillingRecord;
import entity.Booking;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;
import utility.MessageUI;

/**
 * Handles all input and output for the Front-Desk Service module.
 *
 * @author Kat Tan
 */
public class FrontDeskServiceUI {

  private static final String BOOKING_FORMAT =
      "%-12s %-25s %-10s %-12s %-12s%n";

  private final Scanner scanner = MessageUI.scanner;

  public int getMenuChoice() {
    MessageUI.clearScreen();

    System.out.println("\nFRONT-DESK SERVICE");
    System.out.println("==================");
    System.out.printf("%-3s%s%n", "1.", "Create new booking");
    System.out.printf("%-3s%s%n", "2.",
        "Search complete guest information by confirmation number");
    System.out.printf("%-3s%s%n", "3.", "Check room availability");
    System.out.printf("%-3s%s%n", "4.",
        "Search billing details by confirmation number");
    System.out.printf("%-3s%s%n", "5.", "Display all bookings");
    System.out.printf("%-3s%s%n", "6.", "Booking summary report");
    System.out.printf("%-3s%s%n", "7.", "Outstanding billing report");
    System.out.printf("%-3s%s%n", "0.", "Back to main menu");

    return MessageUI.readMenuChoice(
        scanner, 7, "go back to the main menu");
  }

  public int getNextActionChoice() {
    System.out.println();
    System.out.printf("%-3s%s%n", "1.", "Continue with the same task");
    System.out.printf("%-3s%s%n", "0.", "Back to Front-Desk Service");

    return MessageUI.readMenuChoice(
        scanner, 1, "go back to Front-Desk Service");
  }

  public Booking inputBooking() {
    System.out.println("\nCREATE NEW BOOKING");
    System.out.println("==================");

    String confirmationNumber = inputConfirmationNumber();
    String guestName = inputRequiredText("Guest name: ");
    String roomNumber = inputRoomNumber();
    LocalDate checkInDate = inputCheckInDate();
    LocalDate checkOutDate = inputCheckOutDate(checkInDate);

    return new Booking(
        confirmationNumber,
        guestName,
        roomNumber,
        checkInDate,
        checkOutDate
    );
  }

  public String inputConfirmationNumber() {
    while (true) {
      System.out.print("Confirmation number (8 digits): ");
      String confirmationNumber = scanner.nextLine().trim();

      if (confirmationNumber.matches("\\d{8}")) {
        return confirmationNumber;
      }

      System.out.println(
          "Confirmation number must contain exactly 8 digits.");
    }
  }

  public String inputRoomNumber() {
    return inputRequiredText("Room number: ").toUpperCase();
  }

  public LocalDate inputCheckInDate() {
    return inputDate("Check-in date (YYYY-MM-DD): ");
  }

  public LocalDate inputCheckOutDate(LocalDate checkInDate) {
    while (true) {
      LocalDate checkOutDate =
          inputDate("Check-out date (YYYY-MM-DD): ");

      if (checkOutDate.isAfter(checkInDate)) {
        return checkOutDate;
      }

      System.out.println(
          "Check-out date must be after the check-in date.");
    }
  }

  public double inputNonNegativeAmount(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();

      if (input.isEmpty()) {
        System.out.println("Amount cannot be empty.");
        continue;
      }

      try {
        double amount = Double.parseDouble(input);

        if (amount >= 0) {
          return amount;
        }

        System.out.println("Amount cannot be negative.");
      } catch (NumberFormatException e) {
        System.out.println("Please enter a valid amount.");
      }
    }
  }

  public double inputMinimumOutstandingAmount() {
    return inputNonNegativeAmount("Minimum outstanding amount (RM): ");
  }

  private String inputRequiredText(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();

      if (!input.isEmpty()) {
        return input;
      }

      System.out.println("This field cannot be empty.");
    }
  }

  private LocalDate inputDate(String prompt) {
    while (true) {
      System.out.print(prompt);

      try {
        return LocalDate.parse(scanner.nextLine().trim());
      } catch (DateTimeParseException e) {
        System.out.println(
            "Please enter a valid date in YYYY-MM-DD format.");
      }
    }
  }

  public LocalDate inputStartDate() {
    return inputDate("Start date (YYYY-MM-DD): ");
  }

  public LocalDate inputEndDate(LocalDate startDate) {
    while (true) {
      LocalDate endDate =
          inputDate("End date (YYYY-MM-DD): ");

      if (!endDate.isBefore(startDate)) {
        return endDate;
      }

      System.out.println(
          "End date must be after or equal to the start date.");
    }
  }

  public void displayCompleteGuestInformation(
      Booking booking,
      double totalBill,
      double amountPaid) {

    if (booking == null) {
      System.out.println("\nBooking not found.");
      return;
    }

    double outstandingBalance = totalBill - amountPaid;
    String paymentStatus = outstandingBalance <= 0
        ? "PAID" : "OUTSTANDING";

    System.out.println("\nCOMPLETE GUEST INFORMATION");
    System.out.println("==========================");
    System.out.printf("%-22s: %s%n",
        "Confirmation number", booking.getConfirmationNumber());
    System.out.printf("%-22s: %s%n",
        "Guest name", booking.getGuestName());
    System.out.printf("%-22s: %s%n",
        "Room number", booking.getRoomNumber());
    System.out.printf("%-22s: %s%n",
        "Check-in date", booking.getCheckInDate());
    System.out.printf("%-22s: %s%n",
        "Check-out date", booking.getCheckOutDate());
    System.out.printf("%-22s: RM %.2f%n",
        "Total bill", totalBill);
    System.out.printf("%-22s: RM %.2f%n",
        "Amount paid", amountPaid);
    System.out.printf("%-22s: RM %.2f%n",
        "Outstanding balance", outstandingBalance);
    System.out.printf("%-22s: %s%n",
        "Payment status", paymentStatus);
  }

  public void displayBillingDetails(
      Booking booking,
      double totalBill,
      double amountPaid) {

    if (booking == null) {
      System.out.println("\nBooking not found.");
      return;
    }

    double outstandingBalance = totalBill - amountPaid;
    String paymentStatus = outstandingBalance <= 0
        ? "PAID" : "OUTSTANDING";

    System.out.println("\nBILLING DETAILS");
    System.out.println("===============");
    System.out.printf("%-22s: %s%n",
        "Confirmation number", booking.getConfirmationNumber());
    System.out.printf("%-22s: %s%n",
        "Guest name", booking.getGuestName());
    System.out.printf("%-22s: %s%n",
        "Room number", booking.getRoomNumber());
    System.out.printf("%-22s: RM %.2f%n",
        "Total bill", totalBill);
    System.out.printf("%-22s: RM %.2f%n",
        "Amount paid", amountPaid);
    System.out.printf("%-22s: RM %.2f%n",
        "Outstanding balance", outstandingBalance);
    System.out.printf("%-22s: %s%n",
        "Payment status", paymentStatus);
  }

  public void displayRoomAvailability(
      String roomNumber,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      boolean available) {

    System.out.println("\nROOM AVAILABILITY RESULT");
    System.out.println("========================");
    System.out.printf("%-18s: %s%n", "Room number", roomNumber);
    System.out.printf("%-18s: %s%n", "Check-in date", checkInDate);
    System.out.printf("%-18s: %s%n", "Check-out date", checkOutDate);
    System.out.printf("%-18s: %s%n",
        "Status", available ? "AVAILABLE" : "NOT AVAILABLE");
  }

  public void displayAllBookings(ListInterface<Booking> list) {
    if (list.isEmpty()) {
      System.out.println("\nNo bookings found.");
      return;
    }

    System.out.println("\nALL BOOKINGS");
    System.out.println("============");
    System.out.printf("%-5s", "No.");
    displayBookingHeader();

    Iterator<Booking> iterator = list.getIterator();
    int number = 1;

    while (iterator.hasNext()) {
      System.out.printf("%-5d", number++);
      displayBookingRow(iterator.next());
    }
  }

  private void displayBookingHeader() {
    System.out.printf(
        BOOKING_FORMAT,
        "Confirm No.",
        "Guest Name",
        "Room",
        "Check-in",
        "Check-out"
    );
  }

  private void displayBookingRow(Booking booking) {
    System.out.printf(
        BOOKING_FORMAT,
        booking.getConfirmationNumber(),
        booking.getGuestName(),
        booking.getRoomNumber(),
        booking.getCheckInDate(),
        booking.getCheckOutDate()
    );
  }

  public void displayBookingReport(
      ListInterface<Booking> reportList) {

    System.out.println("\nBOOKING REPORT");
    System.out.println("==============");

    if (reportList.isEmpty()) {
      System.out.println("No booking records found.");
      return;
    }

    System.out.printf(
        "%-5s %-12s %-25s %-10s %-12s %-12s%n",
        "No.",
        "Confirm No.",
        "Guest Name",
        "Room",
        "Check-in",
        "Check-out"
    );

    for (int i = 1; i <= reportList.getNumberOfEntries(); i++) {
      Booking booking = reportList.getEntry(i);

      System.out.printf(
          "%-5d %-12s %-25s %-10s %-12s %-12s%n",
          i,
          booking.getConfirmationNumber(),
          booking.getGuestName(),
          booking.getRoomNumber(),
          booking.getCheckInDate(),
          booking.getCheckOutDate()
      );
    }

    System.out.println(
        "\nTotal Bookings: " + reportList.getNumberOfEntries());
  }

  public void displayOutstandingBillingReport(
      ListInterface<Booking> bookingList,
      ListInterface<BillingRecord> billingList) {

    System.out.println("\nOUTSTANDING BILLING REPORT");
    System.out.println("==========================");

    if (bookingList.isEmpty()) {
      System.out.println("No outstanding billing records found.");
      return;
    }

    System.out.printf(
        "%-5s %-12s %-20s %-12s %-12s %-12s%n",
        "No.",
        "Confirm No.",
        "Guest Name",
        "Total Bill",
        "Paid",
        "Balance"
    );

    for (int i = 1; i <= bookingList.getNumberOfEntries(); i++) {
      Booking booking = bookingList.getEntry(i);
      BillingRecord billing = billingList.getEntry(i);

      System.out.printf(
          "%-5d %-12s %-20s RM %-9.2f RM %-9.2f RM %.2f%n",
          i,
          booking.getConfirmationNumber(),
          booking.getGuestName(),
          billing.getTotalBill(),
          billing.getAmountPaid(),
          billing.getOutstandingBalance()
      );
    }
  }

  public void displayMessage(String message) {
    System.out.println("\n" + message);
  }
}