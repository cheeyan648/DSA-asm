package control;

import utility.MessageUI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 *
 * @author Kat Tan
 */
public class TARUMTResortUI {

  // Shared with every other UI class - see MessageUI.scanner for why.
  private Scanner scanner = MessageUI.scanner;

  private WalkInRegistrationBookingMaintenance walkInRegistrationBookingMaintenance = new WalkInRegistrationBookingMaintenance();
  private HousekeepingTaskLogMaintenance housekeepingTaskLogMaintenance = new HousekeepingTaskLogMaintenance();
  private FrontDeskServiceMaintenance frontDeskServiceMaintenance = new FrontDeskServiceMaintenance();
  private LoyaltyRewardsMaintenance loyaltyRewardsMaintenance = new LoyaltyRewardsMaintenance();

  private static final DateTimeFormatter HOME_TIMESTAMP =
      DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");

  /**
   * Draws the home page: the system banner, the four subsystems on offer, and
   * the current date, all inside one frame so the main menu reads as a single
   * screen rather than a loose list of print statements.
   */
  public int getMenuChoice() {
    MessageUI.clearScreen();
    System.out.println();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("TUNKU ABDUL RAHMAN UNIVERSITY");
    MessageUI.displayBoxCentred("OF MANAGEMENT AND TECHNOLOGY");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("T A R U M T   R E S O R T");
    MessageUI.displayBoxCentred("Resort Management System");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxLine("MAIN MENU - please select a subsystem");
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(1, "Walk-In Registration & Standard Booking");
    MessageUI.displayMenuOption(2, "Housekeeping Task Log");
    MessageUI.displayMenuOption(3, "Front-Desk Service");
    MessageUI.displayMenuOption(4, "Loyalty & Rewards");
    MessageUI.displayBoxBlank();
    MessageUI.displayMenuOption(0, "Quit the system");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("Today: " + LocalDate.now().format(HOME_TIMESTAMP));
    MessageUI.displayBoxBottom();

    // Keeps re-prompting (without clearing) until a valid 0-4 is entered, so
    // the user can see the error message and correct their input.
    return MessageUI.readMenuChoice(scanner, 4, "exit");
  }

  /**
   * Draws the closing screen, framed to match the home page so the system opens
   * and closes the same way.
   */
  private void displayExitScreen() {
    MessageUI.clearScreen();
    System.out.println();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("THANK YOU FOR USING");
    MessageUI.displayBoxCentred("TARUMT RESORT MANAGEMENT SYSTEM");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("Have a pleasant day.");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxBottom();
    System.out.println();
  }

  public void runTARUMTResort() {
    int choice = 0;
    do {
      choice = getMenuChoice();
      switch (choice) {
        case 0:
          displayExitScreen();
          break;
        case 1:
          walkInRegistrationBookingMaintenance.runWalkInRegistrationBooking();
          break;
        case 2:
          housekeepingTaskLogMaintenance.runHousekeepingTaskLog();
          break;
        case 3:
          frontDeskServiceMaintenance.runFrontDeskService();
          break;
        case 4:
          loyaltyRewardsMaintenance.runLoyaltyRewards();
          break;
      }
    } while (choice != 0);
  }

  public static void main(String[] args) {
    TARUMTResortUI tarumtResortUI = new TARUMTResortUI();
    tarumtResortUI.runTARUMTResort();
  }
}
