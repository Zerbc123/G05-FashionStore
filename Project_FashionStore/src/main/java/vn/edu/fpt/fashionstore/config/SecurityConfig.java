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

                        // 2. Trang chủ và xem hàng: Cho phép Guest xem thoải mái
                        .requestMatchers("/", "/home", "/products/**", "/product-details/**", "/search/**").permitAll()

                        // 3. Auth pages: Trang login/register/verify-otp/edit-profile
                        .requestMatchers(
                                "/login", "/register", "/verify-otp",
                                "/customer/change-password", "/customer/profile"
                        ).authenticated()

                        .requestMatchers("/customer/**").authenticated()

                        // 4. CHẶN: Chỉ khi thao tác với Giỏ hàng, Thanh toán, và Profile cá nhân mới yêu cầu Login
                        // Lưu ý: "/cart/**" sẽ chặn cả trang xem giỏ hàng và API thêm vào giỏ
                        .requestMatchers("/cart/**", "/checkout/**", "/order/**", "/profile/").authenticated()

                        // 5. Các request khác (nếu có)
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