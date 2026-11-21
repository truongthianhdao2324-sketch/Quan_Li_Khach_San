package org.example.quan_li_khach_san.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "phong")
public class Room {

    @Id
    @Column(name = "so_phong", length = 20)
    private String soPhong;

    @Column(name = "trang_thai")
    private String trangThai;

    @Column(name = "gia_phong")
    private double giaPhong;

    @Column(name = "loai_phong")
    private String loaiPhong;

    public Room() {}

    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String soPhong) { this.soPhong = soPhong; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public double getGiaPhong() { return giaPhong; }
    public void setGiaPhong(double giaPhong) { this.giaPhong = giaPhong; }

    public String getLoaiPhong() { return loaiPhong; }
    public void setLoaiPhong(String loaiPhong) { this.loaiPhong = loaiPhong; }
}