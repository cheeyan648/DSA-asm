package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.HousekeepingTask;
import java.time.LocalDateTime;

/**
 * Generates a small set of sample housekeeping task records, used to seed
 * housekeepingTasks.dat the first time the program runs (or whenever the task
 * log is empty) so there is existing data to demonstrate with.
 *
 * @author Kat Tan
 */
public class HousekeepingTaskInitializer {

    /** Creates sample task records for the first program run. */
    public ListInterface<HousekeepingTask> initializeHousekeepingTasks() {
        ListInterface<HousekeepingTask> taskLog = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        // Records are added in chronological order to preserve history flow.
        taskLog.add(new HousekeepingTask("HT0001", "1001", "Dirty", now.minusHours(24)));
        taskLog.add(new HousekeepingTask("HT0002", "1002", "Cleaning In Progress", now.minusHours(22)));
        taskLog.add(new HousekeepingTask("HT0003", "1003", "Inspected", now.minusHours(20)));
        taskLog.add(new HousekeepingTask("HT0004", "1004", "Ready for Check-In", now.minusHours(18)));
        taskLog.add(new HousekeepingTask("HT0005", "1005", "Dirty", now.minusHours(16)));
        taskLog.add(new HousekeepingTask("HT0006", "1006", "Cleaning In Progress", now.minusHours(14)));
        taskLog.add(new HousekeepingTask("HT0007", "1007", "Inspected", now.minusHours(12)));
        taskLog.add(new HousekeepingTask("HT0008", "1008", "Ready for Check-In", now.minusHours(10)));
        taskLog.add(new HousekeepingTask("HT0009", "1009", "Dirty", now.minusHours(9)));
        taskLog.add(new HousekeepingTask("HT0010", "1010", "Cleaning In Progress", now.minusHours(8)));

        taskLog.add(new HousekeepingTask("HT0011", "1001", "Cleaning In Progress", now.minusHours(7)));
        taskLog.add(new HousekeepingTask("HT0012", "1002", "Inspected", now.minusHours(6)));
        taskLog.add(new HousekeepingTask("HT0013", "1003", "Ready for Check-In", now.minusHours(5)));
        taskLog.add(new HousekeepingTask("HT0014", "1004", "Dirty", now.minusHours(4)));
        taskLog.add(new HousekeepingTask("HT0015", "1005", "Cleaning In Progress", now.minusHours(3)));
        taskLog.add(new HousekeepingTask("HT0016", "1006", "Inspected", now.minusHours(2)));
        taskLog.add(new HousekeepingTask("HT0017", "1007", "Ready for Check-In", now.minusHours(1)));

        taskLog.add(new HousekeepingTask("HT0018", "1008", "Dirty", now.minusMinutes(50)));
        taskLog.add(new HousekeepingTask("HT0019", "1009", "Cleaning In Progress", now.minusMinutes(45)));
        taskLog.add(new HousekeepingTask("HT0020", "1010", "Inspected", now.minusMinutes(40)));
        taskLog.add(new HousekeepingTask("HT0021", "1001", "Ready for Check-In", now.minusMinutes(35)));
        taskLog.add(new HousekeepingTask("HT0022", "1003", "Dirty", now.minusMinutes(30)));
        taskLog.add(new HousekeepingTask("HT0023", "1005", "Cleaning In Progress", now.minusMinutes(25)));
        taskLog.add(new HousekeepingTask("HT0024", "1007", "Inspected", now.minusMinutes(20)));
        taskLog.add(new HousekeepingTask("HT0025", "1009", "Ready for Check-In", now.minusMinutes(15)));

        return taskLog;
    }
}