package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.*;
import org.example.quan_li_khach_san.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DichVuService {
    private final DichVuRepository dichVuRepository;
    private final SuDungDichVuRepository suDungRepository;
    private final DatPhongRepository datPhongRepository;

    public DichVuService(DichVuRepository dichVuRepository, SuDungDichVuRepository suDungRepository,
                         DatPhongRepository datPhongRepository) {
        this.dichVuRepository = dichVuRepository;
        this.suDungRepository = suDungRepository;
        this.datPhongRepository = datPhongRepository;
    }

    public Page<DichVu> danhSach(String tuKhoa, int trang, String cot, String chieu) {
        String cotSapXep = cot != null && Set.of("tenDichVu", "giaDichVu", "maDichVu").contains(cot) ? cot : "tenDichVu";
        Sort sapXep = Sort.by("desc".equalsIgnoreCase(chieu) ? Sort.Direction.DESC : Sort.Direction.ASC, cotSapXep);
        if (!cotSapXep.equals("maDichVu")) sapXep = sapXep.and(Sort.by("maDichVu"));
        String ten = tuKhoa == null ? "" : tuKhoa.strip();
        Page<DichVu> ketQua = dichVuRepository.findByTenDichVuContainingIgnoreCase(ten, PageRequest.of(Math.max(0, trang), 10, sapXep));
        if (ketQua.isEmpty() && ketQua.getTotalPages() > 0 && ketQua.getNumber() >= ketQua.getTotalPages()) {
            ketQua = dichVuRepository.findByTenDichVuContainingIgnoreCase(ten, PageRequest.of(ketQua.getTotalPages() - 1, 10, sapXep));
        }
        return ketQua;
    }

    public DichVu getDichVu(Long id) {
        return dichVuRepository.findById(id).orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy dịch vụ."));
    }
    public List<DichVu> dichVuDangBan() { return dichVuRepository.findAllByOrderByTenDichVuAsc(); }
    public List<DatPhong> luotDangO() { return datPhongRepository.findDangLuuTru(); }
    public DatPhong getLuotO(Long id) {
        return datPhongRepository.findById(id).orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy lượt lưu trú."));
    }
    public List<SuDungDichVu> lichSu(Long id) { return suDungRepository.findLichSu(id); }
    public BigDecimal tongTienTheoLuotO(Long id) { return suDungRepository.tongTienTheoLuotO(id); }

    public Page<SuDungDichVu> lichSuPhanTrang(Long id, int trang) {
        Sort sapXep = Sort.by(Sort.Direction.DESC, "ngaySuDung", "id");
        Page<SuDungDichVu> ketQua = suDungRepository.findByDatPhongMaDatPhong(id, PageRequest.of(Math.max(0, trang), 10, sapXep));
        if (ketQua.isEmpty() && ketQua.getTotalPages() > 0 && ketQua.getNumber() >= ketQua.getTotalPages()) {
            ketQua = suDungRepository.findByDatPhongMaDatPhong(id, PageRequest.of(ketQua.getTotalPages() - 1, 10, sapXep));
        }
        return ketQua;
    }

    @Transactional
    public void luuDanhMuc(Long id, DichVu duLieu) {
        String ten = duLieu.getTenDichVu();
        BigDecimal gia = duLieu.getGiaDichVu();
        if (ten == null || ten.isBlank() || ten.trim().length() > 100)
            throw new DichVuException("Tên dịch vụ phải có từ 1 đến 100 ký tự.");
        if (gia == null || gia.signum() < 0 || gia.compareTo(new BigDecimal("9999999999.99")) > 0 || gia.stripTrailingZeros().scale() > 2)
            throw new DichVuException("Đơn giá phải từ 0 đến 9.999.999.999,99 và tối đa 2 số thập phân.");
        DichVu dichVu = id == null ? new DichVu() : dichVuRepository.findLockedById(id)
                .orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy dịch vụ."));
        dichVu.setTenDichVu(ten.trim());
        dichVu.setGiaDichVu(gia.setScale(2, RoundingMode.UNNECESSARY));
        dichVuRepository.save(dichVu);
    }

    @Transactional
    public void xoaDanhMuc(Long id) {
        DichVu dichVu = dichVuRepository.findLockedById(id).orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy dịch vụ."));
        if (suDungRepository.existsByDichVuMaDichVu(id)) throw new DichVuException("Dịch vụ đã có lịch sử sử dụng nên không thể xóa.");
        dichVuRepository.delete(dichVu);
        dichVuRepository.flush();
    }

    @Transactional
    public Long goiDichVu(Long maDatPhong, Long maDichVu, Integer soLuong) {
        if (maDatPhong == null || maDichVu == null || soLuong == null || soLuong < 1 || soLuong > 999)
            throw new DichVuException("Chọn lượt lưu trú, dịch vụ và số lượng nguyên từ 1 đến 999.");
        DatPhong luotO = datPhongRepository.findLockedById(maDatPhong)
                .orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy lượt lưu trú."));
        if (!luotO.isDangLuuTru()) throw new DichVuException("Chỉ được gọi dịch vụ cho lượt đang lưu trú và chưa trả phòng.");
        DichVu dichVu = dichVuRepository.findLockedById(maDichVu)
                .orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy dịch vụ."));
        SuDungDichVu suDung = new SuDungDichVu();
        suDung.setDatPhong(luotO);
        suDung.setDichVu(dichVu);
        suDung.setSoPhong(luotO.getSoPhong());
        suDung.setSoGiayTo(luotO.getKhachHang().getSoGiayTo());
        suDung.setSoLuong(soLuong);
        suDung.setThanhTien(dichVu.getGiaDichVu().multiply(BigDecimal.valueOf(soLuong)));
        suDung.setNgaySuDung(LocalDateTime.now());
        return suDungRepository.saveAndFlush(suDung).getId();
    }

    public String noiDungThongBao(Long id, Long maDatPhong) {
        SuDungDichVu suDung = suDungRepository.findById(id)
                .orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy khoản dịch vụ."));
        Long maLuotO = suDung.getDatPhong() == null ? maDatPhong : suDung.getDatPhong().getMaDatPhong();
        if (maLuotO == null || (maDatPhong != null && !maDatPhong.equals(maLuotO))
                || (suDung.getDatPhong() == null && lichSu(maLuotO).stream().noneMatch(d -> d.getId().equals(id))))
            throw new DichVuException("Khoản dịch vụ không thuộc lượt ở đang xem hoặc dữ liệu cũ chưa khớp lượt ở.");
        String ten = suDung.getTenDichVu().replace('|', '/').replace('\n', ' ').replace('\r', ' ');
        return "GUI|" + id + "|" + maLuotO + "|" + suDung.getSoPhong() + "|" + ten + "|" + suDung.getSoLuong() + "|" + suDung.getNgaySuDung();
    }
}
