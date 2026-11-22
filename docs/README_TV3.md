# Phần dịch vụ — TV3

Chức năng: quản lý danh mục dịch vụ, gọi dịch vụ cho khách đang lưu trú, xem lịch sử và gửi thông báo đến bộ phận phục vụ qua TCP.

## Chạy chương trình

Dùng JDK 17, database MySQL `QLKS` và tài khoản kết nối của nhóm trong `src/main/resources/application.properties`. MySQL cần chạy trước khi mở web. Nếu máy khác dùng mật khẩu hoặc cổng khác thì chỉnh cấu hình kết nối trên máy đó.

```powershell
./mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=none'
```

`ddl-auto=none` giữ nguyên các bảng đã tạo từ SQL nhóm. Không kèm script tạo database khác. Bảng `su_dung_dich_vu` cần có cột `ma_dat_phong`, cùng cột mà TV4 dùng để cộng tiền dịch vụ.

- `admin / 123456`: thêm, sửa, xóa dịch vụ chưa có lịch sử.
- `letan / 123456`: gọi dịch vụ, xem lịch sử, gửi lại thông báo.
- Menu bên trái chuyển sang các phần khác và giữ phiên đăng nhập. Các trang của thành viên khác cần có trong bản ghép chung.

## Chạy Socket

Sau khi biên dịch, mở mỗi lệnh ở một cửa sổ riêng. `JAVA_HOME` trỏ đến JDK 17.

```powershell
./mvnw.cmd compile

& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketServer 5000 127.0.0.1

& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketClient 127.0.0.1 5000 Phuc-vu-1
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketClient 127.0.0.1 5000 Phuc-vu-2
& "$env:JAVA_HOME/bin/java.exe" '-Dfile.encoding=UTF-8' -cp target/classes org.example.quan_li_khach_san.network.DichVuSocketClient 127.0.0.1 5000 Kho-bep
```

Gọi dịch vụ trên web để cả ba client nhận thông báo. Tắt server rồi gọi tiếp: khoản dịch vụ vẫn được lưu. Mở lại server và client, vào lịch sử để gửi lại thông báo; thao tác này không cộng thêm tiền.

## Các file chính

| Phần | File |
| --- | --- |
| Dữ liệu | `entity/DichVu.java`, `entity/SuDungDichVu.java` |
| Truy vấn | `repository/DichVuRepository.java`, `repository/SuDungDichVuRepository.java` |
| Xử lý dịch vụ | `service/DichVuService.java` |
| Gửi thông báo từ web | `service/ThongBaoDichVuService.java` |
| Điều khiển trang | `controller/DichVuController.java` |
| Server và client chạy riêng | `network/DichVuSocketServer.java`, `network/DichVuSocketClient.java` |
| Giao diện | Các trang dịch vụ trong `templates`, `fragments/dichvu.html`, `static/css/dichvu.css` |
| Hiển thị tiền | `util/TienViet.java` |

`TienViet` chỉ định dạng số tiền để hiển thị, không đổi cách tính tiền. Bootstrap được dùng tại `static/css/bootstrap.min.css`.

Lịch sử lấy từ `su_dung_dich_vu`. Khoản có `ma_dat_phong` được lấy theo mã đó. Khoản cũ chưa có mã chỉ hiển thị khi khớp duy nhất khách, phòng và khoảng thời gian lưu trú; việc đọc này không cập nhật SQL. Thành tiền cũ giữ nguyên, đơn giá hiển thị bằng thành tiền chia số lượng, tên dịch vụ theo danh mục hiện tại.

Mã chống bấm lặp được giữ tối đa 100 yêu cầu mỗi phiên; trạng thái gửi giữ tối đa 1.000 khoản trong bộ nhớ web. Khởi động lại sẽ mất trạng thái gửi, còn tiền và lịch sử vẫn ở database. Trước khi lập lại yêu cầu, kiểm tra lịch sử để tránh gọi trùng.

## Khi ghép phần dịch vụ

Chỉ giữ một `DatPhong` và một `DatPhongRepository`. TV3 dùng thông tin khách, số phòng, ngày nhận/trả và trạng thái lưu trú; repository cần giữ `findDangLuuTru` và `findLockedById`. Phần tính phòng/hóa đơn giữ theo mã của nhóm.

## Cách gửi thông báo

Web lưu khoản tiền xong mới gửi TCP. Server dùng `ServerSocket`; mỗi kết nối được xử lý bởi một `Thread`/`Runnable`. Các client chạy riêng bằng `Socket` và nhận cùng thông báo. Danh sách kết nối và thao tác ghi được đồng bộ bằng `synchronized`.

Dữ liệu UTF-8, mỗi thông điệp một dòng, các trường cách nhau bằng `|`, tối đa 4.096 ký tự:

```text
NHAN|Phuc-vu-1
GUI|id|maDatPhong|soPhong|tenDichVu|soLuong|thoiDiem
DICH_VU|id|maDatPhong|soPhong|tenDichVu|soLuong|thoiDiem
DA_GUI|id|soMayNhan
```

Client đăng ký bằng `NHAN`, nhận `SAN_SANG`. Web gửi `GUI`; server chuyển thành `DICH_VU` cho các client và trả `DA_GUI` về web. Thời điểm có dạng `2026-11-17T09:30:00`. Lệnh sai bị từ chối, kết nối lỗi bị đóng.

Server nhận tối đa 32 kết nối. Chạy trong mạng nội bộ tin cậy; mặc định cùng máy tại `127.0.0.1:5000`. Nếu chạy khác máy, đổi địa chỉ server trong web/client và cho phép cổng qua tường lửa.

`DA_GUI` xác nhận ghi đến kết nối, chưa xác nhận đã phục vụ. Client nhớ tối đa 1.000 ID để bỏ qua thông báo trùng trong lần chạy hiện tại. Server không lưu thông báo khi tắt. Client đọc quá chậm có thể làm lần phát chờ lâu; web chờ phản hồi tối đa 3 giây và vẫn giữ khoản tiền đã lưu.


## Ghi nhận sự cố và khoản bồi thường

Khoản bồi thường dùng đơn giá trong danh mục, do admin quy định. Ví dụ “Bồi thường vỡ bình hoa”; không tự tạo sẵn mức tiền mẫu trong database nhóm.

1. Admin thêm tên khoản và đơn giá trong Danh mục dịch vụ.
2. Lễ tân mở Ghi nhận dịch vụ / bồi thường, chọn đúng lượt ở và khoản đã quy định.
3. Nhập số lượng đồ bị hỏng, kiểm tra lựa chọn rồi xác nhận.
4. Hệ thống tính đơn giá × số lượng, lưu vào `su_dung_dich_vu` cùng `ma_dat_phong`. Khoản này xuất hiện trong lịch sử và được truy vấn tính tiền của TV4 cộng vào hóa đơn.

Giá gửi thêm từ trình duyệt không thay thế đơn giá danh mục. Bấm lặp cùng yêu cầu chỉ lưu một khoản; lượt đã trả phòng không được ghi thêm. Gửi lại thông báo không cộng thêm tiền. Thay đổi giá danh mục giữ nguyên khoản đã ghi.

Use case **Ghi nhận sự cố/bồi thường**: tác nhân chính là lễ tân; xảy ra khi khách đang lưu trú có hư hỏng cần bồi thường. Điều kiện: đã có lượt ở hợp lệ và khoản bồi thường được admin định giá. Kết quả: lưu khoản tiền theo lượt ở để thanh toán. Nhánh thay thế: chưa có khoản phù hợp thì admin bổ sung; nhập sai số lượng thì sửa lại; mất kết nối thông báo thì giữ khoản đã lưu và cho gửi lại. Đây là phần ghi nhận khoản tiền theo sự cố, chưa phải phân hệ theo dõi sửa chữa hoặc xử lý hoàn tất sự cố.
