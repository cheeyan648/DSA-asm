package adt;

import java.io.Serializable;

public class ArrayQueue<T> implements QueueInterface<T>, Serializable {

  private T[] array;
  private int front;
  private int numberOfEntries;
  private static final int DEFAULT_CAPACITY = 5;

  public ArrayQueue() {
    this(DEFAULT_CAPACITY);
  }

  public ArrayQueue(int initialCapacity) {
    front = 0;
    numberOfEntries = 0;
    array = (T[]) new Object[initialCapacity];
  }

  @Override
  public boolean enqueue(T newEntry) {
    if (isArrayFull()) {
      doubleArray();
    }

    int back = (front + numberOfEntries) % array.length;
    array[back] = newEntry;
    numberOfEntries++;
    return true;
  }

  @Override
  public T dequeue() {
    if (isEmpty()) {
      return null;
    }

    T result = array[front];
    array[front] = null;
    front = (front + 1) % array.length;
    numberOfEntries--;
    return result;
  }

  @Override
  public T getFront() {
    if (isEmpty()) {
      return null;
    }
    return array[front];
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
      array[i] = oldArray[(front + i) % oldArray.length];
    }
    front = 0;
  }
}
