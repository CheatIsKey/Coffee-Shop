package jpa.basic.coffeeshop.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jpa.basic.coffeeshop.common.security.auth.LoginUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            response.sendRedirect("/login");
            return false;
        }
        
        LoginUserInfo userInfo = (LoginUserInfo) authentication.getPrincipal();
        
        if (!"ADMIN".equals(userInfo.role())) {
            if (request.getRequestURI().startsWith("/api/")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                return false;
            }
            response.sendRedirect("/");
            return false;
        }
        
        return true;
    }
}