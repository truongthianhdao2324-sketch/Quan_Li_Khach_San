package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RoomConcurrencyTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void kiemTraHaiLuongCungCapNhatMotPhong() throws Exception {

        // Lấy một phòng có sẵn trong cơ sở dữ liệu
        List<Room> danhSachPhong = roomRepository.findAll();

        assertFalse(danhSachPhong.isEmpty(),
                "Cơ sở dữ liệu chưa có phòng để kiểm thử!");

        Room phong = danhSachPhong.get(0);

        String soPhong = phong.getSoPhong();
        String trangThaiBanDau = phong.getTrangThai();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch sanSang = new CountDownLatch(2);
        CountDownLatch batDau = new CountDownLatch(1);

        try {
            System.out.println("========== KIỂM THỬ XỬ LÝ ĐỒNG THỜI ==========");
            System.out.println("Phòng kiểm thử: " + soPhong);

            // Luồng 1
            Future<String> luong1 = executor.submit(() -> {
                sanSang.countDown();
                batDau.await();

                return roomService.doiTrangThai(
                        soPhong, "Đang dọn phòng");
            });

            // Luồng 2
            Future<String> luong2 = executor.submit(() -> {
                sanSang.countDown();
                batDau.await();

                return roomService.doiTrangThai(
                        soPhong, "Còn trống");
            });

            // Chờ cả hai luồng sẵn sàng
            assertTrue(sanSang.await(10, TimeUnit.SECONDS),
                    "Hai luồng không sẵn sàng!");

            // Cho hai luồng bắt đầu
            batDau.countDown();

            String ketQua1 = luong1.get(30, TimeUnit.SECONDS);
            String ketQua2 = luong2.get(30, TimeUnit.SECONDS);

            System.out.println("Kết quả luồng 1: " + ketQua1);
            System.out.println("Kết quả luồng 2: " + ketQua2);

            // Kiểm tra cả hai yêu cầu đều thực hiện được
            assertTrue(ketQua1.contains("thành công"));
            assertTrue(ketQua2.contains("thành công"));

            // Kiểm tra dữ liệu cuối cùng
            Room phongSauKhiCapNhat =
                    roomRepository.findById(soPhong).orElseThrow();

            assertTrue(
                    List.of("Còn trống", "Đang dọn phòng")
                            .contains(phongSauKhiCapNhat.getTrangThai())
            );

            System.out.println("Trạng thái cuối cùng: "
                    + phongSauKhiCapNhat.getTrangThai());

            System.out.println("========== KIỂM THỬ HOÀN TẤT ==========");

        } finally {
            // Đảm bảo các luồng được giải phóng và kết thúc
            batDau.countDown();
            executor.shutdown();

            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
            }

            // Khôi phục trạng thái ban đầu của phòng
            roomService.doiTrangThai(soPhong, trangThaiBanDau);
        }
    }
}