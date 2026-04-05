package com.dirtypay.global.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 세션 접근 권한 검증 어노테이션.
 *
 * <p>Controller 메서드에 이 어노테이션을 적용하면, AOP Aspect가
 * SecurityContext에서 인증된 사용자를 추출하고 지정된 리소스의
 * Session 접근 권한을 검증한다.</p>
 *
 * <p>{@link AccessLevel#OWNER}는 세션 소유자만 허용하고,
 * {@link AccessLevel#MEMBER}는 세션 소유자 또는 세션에 참여 중인 멤버를 허용한다.</p>
 *
 * <p>접근 권한 검증 실패 시 {@code BusinessException(ErrorCode.SESSION_ACCESS_DENIED)}를 던진다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SessionAccess {

    /**
     * 리소스 ID를 추출할 메서드 파라미터 이름.
     *
     * @return 파라미터 이름 (기본값: "sessionId")
     */
    String value() default "sessionId";

    /**
     * 리소스 타입. 리소스에서 Session까지의 체인 조회 방식을 결정한다.
     *
     * @return 리소스 타입 (기본값: SESSION)
     */
    ResourceType type() default ResourceType.SESSION;

    /**
     * 접근 레벨. 소유자 전용 또는 멤버 허용을 결정한다.
     *
     * @return 접근 레벨 (기본값: OWNER)
     */
    AccessLevel level() default AccessLevel.OWNER;

    /**
     * 리소스 타입 열거형.
     *
     * <p>각 타입에 따라 Session까지의 체인 조회 경로가 달라진다.</p>
     */
    enum ResourceType {
        /** sessionId로 직접 검증 */
        SESSION,
        /** nodeId → Node.sessionId → Session */
        NODE,
        /** memberId → OrgMember.nodeId → Node.sessionId → Session */
        MEMBER,
        /** roundId → Round.sessionId → Session */
        ROUND,
        /** orderId → Order.roundId → Round.sessionId → Session */
        ORDER,
        /** groupId → RoundGroup.roundId → Round.sessionId → Session */
        GROUP
    }

    /**
     * 접근 레벨 열거형.
     */
    enum AccessLevel {
        /** 세션 소유자만 접근 가능 */
        OWNER,
        /** 세션 소유자 또는 세션에 참여 중인 멤버가 접근 가능 */
        MEMBER
    }
}
