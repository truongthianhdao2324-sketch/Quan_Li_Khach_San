package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;


@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomLogService roomLogService;

    public RoomService(RoomRepository roomRepository,
                       RoomLogService roomLogService) {
        this.roomRepository = roomRepository;
        this.roomLogService = roomLogService;
    }

    // Xử lý thao tác trạng thái phòng có khóa đồng thời
    @Transactional
    public String thucHienThaoTac(String soPhong, String thaoTac) {

        String tenLuong = Thread.currentThread().getName();

        System.out.println("[" + tenLuong
                + "] Bắt đầu xử lý phòng " + soPhong);

        Optional<Room> ketQua =
                roomRepository.findBySoPhongForUpdate(soPhong);

        if (ketQua.isEmpty()) {
            roomLogService.ghiLog(
                    soPhong, thaoTac, "Không xác định",
                    "Không xác định", "Thất bại - Không tìm thấy phòng");

            return "Không tìm thấy phòng!";
        }

        Room phong = ketQua.get();

        String trangThaiCu = phong.getTrangThai();
        String trangThaiMoi;

        System.out.println("[" + tenLuong
                + "] Đã khóa phòng " + soPhong
                + ", trạng thái hiện tại: " + trangThaiCu);

        // Kiểm tra thao tác hợp lệ
        switch (thaoTac) {

            case "BAO_TRI":
                if (!"Còn trống".equals(trangThaiCu)) {
                    return ghiNhanThatBai(
                            soPhong, thaoTac, trangThaiCu,
                            "Chỉ được đưa phòng còn trống vào bảo trì!");
                }
                trangThaiMoi = "Bảo trì";
                break;

            case "HOAN_TAT_DON":
                if (!"Đang dọn phòng".equals(trangThaiCu)) {
                    return ghiNhanThatBai(
                            soPhong, thaoTac, trangThaiCu,
                            "Phòng không ở trạng thái đang dọn!");
                }
                trangThaiMoi = "Còn trống";
                break;

            case "HOAN_TAT_BAO_TRI":
                if (!"Bảo trì".equals(trangThaiCu)) {
                    return ghiNhanThatBai(
                            soPhong, thaoTac, trangThaiCu,
                            "Phòng không ở trạng thái bảo trì!");
                }
                trangThaiMoi = "Còn trống";
                break;

            default:
                return ghiNhanThatBai(
                        soPhong, thaoTac, trangThaiCu,
                        "Thao tác không hợp lệ!");
        }

        phong.setTrangThai(trangThaiMoi);

        roomRepository.saveAndFlush(phong);

        roomLogService.ghiLog(
                soPhong,
                thaoTac,
                trangThaiCu,
                trangThaiMoi,
                "Thành công"
        );

        System.out.println("[" + tenLuong
                + "] Hoàn tất phòng " + soPhong
                + ": " + trangThaiCu + " -> " + trangThaiMoi);

        return "Cập nhật trạng thái phòng thành công!";
    }

    private String ghiNhanThatBai(String soPhong, String thaoTac,
                                  String trangThaiCu, String thongBao) {

        roomLogService.ghiLog(
                soPhong,
                thaoTac,
                trangThaiCu,
                trangThaiCu,
                "Thất bại - " + thongBao
        );

        return thongBao;
    }

    // Thống kê đủ 4 trạng thái, kể cả trạng thái có số lượng bằng 0
    @Transactional(readOnly = true)
    public List<Object[]> thongKeTrangThaiDayDu() {

        Map<String, Long> ketQua = new LinkedHashMap<>();

        ketQua.put("Còn trống", 0L);
        ketQua.put("Đã có khách", 0L);
        ketQua.put("Đang dọn phòng", 0L);
        ketQua.put("Bảo trì", 0L);

        for (Object[] row : roomRepository.thongKeTheoTrangThai()) {
            String trangThai = (String) row[0];
            Long soLuong = ((Number) row[1]).longValue();

            if (ketQua.containsKey(trangThai)) {
                ketQua.put(trangThai, soLuong);
            }
        }

        List<Object[]> danhSach = new ArrayList<>();

        ketQua.forEach((trangThai, soLuong) ->
                danhSach.add(new Object[]{trangThai, soLuong}));

        return danhSach;
    }
}
