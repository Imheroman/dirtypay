package com.dirtypay.domain.auth.security;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService 구현체.
 *
 * <p>Spring Security 인증 과정에서 사용자 정보를 로드하는 서비스이다.
 * 이메일을 기준으로 회원을 조회하여 UserPrincipal로 변환한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * 이메일로 사용자 정보를 로드한다.
     *
     * @param email 사용자 이메일
     * @return UserDetails 구현체 (UserPrincipal)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = this.memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        return UserPrincipal.from(member);
    }
}
