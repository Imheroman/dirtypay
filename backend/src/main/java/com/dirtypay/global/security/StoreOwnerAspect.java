package com.dirtypay.global.security;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.security.annotation.StoreOwner;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 매장 소유자 권한 검증 AOP Aspect.
 *
 * <p>{@link StoreOwner} 어노테이션이 적용된 Controller 메서드 실행 전에
 * SecurityContext에서 인증된 사용자를 추출하고,
 * 지정된 리소스의 매장 소유권을 검증한다.</p>
 *
 * <p>리소스 타입에 따른 엔티티 체인 조회는 {@link StoreOwnerResolver}에 위임하며,
 * 이 Aspect는 인증/인가 판단만 담당한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Aspect
@Component
@RequiredArgsConstructor
public class StoreOwnerAspect {

    private final StoreOwnerResolver storeOwnerResolver;

    /**
     * {@link StoreOwner} 어노테이션이 적용된 메서드 실행 전에 매장 소유권을 검증한다.
     *
     * <p>현재 인증된 사용자의 ID와 리소스에서 조회한 매장 소유자 ID를 비교한다.
     * 일치하지 않으면 {@link BusinessException}({@link ErrorCode#STORE_ACCESS_DENIED})을 던진다.</p>
     *
     * @param joinPoint  조인 포인트
     * @param storeOwner 어노테이션 정보
     * @throws BusinessException 인증 정보가 없거나 소유권 검증 실패 시
     */
    @Before("@annotation(storeOwner)")
    public void verifyStoreOwner(JoinPoint joinPoint, StoreOwner storeOwner) {
        Long userId = this.extractUserId();
        Long resourceId = this.extractResourceId(joinPoint, storeOwner.value());
        Long ownerId = this.storeOwnerResolver.resolveOwnerId(storeOwner.resourceType(), resourceId);

        if (!ownerId.equals(userId)) {
            throw new BusinessException(ErrorCode.STORE_ACCESS_DENIED);
        }
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
                String.format("@StoreOwner에 지정된 파라미터 '%s'를 찾을 수 없습니다.", parameterName));
    }
}
