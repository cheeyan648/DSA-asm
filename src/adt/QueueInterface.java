package adt;

public interface QueueInterface<T> {

  /**
   * Task: Adds a new entry to the back of the queue.
   *
   * @param newEntry the object to be added
   * @return true if the addition is successful, or false if the queue is full
   */
  public boolean enqueue(T newEntry);

  /**
   * Task: Removes and returns the entry at the front of the queue.
   *
   * @return the front entry, or null if the queue is empty
   */
  public T dequeue();

  /**
   * Task: Retrieves the entry at the front of the queue without removing it.
   *
   * @return the front entry, or null if the queue is empty
   */
  public T getFront();

  /**
   * Task: Sees whether the queue is empty.
   *
   * @return true if the queue is empty, or false if not
   */
  public boolean isEmpty();

  /**
   * Task: Sees whether the queue is full.
   *
   * @return true if the queue is full, or false if not
   */
  public boolean isFull();

  /**
   * Task: Gets the number of entries in the queue.
   *
   * @return the integer number of entries currently in the queue
   */
  public int getNumberOfEntries();
}
