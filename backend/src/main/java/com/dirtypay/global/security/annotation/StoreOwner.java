package com.dirtypay.global.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 매장 소유자 권한 검증 어노테이션.
 *
 * <p>Controller 메서드에 이 어노테이션을 적용하면, AOP Aspect가
 * SecurityContext에서 인증된 사용자를 추출하고 지정된 리소스의
 * 매장 소유권을 검증한다.</p>
 *
 * <p>소유권 검증 실패 시 {@code BusinessException(ErrorCode.STORE_ACCESS_DENIED)}를 던진다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * @PutMapping("/{storeId}")
 * @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
 * public ResponseEntity<?> updateStore(@PathVariable Long storeId, ...) { ... }
 * }</pre>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StoreOwner {

    /**
     * 리소스 ID를 추출할 메서드 파라미터 이름.
     *
     * <p>{@link ResourceType}이 STORE이면 "storeId", STORE_MENU이면 "menuId",
     * STORE_ORDER이면 "orderId"를 기본값으로 사용한다.</p>
     *
     * @return 파라미터 이름 (기본값: "storeId")
     */
    String value() default "storeId";

    /**
     * 검증할 리소스 타입.
     *
     * <p>STORE, STORE_MENU, STORE_ORDER 타입에 따라 소유자 체인 조회 방식이 달라진다.</p>
     *
     * @return 리소스 타입 (기본값: STORE)
     */
    ResourceType resourceType() default ResourceType.STORE;

    /**
     * 리소스 타입 열거형.
     *
     * <p>각 타입에 따라 Store까지의 체인 조회 경로가 달라진다.</p>
     */
    enum ResourceType {
        /** storeId로 직접 매장 소유자 검증. */
        STORE,
        /** menuId → StoreMenu.storeId → Store 소유자 검증. */
        STORE_MENU,
        /** orderId → StoreOrder.storeId → Store 소유자 검증. */
        STORE_ORDER
    }
}
