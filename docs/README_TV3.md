# Dịch vụ khách sạn và thông báo TCP

Phần TV3 gồm quản lý dịch vụ, ghi nhận dịch vụ theo lượt ở và gửi thông báo TCP đến các client phục vụ.

## Chức năng

- Admin thêm, sửa, tìm kiếm, sắp xếp và phân trang danh mục dịch vụ. Dịch vụ đã có lịch sử được ngừng cung cấp thay cho xóa.
- Lễ tân gọi dịch vụ cho lượt đang lưu trú, xem lịch sử và gửi lại thông báo chưa chuyển được.
- Ghi nhận tên và giá tại thời điểm gọi, số lượng, người gọi và mã yêu cầu. Dùng BigDecimal cho tiền dịch vụ và tổng tiền, không lấy đơn giá từ trình duyệt.
- Tổng tiền và lịch sử phân theo mã lượt lưu trú, không gộp các lần khách ở cùng phòng.
- Khóa bản ghi lượt ở khi ghi tiền; mã yêu cầu duy nhất giúp gửi lặp không tính tiền hai lần.
- Lưu giao dịch trước, sau đó mới gửi TCP. Lỗi kết nối giữ khoản tiền và trạng thái chờ gửi lại.
- Server Java tiếp nhận nhiều kết nối, client Java chạy độc lập; giới hạn hàng đợi, thời gian gửi và tài nguyên khi ngắt kết nối.

## Chạy kiểm thử

Yêu cầu JDK 17. Mở PowerShell tại thư mục chứa pom.xml:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
./mvnw.cmd test
```

Ngày 13/11/2026: 23 kiểm thử đạt. Các trường hợp gồm gửi yêu cầu đồng thời, chống tính tiền lặp, khóa lượt ở, ba client Java độc lập, lỗi mạng và phân quyền. Kiểm thử dùng H2 trong RAM. Xem [kết quả](KET_QUA_TV3.json) hoặc `target/surefire-reports` sau khi chạy.

## Xem giao diện bằng dữ liệu mẫu

```powershell
./mvnw.cmd spring-boot:test-run '-Dspring-boot.run.main-class=org.example.quan_li_khach_san.PreviewApplication' '-Dspring-boot.run.arguments=--server.port=8081 --server.address=127.0.0.1 --qlks.socket.port=5000'
```

Mở http://localhost:8081/dang-nhap. Tài khoản admin/123456 và letan/123456. Dữ liệu gồm khách Nguyễn Văn An, phòng 202 và các dịch vụ thông thường. Đây là dữ liệu tạm, mất khi dừng ứng dụng; PreviewApplication chỉ nằm trên test classpath.

Nếu môi trường Windows báo lỗi tạo socket tạm của JDK, tạo thư mục .tools/nio-temp rồi thêm tham số '-Dspring-boot.run.jvmArguments=-Dfile.encoding=UTF-8 -Djdk.net.unixdomain.tmpdir=.tools/nio-temp'. Không cần đổi cấu hình Windows để sử dụng bản xem thử.

## Server và client TCP

Sau khi compile, mở các terminal riêng tại thư mục dự án. Mỗi terminal cần JAVA_HOME trỏ tới JDK 17.

```powershell
# Server
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketServer 5000 127.0.0.1

# Ba client, mỗi lệnh ở một terminal riêng
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketClient 127.0.0.1 5000 Phuc-vu-1
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketClient 127.0.0.1 5000 Phuc-vu-2
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketClient 127.0.0.1 5000 Kho-bep
```

Đăng nhập lễ tân, gọi hai chai nước suối giá 20.000 đồng cho phòng 202. Lịch sử ghi 40.000 đồng và ba client nhận cùng mã khoản dịch vụ. Log của lần chạy ba tiến trình Java tại bang_chung_tcp. Tắt server rồi gọi khoản mới: dữ liệu vẫn được lưu; bật server/client và gửi lại thông báo không thêm khoản tính tiền.

Cấu hình TCP qua QLKS_SOCKET_HOST, QLKS_SOCKET_PORT, QLKS_SOCKET_TOKEN. Token nội bộ phải giống nhau giữa web, server và client. ACK xác nhận ghi tới kết nối, không xác nhận nhân viên đã đọc hoặc phục vụ. Không có lưu bền/replay khi server khởi động lại; client loại thông báo trùng trong phiên.

## Database

Dự án giữ cấu hình MySQL hiện có của nhóm. `qlks.sql` chưa có đủ các cột mới cho lịch sử dịch vụ và mã yêu cầu, nên dùng bản xem thử ở trên trước khi ghép schema vào database chung.

`sql/01_create_demo.sql` tạo database SQL Server mới để chạy dữ liệu mẫu. Script đã được kiểm tra riêng; phần Java hiện được kiểm thử bằng H2, chưa chạy cùng database SQL Server của nhóm.

Các trường dữ liệu cần ghép với đặt phòng và hóa đơn được ghi trong [hướng dẫn ghép code](BAN_GIAO_TICH_HOP.md).
