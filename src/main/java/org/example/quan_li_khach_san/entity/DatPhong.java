package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dat_phong")
public class DatPhong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dat_phong")
    private Integer maDatPhong;

    @Column(name = "ma_khach_hang")
    private Long maKhachHang; // Đồng bộ kiểu Long với KhachHang.maKhachHang

    @Column(name = "ma_phong")
    private String maPhong; // Đồng bộ kiểu String với Room.soPhong

    private LocalDate ngayNhanPhong;
    private LocalDate ngayTraPhong;
    private String trangThai; // Ví dụ: "Dang su dung", "Da tra"

    // Constructor mặc định
    public DatPhong() {}

    // Getters và Setters
    public Integer getMaDatPhong() { return maDatPhong; }
    public void setMaDatPhong(Integer maDatPhong) { this.maDatPhong = maDatPhong; }

    public Long getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(Long maKhachHang) { this.maKhachHang = maKhachHang; }

    public String getMaPhong() { return maPhong; }
    public void setMaPhong(String maPhong) { this.maPhong = maPhong; }

    public LocalDate getNgayNhanPhong() { return ngayNhanPhong; }
    public void setNgayNhanPhong(LocalDate ngayNhanPhong) { this.ngayNhanPhong = ngayNhanPhong; }

    public LocalDate getNgayTraPhong() { return ngayTraPhong; }
    public void setNgayTraPhong(LocalDate ngayTraPhong) { this.ngayTraPhong = ngayTraPhong; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}