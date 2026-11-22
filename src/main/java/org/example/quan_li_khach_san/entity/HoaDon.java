package org.example.quan_li_khach_san.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hoa_don")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_hoa_don")
    private Long maHoaDon;

    @Column(name = "ma_dat_phong", nullable = false)
    private Long maDatPhong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ma_dat_phong", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DatPhong datPhong;

    @Column(name = "ngay_lap")
    private LocalDateTime ngayLap;

    @Column(name = "tien_phong", nullable = false)
    private Double tienPhong = 0.0;

    @Column(name = "tien_dich_vu")
    private Double tienDichVu = 0.0;

    @Column(name = "tong_tien", nullable = false)
    private Double tongTien = 0.0;

    @Column(name = "trang_thai", length = 30)
    private String trangThai = "Đã thanh toán";

    public HoaDon() {
    }

    public String getMaHienThi() {
        if (maHoaDon == null) {
            return "HD0000";
        }
        return String.format("HD%04d", maHoaDon);
    }

    public Long getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(Long maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public Long getMaDatPhong() {
        return maDatPhong;
    }

    public void setMaDatPhong(Long maDatPhong) {
        this.maDatPhong = maDatPhong;
    }

    public DatPhong getDatPhong() {
        return datPhong;
    }

    public void setDatPhong(DatPhong datPhong) {
        this.datPhong = datPhong;
        if (datPhong != null && datPhong.getMaDatPhong() != null) {
            this.maDatPhong = datPhong.getMaDatPhong();
        }
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime ngayLap) {
        this.ngayLap = ngayLap;
    }

    public Double getTienPhong() {
        return tienPhong;
    }

    public void setTienPhong(Double tienPhong) {
        this.tienPhong = tienPhong;
    }

    public Double getTienDichVu() {
        return tienDichVu;
    }

    public void setTienDichVu(Double tienDichVu) {
        this.tienDichVu = tienDichVu;
    }

    public Double getTongTien() {
        return tongTien;
    }

    public void setTongTien(Double tongTien) {
        this.tongTien = tongTien;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}