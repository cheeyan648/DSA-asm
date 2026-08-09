================================================================================
TARUMT RESORT MANAGEMENT SYSTEM
BMCS2063 Data Structures and Algorithms - Assignment 202605

Module 1: Walk-In Registration & Standard Booking  (Linear ADT)
Author  : Tan Chee Yan
Branch  : CheeYan
================================================================================


--------------------------------------------------------------------------------
1. HOW TO RUN THE APPLICATION
--------------------------------------------------------------------------------

OPTION A - NetBeans (recommended)
  1. Open NetBeans -> File -> Open Project -> select the DSA-asm folder.
  2. Right-click the project -> Run, or press F6.
  3. The main class is  control.TARUMTResortUI

OPTION B - Command line
  Compile:
      javac -d build/classes $(find src -name "*.java")
      (Windows PowerShell:  javac -d build\classes (Get-ChildItem -Recurse -Filter *.java src).FullName )
  Run:
      java -cp build/classes control.TARUMTResortUI

REQUIREMENTS
  Java 8 or above (developed and tested on JDK 26).
  No external libraries. No Java Collections Framework classes are used
  anywhere - all collections are the custom ADTs in the adt package.

FIRST RUN
  walkInGuests.dat does not exist in the repository (it is listed in
  .gitignore). On the first run the program automatically creates it and fills
  it with 52 sample guest records from dao/WalkInGuestInitializer.java, so
  there is data to demonstrate with immediately.

IMPORTANT - IF YOU SEE A "Cannot read from file" ERROR
  That means an old walkInGuests.dat from a previous version is present. The
  WalkInGuest entity gained new fields, so old .dat files can no longer be
  read. Simply DELETE walkInGuests.dat and run again - it will be recreated.


--------------------------------------------------------------------------------
2. WHAT THIS MODULE DOES
--------------------------------------------------------------------------------

The module manages guests who arrive at the resort without a prior reservation.
Guests are handled chronologically (first come, first served). A front-desk
officer may flag a guest as URGENT when circumstances require an exception -
an elderly or unwell guest, a wheelchair user, a guest with an infant, a
medical situation, or a complaint escalation. The reason is recorded so the
override can be audited afterwards.

NOTE ON SCOPE: "urgent" here is an operational exception decided by the desk
staff. It is NOT loyalty-tier priority - VIP/loyalty-tier room allocation is a
separate module belonging to another team member, and this module deliberately
does not touch it.

MENU STRUCTURE (5 submodules)

  WALK-IN REGISTRATION & STANDARD BOOKING
   1. Guest registration
        1. Register normal walk-in (join back of queue)
        2. Register urgent walk-in (exception case)
        3. Undo last registration
   2. Queue operations
        1. Serve next guest
        2. Display current queue
        3. Cancel a waiting guest
   3. Search & filter
        1. Search by guest ID
        2. Search by name (partial match)
        3. Filter by status
        4. Filter by guest type
   4. Sorted listings
        1. By arrival time
        2. By guest name (A-Z)
        3. By waiting time (longest first)
        4. By service order (urgent first, then arrival)
   5. Reports
        1. Queue Performance Analysis Report
        2. Urgency Exception Audit Report
   0. Back to main menu


--------------------------------------------------------------------------------
3. COLLECTION ADTs USED
--------------------------------------------------------------------------------

TEAM ADT - List  (adt/ListInterface.java + adt/ArrayList.java)
  The core structure. Holds every walk-in record in chronological order.

  SIX OPERATIONS WERE ADDED to the List ADT for this module (these are the
  "additional/modified operations" the rubric asks for):

    getPosition(T)              - 1-based position of an entry, or -1
    removeEntry(T)              - removes the first matching entry
    sort(Comparator)            - own MERGE SORT, stable, O(n log n)
    filter(Condition)           - new list of entries satisfying a condition
    search(Condition)           - first entry satisfying a condition
    countIf(Condition)          - how many entries satisfy a condition

  adt/Condition.java is a small interface written for filter/search/countIf.
  It exists so the collection does not need to know anything about the objects
  it holds, and so no predefined Java functional interface is needed.

SUPPORTING ADT - Stack  (adt/ArrayStack.java)
  Holds this session's registrations so "Undo last registration" can pop the
  newest one. LIFO is the natural fit for undo. Deliberately not persisted -
  an undo only makes sense for actions taken in the current run.

  Note: the Queue ADT (adt/ArrayQueue.java) exists in the project but this
  module does not use it. A room-allocation feature that would have used it was
  dropped on purpose to avoid overlapping with the VIP & Loyalty Tier Room
  Allocation module owned by another team member.

SOURCE ACKNOWLEDGEMENT
  The core List operations are adapted from the course sample code by
  Frank M. Carrano. This is stated at the top of both ListInterface.java and
  ArrayList.java, as the assignment requires. The six added operations and
  Condition.java are original work.


--------------------------------------------------------------------------------
4. ALGORITHMS IMPLEMENTED
--------------------------------------------------------------------------------

SORTING - Merge sort, written by hand in ArrayList.sort()
  Chosen because it is stable: entries the comparator treats as equal keep
  their existing relative order. That matters here because sorting by name (or
  any other key) must leave tied guests in their original arrival order.
  O(n log n) in all cases, unlike the O(n^2) worst case of exchange sorts.
  Collections.sort() is NOT used anywhere - only Comparator, which is allowed.

SEARCHING - Linear search via search() and filter()
  Used for exact guest-ID lookup, case-insensitive partial name matching, and
  every multi-criteria filter in the reports.

QUEUE INSERTION POLICY - findUrgentInsertPosition()
  An urgent guest is inserted ahead of all waiting NORMAL guests but BEHIND any
  urgent guests already waiting, so an exception can never overtake an earlier
  exception. This prevents starvation and is more defensible than always
  inserting at position 1.


--------------------------------------------------------------------------------
5. WHAT WAS DONE TODAY  (2026-08-10)
--------------------------------------------------------------------------------

Starting point: the module had 5 flat menu options, staff had to type the guest
ID manually, and there were no reports, no search, no sorting.

(a) AUTO-ASSIGNED GUEST ID
    Staff no longer type the guest ID. generateNextGuestId() scans all existing
    records for the highest WG#### and adds one. It scans instead of keeping a
    counter, so IDs stay correct even after an undo frees one.

(b) RESTRUCTURED THE MENU INTO 5 SUBMODULES
    Normal/urgent registration moved inside "Guest registration" as requested,
    rather than sitting on the top-level menu.

(c) REFRAMED "PRIORITY" AS "URGENT"
    Renamed throughout and added a recorded reason (5 presets + free text), so
    this module does not duplicate the VIP/loyalty-tier module.

(d) EXTENDED THE LIST ADT
    Added the six operations listed in section 3, plus Condition.java.

(e) ADDED TWO REPORTS (the assignment requires at least two)
    Both combine searching + sorting + multi-criteria filtering, and both print
    a formatted console report with header, timestamp, sections, an ASCII bar
    chart and a footer:
      Report 1 - Queue Performance Analysis: arrivals, served/waiting/cancelled
                 split, average/longest/shortest waits, top 3 longest waits,
                 arrivals-per-hour chart and the peak hour.
      Report 2 - Urgency Exception Audit: how often the override is used, the
                 reasons given (charted), and its measured impact on waiting
                 times compared with normal guests.

(f) ADDED SEARCH, FILTER AND SORTED LISTINGS
    Search by ID or partial name; filter by status or guest type; four sort
    orders. Sorting always runs on a copy, never on the master list, so the
    chronological queue order is never disturbed by simply viewing a listing.

(g) DISPLAY IMPROVEMENTS
    - "Arrived" column widened to show the full date and time.
    - Replaced the confusing "Wait" (raw minutes) column with "Ahead" = how
      many guests are served before this one. Served/cancelled guests show "-".
      Waiting time is still shown on the single-guest view and in the reports.
    - Every action now clears the screen and prints its own title first.

(h) ADDED CONFIRMATION TO UNDO
    "Undo last registration" now shows exactly which registration will be
    removed and asks Y/N before doing it.

(i) INPUT VALIDATION
    - Name must contain at least one letter (so "123" is rejected), 2-40 chars,
      and allows the punctuation real names use (a/l, s/o, apostrophes,
      hyphens).
    - Contact number must be 9-10 digits and start with 0. Each failure gives
      its own message stating the rule, with examples, and reports how many
      digits were actually entered.

(j) PAGINATION - 20 ROWS PER PAGE
    Every listing (queue, search results, both filters, all four sorted
    listings) pages at 20 rows with [N]ext / [P]revious / jump-to-page / [Q]uit.
    Lists of 20 or fewer show no prompt at all. The "Ahead" counter continues
    correctly across page boundaries instead of restarting each page.

(k) EXPANDED SAMPLE DATA FROM 11 TO 52 RECORDS
    So pagination is demonstrable immediately on a fresh run: 52 records
    (3 pages) with 23 still waiting (2 pages), 25 served and 4 cancelled.


--------------------------------------------------------------------------------
6. BUGS FOUND AND FIXED TODAY
--------------------------------------------------------------------------------

1. UNDO CONSUMED HISTORY EVEN WHEN DECLINED
   Adding the Y/N confirmation exposed this: pop() ran before the question was
   asked, so answering "No" still discarded the registration from the stack and
   the next undo silently skipped one. FIXED by using peek() first and only
   calling pop() after the user confirms.

2. CANCEL FLOW UNUSABLE ONCE THE QUEUE PASSED 20
   "Cancel a waiting guest" displayed the paginated table, so the pager
   swallowed the position number the user typed. FIXED by adding a separate
   compact, non-paged pick list (displayGuestPickList) for choosing from.

3. SEEDED ARRIVALS LANDED IN THE FUTURE AFTER MIDNIGHT
   An earlier guard pushed sample arrivals forward when the program was first
   run just after midnight, so every guest showed "waited 0m". FIXED by scaling
   the sample proportionally into whatever part of today has elapsed, so
   arrivals always stay in the past and on today's date.

4. REPORT CHART LABELS COLLAPSED INTO EACH OTHER
   Labels truncated at 10 characters, so two different urgency reasons both
   displayed as "Wheelchair". FIXED by widening the limit and auto-sizing the
   chart's label column.

5. REPORT TABLE MISALIGNMENT
   The audit report still used the old narrower arrival-time width after the
   date format changed. FIXED.

KNOWN ISSUE - NOT FIXED ON PURPOSE
   utility/MessageUI.java line 65 calls scanner.nextLine() without first
   checking hasNextLine(), so it throws NoSuchElementException if input runs
   out. This is PRE-EXISTING (unchanged since commit d9d8952, before this
   work), it is in a SHARED file used by all four modules, and it cannot happen
   during normal interactive use - only with piped/redirected input. It was
   left alone to avoid editing a teammate's shared file without agreement.
   The one-line fix is to wrap that read in a hasNextLine() check.


--------------------------------------------------------------------------------
7. TESTING DONE
--------------------------------------------------------------------------------

  127 system tests  - all passing
   34 ADT unit tests - all passing

System tests covered: menu navigation with invalid input (letters, empty,
negatives, decimals, very large numbers, out-of-range), entering and leaving
every submodule, all registration paths and cancellations, every validation
branch, undo in all its states, serving until the queue is empty, cancelling
with confirm/abort, all search and filter paths, all four sort orders, both
reports including empty and all-cancelled datasets, all pagination navigation,
and cross-cutting flows such as register-then-serve and cancel-then-filter.

ADT unit tests covered: merge sort stability, sorting past the array's initial
capacity, reversed/already-sorted/all-equal input, null guards, empty lists,
and independence of filtered copies.


--------------------------------------------------------------------------------
8. WHAT STILL NEEDS TO BE DONE  (next steps)
--------------------------------------------------------------------------------

FOR THIS MODULE
  [ ] Take screenshots of every core function for the individual report
      (the report requires labelled input/output screenshots).
  [ ] Paste the control class source into the report - the assignment asks for
      CONTROL CLASSES ONLY, i.e. WalkInRegistrationBookingMaintenance.java.
      Use light mode in the IDE and format with Alt+Shift+F before capturing.
  [ ] Decide with the team whether to fix the MessageUI issue in section 6.

FOR THE TEAM (not this module - do not start these without agreeing first)
  [ ] Confirm with the team that List is the submitted team ADT. This module
      assumes it is.
  [ ] TELL THE TEAM: six methods were added to the shared ListInterface. Any
      OTHER class implementing that interface must implement them too, or the
      project will not compile. ArrayList.java has already been updated.
  [ ] TELL THE TEAM: anyone with an old walkInGuests.dat must delete it once.
  [ ] The other three modules are still unimplemented stubs:
        - Housekeeping Task Log        (HousekeepingTaskLogMaintenance.java)
        - Front-Desk Service           (FrontDeskServiceMaintenance.java)
        - Loyalty & Rewards            (LoyaltyRewardsMaintenance.java)
      Each still contains its original TODO comments describing what to build.
  [ ] Team ADT specification document (Part A of the report).

DEADLINES
  Week 10, Friday 2026-08-21 11:59pm - report (PDF) + NetBeans project
  Week 11-12                         - individual demo


--------------------------------------------------------------------------------
9. FILES CHANGED BY THIS MODULE
--------------------------------------------------------------------------------

NEW
  src/adt/Condition.java                              condition interface

MODIFIED - shared ADT (additive only, nothing removed or altered)
  src/adt/ListInterface.java                          + 6 operations
  src/adt/ArrayList.java                              + implementations

MODIFIED - this module only
  src/entity/WalkInGuest.java                         new fields, urgency
  src/dao/WalkInGuestInitializer.java                 52 sample records
  src/boundary/WalkInRegistrationBookingUI.java       menus, validation, paging
  src/control/WalkInRegistrationBookingMaintenance.java  all logic + reports

NOT TOUCHED (verified with git diff)
  All three teammates' control and boundary classes, HousekeepingTask.java,
  Booking.java, Member.java, MessageUI.java, TARUMTResortUI.java,
  ArrayQueue.java, ArrayStack.java, QueueInterface.java, StackInterface.java
  and WalkInGuestDAO.java.


--------------------------------------------------------------------------------
10. PROJECT STRUCTURE  (Entity-Control-Boundary)
--------------------------------------------------------------------------------

  src/adt/        Collection ADTs (List, Stack, Queue, Condition)
  src/entity/     Data objects - no input or output statements (POJOs)
  src/boundary/   User interface - reads input, prints output, no logic
  src/control/    Business logic - orchestrates boundary and entity objects
  src/dao/        File persistence and sample-data initialisers
  src/utility/    Shared static helpers (MessageUI)

  walkInGuests.dat is generated at runtime and is intentionally gitignored.

================================================================================
END OF README
================================================================================
