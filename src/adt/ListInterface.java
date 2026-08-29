package adt;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Positions in this list are 1-based: the first entry is at position 1 and the
 * last is at position getNumberOfEntries().
 *
 * The core operations (add, remove, clear, replace, getEntry, contains,
 * getNumberOfEntries, isEmpty, isFull, getIterator) are adapted from the
 * course sample code by Frank M. Carrano.
 *
 * The additional operations below (getPosition, removeEntry, sort, filter,
 * search, countIf) were added by Wong Chee Yan for the Walk-In Registration
 * module.
 *
 * @author Frank M. Carrano (core operations)
 * @author Wong Chee Yan (added operations)
 * @version 2.0
 */
public interface ListInterface<T> {

  /**
   * Task: Adds a new entry to the end of the list. Entries currently in the
   * list are unaffected. The list's size is increased by 1.
   *
   * @param newEntry the object to be added as a new entry
   * @return true if the addition is successful, or false if the list is full
   */
  public boolean add(T newEntry);

  /**
   * Task: Adds a new entry at a specified position within the list. Entries
   * originally at and above the specified position are at the next higher
   * position within the list. The list's size is increased by 1.
   *
   * @param newPosition an integer that specifies the desired position of the
   * new entry
   * @param newEntry the object to be added as a new entry
   * @return true if the addition is successful, or false if either the list is
   * full, newPosition &lt; 1, or newPosition &gt; getNumberOfEntries()+1
   */
  public boolean add(int newPosition, T newEntry);

  /**
   * Task: Removes the entry at a given position from the list. Entries
   * originally at positions higher than the given position are at the next
   * lower position within the list, and the list's size is decreased by 1.
   *
   * @param givenPosition an integer that indicates the position of the entry to
   * be removed
   * @return a reference to the removed entry or null, if either the list was
   * empty, givenPosition &lt; 1, or givenPosition &gt; getNumberOfEntries()
   */
  public T remove(int givenPosition);

  /**
   * Task: Removes all entries from the list.
   */
  public void clear();

  /**
   * Task: Replaces the entry at a given position in the list.
   *
   * @param givenPosition an integer that indicates the position of the entry to
   * be replaced
   * @param newEntry the object that will replace the entry at the position
   * givenPosition
   * @return true if the replacement occurs, or false if either the list is
   * empty, givenPosition &lt; 1, or givenPosition &gt; getNumberOfEntries()
   */
  public boolean replace(int givenPosition, T newEntry);

  /**
   * Task: Retrieves the entry at a given position in the list.
   *
   * @param givenPosition an integer that indicates the position of the desired
   * entry
   * @return a reference to the indicated entry or null, if either the list is
   * empty, givenPosition &lt; 1, or givenPosition &gt; getNumberOfEntries()
   */
  public T getEntry(int givenPosition);

  /**
   * Task: Sees whether the list contains a given entry.
   *
   * @param anEntry the object that is the desired entry
   * @return true if the list contains anEntry, or false if not
   */
  public boolean contains(T anEntry);

  /**
   * Task: Gets the number of entries in the list.
   *
   * @return the integer number of entries currently in the list
   */
  public int getNumberOfEntries();

  /**
   * Task: Sees whether the list is empty.
   *
   * @return true if the list is empty, or false if not
   */
  public boolean isEmpty();

  /**
   * Task: Sees whether the list is full.
   *
   * @return true if the list is full, or false if not
   */
  public boolean isFull();

  /**
   * Task: Gets an iterator over the entries in the list, in order from the
   * first entry to the last.
   *
   * @return an iterator over the entries in the list
   */
  public Iterator<T> getIterator();

  /**
   * Task: Gets the position of the first entry in the list that equals a given
   * entry.
   *
   * @param anEntry the object that is the desired entry
   * @return the 1-based position of anEntry, or -1 if the list does not
   * contain anEntry
   */
  public int getPosition(T anEntry);

  /**
   * Task: Removes the first entry in the list that equals a given entry.
   * Entries originally at positions higher than the removed entry are at the
   * next lower position within the list, and the list's size is decreased by 1.
   *
   * @param anEntry the object to be removed
   * @return a reference to the removed entry, or null if the list does not
   * contain anEntry
   */
  public T removeEntry(T anEntry);

  /**
   * Task: Arranges the entries in the list into the order defined by a given
   * comparator. The list's size is unchanged.
   *
   * @param comparator defines the order the entries are arranged into
   */
  public void sort(Comparator<? super T> comparator);

  /**
   * Task: Creates a new list holding every entry in this list that satisfies a
   * given condition, in their existing order. This list is unchanged.
   *
   * @param condition the condition an entry must satisfy to be included
   * @return a new list of the entries that satisfy condition, which is empty
   * if no entry satisfies it
   */
  public ListInterface<T> filter(Condition<? super T> condition);

  /**
   * Task: Retrieves the first entry in the list that satisfies a given
   * condition.
   *
   * @param condition the condition an entry must satisfy to be retrieved
   * @return a reference to the first entry that satisfies condition, or null
   * if no entry satisfies it
   */
  public T search(Condition<? super T> condition);

  /**
   * Task: Counts the entries in the list that satisfy a given condition.
   *
   * @param condition the condition an entry must satisfy to be counted
   * @return the integer number of entries that satisfy condition
   */
  public int countIf(Condition<? super T> condition);
}
