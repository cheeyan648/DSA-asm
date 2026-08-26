package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * A category of room - its rate, its capacity and how long it takes to clean.
 *
 * Holding the rate here rather than on each room means a price change is made
 * in one place, and holding the standard clean time lets housekeeping estimate
 * its workload from the tasks outstanding.
 *
 * @author Tan Chee Yan
 */
public class RoomType implements Serializable {

  private static final long serialVersionUID = 2L;

  private String typeId;
  private String typeName;
  private int maxOccupancy;
  private double baseRatePerNight;
  private int standardCleanMinutes;
  private String description;

  public RoomType() {
  }

  public RoomType(String typeId, String typeName, int maxOccupancy,
      double baseRatePerNight, int standardCleanMinutes, String description) {
    this.typeId = typeId;
    this.typeName = typeName;
    this.maxOccupancy = maxOccupancy;
    this.baseRatePerNight = baseRatePerNight;
    this.standardCleanMinutes = standardCleanMinutes;
    this.description = description;
  }

  public String getTypeId() {
    return typeId;
  }

  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public int getMaxOccupancy() {
    return maxOccupancy;
  }

  public void setMaxOccupancy(int maxOccupancy) {
    this.maxOccupancy = maxOccupancy;
  }

  public double getBaseRatePerNight() {
    return baseRatePerNight;
  }

  public void setBaseRatePerNight(double baseRatePerNight) {
    this.baseRatePerNight = baseRatePerNight;
  }

  public int getStandardCleanMinutes() {
    return standardCleanMinutes;
  }

  public void setStandardCleanMinutes(int standardCleanMinutes) {
    this.standardCleanMinutes = standardCleanMinutes;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(typeId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.typeId, ((RoomType) obj).typeId);
  }

  @Override
  public String toString() {
    return String.format("%-6s %-18s %5d %12.2f %8d  %s",
        typeId, typeName, maxOccupancy, baseRatePerNight, standardCleanMinutes, description);
  }
}
