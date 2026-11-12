package org.example.quan_li_khach_san.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "su_dung_dich_vu", uniqueConstraints = @UniqueConstraint(name = "uq_dichvu_request", columnNames = "ma_yeu_cau"))
public class SuDungDichVu {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_dat_phong") private DatPhong datPhong;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_dich_vu") private DichVu dichVu;
    @Column(name = "so_phong", nullable = false, length = 20) private String soPhong;
    @Column(name = "so_giay_to", nullable = false, length = 50) private String soGiayTo;
    @Column(name = "ten_dich_vu_luc_goi", nullable = false, length = 100) private String tenDichVuLucGoi;
    @Column(name = "don_gia_luc_goi", nullable = false, precision = 15, scale = 2) private BigDecimal donGiaLucGoi;
    @Column(name = "so_luong", nullable = false) private Integer soLuong;
    @Column(name = "thanh_tien", nullable = false, precision = 15, scale = 2) private BigDecimal thanhTien;
    @Column(name = "ngay_su_dung", nullable = false) private LocalDateTime ngaySuDung;
    @Column(name = "ma_yeu_cau", nullable = false, length = 36) private String maYeuCau;
    @Column(name = "nguoi_goi", nullable = false, length = 50) private String nguoiGoi;
    @Column(name = "da_gui_thong_bao", nullable = false) private boolean daGuiThongBao;
    public Long getId() { return id; }
    public DatPhong getDatPhong() { return datPhong; }
    public void setDatPhong(DatPhong value) { datPhong = value; }
    public DichVu getDichVu() { return dichVu; }
    public void setDichVu(DichVu value) { dichVu = value; }
    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String value) { soPhong = value; }
    public String getSoGiayTo() { return soGiayTo; }
    public void setSoGiayTo(String value) { soGiayTo = value; }
    public String getTenDichVuLucGoi() { return tenDichVuLucGoi; }
    public void setTenDichVuLucGoi(String value) { tenDichVuLucGoi = value; }
    public BigDecimal getDonGiaLucGoi() { return donGiaLucGoi; }
    public void setDonGiaLucGoi(BigDecimal value) { donGiaLucGoi = value; }
    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer value) { soLuong = value; }
    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal value) { thanhTien = value; }
    public LocalDateTime getNgaySuDung() { return ngaySuDung; }
    public void setNgaySuDung(LocalDateTime value) { ngaySuDung = value; }
    public String getMaYeuCau() { return maYeuCau; }
    public void setMaYeuCau(String value) { maYeuCau = value; }
    public String getNguoiGoi() { return nguoiGoi; }
    public void setNguoiGoi(String value) { nguoiGoi = value; }
    public boolean isDaGuiThongBao() { return daGuiThongBao; }
    public void setDaGuiThongBao(boolean value) { daGuiThongBao = value; }
}
