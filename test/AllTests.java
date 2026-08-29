package test;

/**
 * Runs every suite in order: units first, then integration, then the whole
 * system.
 *
 * The order is deliberate. A broken ADT fails its own unit test rather than
 * surfacing as a confusing system-level failure ten steps later, so the first
 * thing that fails is usually the thing that is actually wrong.
 *
 * @author Tan Chee Yan
 */
public class AllTests {

  public static void main(String[] args) {
    System.out.println();
    System.out.println("#".repeat(76));
    System.out.println("#  TARUMT RESORT MANAGEMENT SYSTEM - FULL TEST RUN");
    System.out.println("#".repeat(76));

    boolean units = new UnitTest().runAll();
    boolean validation = new ValidationTest().runAll();
    boolean integration = new IntegrationTest().runAll();
    boolean system = new SystemTest().runAll();
    boolean workflow = new WorkflowTest().runAll();

    System.out.println();
    System.out.println("#".repeat(76));
    System.out.println("#  OVERALL");
    System.out.println("#".repeat(76));
    System.out.printf("#    Unit tests         %s%n", units ? "PASSED" : "FAILED");
    System.out.printf("#    Validation tests   %s%n", validation ? "PASSED" : "FAILED");
    System.out.printf("#    Integration tests  %s%n", integration ? "PASSED" : "FAILED");
    System.out.printf("#    System tests       %s%n", system ? "PASSED" : "FAILED");
    System.out.printf("#    Workflow tests     %s%n", workflow ? "PASSED" : "FAILED");
    System.out.println("#".repeat(76));

    boolean allPassed = units && validation && integration && system && workflow;
    System.out.println(allPassed
        ? "#  Everything passed."
        : "#  Something failed - see the summaries above.");
    System.out.println("#".repeat(76));
    System.out.println();

    System.exit(allPassed ? 0 : 1);
  }
}
