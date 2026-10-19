package com.ctu.service;

import com.ctu.exception.PaymentFailedException;
import com.ctu.exception.RoomNotAvailableException;
import com.ctu.model.BookingReceipt;
import com.ctu.model.Room;
import com.ctu.repository.RoomRepository;
import java.util.UUID;

public class BookingService {
    private final RoomRepository roomRepository;
    private final PaymentGateway paymentGateway;
    public BookingService(RoomRepository roomRepository, PaymentGateway paymentGateway) {
        this.roomRepository = roomRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Dat phong khach san
     * Pre-conditions: roomId not null, nights > 0, creditCard not null & not empty
     */
    public BookingReceipt bookRoom(String roomId, int nights, String creditCard) {
        // Edge Case 1: Kiem tra tham so dau vao
        if (roomId == null || nights <= 0 || creditCard == null || creditCard.trim().isEmpty()) {
            throw new IllegalArgumentException("Tham so dat phong khong hop le!");
        }

        // Edge Case 2: Tra cuu phong
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotAvailableException("Khong tim thay phong: " + roomId));

        // Edge Case 3: Kiem tra phong con trong khong
        if (!room.isAvailable()) {
            throw new RoomNotAvailableException("Phong da duoc dat truoc do: " + roomId);
        }

        // Tinh tien
        double totalAmount = room.getPricePerNight() * nights;

        // Edge Case 4: Xu ly thanh toan
        boolean paymentSuccess = paymentGateway.charge(creditCard, totalAmount);
        if (!paymentSuccess) {
            throw new PaymentFailedException("Thanh toan qua the credit card that bai!");
        }

        // Cap nhat trang thai phong & Luu CSDL
        room.setAvailable(false);
        roomRepository.save(room);
        return new BookingReceipt(UUID.randomUUID().toString(), roomId, totalAmount);
    }

}

