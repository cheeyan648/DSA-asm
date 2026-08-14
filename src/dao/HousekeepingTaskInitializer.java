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

    public ListInterface<HousekeepingTask> initializeHousekeepingTasks() {
        ListInterface<HousekeepingTask> taskLog = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        // Records are added in chronological order to preserve history flow.
        taskLog.add(new HousekeepingTask("HT0001", "101", "Dirty", now.minusHours(8)));
        taskLog.add(new HousekeepingTask("HT0002", "102", "Cleaning In Progress", now.minusHours(6)));
        taskLog.add(new HousekeepingTask("HT0003", "103", "Inspected", now.minusHours(5)));
        taskLog.add(new HousekeepingTask("HT0004", "104", "Ready for Check-In", now.minusHours(3)));
        taskLog.add(new HousekeepingTask("HT0005", "101", "Cleaning In Progress", now.minusHours(1)));

        return taskLog;
    }
}