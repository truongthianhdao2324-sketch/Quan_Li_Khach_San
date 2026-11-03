
HƯỚNG DẪN CÁC THÀNH VIÊN ĐƯA CODE LÊN GITHUB
### Đưa / Cập nhật Code trực tiếp trên Web GitHub
1. **Chuyển sang nhánh (branch) của bạn:**
   * Truy cập trang dự án GitHub, bấm vào nút chọn nhánh ở góc trái (mặc định đang hiện `main`).
   * Chọn đúng nhánh được phân công của bạn (`TV1`, `TV2`, `TV3`, `TV4`, hoặc `TV5`).
   * 
2. **Thêm hoặc cập nhật code:**
   * **Nếu tải file/thư mục mới lên:** Nhấn nút **Add file** (góc trên bên phải danh sách file) Chọn **Upload files** kéo thả các file code từ máy tính vào.
   * **Nếu sửa file có sẵn:** Nhấn vào file cần sửa Bấm biểu tượng **(Edit this file)** ở góc phải để sửa trực tiếp.

3. **Lưu lịch sử code (Commit):**
   * Kéo xuống phần **Commit changes...** ở cuối trang.
   * **BẮT BUỘC:** Nhập tên chức năng bạn vừa thêm vào ô mô tả (Xem phần lưu ý bên dưới).
   * Bấm nút màu xanh **Commit changes**.

4. **Gửi bài cho Nhóm trưởng hợp nhất (Pull Request):**
   * Bấm vào nút màu vàng **Compare & pull request** xuất hiện trên đầu trang.
   * Nhấn **Create pull request** để gửi thông báo cho tui duyệt code vào nhánh chính (`main`).
### Mọi người lưu ý
Làm tới đâu Commit tới đó (Commit thường xuyên)
**KHÔNG dồn hết code cả đồ án rồi mới đẩy lên 1 lần duy nhất vào cuối kỳ.**
* Mỗi khi viết xong 1 hàm, tạo xong 1 giao diện HTML, hoặc sửa xong 1 lỗi Thực hiện Commit và Push ngay.
* Quy tắc đặt tên nội dung Commit:Tên commit phải ghi rõ TÊN CHỨC NĂNG bạn vừa thêm hoặc chỉnh sửa.
**Cú pháp mẫu:** 
  * feat: <Tên chức năng bạn đã làm>
  * hoặc fix: <Lỗi bạn vừa sửa>
**Ví dụ:**
* feat: Thêm giao diện Quản lý phòng
* feat: Thêm chức năng Tìm kiếm khách hàng theo SĐT
* feat: Tạo API Đặt phòng
* Check-infix: Sửa lỗi không tính được tổng tiền hóa đơn
