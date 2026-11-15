package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.service.DichVuException;
import org.example.quan_li_khach_san.service.DichVuNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@ControllerAdvice(assignableTypes = DichVuController.class)
public class DichVuErrorHandler {
    @ExceptionHandler(DichVuException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String businessError(DichVuException ex, Model model) { model.addAttribute("message", ex.getMessage()); return "LoiDichVu"; }
    @ExceptionHandler(DichVuNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(DichVuNotFoundException ex, Model model) { model.addAttribute("message", ex.getMessage()); return "LoiDichVu"; }
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String constraintError(Model model) { model.addAttribute("message", "Dữ liệu đã thay đổi hoặc đang được tham chiếu. Hãy tải lại trang và kiểm tra lịch sử trước khi thử lại."); return "LoiDichVu"; }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidParameter(Model model) { model.addAttribute("message", "Thông tin tra cứu không hợp lệ. Mã lượt ở, mã dịch vụ và trang phải là số nguyên."); return "LoiDichVu"; }
    @ExceptionHandler(PessimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String busyRecord(Model model) { model.addAttribute("message", "Lượt ở hoặc dịch vụ đang được cập nhật. Hãy kiểm tra lịch sử rồi thử lại với cùng mã yêu cầu."); return "LoiDichVu"; }
}
