package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 *
 * @author Kat Tan
 */
public class Member implements Serializable {
  private String memberId;
  private String name;
  private int points;
  private LocalDate pointsExpiryDate;

  public Member() {
  }

  public Member(String memberId, String name, int points, LocalDate pointsExpiryDate) {
    this.memberId = memberId;
    this.name = name;
    this.points = points;
    this.pointsExpiryDate = pointsExpiryDate;
  }

  public String getMemberId() {
    return memberId;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getPoints() {
    return points;
  }

  public void setPoints(int points) {
    this.points = points;
  }

  public LocalDate getPointsExpiryDate() {
    return pointsExpiryDate;
  }

  public void setPointsExpiryDate(LocalDate pointsExpiryDate) {
    this.pointsExpiryDate = pointsExpiryDate;
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
    final Member other = (Member) obj;
    return Objects.equals(this.memberId, other.memberId);
  }

  @Override
  public String toString() {
    return String.format("%-10s %-25s %8d %-12s", memberId, name, points, pointsExpiryDate);
  }
}
