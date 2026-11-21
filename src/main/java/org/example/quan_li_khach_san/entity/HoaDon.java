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

    // Tự động tính lại tổng tiền = tiền phòng + tiền dịch vụ mỗi khi Insert hoặc Update
    @PrePersist
    @PreUpdate
    public void capNhatTongTien() {
        this.tienPhong = Math.max(0, this.tienPhong);
        this.tienDichVu = Math.max(0, this.tienDichVu);
        this.tongTien = this.tienPhong + this.tienDichVu;
    }

    public Long getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(Long maHoaDon) { this.maHoaDon = maHoaDon; }

    public Long getMaDatPhong() { return maDatPhong; }
    public void setMaDatPhong(Long maDatPhong) { this.maDatPhong = maDatPhong; }

    public DatPhong getDatPhong() { return datPhong; }
    public void setDatPhong(DatPhong datPhong) { this.datPhong = datPhong; }

    public LocalDateTime getNgayLap() { return ngayLap; }
    public void setNgayLap(LocalDateTime ngayLap) { this.ngayLap = ngayLap; }

    public double getTienPhong() { return tienPhong; }
    public void setTienPhong(double tienPhong) {
        this.tienPhong = Math.max(0, tienPhong);
        this.tongTien = this.tienPhong + this.tienDichVu;
    }

    public double getTienDichVu() { return tienDichVu; }
    public void setTienDichVu(double tienDichVu) {
        this.tienDichVu = Math.max(0, tienDichVu);
        this.tongTien = this.tienPhong + this.tienDichVu;
    }

    public double getTongTien() { return tongTien; }
    public void setTongTien(double tongTien) { this.tongTien = tongTien; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getMaHienThi() {
        if (maHoaDon == null) return "HD----";
        return String.format("HD%04d", maHoaDon);
    }
}