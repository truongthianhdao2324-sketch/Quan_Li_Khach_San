package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "khach_hang")
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_khach_hang")
    private Long maKhachHang;

    @Column(name = "so_giay_to")
    private String soGiayTo;

    @Column(name = "loai_giay_to")
    private String loaiGiayTo;

    @Column(name = "ho_ten")
    private String hoTen;

    @Column(name = "gioi_tinh")
    private String gioiTinh;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "email")
    private String email;

    @Column(name = "trang_thai")
    private String trangThai = "Hoạt động";

    @Column(name = "ly_do_huy", length = 500)
    private String lyDoHuy;

    // Lưu tên đăng nhập (ten_dang_nhap) của tài khoản thực hiện hủy
    @Column(name = "nguoi_huy", length = 50)
    private String nguoiHuy;

    public KhachHang() {}

    public String getMaHienThi() {
        if (maKhachHang == null) return "";
        return String.format("KH%04d", maKhachHang);
    }

    // Getters và Setters
    public Long getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(Long maKhachHang) { this.maKhachHang = maKhachHang; }

    public String getSoGiayTo() { return soGiayTo; }
    public void setSoGiayTo(String soGiayTo) { this.soGiayTo = soGiayTo; }

    public String getLoaiGiayTo() { return loaiGiayTo; }
    public void setLoaiGiayTo(String loaiGiayTo) { this.loaiGiayTo = loaiGiayTo; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTrangThai() {
        return (trangThai == null || trangThai.isBlank()) ? "Hoạt động" : trangThai;
    }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }

    public String getNguoiHuy() { return nguoiHuy; }
    public void setNguoiHuy(String nguoiHuy) { this.nguoiHuy = nguoiHuy; }
}
