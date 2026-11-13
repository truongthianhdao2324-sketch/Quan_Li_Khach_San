# Giao thức thông báo dịch vụ

Mỗi thông điệp là một dòng UTF-8 kết thúc bằng LF. Giới hạn 8.192 ký tự. Trường văn bản được mã hóa Base64 URL-safe không padding, không gửi trực tiếp dấu phân cách | hoặc xuống dòng. Server và client biết cùng token qua QLKS_SOCKET_TOKEN. TCP port mặc định 5000, HTTP web port mặc định 8080; hai vai trò server này là hai chương trình khác nhau.

| Bên gửi | Định dạng | Phản hồi |
| --- | --- | --- |
| Client phục vụ đăng ký | SUB\|base64(token)\|base64(tên client) | READY rồi chờ SERVICE |
| Web gửi dữ liệu đã commit | EVENT\|base64(token)\|id sử dụng dịch vụ\|ma_dat_phong\|base64(số phòng)\|base64(tên lúc gọi)\|số lượng\|thời điểm ISO local\|WEB | ACK\|id\|số kết nối nhận |
| Server chuyển tới phục vụ | SERVICE\|id\|ma_dat_phong\|base64(số phòng)\|base64(tên lúc gọi)\|số lượng\|thời điểm | Client hiển thị, loại lặp trong phiên |
| Client kiểm tra kết nối | PING | PONG |
| Sai token/định dạng/lệnh | — | ERROR\|AUTH hoặc FORMAT hoặc COMMAND |

Số lượng là số nguyên 1–999; ID dương. Server kiểm tra token, độ dài và các trường cần thiết trước khi phát. Không gửi CCCD, mật khẩu web hay thông tin đăng nhập SQL trên thông điệp.

## Từ lúc gọi dịch vụ đến lúc gửi thông báo

1. `DichVuOrderService` gọi `DichVuService.goiDichVu` để lưu khoản dịch vụ.
2. Giao dịch lưu hoàn tất và trả kết nối database về pool.
3. `DichVuNotificationService` đọc dữ liệu đã lưu bằng một transaction ngắn rồi gửi TCP.
4. Nếu ACK báo có ít nhất một kết nối ghi/flush thành công, cập nhật `da_gui_thong_bao`. Nếu không có người nhận hoặc bị timeout, giữ trạng thái chưa gửi để thử lại.

`DichVuOrderService` không nhận transaction bao ngoài. Các transaction đọc và cập nhật trạng thái được mở riêng; kết nối database được trả về trước khi chờ mạng.

## Gửi lại và chống tính tiền lặp

- `ma_yeu_cau` là UUID của thao tác gọi dịch vụ trên web. UUID được kiểm tra đúng dạng và chuyển về chữ thường trước khi lưu hoặc tìm lại.
- ID trong thông báo TCP là ID bản ghi `su_dung_dich_vu`. Gửi lại giữ nguyên ID này và không thêm khoản tính tiền.
- Chỉ gửi lại khoản thuộc lượt ở đang xem và chưa được đánh dấu gửi thành công.
- Client nhớ tối đa 10.000 ID trong phiên để bỏ qua thông báo trùng. Danh sách này mất khi khởi động lại; hai lần gửi đồng thời vẫn có thể phát trùng trước khi trạng thái được cập nhật.

ACK chỉ xác nhận ghi tới kết nối, chưa xác nhận nhân viên đã đọc hay phục vụ. Server không lưu thông báo để phát lại sau khi khởi động lại.

## Xử lý nhiều client

Server nhận kết nối trong luồng chính. Mỗi kết nối có một `Runnable` chạy bằng `Thread` riêng, tên `dichvu-client-N`. Mỗi client nhận thông báo có thêm một luồng `dichvu-send-*` và hàng đợi `ArrayBlockingQueue` tối đa 64 thông điệp.

- Tập kết nối dùng `ConcurrentHashMap.newKeySet`.
- Mỗi hàng đợi chỉ có một luồng ghi để giữ đúng thứ tự và tránh trộn dòng.
- Bước đăng ký, gửi READY, thêm vào hàng đợi và đóng tài nguyên được đồng bộ ở các đoạn tương ứng.
- Một lần phát gửi vào các hàng đợi rồi chờ chung tối đa 1.500 ms. ACK chỉ đếm các lần ghi/flush đã xong.
- Client quá chậm hoặc đầy hàng đợi sẽ bị ngắt. Khi dừng server, listener, socket, hàng đợi và các luồng xử lý đều được đóng hoặc giải phóng trong thời hạn chung.

Cách tạo luồng theo ví dụ `Runnable` cho từng kết nối trong Chương 5, trang 37–39. `Thread.start()` tạo luồng thực thi mới; gọi trực tiếp `run()` thì chạy trên luồng hiện tại.

## Đọc thông điệp

Chấp nhận kết thúc dòng LF hoặc CRLF. Ký tự CR ở giữa dòng bị từ chối để giữ đúng giới hạn độ dài và không ghép nhầm lệnh, ví dụ `PI` + CR + `NG` thành `PING`.
