package org.example.quan_li_khach_san.dto;

import java.math.BigDecimal;

public class DichVuForm {
    private String tenDichVu;
    private BigDecimal giaDichVu;
    public String getTenDichVu() { return tenDichVu; }
    public void setTenDichVu(String value) { tenDichVu = value; }
    public BigDecimal getGiaDichVu() { return giaDichVu; }
    public void setGiaDichVu(BigDecimal value) { giaDichVu = value; }
}
