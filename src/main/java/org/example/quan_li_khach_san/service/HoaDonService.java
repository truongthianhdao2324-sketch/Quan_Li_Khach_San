package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.DatPhong;
import org.example.quan_li_khach_san.entity.HoaDon;
import org.example.quan_li_khach_san.entity.KhachHang;
import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.repository.DatPhongRepository;
import org.example.quan_li_khach_san.repository.HoaDonRepository;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class HoaDonService {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private DatPhongRepository datPhongRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private HoaDonLogService hoaDonLogService;

    // Khóa đồng bộ theo từng phiếu đặt phòng để tránh 2 lễ tân check-out trùng 1 phòng cùng lúc
    private final ConcurrentHashMap<Long, Object> checkoutLocks = new ConcurrentHashMap<>();

    public static class ThongTinCheckOut {
        public HoaDon hoaDon;
        public long soNgayO;
        public double donGiaPhong;
        public LocalDateTime thoiGianCheckOut;
    }

    private boolean laTrangThaiDangO(String trangThai) {
        if (trangThai == null) return false;
        String tt = trangThai.trim();
        return tt.equalsIgnoreCase("Đang lưu trú") || tt.equalsIgnoreCase("Đã có khách");
    }

    /**
     * Tính số ngày ở:
     * - Nếu DatPhong đã có ngayTraPhong (ngày đi): luôn tính theo khoảng ngayNhanPhong -> ngayTraPhong.
     * - Nếu chưa có ngayTraPhong (khách trong CSDL đang lưu trú): mới tính đến mocHienTai (thời điểm hiện tại).
     */
    public long tinhSoNgayO(DatPhong dp, LocalDateTime mocHienTai) {
        if (dp == null || dp.getNgayNhanPhong() == null) {
            return 1L;
        }
        LocalDateTime thoiDiemKetThuc = (dp.getNgayTraPhong() != null)
                ? dp.getNgayTraPhong()
                : (mocHienTai != null ? mocHienTai : LocalDateTime.now());

        long days = ChronoUnit.DAYS.between(
                dp.getNgayNhanPhong().toLocalDate(),
                thoiDiemKetThuc.toLocalDate()
        );
        return Math.max(1L, days);
    }

    // Lấy đơn giá phòng niêm yết (gia_phong) của từng hóa đơn để hiển thị lên cột "GIÁ PHÒNG"
    public Map<Long, Double> layMapGiaPhong(List<HoaDon> dsHoaDon) {
        Map<Long, Double> mapGiaPhong = new HashMap<>();
        if (dsHoaDon == null) {
            return mapGiaPhong;
        }
        for (HoaDon hd : dsHoaDon) {
            double giaPhong = layDonGiaPhong(hd);
            if (hd.getMaHoaDon() != null) {
                mapGiaPhong.put(hd.getMaHoaDon(), giaPhong);
            }
        }
        return mapGiaPhong;
    }

    // Lấy đơn giá 1 ngày của phòng gắn với hóa đơn
    public double layDonGiaPhong(HoaDon hd) {
        if (hd != null && hd.getDatPhong() != null
                && hd.getDatPhong().getSoPhong() != null
                && !hd.getDatPhong().getSoPhong().isBlank()) {
            Room room = roomRepository.findById(hd.getDatPhong().getSoPhong().trim()).orElse(null);
            if (room != null) {
                return Math.max(0, room.getGiaPhong());
            }
        }
        return (hd != null && hd.getTienPhong() != null) ? Math.max(0, hd.getTienPhong()) : 0.0;
    }

    // Tính số ngày ở cố định trên biên lai (không lấy ngày hiện tại lúc in biên lai)
    public long tinhSoNgayOChoHoaDon(HoaDon hd) {
        if (hd != null && hd.getDatPhong() != null) {
            DatPhong dp = hd.getDatPhong();
            if (dp.getNgayTraPhong() != null) {
                return tinhSoNgayO(dp, dp.getNgayTraPhong());
            }
            LocalDateTime mocKetThuc = (hd.getNgayLap() != null) ? hd.getNgayLap() : LocalDateTime.now();
            return tinhSoNgayO(dp, mocKetThuc);
        }
        return 1L;
    }

    // Tự động đồng bộ các khách "Đang lưu trú" thành hóa đơn "Chưa thanh toán"
    @Transactional
    public void dongBoHoaDonKhachDangLuuTru() {
        List<DatPhong> dsDangLuuTru = datPhongRepository.findAll().stream()
                .filter(dp -> laTrangThaiDangO(dp.getTrangThai()))
                .collect(Collectors.toList());

        LocalDateTime hienTai = LocalDateTime.now();

        for (DatPhong dp : dsDangLuuTru) {
            if (dp.getMaDatPhong() == null) {
                continue;
            }

            if (!"Đang lưu trú".equalsIgnoreCase(dp.getTrangThai())) {
                dp.setTrangThai("Đang lưu trú");
                datPhongRepository.save(dp);
            }

            Room room = (dp.getSoPhong() != null && !dp.getSoPhong().isBlank())
                    ? roomRepository.findById(dp.getSoPhong().trim()).orElse(null)
                    : null;
            double donGiaPhong = (room != null) ? Math.max(0, room.getGiaPhong()) : 0.0;

            // Nếu dp đã có ngayTraPhong thì tính đúng theo ngày đến -> ngày đi; ngược lại tính đến hiện tại
            long soNgayO = tinhSoNgayO(dp, hienTai);
            double tienPhong = soNgayO * donGiaPhong;

            Double tienDichVuDb = hoaDonRepository.tinhTongTienDichVuTheoDatPhong(dp.getMaDatPhong());
            double tienDichVu = (tienDichVuDb != null) ? Math.max(0, tienDichVuDb) : 0.0;
            double tongTien = tienPhong + tienDichVu;

            Optional<HoaDon> hdOpt = hoaDonRepository.findFirstByMaDatPhongOrderByMaHoaDonAsc(dp.getMaDatPhong());
            boolean laHoaDonMoi = hdOpt.isEmpty();
            HoaDon hd = hdOpt.orElseGet(HoaDon::new);

            boolean canCapNhat = laHoaDonMoi
                    || !"Chưa thanh toán".equalsIgnoreCase(hd.getTrangThai())
                    || hd.getNgayLap() != null
                    || !Objects.equals(hd.getTienPhong(), tienPhong)
                    || !Objects.equals(hd.getTienDichVu(), tienDichVu);

            if (canCapNhat) {
                hd.setMaDatPhong(dp.getMaDatPhong());
                hd.setNgayLap(null);
                hd.setTienPhong(tienPhong);
                hd.setTienDichVu(tienDichVu);
                hd.setTongTien(tongTien);
                hd.setTrangThai("Chưa thanh toán");
                HoaDon hdDaLuu = hoaDonRepository.save(hd);

                if (laHoaDonMoi) {
                    String tenKhach = (dp.getKhachHang() != null && dp.getKhachHang().getHoTen() != null)
                            ? dp.getKhachHang().getHoTen() : "Khách lẻ";
                    hoaDonLogService.ghiLog(
                            "Hệ thống",
                            "Tạo hóa đơn tạm tính mới",
                            "Mã " + hdDaLuu.getMaHienThi() + " - Phòng " + dp.getSoPhong()
                                    + " - Khách hàng: " + tenKhach + " (Tạm tính: " + String.format("%,.0f", tongTien) + " đ)",
                            "Chưa có hóa đơn",
                            "Chưa thanh toán",
                            "Thành công"
                    );
                }
            }
        }
    }

    @Transactional
    public List<HoaDon> timKiemVaSapXep(String tuKhoa, String trangThai) {
        dongBoHoaDonKhachDangLuuTru();
        String keyword = (tuKhoa != null) ? tuKhoa.trim().toLowerCase() : "";

        return hoaDonRepository.layDanhSachTheoTrangThai(trangThai).stream()
                .filter(hd -> {
                    if (keyword.isEmpty()) return true;
                    String maHienThi = hd.getMaHienThi().toLowerCase();
                    String maSo = (hd.getMaHoaDon() != null) ? String.valueOf(hd.getMaHoaDon()) : "";
                    String soPhong = (hd.getDatPhong() != null && hd.getDatPhong().getSoPhong() != null)
                            ? hd.getDatPhong().getSoPhong().toLowerCase() : "";
                    String tenKhach = (hd.getDatPhong() != null
                            && hd.getDatPhong().getKhachHang() != null
                            && hd.getDatPhong().getKhachHang().getHoTen() != null)
                            ? hd.getDatPhong().getKhachHang().getHoTen().toLowerCase() : "";
                    return maHienThi.contains(keyword)
                            || maSo.contains(keyword)
                            || soPhong.contains(keyword)
                            || tenKhach.contains(keyword);
                })
                .sorted(Comparator.comparing(HoaDon::getMaHoaDon, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<DatPhong> layDanhSachPhongDangLuuTru() {
        return datPhongRepository.findAll().stream()
                .filter(dp -> laTrangThaiDangO(dp.getTrangThai()))
                .sorted(Comparator.comparing(DatPhong::getSoPhong, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public Double layTongDoanhThu() {
        return hoaDonRepository.tinhTongDoanhThu();
    }

    @Transactional
    public HoaDon layTheoId(Long id) {
        if (id == null) return null;
        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd != null && hd.getDatPhong() != null && hd.getDatPhong().getNgayTraPhong() != null) {
            // Đảm bảo tiền phòng khớp đúng số ngày từ ngày đến -> ngày đi
            long soNgayO = tinhSoNgayO(hd.getDatPhong(), hd.getDatPhong().getNgayTraPhong());
            double donGia = layDonGiaPhong(hd);
            double tienPhongChuan = soNgayO * donGia;
            double tienDv = (hd.getTienDichVu() != null) ? hd.getTienDichVu() : 0.0;
            if (!Objects.equals(hd.getTienPhong(), tienPhongChuan)) {
                hd.setTienPhong(tienPhongChuan);
                hd.setTongTien(tienPhongChuan + tienDv);
                hd = hoaDonRepository.save(hd);
            }
        }
        return hd;
    }

    // Bước 1: Mở trang Check-out -> Nếu đã có ngày đi thì tính theo ngày đi, chưa có mới tính đến hiện tại
    public ThongTinCheckOut chuanBiCheckOut(String soPhong) {
        if (soPhong == null || soPhong.isBlank()) {
            return null;
        }
        Room room = roomRepository.findById(soPhong.trim()).orElse(null);
        DatPhong datPhong = datPhongRepository.findAll().stream()
                .filter(dp -> dp.getSoPhong() != null && dp.getSoPhong().equalsIgnoreCase(soPhong.trim()))
                .filter(dp -> laTrangThaiDangO(dp.getTrangThai()))
                .max(Comparator.comparing(DatPhong::getNgayNhanPhong, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        if (room == null || datPhong == null) {
            return null;
        }

        LocalDateTime mocKetThuc = (datPhong.getNgayTraPhong() != null)
                ? datPhong.getNgayTraPhong()
                : LocalDateTime.now();

        long soNgayO = tinhSoNgayO(datPhong, mocKetThuc);
        double donGiaPhong = Math.max(0, room.getGiaPhong());
        double tienPhong = soNgayO * donGiaPhong;

        Double tienDichVuDb = hoaDonRepository.tinhTongTienDichVuTheoDatPhong(datPhong.getMaDatPhong());
        double tienDichVu = (tienDichVuDb != null) ? Math.max(0, tienDichVuDb) : 0.0;

        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaDatPhong(datPhong.getMaDatPhong());
        hoaDon.setDatPhong(datPhong);
        hoaDon.setNgayLap(mocKetThuc);
        hoaDon.setTienPhong(tienPhong);
        hoaDon.setTienDichVu(tienDichVu);
        hoaDon.setTongTien(tienPhong + tienDichVu);
        hoaDon.setTrangThai("Chưa thanh toán");

        ThongTinCheckOut info = new ThongTinCheckOut();
        info.hoaDon = hoaDon;
        info.soNgayO = soNgayO;
        info.donGiaPhong = donGiaPhong;
        info.thoiGianCheckOut = mocKetThuc;
        return info;
    }

    // Bước 2: Xác nhận thanh toán & Trả phòng
    @Transactional
    public HoaDon xacNhanThanhToanVaTraPhong(Long maDatPhong, String nguoiThucHien) {
        if (maDatPhong == null) {
            return null;
        }

        Object lock = checkoutLocks.computeIfAbsent(maDatPhong, k -> new Object());
        synchronized (lock) {
            try {
                DatPhong datPhong = datPhongRepository.findById(maDatPhong).orElse(null);
                if (datPhong == null) {
                    hoaDonLogService.ghiLog(
                            nguoiThucHien,
                            "Thanh toán Check-out trả phòng",
                            "Mã đặt phòng #" + maDatPhong,
                            "Không xác định",
                            "Không xác định",
                            "Thất bại - Không tìm thấy phiếu đặt phòng"
                    );
                    return null;
                }

                String tenKhach = (datPhong.getKhachHang() != null && datPhong.getKhachHang().getHoTen() != null)
                        ? datPhong.getKhachHang().getHoTen() : "Khách lẻ";

                // Chống bấm thanh toán trùng 2 lần
                if ("Đã trả phòng".equalsIgnoreCase(datPhong.getTrangThai())) {
                    HoaDon hdCu = hoaDonRepository.findFirstByMaDatPhongOrderByMaHoaDonAsc(maDatPhong).orElse(null);
                    hoaDonLogService.ghiLog(
                            nguoiThucHien,
                            "Thanh toán Check-out trả phòng",
                            "Phòng " + datPhong.getSoPhong() + " - Khách hàng: " + tenKhach,
                            "Đã thanh toán",
                            "Đã thanh toán",
                            "Thất bại - Hóa đơn đã được thanh toán trước đó"
                    );
                    return hdCu;
                }

                Room room = (datPhong.getSoPhong() != null && !datPhong.getSoPhong().isBlank())
                        ? roomRepository.findById(datPhong.getSoPhong().trim()).orElse(null)
                        : null;
                double donGiaPhong = (room != null) ? Math.max(0, room.getGiaPhong()) : 0.0;

                LocalDateTime thoiDiemThanhToan = LocalDateTime.now();
                // Nếu đã có ngày trả phòng thì tính đúng theo ngày trả phòng; chỉ khách chưa có ngày đi mới lấy giờ hiện tại
                LocalDateTime mocTinhTien = (datPhong.getNgayTraPhong() != null)
                        ? datPhong.getNgayTraPhong()
                        : thoiDiemThanhToan;

                long soNgayO = tinhSoNgayO(datPhong, mocTinhTien);
                double tienPhong = soNgayO * donGiaPhong;

                Double tienDichVuDb = hoaDonRepository.tinhTongTienDichVuTheoDatPhong(maDatPhong);
                double tienDichVu = (tienDichVuDb != null) ? Math.max(0, tienDichVuDb) : 0.0;
                double tongTien = tienPhong + tienDichVu;

                if (datPhong.getNgayTraPhong() == null) {
                    datPhong.setNgayTraPhong(thoiDiemThanhToan);
                }
                datPhong.setTrangThai("Đã trả phòng");
                datPhongRepository.save(datPhong);

                HoaDon hoaDon = hoaDonRepository.findFirstByMaDatPhongOrderByMaHoaDonAsc(maDatPhong)
                        .orElseGet(HoaDon::new);
                hoaDon.setMaDatPhong(maDatPhong);
                hoaDon.setTienPhong(tienPhong);
                hoaDon.setTienDichVu(tienDichVu);
                hoaDon.setTongTien(tongTien);
                hoaDon.setNgayLap(datPhong.getNgayTraPhong());
                hoaDon.setTrangThai("Đã thanh toán");
                HoaDon hoaDonDaLuu = hoaDonRepository.save(hoaDon);

                // Giải phóng phòng hiện tại của khách hàng
                KhachHang kh = datPhong.getKhachHang();
                if (kh != null) {
                    kh.setSoPhong(null);
                    khachHangRepository.save(kh);
                }

                if (room != null) {
                    room.setTrangThai("Còn trống");
                    roomRepository.save(room);
                }

                hoaDonLogService.ghiLog(
                        nguoiThucHien,
                        "Thanh toán Check-out trả phòng",
                        "Hóa đơn " + hoaDonDaLuu.getMaHienThi() + " - Phòng " + datPhong.getSoPhong()
                                + " - Khách hàng: " + tenKhach + " - Tổng thu: " + String.format("%,.0f", tongTien) + " đ",
                        "Chưa thanh toán",
                        "Đã thanh toán",
                        "Thành công"
                );

                return hoaDonDaLuu;
            } finally {
                checkoutLocks.remove(maDatPhong);
            }
        }
    }
}