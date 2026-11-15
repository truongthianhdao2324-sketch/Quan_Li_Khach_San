package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.DatPhong;
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