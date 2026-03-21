package vn.edu.fpt.fashionstore.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.entity.Account;
import java.io.IOException;

public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        String email = user.getAttribute("email");
        String name = user.getAttribute("name");

        HttpSession session = request.getSession();
        session.setAttribute("user", email);
        session.setAttribute("userName", name);

        // Lấy role từ Spring Security
        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        // Lưu role vào session
        session.setAttribute("userRole", role);

        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("Admin"))) {
            response.sendRedirect("/fashionstore/admin");
        }

        else if (
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("Nhân viên bán hàng (Sale)")) ||
                        authentication.getAuthorities().contains(new SimpleGrantedAuthority("Quản lý kho (Stock)")) ||
                        authentication.getAuthorities().contains(new SimpleGrantedAuthority("Hỗ trợ khách hàng (Support)")) ||
                        authentication.getAuthorities().contains(new SimpleGrantedAuthority("Quản lý cửa hàng (Manager)"))
        ) {
            response.sendRedirect("/fashionstore/staff");
        }

        else {
            response.sendRedirect("/fashionstore/home");
        }
    }
}