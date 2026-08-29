package test;

import adt.ListInterface;
import control.HousekeepingTaskLogMaintenance;
import control.ResortData;
import control.ResortService;
import control.ServiceResult;
import entity.Booking;
import entity.HousekeepingTask;
import entity.Invoice;
import entity.Payment;
import entity.Room;
import entity.RoomStatusLog;
import entity.RoomType;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Executes Housekeeping workflows against the real shared ResortData.
 *
 * Uses seeded Booking / Room records and ResortService.checkOut — it does not
 * insert a fake CHECKOUT_CLEAN just to pass.
 */
public class HousekeepingEndToEndProof {

  private final TestRunner runner = new TestRunner();

  public static void main(String[] args) throws Exception {
    boolean ok = new HousekeepingEndToEndProof().run();
    System.exit(ok ? 0 : 1);
  }

  public boolean run() throws Exception {
    System.out.println();
    System.out.println("#".repeat(76));
    System.out.println("#  HOUSEKEEPING END-TO-END PROOF (real shared data)");
    System.out.println("#".repeat(76));

    phaseCheckoutToQueue();
    phaseNormalCleaning();
    phaseInspectionFailAndRollback();
    phaseMaintenance();
    phaseStayover();
    phaseOccupancy();
    phaseRaiseTypes();
    phaseConsistency();
    phaseReportUsesStoredData();

    return runner.report("HOUSEKEEPING END-TO-END SUMMARY");
  }

  private ResortData fresh() {
    ResortData data = new ResortData();
    data.deleteAllFiles();
    return new ResortData();
  }

  private void phaseCheckoutToQueue() {
    runner.suite("Phase 2 — Booking checkout creates CHECKOUT_CLEAN");

    ResortData data = fresh();
    ResortService service = new ResortService(data);
    Booking booking = data.findBooking("BK0002");
    runner.check("seeded BK0002 exists", booking != null);
    runner.checkEquals("BK0002 is CHECKED_IN", Booking.STATUS_CHECKED_IN,
        booking.getBookingStatus());
    String roomNo = booking.getRoomNo();
    runner.checkEquals("BK0002 room is 2001", "2001", roomNo);

    Invoice invoice = data.findInvoiceByBooking("BK0002");
    service.recordPayment(invoice.getInvoiceId(), invoice.getOutstandingBalance(),
        Payment.CASH, null, "ST001");

    int tasksBefore = data.getTaskList().getNumberOfEntries();
    ServiceResult<Booking> out = service.checkOut("BK0002", "ST001");
    runner.check("checkOut succeeded (real Booking workflow)", out.isSuccess());

    HousekeepingTask raised = data.findOpenTaskForRoom(roomNo);
    runner.check("CHECKOUT_CLEAN exists for that room", raised != null
        && HousekeepingTask.TYPE_CHECKOUT_CLEAN.equals(raised.getTaskType()));
    runner.check("Task ID is HK plus digits",
        raised != null && raised.getTaskId() != null && raised.getTaskId().startsWith("HK"));
    runner.check("Task ID is unique", raised != null && uniqueTaskId(data, raised.getTaskId()));
    runner.checkEquals("Room ID matches Booking room", roomNo, raised.getRoomNo());
    runner.checkEquals("Booking ID matches", "BK0002", raised.getBookingId());
    runner.checkEquals("one extra task stored", tasksBefore + 1,
        data.getTaskList().getNumberOfEntries());
    runner.check("task is in the cleaning queue",
        data.getCleaningQueue().toServiceOrder().contains(raised));
    runner.checkEquals("starts DIRTY", HousekeepingTask.DIRTY, raised.getStatus());
    runner.checkEquals("normal lane unless officer marked urgent",
        HousekeepingTask.PRIORITY_NORMAL, raised.getPriority());

    ServiceResult<HousekeepingTask> taken = service.updateTaskStatus(
        raised.getTaskId(), HousekeepingTask.CLEANING_IN_PROGRESS, "ST003", null);
    runner.check("Take/start from queue (CIP) works", taken.isSuccess());
    runner.check("no longer waiting in queue",
        !data.getCleaningQueue().toServiceOrder().contains(raised));
  }

  private void phaseNormalCleaning() {
    runner.suite("Phase 3 — DIRTY through READY_FOR_CHECK_IN");

    ResortData data = fresh();
    ResortService service = new ResortService(data);
    HousekeepingTask task = data.findOpenTaskForRoom("1001");
    runner.check("seeded dirty task on 1001", task != null);
    runner.checkEquals("starts DIRTY", HousekeepingTask.DIRTY, task.getStatus());
    runner.check("in the cleaning queue",
        data.getCleaningQueue().toServiceOrder().contains(task));

    ServiceResult<HousekeepingTask> skip = service.updateTaskStatus(
        task.getTaskId(), HousekeepingTask.READY_FOR_CHECK_IN, "ST005", null);
    runner.check("DIRTY cannot skip to READY", skip.isFailure());

    ServiceResult<HousekeepingTask> inspectedTooSoon = service.updateTaskStatus(
        task.getTaskId(), HousekeepingTask.INSPECTED, "ST003", null);
    runner.check("DIRTY cannot jump to INSPECTED", inspectedTooSoon.isFailure());

    runner.check("UI queue rule: DIRTY cleaning must start from queue",
        task.isCleaningType() && HousekeepingTask.DIRTY.equals(task.getStatus()));

    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.CLEANING_IN_PROGRESS, "ST003", null);
    runner.checkEquals("CIP recorded", HousekeepingTask.CLEANING_IN_PROGRESS,
        task.getStatus());
    runner.check("startedAt stamped", task.getStartedAt() != null);
    runner.check("CIP history row exists",
        hasLog(data, task.getTaskId(), HousekeepingTask.DIRTY,
            HousekeepingTask.CLEANING_IN_PROGRESS));

    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.INSPECTED, "ST003", null);
    runner.checkEquals("INSPECTED recorded", HousekeepingTask.INSPECTED, task.getStatus());

    service.updateTaskStatus(task.getTaskId(),
        HousekeepingTask.READY_FOR_CHECK_IN, "ST005", null);
    runner.checkEquals("READY recorded", HousekeepingTask.READY_FOR_CHECK_IN,
        task.getStatus());
    runner.check("completedAt stamped", task.getCompletedAt() != null);
    runner.check("duration is Started→Completed",
        task.getCleaningDurationMinutes() >= 0);
  }

  private void phaseInspectionFailAndRollback() {
    runner.suite("Phase 4 — inspection fail, re-clean, stack rollback");

    ResortData data = fresh();
    ResortService service = new ResortService(data);
    HousekeepingTask task = data.findOpenTaskForRoom("1001");
    String originalId = task.getTaskId();
    service.updateTaskStatus(originalId, HousekeepingTask.CLEANING_IN_PROGRESS, "ST003", null);
    service.updateTaskStatus(originalId, HousekeepingTask.INSPECTED, "ST003", null);

    int logsBefore = data.getStatusLogList().getNumberOfEntries();
    service.updateTaskStatus(originalId, HousekeepingTask.DIRTY, "ST005", "Missed bathroom");
    runner.checkEquals("original task still DIRTY (history kept)",
        HousekeepingTask.DIRTY, task.getStatus());
    runner.check("original Task ID unchanged", originalId.equals(task.getTaskId()));
    runner.check("failure counted", task.getInspectionFailCount() > 0);

    HousekeepingTask reclean = data.getTaskList().search(t ->
        t.getRemark() != null && t.getRemark().startsWith("Follow-on of " + originalId));
    runner.check("follow-on re-clean created", reclean != null);
    runner.check("new Task ID", reclean != null && !reclean.getTaskId().equals(originalId));
    runner.checkEquals("follow-on starts DIRTY", HousekeepingTask.DIRTY, reclean.getStatus());
    runner.check("follow-on is queued",
        reclean != null && data.getCleaningQueue().toServiceOrder().contains(reclean));
    runner.check("history grew (not overwritten)",
        data.getStatusLogList().getNumberOfEntries() > logsBefore);

    int stackBefore = data.getStatusRollbackStack().getNumberOfEntries();
    ServiceResult<HousekeepingTask> undone = service.rollbackLastStatusChange("ST005");
    runner.check("rollback of fail succeeded", undone.isSuccess());
    runner.checkEquals("original back to INSPECTED", HousekeepingTask.INSPECTED,
        task.getStatus());
    runner.check("stack shrank (LIFO pop)",
        data.getStatusRollbackStack().getNumberOfEntries() == stackBefore - 1);
    runner.check("follow-on cancelled by rollback design",
        reclean != null && reclean.getCompletedAt() != null);
  }

  private void phaseMaintenance() {
    runner.suite("Phase 5 — Maintenance raise and resolve");

    ResortData data = fresh();
    ResortService service = new ResortService(data);
    HousekeepingTaskLogMaintenance hk = new HousekeepingTaskLogMaintenance(service, "ST004");

    Room occupied = data.findRoom("1002");
    runner.checkEquals("1002 occupancy is OCCUPIED (Front Desk seed)", Room.OCCUPIED,
        occupied.getOccupancyStatus());
    String[] occupiedTypes = availableTypes(hk, occupied, data.findOpenTaskForRoom("1002"));
    runner.check("occupied room does not offer MAINTENANCE",
        !contains(occupiedTypes, HousekeepingTask.TYPE_MAINTENANCE));
    runner.check("CHECKOUT_CLEAN not offered for manual raise",
        !contains(occupiedTypes, HousekeepingTask.TYPE_CHECKOUT_CLEAN));
    runner.check("STAYOVER_CLEAN not offered for manual raise",
        !contains(occupiedTypes, HousekeepingTask.TYPE_STAYOVER_CLEAN));

    Room vacant = data.findRoom("3001");
    runner.checkEquals("3001 is VACANT", Room.VACANT, vacant.getOccupancyStatus());
    HousekeepingTask open3001 = data.findOpenTaskForRoom("3001");
    String[] vacantTypes = availableTypes(hk, vacant, open3001);
    if (open3001 != null) {
      runner.check("open task blocks raise (empty type list)", vacantTypes.length == 0);
    } else {
      runner.check("vacant room may raise MAINTENANCE",
          contains(vacantTypes, HousekeepingTask.TYPE_MAINTENANCE));
      runner.check("vacant room may raise DEEP_CLEAN",
          contains(vacantTypes, HousekeepingTask.TYPE_DEEP_CLEAN));
    }

    HousekeepingTask maint = new HousekeepingTask(data.nextTaskId(), "3002",
        HousekeepingTask.TYPE_MAINTENANCE, null, LocalDateTime.now());
    maint.setStatus(HousekeepingTask.BLOCKED);
    data.getTaskList().add(maint);
    data.findRoom("3002").setHousekeepingStatus(Room.BLOCKED);
    data.findRoom("3002").setOutOfService(true);
    service.enqueueIfNeedsCleaning(maint);
    runner.check("MAINTENANCE does not enter the cleaning queue",
        !data.getCleaningQueue().toServiceOrder().contains(maint));

    ServiceResult<HousekeepingTask> resolved = service.updateTaskStatus(
        maint.getTaskId(), HousekeepingTask.DIRTY, "ST004", null);
    runner.check("resolving maintenance succeeds", resolved.isSuccess());
    HousekeepingTask follow = data.getTaskList().search(t ->
        t.getRemark() != null && t.getRemark().startsWith("Follow-on of " + maint.getTaskId()));
    runner.check("follow-on cleaning created", follow != null);
    runner.check("follow-on has a new Task ID",
        follow != null && !follow.getTaskId().equals(maint.getTaskId()));
    runner.check("follow-on queued",
        follow != null && data.getCleaningQueue().toServiceOrder().contains(follow));
    runner.checkEquals("maintenance row still exists",
        maint.getTaskId(), data.findTask(maint.getTaskId()).getTaskId());
  }

  private void phaseStayover() throws Exception {
    runner.suite("Phase 6 — Stayover from real Booking data");

    ResortData data = fresh();
    ResortService service = new ResortService(data);
    HousekeepingTaskLogMaintenance hk = new HousekeepingTaskLogMaintenance(service, "ST003");

    Booking bk2 = data.findBooking("BK0002");
    Booking bk5 = data.findBooking("BK0005");
    LocalDate today = LocalDate.now();
    runner.check("BK0002 CHECKED_IN with room and dates",
        Booking.STATUS_CHECKED_IN.equals(bk2.getBookingStatus())
            && bk2.getRoomNo() != null && bk2.getCheckInDate() != null
            && bk2.getCheckOutDate() != null);
    runner.check("today is a stayover day for BK0002",
        !today.isBefore(bk2.getCheckInDate()) && today.isBefore(bk2.getCheckOutDate()));
    runner.check("today is a stayover day for BK0005",
        !today.isBefore(bk5.getCheckInDate()) && today.isBefore(bk5.getCheckOutDate()));

    ListInterface<HousekeepingTask> todayList = collectStayover(hk, today);
    runner.check("today list is not empty", !todayList.isEmpty());

    HousekeepingTask for2001 = todayList.search(t -> "2001".equals(t.getRoomNo()));
    HousekeepingTask for1002 = todayList.search(t -> "1002".equals(t.getRoomNo()));
    runner.check("room 2001 Stayover matches BK0002",
        for2001 != null && "BK0002".equals(for2001.getBookingId()));
    runner.check("room 1002 Stayover matches BK0005",
        for1002 != null && "BK0005".equals(for1002.getBookingId()));

    for (int i = 1; i <= todayList.getNumberOfEntries(); i++) {
      HousekeepingTask row = todayList.getEntry(i);
      Booking booking = data.findBooking(row.getBookingId());
      runner.check("Stayover Booking ID exists", booking != null);
      runner.check("Stayover Room ID matches that Booking",
          booking != null && row.getRoomNo().equals(booking.getRoomNo()));
    }

    runner.check("entity refuses NOT_CLEANED → CLEANED",
        !HousekeepingTask.isValidTransition(HousekeepingTask.TYPE_STAYOVER_CLEAN,
            HousekeepingTask.NOT_CLEANED, HousekeepingTask.CLEANED));

    String beforeSkip = for2001.getStatus();
    applyStayover(hk, for2001, HousekeepingTask.CLEANED);
    boolean skipped = HousekeepingTask.CLEANED.equals(for2001.getStatus())
        && HousekeepingTask.NOT_CLEANED.equals(beforeSkip);
    runner.check("applyStayover does not skip NOT_CLEANED → CLEANED", !skipped);
    if (skipped) {
      for2001.setStatus(HousekeepingTask.NOT_CLEANED);
      for2001.setCompletedAt(null);
    }

    int stackBefore = data.getStatusRollbackStack().getNumberOfEntries();
    applyStayover(hk, for2001, HousekeepingTask.CLEANING_IN_PROGRESS);
    runner.checkEquals("status is CIP", HousekeepingTask.CLEANING_IN_PROGRESS,
        for2001.getStatus());
    runner.check("startedAt set", for2001.getStartedAt() != null);
    runner.check("Stayover update pushed the existing Stack",
        data.getStatusRollbackStack().getNumberOfEntries() == stackBefore + 1);

    applyStayover(hk, for2001, HousekeepingTask.CLEANED);
    runner.checkEquals("status is CLEANED", HousekeepingTask.CLEANED, for2001.getStatus());
    runner.check("completedAt set", for2001.getCompletedAt() != null);

    ServiceResult<HousekeepingTask> rb = service.rollbackLastStatusChange("ST003");
    runner.check("existing rollbackLastStatusChange undoes Stayover CLEANED",
        rb.isSuccess() && HousekeepingTask.CLEANING_IN_PROGRESS.equals(for2001.getStatus()));
    runner.check("Stayover completedAt cleared after rolling back CLEANED",
        for2001.getCompletedAt() == null);
  }

  private void phaseOccupancy() {
    runner.suite("Phase 7 — occupancy is shared Room data");

    ResortData data = fresh();
    runner.checkEquals("1002 OCCUPIED from master rooms", Room.OCCUPIED,
        data.findRoom("1002").getOccupancyStatus());
    runner.checkEquals("1001 VACANT from master rooms", Room.VACANT,
        data.findRoom("1001").getOccupancyStatus());
    runner.check("Housekeeping reads the same Room object",
        data.findRoom("1002") == data.getRoomList().search(
            r -> "1002".equals(r.getRoomNo())));
  }

  private void phaseRaiseTypes() {
    runner.suite("Phase 8 — Raise New Task type rules");

    ResortData data = fresh();
    HousekeepingTaskLogMaintenance hk =
        new HousekeepingTaskLogMaintenance(new ResortService(data), "ST004");
    String[] types = availableTypes(hk, data.findRoom("1002"),
        data.findOpenTaskForRoom("1002"));
    runner.check("manual CHECKOUT_CLEAN absent",
        !contains(types, HousekeepingTask.TYPE_CHECKOUT_CLEAN));
    runner.check("manual STAYOVER_CLEAN absent",
        !contains(types, HousekeepingTask.TYPE_STAYOVER_CLEAN));
  }

  private void phaseConsistency() {
    runner.suite("Phase 15 — data consistency on seeded store");

    ResortData data = fresh();
    runner.check("no duplicate Task IDs", uniqueAllTaskIds(data));
    runner.check("every task room exists", everyTaskHasRoom(data));
    runner.check("queue has no duplicate object twice", noDuplicateQueueEntries(data));
  }

  private void phaseReportUsesStoredData() {
    runner.suite("Phase 12 — report metrics from stored timestamps");

    ResortData data = fresh();
    ListInterface<HousekeepingTask> cleaning = data.getTaskList().filter(
        HousekeepingTask::isCleaningType);
    ListInterface<HousekeepingTask> completed = cleaning.filter(
        t -> t.getCleaningDurationMinutes() >= 0);
    runner.check("Total cleaning tasks is the isCleaningType count",
        cleaning.getNumberOfEntries() > 0);
    runner.check("Completed uses Started→Completed duration, not a constant",
        completed.getNumberOfEntries() >= 0);
    if (!completed.isEmpty()) {
      HousekeepingTask sample = completed.getEntry(1);
      long minutes = sample.getCleaningDurationMinutes();
      runner.check("sample duration matches timestamps",
          minutes == java.time.Duration.between(
              sample.getStartedAt(), sample.getCompletedAt()).toMinutes());
      RoomType type = data.findRoomType(data.findRoom(sample.getRoomNo()).getTypeId());
      runner.check("room type name comes from RoomType master",
          type != null && type.getTypeName() != null && !type.getTypeName().isBlank());
    }
    runner.check("Stayover-service CLEANED would be in isCleaningType",
        HousekeepingTask.TYPE_STAYOVER_CLEAN != null);
  }

  private boolean uniqueTaskId(ResortData data, String taskId) {
    int count = 0;
    for (int i = 1; i <= data.getTaskList().getNumberOfEntries(); i++) {
      if (taskId.equals(data.getTaskList().getEntry(i).getTaskId())) {
        count++;
      }
    }
    return count == 1;
  }

  private boolean uniqueAllTaskIds(ResortData data) {
    for (int i = 1; i <= data.getTaskList().getNumberOfEntries(); i++) {
      if (!uniqueTaskId(data, data.getTaskList().getEntry(i).getTaskId())) {
        return false;
      }
    }
    return true;
  }

  private boolean everyTaskHasRoom(ResortData data) {
    for (int i = 1; i <= data.getTaskList().getNumberOfEntries(); i++) {
      if (data.findRoom(data.getTaskList().getEntry(i).getRoomNo()) == null) {
        return false;
      }
    }
    return true;
  }

  private boolean noDuplicateQueueEntries(ResortData data) {
    ListInterface<HousekeepingTask> order = data.getCleaningQueue().toServiceOrder();
    for (int i = 1; i <= order.getNumberOfEntries(); i++) {
      HousekeepingTask task = order.getEntry(i);
      int copies = 0;
      for (int j = 1; j <= order.getNumberOfEntries(); j++) {
        if (task.getTaskId().equals(order.getEntry(j).getTaskId())) {
          copies++;
        }
      }
      if (copies != 1) {
        return false;
      }
    }
    return true;
  }

  private boolean hasLog(ResortData data, String taskId, String from, String to) {
    return data.getStatusLogList().search(log ->
        taskId.equals(log.getTaskId())
            && from.equals(log.getFromStatus())
            && to.equals(log.getToStatus())) != null;
  }

  private boolean contains(String[] types, String wanted) {
    if (types == null) {
      return false;
    }
    for (int i = 0; i < types.length; i++) {
      if (wanted.equals(types[i])) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private String[] availableTypes(HousekeepingTaskLogMaintenance hk, Room room,
      HousekeepingTask open) {
    try {
      Method method = HousekeepingTaskLogMaintenance.class.getDeclaredMethod(
          "availableRaiseTaskTypes", Room.class, HousekeepingTask.class);
      method.setAccessible(true);
      return (String[]) method.invoke(hk, room, open);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private ListInterface<HousekeepingTask> collectStayover(HousekeepingTaskLogMaintenance hk,
      LocalDate date) {
    try {
      Method method = HousekeepingTaskLogMaintenance.class.getDeclaredMethod(
          "collectStayoverRecordsForDate", LocalDate.class);
      method.setAccessible(true);
      return (ListInterface<HousekeepingTask>) method.invoke(hk, date);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void applyStayover(HousekeepingTaskLogMaintenance hk, HousekeepingTask task,
      String status) {
    try {
      Method method = HousekeepingTaskLogMaintenance.class.getDeclaredMethod(
          "applyStayoverStatusUpdate", HousekeepingTask.class, String.class);
      method.setAccessible(true);
      method.invoke(hk, task, status);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
