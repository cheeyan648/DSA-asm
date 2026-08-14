package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.FrontDeskRecord;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Handles loading and saving Front-Desk records.
 *
 * @author Yong Le
 */
public class BookingDAO {

    private static final String FILE_NAME = "bookings.dat";

    public ListInterface<FrontDeskRecord> loadRecords() {

        ListInterface<FrontDeskRecord> recordList
                = new ArrayList<>();

        try (
            ObjectInputStream inputStream
                    = new ObjectInputStream(
                            new FileInputStream(FILE_NAME))
        ) {

            while (true) {

                FrontDeskRecord record
                        = (FrontDeskRecord)
                                inputStream.readObject();

                recordList.add(record);
            }

        } catch (EOFException e) {

            // End of file reached normally.

        } catch (IOException | ClassNotFoundException e) {

            System.out.println(
                    "No existing booking data found. "
                    + "A new file will be created.");
        }

        return recordList;
    }

    public void saveRecords(
            ListInterface<FrontDeskRecord> recordList) {

        try (
            ObjectOutputStream outputStream
                    = new ObjectOutputStream(
                            new FileOutputStream(FILE_NAME))
        ) {

            for (int i = 1;
                    i <= recordList.getNumberOfEntries();
                    i++) {

                outputStream.writeObject(
                        recordList.getEntry(i));
            }

        } catch (IOException e) {

            System.out.println(
                    "Error saving booking data: "
                    + e.getMessage());
        }
    }
}