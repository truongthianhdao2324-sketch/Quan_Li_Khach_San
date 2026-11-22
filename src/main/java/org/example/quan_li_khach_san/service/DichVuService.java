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
    public List<DichVu> dichVuDangBan() {
        return dichVuRepository.findAllByOrderByTenDichVuAsc().stream()
                .filter(d -> !d.getTenDichVu().startsWith("Bồi thường: ")).toList();
    }
    public List<DatPhong> luotDangO() { return datPhongRepository.findDangLuuTru(); }
    public DatPhong getLuotO(Long id) {
        return datPhongRepository.findById(id).orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy lượt lưu trú."));
    }
    public List<SuDungDichVu> lichSu(Long id) { return suDungRepository.findLichSu(id); }
    public BigDecimal tongTienTheoLuotO(Long id) { return suDungRepository.tongTienTheoLuotO(id); }

    public Page<SuDungDichVu> lichSuPhanTrang(Long id, int trang) {
        Sort sapXep = Sort.by(Sort.Direction.DESC, "ngaySuDung", "id");
        Page<SuDungDichVu> ketQua = suDungRepository.findTheoLuotO(id, PageRequest.of(Math.max(0, trang), 10, sapXep));
        if (ketQua.isEmpty() && ketQua.getTotalPages() > 0 && ketQua.getNumber() >= ketQua.getTotalPages()) {
            ketQua = suDungRepository.findTheoLuotO(id, PageRequest.of(ketQua.getTotalPages() - 1, 10, sapXep));
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
        if (dichVu.getTenDichVu().startsWith("Bồi thường: "))
            throw new DichVuException("Chọn Ghi nhận bồi thường để nhập khoản mới.");
        return luuKhoan(luotO, dichVu, soLuong);
    }

    @Transactional
    public Long ghiNhanBoiThuong(Long maDatPhong, String noiDung, BigDecimal donGia, Integer soLuong) {
        String ten = noiDung == null ? "" : noiDung.strip();
        if (ten.isEmpty() || ten.length() > 80 || ten.chars().anyMatch(c -> Character.isISOControl(c) || c == '|'))
            throw new DichVuException("Nhập nội dung bồi thường từ 1 đến 80 ký tự, không chứa ký tự xuống dòng hoặc dấu |.");
        if (donGia == null || donGia.signum() <= 0 || donGia.compareTo(new BigDecimal("9999999999.99")) > 0
                || donGia.stripTrailingZeros().scale() > 2)
            throw new DichVuException("Đơn giá bồi thường phải lớn hơn 0, tối đa 9.999.999.999,99 và hai số thập phân.");
        if (maDatPhong == null || soLuong == null || soLuong < 1 || soLuong > 999)
            throw new DichVuException("Chọn lượt lưu trú và số lượng nguyên từ 1 đến 999.");
        DatPhong luotO = datPhongRepository.findLockedById(maDatPhong)
                .orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy lượt lưu trú."));
        if (!luotO.isDangLuuTru()) throw new DichVuException("Chỉ được ghi nhận bồi thường trước khi trả phòng.");
        // Mỗi sự cố giữ một tên và đơn giá riêng; dùng các cột hiện có của SQL nhóm.
        DichVu khoan = new DichVu();
        khoan.setTenDichVu("Bồi thường: " + ten);
        khoan.setGiaDichVu(donGia.setScale(2, RoundingMode.UNNECESSARY));
        dichVuRepository.save(khoan);
        return luuKhoan(luotO, khoan, soLuong);
    }

    private Long luuKhoan(DatPhong luotO, DichVu dichVu, Integer soLuong) {
        if (luotO.getKhachHang() == null || luotO.getKhachHang().getSoGiayTo() == null)
            throw new DichVuException("Lượt lưu trú chưa có thông tin giấy tờ khách hàng.");
        LocalDateTime thoiDiem = LocalDateTime.now().withNano(0);
        List<DatPhong> phuHop = suDungRepository.findLuotOPhuHop(
                luotO.getSoPhong(), luotO.getKhachHang().getSoGiayTo(), thoiDiem);
        if (phuHop.size() != 1 || !phuHop.get(0).getMaDatPhong().equals(luotO.getMaDatPhong()))
            throw new DichVuException("Không xác định duy nhất lượt ở theo phòng, giấy tờ và thời gian. Kiểm tra lại thông tin lưu trú.");
        SuDungDichVu suDung = new SuDungDichVu();
        suDung.setDichVu(dichVu);
        suDung.setSoPhong(luotO.getSoPhong());
        suDung.setSoGiayTo(luotO.getKhachHang().getSoGiayTo());
        suDung.setSoLuong(soLuong);
        suDung.setThanhTien(dichVu.getGiaDichVu().multiply(BigDecimal.valueOf(soLuong)));
        suDung.setNgaySuDung(thoiDiem);
        return suDungRepository.saveAndFlush(suDung).getId();
    }

    public String noiDungThongBao(Long id, Long maDatPhong) {
        SuDungDichVu suDung = suDungRepository.findById(id)
                .orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy khoản dịch vụ."));
        List<DatPhong> phuHop = suDung.getNgaySuDung() == null ? List.of()
                : suDungRepository.findLuotOPhuHop(suDung.getSoPhong(), suDung.getSoGiayTo(), suDung.getNgaySuDung());
        if (phuHop.size() != 1 || (maDatPhong != null && !maDatPhong.equals(phuHop.get(0).getMaDatPhong())))
            throw new DichVuException("Khoản dịch vụ không thuộc lượt ở đang xem hoặc dữ liệu cũ chưa khớp duy nhất một lượt ở.");
        Long maLuotO = phuHop.get(0).getMaDatPhong();
        String ten = suDung.getTenDichVu().replace('|', '/').replace('\n', ' ').replace('\r', ' ');
        return "GUI|" + id + "|" + maLuotO + "|" + suDung.getSoPhong() + "|" + ten + "|" + suDung.getSoLuong() + "|" + suDung.getNgaySuDung();
    }
}
