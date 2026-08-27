package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Room;
import entity.RoomType;
import entity.Staff;
import java.time.LocalDateTime;

/**
 * Seeds the four shared master tables on the first run: staff, room types,
 * rooms and guests.
 *
 * These are seeded together because they only make sense together - a room
 * needs its type to be priced, and every module records who did something by
 * pointing at a staff member.
 *
 * @author Tan Chee Yan
 */
public class MasterDataInitializer {

  /** The staff on duty. */
  public ListInterface<Staff> initializeStaff() {
    ListInterface<Staff> staff = new ArrayList<>();

    staff.add(new Staff("ST001", "Nurul Aisyah binti Rahim", Staff.ROLE_FRONT_DESK,
        Staff.SHIFT_MORNING, "012-3345678", true));
    staff.add(new Staff("ST002", "Lim Yong Le", Staff.ROLE_FRONT_DESK,
        Staff.SHIFT_EVENING, "016-7781234", true));
    staff.add(new Staff("ST003", "Kavitha a/p Muniandy", Staff.ROLE_HOUSEKEEPER,
        Staff.SHIFT_MORNING, "011-22398765", true));
    staff.add(new Staff("ST004", "Chong Wei Ming", Staff.ROLE_HOUSEKEEPER,
        Staff.SHIFT_EVENING, "013-4456123", true));
    staff.add(new Staff("ST005", "Tan Chee Yan", Staff.ROLE_SUPERVISOR,
        Staff.SHIFT_MORNING, "017-9987654", true));
    staff.add(new Staff("ST006", "Ivan Wong Jia Hao", Staff.ROLE_MANAGER,
        Staff.SHIFT_MORNING, "019-3312890", true));

    return staff;
  }

  /** The room categories on offer. */
  public ListInterface<RoomType> initializeRoomTypes() {
    ListInterface<RoomType> types = new ArrayList<>();

    types.add(new RoomType("RT01", "Standard Twin", 2, 150.00, 30,
        "Two single beds, garden view"));
    types.add(new RoomType("RT02", "Standard Queen", 2, 180.00, 30,
        "One queen bed, garden view"));
    types.add(new RoomType("RT03", "Deluxe King", 3, 260.00, 45,
        "King bed with balcony, sea view"));
    types.add(new RoomType("RT04", "Family Suite", 5, 420.00, 60,
        "Two bedrooms, living area"));
    types.add(new RoomType("RT05", "Executive Villa", 6, 780.00, 90,
        "Private pool, butler service"));

    return types;
  }

  /**
   * The physical rooms.
   *
   * The sample deliberately covers every combination the front desk has to
   * cope with: rooms ready to sell, rooms occupied, a room mid-clean with a
   * guest waiting on it, one awaiting a supervisor's sign-off, and one out of
   * service. A set where everything was ready would make the availability
   * rules impossible to demonstrate.
   */
  public ListInterface<Room> initializeRooms() {
    ListInterface<Room> rooms = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    // Just checked out of, so empty but not yet cleaned - task HK0001 is
    // waiting against it.
    rooms.add(new Room("1001", "RT01", 10, Room.VACANT, Room.DIRTY,
        false, now.minusDays(1).withHour(10).withMinute(20), ""));
    rooms.add(new Room("1002", "RT01", 10, Room.OCCUPIED, Room.READY_FOR_CHECK_IN,
        false, now.withHour(9).withMinute(5), ""));
    rooms.add(new Room("1003", "RT02", 10, Room.VACANT, Room.CLEANING_IN_PROGRESS,
        false, now.minusDays(1).withHour(11).withMinute(40), "Expedited for urgent booking"));
    rooms.add(new Room("1004", "RT02", 10, Room.RESERVED, Room.READY_FOR_CHECK_IN,
        false, now.withHour(8).withMinute(30), "Held for walk-in booking"));
    rooms.add(new Room("1005", "RT03", 10, Room.VACANT, Room.READY_FOR_CHECK_IN,
        false, now.withHour(9).withMinute(50), ""));
    rooms.add(new Room("1006", "RT01", 10, Room.VACANT, Room.DIRTY,
        false, now.minusDays(1).withHour(8).withMinute(40), "Waiting for checkout clean"));
    rooms.add(new Room("1007", "RT01", 10, Room.VACANT, Room.DIRTY,
        false, now.minusDays(1).withHour(9).withMinute(10), "Urgent booking waiting"));
    rooms.add(new Room("1008", "RT01", 10, Room.VACANT, Room.DIRTY,
        false, now.minusDays(1).withHour(9).withMinute(50), "Urgent booking waiting"));
    // A room with a guest in it was cleaned before they arrived, so it is
    // occupied AND ready - the two statuses describe different things.
    rooms.add(new Room("2001", "RT03", 20, Room.OCCUPIED, Room.READY_FOR_CHECK_IN,
        false, now.withHour(7).withMinute(15), ""));
    rooms.add(new Room("2002", "RT03", 20, Room.VACANT, Room.CLEANING_IN_PROGRESS,
        false, now.minusDays(2).withHour(14).withMinute(0), ""));
    rooms.add(new Room("2003", "RT04", 20, Room.VACANT, Room.READY_FOR_CHECK_IN,
        false, now.withHour(10).withMinute(10), ""));
    rooms.add(new Room("2004", "RT04", 20, Room.VACANT, Room.INSPECTED,
        false, now.withHour(10).withMinute(45), "Awaiting supervisor sign-off"));
    rooms.add(new Room("2005", "RT04", 20, Room.VACANT, Room.READY_FOR_CHECK_IN,
        false, now.withHour(11).withMinute(20), ""));
    rooms.add(new Room("2006", "RT03", 20, Room.VACANT, Room.READY_FOR_CHECK_IN,
        false, now.withHour(12).withMinute(5), ""));
    rooms.add(new Room("3001", "RT05", 30, Room.VACANT, Room.BLOCKED,
        true, now.minusDays(6).withHour(16).withMinute(0), "Aircon compressor replacement"));

    return rooms;
  }

  /** The guests already known to the resort. */
  public ListInterface<Guest> initializeGuests() {
    ListInterface<Guest> guests = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    guests.add(new Guest("G0001", "Tan Chee Yan", "040512-14-5533",
        "012-3456789", "cheeyan@gmail.com", now.minusMonths(17)));
    guests.add(new Guest("G0002", "Lim Yong Le", "031120-08-6621",
        "016-7788990", "yongle.lim@gmail.com", now.minusMonths(14)));
    guests.add(new Guest("G0003", "Nur Farah binti Idris", "990304-10-5128",
        "011-33445566", "farah.idris@yahoo.com", now.minusMonths(21)));
    guests.add(new Guest("G0004", "Daniel Lee Wen Hao", "011225-05-5017",
        "013-2233445", "", now.withHour(9).withMinute(12)));
    guests.add(new Guest("G0005", "Sarah Lim Mei Xin", "970818-14-5266",
        "017-8899001", "sarahlim@hotmail.com", now.minusMonths(37)));
    guests.add(new Guest("G0006", "Rajesh a/l Kumaran", "850226-07-5439",
        "019-4455667", "rajesh.k@gmail.com", now.withHour(9).withMinute(41)));
    guests.add(new Guest("G0007", "Michelle Wong Sze Ying", "020909-14-5872",
        "018-6677889", "", now.withHour(10).withMinute(3)));

    return guests;
  }
}
