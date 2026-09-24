package com.assetmanagement.asset_management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cho phép frontend chạy ở origin khác (ví dụ Vite dev server tại
 * http://localhost:5173) gọi được API.
 *
 * Lưu ý: bean này chỉ có tác dụng khi SecurityConfig có bật .cors(...)
 * trong SecurityFilterChain — nếu không, request preflight OPTIONS sẽ bị
 * Spring Security chặn trước khi tới tầng MVC.
 *
 * Danh sách origin đọc từ biến môi trường CORS_ALLOWED_ORIGINS
 * (phân tách bằng dấu phẩy), mặc định là 2 cổng dev thường dùng của Vite.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        // FE gửi JWT qua header Authorization chứ không dùng cookie, nên không
        // cần allowCredentials. Để false giúp tránh ràng buộc phải liệt kê
        // origin tuyệt đối khi muốn mở rộng sau này.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
