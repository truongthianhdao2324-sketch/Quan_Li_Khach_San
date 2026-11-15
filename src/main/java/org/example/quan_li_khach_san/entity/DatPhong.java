package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
@Entity
@Table(name = "dat_phong")
public class DatPhong {
    @Id // <--- THÊM DÒNG NÀY VÀO
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maDatPhong; // Đổi từ int sang Integer

    @Column(name = "ma_khach_hang")
    private Integer maKhachHang; // Đổi từ int sang Integer

    @Column(name = "ma_phong")
    private Integer maPhong;
        private LocalDate ngayNhanPhong;
        private LocalDate ngayTraPhong;
        private String trangThai; // Ví dụ: "Dang su dung", "Da tra"

        // Constructor mặc định
        public DatPhong() {
        }

        // Constructor đầy đủ tham số
        public DatPhong(int maDatPhong, int maKhachHang, int maPhong, LocalDate ngayNhanPhong, LocalDate ngayTraPhong, String trangThai) {
            this.maDatPhong = maDatPhong;
            this.maKhachHang = maKhachHang;
            this.maPhong = maPhong;
            this.ngayNhanPhong = ngayNhanPhong;
            this.ngayTraPhong = ngayTraPhong;
            this.trangThai = trangThai;
        }

        // Getter và Setter
        public Integer getMaDatPhong() { return maDatPhong; }
        public void setMaDatPhong(int maDatPhong) { this.maDatPhong = maDatPhong; }

        public Integer getMaKhachHang() { return maKhachHang; }
        public void setMaKhachHang(int maKhachHang) { this.maKhachHang = maKhachHang; }

        public Integer getMaPhong() { return maPhong; }
        public void setMaPhong(int maPhong) { this.maPhong = maPhong; }

        public LocalDate getNgayNhanPhong() { return ngayNhanPhong; }
        public void setNgayNhanPhong(LocalDate ngayNhanPhong) { this.ngayNhanPhong = ngayNhanPhong; }

        public LocalDate getNgayTraPhong() { return ngayTraPhong; }
        public void setNgayTraPhong(LocalDate ngayTraPhong) { this.ngayTraPhong = ngayTraPhong; }

        public String getTrangThai() { return trangThai; }
        public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    }

