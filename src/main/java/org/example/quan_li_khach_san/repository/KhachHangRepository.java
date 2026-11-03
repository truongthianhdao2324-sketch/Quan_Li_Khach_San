package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {
    List<KhachHang> findByHoTenContainingOrSoGiayToContaining(String hoTen, String soGiayTo);
    Optional<KhachHang> findBySoGiayTo(String soGiayTo);
    void deleteBySoGiayTo(String soGiayTo);
}