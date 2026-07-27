package adt;

import java.io.Serializable;

/**
 *
 * @author Kat Tan
 */
public class ArrayStack<T> implements StackInterface<T>, Serializable {

  private T[] array;
  private int numberOfEntries;
  private static final int DEFAULT_CAPACITY = 5;

  public ArrayStack() {
    this(DEFAULT_CAPACITY);
  }

  public ArrayStack(int initialCapacity) {
    numberOfEntries = 0;
    array = (T[]) new Object[initialCapacity];
  }

  @Override
  public boolean push(T newEntry) {
    if (isArrayFull()) {
      doubleArray();
    }

    array[numberOfEntries] = newEntry;
    numberOfEntries++;
    return true;
  }

  @Override
  public T pop() {
    if (isEmpty()) {
      return null;
    }

    numberOfEntries--;
    T result = array[numberOfEntries];
    array[numberOfEntries] = null;
    return result;
  }

  @Override
  public T peek() {
    if (isEmpty()) {
      return null;
    }
    return array[numberOfEntries - 1];
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
  public int getNumberOfEntries() {
    return numberOfEntries;
  }

  private boolean isArrayFull() {
    return numberOfEntries == array.length;
  }

  private void doubleArray() {
    T[] oldArray = array;
    array = (T[]) new Object[oldArray.length * 2];
    for (int i = 0; i < numberOfEntries; i++) {
      array[i] = oldArray[i];
    }
  }
}
