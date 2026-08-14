package adt;

import entity.Booking;

/**
 * @author Yong Le
 */
public interface BookingBSTInterface {

    boolean add(Booking booking);

    Booking search(String confirmationNumber);

    boolean contains(String confirmationNumber);

    ListInterface<Booking> getAllBookings();

    int getNumberOfEntries();

    boolean isEmpty();
}