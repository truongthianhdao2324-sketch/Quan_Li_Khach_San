# Bàn giao tích hợp phần dịch vụ

Ngày đối chiếu: 13/11/2026. Các commit nguồn lưu trong github_refs_20261113.json.

| Phần | Nguồn hiện tại | Liên quan TV3 |
|---|---|---|
| main | cdca6ca | Nền chung, đã có liên kết hóa đơn nhưng chưa có mã xử lý hóa đơn |
| TV2 | ca6ddfb | Phòng; TV3 dùng khóa ngoại so_phong, không phụ thuộc RoomController |
| TV4 | f9a3daa | Hóa đơn đọc tổng dịch vụ theo ma_dat_phong |
| TV5 | 792a2ca | Chưa có cập nhật module nhận phòng; dùng mapping DatPhong tối thiểu để kiểm tra TV3 |

## Hợp đồng với TV5

Chỉ giữ một entity DatPhong và một DatPhongRepository. Các thuộc tính cần có:

| Thuộc tính | Kiểu Java | Cột |
|---|---|---|
| maDatPhong | Long | ma_dat_phong |
| khachHang | ManyToOne KhachHang | ma_khach_hang |
| phong | ManyToOne Room | so_phong |
| soPhong | String, ánh xạ chỉ đọc | cùng cột so_phong, không ghi hai lần |
| ngayNhanPhong | LocalDateTime | ngay_nhan_phong |
| ngayTraPhong | LocalDateTime, có thể null | ngay_tra_phong |
| trangThai | String | trang_thai |

Lượt nhận dịch vụ phải có trạng thái Đang lưu trú và chưa có ngày trả. Check-in cần lưu bản ghi dat_phong; chỉ đặt trạng thái phòng Đã có khách không tạo ra lượt ở hợp lệ cho TV3.

Giữ các phương thức repository findDangLuuTru, findLockedById và phương thức tìm lượt đang ở theo phòng. findLockedById dùng PESSIMISTIC_WRITE trong transaction. Khi ghép mapping TV5, giữ tên thuộc tính tương ứng hoặc sửa đồng bộ các truy vấn tham chiếu. Không tạo entity thứ hai cho cùng bảng.

## Hợp đồng với TV4

- DichVuService.tongTienTheoLuotO(Long): BigDecimal, tổng tất cả khoản dịch vụ của đúng lượt ở, không phụ thuộc phân trang.
- DichVuService.lichSu(Long): toàn bộ lịch sử của lượt ở, giữ tên/đơn giá lúc gọi.
- su_dung_dich_vu.ma_dat_phong phải là khóa ngoại tới dat_phong; không cộng tiền theo số phòng hoặc giấy tờ khách.
- Trước khi chốt hóa đơn: mở transaction, khóa lượt ở bằng DatPhongRepository.findLockedById, tính lại tiền dịch vụ từ database, lưu hóa đơn và đóng lượt ở trong cùng transaction. Không dùng số tiền từ input ẩn để chốt hóa đơn.
- Dùng cùng thứ tự khóa: lượt ở trước; TV3 sau đó mới khóa dịch vụ. Không thêm luồng checkout bỏ qua khóa này.

Bản vá patches/TV4_ghep_dich_vu.patch dành cho nhóm trưởng sau khi lấy mã TV4 f9a3daa và TV3. Bản vá điều chỉnh kiểu tiền hóa đơn, thêm ThanhToanService, tải đủ dữ liệu hiển thị, bổ sung CSRF cho hóa đơn và tính lại tiền khi xác nhận. Không tự áp dụng vào nhánh TV4. Không áp dụng lặp nếu nhóm trưởng đã sửa các tệp tương ứng.

Bản vá giữ Room.giaPhong kiểu double hiện tại của nhóm, chuyển qua BigDecimal.valueOf ở ranh giới tính hóa đơn. Việc chuyển toàn bộ tiền phòng/nhân viên sang DECIMAL và BigDecimal là quyết định dùng chung, không nằm trong nguồn TV3 đưa lên. Trong lúc nhóm chưa đổi kiểu phòng, các khoản dịch vụ vẫn dùng BigDecimal xuyên suốt.

Kiểm tra trên bản ghép riêng: 23 kiểm thử TV3 + 4 kiểm thử thanh toán, tổng 27 đạt. Bao gồm tiền phát sinh sau khi mở trang thanh toán, sửa tiền/mã hóa đơn trong POST, hai lần thanh toán đồng thời, checkout chờ khóa gọi dịch vụ, và lượt ở tiếp theo không nhận tiền cũ. Kết quả dùng H2; không thay cho nghiệm thu SQL Server chung. Bộ kiểm thử bổ sung tại kiem_thu_tich_hop/ThanhToanIntegrationTests.java chỉ đưa vào src/test sau khi ghép mã hóa đơn.

## Hợp đồng với TV2

Room và RoomRepository trên nhánh TV3 vẫn giữ nguyên nền main. TV3 không thay mã CRUD, xử lý phòng hoặc kiểm thử của TV2. Nếu nhóm trưởng chuyển Room.giaPhong sang BigDecimal thì áp dụng patches/TV2_BigDecimal.patch để sửa hai biểu thức kiểm tra giá trong RoomController; không áp dụng bản vá này khi Room vẫn dùng double.

Khi ghép RoomController, cần nhận thay đổi bỏ mapping danh sách phòng trong KhachHangController của TV2 để tránh trùng GET /khachhang/phong. Đây là việc ghép module phòng, không phải bổ sung controller thứ hai trong TV3.

## Schema TV3 và ranh giới dữ liệu

- dich_vu: ma_dich_vu, ten_dich_vu, gia_dich_vu DECIMAL(15,2), dang_kinh_doanh.
- su_dung_dich_vu: id, ma_dich_vu, ma_dat_phong, so_phong, so_giay_to, so_luong, thanh_tien DECIMAL(15,2), ngay_su_dung, ten_dich_vu_luc_goi, don_gia_luc_goi DECIMAL(15,2), ma_yeu_cau duy nhất, nguoi_goi, da_gui_thong_bao.
- Không xóa dây chuyền lịch sử khi xóa danh mục, khách hoặc phòng.
- Các khoản cũ cần gắn đúng lượt ở theo khách/phòng/thời gian. Dữ liệu không đủ hoặc nhiều lượt khớp phải đối chiếu, không tự gán vào lượt đang ở mới nhất.

Cấu hình MySQL trong main khác hướng SQL Server của rubric. TV3 không tự thay cấu hình cả nhóm. Nhóm trưởng hợp nhất schema và chọn driver/cấu hình chung; script SQL Server tạo database mới đã được chuẩn bị riêng. Không bật ddl-auto lên database nhóm để thay cho việc kiểm tra và chuyển dữ liệu có sẵn.

## Trách nhiệm sau bàn giao

TV3 hoàn thành chức năng dịch vụ, lịch sử, tổng tiền, thông báo TCP và kiểm thử module. TV5 cung cấp nhận phòng/dữ liệu lượt ở; TV4 cung cấp hóa đơn; nhóm trưởng hợp nhất ứng dụng và chạy nghiệm thu đầu cuối. Khi ghép phát hiện lỗi thuộc mã TV3, sửa trong TV3; không yêu cầu TV5 tự sửa thay phần dịch vụ. Việc chưa có mã TV5 không ngăn bàn giao phạm vi TV3, nhưng chưa thể khẳng định toàn hệ thống đã hoàn tất.
