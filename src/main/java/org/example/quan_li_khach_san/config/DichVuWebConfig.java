package org.example.quan_li_khach_san.config;

import jakarta.servlet.http.*;
import org.example.quan_li_khach_san.entity.TaiKhoan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/** Protect the new module without silently changing authentication of teammates' modules. */
@Configuration
public class DichVuWebConfig implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                HttpSession session = request.getSession(false);
                TaiKhoan user = session == null ? null : (TaiKhoan) session.getAttribute("user");
                if (user == null) { response.sendRedirect(request.getContextPath() + "/dang-nhap"); return false; }
                String role = user.getVaiTro();
                if (!"ADMIN".equalsIgnoreCase(role) && !"RECEPTIONIST".equalsIgnoreCase(role)) { response.sendError(403); return false; }
                // Use the resolved MVC route, not the raw URL (which can be percent-encoded).
                String route = String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
                boolean ordering = Set.of("/dich-vu/goi", "/dich-vu/su-dung/{id}/gui-lai").contains(route);
                boolean management = Set.of("/dich-vu/them", "/dich-vu/{id}/sua", "/dich-vu/{id}/luu", "/dich-vu/{id}/xoa", "/dich-vu/luu").contains(route);
                if ((ordering && !"RECEPTIONIST".equalsIgnoreCase(role)) || (management && !"ADMIN".equalsIgnoreCase(role))) { response.sendError(403); return false; }
                String csrf = (String) session.getAttribute("dichVuCsrf");
                if (csrf == null) { csrf = UUID.randomUUID().toString(); session.setAttribute("dichVuCsrf", csrf); }
                if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
                    String supplied = request.getParameter("_csrf");
                    if (supplied == null || !MessageDigest.isEqual(csrf.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) { response.sendError(403); return false; }
                }
                return true;
            }
        }).addPathPatterns("/dich-vu", "/dich-vu/**");
    }
}
