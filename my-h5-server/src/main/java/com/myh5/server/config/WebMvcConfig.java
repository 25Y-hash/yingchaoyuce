package com.myh5.server.config;

import com.myh5.server.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String uploadDir;

    public WebMvcConfig(AuthInterceptor authInterceptor,
                        @Value("${app.upload-dir}") String uploadDir) {
        this.authInterceptor = authInterceptor;
        this.uploadDir = uploadDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/user/**")
                .addPathPatterns("/prediction/analysis/*/comment")
                // 新增：观众预测发评论，需要登录
                .addPathPatterns("/audience/comment");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadDir + (uploadDir.endsWith("/") ? "" : "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}