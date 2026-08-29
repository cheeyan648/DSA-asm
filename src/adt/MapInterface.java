package adt;

/**
 * A collection of key-value pairs where each key appears at most once.
 *
 * Written for the lookups the system performs constantly - a room by its
 * number, a guest by their ID - where scanning a list every time would be
 * wasteful.
 *
 * @author Wong Chee Yan
 */
public interface MapInterface<K, V> {

  /**
   * Task: Associates a value with a key, replacing any value already stored
   * against that key.
   *
   * @param key the key to store the value against
   * @param value the value to store
   * @return the value previously stored against key, or null if there was none
   */
  public V put(K key, V value);

  /**
   * Task: Retrieves the value stored against a key.
   *
   * @param key the key to look up
   * @return the value stored against key, or null if the key is not present
   */
  public V get(K key);

  /**
   * Task: Sees whether a key is present.
   *
   * @param key the key to look for
   * @return true if the key has a value stored against it
   */
  public boolean containsKey(K key);

  /**
   * Task: Removes the pair stored against a key.
   *
   * @param key the key to remove
   * @return the value that was removed, or null if the key was not present
   */
  public V remove(K key);

  /**
   * Task: Removes every pair.
   */
  public void clear();

  /**
   * Task: Counts the pairs held.
   *
   * @return the number of key-value pairs
   */
  public int getNumberOfEntries();

  /**
   * Task: Sees whether the map is empty.
   *
   * @return true if the map holds no pairs
   */
  public boolean isEmpty();

  /**
   * Task: Lists every key held, in no guaranteed order.
   *
   * @return a list of the keys
   */
  public ListInterface<K> getKeys();

  /**
   * Task: Lists every value held, in no guaranteed order.
   *
   * @return a list of the values
   */
  public ListInterface<V> getValues();
}
