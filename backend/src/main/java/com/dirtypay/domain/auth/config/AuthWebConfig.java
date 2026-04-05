package com.dirtypay.domain.auth.config;

import com.dirtypay.domain.auth.security.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auth 도메인 웹 설정.
 *
 * <p>인증 엔드포인트에 Rate Limiting 인터셉터를 등록한다.</p>
 *
 * <p>전역 CORS 설정은 {@code global.config.WebConfig}에서 담당하며,
 * 이 설정 클래스는 인증 도메인 전용 인터셉터 등록에만 집중한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class AuthWebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Rate Limiting 인터셉터를 인증 API 경로에 등록한다.
     *
     * @param registry 인터셉터 레지스트리
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.rateLimitInterceptor)
                .addPathPatterns("/api/auth/**");
    }
}
