package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    @Query("SELECT r.soPhong FROM Room r WHERE r.trangThai = 'Còn trống'")
    List<String> findSoPhongByTrangThaiTrong();

    @Modifying
    @Query("UPDATE Room r SET r.trangThai = :trangThai WHERE r.soPhong = :soPhong")
    void updateTrangThai(@Param("soPhong") String soPhong, @Param("trangThai") String trangThai);
}