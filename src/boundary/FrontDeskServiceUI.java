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

  /**
   * Returned by inputNonNegativeAmount() when the user cancels. Negative, so
   * it can never be mistaken for a real amount.
   */
  public static final double CANCELLED_AMOUNT = -1;

  private final Scanner scanner = MessageUI.scanner;

  public int getMenuChoice() {
    MessageUI.clearScreen();
    System.out.println();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("F R O N T - D E S K   S E R V I C E");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  Main Menu  >  Front-Desk Service");
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(1, "Create new booking");
    MessageUI.displayMenuOption(2, "Search information");
    MessageUI.displayMenuOption(3, "Check room availability");
    MessageUI.displayMenuOption(4, "Display all bookings");
    MessageUI.displayMenuOption(5, "Reports");
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(0, "Back to main menu");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxBottom();

    return MessageUI.readMenuChoice(
        scanner, 5, "go back to the main menu");
  }

  /**
   * Displays the Search Information submenu.
   *
   * Keeps guest and billing searches under one menu so the
   * user can choose the type of information to search.
   *
   * @return the selected search option
   */
  public int getSearchMenuChoice() {
    MessageUI.clearScreen();
    System.out.println();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("S E A R C H   I N F O R M A T I O N");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine(
        "  Main Menu  >  Front-Desk Service  >  Search Information");
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxBlank();

    MessageUI.displayMenuOption(1, "Search guest information");
    MessageUI.displayMenuOption(2, "Search billing details");
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(0, "Back to Front-Desk Service");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxBottom();

    return MessageUI.readMenuChoice(
        scanner, 2, "go back to Front-Desk Service");
  }

  /**
   * Displays the Reports submenu.
   *
   * Keeps related report functions on the same page so the user can
   * choose which report to generate.
   *
   * @return the selected report option
   */
  public int getReportMenuChoice() {
    MessageUI.clearScreen();
    System.out.println();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("R E P O R T S");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  Main Menu  >  Front-Desk Service  >  Reports");
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxBlank();

    MessageUI.displayMenuOption(1, "Booking summary report");
    MessageUI.displayMenuOption(2, "Outstanding billing report");
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(0, "Back to Front-Desk Service");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxBottom();

    return MessageUI.readMenuChoice(
        scanner, 2, "go back to Front-Desk Service");
  }

  /**
   * Draws the framed title every action in this module starts with, matching
   * the layout used across the rest of the system.
   *
   * @param title the name of the action being started
   */
  private void displayActionHeader(String title) {
    MessageUI.clearScreen();
    System.out.println();
    MessageUI.displayBoxTop();
    MessageUI.displayBoxCentred(title);
    MessageUI.displayBoxBottom();
  }

  public int getNextActionChoice() {
    System.out.println();
    System.out.println("  [1]  Continue with the same task");
    System.out.println("  [0]  Back to Front-Desk Service");

    return MessageUI.readMenuChoice(
        scanner, 1, "go back to Front-Desk Service");
  }

  /**
   * Collects the details needed to create a booking.
   *
   * @return the new Booking, or null if the user cancels at any prompt
   */
  public Booking inputBooking() {
    displayActionHeader("CREATE NEW BOOKING");
    System.out.println("Enter 0 at any prompt to cancel.");
    System.out.println();

    String confirmationNumber = inputConfirmationNumber();
    if (confirmationNumber == null) {
      return null;
    }

    String guestName = inputRequiredText("Guest name (0 to cancel): ");
    if (guestName == null) {
      return null;
    }

    String roomNumber = inputRoomNumber();
    if (roomNumber == null) {
      return null;
    }

    LocalDate checkInDate = inputCheckInDate();
    if (checkInDate == null) {
      return null;
    }

    LocalDate checkOutDate = inputCheckOutDate(checkInDate);
    if (checkOutDate == null) {
      return null;
    }

    return new Booking(
        confirmationNumber,
        guestName,
        roomNumber,
        checkInDate,
        checkOutDate
    );
  }

  /**
   * Reads a confirmation number.
   *
   * @return the 8-digit number, or null if the user enters 0 to cancel
   */
  public String inputConfirmationNumber() {
    while (true) {
      System.out.print("Confirmation number (8 digits, 0 to cancel): ");
      String confirmationNumber = MessageUI.readLine(scanner);

      if (confirmationNumber.equals("0")) {
        return null;
      }

      if (confirmationNumber.matches("\\d{8}")) {
        return confirmationNumber;
      }

      System.out.println(
          "Confirmation number must contain exactly 8 digits.");
    }
  }

  /**
   * @return the room number, or null if the user enters 0 to cancel
   */
  public String inputRoomNumber() {
    String roomNumber = inputRequiredText("Room number (0 to cancel): ");
    return roomNumber == null ? null : roomNumber.toUpperCase();
  }

  /**
   * @return the check-in date, or null if the user enters 0 to cancel
   */
  public LocalDate inputCheckInDate() {
    return inputDate("Check-in date (YYYY-MM-DD, 0 to cancel): ");
  }

  /**
   * @return the check-out date, or null if the user enters 0 to cancel
   */
  public LocalDate inputCheckOutDate(LocalDate checkInDate) {
    while (true) {
      LocalDate checkOutDate =
          inputDate("Check-out date (YYYY-MM-DD, 0 to cancel): ");

      if (checkOutDate == null) {
        return null;
      }

      if (checkOutDate.isAfter(checkInDate)) {
        return checkOutDate;
      }

      System.out.println(
          "Check-out date must be after the check-in date.");
    }
  }

  /**
   * Reads a non-negative amount.
   *
   * 0 is a valid amount here rather than a cancel key, so cancelling uses -1
   * and is reported through the returned value being negative.
   *
   * @return the amount entered, or CANCELLED_AMOUNT if the user cancels
   */
  public double inputNonNegativeAmount(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (input.equals("-1")) {
        return CANCELLED_AMOUNT;
      }

      if (input.isEmpty()) {
        System.out.println("Amount cannot be empty.");
        continue;
      }

      // Accept only non-negative numbers with up to 2 decimal places.
      if (!input.matches("\\d+(\\.\\d{1,2})?")) {
        System.out.println(
                "Please enter a valid amount (e.g. 100 or 100.50).");
        continue;
      }

      try {
        double amount = Double.parseDouble(input);

        if (Double.isFinite(amount) && amount >= 0) {
          return amount;
        }

        System.out.println("Amount cannot be negative.");
      } catch (NumberFormatException e) {
        // This is unlikely after regex validation, but keeps the method safe.
        System.out.println("Please enter a valid amount.");
      }
    }
  }

  public double inputMinimumOutstandingAmount() {
    return inputNonNegativeAmount(
        "Minimum outstanding amount (RM, -1 to cancel): ");
  }

  /**
   * @return the text entered, or null if the user enters 0 to cancel
   */
  private String inputRequiredText(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (input.equals("0")) {
        return null;
      }

      if (!input.isEmpty()) {
        return input;
      }

      System.out.println("This field cannot be empty.");
    }
  }

  /**
   * @return the date entered, or null if the user enters 0 to cancel
   */
  private LocalDate inputDate(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = MessageUI.readLine(scanner);

      if (input.equals("0")) {
        return null;
      }

      try {
        return LocalDate.parse(input);
      } catch (DateTimeParseException e) {
        System.out.println(
            "Please enter a valid date in YYYY-MM-DD format.");
      }
    }
  }

  /**
   * @return the start date, or null if the user enters 0 to cancel
   */
  public LocalDate inputStartDate() {
    return inputDate("Start date (YYYY-MM-DD, 0 to cancel): ");
  }

  /**
   * @return the end date, or null if the user enters 0 to cancel
   */
  public LocalDate inputEndDate(LocalDate startDate) {
    while (true) {
      LocalDate endDate =
          inputDate("End date (YYYY-MM-DD, 0 to cancel): ");

      if (endDate == null) {
        return null;
      }

      if (!endDate.isBefore(startDate)) {
        return endDate;
      }

      System.out.println(
          "End date must be after or equal to the start date.");
    }
  }

  /**
   * Displays guest and booking information.
   *
   * Billing information is intentionally excluded because it is handled
   * by the separate Billing Details search.
   *
   * @param booking the booking to display
   */
  public void displayCompleteGuestInformation(Booking booking) {

    if (booking == null) {
      System.out.println("\nBooking not found.");
      return;
    }

    displayActionHeader("GUEST INFORMATION");
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

    displayActionHeader("BILLING DETAILS");
    System.out.printf("%-22s: %s%n",
        "Confirmation number", booking.getConfirmationNumber());
    System.out.printf("%-22s: %s%n",
        "Guest name", booking.getGuestName());
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

    displayActionHeader("ROOM AVAILABILITY RESULT");
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

    displayActionHeader("ALL BOOKINGS");
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

    displayActionHeader("BOOKING REPORT");

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

    displayActionHeader("OUTSTANDING BILLING REPORT");

    if (bookingList.isEmpty()) {
      System.out.println("No outstanding billing records found.");
      return;
    }

    System.out.printf(
        "%-5s %-12s %-28s %-12s %-12s %-12s%n",
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
          "%-5d %-12s %-28s RM %-9.2f RM %-9.2f RM %.2f%n",
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