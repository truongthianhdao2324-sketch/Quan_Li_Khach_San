package org.example.quan_li_khach_san.controller;

import jakarta.servlet.http.HttpSession;
import org.example.quan_li_khach_san.entity.DichVu;
import org.example.quan_li_khach_san.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/dich-vu")
public class DichVuController {
    private final DichVuService dichVuService;
    private final ThongBaoDichVuService thongBaoService;

    public DichVuController(DichVuService dichVuService, ThongBaoDichVuService thongBaoService) {
        this.dichVuService = dichVuService;
        this.thongBaoService = thongBaoService;
    }

    @InitBinder("form")
    public void gioiHanDuLieuForm(WebDataBinder binder) {
        binder.setAllowedFields("tenDichVu", "giaDichVu");
    }

    @GetMapping
    public String danhSach(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "tenDichVu") String sort, @RequestParam(defaultValue = "asc") String dir,
                           Model model) {
        var danhSach = dichVuService.danhSach(q, page, sort, dir);
        var sapXep = danhSach.getSort().iterator().next();
        model.addAttribute("page", danhSach);
        model.addAttribute("q", q.strip());
        model.addAttribute("sort", sapXep.getProperty());
        model.addAttribute("dir", sapXep.getDirection().isDescending() ? "desc" : "asc");
        return "DanhSachDichVu";
    }

    @GetMapping("/them")
    public String them(Model model) {
        model.addAttribute("form", new DichVu());
        return "ThemDichVu";
    }

    @GetMapping("/{id}/sua")
    public String sua(@PathVariable Long id, Model model) {
        model.addAttribute("form", dichVuService.getDichVu(id));
        model.addAttribute("id", id);
        return "ThemDichVu";
    }

    @PostMapping({"/luu", "/{id}/luu"})
    public String luu(@PathVariable(required = false) Long id, @ModelAttribute("form") DichVu dichVu,
                      BindingResult loi, Model model, RedirectAttributes thongBao) {
        if (dichVu.getTenDichVu() == null || dichVu.getTenDichVu().isBlank() || dichVu.getTenDichVu().trim().length() > 100)
            loi.rejectValue("tenDichVu", "invalid", "Nhập tên dịch vụ từ 1 đến 100 ký tự.");
        BigDecimal gia = dichVu.getGiaDichVu();
        if (!loi.hasFieldErrors("giaDichVu") && (gia == null || gia.signum() < 0
                || gia.compareTo(new BigDecimal("9999999999.99")) > 0 || gia.stripTrailingZeros().scale() > 2))
            loi.rejectValue("giaDichVu", "invalid", "Giá không âm, tối đa 9.999.999.999,99 và hai số thập phân.");
        if (loi.hasErrors()) {
            model.addAttribute("id", id);
            return "ThemDichVu";
        }
        dichVuService.luuDanhMuc(id, dichVu);
        thongBao.addFlashAttribute("success", "Đã lưu dịch vụ.");
        return "redirect:/dich-vu";
    }

    @PostMapping("/{id}/xoa")
    public String xoa(@PathVariable Long id, RedirectAttributes thongBao) {
        try {
            dichVuService.xoaDanhMuc(id);
            thongBao.addFlashAttribute("success", "Đã xóa dịch vụ chưa có lịch sử.");
        } catch (DichVuException loi) { thongBao.addFlashAttribute("error", loi.getMessage()); }
        return "redirect:/dich-vu";
    }

    private void napFormGoi(Model model, Long maDatPhong, Long maDichVu, String soLuong, String maYeuCau) {
        model.addAttribute("luotO", dichVuService.luotDangO());
        model.addAttribute("dichVu", dichVuService.dichVuDangBan());
        model.addAttribute("maDatPhong", maDatPhong);
        model.addAttribute("maDichVu", maDichVu);
        model.addAttribute("soLuong", soLuong);
        model.addAttribute("maYeuCau", maYeuCau);
    }

    @GetMapping("/goi")
    public String goi(@RequestParam(required = false) Long maDatPhong, Model model) {
        napFormGoi(model, maDatPhong, null, "1", UUID.randomUUID().toString());
        return "GoiDichVu";
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> yeuCauDaLuu(HttpSession phien) {
        Map<String, String> danhSach = (Map<String, String>) phien.getAttribute("dichVuDaLuu");
        if (danhSach == null) {
            danhSach = new LinkedHashMap<>();
            phien.setAttribute("dichVuDaLuu", danhSach);
        }
        return danhSach;
    }

    @PostMapping("/goi")
    public String luuYeuCau(@RequestParam(required = false) Long maDatPhong,
                          @RequestParam(required = false) Long maDichVu,
                          @RequestParam(defaultValue = "") String soLuong,
                          @RequestParam(defaultValue = "") String maYeuCau,
                          HttpSession phien, Model model, RedirectAttributes thongBao) {
        Integer soLuongNhap = null;
        if (maDatPhong == null) model.addAttribute("loiLuotO", "Chọn lượt đang lưu trú.");
        if (maDichVu == null) model.addAttribute("loiDichVu", "Chọn dịch vụ.");
        try {
            soLuongNhap = Integer.valueOf(soLuong);
            if (soLuongNhap < 1 || soLuongNhap > 999) throw new NumberFormatException();
        } catch (NumberFormatException loi) { model.addAttribute("loiSoLuong", "Nhập số lượng nguyên từ 1 đến 999."); }
        if (maDatPhong == null || maDichVu == null || soLuongNhap == null || soLuongNhap < 1 || soLuongNhap > 999) {
            napFormGoi(model, maDatPhong, maDichVu, soLuong, maYeuCau);
            return "GoiDichVu";
        }
        Long id;
        try {
            if (!UUID.fromString(maYeuCau).toString().equalsIgnoreCase(maYeuCau)) throw new IllegalArgumentException();
            String noiDung = maDatPhong + "|" + maDichVu + "|" + soLuongNhap;
            // Hai lần bấm của cùng một form trong phiên chỉ lưu một lần.
            synchronized (phien) {
                Map<String, String> daLuu = yeuCauDaLuu(phien);
                String cu = daLuu.get(maYeuCau.toLowerCase(Locale.ROOT));
                if (cu != null) {
                    if (!cu.startsWith(noiDung + "|")) throw new DichVuException("Yêu cầu đã được lưu với nội dung khác. Hãy mở lại form.");
                    id = Long.valueOf(cu.substring(cu.lastIndexOf('|') + 1));
                } else {
                    id = dichVuService.goiDichVu(maDatPhong, maDichVu, soLuongNhap);
                    daLuu.put(maYeuCau.toLowerCase(Locale.ROOT), noiDung + "|" + id);
                    if (daLuu.size() > 100) daLuu.remove(daLuu.keySet().iterator().next());
                }
            }
        } catch (IllegalArgumentException loi) {
            model.addAttribute("error", "Mã yêu cầu không hợp lệ. Hãy mở lại form gọi dịch vụ.");
            napFormGoi(model, maDatPhong, maDichVu, soLuong, UUID.randomUUID().toString());
            return "GoiDichVu";
        } catch (DichVuException loi) {
            model.addAttribute("error", loi.getMessage());
            napFormGoi(model, maDatPhong, maDichVu, soLuong, maYeuCau);
            return "GoiDichVu";
        }
        boolean daGui = thongBaoService.guiLai(id, maDatPhong);
        thongBao.addFlashAttribute(daGui ? "success" : "warning", daGui
                ? "Đã lưu khoản dịch vụ #" + id + " và chuyển thông báo tới bộ phận phục vụ."
                : "Đã lưu khoản dịch vụ #" + id + ". Chưa chuyển được thông báo; có thể gửi lại trong lịch sử, không tính thêm tiền.");
        return "redirect:/dich-vu/lich-su?maDatPhong=" + maDatPhong;
    }

    @GetMapping("/lich-su")
    public String lichSu(@RequestParam(required = false) Long maDatPhong,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("luotO", dichVuService.luotDangO());
        model.addAttribute("daGuiIds", thongBaoService.daGuiIds());
        if (maDatPhong != null) {
            model.addAttribute("selectedStay", dichVuService.getLuotO(maDatPhong));
            var lichSu = dichVuService.lichSuPhanTrang(maDatPhong, page);
            model.addAttribute("history", lichSu.getContent());
            model.addAttribute("historyPage", lichSu);
            model.addAttribute("total", dichVuService.tongTienTheoLuotO(maDatPhong));
        }
        return "LichSuDichVu";
    }

    @PostMapping("/su-dung/{id}/gui-lai")
    public String guiLai(@PathVariable Long id, @RequestParam Long maDatPhong, RedirectAttributes thongBao) {
        boolean daGui = thongBaoService.guiLai(id, maDatPhong);
        thongBao.addFlashAttribute(daGui ? "success" : "warning", daGui
                ? "Đã chuyển thông báo; tiền dịch vụ giữ nguyên." : "Chưa kết nối được máy nhận. Khoản dịch vụ vẫn đã lưu.");
        return "redirect:/dich-vu/lich-su?maDatPhong=" + maDatPhong;
    }
}
