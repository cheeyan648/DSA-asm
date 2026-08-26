package dao;

import adt.ArrayList;
import adt.ListInterface;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Reads and writes one entity type to its own .dat file.
 *
 * Every entity is persisted the same way, so rather than repeat that code once
 * per table the file name is passed in and the type is left generic. The
 * records are written one object at a time and read back until the stream ends,
 * which is why the end-of-file exception is caught and treated as a normal
 * finish rather than an error.
 *
 * A missing file is not a failure either - it simply means the system has not
 * been run before, and the caller seeds the file from an initializer.
 *
 * @author Tan Chee Yan
 */
public class GenericDAO<T> {

  private final String fileName;

  public GenericDAO(String fileName) {
    this.fileName = fileName;
  }

  public String getFileName() {
    return fileName;
  }

  /**
   * Whether this file has been written before.
   *
   * @return true if the data file exists and holds something
   */
  public boolean exists() {
    File file = new File(fileName);
    return file.exists() && file.length() > 0;
  }

  /**
   * Reads every record from the file.
   *
   * @return the stored records, or an empty list if the file is absent or
   * unreadable
   */
  @SuppressWarnings("unchecked")
  public ListInterface<T> retrieveFromFile() {
    ListInterface<T> records = new ArrayList<>();

    if (!exists()) {
      return records;
    }

    try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(fileName))) {
      while (true) {
        records.add((T) input.readObject());
      }
    } catch (EOFException endOfFile) {
      // Every record has been read - this is how the loop ends normally.
    } catch (IOException | ClassNotFoundException e) {
      System.out.println("Could not read " + fileName + ": " + e.getMessage());
      System.out.println("Starting with no records for this file.");
      return new ArrayList<>();
    }

    return records;
  }

  /**
   * Writes every record, replacing whatever the file held before.
   *
   * @param records the records to store
   * @return true if the file was written
   */
  public boolean saveToFile(ListInterface<T> records) {
    if (records == null) {
      return false;
    }

    try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(fileName))) {
      for (int i = 1; i <= records.getNumberOfEntries(); i++) {
        output.writeObject(records.getEntry(i));
      }
      return true;
    } catch (IOException e) {
      System.out.println("Could not save " + fileName + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Deletes the data file. Used by the tests to start from a known state.
   *
   * @return true if a file was deleted
   */
  public boolean deleteFile() {
    File file = new File(fileName);
    return file.exists() && file.delete();
  }
}
