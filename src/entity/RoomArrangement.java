package entity;

import adt.ArrayList;
import adt.ListInterface;
import java.util.Objects;

/**
 * One way of housing a party across the rooms that are actually free.
 *
 * A party that cannot be given a single room of the type they asked for can
 * often still be housed by splitting them across smaller ones - six guests fit
 * one Family Suite, or three Standard Twins, or a Deluxe King and a Standard
 * Twin. Each of those is an arrangement, and the front desk offers the guest
 * the choice between them.
 *
 * An arrangement holds the actual rooms, not just the types, because two rooms
 * of the same type are not interchangeable once one of them has been given to
 * somebody else.
 *
 * @author Tan Chee Yan
 */
public class RoomArrangement {

  private final ListInterface<Room> rooms = new ArrayList<>();
  private int totalCapacity;
  private double totalRatePerNight;

  /**
   * Adds a room to this arrangement.
   *
   * @param room the room being included
   * @param type its type, used for the capacity and the rate
   */
  public void add(Room room, RoomType type) {
    rooms.add(room);
    if (type != null) {
      totalCapacity += type.getMaxOccupancy();
      totalRatePerNight += type.getBaseRatePerNight();
    }
  }

  public ListInterface<Room> getRooms() {
    return rooms;
  }

  public int getRoomCount() {
    return rooms.getNumberOfEntries();
  }

  public int getTotalCapacity() {
    return totalCapacity;
  }

  public double getTotalRatePerNight() {
    return totalRatePerNight;
  }

  /**
   * Whether this arrangement holds a party of the given size.
   *
   * @param guests how many people are staying
   * @return true if there are enough beds
   */
  public boolean holds(int guests) {
    return totalCapacity >= guests;
  }

  /**
   * Whether every room here is pulling its weight.
   *
   * An arrangement is wasteful if it still houses the party after any one room
   * is taken out of it - offering four twins to a party of six would be, since
   * three already hold them. Only the lean arrangements are worth showing.
   *
   * @param guests how many people are staying
   * @param typeOf looks up a room's type, for its capacity
   * @return true if removing any single room would leave the party short
   */
  public boolean isMinimalFor(int guests, java.util.function.Function<Room, RoomType> typeOf) {
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      RoomType type = typeOf.apply(rooms.getEntry(i));
      int without = totalCapacity - (type == null ? 0 : type.getMaxOccupancy());
      if (without >= guests) {
        return false;
      }
    }
    return true;
  }

  /**
   * The arrangement written for the officer to read, e.g.
   * "2 x Standard Twin + 1 x Deluxe King".
   *
   * @param typeOf looks up a room's type, for its name
   * @return the arrangement as a phrase
   */
  public String describe(java.util.function.Function<Room, RoomType> typeOf) {
    ListInterface<String> names = new ArrayList<>();
    ListInterface<Integer> counts = new ArrayList<>();

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      RoomType type = typeOf.apply(rooms.getEntry(i));
      String name = (type == null) ? "Room" : type.getTypeName();

      int at = names.getPosition(name);
      if (at < 0) {
        names.add(name);
        counts.add(1);
      } else {
        counts.replace(at, counts.getEntry(at) + 1);
      }
    }

    StringBuilder description = new StringBuilder();
    for (int i = 1; i <= names.getNumberOfEntries(); i++) {
      if (description.length() > 0) {
        description.append(" + ");
      }
      description.append(counts.getEntry(i)).append(" x ").append(names.getEntry(i));
    }
    return description.toString();
  }

  /**
   * A key naming which types this arrangement uses, and how many of each.
   *
   * Two arrangements holding the same types in a different order are the same
   * offer to a guest, so the key is built from the type IDs sorted - unlike
   * describe(), which reads them in the order the rooms happen to sit in.
   *
   * @param typeOf looks up a room's type
   * @return a key equal for any two arrangements of the same shape
   */
  public String shapeKey(java.util.function.Function<Room, RoomType> typeOf) {
    ListInterface<String> ids = new ArrayList<>();
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      RoomType type = typeOf.apply(rooms.getEntry(i));
      ids.add(type == null ? "?" : type.getTypeId());
    }
    ids.sort(java.util.Comparator.naturalOrder());

    StringBuilder key = new StringBuilder();
    for (int i = 1; i <= ids.getNumberOfEntries(); i++) {
      key.append(ids.getEntry(i)).append('|');
    }
    return key.toString();
  }

  /** The room numbers, e.g. "1001, 1002". */
  public String roomNumbers() {
    StringBuilder numbers = new StringBuilder();
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      if (numbers.length() > 0) {
        numbers.append(", ");
      }
      numbers.append(rooms.getEntry(i).getRoomNo());
    }
    return numbers.toString();
  }

  /**
   * Two arrangements are the same when they hold the same rooms.
   *
   * The rooms are what an arrangement is: the capacity and the rate are both
   * worked out from them, so comparing the room numbers compares everything
   * that matters.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return roomNumbers().equals(((RoomArrangement) obj).roomNumbers());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(roomNumbers());
  }

  /** The arrangement as one line, e.g. "1001, 2003  sleeps 6  RM410.00". */
  @Override
  public String toString() {
    return String.format("%-20s sleeps %d  RM%.2f",
        roomNumbers(), totalCapacity, totalRatePerNight);
  }
}
