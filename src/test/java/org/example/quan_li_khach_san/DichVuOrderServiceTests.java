package org.example.quan_li_khach_san;

import org.example.quan_li_khach_san.dto.GoiDichVuForm;
import org.example.quan_li_khach_san.service.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DichVuOrderServiceTests {
    @Test void failedSaveNeverSendsNotification() {
        DichVuService persistence = mock(DichVuService.class);
        DichVuNotificationService network = mock(DichVuNotificationService.class);
        GoiDichVuForm form = new GoiDichVuForm();
        when(persistence.goiDichVu(form, "letan")).thenThrow(new DichVuException("Lượt đã đóng"));
        assertThrows(DichVuException.class, () -> new DichVuOrderService(persistence, network).goiVaThongBao(form, "letan"));
        verifyNoInteractions(network);
    }
    @Test void notificationFailurePreservesSuccessfulChargeResult() {
        DichVuService persistence = mock(DichVuService.class);
        DichVuNotificationService network = mock(DichVuNotificationService.class);
        GoiDichVuForm form = new GoiDichVuForm();
        when(persistence.goiDichVu(form, "letan")).thenReturn(9L);
        when(network.guiLai(9L)).thenThrow(new IllegalStateException("Không cập nhật được trạng thái TCP"));
        var result = new DichVuOrderService(persistence, network).goiVaThongBao(form, "letan");
        assertEquals(9L, result.id()); assertFalse(result.daGui());
        var sequence = inOrder(persistence, network);
        sequence.verify(persistence).goiDichVu(form, "letan"); sequence.verify(network).guiLai(9L);
    }
}
