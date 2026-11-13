package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.DichVu;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface DichVuRepository extends JpaRepository<DichVu, Long> {
    Page<DichVu> findByTenDichVuContainingIgnoreCase(String keyword, Pageable pageable);
    List<DichVu> findByDangKinhDoanhTrueOrderByTenDichVuAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DichVu d where d.maDichVu = :id")
    Optional<DichVu> findLockedById(@Param("id") Long id);
}
