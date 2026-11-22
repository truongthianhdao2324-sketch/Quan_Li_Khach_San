package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    @Query("SELECT r.soPhong FROM Room r WHERE r.trangThai = 'Còn trống'")
    List<String> findSoPhongByTrangThaiTrong();

    // Thống kê số phòng theo trạng thái
    @Query("SELECT r.trangThai, COUNT(r) FROM Room r GROUP BY r.trangThai")
    List<Object[]> thongKeTheoTrangThai();

    // Thống kê số phòng theo loại phòng
    @Query("SELECT r.loaiPhong, COUNT(r) FROM Room r GROUP BY r.loaiPhong")
    List<Object[]> thongKeTheoLoaiPhong();

    // Tìm kiếm và lọc phòng
    @Query("SELECT r FROM Room r WHERE " +
            "(:soPhong IS NULL OR LOWER(r.soPhong) LIKE LOWER(CONCAT('%', :soPhong, '%'))) AND " +
            "(:loaiPhong IS NULL OR r.loaiPhong = :loaiPhong) AND " +
            "(:trangThai IS NULL OR r.trangThai = :trangThai) " +
            "ORDER BY " +
            "CASE " +
            "WHEN r.trangThai = 'Đang dọn phòng' THEN 0 " +
            "WHEN r.trangThai = 'Bảo trì' THEN 1 " +
            "WHEN r.trangThai = 'Còn trống' THEN 2 " +
            "WHEN r.trangThai = 'Đã có khách' THEN 3 " +
            "ELSE 4 END, " +
            "CASE WHEN :sapXep = 'giamDan' THEN r.soPhong END DESC, " +
            "CASE WHEN :sapXep = 'tangDan' THEN r.soPhong END ASC")
    List<Room> timKiemVaLoc(
            @Param("soPhong") String soPhong,
            @Param("loaiPhong") String loaiPhong,
            @Param("trangThai") String trangThai,
            @Param("sapXep") String sapXep
    );

    @Query("SELECT DISTINCT r.loaiPhong FROM Room r ORDER BY r.loaiPhong")
    List<String> findAllLoaiPhong();

    long countByTrangThai(String trangThai);

    // Khóa bản ghi phòng khi xử lý cập nhật trạng thái



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.soPhong = :soPhong")
    Optional<Room> findBySoPhongForUpdate(@Param("soPhong") String soPhong);

    // Tương thích với KhachHangController hiện tại
    @Modifying
    @Query("UPDATE Room r SET r.trangThai = :trangThai WHERE r.soPhong = :soPhong")
    void updateTrangThai(
            @Param("soPhong") String soPhong,
            @Param("trangThai") String trangThai
    );
}
=======
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
>>>>>>> 6026e3ee81e4d8efd379d4baf1aa115026e76d3e
