package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "su_dung_dich_vu")
public class SuDungDichVu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dat_phong")
    private DatPhong datPhong;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_dich_vu")
    private DichVu dichVu;

    @Column(name = "so_phong", nullable = false, length = 20)
    private String soPhong;
    @Column(name = "so_giay_to", nullable = false, length = 50)
    private String soGiayTo;
    @Column(name = "so_luong")
    private Integer soLuong;
    @Column(name = "thanh_tien", precision = 15, scale = 2)
    private BigDecimal thanhTien;
    @Column(name = "ngay_su_dung")
    private LocalDateTime ngaySuDung;

    public Long getId() { return id; }
    public DatPhong getDatPhong() { return datPhong; }
    public void setDatPhong(DatPhong value) { datPhong = value; }
    public DichVu getDichVu() { return dichVu; }
    public void setDichVu(DichVu value) { dichVu = value; }
    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String value) { soPhong = value; }
    public String getSoGiayTo() { return soGiayTo; }
    public void setSoGiayTo(String value) { soGiayTo = value; }
    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer value) { soLuong = value; }
    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal value) { thanhTien = value; }
    public LocalDateTime getNgaySuDung() { return ngaySuDung; }
    public void setNgaySuDung(LocalDateTime value) { ngaySuDung = value; }

    // Đơn giá được lấy lại từ khoản tiền đã lưu, không lấy giá mới trong danh mục.
    public BigDecimal getDonGiaLucGoi() {
        if (thanhTien == null || soLuong == null || soLuong <= 0) return null;
        return thanhTien.divide(BigDecimal.valueOf(soLuong), 2, RoundingMode.HALF_UP);
    }
    public String getTenDichVu() { return dichVu.getTenDichVu(); }
}
