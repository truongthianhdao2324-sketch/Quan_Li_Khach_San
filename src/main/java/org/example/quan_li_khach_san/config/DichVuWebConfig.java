package org.example.quan_li_khach_san.config;

import jakarta.servlet.http.*;
import org.example.quan_li_khach_san.entity.TaiKhoan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Configuration
public class DichVuWebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DichVuInterceptor()).addPathPatterns("/dich-vu", "/dich-vu/**");
    }

    private static class DichVuInterceptor implements HandlerInterceptor {
        private static final Set<String> ORDER_ROUTES = Set.of("/dich-vu/goi", "/dich-vu/su-dung/{id}/gui-lai");
        private static final Set<String> MANAGEMENT_ROUTES = Set.of("/dich-vu/them", "/dich-vu/{id}/sua",
                "/dich-vu/{id}/luu", "/dich-vu/{id}/xoa", "/dich-vu/luu");

        private String role(TaiKhoan user) {
            String role = user.getVaiTro() == null ? "" : user.getVaiTro().trim().toUpperCase(Locale.ROOT);
            return "LETAN".equals(role) ? "RECEPTIONIST" : role;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            HttpSession session = request.getSession(false);
            TaiKhoan user = session == null ? null : (TaiKhoan) session.getAttribute("user");
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/dang-nhap");
                return false;
            }
            String role = role(user);
            if (!Set.of("ADMIN", "RECEPTIONIST").contains(role)) {
                response.sendError(403);
                return false;
            }
            String route = String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
            if ((ORDER_ROUTES.contains(route) && !"RECEPTIONIST".equals(role))
                    || (MANAGEMENT_ROUTES.contains(route) && !"ADMIN".equals(role))) {
                response.sendError(403);
                return false;
            }
            String csrf = (String) session.getAttribute("dichVuCsrf");
            if (csrf == null) {
                csrf = UUID.randomUUID().toString();
                session.setAttribute("dichVuCsrf", csrf);
            }
            if (!Set.of("GET", "HEAD").contains(request.getMethod())) {
                String supplied = request.getParameter("_csrf");
                if (supplied == null || !MessageDigest.isEqual(csrf.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                    response.sendError(403);
                    return false;
                }
            }
            return true;
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView view) {
            if (view == null || view.getViewName() == null || view.getViewName().startsWith("redirect:")) return;
            TaiKhoan user = (TaiKhoan) request.getSession().getAttribute("user");
            view.addObject("dichVuAdmin", "ADMIN".equals(role(user)));
            view.addObject("dichVuLeTan", "RECEPTIONIST".equals(role(user)));
            view.addObject("dichVuRoute", String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)));
        }
    }
}
