package com.dirtypay.domain.auth.security;

import com.dirtypay.domain.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 인증 주체.
 *
 * <p>Spring Security의 UserDetails 인터페이스를 구현하여
 * 인증된 사용자 정보를 담는다. Member 엔티티로부터 생성된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Long id, String email, String password, String name,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.authorities = authorities;
    }

    /**
     * JWT Claims로부터 UserPrincipal을 생성한다.
     *
     * <p>DB 조회 없이 토큰의 Claims만으로 인증 주체를 생성한다.</p>
     *
     * @param memberId 회원 ID
     * @param email    회원 이메일
     * @param role     회원 권한 (예: "ROLE_USER", "ROLE_ADMIN"). null이면 ROLE_USER로 폴백
     * @return UserPrincipal 인스턴스
     */
    public static UserPrincipal fromClaims(Long memberId, String email, String role) {
        String authority = role != null ? role : "ROLE_USER";
        return new UserPrincipal(
                memberId, email, null, null,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }

    /**
     * Member 엔티티로부터 UserPrincipal을 생성한다.
     *
     * @param member 회원 엔티티
     * @return UserPrincipal 인스턴스
     */
    public static UserPrincipal from(Member member) {
        return new UserPrincipal(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                member.getName(),
                Collections.singletonList(new SimpleGrantedAuthority(member.getRole().getAuthority()))
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
