package vn.edu.fpt.fashionstore.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import vn.edu.fpt.fashionstore.service.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Tài nguyên tĩnh: Luôn cho phép để giao diện không bị vỡ
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/vendor/**").permitAll()

                        // 2. API endpoints: Cho phép truy cập API
                        .requestMatchers("/api/**").permitAll()

                        // 3. Trang chủ và xem hàng: Cho phép Guest xem thoải mái
                        .requestMatchers("/", "/home", "/products/**", "/product-details/**", "/search/**").permitAll()

                        // 4. Auth pages: Trang login/register/verify-otp không cần login
                        .requestMatchers("/login", "/register", "/verify-otp").permitAll()

                        // 5. Admin và Staff routes: Cho phép truy cập, controllers sẽ tự kiểm tra session
                        // Vì app dùng custom session-based auth, không dùng Spring Security authentication
                        .requestMatchers("/admin/**", "/staff/**").permitAll()

                        // 5. Các route khác: Cho phép truy cập, controllers sẽ tự kiểm tra session
                        // Vì app dùng custom session-based auth, không dùng Spring Security authentication
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable()); // Vẫn dùng Custom Login của bạn

        if (customOAuth2UserService != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/home", true)
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    )
                    .permitAll()
            );
        }

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
        );

        return http.build();
    }
}