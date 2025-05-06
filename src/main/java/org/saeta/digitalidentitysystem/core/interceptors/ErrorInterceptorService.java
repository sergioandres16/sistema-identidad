package org.saeta.digitalidentitysystem.core.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class ErrorInterceptorService implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (request.getRequestURI().contains("//")) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid URL format");
            return false;
        }
        return true;
    }
}
