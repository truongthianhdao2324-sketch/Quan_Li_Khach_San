# Ghép phần dịch vụ với các phần khác

Tài liệu dựa trên mã nhóm có ngày 13/11/2026: main `cdca6ca`, TV2 `ca6ddfb`, TV4 `f9a3daa`. Lúc kiểm tra, TV5 chưa có mã nhận phòng mới. Mã commit đầy đủ nằm trong [github_refs_20261113.json](github_refs_20261113.json).

## Ghép với đặt phòng — TV5

Phần dịch vụ dùng `DatPhong` để biết khách đang ở phòng nào. Khi ghép, dùng chung một `DatPhong` và một `DatPhongRepository` với các trường sau:

| Thuộc tính | Kiểu Java | Cột |
| --- | --- | --- |
| maDatPhong | Long | ma_dat_phong |
| khachHang | ManyToOne KhachHang | ma_khach_hang |
| phong | ManyToOne Room | so_phong |
| soPhong | String, chỉ đọc | so_phong |
| ngayNhanPhong | LocalDateTime | ngay_nhan_phong |
| ngayTraPhong | LocalDateTime, có thể null | ngay_tra_phong |
| trangThai | String | trang_thai |

`phong` ghi cột `so_phong`; thuộc tính `soPhong` chỉ đọc lại cột này. Nếu đổi tên thuộc tính khi ghép thì sửa cả các truy vấn đang dùng tên đó.

Khi nhận phòng, cần lưu bản ghi `dat_phong` có khách, phòng, ngày nhận và trạng thái **Đang lưu trú**. Dịch vụ chỉ nhận yêu cầu cho lượt chưa có ngày trả phòng.

Giữ các hàm `findDangLuuTru`, `findLockedById` và hàm tìm lượt đang ở theo số phòng. `findLockedById` khóa dòng bằng `PESSIMISTIC_WRITE` trong transaction, dùng chung với bước thanh toán bên dưới.

## Lấy tiền dịch vụ để tính hóa đơn — TV4

Hai hàm có thể gọi từ `DichVuService`:

- `tongTienTheoLuotO(Long maDatPhong)`: trả về `BigDecimal`, tổng tiền của toàn bộ lượt ở.
- `lichSu(Long maDatPhong)`: lấy các khoản dịch vụ kèm tên và đơn giá lúc gọi.

Tiền được cộng theo `ma_dat_phong`. Khách quay lại cùng phòng sẽ có lượt ở mới và lịch sử riêng.

Khi xác nhận thanh toán, thực hiện trong cùng một transaction:

1. Khóa lượt ở bằng `DatPhongRepository.findLockedById`.
2. Đọc lại tổng tiền dịch vụ từ database và tính hóa đơn trên máy chủ.
3. Lưu hóa đơn, cập nhật ngày trả và trạng thái lượt ở.

Phần gọi dịch vụ cũng khóa lượt ở trước, sau đó mới khóa dịch vụ. Giữ cùng thứ tự này để yêu cầu vừa gọi và thanh toán không bỏ sót tiền. Các số tiền gửi từ form chỉ để hiển thị, không dùng làm số tiền chốt hóa đơn.

Có bản sửa tham khảo tại [patches/TV4_ghep_dich_vu.patch](patches/TV4_ghep_dich_vu.patch), dùng sau khi ghép mã TV3 và TV4 `f9a3daa`. Bản sửa thêm `ThanhToanService`, tính lại tiền khi xác nhận, dùng `BigDecimal` cho hóa đơn, tải dữ liệu khách/phòng và kiểm tra CSRF. Cần xem lại diff nếu các tệp TV4 đã được sửa tiếp.

`Room.giaPhong` hiện vẫn là `double`; bản sửa dùng `BigDecimal.valueOf` khi tính hóa đơn. Tiền dịch vụ dùng `BigDecimal` từ lúc lưu đến lúc cộng tổng.

Bản ghép thử đạt 27 kiểm thử: 23 của TV3 và 4 kiểm tra thanh toán. Có kiểm tra gọi dịch vụ trong lúc thanh toán, bấm thanh toán lặp và khách quay lại cùng phòng. Các kiểm thử dùng H2. Tệp [ThanhToanIntegrationTests.java](kiem_thu_tich_hop/ThanhToanIntegrationTests.java) chỉ chép vào `src/test` sau khi đã ghép mã hóa đơn.

## Lưu ý với phần phòng — TV2

- `Room` và `RoomRepository` của TV3 đang giữ theo main.
- Khi nhận `RoomController` của TV2, lấy cả thay đổi bỏ handler danh sách phòng trong `KhachHangController` để tránh trùng `GET /khachhang/phong`.
- [patches/TV2_BigDecimal.patch](patches/TV2_BigDecimal.patch) chỉ cần nếu nhóm chuyển `Room.giaPhong` sang `BigDecimal`. Khi giá phòng vẫn là `double` thì giữ cách kiểm tra giá hiện tại.

## Các bảng và cột phần dịch vụ cần dùng

| Bảng | Các cột |
| --- | --- |
| dich_vu | ma_dich_vu, ten_dich_vu, gia_dich_vu, dang_kinh_doanh |
| su_dung_dich_vu | id, ma_dich_vu, ma_dat_phong, so_phong, so_giay_to, so_luong, thanh_tien, ngay_su_dung, ten_dich_vu_luc_goi, don_gia_luc_goi, ma_yeu_cau, nguoi_goi, da_gui_thong_bao |

Các cột tiền dịch vụ dùng `DECIMAL(15,2)`. `ma_yeu_cau` có ràng buộc duy nhất; `ma_dat_phong` là khóa ngoại tới lượt ở. Lịch sử cần được giữ khi ngừng cung cấp dịch vụ hoặc xử lý dữ liệu khách/phòng.

Nếu có dữ liệu sử dụng dịch vụ cũ, cần đối chiếu khách, phòng và thời gian để gắn đúng lượt ở. Các dòng không đủ dữ liệu hoặc khớp nhiều lượt cần kiểm tra riêng.

Main hiện cấu hình MySQL, còn khung chấm nêu SQL Server. Khi nhóm thống nhất database, cần ghép schema và cấu hình kết nối rồi chạy lại luồng nhận phòng → gọi dịch vụ → thanh toán. Script `sql/01_create_demo.sql` tạo database SQL Server mới; không dùng để nâng cấp database đang có. Kiểm tra dữ liệu trước khi đổi schema, không dựa vào `ddl-auto` để tự chuyển các dòng cũ.
