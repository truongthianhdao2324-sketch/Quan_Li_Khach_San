package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.entity.*;
import org.example.quan_li_khach_san.dto.*;
import org.example.quan_li_khach_san.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.UUID;
import java.math.BigDecimal;

@Controller
@RequestMapping("/dich-vu")
public class DichVuController {
    private final DichVuService service;
    private final DichVuNotificationService notifications;
    private final DichVuOrderService orders;
    public DichVuController(DichVuService service, DichVuNotificationService notifications, DichVuOrderService orders) {
        this.service = service; this.notifications = notifications; this.orders = orders;
    }
    @GetMapping
    public String danhSach(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "tenDichVu") String sort, @RequestParam(defaultValue = "asc") String dir, Model model) {
        var result = service.danhSach(q, page, sort, dir);
        var ordering = result.getSort().iterator().next();
        model.addAttribute("page", result);
        model.addAttribute("q", q.strip()); model.addAttribute("sort", ordering.getProperty());
        model.addAttribute("dir", ordering.getDirection().isDescending() ? "desc" : "asc");
        return "DanhSachDichVu";
    }
    @GetMapping("/them")
    public String them(Model model) { model.addAttribute("form", new DichVuForm()); return "ThemDichVu"; }
    @GetMapping("/{id}/sua")
    public String sua(@PathVariable Long id, Model model) {
        DichVu item = service.getDichVu(id);
        DichVuForm form = new DichVuForm(); form.setTenDichVu(item.getTenDichVu()); form.setGiaDichVu(item.getGiaDichVu()); form.setDangKinhDoanh(item.isDangKinhDoanh());
        model.addAttribute("form", form); model.addAttribute("id", id); return "ThemDichVu";
    }
    @PostMapping({"/luu", "/{id}/luu"})
    public String luu(@PathVariable(required = false) Long id, @ModelAttribute("form") DichVuForm form,
                      BindingResult errors, Model model, RedirectAttributes redirect) {
        if (form.getTenDichVu() == null || form.getTenDichVu().isBlank() || form.getTenDichVu().trim().length() > 100)
            errors.rejectValue("tenDichVu", "invalid", "Nhập tên dịch vụ từ 1 đến 100 ký tự.");
        BigDecimal price = form.getGiaDichVu();
        if (!errors.hasFieldErrors("giaDichVu") && (price == null || price.signum() < 0 || price.compareTo(new BigDecimal("9999999999.99")) > 0 || price.stripTrailingZeros().scale() > 2))
            errors.rejectValue("giaDichVu", "invalid", "Giá từ 0 đến 9.999.999.999,99; tối đa 2 số thập phân.");
        if (errors.hasErrors()) { model.addAttribute("id", id); return "ThemDichVu"; }
        service.luuDanhMuc(id, form); redirect.addFlashAttribute("success", "Đã lưu dịch vụ."); return "redirect:/dich-vu";
    }
    @PostMapping("/{id}/xoa")
    public String xoa(@PathVariable Long id, RedirectAttributes redirect) {
        try { service.xoaDanhMuc(id); redirect.addFlashAttribute("success", "Đã xóa dịch vụ chưa có lịch sử."); }
        catch (DichVuException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/dich-vu";
    }
    @GetMapping("/goi")
    public String goi(@RequestParam(required = false) Long maDatPhong, Model model) {
        GoiDichVuForm form = new GoiDichVuForm(); form.setMaDatPhong(maDatPhong); form.setMaYeuCau(UUID.randomUUID().toString());
        model.addAttribute("form", form); napLuaChon(model); return "GoiDichVu";
    }
    private void napLuaChon(Model model) { model.addAttribute("luotO", service.luotDangO()); model.addAttribute("dichVu", service.dichVuDangBan()); }
    @PostMapping("/goi")
    public String luuYeuCau(@ModelAttribute("form") GoiDichVuForm form, BindingResult errors,
            HttpSession session, Model model, RedirectAttributes redirect) {
        if (form.getMaDatPhong() == null) errors.rejectValue("maDatPhong", "required", "Chọn lượt đang lưu trú.");
        if (form.getMaDichVu() == null) errors.rejectValue("maDichVu", "required", "Chọn dịch vụ.");
        if (!errors.hasFieldErrors("soLuong") && (form.getSoLuong() == null || form.getSoLuong() < 1 || form.getSoLuong() > 999))
            errors.rejectValue("soLuong", "invalid", "Số lượng nguyên từ 1 đến 999.");
        if (errors.hasErrors()) { napLuaChon(model); return "GoiDichVu"; }
        DichVuOrderService.Result result;
        try { result = orders.goiVaThongBao(form, ((TaiKhoan)session.getAttribute("user")).getTenDangNhap()); }
        catch (DichVuException ex) { model.addAttribute("error", ex.getMessage()); napLuaChon(model); return "GoiDichVu"; }
        redirect.addFlashAttribute(result.daGui() ? "success" : "warning", result.daGui()
                ? "Đã lưu khoản dịch vụ #" + result.id() + " và chuyển thông báo tới client đang kết nối."
                : "Đã lưu khoản dịch vụ #" + result.id() + ". Chưa chuyển được thông báo đến client; có thể gửi lại, không tính thêm tiền.");
        return "redirect:/dich-vu/lich-su?maDatPhong=" + form.getMaDatPhong();
    }
    @GetMapping("/lich-su")
    public String lichSu(@RequestParam(required = false) Long maDatPhong, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("luotO", service.luotDangO());
        if (maDatPhong != null) {
            model.addAttribute("selectedStay", service.getLuotO(maDatPhong));
            var historyPage = service.lichSuPhanTrang(maDatPhong, page);
            model.addAttribute("history", historyPage.getContent());
            model.addAttribute("historyPage", historyPage);
            model.addAttribute("total", service.tongTienTheoLuotO(maDatPhong));
        }
        return "LichSuDichVu";
    }
    @PostMapping("/su-dung/{id}/gui-lai")
    public String guiLai(@PathVariable Long id, @RequestParam Long maDatPhong, RedirectAttributes redirect) {
        boolean sent = notifications.guiLai(id, maDatPhong);
        redirect.addFlashAttribute(sent ? "success" : "warning", sent ? "Đã gửi lại thông báo; tiền dịch vụ giữ nguyên." : "Chưa có client nhận hoặc chưa kết nối được server. Khoản dịch vụ vẫn đã lưu.");
        return "redirect:/dich-vu/lich-su?maDatPhong=" + maDatPhong;
    }
}
