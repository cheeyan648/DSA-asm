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
      javac --release 17 -d build/classes $(find src -name "*.java")

      Windows PowerShell:
      javac --release 17 -d build\classes (Get-ChildItem -Recurse -Filter *.java src).FullName

  Run:
      java -cp build/classes control.TARUMTResortUI

  WHY --release 17
      If you compile with a newer JDK than the one that runs the program, the
      run fails with:

          UnsupportedClassVersionError: ... has been compiled by a more recent
          version of the Java Runtime (classfile version 70.0), this version
          of the Java Runtime only recognizes class file versions up to 61.0

      That means the class files are newer than the JVM. It happens easily
      when several JDKs are installed and the IDE launches a different one
      than the terminal. Compiling with --release 17 produces class files any
      Java 17 or newer runtime accepts. If your JVM is older still, lower the
      number - the code needs nothing beyond Java 8.

      NetBeans does not have this problem: nbproject/project.properties
      already pins javac.source and javac.target, so building with F6 is
      always consistent.

REQUIREMENTS
  Java 8 or above (developed and tested on JDK 26).
  No external libraries. No Java Collections Framework classes are used - every
  collection is a custom ADT in the adt package.


2. MAIN MENU
--------------------------------------------------------------------------------

  1. Walk-In Registration & Standard Booking     Tan Chee Yan
  2. Housekeeping Task Log                       Zhi Ying
  3. Front-Desk Service                          Yong Le
  4. Loyalty & Rewards                           Ivan
  0. Quit the system

  Each module runs its own sub-menu and returns here when you enter 0.


3. DATA FILES
--------------------------------------------------------------------------------

  Three .dat files are created automatically on first run:

      walkInGuests.dat        Module 1
      housekeepingTasks.dat   Module 2
      bookings.dat            Module 3

  If a file is missing, that module seeds sample data from its initializer class
  in the dao package, so there is data to work with immediately. Module 4 keeps
  its data in memory only and writes nothing to disk.

  IF YOU SEE "Cannot read from file"
  An old .dat file from an earlier version is present and can no longer be read.
  Delete the .dat file named in the message and run again - it is recreated.


4. PROJECT STRUCTURE  (Entity-Control-Boundary)
--------------------------------------------------------------------------------

  src/adt/        Collection ADTs
  src/entity/     Data objects (POJOs - no input or output statements)
  src/boundary/   User interface - reads input, prints output
  src/control/    Business logic
  src/dao/        File persistence and sample-data initialisers
  src/utility/    Shared static helpers (MessageUI)

  TEAM COLLECTION ADT:  List
      src/adt/ListInterface.java
      src/adt/ArrayList.java

  Supporting ADTs:
      src/adt/StackInterface.java  + ArrayStack.java   Modules 1, 2
      src/adt/QueueInterface.java  + ArrayQueue.java   Module 4
      src/adt/BookingBSTInterface.java + BookingBST.java   Module 3
      src/adt/Condition.java       supports filter/search/countIf


5. PRESENTATION AND DEMO NOTES
--------------------------------------------------------------------------------

  Not part of the submitted application. Kept separately so this file stays a
  run guide, which is all the assignment specification asks a ReadMe to be.

      docs/PRESENTATION-General.txt        team ADT, rubric mapping, Q&A prep
      docs/PRESENTATION-Module1-WalkIn.txt        Tan Chee Yan
      docs/PRESENTATION-Module2-Housekeeping.txt  Zhi Ying
      docs/PRESENTATION-Module3-FrontDesk.txt     Yong Le
      docs/PRESENTATION-Module4-Loyalty.txt       Ivan

================================================================================
