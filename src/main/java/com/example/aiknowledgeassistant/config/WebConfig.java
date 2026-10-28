package com.example.aiknowledgeassistant.config;

import com.example.aiknowledgeassistant.exception.ApiProblem;
import com.example.aiknowledgeassistant.security.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                SessionUser.require(request);
                if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())
                        && !"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                    throw new ApiProblem(403, "请求来源校验失败，请刷新页面后重试");
                }
                return true;
            }
        }).addPathPatterns("/api/knowledge", "/api/knowledge/**", "/api/ask");
    }
}
