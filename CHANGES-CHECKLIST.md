# Change Checklist — Walk-In Registration & System-Wide Fixes

Tick each item as you verify it. Every item lists **where to look** and **what
you should see**.

How to run:

```powershell
& 'C:\Program Files\Java\jdk-17\bin\javac' -d build\classes (Get-ChildItem -Recurse src -Filter *.java).FullName
& 'C:\Program Files\Java\jdk-17\bin\java' -cp build\classes control.TARUMTResortUI
```

---

## A. Screens you asked to clean up

- [ ] **A1. Sign-in screen removed entirely**
  Starting the program goes **straight to the main menu** — no staff list, no
  "Staff number" prompt, no "press ENTER to continue". The officer rostered
  first is taken as on duty internally (actions and reports still need a name
  to record against), but nothing about that is shown or asked on screen.

- [ ] **A1b. Home screen footer removed**
  The main menu used to end with a live-summary block (`Queue: 2 waiting...`,
  `Rooms sellable now...`), an `On duty:` line and a `Today:` date. All of it
  is gone — the menu now ends right after `[0]  Quit the system`.

- [ ] **A2. Module-name subtitle removed**
  Walk-In menu no longer shows `& S T A N D A R D   B O O K I N G`.
  Also removed for consistency: Housekeeping (`T A S K   L O G`) and
  Loyalty (`& R E W A R D S`).

- [ ] **A3. Typo fixed (found while testing)**
  Main menu read *"Select a subsystem**do** to continue."* → now
  *"Select a subsystem to continue."*

---

## B. Contact number & other input validation

Walk-In → Guest registration → Register normal walk-in → new IC number.

- [ ] **B1. Contact number rejects letters** ← *your original bug*
  Type `abcdefghij` → `[!] A contact number can only contain digits, spaces and dashes.`
  It re-prompts; it does **not** accept and continue.

- [ ] **B2. Must start with 0**
  Type `1123456789` → `[!] A contact number must start with 0, e.g. 0123456789.`

- [ ] **B3. Must be 10–11 digits**
  Type `012345678` (9) → refused. `012345678901` (12) → refused.
  `0123456789` (10) and `01234567890` (11) → accepted.

- [ ] **B4. Spaces and dashes allowed, then stripped**
  Type `012-345 6789` → accepted, stored as `0123456789`.

- [ ] **B5. Name rejects digits**
  Type `Guest 123` → `[!] A name can only contain letters, spaces, apostrophes, dots and hyphens.`
  `O'Brien`, `Nurul-Ain`, `Muthu a/l Samy` are accepted.

- [ ] **B6. Email shape checked**
  Type `notanemail` → refused. `a@b.com` → accepted. Blank ENTER → skipped (optional).

- [ ] **B7. IC / passport checked**
  Letters+digits only, 6–15 characters. Punctuation refused.

- [ ] **B8. Same validation at Front Desk**
  Front-Desk → Bookings → new booking with an unknown IC.
  Name / contact / email prompts behave exactly as B1–B6.

---

## C. Room type selection is meaningful

- [ ] **C1. NO column added**
  Registration → room type table now reads:

  ```
    NO    TYPE   NAME                 MAX   RATE/NIGHT  DESCRIPTION
    [1]   RT01   Standard Twin          2       150.00  ...
    [2]   RT02   Standard Queen         2       180.00  ...
  ```

  The prompt asks 1–5 and the table shows `[1]`–`[5]`. Applied to Walk-In
  **and** Front-Desk.

---

## D. Waiting record stored at both ends

- [ ] **D1. Two timestamps kept**
  A registration now stores **queuedAt** (joining the queue) and
  **servedAt** (leaving it). Previously only the call was stamped.

- [ ] **D2. Visible on the detail screen**
  Search a registration → shows `Joined queue` and, once served, `Left queue`.

- [ ] **D3. Visible in listings**
  Any registration listing has `QUEUED` and `LEFT QUEUE` columns.

- [ ] **D4. Status changes to served**
  Serve a guest → status `WAITING` → `IN_SERVICE`, and `Left queue` is stamped.

- [ ] **D5. Every exit stamps it**
  Cancel and no-show also stamp `servedAt`, so a wait can still be measured
  after the guest is gone. Waiting time stops growing once closed.

---

## E. Cancel a waiting guest — by position

Walk-In → Queue operations → Cancel a waiting guest.

- [ ] **E1. POS column shown**
  Table shows `POS  REG ID  GUEST ...` with positions `1, 2, 3, 4`.

- [ ] **E2. You type the position, not the ID**
  Prompt: `Position to cancel (1-2, 0 to quit):` — type `1`, not `WR0004`.

- [ ] **E3. Registration ID still displayed**
  The `REG ID` column still shows `WR0003`, `WR0004`, … as before.

- [ ] **E4. Loops until valid — no "press enter to continue"**
  Type `abc` → `[!] Please enter one of the numbers shown in the first column, e.g. 1.` → asks again.
  Type `99` → `[!] There is no number 99 in that list. Enter a number from 1 to 2.` → asks again.
  It never dumps you back to the menu on a typo.

- [ ] **E5. 0 quits**
  Typing `0` leaves the action cleanly.

- [ ] **E6. Grammar fixed**
  Was: `Guests still waiting: 3`
  Now: `[OK] WR0003 cancelled. 1 guest is still waiting.`
  and with 3 → `3 guests are still waiting.` (is/are, guest/guests correct)

---

## F. Serving a guest

Walk-In → Queue operations → Serve next guest.

- [ ] **F1. No booking option here**
  It no longer asks *"Create their booking now?"*. It closes with:
  `Their booking is made at the Front Desk.`

- [ ] **F2. Next guest announced**
  After serving:
  ```
  1 guest is still waiting.
  Next to be called: Sarah Lim Mei Xin (WR0004, NORMAL), waiting 33m.
  ```

- [ ] **F3. Same summary after cancel and no-show**
  Both also report who is next and how many are waiting.

---

## G. Mark as no-show — what it is for

Walk-In → Queue operations → Mark a called guest as no-show.

- [ ] **G1. Explained on screen**
  `A guest can only be a no-show once they have been called to the counter and have not come forward.`

- [ ] **G2. Only called guests appear**
  The list shows `IN_SERVICE` guests only. If nobody has been called, it says so.
  (Serve a guest first, then this list has one row.)

- [ ] **G3. Picked by position**
  `Position to mark as a no-show (1-1, 0 to quit):` — same loop-until-valid rule.

- [ ] **G4. Stays in Walk-In (answering your question)**
  A no-show never reached the front desk — they were called at the queue and
  did not appear, so the queue is what records it. Had they arrived at the
  desk, the front desk would record what happened instead. The wait keeps the
  time they were *called*, not the time somebody got round to logging it.

---

## H. Queue display moved

- [ ] **H1. Gone from Queue operations**
  That menu is now: Serve next guest / Cancel a waiting guest / Mark as no-show.

- [ ] **H2. Now first under Search & filter**
  ```
  [1]  Display current waiting queue
  [2]  Search by registration number
  [3]  Search by guest name
  [4]  Filter by status
  [5]  Filter by priority
  ```

---

## I. Registration number — no prefix needed

- [ ] **I1. Type digits only**
  Search & filter → Search by registration number.
  Prompt: `Registration number (number only, e.g. 3 or 0003, 0 to cancel):`
  Type `3` → finds `WR0003`. Typing `0003` or `WR0003` also works.

- [ ] **I2. Wrong number does not quit** ← *your point*
  Type `999` → `[!] No registration WR0999. Enter another number, or 0 to go back.`
  It asks again rather than exiting on the first bad input.

- [ ] **I3. Non-numeric refused**
  `WRabc` → `[!] Please enter just the number, e.g. 3 or 0003.`

- [ ] **I4. Applied to every module**
  Booking → `BK`, Invoice → `INV`, Task → `HK`, Member → `L`, Guest → `G`,
  Room number → 4 digits, no prefix.

---

## J. Pagination everywhere

- [ ] **J1. Long lists page**
  Any listing over 15 rows shows `Page 1 of 3. [N]ext page or [Q]uit listing:`

- [ ] **J2. Last page no longer vanishes**
  Previously the final page was wiped by the next screen. It now stops with
  `Page 3 of 3 - end of list.`

- [ ] **J3. Positions stay correct across pages**
  On page 2, POS continues 16, 17, 18 — it does not restart at 1. This matters
  because POS is what you type to cancel.

- [ ] **J4. Invalid paging key re-asks**
  Anything other than N/Q → `[!] Please enter N for the next page, or Q to stop.`

---

## K. Reports start at the top ← *your point*

- [ ] **K1. Report opens at its title**
  Walk-In → Reports → Queue Performance Analysis Report.
  First thing on screen is the framed title and `Generated:` line — **not**
  the end of the report.

- [ ] **K2. Paged, not dumped**
  `Screen 1 of 3. [N]ext or [Q]uit:` — read top to bottom in order.

- [ ] **K3. No double prompt**
  Reports end once, cleanly. (Removed the duplicated "press enter" that
  followed every report footer.)

- [ ] **K4. Applied to all four modules**
  Front-Desk, Housekeeping and Loyalty reports behave identically.

---

## L. Duplication between Sorted lists and Search/filter

You asked whether these overlapped. They did — fixed:

- [ ] **L1. "By service order" removed**
  It listed only WAITING rows in queue order — exactly what
  *Display current waiting queue* shows. Two screens claiming to say who is
  next could disagree.

- [ ] **L2. Replaced with "By status, then arrival time"**
  Covers **all** registrations grouped by outcome — a different question from
  the live queue.

- [ ] **L3. "Filter by priority" kept**
  Not a duplicate: it filters every registration ever made by URGENT/NORMAL,
  including finished ones. The queue display only ever shows who is waiting now.

---

## M. Tests

```powershell
& 'C:\Program Files\Java\jdk-17\bin\javac' -cp build\classes -d build\test (Get-ChildItem test -Filter *.java).FullName
& 'C:\Program Files\Java\jdk-17\bin\java' -cp "build\classes;build\test" test.AllTests
```

- [ ] **M1. All suites pass — 540 checks, 0 failures**

| Suite | Checks | What it covers |
|---|---|---|
| Unit | 218 | ADTs and entities in isolation |
| **Validation (new)** | **70** | phone/email/name/IC rules, queue history, grammar |
| Integration | 141 | modules working together |
| System | 111 | whole-system consistency, save/reload |

- [ ] **M2. New `test/ValidationTest.java`** covers:
  - Contact number: valid 10/11-digit, separators, and every rejection
    (letters, wrong start, too short, too long, empty, null, `+60…`)
  - Email and name rules, boundary cases
  - Queue history: both stamps written by call / cancel / urgent paths;
    a closed wait does not keep growing
  - "1 guest is / 3 guests are still waiting" grammar

- [ ] **M3. Defect testing done by driving the real UI**
  Scripted keystroke runs through registration, cancel, no-show, search and
  reports with deliberately bad input at every prompt. Fixed what it found:
  the `subsystemdo` typo, and error wording that named a "NO column" on a
  table headed POS.

---

## Files changed

| File | Change |
|---|---|
| `src/utility/MessageUI.java` | `readPhone`, `readName`, `readIcPassport`, `readOptionalEmail`, `readIdNumber` + validators; paging rewritten; report capture/paging |
| `src/entity/WalkInRegistration.java` | `queuedAt` / `servedAt`, `leaveQueue()`, wait measured between stamps, `serialVersionUID` 2→3 |
| `src/boundary/WalkInRegistrationUI.java` | menus reordered, NO/POS columns, position prompts, queue-history columns |
| `src/control/WalkInRegistrationMaintenance.java` | serve/cancel/no-show rewritten, booking prompt removed, search loops |
| `src/control/ResortService.java` | serving stamps both ends |
| `src/control/TARUMTResortUI.java` | "Who is on duty?" removed, typo fixed |
| `src/boundary/FrontDeskServiceUI.java`, `HousekeepingTaskLogUI.java`, `LoyaltyRewardsUI.java` | subtitles removed, ID prompts, report paging |
| `src/control/FrontDeskServiceMaintenance.java` + Housekeeping/Loyalty | validated input, duplicate pauses removed |
| `test/ValidationTest.java` | **new** |
| `test/AllTests.java` | runs the new suite |

---

## One thing to know

`serialVersionUID` on `WalkInRegistration` moved 2 → 3 because fields were
added. Old `walkInRegistrations.dat` files will not load and the app starts
that file empty (it says so and carries on — it does not crash). Your current
data files still load because they were re-saved after the change. If you
restore an older backup, expect the registrations to start fresh.

---

## N. Second round of changes

### N1. Zero cancels, however many you type

- [ ] **N1a.** `0` backs out of any prompt or menu, as before.
- [ ] **N1b.** `00`, `000`, `0000` do exactly the same — no "invalid choice".
- [ ] **N1c.** `10` and `01` are still ordinary numbers, not cancels.

### N2. "Press ENTER" accepts only ENTER

- [ ] **N2a.** At a pause, typing anything → `[!] Nothing to type here - just press ENTER.`
  It asks again instead of swallowing the line.
- [ ] **N2b.** Wording changed from "press ENTER to continue" to **"Press ENTER to exit"**.

### N3. Email validation

- [ ] **N3a.** `123` alone → refused. `ali` alone → refused. A bare gmail name → refused.
- [ ] **N3b.** `123@gmail.co` → **accepted** (short but valid, exactly as you asked).
- [ ] **N3c.** `guest@gmail` (no dot), `guest@gmail..com`, `guest@.com`, `guest@gmail.` → all refused.

### N4. Walk-in nights carry to the Front Desk booking

- [ ] **N4a.** Register a walk-in asking for e.g. 3 nights, serve them.
- [ ] **N4b.** Front Desk → Bookings → new booking with that guest's IC.
  It says `This guest was sent over from the walk-in queue (WR000n)` and shows:
  ```
  Nights requested   : 3 night(s)
  Check-in           : Wednesday, 26/08/2026
  Check-out          : Saturday, 29/08/2026
  ```
- [ ] **N4c.** Answering **no** falls back to typing the dates by hand.

### N5. "Next to be called" rewritten

- [ ] **N5.** Was: `Sarah Lim Mei Xin (WR0004, NORMAL), waiting 1h 39m.`
  Now: `Next to be called: Sarah Lim Mei Xin, normal booking, arrived 26/08/2026 20:39, waited 2h 04m.`
  No brackets; priority spelled out as a kind of booking; arrival time shown; wait last.

### N6. Search & filter screens loop and refresh

Walk-In → Search & filter. Also applied to Front-Desk search screens.

- [ ] **N6a.** Search by registration number → enter `3`, see WR0003's details.
- [ ] **N6b.** It then asks `Look up another registration number? (y/n)`.
  Answer **y**, enter `5` → the screen **clears and redraws** with WR0005.
  There is no "press enter to continue" any more.
- [ ] **N6c.** Answering **n**, or entering `0`, leaves the screen.
- [ ] **N6d.** Same looping on: search by guest name, filter by status, filter by priority.
- [ ] **N6e.** Results are ordered **newest first** — the guest who joined the queue
  most recently at the top, earliest at the bottom.

### N7. Sorted listings pick the order underneath the table

- [ ] **N7a.** Walk-In → Sorted listings opens straight on arrival order.
- [ ] **N7b.** A chooser sits **under** the listing:
  ```
    Sort this listing by:
      [1]  Arrival time (earliest first)
      [2]  Guest name (A-Z)
      [3]  Waiting time (longest first)
      [4]  Status, then arrival time
      [0]  Back
  ```
- [ ] **N7c.** Pressing `3` redraws the same data in the new order — no leaving and re-entering.

### N8. Reports print in full, exit at the bottom

- [ ] **N8a.** The whole report prints at once — **no** `Screen 1 of 3` prompts.
- [ ] **N8b.** Scroll up to read from the top; the console scrollbar does the moving.
- [ ] **N8c.** One prompt at the very bottom: `End of report. Press ENTER or 0 to exit...`

---

## O. Housekeeping and Front-Desk integration

### O1. Housekeeping manages the rooms

Housekeeping → **Manage rooms** (new menu item 4).

- [ ] **O1a. List every room** — with a `SELLABLE?` column saying in words why a
  room cannot be given out (`No - needs cleaning`, `No - guest in it`,
  `No - held for a booking`, `No - out of service`, or `Yes`).
- [ ] **O1b. Add a room** — asks number, type (numbered `[1]`-`[5]`), floor.
  The new room starts **DIRTY** and is queued for cleaning automatically:
  `Room 9001 added and sent to housekeeping to be prepared (task HK0008).`
  It **cannot be sold** until cleaned and inspected.
- [ ] **O1c. Remove a room** — refused while a guest is in it or a live booking
  holds it (`Booking BK000n is holding room 9001. Move it first.`).
  Any queued cleaning for it is dropped.
- [ ] **O1d. Take a room out of service** — kept off the market without deleting it.
  Refused if a guest is in it.
- [ ] **O1e. Return a room to service** — comes back **DIRTY**, not ready, and is
  re-queued for cleaning.

### O2. A dirty room cannot be served

- [ ] **O2.** Add room 9001 in Housekeeping, then look at
  Front-Desk → Rooms → Room status board. It appears immediately as
  `9001  Standard Twin  VACANT  DIRTY  No (not cleaned)`.
  **This is the integration**: one `ResortData` is built in main and handed to
  all four modules, so a room created in Housekeeping is instantly visible at
  the Front Desk — no save, no reload.

### O3. Urgent first, in both queues

- [ ] **O3a. Cleaning** — a room with an urgent booking waiting on it is cleaned
  first. The lane is read from the booking, never typed in
  (`refreshTaskPriority`), so an urgency granted at the walk-in door travels
  into housekeeping unchanged.
- [ ] **O3b. Room assignment** — Front-Desk → Rooms → Assign a room now lists the
  waiting bookings **urgent first**:
  ```
    NO   BOOKING GUEST                TYPE   ROOM   CHECK IN    CHECK OUT   STATUS       PRI
    1    BK0007  Nur Farah binti Idr. RT02   -      2026-08-26  2026-08-28  PENDING      URG
    2    BK0006  Sarah Lim Mei Xin    RT04   -      2026-09-02  2026-09-06  PENDING      -
  ```
  `Urgent bookings are listed first - work down from the top.`
  Note BK0007 sorts above BK0006 even though BK0006 was made earlier.
- [ ] **O3c.** The booking is picked by **position** (1, 2, 3), same
  loop-until-valid rule as everywhere else, and the screen loops so several
  rooms can be handed out in a row.

---

## P. Your question: does main hold the data for everyone?

**Yes.** `TARUMTResortUI` builds `ResortData` once and passes it to all four
modules via `ResortService`:

```java
data = new ResortData();              // the one copy of every table
service = new ResortService(data);

walkIn       = new WalkInRegistrationMaintenance(service, staffId);
frontDesk    = new FrontDeskServiceMaintenance(service, staffId);
housekeeping = new HousekeepingTaskLogMaintenance(service, staffId);
loyalty      = new LoyaltyRewardsMaintenance(service, staffId);
```

Every module then does `this.data = service.getData()` — the **same object**,
not a copy. That is why O2 works: a room added in Housekeeping is visible at
the Front Desk in the same breath. Shared operations that touch more than one
module (serving a guest, converting to a booking, urgent cleaning, room
create/remove) live in `ResortService` so both modules obey the same rules.

---

## Q. Tests after this round

- [ ] **Q1. 561 checks pass, 0 failures**

| Suite | Checks |
|---|---|
| Unit | 218 |
| Validation | **91** (was 70) |
| Integration | 141 |
| System | 111 |

- [ ] **Q2. New validation cases** cover the multi-zero cancel key and every
  email case named above.
- [ ] **Q3. Interactive runs verified**: multi-zero exit, looping search with
  refresh, sorted-listing chooser, full-page report, strict pause, room
  add/list across modules, urgent-first assignment.
