package com.dirtypay.global.security;

import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.security.annotation.SessionAccess;
import com.dirtypay.global.security.annotation.SessionAccess.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.dirtypay.domain.auth.security.UserPrincipal;

/**
 * 세션 접근 권한 검증 AOP Aspect.
 *
 * <p>{@link SessionAccess} 어노테이션이 적용된 Controller 메서드 실행 전에
 * SecurityContext에서 인증된 사용자를 추출하고,
 * 지정된 리소스의 Session 접근 권한을 검증한다.</p>
 *
 * <p>{@link AccessLevel#OWNER}는 세션 소유자만 허용하고,
 * {@link AccessLevel#MEMBER}는 세션 소유자 또는 세션에 참여 중인 멤버를 허용한다.</p>
 *
 * <p>리소스 타입에 따른 엔티티 체인 조회는 {@link SessionAccessResolver}에 위임하며,
 * 이 Aspect는 인증/인가 판단만 담당한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Aspect
@Component
@RequiredArgsConstructor
public class SessionAccessAspect {

    private final SessionAccessResolver sessionAccessResolver;
    private final OrgMemberRepository orgMemberRepository;

    /**
     * {@link SessionAccess} 어노테이션이 적용된 메서드 실행 전에 접근 권한을 검증한다.
     *
     * @param joinPoint     조인 포인트
     * @param sessionAccess 어노테이션 정보
     */
    @Before("@annotation(sessionAccess)")
    public void verifySessionAccess(JoinPoint joinPoint, SessionAccess sessionAccess) {
        Long userId = this.extractUserId();
        Long resourceId = this.extractResourceId(joinPoint, sessionAccess.value());
        Session session = this.sessionAccessResolver.resolve(resourceId, sessionAccess.type());

        if (session.getOwnerId().equals(userId)) {
            return;
        }

        if (sessionAccess.level() == AccessLevel.MEMBER) {
            boolean isMember = this.orgMemberRepository
                    .findBySessionIdAndUserId(session.getId(), userId)
                    .isPresent();
            if (isMember) {
                return;
            }
        }

        throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
    }

    private Long extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userPrincipal.getId();
    }

    private Long extractResourceId(JoinPoint joinPoint, String parameterName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(parameterName)) {
                return (Long) args[i];
            }
        }

        throw new IllegalArgumentException(
                String.format("@SessionAccess에 지정된 파라미터 '%s'를 찾을 수 없습니다.", parameterName));
    }
}
