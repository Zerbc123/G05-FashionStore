package vn.edu.fpt.fashionstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // CẤP QUYỀN TRUY CẬP FILE TĨNH (CSS, JS, IMAGES) - Cực kỳ quan trọng
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // CÁC TRANG CÔNG KHAI
                        .requestMatchers("/", "/home", "/login", "/register", "/products/**", "/product-details/**", "/profile/**").permitAll()

                        // 1. CHẶN: Chỉ giỏ hàng và thanh toán mới cần Login
                        .requestMatchers("/cart/**", "/checkout/**", "/order/**").authenticated()

                        // 2. CÒN LẠI: Cho phép hết (để tránh lỗi load tài nguyên ngầm)
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login") // Phải khớp với @GetMapping trong Controller
                        .defaultSuccessUrl("/home", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/ss-logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}