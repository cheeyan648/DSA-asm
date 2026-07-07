package control;

import utility.MessageUI;

import java.util.Scanner;

/**
 *
 * @author Kat Tan
 */
public class TARUMTResortUI {

  Scanner scanner = new Scanner(System.in);

  private WalkInRegistrationBookingMaintenance walkInRegistrationBookingMaintenance = new WalkInRegistrationBookingMaintenance();
  private HousekeepingTaskLogMaintenance housekeepingTaskLogMaintenance = new HousekeepingTaskLogMaintenance();
  private FrontDeskServiceMaintenance frontDeskServiceMaintenance = new FrontDeskServiceMaintenance();
  private LoyaltyRewardsMaintenance loyaltyRewardsMaintenance = new LoyaltyRewardsMaintenance();

  public int getMenuChoice() {
    System.out.println("\nTARUMT RESORT MANAGEMENT SYSTEM");
    System.out.println("1. Walk-In Registration & Standard Booking");
    System.out.println("2. Housekeeping Task Log");
    System.out.println("3. Front-Desk Service");
    System.out.println("4. Loyalty & Rewards");
    System.out.println("0. Quit");
    System.out.print("Enter choice: ");
    int choice = scanner.nextInt();
    scanner.nextLine();
    System.out.println();
    return choice;
  }

  public void runTARUMTResort() {
    int choice = 0;
    do {
      choice = getMenuChoice();
      switch (choice) {
        case 0:
          MessageUI.displayExitMessage();
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
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  public static void main(String[] args) {
    TARUMTResortUI tarumtResortUI = new TARUMTResortUI();
    tarumtResortUI.runTARUMTResort();
  }
}
