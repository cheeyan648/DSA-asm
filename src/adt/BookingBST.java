package adt;

import entity.Booking;

/**
 * Binary Search Tree for storing and searching bookings
 * using the 8-digit confirmation number.
 *
 * @author Yong Le
 */
public class BookingBST implements BookingBSTInterface {

    private Node root;
    private int numberOfEntries;

    private class Node {

        private Booking data;
        private Node left;
        private Node right;

        public Node(Booking data) {
            this.data = data;
            left = null;
            right = null;
        }
    }

    public BookingBST() {
        root = null;
        numberOfEntries = 0;
    }

    @Override
    public boolean add(Booking booking) {

        if (booking == null) {
            return false;
        }

        if (root == null) {
            root = new Node(booking);
            numberOfEntries++;
            return true;
        }

        Node current = root;

        while (true) {

            int comparison = booking.getConfirmationNumber()
                    .compareTo(current.data.getConfirmationNumber());

            if (comparison == 0) {
                return false;
            } else if (comparison < 0) {

                if (current.left == null) {
                    current.left = new Node(booking);
                    numberOfEntries++;
                    return true;
                }

                current = current.left;

            } else {

                if (current.right == null) {
                    current.right = new Node(booking);
                    numberOfEntries++;
                    return true;
                }

                current = current.right;
            }
        }
    }

    @Override
    public Booking search(String confirmationNumber) {

        Node current = root;

        while (current != null) {

            int comparison = confirmationNumber.compareTo(
                    current.data.getConfirmationNumber());

            if (comparison == 0) {
                return current.data;
            } else if (comparison < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }

        return null;
    }

    @Override
    public boolean contains(String confirmationNumber) {
        return search(confirmationNumber) != null;
    }

    @Override
    public ListInterface<Booking> getAllBookings() {

        ListInterface<Booking> bookingList = new ArrayList<>();

        inOrderTraversal(root, bookingList);

        return bookingList;
    }

    private void inOrderTraversal(
            Node current,
            ListInterface<Booking> bookingList) {

        if (current != null) {

            inOrderTraversal(current.left, bookingList);

            bookingList.add(current.data);

            inOrderTraversal(current.right, bookingList);
        }
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }
}