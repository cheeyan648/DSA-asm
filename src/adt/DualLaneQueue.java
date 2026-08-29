package adt;

import java.io.Serializable;

/**
 * Two FIFO queues behind one interface: an urgent lane and a normal lane.
 *
 * The whole of the urgent lane is served before any of the normal lane, and
 * inside each lane the order is strictly the order entries arrived. A long
 * wait never promotes a normal entry into the urgent lane - only the priority
 * passed to enqueue decides the lane, so an override is always a deliberate,
 * recorded decision rather than something the queue does on its own.
 *
 * This one class serves all three modules that need a waiting line: the
 * walk-in waiting list, the booking handling lanes and the cleaning queue.
 *
 * @author Wong Chee Yan
 */
public class DualLaneQueue<T> implements DualLaneQueueInterface<T>, Serializable {

  private static final long serialVersionUID = 2L;

  private QueueInterface<T> urgentLane;
  private QueueInterface<T> normalLane;

  public DualLaneQueue() {
    urgentLane = new ArrayQueue<>();
    normalLane = new ArrayQueue<>();
  }

  /**
   * Sees whether a priority names the urgent lane.
   *
   * Anything that is not exactly URGENT is treated as normal, so a missing or
   * misspelled priority can never silently grant a queue jump.
   */
  private boolean isUrgent(String priority) {
    return URGENT.equalsIgnoreCase(priority);
  }

  @Override
  public boolean enqueue(T newEntry, String priority) {
    if (newEntry == null) {
      return false;
    }
    return isUrgent(priority) ? urgentLane.enqueue(newEntry) : normalLane.enqueue(newEntry);
  }

  @Override
  public T next() {
    if (!urgentLane.isEmpty()) {
      return urgentLane.dequeue();
    }
    return normalLane.dequeue();
  }

  @Override
  public T peekNext() {
    if (!urgentLane.isEmpty()) {
      return urgentLane.getFront();
    }
    return normalLane.getFront();
  }

  /**
   * Removes an entry from whichever lane holds it.
   *
   * A queue has no random-access removal, so the lane is drained into a
   * temporary queue and refilled without the unwanted entry. That keeps the
   * surviving entries in their original order, which matters because their
   * position in the lane is what decides who is served next.
   */
  @Override
  public boolean removeEntry(T anEntry) {
    if (anEntry == null) {
      return false;
    }
    if (removeFromLane(urgentLane, anEntry)) {
      return true;
    }
    return removeFromLane(normalLane, anEntry);
  }

  private boolean removeFromLane(QueueInterface<T> lane, T anEntry) {
    boolean removed = false;
    QueueInterface<T> kept = new ArrayQueue<>();

    while (!lane.isEmpty()) {
      T entry = lane.dequeue();
      // Only the first match is dropped, so duplicates behave the same way
      // ListInterface.removeEntry does.
      if (!removed && anEntry.equals(entry)) {
        removed = true;
      } else {
        kept.enqueue(entry);
      }
    }

    while (!kept.isEmpty()) {
      lane.enqueue(kept.dequeue());
    }
    return removed;
  }

  @Override
  public boolean isEmpty() {
    return urgentLane.isEmpty() && normalLane.isEmpty();
  }

  @Override
  public void clear() {
    while (!urgentLane.isEmpty()) {
      urgentLane.dequeue();
    }
    while (!normalLane.isEmpty()) {
      normalLane.dequeue();
    }
  }

  @Override
  public int getUrgentCount() {
    return urgentLane.getNumberOfEntries();
  }

  @Override
  public int getNormalCount() {
    return normalLane.getNumberOfEntries();
  }

  @Override
  public int getNumberOfEntries() {
    return getUrgentCount() + getNormalCount();
  }

  /**
   * Lists the waiting entries in service order without disturbing the lanes.
   *
   * Each lane is drained into a list and immediately refilled in the same
   * order, so the queue is left exactly as it was found.
   */
  @Override
  public ListInterface<T> toServiceOrder() {
    ListInterface<T> order = new ArrayList<>();
    copyLaneInto(urgentLane, order);
    copyLaneInto(normalLane, order);
    return order;
  }

  private void copyLaneInto(QueueInterface<T> lane, ListInterface<T> order) {
    QueueInterface<T> kept = new ArrayQueue<>();

    while (!lane.isEmpty()) {
      T entry = lane.dequeue();
      order.add(entry);
      kept.enqueue(entry);
    }
    while (!kept.isEmpty()) {
      lane.enqueue(kept.dequeue());
    }
  }

  @Override
  public String toString() {
    return String.format("DualLaneQueue[urgent=%d, normal=%d]",
        getUrgentCount(), getNormalCount());
  }
}
