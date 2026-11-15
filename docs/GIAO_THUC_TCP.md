# Thông báo dịch vụ qua TCP

Web và các client dùng UTF-8, mỗi thông điệp một dòng LF hoặc CRLF, tối đa 8.192 ký tự. Chuỗi văn bản mã hóa Base64 URL-safe để không lẫn dấu `|` hoặc xuống dòng. Cổng mặc định 5000; token lấy từ `QLKS_SOCKET_TOKEN` ở cả web, server và client.

| Lệnh | Nội dung | Phản hồi |
| --- | --- | --- |
| SUB | token, tên client | READY |
| EVENT | token, ID khoản dịch vụ, mã lượt ở, phòng, tên dịch vụ, số lượng, thời điểm, WEB | ACK, ID, số kết nối đã nhận |
| SERVICE | ID khoản dịch vụ, mã lượt ở, phòng, tên dịch vụ, số lượng, thời điểm | Client hiển thị |
| PING | Kiểm tra kết nối | PONG |

Các trường ngăn cách bằng `|`; token, tên client, số phòng và tên dịch vụ được mã hóa Base64. Lệnh sai trả `ERROR|AUTH`, `ERROR|FORMAT` hoặc `ERROR|COMMAND`.

## Luồng xử lý

1. Web kiểm tra dữ liệu và khóa lượt ở trong transaction.
2. Lưu khoản dịch vụ vào database, hoàn tất transaction rồi mới gửi TCP.
3. Server chuyển tới các client đang kết nối. Mỗi client có hàng đợi 64 thông điệp và một luồng ghi, tránh trộn dòng khi gửi đồng thời.
4. ACK có ít nhất một kết nối nhận thì web ghi nhớ trạng thái đã gửi trong bộ nhớ. Nếu lỗi mạng, có thể gửi lại cùng ID khoản tiền.

Mỗi kết nối có một `Runnable` chạy bằng `Thread.start()`, tên luồng `dichvu-client-N`. Danh sách kết nối dùng tập an toàn luồng; thao tác đăng ký, ghi và đóng được đồng bộ. Thời gian chờ cho một lần phát tối đa 1.500 ms. Client quá chậm hoặc hàng đợi đầy bị ngắt; khi dừng server, socket, hàng đợi và các luồng đều được giải phóng.

## Giới hạn

- ACK xác nhận ghi tới kết nối, chưa xác nhận đã phục vụ.
- Client nhớ tối đa 10.000 ID trong phiên để bỏ qua thông báo lặp.
- Mã chống bấm lặp và trạng thái gửi được nhớ trong phiên chạy web, không thêm cột vào SQL nhóm. Khởi động lại web sẽ mất các trạng thái này, còn khoản tiền đã lưu vẫn giữ nguyên.
- Server không lưu thông báo để phát lại sau khi khởi động lại.
