package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "dat_phong")
public class DatPhong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dat_phong")
    private Long maDatPhong;

    @Column(name = "ma_khach_hang")
    private Long maKhachHang;

    // Quan hệ lấy thông tin Khách hàng
    @ManyToOne
    @JoinColumn(name = "ma_khach_hang", insertable = false, updatable = false)
    private KhachHang khachHang;

    @Column(name = "so_phong")
    private String soPhong;

    // Quan hệ lấy thông tin Phòng (giá phòng, loại phòng)
    @ManyToOne
    @JoinColumn(name = "so_phong", insertable = false, updatable = false)
    private Room room;

    @Column(name = "ngay_nhan_phong")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime ngayNhanPhong;

    @Column(name = "ngay_tra_phong")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime ngayTraPhong;

    @Column(name = "trang_thai")
    private String trangThai;
    @Column(name = "ma_phong")
    private Integer maPhong;



    public DatPhong() {}

    public Long getMaDatPhong() { return maDatPhong; }
    public void setMaDatPhong(Long maDatPhong) { this.maDatPhong = maDatPhong; }

    public Long getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(Long maKhachHang) { this.maKhachHang = maKhachHang; }

    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }

    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String soPhong) { this.soPhong = soPhong; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public LocalDateTime getNgayNhanPhong() { return ngayNhanPhong; }
    public void setNgayNhanPhong(LocalDateTime ngayNhanPhong) { this.ngayNhanPhong = ngayNhanPhong; }

    public LocalDateTime getNgayTraPhong() { return ngayTraPhong; }
    public void setNgayTraPhong(LocalDateTime ngayTraPhong) { this.ngayTraPhong = ngayTraPhong; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public Integer getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(Integer maPhong) {
        this.maPhong = maPhong;
    }


    public boolean isDangLuuTru() {
        return this.ngayTraPhong == null
                && this.trangThai != null
                && "Đang lưu trú".equalsIgnoreCase(this.trangThai.trim());
    }
}