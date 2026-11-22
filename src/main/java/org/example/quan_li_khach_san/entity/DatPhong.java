package org.example.quan_li_khach_san.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dat_phong")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DatPhong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dat_phong")
    private Long maDatPhong;

    @Column(name = "ma_khach_hang", nullable = false)
    private Long maKhachHang;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ma_khach_hang", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private KhachHang khachHang;

    @Column(name = "so_phong", nullable = false, length = 20)
    private String soPhong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "so_phong", referencedColumnName = "so_phong", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Room room;

    @Transient
    private Integer maPhong;

    @Column(name = "ngay_nhan_phong", nullable = false)
    private LocalDateTime ngayNhanPhong;

    @Column(name = "ngay_tra_phong")
    private LocalDateTime ngayTraPhong;

    @Column(name = "trang_thai", length = 30)
    private String trangThai = "Đang lưu trú";

    public DatPhong() {
    }

    public boolean isDangLuuTru() {
        return "Đang lưu trú".equalsIgnoreCase(this.trangThai);
    }

    public Long getMaDatPhong() {
        return maDatPhong;
    }

    public void setMaDatPhong(Long maDatPhong) {
        this.maDatPhong = maDatPhong;
    }

    public Long getId() {
        return maDatPhong;
    }

    public void setId(Long id) {
        this.maDatPhong = id;
    }

    public Long getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(Long maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
        if (khachHang != null && khachHang.getMaKhachHang() != null) {
            this.maKhachHang = khachHang.getMaKhachHang();
        }
    }

    public String getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(String soPhong) {
        this.soPhong = soPhong;
    }

    public Room getRoom() {
        if (room == null && soPhong != null) {
            Room r = new Room();
            r.setSoPhong(soPhong);
            return r;
        }
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
        if (room != null && room.getSoPhong() != null) {
            this.soPhong = room.getSoPhong();
        }
    }

    public Integer getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(Integer maPhong) {
        this.maPhong = maPhong;
    }

    public LocalDateTime getNgayNhanPhong() {
        return ngayNhanPhong;
    }

    public void setNgayNhanPhong(LocalDateTime ngayNhanPhong) {
        this.ngayNhanPhong = ngayNhanPhong;
    }

    public LocalDateTime getNgayTraPhong() {
        return ngayTraPhong;
    }

    public void setNgayTraPhong(LocalDateTime ngayTraPhong) {
        this.ngayTraPhong = ngayTraPhong;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}