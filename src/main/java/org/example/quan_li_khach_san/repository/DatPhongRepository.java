package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.DatPhong;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface DatPhongRepository extends JpaRepository<DatPhong, Long> {
    @EntityGraph(attributePaths = {"khachHang", "room"})
    @Query("select d from DatPhong d where d.ngayTraPhong is null and d.trangThai = 'Đang lưu trú' order by d.room.soPhong")
    List<DatPhong> findDangLuuTru();
    @EntityGraph(attributePaths = {"khachHang", "room"})
    Optional<DatPhong> findFirstBySoPhongAndTrangThaiOrderByNgayNhanPhongDesc(String soPhong, String trangThai);
    List<DatPhong> findByTrangThai(String trangThai);
    // Gọi dịch vụ và trả phòng cần dùng chung khóa lượt ở.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DatPhong d where d.maDatPhong = :id")
    Optional<DatPhong> findLockedById(@Param("id") Long id);
}
=======
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatPhongRepository extends JpaRepository<DatPhong, Integer> {

    // 1. Lấy danh sách các phòng đang trống (hoặc có thể tùy chỉnh tên trạng thái cho khớp entity Room/DatPhong)
    @Query("SELECT d FROM DatPhong d WHERE d.trangThai = 'Trong'")
    List<DatPhong> findDatPhongByTrangThaiTrong();

    // 2. Cập nhật trạng thái đặt phòng
    @Modifying
    @Query("UPDATE DatPhong d SET d.trangThai = :trangThai WHERE d.maDatPhong = :maDatPhong")
    void updateTrangThai(@Param("maDatPhong") int maDatPhong, @Param("trangThai") String trangThai);
}
>>>>>>> 6026e3ee81e4d8efd379d4baf1aa115026e76d3e
