package com.dirtypay.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 웹 설정.
 *
 * <p>CORS 정책을 구성한다. 허용 오리진은 {@link CorsProperties}를 통해
 * application.yml에서 주입받으므로 프로필별로 외부 설정이 가능하다.</p>
 *
 * <p>OWASP 권고에 따라 {@code allowCredentials(true)} 사용 시
 * {@code allowedHeaders("*")} 와일드카드 대신 실제 사용하는 헤더를 명시적으로 열거한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    /**
     * CORS 매핑을 등록한다.
     *
     * <p>/api/** 경로에 대해 {@link CorsProperties#getAllowedOrigins()}에서
     * 읽어온 오리진 목록을 허용한다.</p>
     *
     * <p>허용 헤더는 애플리케이션에서 실제로 사용하는 헤더만 명시한다:
     * <ul>
     *   <li>{@code Content-Type} — 요청 본문 미디어 타입</li>
     *   <li>{@code Authorization} — JWT Bearer 토큰 인증</li>
     *   <li>{@code X-Requested-With} — Ajax 요청 식별</li>
     *   <li>{@code Accept} — 응답 미디어 타입 협상</li>
     *   <li>{@code Cache-Control} — 캐시 제어</li>
     * </ul>
     * </p>
     *
     * @param registry CORS 레지스트리
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders(
                        "Content-Type",
                        "Authorization",
                        "X-Requested-With",
                        "Accept",
                        "Cache-Control"
                )
                .allowCredentials(true)
                .maxAge(3600);
    }
}
