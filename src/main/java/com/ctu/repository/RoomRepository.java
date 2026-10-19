package com.ctu.repository;

import com.ctu.model.Room;
import java.util.Optional;

public interface RoomRepository {
    Optional<Room> findById(String roomId);
    void save(Room room);
}
