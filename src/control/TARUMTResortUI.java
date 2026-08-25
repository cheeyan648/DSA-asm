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
    MessageUI.displayBlankLine();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("TUNKU ABDUL RAHMAN UNIVERSITY OF MANAGEMENT AND TECHNOLOGY");
    MessageUI.displayBoxBlank();

    // The resort name is drawn in block letters so it reads as the title of the
    // screen at a glance, the way a heading would in a printed document.
    MessageUI.displayBanner("TARUMT");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("R E S O R T   M A N A G E M E N T   S Y S T E M");
    MessageUI.displayBoxBlank();

    MessageUI.displayBoxDivider();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxLine("  MAIN MENU");
    MessageUI.displayBoxLine("  Select a subsystem to continue.");
    MessageUI.displayBoxBlank();
    MessageUI.displaySubsystemOption(1, "Walk-In Registration & Standard Booking",
        "Register guests, manage the waiting queue");
    MessageUI.displaySubsystemOption(2, "Housekeeping Task Log",
        "Record and track room cleaning tasks");
    MessageUI.displaySubsystemOption(3, "Front-Desk Service",
        "Handle guest requests at the counter");
    MessageUI.displaySubsystemOption(4, "Loyalty & Rewards",
        "Manage members, points and redemptions");
    MessageUI.displayMenuOption(0, "Quit the system");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxLine("  Today: " + LocalDate.now().format(HOME_TIMESTAMP));
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
    MessageUI.displayBlankLine();

    MessageUI.displayBoxTop();
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("THANK YOU FOR USING");
    MessageUI.displayBoxBlank();
    MessageUI.displayBanner("TARUMT");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxCentred("R E S O R T   M A N A G E M E N T   S Y S T E M");
    MessageUI.displayBoxBlank();
    MessageUI.displayBoxDivider();
    MessageUI.displayBoxCentred("Have a pleasant day.");
    MessageUI.displayBoxBottom();
    MessageUI.displayBlankLine();
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