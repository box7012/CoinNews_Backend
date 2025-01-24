package app.message.demo1;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://coin-dashboard.xyz")  // 허용할 출처 명시
                .allowedMethods("GET", "POST")  // 허용할 HTTP method
                .allowCredentials(true);  // 쿠키 인증 요청 허용
    }
}