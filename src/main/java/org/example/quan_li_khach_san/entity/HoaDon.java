package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hoa_don")
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_hoa_don")
    private Long maHoaDon;

    @Column(name = "ma_dat_phong", nullable = false)
    private Long maDatPhong;

    // Quan hệ ManyToOne liên kết với bảng dat_phong
    @ManyToOne
    @JoinColumn(name = "ma_dat_phong", insertable = false, updatable = false)
    private DatPhong datPhong;

    @Column(name = "ngay_lap")
    private LocalDateTime ngayLap;

    @Column(name = "tien_phong")
    private double tienPhong;

    @Column(name = "tien_dich_vu")
    private double tienDichVu;

    @Column(name = "tong_tien")
    private double tongTien;

    @Column(name = "trang_thai")
    private String trangThai;

    public HoaDon() {}

    public Long getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(Long maHoaDon) { this.maHoaDon = maHoaDon; }

    public Long getMaDatPhong() { return maDatPhong; }
    public void setMaDatPhong(Long maDatPhong) { this.maDatPhong = maDatPhong; }

    public DatPhong getDatPhong() { return datPhong; }
    public void setDatPhong(DatPhong datPhong) { this.datPhong = datPhong; }

    public LocalDateTime getNgayLap() { return ngayLap; }
    public void setNgayLap(LocalDateTime ngayLap) { this.ngayLap = ngayLap; }

    public double getTienPhong() { return tienPhong; }
    public void setTienPhong(double tienPhong) { this.tienPhong = tienPhong; }

    public double getTienDichVu() { return tienDichVu; }
    public void setTienDichVu(double tienDichVu) { this.tienDichVu = tienDichVu; }

    public double getTongTien() { return tongTien; }
    public void setTongTien(double tongTien) { this.tongTien = tongTien; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getMaHienThi() {
        if (maHoaDon == null) return "HD----";
        return String.format("HD%04d", maHoaDon);
    }
}