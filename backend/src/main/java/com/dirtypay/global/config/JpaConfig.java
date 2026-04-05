package com.dirtypay.global.config;

import com.dirtypay.domain.auth.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing 설정.
 *
 * <p>엔티티의 {@code @CreatedDate}, {@code @LastModifiedDate} 자동 업데이트를 활성화하고,
 * {@code @CreatedBy}, {@code @LastModifiedBy}에 현재 인증 사용자 ID를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * 현재 인증된 사용자의 ID를 반환하는 AuditorAware.
     *
     * <p>SecurityContext에서 인증 정보를 가져와 사용자 ID(Long)를 문자열로 반환한다.
     * 인증되지 않은 요청(스케줄러, 시스템 작업 등)에서는 "system"을 반환한다.</p>
     *
     * @return AuditorAware 구현체
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                return Optional.of("system");
            }

            return Optional.of(String.valueOf(principal.getId()));
        };
    }
}
