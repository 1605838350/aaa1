package com.ruoyi.framework.security.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.common.utils.StringUtils;

/**
 * URL 参数 Token 过滤器
 * 从请求参数中提取 token 并注入到 Authorization header，
 * 供 @vue-office 等无法自定义 header 的组件通过 URL 传 token 认证
 */
@Component
@Order(Integer.MIN_VALUE)
public class QueryParamTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = request.getParameter("token");
        if (StringUtils.isNotEmpty(token) && StringUtils.isEmpty(request.getHeader("Authorization"))) {
            // 只有 header 里没有 token 时才从 query param 注入
            final String authHeader = "Bearer " + token;
            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return authHeader;
                    }
                    return super.getHeader(name);
                }
            };
        }

        chain.doFilter(request, response);
    }
}
