package adt;

/**
 * A search tree of key-value pairs kept in key order.
 *
 * Chosen where a collection is both looked up by key and listed in order:
 * the ordering is a property of the structure, so a sorted listing costs
 * nothing beyond walking the tree.
 *
 * @author Tan Chee Yan
 */
public interface TreeInterface<K extends Comparable<K>, V> {

  /**
   * Task: Adds a key-value pair, replacing the value if the key is present.
   *
   * @param key the key to store against
   * @param value the value to store
   * @return the value previously stored against key, or null if there was none
   */
  public V add(K key, V value);

  /**
   * Task: Retrieves the value stored against a key.
   *
   * @param key the key to look up
   * @return the value stored against key, or null if the key is not present
   */
  public V search(K key);

  /**
   * Task: Sees whether a key is present.
   *
   * @param key the key to look for
   * @return true if the tree holds that key
   */
  public boolean contains(K key);

  /**
   * Task: Removes the pair stored against a key.
   *
   * @param key the key to remove
   * @return the value removed, or null if the key was not present
   */
  public V remove(K key);

  /**
   * Task: Removes every pair.
   */
  public void clear();

  /**
   * Task: Counts the pairs held.
   *
   * @return the number of pairs
   */
  public int getNumberOfEntries();

  /**
   * Task: Sees whether the tree is empty.
   *
   * @return true if the tree holds no pairs
   */
  public boolean isEmpty();

  /**
   * Task: Lists every value in ascending key order.
   *
   * @return the values, sorted by key
   */
  public ListInterface<V> getAllInOrder();

  /**
   * Task: Lists every key in ascending order.
   *
   * @return the keys, sorted
   */
  public ListInterface<K> getKeysInOrder();
}
