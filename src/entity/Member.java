package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A loyalty membership attached to a guest.
 *
 * Holds no name of its own - it points at the Guest record, so a guest who
 * changes their contact details does not end up with two versions of
 * themselves.
 *
 * Tier is worked out from lifetimePoints, never from the spendable balance.
 * Spending points must not cost a member their standing, so the two figures
 * are tracked separately: lifetimePoints only ever rises, while pointsBalance
 * goes up and down as points are earned and redeemed.
 *
 * @author Ivan Wong
 */
public class Member implements Serializable {

  private static final long serialVersionUID = 2L;

  public static final String SILVER = "SILVER";
  public static final String GOLD = "GOLD";
  public static final String PLATINUM = "PLATINUM";
  public static final String DIAMOND = "DIAMOND";

  public static final int GOLD_THRESHOLD = 1000;
  public static final int PLATINUM_THRESHOLD = 5000;
  public static final int DIAMOND_THRESHOLD = 15000;

  public static final String ACTIVE = "ACTIVE";
  public static final String DORMANT = "DORMANT";
  public static final String SUSPENDED = "SUSPENDED";

  /** How long points last before expiring, measured from the last stay. */
  public static final int POINTS_VALIDITY_MONTHS = 12;

  /** Points expiring within this many days count as "expiring soon". */
  public static final int EXPIRING_SOON_DAYS = 30;

  private String memberId;
  private String guestId;
  private String tier;
  private int pointsBalance;
  private int lifetimePoints;
  private LocalDate pointsExpiryDate;
  private LocalDate joinDate;
  private String status;

  public Member() {
  }

  public Member(String memberId, String guestId, LocalDate joinDate) {
    this.memberId = memberId;
    this.guestId = guestId;
    this.joinDate = joinDate;
    this.tier = SILVER;
    this.pointsBalance = 0;
    this.lifetimePoints = 0;
    this.status = ACTIVE;
    this.pointsExpiryDate = joinDate.plusMonths(POINTS_VALIDITY_MONTHS);
  }

  public Member(String memberId, String guestId, String tier, int pointsBalance,
      int lifetimePoints, LocalDate pointsExpiryDate, LocalDate joinDate, String status) {
    this.memberId = memberId;
    this.guestId = guestId;
    this.tier = tier;
    this.pointsBalance = pointsBalance;
    this.lifetimePoints = lifetimePoints;
    this.pointsExpiryDate = pointsExpiryDate;
    this.joinDate = joinDate;
    this.status = status;
  }

  public String getMemberId() {
    return memberId;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
  }

  public String getGuestId() {
    return guestId;
  }

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public String getTier() {
    return tier;
  }

  public void setTier(String tier) {
    this.tier = tier;
  }

  public int getPointsBalance() {
    return pointsBalance;
  }

  public void setPointsBalance(int pointsBalance) {
    this.pointsBalance = Math.max(0, pointsBalance);
  }

  public int getLifetimePoints() {
    return lifetimePoints;
  }

  public void setLifetimePoints(int lifetimePoints) {
    this.lifetimePoints = lifetimePoints;
  }

  public LocalDate getPointsExpiryDate() {
    return pointsExpiryDate;
  }

  public void setPointsExpiryDate(LocalDate pointsExpiryDate) {
    this.pointsExpiryDate = pointsExpiryDate;
  }

  public LocalDate getJoinDate() {
    return joinDate;
  }

  public void setJoinDate(LocalDate joinDate) {
    this.joinDate = joinDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * The tier a given lifetime total earns.
   *
   * @param lifetimePoints the member's lifetime points
   * @return the tier name
   */
  public static String tierFor(int lifetimePoints) {
    if (lifetimePoints >= DIAMOND_THRESHOLD) {
      return DIAMOND;
    }
    if (lifetimePoints >= PLATINUM_THRESHOLD) {
      return PLATINUM;
    }
    if (lifetimePoints >= GOLD_THRESHOLD) {
      return GOLD;
    }
    return SILVER;
  }

  /**
   * How many points each ringgit spent is worth at a given tier.
   *
   * @param tier the tier name
   * @return the multiplier applied to the base earning rate
   */
  public static double multiplierFor(String tier) {
    if (DIAMOND.equals(tier)) {
      return 2.00;
    }
    if (PLATINUM.equals(tier)) {
      return 1.50;
    }
    if (GOLD.equals(tier)) {
      return 1.25;
    }
    return 1.00;
  }

  /**
   * How the tiers rank against each other.
   *
   * Used to test whether a member is senior enough for a reward that has a
   * minimum tier, which a string comparison could not answer.
   *
   * @param tier the tier name
   * @return 0 silver, 1 gold, 2 platinum, 3 diamond
   */
  public static int tierRank(String tier) {
    if (DIAMOND.equals(tier)) {
      return 3;
    }
    if (PLATINUM.equals(tier)) {
      return 2;
    }
    if (GOLD.equals(tier)) {
      return 1;
    }
    return 0;
  }

  /** The multiplier this member currently earns at. */
  public double getMultiplier() {
    return multiplierFor(tier);
  }

  /**
   * Recomputes the tier from lifetime points.
   *
   * @return true if the member moved up a tier
   */
  public boolean refreshTier() {
    String updated = tierFor(lifetimePoints);
    if (!updated.equals(tier)) {
      tier = updated;
      return true;
    }
    return false;
  }

  /** The tier above this member's, or null if they are already at the top. */
  public String getNextTier() {
    switch (tierRank(tier)) {
      case 0:
        return GOLD;
      case 1:
        return PLATINUM;
      case 2:
        return DIAMOND;
      default:
        return null;
    }
  }

  /**
   * How many more lifetime points would reach the next tier.
   *
   * @return the shortfall, or 0 if the member is already at the top tier
   */
  public int getPointsToNextTier() {
    switch (tierRank(tier)) {
      case 0:
        return GOLD_THRESHOLD - lifetimePoints;
      case 1:
        return PLATINUM_THRESHOLD - lifetimePoints;
      case 2:
        return DIAMOND_THRESHOLD - lifetimePoints;
      default:
        return 0;
    }
  }

  /**
   * Whether this member's points are close enough to expiry to warn them.
   *
   * @param today the date to measure from
   * @return true if the points expire within the warning window
   */
  public boolean hasExpiringPoints(LocalDate today) {
    if (pointsExpiryDate == null || pointsBalance <= 0) {
      return false;
    }
    return !pointsExpiryDate.isBefore(today)
        && !pointsExpiryDate.isAfter(today.plusDays(EXPIRING_SOON_DAYS));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(memberId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.memberId, ((Member) obj).memberId);
  }

  @Override
  public String toString() {
    return String.format("%-7s %-6s %-9s %8d %10d  %-11s %s",
        memberId, guestId, tier, pointsBalance, lifetimePoints, pointsExpiryDate, status);
  }
}
