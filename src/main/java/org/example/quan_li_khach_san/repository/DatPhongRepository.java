package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.DatPhong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatPhongRepository extends JpaRepository<DatPhong, Long> {
    Optional<DatPhong> findFirstBySoPhongAndTrangThaiOrderByNgayNhanPhongDesc(String soPhong, String trangThai);

    List<DatPhong> findByTrangThai(String trangThai);
}