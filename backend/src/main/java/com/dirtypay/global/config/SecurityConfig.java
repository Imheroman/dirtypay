package com.dirtypay.global.config;

import com.dirtypay.domain.auth.security.jwt.JwtAuthenticationEntryPoint;
import com.dirtypay.domain.auth.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정.
 *
 * <p>JWT 기반 인증을 사용하며, 세션을 사용하지 않는 Stateless 방식으로 동작한다.</p>
 *
 * <p>Actuator 접근 제어 정책:</p>
 * <ul>
 *   <li>{@code /actuator/health}, {@code /actuator/info} — 비인증 접근 허용 (헬스 체크 용도)</li>
 *   <li>그 외 {@code /actuator/**} — 인증 필요 (민감 메트릭 보호)</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsProperties corsProperties;

    /**
     * 인증 없이 접근 가능한 엔드포인트.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/h2-console/**"
    };

    /**
     * 인증 없이 접근 가능한 Actuator 엔드포인트.
     *
     * <p>헬스 체크 및 기본 정보 조회만 비인증 접근을 허용한다.
     * 그 외 {@code /actuator/**}는 인증 후 접근 가능하다.</p>
     */
    private static final String[] PUBLIC_ACTUATOR_ENDPOINTS = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info"
    };

    /**
     * 인증 없이 접근 가능한 Auth API.
     * 로그아웃은 인증이 필요하므로 제외.
     */
    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/validate"
    };

    /**
     * Security Filter Chain 설정.
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 설정 오류
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(this.corsConfigurationSource()))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(PUBLIC_ACTUATOR_ENDPOINTS).permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .requestMatchers(PUBLIC_AUTH_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stores/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(this.jwtAuthenticationEntryPoint))
                .addFilterBefore(this.jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * CORS 설정 소스 빈.
     *
     * <p>Spring Security 필터 체인에서 CORS 처리에 사용하는 {@link CorsConfigurationSource}를
     * 명시적인 빈으로 등록한다. {@code cors(cors -> {})} 방식은 {@code WebMvcConfigurer}가
     * 로드되지 않는 {@code WebEnvironment.NONE} 테스트 컨텍스트에서 빈을 찾지 못해
     * 애플리케이션 컨텍스트 로딩에 실패하는 문제를 일으키므로, 이 빈을 통해 명시적으로 주입한다.</p>
     *
     * @return CORS 설정 소스
     * @author kim-young-woong
     * @since 1.0.0
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(this.corsProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept",
                "Cache-Control"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * 비밀번호 인코더.
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
