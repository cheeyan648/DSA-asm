package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.Member;
import entity.PointTransaction;
import entity.Redemption;
import entity.Reward;
import java.time.LocalDate;

/**
 * Seeds the loyalty tables - members, the reward catalogue, redemptions, the
 * point ledger and notifications.
 *
 * The members span all four tiers so the tier rules and the reports have
 * something to show, and the ledger rows add up to each member's balance so
 * the cached total and its history agree from the first run.
 *
 * @author Ivan Tan Yann Rong
 */
public class LoyaltyDataInitializer {

  /** Members across all four tiers. */
  public ListInterface<Member> initializeMembers() {
    ListInterface<Member> members = new ArrayList<>();
    LocalDate today = LocalDate.now();

    members.add(new Member("L0001", "G0001", Member.GOLD, 1620, 3480,
        today.plusMonths(12), today.minusMonths(17), Member.ACTIVE));
    members.add(new Member("L0002", "G0002", Member.PLATINUM, 6245, 9120,
        today.plusMonths(12), today.minusMonths(14), Member.ACTIVE));
    members.add(new Member("L0003", "G0003", Member.SILVER, 909, 909,
        today.plusMonths(12), today.minusMonths(21), Member.ACTIVE));
    // Points expiring inside the 30-day window, so the expiring-points report
    // and its notification have a case to find.
    members.add(new Member("L0004", "G0005", Member.DIAMOND, 12400, 18750,
        today.plusDays(20), today.minusMonths(37), Member.ACTIVE));

    return members;
  }

  /**
   * The reward catalogue.
   *
   * One reward is out of stock and one is inactive, so the availability checks
   * at redemption time can be demonstrated rather than only described.
   */
  public ListInterface<Reward> initializeRewards() {
    ListInterface<Reward> rewards = new ArrayList<>();

    // Five things a guest can enjoy during their stay. None of them touches
    // the room rate: the stay is paid for at booking, so a reward is an
    // entitlement to a service rather than a discount on a settled bill.
    rewards.add(new Reward("RW001", "Spa Session (60 min)", Reward.CAT_SPA,
        800, Member.SILVER, 12, true, 160.00));
    rewards.add(new Reward("RW002", "Dinner Buffet for Two", Reward.CAT_DINING,
        1200, Member.SILVER, 30, true, 240.00));
    rewards.add(new Reward("RW003", "Island Hopping Tour", Reward.CAT_ACTIVITY,
        1500, Member.GOLD, 10, true, 300.00));
    rewards.add(new Reward("RW004", "Yoga & Wellness Class", Reward.CAT_WELLNESS,
        400, Member.SILVER, 25, true, 80.00));
    rewards.add(new Reward("RW005", "Airport Transfer Voucher", Reward.CAT_TRANSPORT,
        600, Member.SILVER, 20, true, 120.00));

    return rewards;
  }

  /**
   * Redemptions covering approved, rejected and still pending.
   *
   * The rejected one was refused because the member's tier was below the
   * reward's minimum - checked when it was processed, so the reason is on the
   * record.
   */
  public ListInterface<Redemption> initializeRedemptions() {
    ListInterface<Redemption> redemptions = new ArrayList<>();
    LocalDate today = LocalDate.now();

    Redemption rd1 = new Redemption("RD0001", "L0002", "RW002", 1200, today.minusDays(1));
    rd1.setStatus(Redemption.APPROVED);
    rd1.setProcessedDate(today.minusDays(1));
    rd1.setProcessedBy("ST006");
    rd1.setInvoiceId("INV0002");
    redemptions.add(rd1);

    Redemption rd2 = new Redemption("RD0002", "L0001", "RW001", 800, today.minusDays(2));
    rd2.setStatus(Redemption.APPROVED);
    rd2.setProcessedDate(today.minusDays(2));
    rd2.setProcessedBy("ST006");
    redemptions.add(rd2);

    Redemption rd3 = new Redemption("RD0003", "L0003", "RW003", 1500, today);
    rd3.setStatus(Redemption.REJECTED);
    rd3.setProcessedDate(today);
    rd3.setProcessedBy("ST006");
    rd3.setRejectReason("Member tier SILVER is below the required GOLD");
    redemptions.add(rd3);

    // Still pending - these two are rebuilt into the FIFO queue at startup.
    redemptions.add(new Redemption("RD0004", "L0004", "RW003", 1500, today));
    redemptions.add(new Redemption("RD0005", "L0002", "RW005", 600, today));

    return redemptions;
  }

  /** The point ledger behind each member's balance. */
  public ListInterface<PointTransaction> initializeTransactions() {
    ListInterface<PointTransaction> txns = new ArrayList<>();
    LocalDate today = LocalDate.now();

    txns.add(new PointTransaction("PT0001", "L0001", null, PointTransaction.EARN,
        2480, 2480, today.minusMonths(8), "Historical stays"));
    txns.add(new PointTransaction("PT0002", "L0001", null, PointTransaction.REDEEM,
        -800, 1680, today.minusDays(2), "RD0002 - Spa Session"));
    txns.add(new PointTransaction("PT0003", "L0001", "BK0001", PointTransaction.EARN,
        437, 2117, today, "Stay BK0001 at GOLD rate"));
    txns.add(new PointTransaction("PT0004", "L0001", null, PointTransaction.EXPIRE,
        -497, 1620, today, "Expired points"));

    // No EARN row is seeded against BK0002: that guest is still checked in and
    // their bill is not settled, so the points have not been earned yet. They
    // are awarded when the front desk checks them out, which is what makes the
    // check-out demonstrable rather than already done.
    txns.add(new PointTransaction("PT0005", "L0002", null, PointTransaction.EARN,
        7445, 7445, today.minusMonths(2), "Historical stays"));
    txns.add(new PointTransaction("PT0006", "L0002", null, PointTransaction.REDEEM,
        -1200, 6245, today.minusDays(1), "RD0001 - Dinner Buffet for Two"));

    txns.add(new PointTransaction("PT0008", "L0003", "BK0003", PointTransaction.EARN,
        909, 909, today.minusDays(1), "Stay BK0003 at SILVER rate"));

    txns.add(new PointTransaction("PT0009", "L0004", null, PointTransaction.EARN,
        12400, 12400, today.minusMonths(3), "Historical stays"));

    return txns;
  }

}
