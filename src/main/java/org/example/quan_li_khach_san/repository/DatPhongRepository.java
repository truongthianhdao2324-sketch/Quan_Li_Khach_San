package org.example.quan_li_khach_san.repository;

import jakarta.persistence.LockModeType;
import org.example.quan_li_khach_san.entity.DatPhong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatPhongRepository extends JpaRepository<DatPhong, Long> {

    List<DatPhong> findByTrangThai(String trangThai);

    long countByTrangThai(String trangThai);

    @Query("SELECT d FROM DatPhong d WHERE d.trangThai = 'Đang lưu trú'")
    List<DatPhong> findDangLuuTru();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DatPhong d WHERE d.maDatPhong = :id")
    Optional<DatPhong> findLockedById(@Param("id") Long id);

    Optional<DatPhong> findFirstBySoPhongAndTrangThaiOrderByNgayNhanPhongDesc(String soPhong, String trangThai);
}