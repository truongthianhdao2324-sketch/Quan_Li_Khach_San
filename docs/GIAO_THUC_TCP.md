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

Web gọi DichVuOrderService (không cho phép transaction bao ngoài). OrderService gọi DichVuService.goiDichVu qua Spring proxy: transaction tạo khoản tiền commit và giải phóng kết nối trước khi phương thức trả về. Sau đó NotificationService đọc snapshot bằng một transaction ngắn, đóng transaction, gửi TCP rồi cập nhật cờ bằng transaction ngắn khác. Cách này thay listener AFTER_COMMIT cũ vì callback đó có thể vẫn giữ tài nguyên transaction ban đầu. Nếu nhận ACK với ít nhất một lần ghi/flush thành công thì cập nhật da_gui_thong_bao; ACK với 0 người nhận hoặc timeout giữ cờ chưa gửi. Trạng thái đã chuyển tới client chưa có nghĩa nhân viên đã đọc/đã phục vụ.

Mã sự kiện là ID bản ghi su_dung_dich_vu, khác với UUID ma_yeu_cau dùng chống bấm lặp ở web. UUID được kiểm tra đủ dạng chuẩn rồi chuyển về chữ thường trước lookup/lưu để H2 và SQL Server xử lý nhất quán. Gửi lại TCP giữ nguyên ID và không chèn bản ghi tiền; thao tác web kiểm tra khoản thuộc đúng lượt ở, và không phát lại khoản đã đánh dấu gửi thành công. Client giữ tối đa 10.000 ID gần nhất trong RAM để ẩn bản sao; không có bảo đảm chống lặp qua lần restart hoặc hai thao tác gửi đồng thời trước khi cờ được cập nhật.

Tài nguyên dùng chung là tập kết nối, hàng đợi và writer của mỗi client. Tập dùng ConcurrentHashMap.newKeySet; mỗi subscriber có ArrayBlockingQueue tối đa 64 khung và một writer thread duy nhất để ghi nguyên khung theo FIFO. Offer/close được đồng bộ để không nhận thêm khung sau khi đóng. Server đưa thông điệp tới mọi hàng đợi trước khi chờ các future, dùng chung một deadline mặc định 1.500 ms; khung chỉ được tính vào ACK sau khi flush thành công. Quá hạn hoặc hàng đợi đầy thì đóng socket client đó, nhờ đó ngắt cả thao tác write bị kẹt. Không giữ khóa registry toàn cục khi ghi mạng.

Server accept tiếp trong luồng chính; từng kết nối dùng Runnable chạy bằng Thread riêng, log tên dichvu-client-N theo mẫu slide Chương 5 trang 37–39, cộng luồng dichvu-send-* cho subscriber. Synchronized bảo vệ bước đăng ký/gửi READY và tài nguyên vòng đời; chưa được bỏ đồng bộ chỉ vì cấu trúc tập kết nối đã thread-safe. Khi dừng, server đóng listener/socket, giải phóng hàng đợi, ngắt writer đang chờ và đợi các worker trong thời hạn chung. Sự khác biệt giữa Thread.start và gọi run trực tiếp phải giải thích được khi bảo vệ.

Parser chấp nhận LF hoặc CRLF; CR nằm giữa dòng bị từ chối. Điều này tránh trường hợp gửi nhiều CR không được tính vào giới hạn hoặc làm PI-CR-NG bị hiểu thành PING.
