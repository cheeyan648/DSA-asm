package test;

import entity.WalkInRegistration;
import java.time.LocalDateTime;
import utility.MessageUI;

/**
 * Validation and defect tests - the rules that decide whether input is
 * accepted, and the queue history that records both ends of a guest's wait.
 *
 * These are the checks that would otherwise only ever be made by a person
 * typing at the console. A contact number that accepts letters, or an ID
 * prompt that silently rebuilds the wrong record, fails here rather than in
 * front of a guest at the counter.
 *
 * @author Tan Chee Yan
 */
public class ValidationTest {

  private final TestRunner runner = new TestRunner();

  public static void main(String[] args) {
    ValidationTest tests = new ValidationTest();
    System.exit(tests.runAll() ? 0 : 1);
  }

  public boolean runAll() {
    System.out.println();
    System.out.println("=".repeat(76));
    System.out.println("  VALIDATION TESTS - input rules and queue history");
    System.out.println("=".repeat(76));

    testPhoneValidation();
    testEmailValidation();
    testNameValidation();
    testDigitHelpers();
    testCancelKey();
    testGmailCases();
    testQueueHistory();
    testWaitingSentence();

    return runner.report("VALIDATION TEST SUMMARY");
  }

  // ==================================================================
  // CONTACT NUMBER
  //
  // The rule is 10 or 11 digits beginning with 0. Each case below is one
  // either side of a boundary, because that is where a length check goes
  // wrong rather than in the middle of the range.
  // ==================================================================

  private void testPhoneValidation() {
    runner.suite("Contact number - what is accepted");

    runner.check("a 10-digit mobile is valid", MessageUI.isValidPhone("0123456789"));
    runner.check("an 11-digit mobile is valid", MessageUI.isValidPhone("01234567890"));
    runner.check("a landline starting 03 is valid", MessageUI.isValidPhone("0312345678"));
    runner.check("dashes are allowed while typing",
        MessageUI.isValidPhone("012-345 6789"));
    runner.check("and so are spaces", MessageUI.isValidPhone("012 345 6789"));

    runner.suite("Contact number - what is refused");

    // The defect this was written for: letters were accepted and stored.
    runner.check("letters are refused", !MessageUI.isValidPhone("abcdefghij"));
    runner.check("a number with a letter in it is refused",
        !MessageUI.isValidPhone("01234a6789"));
    runner.check("a name is refused", !MessageUI.isValidPhone("Ali bin Ahmad"));

    runner.check("9 digits is too short", !MessageUI.isValidPhone("012345678"));
    runner.check("12 digits is too long", !MessageUI.isValidPhone("012345678901"));
    runner.check("not starting with 0 is refused",
        !MessageUI.isValidPhone("1123456789"));
    runner.check("empty is refused", !MessageUI.isValidPhone(""));
    runner.check("null is refused", !MessageUI.isValidPhone(null));
    runner.check("only separators is refused", !MessageUI.isValidPhone("---- ----"));
    runner.check("a plus-prefixed international number is refused",
        !MessageUI.isValidPhone("+60123456789"));
  }

  // ==================================================================
  // EMAIL
  // ==================================================================

  private void testEmailValidation() {
    runner.suite("Email - what is accepted");

    runner.check("an ordinary address is valid",
        MessageUI.isValidEmail("guest@example.com"));
    runner.check("a subdomain is valid",
        MessageUI.isValidEmail("guest@mail.example.com"));
    runner.check("dots in the name are valid",
        MessageUI.isValidEmail("chee.yan@tarumt.edu.my"));

    runner.suite("Email - what is refused");

    runner.check("no at sign is refused", !MessageUI.isValidEmail("guest.example.com"));
    runner.check("two at signs are refused", !MessageUI.isValidEmail("a@b@example.com"));
    runner.check("nothing before the at is refused",
        !MessageUI.isValidEmail("@example.com"));
    runner.check("nothing after the at is refused", !MessageUI.isValidEmail("guest@"));
    runner.check("no dot in the domain is refused",
        !MessageUI.isValidEmail("guest@example"));
    runner.check("a trailing dot is refused", !MessageUI.isValidEmail("guest@example."));
    runner.check("a space is refused", !MessageUI.isValidEmail("gu est@example.com"));
    runner.check("empty is refused", !MessageUI.isValidEmail(""));
    runner.check("null is refused", !MessageUI.isValidEmail(null));
  }

  // ==================================================================
  // NAME
  // ==================================================================

  private void testNameValidation() {
    runner.suite("Guest name - what is accepted");

    runner.check("a plain name is valid", MessageUI.isNameLike("Tan Chee Yan"));
    runner.check("an apostrophe is allowed", MessageUI.isNameLike("O'Brien"));
    runner.check("a hyphen is allowed", MessageUI.isNameLike("Nurul-Ain"));
    runner.check("bin and a dot are allowed", MessageUI.isNameLike("Ali b. Ahmad"));
    runner.check("a slash for a compound name is allowed",
        MessageUI.isNameLike("Muthu a/l Samy"));

    runner.suite("Guest name - what is refused");

    runner.check("digits are refused", !MessageUI.isNameLike("Guest 123"));
    runner.check("a phone number is refused", !MessageUI.isNameLike("0123456789"));
    runner.check("blank is refused", !MessageUI.isNameLike("   "));
    runner.check("empty is refused", !MessageUI.isNameLike(""));
    runner.check("null is refused", !MessageUI.isNameLike(null));
    runner.check("punctuation only is refused", !MessageUI.isNameLike("---"));
  }

  private void testDigitHelpers() {
    runner.suite("Digit and alphanumeric helpers");

    runner.check("digits only is true", MessageUI.isAllDigits("0003"));
    runner.check("a letter makes it false", !MessageUI.isAllDigits("00a3"));
    runner.check("empty is false", !MessageUI.isAllDigits(""));
    runner.check("null is false", !MessageUI.isAllDigits(null));

    runner.check("an IC of digits is alphanumeric",
        MessageUI.isLettersOrDigits("040815071234"));
    runner.check("a passport of letters and digits is too",
        MessageUI.isLettersOrDigits("A12345678"));
    runner.check("a slash is not", !MessageUI.isLettersOrDigits("A123/456"));
    runner.check("empty is not", !MessageUI.isLettersOrDigits(""));
  }

  // ==================================================================
  // CANCEL KEY
  //
  // "0" backs out of every prompt. Somebody leaving a nested screen often
  // sends a run of them, and reading "000" as a bad entry would be the
  // opposite of what they asked for.
  // ==================================================================

  private void testCancelKey() {
    runner.suite("Zero cancels, however many are typed");

    runner.check("a single zero cancels", MessageUI.isCancelKey("0"));
    runner.check("two zeros cancel", MessageUI.isCancelKey("00"));
    runner.check("four zeros cancel", MessageUI.isCancelKey("0000"));
    runner.check("a long run of zeros cancels", MessageUI.isCancelKey("000000000"));

    runner.suite("What is not a cancel");

    runner.check("empty is not a cancel", !MessageUI.isCancelKey(""));
    runner.check("null is not a cancel", !MessageUI.isCancelKey(null));
    runner.check("a real option is not a cancel", !MessageUI.isCancelKey("1"));
    runner.check("a number ending in zero is not a cancel",
        !MessageUI.isCancelKey("10"));
    runner.check("a number starting with zero is not a cancel",
        !MessageUI.isCancelKey("01"));
    runner.check("letters are not a cancel", !MessageUI.isCancelKey("oo"));
  }

  // ==================================================================
  // EMAIL - THE CASES ASKED FOR
  // ==================================================================

  private void testGmailCases() {
    runner.suite("Email - a bare name or number is refused");

    // The defect: a guest name or a few digits typed into the email field was
    // stored as though it were an address.
    runner.check("123 alone is refused", !MessageUI.isValidEmail("123"));
    runner.check("a name alone is refused", !MessageUI.isValidEmail("ali"));
    runner.check("a longer name alone is refused",
        !MessageUI.isValidEmail("Sarah Lim"));
    runner.check("a gmail name without the domain is refused",
        !MessageUI.isValidEmail("sarahlim"));

    runner.suite("Email - short but valid addresses still pass");

    runner.check("123@gmail.co is accepted", MessageUI.isValidEmail("123@gmail.co"));
    runner.check("123@gmail.com is accepted", MessageUI.isValidEmail("123@gmail.com"));
    runner.check("a one-letter domain label is accepted",
        MessageUI.isValidEmail("a@b.c"));

    runner.suite("Email - malformed domains are refused");

    runner.check("no dot in the domain is refused",
        !MessageUI.isValidEmail("guest@gmail"));
    runner.check("a doubled dot is refused",
        !MessageUI.isValidEmail("guest@gmail..com"));
    runner.check("a domain starting on a dot is refused",
        !MessageUI.isValidEmail("guest@.com"));
    runner.check("a domain ending on a dot is refused",
        !MessageUI.isValidEmail("guest@gmail."));
  }

  // ==================================================================
  // QUEUE HISTORY
  //
  // A registration is stored at both ends of the wait: once when the guest
  // joins the queue, and once when they leave it. These check that the second
  // stamp is written by every route out of the queue, and that the wait stops
  // growing once it is.
  // ==================================================================

  private void testQueueHistory() {
    runner.suite("Queue history - joining the queue");

    LocalDateTime arrived = LocalDateTime.now().minusMinutes(30);
    WalkInRegistration reg = new WalkInRegistration("WR0001", "G0001", arrived,
        WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 2);

    runner.check("joining the queue is stamped", reg.getQueuedAt() != null);
    runner.checkEquals("at the moment they arrived", arrived, reg.getQueuedAt());
    runner.check("leaving it is not stamped yet", reg.getServedAt() == null);
    runner.checkEquals("and it shows as a dash", "-", reg.getFormattedServedAt());
    runner.check("the wait is still running",
        reg.getWaitingMinutes() >= 30 && reg.getWaitingMinutes() <= 31);

    runner.suite("Queue history - being called to the counter");

    WalkInRegistration called = new WalkInRegistration("WR0002", "G0002", arrived,
        WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 1);
    called.leaveQueue(WalkInRegistration.STATUS_IN_SERVICE, arrived.plusMinutes(20));

    runner.checkEquals("the status becomes IN_SERVICE",
        WalkInRegistration.STATUS_IN_SERVICE, called.getStatus());
    runner.check("leaving the queue is stamped", called.getServedAt() != null);
    runner.checkEquals("calledAt is stamped with it",
        arrived.plusMinutes(20), called.getCalledAt());
    runner.checkEquals("the wait is fixed between the two stamps", 20L,
        called.getWaitingMinutes());
    runner.check("and they are no longer waiting", !called.isWaiting());

    // The wait must not creep upward once it has been closed.
    long firstRead = called.getWaitingMinutes();
    long secondRead = called.getWaitingMinutes();
    runner.checkEquals("a finished wait does not keep growing", firstRead, secondRead);

    runner.suite("Queue history - cancelling");

    WalkInRegistration cancelled = new WalkInRegistration("WR0003", "G0003", arrived,
        WalkInRegistration.PRIORITY_NORMAL, null, "RT01", 1);
    cancelled.leaveQueue(WalkInRegistration.STATUS_CANCELLED, arrived.plusMinutes(15));

    runner.checkEquals("the status becomes CANCELLED",
        WalkInRegistration.STATUS_CANCELLED, cancelled.getStatus());
    runner.checkEquals("the wait stops when they gave up", 15L,
        cancelled.getWaitingMinutes());
    runner.check("a cancelled guest was never called",
        cancelled.getCalledAt() == null);
    runner.check("but leaving the queue is still stamped",
        cancelled.getServedAt() != null);

    runner.suite("Queue history - an urgent guest");

    WalkInRegistration urgent = new WalkInRegistration("WR0004", "G0004", arrived,
        WalkInRegistration.PRIORITY_URGENT, "Elderly guest", "RT02", 1);
    runner.check("is urgent", urgent.isUrgent());
    runner.check("joins the queue with a stamp", urgent.getQueuedAt() != null);
    urgent.leaveQueue(WalkInRegistration.STATUS_IN_SERVICE, arrived.plusMinutes(2));
    runner.checkEquals("and is served quickly", 2L, urgent.getWaitingMinutes());

    runner.suite("Queue history - a registration with no times");

    WalkInRegistration empty = new WalkInRegistration();
    runner.checkEquals("no times means no wait", 0L, empty.getWaitingMinutes());
    runner.checkEquals("queued shows as a dash", "-", empty.getFormattedQueuedAt());
    runner.checkEquals("left shows as a dash", "-", empty.getFormattedServedAt());
  }

  // ==================================================================
  // WORDING
  // ==================================================================

  private void testWaitingSentence() {
    runner.suite("Waiting count reads as a sentence");

    runner.checkEquals("one guest is singular", "1 guest is still waiting",
        boundary.WalkInRegistrationUI.waitingSentence(1));
    runner.checkEquals("three guests are plural", "3 guests are still waiting",
        boundary.WalkInRegistrationUI.waitingSentence(3));
    runner.checkEquals("none reads properly too", "no guests are still waiting",
        boundary.WalkInRegistrationUI.waitingSentence(0));
  }
}
