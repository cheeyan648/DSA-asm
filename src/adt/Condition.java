package adt;

/**
 * A test that an entry either satisfies or does not satisfy.
 *
 * Written for the filter/search/countIf operations of ListInterface so that
 * those operations can express "which entries" without the collection needing
 * to know anything about the entries it holds. Declared here (rather than
 * reusing a predefined Java functional interface) to keep the collection ADT
 * self-contained.
 *
 * @author Wong Chee Yan
 */
@FunctionalInterface
public interface Condition<T> {

  /**
   * Task: Tests whether a given entry satisfies this condition.
   *
   * @param entry the object to be tested
   * @return true if entry satisfies the condition, or false if not
   */
  public boolean isSatisfiedBy(T entry);
}
