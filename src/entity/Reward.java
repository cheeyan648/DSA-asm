package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Kat Tan
 */
public class Reward implements Serializable {

  private String rewardId;
  private String rewardName;
  private int pointsRequired;

  public Reward() {
  }

  public Reward(String rewardId, String rewardName, int pointsRequired) {
    this.rewardId = rewardId;
    this.rewardName = rewardName;
    this.pointsRequired = pointsRequired;
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

  public int getPointsRequired() {
    return pointsRequired;
  }

  public void setPointsRequired(int pointsRequired) {
    this.pointsRequired = pointsRequired;
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
    final Reward other = (Reward) obj;
    return Objects.equals(this.rewardId, other.rewardId);
  }

  @Override
  public String toString() {
    return String.format("%-8s %-30s %8d", rewardId, rewardName, pointsRequired);
  }
}
