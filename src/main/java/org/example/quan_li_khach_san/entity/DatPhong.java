package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Shared mapping for integration with member 5; keep this entity only once when merging. */
@Entity @Table(name = "dat_phong")
public class DatPhong {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dat_phong") private Long maDatPhong;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_khach_hang") private KhachHang khachHang;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "so_phong") private Room phong;
    @Column(name = "so_phong", insertable = false, updatable = false) private String soPhong;
    @Column(name = "ngay_nhan_phong", nullable = false) private LocalDateTime ngayNhanPhong;
    @Column(name = "ngay_tra_phong") private LocalDateTime ngayTraPhong;
    @Column(name = "trang_thai", nullable = false) private String trangThai;
    public Long getMaDatPhong() { return maDatPhong; }
    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang value) { khachHang = value; }
    public String getSoPhong() { return phong == null ? soPhong : phong.getSoPhong(); }
    public Room getPhong() { return phong; }
    public void setPhong(Room value) { phong = value; }
    public LocalDateTime getNgayNhanPhong() { return ngayNhanPhong; }
    public void setNgayNhanPhong(LocalDateTime value) { ngayNhanPhong = value; }
    public LocalDateTime getNgayTraPhong() { return ngayTraPhong; }
    public void setNgayTraPhong(LocalDateTime value) { ngayTraPhong = value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { trangThai = value; }
    public boolean isDangLuuTru() { return ngayTraPhong == null && "Đang lưu trú".equals(trangThai); }
}
