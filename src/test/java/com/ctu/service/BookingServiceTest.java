package com.ctu.service;

import com.ctu.exception.PaymentFailedException;
import com.ctu.exception.RoomNotAvailableException;
import com.ctu.model.BookingReceipt;
import com.ctu.model.Room;
import com.ctu.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Test Suite")
class BookingServiceTest {

    private static final String ROOM_ID = "R101";
    private static final String CARD = "4111-1111-1111-1111";

    @Mock
    private RoomRepository roomRepository;   // Gia lap CSDL phong

    @Mock
    private PaymentGateway paymentGateway;   // Gia lap cong thanh toan

    @InjectMocks
    private BookingService bookingService;   // Doi tuong that, duoc tiem 2 mock

    // ================= 1. LUONG THANH CONG =================

    @Test
    @DisplayName("Dat phong thanh cong: phong chuyen sang da dat, charge va save goi dung 1 lan")
    void testBookRoom_Success() {
        // ARRANGE: phong 500k/dem dang trong, thanh toan 3 dem = 1.500k thanh cong
        Room room = new Room(ROOM_ID, 500_000, true);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(paymentGateway.charge(CARD, 1_500_000.0)).thenReturn(true);

        // ACT
        BookingReceipt receipt = bookingService.bookRoom(ROOM_ID, 3, CARD);

        // ASSERT: trang thai phong + hoa don + tuong tac voi mock
        assertFalse(room.isAvailable(), "Phong phai chuyen sang da dat");
        assertEquals(ROOM_ID, receipt.getRoomId());
        assertEquals(1_500_000.0, receipt.getTotalAmount(), 0.001);
        assertNotNull(receipt.getBookingId());
        verify(paymentGateway, times(1)).charge(CARD, 1_500_000.0);
        verify(roomRepository, times(1)).save(room);
    }

    // ================= 2. NGOAI LE NGHIEP VU =================

    @Test
    @DisplayName("Nem RoomNotAvailableException khi khong tim thay phong")
    void testBookRoom_RoomNotFound_ThrowsException() {
        // ARRANGE
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        // ACT & ASSERT
        RoomNotAvailableException ex = assertThrows(RoomNotAvailableException.class,
                () -> bookingService.bookRoom(ROOM_ID, 2, CARD));
        assertEquals("Khong tim thay phong: " + ROOM_ID, ex.getMessage());

        // Loi thi KHONG duoc tru tien va KHONG duoc luu phong
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nem RoomNotAvailableException khi phong da duoc dat truoc")
    void testBookRoom_RoomAlreadyBooked_ThrowsException() {
        // ARRANGE: phong ton tai nhung isAvailable == false
        Room bookedRoom = new Room(ROOM_ID, 500_000, false);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(bookedRoom));

        // ACT & ASSERT
        RoomNotAvailableException ex = assertThrows(RoomNotAvailableException.class,
                () -> bookingService.bookRoom(ROOM_ID, 2, CARD));
        assertEquals("Phong da duoc dat truoc do: " + ROOM_ID, ex.getMessage());

        verify(paymentGateway, never()).charge(anyString(), anyDouble());
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nem PaymentFailedException khi thanh toan that bai, phong van con trong")
    void testBookRoom_PaymentFailed_ThrowsException() {
        // ARRANGE: phong trong, nhung cong thanh toan tu choi (2 dem = 1.000k)
        Room room = new Room(ROOM_ID, 500_000, true);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(paymentGateway.charge(CARD, 1_000_000.0)).thenReturn(false);

        // ACT & ASSERT
        PaymentFailedException ex = assertThrows(PaymentFailedException.class,
                () -> bookingService.bookRoom(ROOM_ID, 2, CARD));
        assertEquals("Thanh toan qua the credit card that bai!", ex.getMessage());

        // Thanh toan that bai: phong KHONG bi khoa, KHONG luu CSDL
        assertTrue(room.isAvailable(), "Phong phai con trong khi thanh toan that bai");
        verify(paymentGateway, times(1)).charge(CARD, 1_000_000.0);
        verify(roomRepository, never()).save(any());
    }

    // ================= 3. THAM SO BIEN (BOUNDARY) =================

    @ParameterizedTest(name = "nights = {0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("Nem IllegalArgumentException khi so dem <= 0")
    void testBookRoom_InvalidNights_ThrowsException(int nights) {
        // ARRANGE: khong stub gi, vi service phai tu choi truoc khi goi mock

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookRoom(ROOM_ID, nights, CARD));
        assertEquals("Tham so dat phong khong hop le!", ex.getMessage());
        verifyNoInteractions(roomRepository, paymentGateway);
    }

    @ParameterizedTest(name = "creditCard = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Nem IllegalArgumentException khi the null, rong hoac chi co khoang trang")
    void testBookRoom_NullOrEmptyCreditCard_ThrowsException(String creditCard) {
        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookRoom(ROOM_ID, 2, creditCard));
        assertEquals("Tham so dat phong khong hop le!", ex.getMessage());
        verifyNoInteractions(roomRepository, paymentGateway);
    }

    @Test
    @DisplayName("Nem IllegalArgumentException khi roomId null")
    void testBookRoom_NullRoomId_ThrowsException() {
        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookRoom(null, 2, CARD));
        assertEquals("Tham so dat phong khong hop le!", ex.getMessage());
        verifyNoInteractions(roomRepository, paymentGateway);
    }
}