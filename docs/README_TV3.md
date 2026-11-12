# Dịch vụ khách sạn và thông báo TCP

Phạm vi: thành viên 3. Mã được chuẩn bị trên nhánh TV3, lấy nền main cdca6ca ngày 13/11/2026. Không đưa module hóa đơn TV4 hoặc module phòng TV2 vào phần nguồn triển khai TV3.

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

Kết quả kiểm tra ngày 13/11/2026: 23 kiểm thử TV3 đạt, không lỗi hoặc bỏ qua. Kết quả tóm tắt tại KET_QUA_TV3.json; kết quả đầy đủ sau khi chạy nằm trong target/surefire-reports. Kiểm thử tự dùng H2 riêng trong RAM, không truy cập database nhóm. Có kiểm tra 16 yêu cầu cùng mã, 8 yêu cầu khác mã đồng thời, khóa checkout, ba tiến trình client Java độc lập, lỗi mạng, CSRF và phân quyền.

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

## Database và bàn giao

Cấu hình database và driver MySQL hiện có trên main được giữ nguyên. Không chạy trực tiếp lên database nhóm chỉ để thử module mới; sử dụng preview/test trước khi nhóm trưởng ghép schema. qlks.sql của nhóm chưa có đầy đủ các cột lịch sử/khóa yêu cầu mà TV3 cần.

sql/01_create_demo.sql là script tham khảo SQL Server cho database mới, phù hợp hướng rubric; không phải bản nâng cấp database có sẵn và không tự chuyển cấu hình dự án sang SQL Server. Script đã kiểm tra riêng ở đợt trước; kiểm thử Java lần này dùng H2, chưa nghiệm thu JDBC SQL Server. Nhóm trưởng chốt hệ quản trị, hợp nhất schema và cấu hình chung trước khi chạy toàn hệ thống.

Xem BAN_GIAO_TICH_HOP.md để ghép đặt phòng TV5, hóa đơn TV4 và phòng TV2. Phần TV3 có thể bàn giao khi TV5 chưa đẩy mã; việc nghiệm thu toàn hệ thống được thực hiện sau khi nhóm trưởng ghép các module.
