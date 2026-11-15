package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.*;
import org.example.quan_li_khach_san.dto.*;
import org.example.quan_li_khach_san.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DichVuService {
    private final DichVuRepository services;
    private final SuDungDichVuRepository usages;
    private final DatPhongRepository stays;
    private final DichVuRequestCache requests;
    public DichVuService(DichVuRepository services, SuDungDichVuRepository usages,
                         DatPhongRepository stays, DichVuRequestCache requests) {
        this.services = services; this.usages = usages; this.stays = stays; this.requests = requests;
    }
    public Page<DichVu> danhSach(String keyword, int page, String sort, String direction) {
        String field = sort != null && Set.of("tenDichVu", "giaDichVu", "maDichVu").contains(sort) ? sort : "tenDichVu";
        Sort ordering = Sort.by("desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, field);
        if (!field.equals("maDichVu")) ordering = ordering.and(Sort.by("maDichVu"));
        String search = keyword == null ? "" : keyword.strip();
        Page<DichVu> result = services.findByTenDichVuContainingIgnoreCase(search, PageRequest.of(Math.max(0, page), 10, ordering));
        if (result.isEmpty() && result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages())
            result = services.findByTenDichVuContainingIgnoreCase(search, PageRequest.of(result.getTotalPages() - 1, 10, ordering));
        return result;
    }
    public DichVu getDichVu(Long id) { return services.findById(id).orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy dịch vụ.")); }
    public List<DichVu> dichVuDangBan() { return services.findAllByOrderByTenDichVuAsc(); }
    public List<DatPhong> luotDangO() { return stays.findDangLuuTru(); }
    public DatPhong getLuotO(Long id) {
        DatPhong stay = stays.findById(id).orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy lượt lưu trú."));
        // Initialize the two names while inside the transaction, without OpenEntityManagerInView.
        stay.getKhachHang().getHoTen(); stay.getRoom().getSoPhong();
        return stay;
    }
    public List<SuDungDichVu> lichSu(Long stayId) { return usages.findLichSu(stayId); }
    public Page<SuDungDichVu> lichSuPhanTrang(Long stayId, int page) {
        Page<SuDungDichVu> result = usages.findByDatPhongMaDatPhong(stayId,
                PageRequest.of(Math.max(0, page), 10, Sort.by(Sort.Direction.DESC, "ngaySuDung", "id")));
        if (result.isEmpty() && result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages())
            result = usages.findByDatPhongMaDatPhong(stayId, PageRequest.of(result.getTotalPages() - 1, 10,
                    Sort.by(Sort.Direction.DESC, "ngaySuDung", "id")));
        return result;
    }
    /** Tổng tiền của một lượt ở để phần hóa đơn sử dụng. */
    public BigDecimal tongTienTheoLuotO(Long stayId) { return usages.tongTienTheoLuotO(stayId); }

    @Transactional
    public void luuDanhMuc(Long id, DichVuForm form) {
        if (form.getTenDichVu() == null || form.getTenDichVu().isBlank() || form.getTenDichVu().trim().length() > 100)
            throw new DichVuException("Tên dịch vụ phải có từ 1 đến 100 ký tự.");
        BigDecimal price = form.getGiaDichVu();
        if (price == null || price.signum() < 0 || price.compareTo(new BigDecimal("9999999999.99")) > 0 || price.stripTrailingZeros().scale() > 2)
            throw new DichVuException("Đơn giá phải từ 0 đến 9.999.999.999,99 và tối đa 2 số thập phân.");
        DichVu item = id == null ? new DichVu() : services.findLockedById(id).orElseThrow(() -> new DichVuException("Không tìm thấy dịch vụ."));
        item.setTenDichVu(form.getTenDichVu().trim()); item.setGiaDichVu(price.setScale(2, RoundingMode.UNNECESSARY));
        services.save(item);
    }
    @Transactional
    public void xoaDanhMuc(Long id) {
        DichVu item = services.findLockedById(id).orElseThrow(() -> new DichVuException("Không tìm thấy dịch vụ."));
        if (usages.existsByDichVuMaDichVu(id)) throw new DichVuException("Dịch vụ đã có lịch sử sử dụng nên không thể xóa.");
        services.delete(item); services.flush();
    }
    @Transactional
    public Long goiDichVu(GoiDichVuForm form, String username) {
        if (form.getMaDatPhong() == null || form.getMaDichVu() == null || form.getSoLuong() == null || form.getSoLuong() < 1 || form.getSoLuong() > 999)
            throw new DichVuException("Chọn lượt lưu trú, dịch vụ và số lượng nguyên từ 1 đến 999.");
        String requestId;
        try {
            requestId = UUID.fromString(form.getMaYeuCau()).toString();
            if (!requestId.equalsIgnoreCase(form.getMaYeuCau())) throw new IllegalArgumentException();
        } catch (RuntimeException ex) { throw new DichVuException("Mã yêu cầu không hợp lệ. Hãy mở lại trang gọi dịch vụ."); }
        if (username == null || username.isBlank() || username.length() > 50)
            throw new DichVuException("Phiên đăng nhập không hợp lệ. Hãy đăng nhập lại.");
        // Khóa lượt ở trong lúc ghi tiền, để không ghi thêm sau khi lượt đã đóng.
        DatPhong stay = stays.findLockedById(form.getMaDatPhong()).orElseThrow(() -> new DichVuException("Không tìm thấy lượt lưu trú."));
        DichVuRequestCache.Request previous = requests.find(requestId);
        if (previous != null) {
            if (!previous.stayId().equals(form.getMaDatPhong()) || !previous.serviceId().equals(form.getMaDichVu())
                    || previous.quantity() != form.getSoLuong() || !previous.username().equals(username))
                throw new DichVuException("Mã yêu cầu này đã được dùng với nội dung khác. Hãy mở lại biểu mẫu.");
            return previous.usageId();
        }
        if (!stay.isDangLuuTru()) throw new DichVuException("Chỉ được gọi dịch vụ cho lượt đang lưu trú và chưa trả phòng.");
        DichVu item = services.findLockedById(form.getMaDichVu()).orElseThrow(() -> new DichVuException("Không tìm thấy dịch vụ."));
        SuDungDichVu usage = new SuDungDichVu();
        usage.setDatPhong(stay); usage.setDichVu(item);
        usage.setSoPhong(stay.getRoom().getSoPhong()); usage.setSoGiayTo(stay.getKhachHang().getSoGiayTo());
        usage.setSoLuong(form.getSoLuong()); usage.setThanhTien(item.getGiaDichVu().multiply(BigDecimal.valueOf(form.getSoLuong())));
        usage.setNgaySuDung(LocalDateTime.now());
        usages.saveAndFlush(usage);
        var receipt = new DichVuRequestCache.Request(form.getMaDatPhong(), form.getMaDichVu(),
                form.getSoLuong(), username, usage.getId());
        if (!requests.remember(requestId, receipt))
            throw new DichVuException("Yêu cầu này đang được xử lý. Hãy kiểm tra lịch sử trước khi thử lại.");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) requests.forget(requestId, receipt);
            }
        });
        return usage.getId();
    }
    public boolean daGui(Long id) { return requests.wasDelivered(id); }
}
