package com.clb.interceptor;

import com.clb.constant.Common;
import com.clb.constant.Excep;
import com.clb.exception.BaseException;
import com.clb.util.JwtUtils;
import com.clb.util.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenInterceptor implements GlobalFilter, Ordered {

    private final StringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        // 不对/login和/register接口校验
        if (path.endsWith("/login") || path.endsWith("/register")) {
            log.debug("直接跳过...");
            return chain.filter(exchange);
        }

        log.debug("开始校验...");

        String token = exchange.getRequest().getHeaders().getFirst(Common.TOKEN);
        if (token == null || token.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String redisToken = redisTemplate.opsForValue().get(token);
            if (redisToken == null) {
                throw new BaseException(Excep.TOKEN_ALREADY_EXPIRED);
            }
            Claims claims = JwtUtils.parseJWT(token);
            // 保存用户信息
            ThreadLocalUtil.set(claims);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
