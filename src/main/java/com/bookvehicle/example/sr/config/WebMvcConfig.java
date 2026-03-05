package com.bookvehicle.example.sr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionInterceptor())
                .addPathPatterns("/**")
                // Loại trừ: auth pages, static resources
                .excludePathPatterns("/auth/**", "/css/**", "/js/**", "/images/**",
                        "/favicon.ico", "/error");
    }
}
