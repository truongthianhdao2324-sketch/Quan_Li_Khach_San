package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.dto.GoiDichVuForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Web use case: commit AND release database resources before sending network data. */
@Service
public class DichVuOrderService {
    private static final Logger log = LoggerFactory.getLogger(DichVuOrderService.class);
    private final DichVuService service;
    private final DichVuNotificationService notifications;

    public DichVuOrderService(DichVuService service, DichVuNotificationService notifications) {
        this.service = service;
        this.notifications = notifications;
    }

    // Calling this inside another transaction is a programming error: a later rollback
    // must never undo a charge whose notification has already been transmitted.
    @Transactional(propagation = Propagation.NEVER)
    public Result goiVaThongBao(GoiDichVuForm form, String username) {
        Long id = service.goiDichVu(form, username); // Spring proxy commits before returning.
        boolean sent = false;
        try {
            sent = notifications.guiLai(id);
        } catch (RuntimeException ex) {
            // The charge is committed. Report pending notification rather than "save failed".
            log.warn("Đã lưu khoản #{}; chưa xác nhận được thông báo: {}", id, ex.getMessage());
        }
        return new Result(id, sent);
    }

    public record Result(Long id, boolean daGui) { }
}
