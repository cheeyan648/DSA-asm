package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.WalkInGuest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Saves and loads the walk-in guest queue to/from a .dat file so the queue
 * survives between runs of the program.
 *
 * @author Kat Tan
 */
public class WalkInGuestDAO {

  private String fileName = "walkInGuests.dat";

  public void saveToFile(ListInterface<WalkInGuest> walkInQueue) {
    File file = new File(fileName);
    try {
      ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file));
      ooStream.writeObject(walkInQueue);
      ooStream.close();
    } catch (FileNotFoundException ex) {
      System.out.println("\nFile not found");
    } catch (IOException ex) {
      System.out.println("\nCannot save to file");
    }
  }

  public ListInterface<WalkInGuest> retrieveFromFile() {
    File file = new File(fileName);
    ListInterface<WalkInGuest> walkInQueue = new ArrayList<>();
    try {
      ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file));
      walkInQueue = (ArrayList<WalkInGuest>) (oiStream.readObject());
      oiStream.close();
    } catch (FileNotFoundException ex) {
      // Normal on the very first run - the caller seeds sample data instead.
    } catch (IOException ex) {
      System.out.println("\nCannot read from file.");
    } catch (ClassNotFoundException ex) {
      System.out.println("\nClass not found.");
    }
    return walkInQueue;
  }
}
