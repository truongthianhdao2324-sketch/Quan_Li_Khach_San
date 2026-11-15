# Ghép phần dịch vụ

Đối chiếu ngày 15/11/2026 với TV2 `ca6ddfb`, TV4 `fec2f17` và SQL `qlks.sql` trên main.

- Giữ tên bảng, cột và dữ liệu mẫu của nhóm. Chỉ bổ sung `su_dung_dich_vu.ma_dat_phong`, là cột truy vấn tổng dịch vụ của TV4 đang dùng. Không yêu cầu các cột phụ hoặc database mới.
- `DatPhong` dùng các tên `maDatPhong`, `maKhachHang`, `khachHang`, `soPhong`, `room`, ngày nhận/trả và trạng thái như TV4. Khi ghép chỉ giữ một entity và một repository; giữ thêm `findDangLuuTru` và `findLockedById` cho phần dịch vụ.
- Lượt gọi dịch vụ phải có trạng thái Đang lưu trú và chưa có ngày trả. Phần nhận phòng cần lưu bản ghi `dat_phong`, không chỉ đổi trạng thái phòng.
- TV3 lưu thành tiền bằng đơn giá × số lượng. Truy vấn TV4 `SUM(thanh_tien) WHERE ma_dat_phong = ...` đọc trực tiếp các khoản này. Kiểu tiền và cách tính phòng/hóa đơn của TV2/TV4 giữ nguyên.
- Khi ghép luồng thanh toán, cần đối chiếu việc khóa cùng lượt ở trước khi tính lại tiền và đóng lượt. TV3 có `DatPhongRepository.findLockedById`; thay đổi trong module hóa đơn do TV4/nhóm trưởng xem xét.
- SQL bổ sung chỉ gắn dữ liệu cũ khi khách, phòng và thời gian khớp duy nhất. Những dòng còn `ma_dat_phong IS NULL` cần kiểm tra trước khi demo hóa đơn cũ. Trong dữ liệu mẫu gốc, các khoản ID 10, 14, 20 chưa khớp khách/phòng/thời gian; script giữ nguyên để nhóm đối chiếu.

Menu dịch vụ dùng giao diện hồng và thanh bên của mẫu nhóm. Lễ tân đi sang `/khachhang/danhsach`, `/khachhang/phong`, `/hoadon/danhsach`; admin đi sang `/admin/dashboard`, `/admin/nhanvien`, `/admin/hoadon`. Chuyển trang giữ phiên đăng nhập; quyền thêm/sửa/xóa dịch vụ vẫn là admin, gọi dịch vụ là lễ tân.

Khung chấm nêu SQL Server, còn mã nhóm đang dùng MySQL. Phần TV3 hiện bám theo database nhóm; việc thống nhất hệ quản trị khi nộp cần nhóm trưởng xác nhận với giảng viên.
