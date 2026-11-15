package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dat_phong")
public class DatPhong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dat_phong")
    private Long maDatPhong;
    @Column(name = "ma_khach_hang")
    private Long maKhachHang;
    @ManyToOne
    @JoinColumn(name = "ma_khach_hang", insertable = false, updatable = false)
    private KhachHang khachHang;
    @Column(name = "so_phong")
    private String soPhong;
    @ManyToOne
    @JoinColumn(name = "so_phong", insertable = false, updatable = false)
    private Room room;
    @Column(name = "ngay_nhan_phong")
    private LocalDateTime ngayNhanPhong;
    @Column(name = "ngay_tra_phong")
    private LocalDateTime ngayTraPhong;
    @Column(name = "trang_thai")
    private String trangThai;

    public Long getMaDatPhong() { return maDatPhong; }
    public void setMaDatPhong(Long value) { maDatPhong = value; }
    public Long getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(Long value) { maKhachHang = value; }
    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang value) {
        khachHang = value;
        maKhachHang = value == null ? null : value.getMaKhachHang();
    }
    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String value) { soPhong = value; }
    public Room getRoom() { return room; }
    public void setRoom(Room value) {
        room = value;
        soPhong = value == null ? null : value.getSoPhong();
    }
    public LocalDateTime getNgayNhanPhong() { return ngayNhanPhong; }
    public void setNgayNhanPhong(LocalDateTime value) { ngayNhanPhong = value; }
    public LocalDateTime getNgayTraPhong() { return ngayTraPhong; }
    public void setNgayTraPhong(LocalDateTime value) { ngayTraPhong = value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { trangThai = value; }
    public boolean isDangLuuTru() { return ngayTraPhong == null && "Đang lưu trú".equals(trangThai); }
}
