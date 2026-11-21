package org.example.quan_li_khach_san.service;

import jakarta.annotation.PostConstruct;
import org.example.quan_li_khach_san.entity.KhachHang;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class KhachHangService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private KhachHangScheduledService khachHangLogService;

    private static final String CANCEL_STORE_PATH = "logs/khach_hang_huy.txt";
    // Key: maKhachHang | Value: [nguoiHuy, lyDoHuy]
    private final Map<Long, String[]> duLieuHuyMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void napDuLieuHuyTuFile() {
        File file = new File(CANCEL_STORE_PATH);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 3);
                if (parts.length == 3) {
                    duLieuHuyMap.put(Long.parseLong(parts[0]), new String[]{parts[1], parts[2]});
                }
            }
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO ĐỌC TRẠNG THÁI HỦY KH]: " + e.getMessage());
        }
    }

    private synchronized void luuDuLieuHuyRaFile() {
        try {
            File file = new File(CANCEL_STORE_PATH);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
                for (Map.Entry<Long, String[]> entry : duLieuHuyMap.entrySet()) {
                    String nguoiHuy = entry.getValue()[0].replace("|", "/").replace("\n", " ");
                    String lyDo = entry.getValue()[1].replace("|", "/").replace("\n", " ");
                    bw.write(entry.getKey() + "|" + nguoiHuy + "|" + lyDo);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("[LỖI LƯU TRẠNG THÁI HỦY KH]: " + e.getMessage());
        }
    }

    public KhachHang apDungTrangThaiHuy(KhachHang kh) {
        if (kh == null) return null;
        String[] info = (kh.getMaKhachHang() != null) ? duLieuHuyMap.get(kh.getMaKhachHang()) : null;
        if (info != null) {
            kh.setTrangThai("Đã hủy");
            kh.setNguoiHuy(info[0]);
            kh.setLyDoHuy(info[1]);
        } else {
            kh.setTrangThai("Hoạt động");
            kh.setNguoiHuy(null);
            kh.setLyDoHuy(null);
        }
        return kh;
    }

    public List<KhachHang> layTatCaKhachHang() {
        return khachHangRepository.findAll().stream()
                .map(this::apDungTrangThaiHuy)
                .collect(Collectors.toList());
    }

    public KhachHang timKhachHangTheoId(Long id) {
        KhachHang kh = khachHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng có mã: " + id));
        return apDungTrangThaiHuy(kh);
    }

    // 1. Tìm kiếm & Lọc theo trạng thái
    public List<KhachHang> timKiemVaLocKhachHang(String keyword, String trangThai) {
        return layTatCaKhachHang().stream()
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
                    return kh.getTrangThai().equalsIgnoreCase(trangThai.trim());
                })
                .sorted(Comparator.comparing(KhachHang::getMaKhachHang))
                .collect(Collectors.toList());
    }

    // 2. Lấy chi tiết 1 khách hàng
    public Map<String, Object> layChiTietKhachHang(Long maKhachHang) {
        KhachHang kh = timKhachHangTheoId(maKhachHang);

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
        boolean laThemMoi = (khachHang.getMaKhachHang() == null);
        String thaoTac = laThemMoi ? "THEM_KHACH_HANG" : "CAP_NHAT_KHACH_HANG";
        String maHienThi = laThemMoi ? "Mới" : String.format("KH%04d", khachHang.getMaKhachHang());
        String trangThaiCu = laThemMoi ? "Chưa có" : "Hoạt động";

        if (khachHang.getHoTen() == null || khachHang.getHoTen().trim().isEmpty()) {
            khachHangLogService.ghiLog(maHienThi, khachHang.getHoTen(), thaoTac,
                    trangThaiCu, trangThaiCu, "Thất bại - Họ tên khách hàng trống");
            throw new IllegalArgumentException("Họ tên khách hàng không được để trống!");
        }
        if (khachHang.getSoGiayTo() == null || !khachHang.getSoGiayTo().trim().matches("\\d{9}|\\d{12}")) {
            khachHangLogService.ghiLog(maHienThi, khachHang.getHoTen(), thaoTac,
                    trangThaiCu, trangThaiCu, "Thất bại - Số CCCD/CMND không hợp lệ");
            throw new IllegalArgumentException("Số CCCD/CMND phải gồm đúng 9 hoặc 12 chữ số!");
        }
        if (khachHang.getSoDienThoai() == null || !khachHang.getSoDienThoai().trim().matches("^0\\d{9}$")) {
            khachHangLogService.ghiLog(maHienThi, khachHang.getHoTen(), thaoTac,
                    trangThaiCu, trangThaiCu, "Thất bại - Số điện thoại không hợp lệ");
            throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng số 0 và có đủ 10 chữ số!");
        }

        boolean biTrungGiayTo = khachHangRepository.findAll().stream()
                .anyMatch(existing -> existing.getSoGiayTo() != null
                        && existing.getSoGiayTo().equalsIgnoreCase(khachHang.getSoGiayTo().trim())
                        && !Objects.equals(existing.getMaKhachHang(), khachHang.getMaKhachHang()));
        if (biTrungGiayTo) {
            khachHangLogService.ghiLog(maHienThi, khachHang.getHoTen(), thaoTac,
                    trangThaiCu, trangThaiCu, "Thất bại - Trùng số giấy tờ " + khachHang.getSoGiayTo().trim());
            throw new IllegalArgumentException("Số giấy tờ " + khachHang.getSoGiayTo() + " đã tồn tại trong hệ thống!");
        }

        if (!laThemMoi) {
            KhachHang cu = khachHangRepository.findById(khachHang.getMaKhachHang())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng cần cập nhật!"));
            khachHang.setSoPhong(cu.getSoPhong());
        }

        khachHang.setHoTen(khachHang.getHoTen().trim());
        khachHang.setSoGiayTo(khachHang.getSoGiayTo().trim());
        khachHang.setSoDienThoai(khachHang.getSoDienThoai().trim());

        KhachHang daLuu = khachHangRepository.save(khachHang);

        khachHangLogService.ghiLog(
                String.format("KH%04d", daLuu.getMaKhachHang()),
                daLuu.getHoTen(),
                thaoTac,
                trangThaiCu,
                "Hoạt động",
                "Thành công"
        );
    }

    // 4. Hủy hồ sơ khách hàng
    public synchronized void huyKhachHang(Long maKhachHang, String lyDoHuy, String tenDangNhapHuy) {
        KhachHang kh = timKhachHangTheoId(maKhachHang);
        String maHienThi = String.format("KH%04d", kh.getMaKhachHang());
        String trangThaiCu = kh.getTrangThai();

        if (lyDoHuy == null || lyDoHuy.trim().isEmpty()) {
            khachHangLogService.ghiLog(maHienThi, kh.getHoTen(), "HUY_HO_SO",
                    trangThaiCu, trangThaiCu, "Thất bại - Thiếu lý do hủy hồ sơ");
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy hồ sơ khách hàng!");
        }

        if ("Đã hủy".equalsIgnoreCase(trangThaiCu)) {
            khachHangLogService.ghiLog(maHienThi, kh.getHoTen(), "HUY_HO_SO",
                    trangThaiCu, trangThaiCu, "Thất bại - Hồ sơ đã hủy trước đó");
            throw new IllegalStateException("Hồ sơ khách hàng này đã ở trạng thái Đã hủy trước đó!");
        }

        String taiKhoan = (tenDangNhapHuy != null && !tenDangNhapHuy.isBlank()) ? tenDangNhapHuy.trim() : "letan";
        String thoiGianHuy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String lyDoDayDu = lyDoHuy.trim() + " (Hủy lúc: " + thoiGianHuy + ")";

        duLieuHuyMap.put(maKhachHang, new String[]{taiKhoan, lyDoDayDu});
        luuDuLieuHuyRaFile();

        khachHangLogService.ghiLog(
                maHienThi,
                kh.getHoTen(),
                "HUY_HO_SO (Bởi: " + taiKhoan + ")",
                trangThaiCu,
                "Đã hủy",
                "Thành công"
        );
    }
}