package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.KhachHang;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KhachHangService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    // 1. Tìm kiếm theo từ khóa (Tên / SĐT / CCCD) + Lọc theo trạng thái (Hoạt động / Đã hủy)
    public List<KhachHang> timKiemVaLocKhachHang(String keyword, String trangThai) {
        List<KhachHang> tatCa = khachHangRepository.findAll();

        return tatCa.stream()
                .filter(kh -> {
                    if (keyword == null || keyword.trim().isEmpty()) return true;
                    String tk = keyword.trim().toLowerCase();
                    return (kh.getHoTen() != null && kh.getHoTen().toLowerCase().contains(tk))
                            || (kh.getSoGiayTo() != null && kh.getSoGiayTo().toLowerCase().contains(tk))
                            || (kh.getSoDienThoai() != null && kh.getSoDienThoai().contains(tk));
                })
                .filter(kh -> {
                    if (trangThai == null || trangThai.trim().isEmpty() || "ALL".equalsIgnoreCase(trangThai)) {
                        return true;
                    }
                    String ttHienTai = kh.getTrangThai();
                    return ttHienTai.equalsIgnoreCase(trangThai.trim());
                })
                .sorted(Comparator.comparing(KhachHang::getMaKhachHang))
                .collect(Collectors.toList());
    }

    // 2. Lấy chi tiết 1 khách hàng (bao gồm trạng thái, lý do hủy & tài khoản hủy)
    public Map<String, Object> layChiTietKhachHang(Long maKhachHang) {
        KhachHang kh = khachHangRepository.findById(maKhachHang)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng có mã: " + maKhachHang));

        Map<String, Object> hoSo = new LinkedHashMap<>();
        hoSo.put("maKhachHang", kh.getMaKhachHang());
        hoSo.put("maKHFormat", String.format("KH%04d", kh.getMaKhachHang()));
        hoSo.put("hoTen", kh.getHoTen());
        hoSo.put("gioiTinh", kh.getGioiTinh());
        hoSo.put("loaiGiayTo", kh.getLoaiGiayTo());
        hoSo.put("soGiayTo", kh.getSoGiayTo());
        hoSo.put("soDienThoai", kh.getSoDienThoai());
        hoSo.put("email", (kh.getEmail() != null && !kh.getEmail().isBlank()) ? kh.getEmail() : "Chưa cập nhật");
        hoSo.put("trangThai", kh.getTrangThai());
        hoSo.put("lyDoHuy", (kh.getLyDoHuy() != null && !kh.getLyDoHuy().isBlank()) ? kh.getLyDoHuy() : "");
        hoSo.put("nguoiHuy", (kh.getNguoiHuy() != null && !kh.getNguoiHuy().isBlank()) ? kh.getNguoiHuy() : "Không xác định");
        return hoSo;
    }

    // 3. Thêm mới / Cập nhật thông tin khách hàng
    @Transactional
    public synchronized void luuHoacCapNhatKhachHang(KhachHang khachHang) {
        if (khachHang.getHoTen() == null || khachHang.getHoTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên khách hàng không được để trống!");
        }
        if (khachHang.getSoGiayTo() == null || !khachHang.getSoGiayTo().trim().matches("\\d{9}|\\d{12}")) {
            throw new IllegalArgumentException("Số CCCD/CMND phải gồm đúng 9 hoặc 12 chữ số!");
        }
        if (khachHang.getSoDienThoai() == null || !khachHang.getSoDienThoai().trim().matches("^0\\d{9}$")) {
            throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng số 0 và có đủ 10 chữ số!");
        }

        boolean biTrungGiayTo = khachHangRepository.findAll().stream()
                .anyMatch(existing -> existing.getSoGiayTo() != null
                        && existing.getSoGiayTo().equalsIgnoreCase(khachHang.getSoGiayTo().trim())
                        && !Objects.equals(existing.getMaKhachHang(), khachHang.getMaKhachHang()));
        if (biTrungGiayTo) {
            throw new IllegalArgumentException("Số giấy tờ " + khachHang.getSoGiayTo() + " đã tồn tại trong hệ thống!");
        }

        if (khachHang.getMaKhachHang() != null) {
            KhachHang cu = khachHangRepository.findById(khachHang.getMaKhachHang())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng cần cập nhật!"));
            khachHang.setTrangThai(cu.getTrangThai());
            khachHang.setLyDoHuy(cu.getLyDoHuy());
            khachHang.setNguoiHuy(cu.getNguoiHuy());
        } else {
            khachHang.setTrangThai("Hoạt động");
            khachHang.setLyDoHuy(null);
            khachHang.setNguoiHuy(null);
        }

        khachHang.setHoTen(khachHang.getHoTen().trim());
        khachHang.setSoGiayTo(khachHang.getSoGiayTo().trim());
        khachHang.setSoDienThoai(khachHang.getSoDienThoai().trim());
        khachHangRepository.save(khachHang);
    }

    // 4. Hủy hồ sơ khách hàng (Lưu lý do hủy + tên đăng nhập tài khoản thực hiện hủy)
    @Transactional
    public synchronized void huyKhachHang(Long maKhachHang, String lyDoHuy, String tenDangNhapHuy) {
        if (lyDoHuy == null || lyDoHuy.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy hồ sơ khách hàng!");
        }

        KhachHang kh = khachHangRepository.findById(maKhachHang)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng có mã: " + maKhachHang));

        if ("Đã hủy".equalsIgnoreCase(kh.getTrangThai())) {
            throw new IllegalStateException("Hồ sơ khách hàng này đã ở trạng thái Đã hủy trước đó!");
        }

        String taiKhoan = (tenDangNhapHuy != null && !tenDangNhapHuy.isBlank()) ? tenDangNhapHuy.trim() : "letan";
        String thoiGianHuy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        kh.setTrangThai("Đã hủy");
        kh.setLyDoHuy(lyDoHuy.trim() + " (Hủy lúc: " + thoiGianHuy + ")");
        kh.setNguoiHuy(taiKhoan);
        khachHangRepository.save(kh);
    }
}
