package jpa.basic.coffeeshop.common.security.config;

import jpa.basic.coffeeshop.common.interceptor.AdminInterceptor;
import jpa.basic.coffeeshop.common.interceptor.AuthModelInterceptor;
import jpa.basic.coffeeshop.common.security.LoginUserArgumentResolver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginUserArgumentResolver loginUserArgumentResolver;
    private final AuthModelInterceptor authModelInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(authModelInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/**", "/css/**", "/js/**", "/images/**", "/favicon.ico");
                
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }
}