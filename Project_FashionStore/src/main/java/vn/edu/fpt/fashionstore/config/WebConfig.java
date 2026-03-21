package vn.edu.fpt.fashionstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/css/**", "/js/**", "/images/**");
    }

    public static class SessionInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            // Không kiểm tra session cho các trang login/register
            String path = request.getRequestURI();
            if (path.contains("/login") || path.contains("/register") || path.contains("/css/") || 
                path.contains("/js/") || path.contains("/images/") || path.contains("/api/")) {
                return true;
            }

            HttpSession session = request.getSession(false);
            if (session != null) {
                // Cho phép multiple sessions cùng email - chỉ cần có session hợp lệ
                String email = (String) session.getAttribute("user");
                String role = (String) session.getAttribute("userRole");
                
                if (email != null && role != null) {
                    // Session hợp lệ, cho phép tiếp tục
                    return true;
                }
            }
            
            return true; // Cho phép tiếp tục, sẽ được xử lý ở controller level
        }
    }
}
