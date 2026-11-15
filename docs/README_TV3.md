# Phần dịch vụ — TV3

## Chạy cùng database nhóm

1. Dùng `qlks.sql` và cấu hình MySQL của nhóm.
2. Trước khi ghép, nhóm trưởng đối chiếu cột `su_dung_dich_vu.ma_dat_phong` với `dat_phong.ma_dat_phong`. Code TV3 và truy vấn tổng dịch vụ TV4 đều dùng cột này. Việc cập nhật SQL chung và dữ liệu cũ do nhóm trưởng xử lý; TV3 không kèm script sửa database.
3. Mở dự án với JDK 17, chạy:

```powershell
./mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=none'
```

Tham số `ddl-auto=none` giữ các bảng đã tạo từ SQL, tránh Hibernate tự đổi kiểu cột. Cấu hình kết nối MySQL gốc của nhóm giữ nguyên.

Đăng nhập `admin / 123456` để thêm, sửa và xóa dịch vụ chưa có lịch sử; `letan / 123456` để gọi dịch vụ. Các trang dịch vụ có menu sang khách hàng, phòng và hóa đơn, giữ nguyên phiên đăng nhập. Trang hóa đơn cần mã TV4 được ghép vào dự án.

## Chạy server và ba client TCP

Sau khi compile, mở mỗi lệnh ở một terminal riêng. `JAVA_HOME` trỏ tới JDK 17.

```powershell
# Server
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketServer 5000 127.0.0.1

# Ba client
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketClient 127.0.0.1 5000 Phuc-vu-1
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketClient 127.0.0.1 5000 Phuc-vu-2
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketClient 127.0.0.1 5000 Kho-bep
```

Lễ tân chọn khách đang ở, dịch vụ và số lượng. Thành tiền = đơn giá × số lượng, lưu vào `su_dung_dich_vu`, sau đó gửi thông báo tới client. Khi mất kết nối, khoản tiền vẫn được lưu và có thể gửi lại thông báo trong lịch sử.

Mã chống bấm lặp và trạng thái gửi thông báo được nhớ trong bộ nhớ của web, tối đa 10.000 mục. Khi khởi động lại web, các trạng thái này mất; tiền và lịch sử vẫn nằm trong database. Kiểm tra lịch sử trước khi lập yêu cầu mới. Đơn giá lịch sử được tính từ thành tiền đã lưu chia số lượng; tên dịch vụ theo danh mục hiện tại.

## Kiểm tra

```powershell
./mvnw.cmd test
```

Kiểm thử dùng H2 ở chế độ MySQL với bảng lấy từ SQL gốc cộng cột `ma_dat_phong`, không dùng database nhóm. Kết quả sau khi chạy nằm trong `target/surefire-reports`.

Ngày 15/11/2026: 28 kiểm thử TV3 đạt; bản ghép thử với mã TV2/TV4 đạt 29 kiểm thử. Bản ghép đọc đúng tiền dịch vụ bằng truy vấn gốc của TV4, giữ nguyên kiểu tiền của các bạn. Các kết quả này dùng H2, chưa phải chạy trên MySQL thật của nhóm.

Có thể xem giao diện bằng dữ liệu tạm qua `PreviewApplication` trong `src/test`; dữ liệu này mất khi dừng và không thay thế việc chạy thử với MySQL của nhóm.

[Ghi chú ghép code](BAN_GIAO_TICH_HOP.md) · [Giao thức TCP](GIAO_THUC_TCP.md)
