package adt;

import java.io.Serializable;

/**
 * A hash table using separate chaining, so lookups do not depend on how many
 * entries the table holds.
 *
 * Collisions are handled by chaining: every bucket holds a linked chain of the
 * entries whose keys hashed to it. When the table grows past its load factor
 * it is rehashed into a larger array, which keeps the chains short and the
 * lookups close to constant time.
 *
 * @author Tan Chee Yan
 */
public class HashMap<K, V> implements MapInterface<K, V>, Serializable {

  private static final long serialVersionUID = 2L;

  private static final int DEFAULT_CAPACITY = 31;
  private static final double MAX_LOAD_FACTOR = 0.75;

  /** One key-value pair, and a link to the next pair in the same bucket. */
  private static class Node<K, V> implements Serializable {
    private static final long serialVersionUID = 2L;
    private K key;
    private V value;
    private Node<K, V> next;

    private Node(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }

  private Node<K, V>[] buckets;
  private int numberOfEntries;

  @SuppressWarnings("unchecked")
  public HashMap() {
    buckets = new Node[DEFAULT_CAPACITY];
    numberOfEntries = 0;
  }

  /**
   * Works out which bucket a key belongs in.
   *
   * The sign is stripped because hashCode may be negative, and Math.abs alone
   * would still return a negative for Integer.MIN_VALUE.
   */
  private int bucketFor(K key, int capacity) {
    return (key.hashCode() & 0x7fffffff) % capacity;
  }

  @Override
  public V put(K key, V value) {
    if (key == null) {
      return null;
    }

    int index = bucketFor(key, buckets.length);
    for (Node<K, V> node = buckets[index]; node != null; node = node.next) {
      if (key.equals(node.key)) {
        V previous = node.value;
        node.value = value;
        return previous;
      }
    }

    // Not present - add to the front of the chain, which is O(1) and avoids
    // walking to the end a second time.
    Node<K, V> added = new Node<>(key, value);
    added.next = buckets[index];
    buckets[index] = added;
    numberOfEntries++;

    if ((double) numberOfEntries / buckets.length > MAX_LOAD_FACTOR) {
      rehash();
    }
    return null;
  }

  /**
   * Doubles the table and redistributes every entry.
   *
   * Bucket positions depend on the table size, so entries cannot simply be
   * copied across - each one is hashed again against the new capacity.
   */
  @SuppressWarnings("unchecked")
  private void rehash() {
    Node<K, V>[] old = buckets;
    int newCapacity = old.length * 2 + 1;
    buckets = new Node[newCapacity];

    for (Node<K, V> head : old) {
      for (Node<K, V> node = head; node != null; ) {
        Node<K, V> nextNode = node.next;
        int index = bucketFor(node.key, newCapacity);
        node.next = buckets[index];
        buckets[index] = node;
        node = nextNode;
      }
    }
  }

  @Override
  public V get(K key) {
    if (key == null) {
      return null;
    }
    for (Node<K, V> node = buckets[bucketFor(key, buckets.length)]; node != null; node = node.next) {
      if (key.equals(node.key)) {
        return node.value;
      }
    }
    return null;
  }

  @Override
  public boolean containsKey(K key) {
    return get(key) != null;
  }

  @Override
  public V remove(K key) {
    if (key == null) {
      return null;
    }

    int index = bucketFor(key, buckets.length);
    Node<K, V> previous = null;

    for (Node<K, V> node = buckets[index]; node != null; node = node.next) {
      if (key.equals(node.key)) {
        if (previous == null) {
          buckets[index] = node.next;
        } else {
          previous.next = node.next;
        }
        numberOfEntries--;
        return node.value;
      }
      previous = node;
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void clear() {
    buckets = new Node[DEFAULT_CAPACITY];
    numberOfEntries = 0;
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
  public ListInterface<K> getKeys() {
    ListInterface<K> keys = new ArrayList<>();
    for (Node<K, V> head : buckets) {
      for (Node<K, V> node = head; node != null; node = node.next) {
        keys.add(node.key);
      }
    }
    return keys;
  }

  @Override
  public ListInterface<V> getValues() {
    ListInterface<V> values = new ArrayList<>();
    for (Node<K, V> head : buckets) {
      for (Node<K, V> node = head; node != null; node = node.next) {
        values.add(node.value);
      }
    }
    return values;
  }

  @Override
  public String toString() {
    return String.format("HashMap[entries=%d, capacity=%d]", numberOfEntries, buckets.length);
  }
}
