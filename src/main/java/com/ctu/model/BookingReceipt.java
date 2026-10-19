package com.ctu.model;

public class BookingReceipt {
        private String bookingId;
        private String roomId;
        private double totalAmount;
        public BookingReceipt(String bookingId, String roomId, double totalAmount) {
            this.bookingId = bookingId;
            this.roomId = roomId;
            this.totalAmount = totalAmount;
        }

        public String getBookingId() { return bookingId; }
        public String getRoomId() { return roomId; }
        public double getTotalAmount() { return totalAmount; }
}
