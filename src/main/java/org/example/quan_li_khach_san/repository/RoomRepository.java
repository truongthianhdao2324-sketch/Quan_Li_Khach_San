package org.example.quan_li_khach_san.repository;

import org.springframework.stereotype.Repository;
import org.example.quan_li_khach_san.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;


@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    @Query("SELECT r.soPhong FROM Room r WHERE r.trangThai = 'Còn trống'")
    List<String> findSoPhongByTrangThaiTrong();

    @Modifying
    @Query("UPDATE Room r SET r.trangThai = :trangThai WHERE r.soPhong = :soPhong")
    void updateTrangThai(@Param("soPhong") String soPhong, @Param("trangThai") String trangThai);

    // Thống kê số phòng theo trạng thái
    @Query("SELECT r.trangThai, COUNT(r) FROM Room r GROUP BY r.trangThai")
    List<Object[]> thongKeTheoTrangThai();

    // Thống kê số phòng theo loại phòng
    @Query("SELECT r.loaiPhong, COUNT(r) FROM Room r GROUP BY r.loaiPhong")
    List<Object[]> thongKeTheoLoaiPhong();

    @Query("SELECT r FROM Room r WHERE " +
            "(:soPhong IS NULL OR LOWER(r.soPhong) LIKE LOWER(CONCAT('%', :soPhong, '%'))) AND " +
            "(:loaiPhong IS NULL OR r.loaiPhong = :loaiPhong) AND " +
            "(:trangThai IS NULL OR r.trangThai = :trangThai) " +
            "ORDER BY r.soPhong")
    List<Room> timKiemVaLoc(
            @Param("soPhong") String soPhong,
            @Param("loaiPhong") String loaiPhong,
            @Param("trangThai") String trangThai
    );

    @Query("SELECT DISTINCT r.loaiPhong FROM Room r ORDER BY r.loaiPhong")
    List<String> findAllLoaiPhong();

    long countByTrangThai(String trangThai);

    // Khóa bản ghi phòng trong lúc xử lý cập nhật trạng thái
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.soPhong = :soPhong")
    Optional<Room> findBySoPhongForUpdate(@Param("soPhong") String soPhong);
}