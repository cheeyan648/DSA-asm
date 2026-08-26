package adt;

import java.io.Serializable;

/**
 * A binary search tree of key-value pairs.
 *
 * Every key in a node's left subtree sorts before that node's key, and every
 * key in its right subtree sorts after. That single rule gives both of the
 * things this system needs from it: a lookup discards half the remaining tree
 * at each step, and an in-order walk produces the entries already sorted, so
 * listing bookings by ID or members by ID needs no sort at all.
 *
 * Generalised from the module-specific BookingBST so the same structure serves
 * bookings, members and any other keyed entity.
 *
 * @author Tan Chee Yan
 */
public class BinarySearchTree<K extends Comparable<K>, V>
    implements TreeInterface<K, V>, Serializable {

  private static final long serialVersionUID = 2L;

  private static class Node<K, V> implements Serializable {
    private static final long serialVersionUID = 2L;
    private K key;
    private V value;
    private Node<K, V> left;
    private Node<K, V> right;

    private Node(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }

  private Node<K, V> root;
  private int numberOfEntries;

  /**
   * Holds the displaced value when add() replaces an existing key.
   *
   * The recursive insert returns the subtree it rebuilt, so it cannot also
   * return the old value; this field carries it back to the caller instead.
   */
  private V replacedValue;

  public BinarySearchTree() {
    root = null;
    numberOfEntries = 0;
  }

  @Override
  public V add(K key, V value) {
    if (key == null) {
      return null;
    }
    replacedValue = null;
    root = insert(root, key, value);
    return replacedValue;
  }

  private Node<K, V> insert(Node<K, V> node, K key, V value) {
    if (node == null) {
      numberOfEntries++;
      return new Node<>(key, value);
    }

    int comparison = key.compareTo(node.key);
    if (comparison < 0) {
      node.left = insert(node.left, key, value);
    } else if (comparison > 0) {
      node.right = insert(node.right, key, value);
    } else {
      replacedValue = node.value;
      node.value = value;
    }
    return node;
  }

  @Override
  public V search(K key) {
    Node<K, V> node = findNode(key);
    return (node == null) ? null : node.value;
  }

  /**
   * Walks down to the node holding a key.
   *
   * Written as a loop rather than a recursion: the descent needs no state from
   * the levels above it, so there is nothing for a call stack to carry.
   */
  private Node<K, V> findNode(K key) {
    if (key == null) {
      return null;
    }

    Node<K, V> node = root;
    while (node != null) {
      int comparison = key.compareTo(node.key);
      if (comparison == 0) {
        return node;
      }
      node = (comparison < 0) ? node.left : node.right;
    }
    return null;
  }

  @Override
  public boolean contains(K key) {
    return findNode(key) != null;
  }

  @Override
  public V remove(K key) {
    if (key == null) {
      return null;
    }
    V removed = search(key);
    if (removed != null) {
      root = delete(root, key);
      numberOfEntries--;
    }
    return removed;
  }

  private Node<K, V> delete(Node<K, V> node, K key) {
    if (node == null) {
      return null;
    }

    int comparison = key.compareTo(node.key);
    if (comparison < 0) {
      node.left = delete(node.left, key);
      return node;
    }
    if (comparison > 0) {
      node.right = delete(node.right, key);
      return node;
    }

    // Found it. With at most one child the node is simply replaced by that
    // child.
    if (node.left == null) {
      return node.right;
    }
    if (node.right == null) {
      return node.left;
    }

    // Two children: the smallest key in the right subtree is the only value
    // that can take this node's place and keep every key ordered, since it
    // sorts after everything on the left and before everything else on the
    // right. It is moved up and its old position deleted.
    Node<K, V> successor = node.right;
    while (successor.left != null) {
      successor = successor.left;
    }
    node.key = successor.key;
    node.value = successor.value;
    node.right = delete(node.right, successor.key);
    return node;
  }

  @Override
  public void clear() {
    root = null;
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
  public ListInterface<V> getAllInOrder() {
    ListInterface<V> values = new ArrayList<>();
    collectValues(root, values);
    return values;
  }

  /**
   * Visits left subtree, then node, then right subtree - which by the ordering
   * rule reaches the keys in ascending order.
   */
  private void collectValues(Node<K, V> node, ListInterface<V> values) {
    if (node == null) {
      return;
    }
    collectValues(node.left, values);
    values.add(node.value);
    collectValues(node.right, values);
  }

  @Override
  public ListInterface<K> getKeysInOrder() {
    ListInterface<K> keys = new ArrayList<>();
    collectKeys(root, keys);
    return keys;
  }

  private void collectKeys(Node<K, V> node, ListInterface<K> keys) {
    if (node == null) {
      return;
    }
    collectKeys(node.left, keys);
    keys.add(node.key);
    collectKeys(node.right, keys);
  }

  @Override
  public String toString() {
    return String.format("BinarySearchTree[entries=%d]", numberOfEntries);
  }
}
