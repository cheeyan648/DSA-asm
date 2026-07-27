package adt;

/**
 *
 * @author Kat Tan
 */
public interface StackInterface<T> {

  /**
   * Task: Adds a new entry to the top of the stack.
   *
   * @param newEntry the object to be added
   * @return true if the addition is successful, or false if the stack is full
   */
  public boolean push(T newEntry);

  /**
   * Task: Removes and returns the entry at the top of the stack.
   *
   * @return the top entry, or null if the stack is empty
   */
  public T pop();

  /**
   * Task: Retrieves the entry at the top of the stack without removing it.
   *
   * @return the top entry, or null if the stack is empty
   */
  public T peek();

  /**
   * Task: Sees whether the stack is empty.
   *
   * @return true if the stack is empty, or false if not
   */
  public boolean isEmpty();

  /**
   * Task: Sees whether the stack is full.
   *
   * @return true if the stack is full, or false if not
   */
  public boolean isFull();

  /**
   * Task: Gets the number of entries in the stack.
   *
   * @return the integer number of entries currently in the stack
   */
  public int getNumberOfEntries();
}
