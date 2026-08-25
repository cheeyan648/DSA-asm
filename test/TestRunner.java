/**
 * Shared assertion helpers for the test suite.
 *
 * Deliberately tiny and dependency-free - JUnit is not available and the
 * assignment forbids external libraries, so the suite is a plain Java program
 * that reports pass/fail counts and exits non-zero if anything fails.
 *
 * @author Tan Chee Yan
 */
public class TestRunner {

  private static int run = 0;
  private static int passed = 0;
  private static int sectionRun = 0;
  private static int sectionPassed = 0;

  public static void suite(String title) {
    System.out.println();
    System.out.println("=".repeat(78));
    System.out.println("  " + title);
    System.out.println("=".repeat(78));
  }

  public static void section(String title) {
    sectionRun = 0;
    sectionPassed = 0;
    System.out.println();
    System.out.println(title);
    System.out.println("-".repeat(78));
  }

  public static void check(String description, boolean condition) {
    run++;
    sectionRun++;
    if (condition) {
      passed++;
      sectionPassed++;
      System.out.printf("  [PASS] %s%n", description);
    } else {
      System.out.printf("  [FAIL] %s%n", description);
    }
  }

  public static void checkEquals(String description, Object expected, Object actual) {
    boolean same = (expected == null) ? actual == null : expected.equals(actual);
    run++;
    sectionRun++;
    if (same) {
      passed++;
      sectionPassed++;
      System.out.printf("  [PASS] %s%n", description);
    } else {
      System.out.printf("  [FAIL] %s%n         expected <%s> but was <%s>%n",
          description, expected, actual);
    }
  }

  public static int getRun() {
    return run;
  }

  public static int getPassed() {
    return passed;
  }

  public static void report(String label) {
    System.out.println();
    System.out.println("=".repeat(78));
    System.out.printf("  %s: %d of %d passed%s%n", label, passed, run,
        passed == run ? "" : "   *** " + (run - passed) + " FAILED ***");
    System.out.println("=".repeat(78));
  }

  public static void exitWithStatus() {
    System.exit(passed == run ? 0 : 1);
  }
}
