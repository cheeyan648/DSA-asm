package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.HousekeepingTask;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Kat Tan
 */
public class HousekeepingTaskDAO {

        private final String fileName = "housekeepingTasks.dat";

        private int nextTaskNumber = 1;

        public void saveToFile(ListInterface<HousekeepingTask> taskLog, int nextTaskNumber) {
                try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(fileName))) {
                        // Save the whole Housekeeping List ADT
                        outputStream.writeObject(taskLog);

                        // Save next Task ID number
                        outputStream.writeInt(nextTaskNumber);

                        // Keep DAO field synchronized
                        this.nextTaskNumber = nextTaskNumber;
                } catch (IOException ex) {
                        System.out.println("\nUnable to save housekeeping data.");
                        System.out.println("Reason: " + ex.getMessage());
                }
        }

        @SuppressWarnings("unchecked")
        public ListInterface<HousekeepingTask> retrieveFromFile() {
                ListInterface<HousekeepingTask> taskLog = new ArrayList<>();
                File file = new File(fileName);
                // First time running the program
                if (!file.exists() || file.length() == 0) {
                        nextTaskNumber = 1;
                        return taskLog;
                }

                try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
                        // Read Housekeeping List ADT
                        Object data = inputStream.readObject();
                        taskLog = (ListInterface<HousekeepingTask>) data;
                        try {
                                // Read next available Task ID number
                                nextTaskNumber = inputStream.readInt();
                        } catch (EOFException ex) {
                                nextTaskNumber = determineNextTaskNumber(taskLog);
                        }

                } catch (ClassNotFoundException ex) {
                        System.out.println("\nUnable to load housekeeping data.");
                        System.out.println("Housekeeping data class not found.");
                        taskLog = new ArrayList<>();
                        nextTaskNumber = 1;
                } catch (ClassCastException ex) {
                        System.out.println("\nUnable to load housekeeping data.");
                        System.out.println("Invalid housekeeping data format.");
                        taskLog = new ArrayList<>();
                        nextTaskNumber = 1;
                } catch (IOException ex) {
                        System.out.println("\nUnable to read housekeeping data.");
                        System.out.println("Reason: " + ex.getMessage());
                        taskLog = new ArrayList<>();
                        nextTaskNumber = 1;
                }
                return taskLog;
        }

        private int determineNextTaskNumber(ListInterface<HousekeepingTask> taskLog) {
                int highestNumber = 0;
                for (int i = 1; i <= taskLog.getNumberOfEntries(); i++) {
                        HousekeepingTask task = taskLog.getEntry(i);
                        String taskId = task.getTaskId();
                        if (taskId != null && taskId.matches("HT\\d{4}")) {
                                try {
                                        int number = Integer.parseInt(taskId.substring(2));
                                        if (number > highestNumber) {
                                                highestNumber = number;
                                        }
                                } catch (NumberFormatException ex) {
                                        // Ignore invalid Task ID
                                }
                        }
                }
                return highestNumber + 1;
        }

        public int getNextTaskNumber() {
                return nextTaskNumber;
        }
}