#!/bin/bash
# ==============================================================================
# SYSTEM TESTS - drives the whole application through its real console menus.
#
# Each case pipes a scripted keystroke sequence into control.TARUMTResortUI and
# checks the output for what should and should not appear. This is the only
# layer that exercises the boundary classes, the menu loops and the clear-screen
# behaviour, because those need a real console session.
#
# Runs in a temporary directory so the project's .dat files are never touched.
#
# Usage:  bash test/run-system-tests.sh
#
# @author Tan Chee Yan
# ==============================================================================

# Two forms of the project path are needed on Windows: bash navigates with the
# Unix form (/c/...), while the JVM only understands the Windows form (C:/...).
PROJECT="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_WIN="$(cd "$PROJECT" && { pwd -W 2>/dev/null || pwd; })"
CLASSES="$PROJECT_WIN/build/classes"
SANDBOX="$PROJECT/build/systest"

run=0
passed=0

setup() {
  # Return to the project first: the sandbox cannot be removed while it is the
  # shell's working directory, which is where drive() leaves us.
  cd "$PROJECT" || exit 1
  rm -rf "$SANDBOX" 2>/dev/null
  mkdir -p "$SANDBOX"
}

# drive <input> -> prints the application output with ANSI codes stripped
drive() {
  cd "$SANDBOX" || exit 1
  printf '%b' "$1" | timeout 90 java -cp "$CLASSES" control.TARUMTResortUI 2>&1 \
    | sed 's/\x1b\[[0-9;]*[A-Za-z]//g'
  cd "$PROJECT" || exit 1
}

# expect <description> <output> <pattern>
expect() {
  run=$((run + 1))
  if echo "$2" | grep -qF -- "$3"; then
    passed=$((passed + 1))
    echo "  [PASS] $1"
  else
    echo "  [FAIL] $1"
    echo "         expected to find: $3"
  fi
}

# reject <description> <output> <pattern>
reject() {
  run=$((run + 1))
  if echo "$2" | grep -qF -- "$3"; then
    echo "  [FAIL] $1"
    echo "         should NOT have found: $3"
  else
    passed=$((passed + 1))
    echo "  [PASS] $1"
  fi
}

section() {
  echo ""
  echo "$1"
  echo "------------------------------------------------------------------------------"
}

echo "=============================================================================="
echo "  SYSTEM TESTS - WHOLE APPLICATION THROUGH THE REAL MENUS"
echo "=============================================================================="

# ------------------------------------------------------------------------------
section "1. NAVIGATION - entering and leaving every module"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '0\n')
expect "the home screen shows the system title" "$OUT" "R E S O R T   M A N A G E M E N T   S Y S T E M"
expect "the home screen lists all four subsystems" "$OUT" "Walk-In Registration & Standard Booking"
expect "the home screen lists Housekeeping" "$OUT" "Housekeeping Task Log"
expect "the home screen lists Front-Desk" "$OUT" "Front-Desk Service"
expect "the home screen lists Loyalty" "$OUT" "Loyalty & Rewards"
expect "quitting shows the exit screen" "$OUT" "THANK YOU FOR USING"
reject "a clean exit prints no stack trace" "$OUT" "	at "

setup
OUT=$(drive '1\n0\n2\n0\n3\n0\n4\n0\n0\n')
# Module 1's title is drawn letter-spaced by the UI's spaced() helper, so the
# on-screen text is "W A L K - I N   R E G I S T R A T I O N".
expect "Module 1 opens" "$OUT" "W A L K - I N   R E G I S T R A T I O N"
expect "Module 2 opens" "$OUT" "Housekeeping Task Log"
expect "Module 3 opens" "$OUT" "F R O N T - D E S K   S E R V I C E"
expect "Module 4 opens" "$OUT" "Loyalty & Rewards"
expect "every module returns to the home screen" "$OUT" "THANK YOU FOR USING"
reject "no crash while touring all four modules" "$OUT" "Exception in thread"

# ------------------------------------------------------------------------------
section "2. INVALID INPUT - menus reject bad choices without crashing"
# ------------------------------------------------------------------------------
setup
OUT=$(drive 'abc\n99\n-5\n2.5\n\n0\n')
expect "an out-of-range choice is rejected" "$OUT" "Invalid choice!"
reject "invalid input never crashes the home menu" "$OUT" "Exception in thread"
expect "the user can still exit afterwards" "$OUT" "THANK YOU FOR USING"

setup
OUT=$(drive '1\nabc\n99\n-1\n0\n0\n')
expect "a module submenu rejects bad input" "$OUT" "Invalid choice!"
reject "invalid submenu input never crashes" "$OUT" "Exception in thread"

# ------------------------------------------------------------------------------
section "3. MODULE 1 - REGISTRATION, CANCEL AT FIRST INPUT"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '1\n1\n1\n0\n\n0\n0\n0\n')
expect "the register screen opens" "$OUT" "REGISTER NORMAL WALK-IN GUEST"
expect "a guest id is assigned automatically" "$OUT" "Assigned guest ID:"
expect "the cancel option is offered up front" "$OUT" "(enter 0 as the name to cancel)"
expect "entering 0 at the FIRST input cancels" "$OUT" "Registration cancelled."
reject "cancelling registers nobody" "$OUT" "registered at the back of the queue"

# ------------------------------------------------------------------------------
section "4. MODULE 1 - VALIDATION"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '1\n1\n1\n\nA\n123\nX@#\nValid Name\n12345\n0123456789\n\n0\n0\n0\n')
expect "an empty name is rejected" "$OUT" "Name cannot be empty!"
expect "a one-character name is rejected" "$OUT" "Name is too short!"
expect "a digits-only name is rejected" "$OUT" "A name must contain letters"
expect "an illegal symbol is rejected" "$OUT" "Letters, numbers, spaces"
expect "a too-short contact number is rejected" "$OUT" "Contact number is too short!"
expect "the error states how many digits were entered" "$OUT" "digit(s)"
expect "a valid registration finally succeeds" "$OUT" "registered at the back of the queue"

# ------------------------------------------------------------------------------
section "5. MODULE 1 - UNDO CONFIRMATION (peek, decline, pop)"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '1\n1\n3\n\n0\n0\n0\n')
expect "undo with nothing registered reports it" "$OUT" "Nothing to undo"

setup
OUT=$(drive '1\n1\n1\nUndo Test\n0123456789\n\n3\nN\n\n3\nY\n\n0\n0\n0\n')
expect "undo names the exact registration" "$OUT" "This registration will be removed:"
expect "undo asks for confirmation" "$OUT" "Are you sure you want to undo"
expect "declining changes nothing" "$OUT" "Undo cancelled - nothing has been changed."
expect "the SAME guest is still undoable afterwards" "$OUT" "Undone - registration for guest"
expect "the undone guest is named" "$OUT" "Undo Test"

# ------------------------------------------------------------------------------
section "6. MODULE 1 - SERVE AND CANCEL BOTH CONFIRM"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '1\n2\n1\nN\n\n0\n0\n0\n')
expect "serving shows the guest first" "$OUT" "Next guest in the queue:"
expect "serving asks for confirmation" "$OUT" "Serve this guest now?"
expect "declining leaves the guest waiting" "$OUT" "Serving aborted - the guest is still waiting."
reject "declining does not serve anyone" "$OUT" "NOW SERVING"

setup
OUT=$(drive '1\n2\n1\nY\n\n0\n0\n0\n')
expect "confirming serves the guest" "$OUT" "NOW SERVING"

setup
OUT=$(drive '1\n2\n3\n1\nN\n\n0\n0\n0\n')
expect "cancelling names the specific guest" "$OUT" "Cancel the registration for"
expect "declining a cancellation aborts it" "$OUT" "Cancellation aborted."
reject "declining removes nobody" "$OUT" "has been cancelled and removed"

setup
OUT=$(drive '1\n2\n3\n1\nY\n\n0\n0\n0\n')
expect "confirming a cancellation removes the guest" "$OUT" "has been cancelled and removed"

# ------------------------------------------------------------------------------
section "7. MODULE 1 - PAGINATION AND QUIT-RETURNS-TO-MENU"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '1\n2\n2\nQ\n0\n0\n0\n')
expect "the queue listing pages" "$OUT" "Page 1 of"
expect "the pager offers its controls" "$OUT" "[Q] quit listing"
expect "the Ahead column is explained" "$OUT" "guests to be served before this one"
expect "quitting the listing returns straight to the menu" "$OUT" "Serve next guest"

# Leaving a PAGED listing with [Q] must not then ask for Enter as well - that
# was the two-keystroke defect. A SINGLE-page listing has no [Q] prompt, so it
# must still pause or the table would vanish before it could be read.
run=$((run + 1))
PAGED_PAUSES=$(echo "$OUT" | grep -c "Press Enter to continue")
if [ "$PAGED_PAUSES" -eq 0 ]; then
  passed=$((passed + 1))
  echo "  [PASS] quitting a paged listing does NOT also ask to press Enter"
else
  echo "  [FAIL] quitting a paged listing does NOT also ask to press Enter"
  echo "         expected 0 pauses, found $PAGED_PAUSES"
fi

setup
SINGLE=$(drive '1\n3\n4\n1\n\n0\n0\n0\n')
run=$((run + 1))
SINGLE_PAUSES=$(echo "$SINGLE" | grep -c "Press Enter to continue")
if [ "$SINGLE_PAUSES" -ge 1 ]; then
  passed=$((passed + 1))
  echo "  [PASS] a single-page listing still pauses so it can be read"
else
  echo "  [FAIL] a single-page listing still pauses so it can be read"
  echo "         expected at least 1 pause, found $SINGLE_PAUSES"
fi

setup
OUT=$(drive '1\n2\n2\nN\nP\nQ\n0\n0\n0\n')
expect "paging forward works" "$OUT" "Page 2 of"
reject "paging never crashes" "$OUT" "Exception in thread"

# ------------------------------------------------------------------------------
section "8. MODULE 1 - SEARCH, FILTER AND SORT"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '1\n3\n2\na\nQ\n0\n0\n0\n')
expect "a partial-name search reports its term" "$OUT" "SEARCH RESULTS FOR"

setup
OUT=$(drive '1\n3\n1\n9999\n\n0\n0\n0\n')
expect "searching an unknown id says so" "$OUT" "No guest found"

setup
OUT=$(drive '1\n3\n3\n0\n\n0\n0\n0\n')
expect "cancelling a status filter confirms it" "$OUT" "Filter cancelled."

setup
OUT=$(drive '1\n3\n4\n0\n\n0\n0\n0\n')
expect "cancelling a type filter confirms it" "$OUT" "Filter cancelled."

setup
OUT=$(drive '1\n4\n1\nQ\n2\nQ\n3\nQ\n4\nQ\n0\n0\n0\n')
expect "sort by arrival time works" "$OUT" "BY ARRIVAL TIME"
expect "sort by name works" "$OUT" "BY GUEST NAME"
expect "sort by waiting time works" "$OUT" "BY WAITING TIME"
expect "sort by service order works" "$OUT" "BY SERVICE ORDER"
reject "no sorted listing crashes" "$OUT" "Exception in thread"

# ------------------------------------------------------------------------------
section "9. MODULE 1 - BOTH REPORTS"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '1\n5\n1\n\n2\n\n0\n0\n0\n')
expect "the performance report renders" "$OUT" "QUEUE PERFORMANCE ANALYSIS REPORT"
expect "it summarises arrivals and service" "$OUT" "ARRIVAL AND SERVICE SUMMARY"
expect "it analyses waiting times" "$OUT" "WAITING TIME ANALYSIS"
expect "it charts arrivals by hour" "$OUT" "ARRIVALS BY HOUR"
expect "it identifies the peak hour" "$OUT" "Peak arrival hour"
expect "the audit report renders" "$OUT" "URGENCY EXCEPTION AUDIT REPORT"
expect "reports are closed off properly" "$OUT" "END OF THE REPORT"
reject "neither report crashes" "$OUT" "Exception in thread"

# ------------------------------------------------------------------------------
section "10. MODULE 2 - HOUSEKEEPING TASK LOG"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '2\n0\n0\n')
expect "the housekeeping menu opens" "$OUT" "Housekeeping Task Log"
expect "it offers logging a status update" "$OUT" "Log new task status update"
expect "it offers rollback" "$OUT" "Rollback last status update"

setup
OUT=$(drive '2\n2\n\n0\n0\n')
reject "rollback with nothing to undo does not crash" "$OUT" "Exception in thread"

setup
OUT=$(drive '2\n3\nQ\n\n0\n0\n')
reject "displaying the task log does not crash" "$OUT" "Exception in thread"

# ------------------------------------------------------------------------------
section "11. MODULE 3 - FRONT-DESK SERVICE"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '3\n0\n0\n')
expect "the front-desk menu opens" "$OUT" "F R O N T - D E S K   S E R V I C E"
expect "it offers creating a booking" "$OUT" "Create new booking"
expect "it offers the search submenu" "$OUT" "Search information"
expect "it offers the reports submenu" "$OUT" "Reports"

setup
OUT=$(drive '3\n2\n1\n00000000\n0\n0\n0\n0\n')
expect "the guest search screen opens" "$OUT" "SEARCH COMPLETE GUEST INFORMATION"
reject "searching an unknown booking does not crash" "$OUT" "Exception in thread"

setup
OUT=$(drive '3\n1\nabc\n0\n0\n0\n')
expect "a malformed confirmation number is rejected" "$OUT" "must contain exactly 8 digits"

setup
OUT=$(drive '3\n4\nQ\n\n0\n0\n')
reject "displaying all bookings does not crash" "$OUT" "Exception in thread"

# ------------------------------------------------------------------------------
section "12. MODULE 4 - LOYALTY & REWARDS"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '4\n0\n0\n')
expect "the loyalty menu opens" "$OUT" "Loyalty & Rewards"
expect "it offers member registration" "$OUT" "Register new member"
expect "it offers redemption requests" "$OUT" "Request reward redemption"
expect "it offers redemption processing" "$OUT" "Process next pending redemption"

setup
OUT=$(drive '4\n3\n\n0\n0\n')
reject "displaying all members does not crash" "$OUT" "Exception in thread"

setup
OUT=$(drive '4\n7\n\n0\n0\n')
reject "viewing rewards does not crash" "$OUT" "Exception in thread"

setup
OUT=$(drive '4\n9\n\n0\n0\n')
reject "processing an empty redemption queue does not crash" "$OUT" "Exception in thread"

# ------------------------------------------------------------------------------
section "13. FULL SWEEP - every module in one session"
# ------------------------------------------------------------------------------
setup
OUT=$(drive '1\n1\n1\nSweep Test\n0123456789\n\n0\n2\n2\nQ\n0\n3\n2\na\nQ\n0\n4\n1\nQ\n0\n5\n1\n\n0\n0\n2\n3\nQ\n\n0\n3\n4\nQ\n\n0\n4\n3\n\n0\n0\n')
expect "a registration made early in the sweep succeeds" "$OUT" "registered at the back of the queue"
expect "the sweep reaches the reports" "$OUT" "END OF THE REPORT"
expect "the sweep exits cleanly" "$OUT" "THANK YOU FOR USING"
reject "no crash anywhere in the full sweep" "$OUT" "Exception in thread"
reject "no null pointer anywhere in the full sweep" "$OUT" "NullPointerException"
reject "no index error anywhere in the full sweep" "$OUT" "IndexOutOfBounds"
reject "no missing-element error anywhere in the full sweep" "$OUT" "NoSuchElementException"

# ------------------------------------------------------------------------------
section "14. PERSISTENCE ACROSS RESTARTS"
# ------------------------------------------------------------------------------
setup
drive '1\n1\n1\nPersisted Guest\n0123456789\n\n0\n0\n0\n' > /dev/null
OUT=$(drive '1\n3\n2\nPersisted\nQ\n0\n0\n0\n')
expect "a guest registered in one run is found in the next" "$OUT" "Persisted Guest"

# ------------------------------------------------------------------------------
echo ""
echo "=============================================================================="
printf "  SYSTEM TESTS: %d of %d passed" "$passed" "$run"
if [ "$passed" -ne "$run" ]; then
  printf "   *** %d FAILED ***" "$((run - passed))"
fi
echo ""
echo "=============================================================================="

cd "$PROJECT" || exit 1
rm -rf "$SANDBOX" 2>/dev/null
[ "$passed" -eq "$run" ]
