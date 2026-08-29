package test;

import adt.ListInterface;
import control.HousekeepingTaskLogMaintenance;
import control.ResortData;
import control.ResortService;
import entity.Booking;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatusLog;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Optional local Housekeeping demo records. Run separately:
 *
 *   java -cp "build/classes;build/test-classes" test.HousekeepingDemoData
 *
 * Adds only HousekeepingTask / RoomStatusLog rows marked HK-DEMO.
 * Does not change Booking, Room, or Front Desk seed tables.
 * Re-running is a no-op if HK-DEMO tasks already exist.
 */
public class HousekeepingDemoData {

  public static final String MARKER = "HK-DEMO";

  public static void main(String[] args) throws Exception {
    ResortData data = new ResortData();
    if (alreadyApplied(data)) {
      System.out.println("Housekeeping demo data already present. Nothing added.");
      return;
    }

    addCompleted(data, "1004", HousekeepingTask.TYPE_CHECKOUT_CLEAN,
        HousekeepingTask.PRIORITY_NORMAL, 8, 22);
    addCompleted(data, "2003", HousekeepingTask.TYPE_DEEP_CLEAN,
        HousekeepingTask.PRIORITY_NORMAL, 14, 41);
    addCompleted(data, "3001", HousekeepingTask.TYPE_CHECKOUT_CLEAN,
        HousekeepingTask.PRIORITY_URGENT, 19, 63);
    addCompleted(data, "3002", HousekeepingTask.TYPE_DEEP_CLEAN,
        HousekeepingTask.PRIORITY_NORMAL, 9, 28);
    addCompleted(data, "2005", HousekeepingTask.TYPE_CHECKOUT_CLEAN,
        HousekeepingTask.PRIORITY_NORMAL, 15, 37);

    HousekeepingTask maint = new HousekeepingTask(data.nextTaskId(), "3002",
        HousekeepingTask.TYPE_MAINTENANCE, null, LocalDateTime.now().minusDays(1));
    maint.setStatus(HousekeepingTask.BLOCKED);
    maint.setRemark(MARKER + " maintenance in progress");
    data.getTaskList().add(maint);
    data.getStatusLogList().add(new RoomStatusLog(data.nextStatusLogId(),
        maint.getTaskId(), "3002", null, HousekeepingTask.BLOCKED,
        LocalDateTime.now().minusDays(1), "ST004", false, MARKER));

    HousekeepingTaskLogMaintenance hk =
        new HousekeepingTaskLogMaintenance(new ResortService(data), "ST003");
    @SuppressWarnings("unchecked")
    ListInterface<HousekeepingTask> stayover = (ListInterface<HousekeepingTask>)
        invoke(hk, "collectStayoverRecordsForDate",
            new Class<?>[] { LocalDate.class }, LocalDate.now());

    int stayoverCompleted = 0;
    for (int i = 1; i <= stayover.getNumberOfEntries(); i++) {
      HousekeepingTask row = stayover.getEntry(i);
      Booking booking = data.findBooking(row.getBookingId());
      if (booking == null || !row.getRoomNo().equals(booking.getRoomNo())) {
        System.out.println("Skipped Stayover " + row.getTaskId()
            + " — Booking mismatch, not inventing IDs.");
        continue;
      }
      if (!HousekeepingTask.NOT_CLEANED.equals(row.getStatus())) {
        continue;
      }
      LocalDateTime start = LocalDate.now().atTime(i == 1 ? 8 : 16, 10);
      row.setStartedAt(start);
      row.setCompletedAt(start.plusMinutes(i == 1 ? 24 : 48));
      row.setStatus(HousekeepingTask.CLEANED);
      row.setRemark("Stayover service (" + MARKER + ")");
      data.getStatusLogList().add(new RoomStatusLog(data.nextStatusLogId(),
          row.getTaskId(), row.getRoomNo(),
          HousekeepingTask.CLEANING_IN_PROGRESS, HousekeepingTask.CLEANED,
          row.getCompletedAt(), "ST003", false, MARKER));
      stayoverCompleted++;
      if (stayoverCompleted >= 2) {
        break;
      }
    }

    data.saveHousekeeping();
    System.out.println("Housekeeping demo data saved (tasks + status logs only).");
    System.out.println("Rooms used: 1004, 2003, 2005, 3001, 3002 plus Stayover rooms from Booking.");
    System.out.println("Stayover completed from real Booking rows: " + stayoverCompleted);
  }

  private static boolean alreadyApplied(ResortData data) {
    return data.getTaskList().search(task ->
        task.getRemark() != null && task.getRemark().contains(MARKER)) != null;
  }

  private static void addCompleted(ResortData data, String roomNo, String type,
      String priority, int startHour, int durationMinutes) {
    Room room = data.findRoom(roomNo);
    if (room == null) {
      System.out.println("Skip " + roomNo + " — not in master Room list.");
      return;
    }
    LocalDateTime started = LocalDate.now().minusDays(1).atTime(startHour, 15);
    HousekeepingTask task = new HousekeepingTask(data.nextTaskId(), roomNo, type,
        null, started.minusMinutes(5));
    task.setPriority(priority);
    task.setStatus(HousekeepingTask.READY_FOR_CHECK_IN);
    task.setStartedAt(started);
    task.setCompletedAt(started.plusMinutes(durationMinutes));
    task.setRemark(MARKER + " completed " + type);
    data.getTaskList().add(task);
    data.getStatusLogList().add(new RoomStatusLog(data.nextStatusLogId(),
        task.getTaskId(), roomNo, HousekeepingTask.INSPECTED,
        HousekeepingTask.READY_FOR_CHECK_IN, task.getCompletedAt(),
        "ST005", false, MARKER));
  }

  private static Object invoke(Object target, String name, Class<?>[] types, Object... args)
      throws Exception {
    Method method = target.getClass().getDeclaredMethod(name, types);
    method.setAccessible(true);
    return method.invoke(target, args);
  }
}
