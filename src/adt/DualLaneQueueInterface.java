package adt;

/**
 * A waiting line with two fixed lanes: URGENT and NORMAL.
 *
 * next() always drains the urgent lane before it touches the normal lane, and
 * inside each lane the order is strict FIFO. Nothing is ever promoted from the
 * normal lane to the urgent lane - the only thing that decides a lane is the
 * priority given when the entry is enqueued.
 *
 * @author Wong Chee Yan
 */
public interface DualLaneQueueInterface<T> {

  public static final String URGENT = "URGENT";
  public static final String NORMAL = "NORMAL";

  /**
   * Task: Adds an entry to the back of the lane its priority names.
   *
   * @param newEntry the object to be added
   * @param priority URGENT or NORMAL; anything else is treated as NORMAL
   * @return true if the addition is successful, or false if that lane is full
   */
  public boolean enqueue(T newEntry, String priority);

  /**
   * Task: Removes and returns the entry that should be handled next - the front
   * of the urgent lane if one is waiting, otherwise the front of the normal
   * lane.
   *
   * @return the next entry, or null if both lanes are empty
   */
  public T next();

  /**
   * Task: Retrieves, without removing, the entry that next() would return.
   *
   * @return the next entry, or null if both lanes are empty
   */
  public T peekNext();

  /**
   * Task: Removes a given entry from whichever lane holds it.
   *
   * @param anEntry the object to be removed
   * @return true if the entry was found and removed, or false if not
   */
  public boolean removeEntry(T anEntry);

  /**
   * Task: Sees whether both lanes are empty.
   *
   * @return true if no entry is waiting in either lane
   */
  public boolean isEmpty();

  /**
   * Task: Empties both lanes.
   */
  public void clear();

  /**
   * Task: Counts the entries waiting in the urgent lane.
   *
   * @return the number of urgent entries
   */
  public int getUrgentCount();

  /**
   * Task: Counts the entries waiting in the normal lane.
   *
   * @return the number of normal entries
   */
  public int getNormalCount();

  /**
   * Task: Counts every entry waiting in both lanes.
   *
   * @return the total number of waiting entries
   */
  public int getNumberOfEntries();

  /**
   * Task: Lists every waiting entry in the exact order next() would return
   * them - the whole urgent lane first, then the whole normal lane - without
   * removing anything.
   *
   * @return the waiting entries in service order
   */
  public ListInterface<T> toServiceOrder();
}
