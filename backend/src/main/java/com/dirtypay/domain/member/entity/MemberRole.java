package com.dirtypay.domain.member.entity;

/**
 * 회원 권한 열거형.
 *
 * <p>Spring Security의 권한 체계와 연동되어 사용된다.
 * 각 권한은 "ROLE_" 접두사가 붙은 authority 값을 가진다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public enum MemberRole {

    /** 관리자 권한 */
    ADMIN("ROLE_ADMIN"),

    /** 일반 사용자 권한 */
    USER("ROLE_USER");

    private final String authority;

    MemberRole(String authority) {
        this.authority = authority;
    }

    /**
     * Spring Security에서 사용하는 권한 문자열을 반환한다.
     *
     * @return "ROLE_" 접두사가 붙은 권한 문자열
     */
    public String getAuthority() {
        return this.authority;
    }
}
