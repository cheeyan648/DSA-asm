package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * An item in the rewards catalogue that points can be exchanged for.
 *
 * @author Ivan Wong
 */
public class Reward implements Serializable {

  private static final long serialVersionUID = 2L;

  // Rewards are things the guest enjoys while they are here, not money off
  // the stay itself: the room is paid for when it is booked, so a reward
  // entitles them to a service rather than reducing a settled bill.
  public static final String CAT_SPA = "SPA";
  public static final String CAT_DINING = "DINING";
  public static final String CAT_ACTIVITY = "ACTIVITY";
  public static final String CAT_WELLNESS = "WELLNESS";
  public static final String CAT_TRANSPORT = "TRANSPORT";

  private String rewardId;
  private String rewardName;
  private String category;
  private int pointsRequired;
  private String minimumTier;
  private int stockQuantity;
  private boolean active;
  private double cashValue;

  public Reward() {
  }

  public Reward(String rewardId, String rewardName, String category, int pointsRequired,
      String minimumTier, int stockQuantity, boolean active, double cashValue) {
    this.rewardId = rewardId;
    this.rewardName = rewardName;
    this.category = category;
    this.pointsRequired = pointsRequired;
    this.minimumTier = minimumTier;
    this.stockQuantity = stockQuantity;
    this.active = active;
    this.cashValue = cashValue;
  }

  public String getRewardId() {
    return rewardId;
  }

  public void setRewardId(String rewardId) {
    this.rewardId = rewardId;
  }

  public String getRewardName() {
    return rewardName;
  }

  public void setRewardName(String rewardName) {
    this.rewardName = rewardName;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public int getPointsRequired() {
    return pointsRequired;
  }

  public void setPointsRequired(int pointsRequired) {
    this.pointsRequired = pointsRequired;
  }

  public String getMinimumTier() {
    return minimumTier;
  }

  public void setMinimumTier(String minimumTier) {
    this.minimumTier = minimumTier;
  }

  public int getStockQuantity() {
    return stockQuantity;
  }

  public void setStockQuantity(int stockQuantity) {
    this.stockQuantity = Math.max(0, stockQuantity);
  }

  public void reduceStock() {
    if (stockQuantity > 0) {
      stockQuantity--;
    }
  }

  /** Puts one unit back, used when an approved redemption is reversed. */
  public void restoreStock() {
    stockQuantity++;
  }

  /**
   * Adds units to the catalogue stock.
   *
   * @param amount how many to add; ignored when zero or negative
   */
  public void addQuantity(int amount) {
    if (amount > 0) {
      stockQuantity += amount;
    }
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * What this reward is worth in ringgit when taken off a bill.
   *
   * Held explicitly rather than derived from the points, because the exchange
   * rate the resort offers on a redemption is a commercial decision and is not
   * the same for every reward.
   */
  public double getCashValue() {
    return cashValue;
  }

  public void setCashValue(double cashValue) {
    this.cashValue = cashValue;
  }

  /** Whether this reward can currently be requested at all. */
  public boolean isAvailable() {
    return active && stockQuantity > 0;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(rewardId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.rewardId, ((Reward) obj).rewardId);
  }

  @Override
  public String toString() {
    return String.format("%-6s %-30s %-10s %7d %-9s %5d  %s",
        rewardId, rewardName, category, pointsRequired, minimumTier, stockQuantity,
        active ? "ACTIVE" : "INACTIVE");
  }
}
