package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "nhan_vien")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nhan_vien")
    private Long maNhanVien;

    @Column(name = "ho_ten")
    private String hoTen;

    @Column(name = "tuoi")
    private Integer tuoi;

    @Column(name = "gioi_tinh")
    private String gioiTinh;

    @Column(name = "vi_tri_cong_viec")
    private String viTriCongViec;

    @Column(name = "luong")
    private Double luong;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "email")
    private String email;

    @Column(name = "so_cccd")
    private String soCccd;

    public Employee() {}

    // Chổ này là tự động sinh dạng mã (NV001, NV002 ...) ai làm phần nay lưu ý nha
    public String getMaHienThi() {
        if (maNhanVien == null) return "";
        return String.format("NV%03d", maNhanVien);
    }


    public Long getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(Long maNhanVien) { this.maNhanVien = maNhanVien; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public Integer getTuoi() { return tuoi; }
    public void setTuoi(Integer tuoi) { this.tuoi = tuoi; }

    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }

    public String getViTriCongViec() { return viTriCongViec; }
    public void setViTriCongViec(String viTriCongViec) { this.viTriCongViec = viTriCongViec; }

    public Double getLuong() { return luong; }
    public void setLuong(Double luong) { this.luong = luong; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSoCccd() { return soCccd; }
    public void setSoCccd(String soCccd) { this.soCccd = soCccd; }
}