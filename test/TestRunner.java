package test;

/**
 * A small test harness, written by hand.
 *
 * JUnit is not available in the NetBeans project this assignment is built as,
 * so the checks are made with plain assertions and counted here. It reports
 * what passed and what failed, and shows the expected and actual values for
 * anything that fails - which is what makes a failure diagnosable rather than
 * merely visible.
 *
 * @author Tan Chee Yan
 */
public class TestRunner {

  private int passed;
  private int failed;
  private String currentSuite = "";
  private final StringBuilder failures = new StringBuilder();

  /** Starts a new group of checks. */
  public void suite(String name) {
    currentSuite = name;
    System.out.println();
    System.out.println("  " + name);
    System.out.println("  " + "-".repeat(Math.min(70, name.length() + 20)));
  }

  /** Checks that something is true. */
  public void check(String what, boolean condition) {
    if (condition) {
      passed++;
      System.out.printf("    [PASS]  %s%n", what);
    } else {
      failed++;
      System.out.printf("    [FAIL]  %s%n", what);
      failures.append(String.format("      %s / %s%n", currentSuite, what));
    }
  }

  /** Checks two values are equal, showing both when they are not. */
  public void checkEquals(String what, Object expected, Object actual) {
    boolean same = (expected == null) ? actual == null : expected.equals(actual);

    if (same) {
      passed++;
      System.out.printf("    [PASS]  %s%n", what);
    } else {
      failed++;
      System.out.printf("    [FAIL]  %s%n", what);
      System.out.printf("            expected: %s%n", expected);
      System.out.printf("            actual:   %s%n", actual);
      failures.append(String.format("      %s / %s  (expected %s, got %s)%n",
          currentSuite, what, expected, actual));
    }
  }

  /** Checks two amounts of money are equal to the sen. */
  public void checkAmount(String what, double expected, double actual) {
    boolean same = Math.abs(expected - actual) < 0.005;

    if (same) {
      passed++;
      System.out.printf("    [PASS]  %s%n", what);
    } else {
      failed++;
      System.out.printf("    [FAIL]  %s%n", what);
      System.out.printf("            expected: %.2f%n", expected);
      System.out.printf("            actual:   %.2f%n", actual);
      failures.append(String.format("      %s / %s  (expected %.2f, got %.2f)%n",
          currentSuite, what, expected, actual));
    }
  }

  /** Prints the totals and says whether everything passed. */
  public boolean report(String title) {
    System.out.println();
    System.out.println("=".repeat(76));
    System.out.printf("  %s%n", title);
    System.out.println("=".repeat(76));
    System.out.printf("  Passed: %d%n", passed);
    System.out.printf("  Failed: %d%n", failed);

    if (failed > 0) {
      System.out.println();
      System.out.println("  Failures:");
      System.out.print(failures);
    } else {
      System.out.println();
      System.out.println("  Everything passed.");
    }
    System.out.println("=".repeat(76));

    return failed == 0;
  }

  public int getFailed() {
    return failed;
  }

  public int getPassed() {
    return passed;
  }
}
