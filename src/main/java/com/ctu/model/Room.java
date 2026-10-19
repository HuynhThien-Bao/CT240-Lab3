package com.ctu.model;

public class Room {

    private String roomId;
    private double pricePerNight;
    private boolean isAvailable;

    public Room(String roomId, double pricePerNight, boolean isAvailable) {

        this.roomId = roomId;
        this.pricePerNight = pricePerNight;
        this.isAvailable = isAvailable;

    }

    public String getRoomId() { return roomId; }
    public double getPricePerNight() { return pricePerNight; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

}
