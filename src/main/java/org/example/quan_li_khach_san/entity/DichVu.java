package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "dich_vu")
public class DichVu {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dich_vu")
    private Long maDichVu;
    @Column(name = "ten_dich_vu", nullable = false, length = 100)
    private String tenDichVu;
    @Column(name = "gia_dich_vu", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaDichVu;
    @Column(name = "dang_kinh_doanh", nullable = false)
    private boolean dangKinhDoanh = true;
    public Long getMaDichVu() { return maDichVu; }
    public String getTenDichVu() { return tenDichVu; }
    public void setTenDichVu(String value) { tenDichVu = value; }
    public BigDecimal getGiaDichVu() { return giaDichVu; }
    public void setGiaDichVu(BigDecimal value) { giaDichVu = value; }
    public boolean isDangKinhDoanh() { return dangKinhDoanh; }
    public void setDangKinhDoanh(boolean value) { dangKinhDoanh = value; }
}
