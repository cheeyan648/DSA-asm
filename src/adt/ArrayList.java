package adt;

/**
 * @author Frank M. Carrano
 * @version 2.0
 */

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayList<T> implements ListInterface<T>, Serializable {

  private T[] array;
  private int numberOfEntries;
  private static final int DEFAULT_CAPACITY = 5;

  public ArrayList() {
    this(DEFAULT_CAPACITY);
  }

  public ArrayList(int initialCapacity) {
    numberOfEntries = 0;
    array = (T[]) new Object[initialCapacity];
  }

  @Override
  public boolean add(T newEntry) {
    if (isArrayFull()) {
      doubleArray();
    }

    array[numberOfEntries] = newEntry;
    numberOfEntries++;
    return true;
  }

  @Override
  public boolean add(int newPosition, T newEntry) {
    boolean isSuccessful = true;

    if ((newPosition >= 1) && (newPosition <= numberOfEntries + 1)) {
      if (isArrayFull()) {
        doubleArray();
      }
      makeRoom(newPosition);
      array[newPosition - 1] = newEntry;
      numberOfEntries++;
    } else {
      isSuccessful = false;
    }

    return isSuccessful;
  }

  @Override
  public T remove(int givenPosition) {
    T result = null;

    if ((givenPosition >= 1) && (givenPosition <= numberOfEntries)) {
      result = array[givenPosition - 1];

      if (givenPosition < numberOfEntries) {
        removeGap(givenPosition);
      }

      numberOfEntries--;
    }

    return result;
  }

  @Override
  public void clear() {
    numberOfEntries = 0;
  }

  @Override
  public boolean replace(int givenPosition, T newEntry) {
    boolean isSuccessful = true;

    if ((givenPosition >= 1) && (givenPosition <= numberOfEntries)) {
      array[givenPosition - 1] = newEntry;
    } else {
      isSuccessful = false;
    }

    return isSuccessful;
  }

  @Override
  public T getEntry(int givenPosition) {
    T result = null;

    if ((givenPosition >= 1) && (givenPosition <= numberOfEntries)) {
      result = array[givenPosition - 1];
    }

    return result;
  }

  @Override
  public boolean contains(T anEntry) {
    boolean found = false;
    for (int index = 0; !found && (index < numberOfEntries); index++) {
      if (anEntry.equals(array[index])) {
        found = true;
      }
    }
    return found;
  }

  @Override
  public int getNumberOfEntries() {
    return numberOfEntries;
  }

  @Override
  public boolean isEmpty() {
    return numberOfEntries == 0;
  }

  @Override
  public boolean isFull() {
    return false;
  }

  @Override
  public Iterator<T> getIterator() {
    return new ArrayListIterator();
  }

  /**
   * Sorts using merge sort, which is stable: entries the comparator considers
   * equal keep the relative order they already had.
   *
   * Merge sort is O(n log n) in every case, unlike the O(n^2) worst case of the
   * simple exchange sorts.
   */
  @Override
  public void sort(Comparator<? super T> comparator) {
    if (comparator == null || numberOfEntries < 2) {
      return;
    }

    T[] workspace = (T[]) new Object[numberOfEntries];
    mergeSort(0, numberOfEntries - 1, workspace, comparator);
  }

  @Override
  public ListInterface<T> filter(Condition<? super T> condition) {
    ListInterface<T> matches = new ArrayList<>();
    if (condition == null) {
      return matches;
    }

    for (int index = 0; index < numberOfEntries; index++) {
      if (condition.isSatisfiedBy(array[index])) {
        matches.add(array[index]);
      }
    }
    return matches;
  }

  @Override
  public T search(Condition<? super T> condition) {
    if (condition == null) {
      return null;
    }

    for (int index = 0; index < numberOfEntries; index++) {
      if (condition.isSatisfiedBy(array[index])) {
        return array[index];
      }
    }
    return null;
  }

  @Override
  public int countIf(Condition<? super T> condition) {
    if (condition == null) {
      return 0;
    }

    int count = 0;
    for (int index = 0; index < numberOfEntries; index++) {
      if (condition.isSatisfiedBy(array[index])) {
        count++;
      }
    }
    return count;
  }

  /**
   * Recursively sorts array[first..last] by splitting it in half, sorting each
   * half, then merging the two sorted halves back together.
   */
  private void mergeSort(int first, int last, T[] workspace,
      Comparator<? super T> comparator) {
    if (first >= last) {
      return;
    }

    // Written this way rather than (first + last) / 2 so a large first + last
    // cannot overflow int.
    int middle = first + (last - first) / 2;

    mergeSort(first, middle, workspace, comparator);
    mergeSort(middle + 1, last, workspace, comparator);
    merge(first, middle, last, workspace, comparator);
  }

  /**
   * Merges the two already-sorted runs array[first..middle] and
   * array[middle+1..last] into one sorted run in array[first..last].
   */
  private void merge(int first, int middle, int last, T[] workspace,
      Comparator<? super T> comparator) {
    int left = first;
    int right = middle + 1;
    int target = first;

    while (left <= middle && right <= last) {
      // <= 0 keeps the left run's entry first when the two tie, which is what
      // makes this sort stable.
      if (comparator.compare(array[left], array[right]) <= 0) {
        workspace[target] = array[left];
        left++;
      } else {
        workspace[target] = array[right];
        right++;
      }
      target++;
    }

    while (left <= middle) {
      workspace[target] = array[left];
      left++;
      target++;
    }

    while (right <= last) {
      workspace[target] = array[right];
      right++;
      target++;
    }

    for (int index = first; index <= last; index++) {
      array[index] = workspace[index];
    }
  }

  private class ArrayListIterator implements Iterator<T> {
    private int currentIndex = 0;

    @Override
    public boolean hasNext() {
      return currentIndex < numberOfEntries;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T nextEntry = array[currentIndex];
      currentIndex++;
      return nextEntry;
    }
  }

  private void doubleArray() {
    T[] oldArray = array;
    array = (T[]) new Object[oldArray.length * 2];
    for (int i = 0; i < oldArray.length; i++) {
      array[i] = oldArray[i];
    }
  }

  private boolean isArrayFull() {
    return numberOfEntries == array.length;
  }

  @Override
  public String toString() {
    String outputStr = "";
    for (int index = 0; index < numberOfEntries; ++index) {
      outputStr += array[index] + "\n";
    }

    return outputStr;
  }

  /**
   * Task: Makes room for a new entry at newPosition. Precondition: 1 <=
   * newPosition <= numberOfEntries + 1; numberOfEntries is array's
   * numberOfEntries before addition.
   */
  private void makeRoom(int newPosition) {
    int newIndex = newPosition - 1;
    int lastIndex = numberOfEntries - 1;

    // move each entry to next higher index, starting at end of
    // array and continuing until the entry at newIndex is moved
    for (int index = lastIndex; index >= newIndex; index--) {
      array[index + 1] = array[index];
    }
  }

  /**
   * Task: Shifts entries that are beyond the entry to be removed to the next
   * lower position. Precondition: array is not empty; 1 <= givenPosition <
   * numberOfEntries; numberOfEntries is array's numberOfEntries before removal.
   */
  private void removeGap(int givenPosition) {
    // move each entry to next lower position starting at entry after the
    // one removed and continuing until end of array
    int removedIndex = givenPosition - 1;
    int lastIndex = numberOfEntries - 1;

    for (int index = removedIndex; index < lastIndex; index++) {
      array[index] = array[index + 1];
    }
  }
}
