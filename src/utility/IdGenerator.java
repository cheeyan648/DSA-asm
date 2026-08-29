package utility;

import adt.ListInterface;

/**
 * Produces the next ID in a series - G0004, BK0008, HK0011 and so on.
 *
 * The next number is taken from the highest suffix already in use, not from
 * how many records there are. Counting would re-issue an ID as soon as
 * anything was deleted, and two records sharing a primary key is the kind of
 * fault that is very hard to trace afterwards.
 *
 * @author Wong Chee Yan
 */
public class IdGenerator {

  private IdGenerator() {
  }

  /**
   * Builds the next ID in a series.
   *
   * @param prefix the letters the ID starts with, e.g. "BK"
   * @param digits how many digits follow the prefix
   * @param existingIds every ID already issued in this series
   * @return the next unused ID
   */
  public static String next(String prefix, int digits, ListInterface<String> existingIds) {
    int highest = 0;

    for (int i = 1; i <= existingIds.getNumberOfEntries(); i++) {
      String id = existingIds.getEntry(i);
      int value = suffixOf(id, prefix);
      if (value > highest) {
        highest = value;
      }
    }

    return format(prefix, digits, highest + 1);
  }

  /**
   * Reads the numeric part of an ID.
   *
   * An ID that does not fit the series - a different prefix, or letters where
   * the digits should be - is ignored rather than rejected, so one malformed
   * record cannot stop new IDs being issued.
   *
   * @return the number, or 0 if the ID does not belong to this series
   */
  private static int suffixOf(String id, String prefix) {
    if (id == null || prefix == null || !id.startsWith(prefix) || id.length() <= prefix.length()) {
      return 0;
    }

    String suffix = id.substring(prefix.length());
    for (int i = 0; i < suffix.length(); i++) {
      if (!Character.isDigit(suffix.charAt(i))) {
        return 0;
      }
    }

    try {
      return Integer.parseInt(suffix);
    } catch (NumberFormatException notANumber) {
      return 0;
    }
  }

  /**
   * Assembles one ID from its parts, padding the number with leading zeros.
   *
   * @param prefix the letters the ID starts with
   * @param digits how many digits follow the prefix
   * @param number the number to use
   * @return the formatted ID
   */
  public static String format(String prefix, int digits, int number) {
    return prefix + String.format("%0" + digits + "d", number);
  }
}
