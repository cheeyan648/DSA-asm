package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.WalkInGuest;
import java.time.LocalDateTime;

/**
 * Generates a set of sample walk-in guests, used to seed walkInGuests.dat the
 * first time the program runs (or whenever the queue is empty) so there is
 * something to demonstrate with.
 *
 * The sample covers all three statuses and a spread of arrival times across the
 * day, so the reports have enough variety to be worth reading - a queue of only
 * still-waiting guests would make the served/cancelled figures all zero.
 *
 * There are deliberately more than 20 records, and more than 20 of them are
 * still waiting, so both the full-record listings and the current-queue display
 * span several pages. That makes the paging controls demonstrable straight
 * after a fresh start, without anyone having to register guests by hand first.
 *
 * @author Tan Chee Yan
 */
public class WalkInGuestInitializer {

  // How far back the earliest sample arrival sits when there is a full day to
  // work with. Every other arrival is positioned within this window.
  private static final int FULL_SAMPLE_SPAN_MINUTES = 300;

  // How many minutes the sample is actually spread over for this run. Normally
  // the full span, but compressed when the program is first run soon after
  // midnight - see initializeWalkInGuests() for why.
  private int sampleSpan = FULL_SAMPLE_SPAN_MINUTES;

  public ListInterface<WalkInGuest> initializeWalkInGuests() {
    ListInterface<WalkInGuest> walkInQueue = new ArrayList<>();

    // Arrivals are seeded relative to the moment the program is first run, not
    // to fixed clock times. A guest seeded at "12:05" would show a waiting time
    // of ten hours if the system were demonstrated at 22:00; seeding them
    // "35 minutes ago" instead keeps every waiting time realistic whenever the
    // program is run. It also guarantees the arrivals fall on today's date, so
    // the "today" filters in the reports always have data.
    // The sample spans the 300 minutes before "now". Run just after midnight
    // that would reach back into yesterday, and the "today" filters in the
    // reports would then miss the earliest guests.
    //
    // The fix is to compress the sample rather than move it: the arrivals are
    // scaled to fit between midnight and now whenever less than 300 minutes of
    // today have passed. Every arrival therefore stays in the past - so waiting
    // times are real - while still falling on today's date. Anchoring "now"
    // forward instead would put arrivals in the future and show every guest as
    // having waited 0 minutes.
    LocalDateTime now = LocalDateTime.now();
    int minutesSinceMidnight = now.getHour() * 60 + now.getMinute();

    // Never scale to zero: a run in the first minute of the day would otherwise
    // collapse every arrival onto the same instant.
    sampleSpan = Math.min(FULL_SAMPLE_SPAN_MINUTES, Math.max(minutesSinceMidnight, 1));

    // --- Already served (arrived a while back, served shortly after) ---
    walkInQueue.add(served("WG1001", "Tan Chee Yan", "012-3456789",
        false, null, minutesAgo(now, 300), minutesAgo(now, 287)));
    walkInQueue.add(served("WG1002", "Lim Yong Le", "013-2233445",
        false, null, minutesAgo(now, 296), minutesAgo(now, 269)));
    walkInQueue.add(served("WG1003", "Nur Aisyah binti Rahman", "011-98765432",
        true, "Travelling with infant or young children",
        minutesAgo(now, 291), minutesAgo(now, 285)));
    walkInQueue.add(served("WG1004", "Ivan Wong", "016-7788990",
        false, null, minutesAgo(now, 288), minutesAgo(now, 245)));
    walkInQueue.add(served("WG1005", "Chong Zhi Ying", "014-5566778",
        false, null, minutesAgo(now, 283), minutesAgo(now, 265)));
    walkInQueue.add(served("WG1006", "Rajesh Kumar", "017-3344556",
        true, "Wheelchair / mobility assistance", minutesAgo(now, 279), minutesAgo(now, 276)));
    walkInQueue.add(served("WG1012", "Lee Chong Wei", "012-4455663",
        false, null, minutesAgo(now, 272), minutesAgo(now, 241)));
    walkInQueue.add(served("WG1013", "Aminah binti Yusof", "013-7766554",
        false, null, minutesAgo(now, 266), minutesAgo(now, 238)));
    walkInQueue.add(served("WG1014", "Devi Priya", "018-2299887",
        true, "Medical or emergency situation", minutesAgo(now, 260), minutesAgo(now, 256)));
    walkInQueue.add(served("WG1015", "Kelvin Tan", "016-3311225",
        false, null, minutesAgo(now, 254), minutesAgo(now, 209)));
    walkInQueue.add(served("WG1016", "Hafiz bin Ismail", "019-5544332",
        false, null, minutesAgo(now, 247), minutesAgo(now, 216)));
    walkInQueue.add(served("WG1017", "Michelle Yeoh", "011-24681357",
        false, null, minutesAgo(now, 240), minutesAgo(now, 203)));
    walkInQueue.add(served("WG1018", "Suresh Nair", "017-8811994",
        true, "Elderly or unwell guest", minutesAgo(now, 232), minutesAgo(now, 227)));
    walkInQueue.add(served("WG1019", "Ong Wei Xuan", "012-6677889",
        false, null, minutesAgo(now, 225), minutesAgo(now, 191)));
    walkInQueue.add(served("WG1020", "Zainab binti Omar", "013-9900112",
        false, null, minutesAgo(now, 218), minutesAgo(now, 184)));
    walkInQueue.add(served("WG1021", "Bryan Ng", "018-7733669",
        false, null, minutesAgo(now, 210), minutesAgo(now, 173)));
    walkInQueue.add(served("WG1022", "Priya Ramasamy", "016-4422881",
        true, "Complaint escalation", minutesAgo(now, 203), minutesAgo(now, 199)));
    walkInQueue.add(served("WG1023", "Cheah Jun Hao", "019-1177335",
        false, null, minutesAgo(now, 196), minutesAgo(now, 158)));
    walkInQueue.add(served("WG1024", "Nadia Hassan", "011-55668899",
        false, null, minutesAgo(now, 188), minutesAgo(now, 149)));
    walkInQueue.add(served("WG1025", "Tommy Lim", "012-2244668",
        false, null, minutesAgo(now, 181), minutesAgo(now, 140)));
    walkInQueue.add(served("WG1026", "Sarah Abdullah", "017-9933771",
        false, null, minutesAgo(now, 174), minutesAgo(now, 132)));
    walkInQueue.add(served("WG1027", "Vincent Chong", "013-6688442",
        true, "Wheelchair / mobility assistance", minutesAgo(now, 167), minutesAgo(now, 163)));
    walkInQueue.add(served("WG1028", "Farhana Idris", "018-3355779",
        false, null, minutesAgo(now, 159), minutesAgo(now, 118)));
    walkInQueue.add(served("WG1029", "Anand Krishnan", "016-8844226",
        false, null, minutesAgo(now, 152), minutesAgo(now, 110)));
    walkInQueue.add(served("WG1030", "Yap Siew Ling", "019-2266448",
        false, null, minutesAgo(now, 145), minutesAgo(now, 101)));

    // --- Left before being served ---
    walkInQueue.add(cancelled("WG1007", "Siti Nurhaliza", "019-2211334",
        false, null, minutesAgo(now, 140)));
    walkInQueue.add(cancelled("WG1031", "Marcus Teoh", "012-7799113",
        false, null, minutesAgo(now, 128)));
    walkInQueue.add(cancelled("WG1032", "Halimah binti Saad", "013-4477882",
        false, null, minutesAgo(now, 115)));
    walkInQueue.add(cancelled("WG1033", "Ganesh Pillai", "017-5566993",
        true, "Travelling with infant or young children", minutesAgo(now, 104)));

    // --- Still waiting ---
    // Urgent cases sit ahead of every normal guest, and within each group the
    // earliest arrival comes first - the same order registerWalkIn() produces,
    // so the seeded queue is indistinguishable from one built by hand.
    walkInQueue.add(waiting("WG1034", "Goh Mei Ling", "012-8899001",
        true, "Elderly or unwell guest", minutesAgo(now, 96)));
    walkInQueue.add(waiting("WG1035", "Rosnah binti Karim", "018-6622447",
        true, "Wheelchair / mobility assistance", minutesAgo(now, 74)));
    walkInQueue.add(waiting("WG1036", "Jason Chew", "016-1133557",
        true, "Travelling with infant or young children", minutesAgo(now, 52)));
    walkInQueue.add(waiting("WG1037", "Kavitha Raj", "019-8877665",
        true, "Complaint escalation", minutesAgo(now, 27)));

    walkInQueue.add(waiting("WG1038", "Daniel Lee", "018-4455667",
        false, null, minutesAgo(now, 92)));
    walkInQueue.add(waiting("WG1039", "Farah Aziz", "011-33445566",
        false, null, minutesAgo(now, 87)));
    walkInQueue.add(waiting("WG1040", "Wong Kah Hoe", "016-1122334",
        false, null, minutesAgo(now, 81)));
    walkInQueue.add(waiting("WG1041", "Amirul bin Rashid", "012-9911224",
        false, null, minutesAgo(now, 76)));
    walkInQueue.add(waiting("WG1042", "Christine Lau", "013-8822446",
        false, null, minutesAgo(now, 70)));
    walkInQueue.add(waiting("WG1043", "Ravi Chandran", "017-2255889",
        false, null, minutesAgo(now, 65)));
    walkInQueue.add(waiting("WG1044", "Noraini binti Jamal", "018-4466228",
        false, null, minutesAgo(now, 59)));
    walkInQueue.add(waiting("WG1045", "Edwin Soh", "016-7711335",
        false, null, minutesAgo(now, 54)));
    walkInQueue.add(waiting("WG1046", "Tan Li Wen", "019-3388776",
        false, null, minutesAgo(now, 48)));
    walkInQueue.add(waiting("WG1047", "Mohd Faizal bin Latif", "011-77992244",
        false, null, minutesAgo(now, 43)));
    walkInQueue.add(waiting("WG1048", "Serena Koh", "012-5533991",
        false, null, minutesAgo(now, 37)));
    walkInQueue.add(waiting("WG1049", "Bala Subramaniam", "013-1199663",
        false, null, minutesAgo(now, 32)));
    walkInQueue.add(waiting("WG1050", "Low Jia Hui", "018-9955117",
        false, null, minutesAgo(now, 24)));
    walkInQueue.add(waiting("WG1051", "Aisyah binti Roslan", "016-2277449",
        false, null, minutesAgo(now, 19)));
    walkInQueue.add(waiting("WG1052", "Gerard Fernandez", "017-6644882",
        false, null, minutesAgo(now, 13)));
    walkInQueue.add(waiting("WG1053", "Chin Mei Fong", "019-4411778",
        false, null, minutesAgo(now, 7)));
    walkInQueue.add(waiting("WG1054", "Khairul bin Anuar", "012-3366995",
        false, null, minutesAgo(now, 5)));
    walkInQueue.add(waiting("WG1055", "Melissa Tang", "018-1144773",
        false, null, minutesAgo(now, 3)));
    walkInQueue.add(waiting("WG1056", "Arun Selvam", "016-9922556",
        false, null, minutesAgo(now, 1)));

    return walkInQueue;
  }

  /**
   * A moment a given number of minutes before the reference time, scaled to fit
   * the available sample window, with the seconds stripped so displayed waiting
   * times are whole minutes.
   *
   * When the whole 300-minute window fits before "now", the offset is used as
   * written. Closer to midnight the window is compressed - an arrival written
   * as "300 minutes ago" lands at the start of the available span and one
   * written as "1 minute ago" stays nearest to now - so the sample keeps its
   * shape while remaining entirely within today.
   *
   * @param reference the moment the sample is anchored to, normally now
   * @param sampleSpan how many minutes are actually available before reference
   * @param minutes how far back this arrival sits in the full 300-minute window
   */
  private LocalDateTime minutesAgo(LocalDateTime reference, int minutes) {
    long scaled = Math.round((double) minutes * sampleSpan / FULL_SAMPLE_SPAN_MINUTES);
    return reference.minusMinutes(scaled).withSecond(0).withNano(0);
  }

  private WalkInGuest waiting(String guestId, String name, String contactNumber,
      boolean urgent, String urgencyReason, LocalDateTime arrivalTime) {
    return new WalkInGuest(guestId, name, contactNumber, urgent, urgencyReason, arrivalTime);
  }

  private WalkInGuest served(String guestId, String name, String contactNumber,
      boolean urgent, String urgencyReason, LocalDateTime arrivalTime,
      LocalDateTime servedTime) {
    WalkInGuest guest = waiting(guestId, name, contactNumber, urgent, urgencyReason, arrivalTime);
    guest.setServedTime(servedTime);
    guest.setStatus(WalkInGuest.STATUS_SERVED);
    return guest;
  }

  private WalkInGuest cancelled(String guestId, String name, String contactNumber,
      boolean urgent, String urgencyReason, LocalDateTime arrivalTime) {
    WalkInGuest guest = waiting(guestId, name, contactNumber, urgent, urgencyReason, arrivalTime);
    guest.setStatus(WalkInGuest.STATUS_CANCELLED);
    return guest;
  }
}
