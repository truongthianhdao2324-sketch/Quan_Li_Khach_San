# Hướng dẫn chạy đồ án: Hệ thống Quản lý Khách sạn
## 1. Tài khoản đăng nhập
* **Vai Trò Quản Trị Viên:** Tên đăng nhập :admin  Mật khẩu: 123456
* **Lễ tân:** letan / Mật khẩu: 123456 (hoặc letan1 / 123456)

## 2. Cấu hình CSDL MySQL
1. Chạy file qlks.sql để tạo database QLKS, 8 bảng và dữ liệu mẫu.

## 3. Cách chạy ứng dụng

### Bước 1: Chạy ứng dụng Web chính (Spring Boot)
* Mở project trên IntelliJ IDEA, chạy QuanLiKhachSanApplication.java.
* Mở trình duyệt truy cập: localhost:8080.

### Bước 2: Chạy cụm TCP Socket thông báo dịch vụ (tùy chọn)
1. Chạy DichVuSocketServer.java .
2. Chạy 1 hoặc nhiều tiến trình DichVuSocketClient.java (nhập tên trạm khi được hỏi, ví dụ: Phuc-vu-1, Phuc-vu-2, Phuc-vu-3).
3. Thao tác gọi dịch vụ trên Web để kiểm tra thông báo đẩy sang các cửa sổ Client.

### Bước 3: Chạy ứng dụng Client Kế toán (KeToanClientApp)
1. Đảm bảo QuanLiKhachSanApplication đang chạy ở cổng 8080.
2. Chạy hàm main() tại KeToanClientApp.java để đồng bộ hóa đơn từ xa qua /api/hoadon/dong-bo.
