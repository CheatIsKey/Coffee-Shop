package jpa.basic.coffeeshop.common.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        // API 요청 에러는 기본 JSON/에러 처리 로직을 따르도록 둠
        if (requestUri != null && requestUri.startsWith("/api")) {
            return "error"; 
        }

        // 존재하지 않는 페이지(404)나 권한 없음(403)으로 관리자 페이지 접근 시
        // 화이트라벨 에러가 뜨지 않도록 프론트엔드(SPA) 메인 페이지로 포워딩
        return "forward:/index.html";
    }
}
