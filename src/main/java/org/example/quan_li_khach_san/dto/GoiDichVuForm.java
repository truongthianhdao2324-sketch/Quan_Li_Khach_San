package org.example.quan_li_khach_san.dto;

public class GoiDichVuForm {
    private Long maDatPhong;
    private Long maDichVu;
    private Integer soLuong = 1;
    private String maYeuCau;
    public Long getMaDatPhong() { return maDatPhong; }
    public void setMaDatPhong(Long value) { maDatPhong = value; }
    public Long getMaDichVu() { return maDichVu; }
    public void setMaDichVu(Long value) { maDichVu = value; }
    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer value) { soLuong = value; }
    public String getMaYeuCau() { return maYeuCau; }
    public void setMaYeuCau(String value) { maYeuCau = value; }
}
