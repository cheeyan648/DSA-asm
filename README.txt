================================================================================
TARUMT RESORT MANAGEMENT SYSTEM
BMCS2063 Data Structures and Algorithms - Assignment 202605

ReadMe - how to run the application.
================================================================================


1. HOW TO RUN
--------------------------------------------------------------------------------

OPTION A - NetBeans (recommended)
  1. Open NetBeans -> File -> Open Project -> select the DSA-asm folder.
  2. Right-click the project -> Run, or press F6.
  3. Main class:  control.TARUMTResortUI

OPTION B - Command line
  Compile:
      javac --release 14 -d build/classes $(find src -name "*.java")

      Windows PowerShell:
      javac --release 14 -d build\classes (Get-ChildItem -Recurse -Filter *.java src).FullName

  Run:
      java -cp build/classes control.TARUMTResortUI

  WHY --release 14
      If you compile with a newer JDK than the one that runs the program, the
      run fails with:

          UnsupportedClassVersionError: ... has been compiled by a more recent
          version of the Java Runtime

      That means the class files are newer than the JVM. It happens easily
      when several JDKs are installed and the IDE launches a different one
      than the terminal. 14 is the level nbproject/project.properties pins,
      so the command line and NetBeans then agree.

      NetBeans does not have this problem: javac.source and javac.target are
      already set there, so building with F6 is always consistent.

REQUIREMENTS
  Java 14 or above.
  No external libraries. No Java Collections Framework collection classes are
  used - every collection is a custom ADT in the adt package.


2. MAIN MENU
--------------------------------------------------------------------------------

  1. Walk-In Registration & Standard Booking     Wong Chee Yan
  2. Housekeeping Task Log                       Gan Zhi Ying
  3. Front-Desk Service                          Tew Yong Le
  4. Loyalty & Rewards                           Ivan Tan Yann Rong
  0. Quit the system

  Each module runs its own sub-menu and returns here when you enter 0.


3. DATA FILES
--------------------------------------------------------------------------------

  The four modules share one registry (control/ResortData), so all of them see
  the same rooms, guests and bookings. Fifteen .dat files are written beside
  the project folder and created automatically on first run:

      Masters        staff.dat  roomTypes.dat  rooms.dat  guests.dat
      Module 1       walkInRegistrations.dat
      Module 2       housekeepingTasks.dat  roomStatusLogs.dat
      Module 3       bookings.dat  roomAssignments.dat  invoices.dat
                     payments.dat
      Module 4       members.dat  rewards.dat  redemptions.dat
                     pointTransactions.dat

  On first run each table is seeded with sample data from its initializer class
  in the dao package, so there is something to work with immediately. After
  that the saved files are the record: the seed never runs again while they
  exist, so anything registered or booked survives a restart.

  TO START FROM A CLEAN SAMPLE
  Delete every .dat file in the project folder and run again. They are all
  recreated from the initializers.

  IF YOU SEE "Cannot read from file"
  An old .dat file from an earlier version is present and can no longer be
  read. Delete the file named in the message and run again.


4. PROJECT STRUCTURE  (Entity-Control-Boundary)
--------------------------------------------------------------------------------

  src/adt/        Collection ADTs
  src/entity/     Data objects (POJOs - no input or output statements)
  src/boundary/   User interface - reads input, prints output
  src/control/    Business logic
  src/dao/        File persistence and sample-data initialisers
  src/utility/    Shared static helpers (MessageUI)
  test/           Test suites (plain classes with main, not JUnit)

  TEAM COLLECTION ADT:  List
      src/adt/ListInterface.java
      src/adt/ArrayList.java

  Supporting ADTs:
      src/adt/StackInterface.java + ArrayStack.java
          Undo a registration (M1), roll back a status update (M2)
      src/adt/QueueInterface.java + ArrayQueue.java
          Backs the dual-lane queue below
      src/adt/DualLaneQueueInterface.java + DualLaneQueue.java
          Urgent and normal lanes: the walk-in waiting list (M1) and the
          cleaning queue (M2)
      src/adt/TreeInterface.java + BinarySearchTree.java
          Lookup by bookingId, by the 8-character confirmation code (M3),
          and by memberId (M4)
      src/adt/MapInterface.java + HashMap.java
          roomNo, guestId, staffId and typeId to their objects
      src/adt/Condition.java
          Supports filter, search and countIf


5. RUNNING THE TESTS
--------------------------------------------------------------------------------

  The tests are plain classes with a main method, not JUnit, so NetBeans'
  "Test Project" will not find them. Run them as files instead.

  NetBeans
      Right-click test/AllTests.java -> Run File   (Shift+F6)

  Command line
      javac -d build/test -cp src $(find src test -name "*.java")
      java -cp build/test test.AllTests

  AllTests runs five suites: unit, validation, integration, system and
  workflow. test/HousekeepingEndToEndProof.java runs separately in the same
  way. TEST-REPORT.txt in this folder is the output of a full run.


6. PRESENTATION AND DEMO NOTES
--------------------------------------------------------------------------------

  Not part of the submitted application. Kept separately so this file stays a
  run guide, which is all the assignment specification asks a ReadMe to be.

      docs/PRESENTATION-General.txt        team ADT, rubric mapping, Q&A prep
      docs/PRESENTATION-Module1-WalkIn.txt        Wong Chee Yan
      docs/PRESENTATION-Module2-Housekeeping.txt  Gan Zhi Ying
      docs/PRESENTATION-Module3-FrontDesk.txt     Tew Yong Le
      docs/PRESENTATION-Module4-Loyalty.txt       Ivan Tan Yann Rong

================================================================================
