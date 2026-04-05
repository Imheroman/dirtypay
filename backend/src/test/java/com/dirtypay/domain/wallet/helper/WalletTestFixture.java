package com.dirtypay.domain.wallet.helper;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.domain.wallet.repository.WalletRepository;
import com.dirtypay.domain.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

/**
 * 통합 테스트 및 동시성 테스트에서 Member + Wallet 테스트 데이터를 생성·정리하는 공유 픽스처 헬퍼.
 *
 * <p>각 테스트 클래스에서 {@code @Import(WalletTestFixture.class)}로 등록하여 사용한다.
 * FK 제약 순서를 고려하여 {@link #cleanupAll()}에서 WalletTransaction → Wallet → Member 순으로 삭제한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class WalletTestFixture {

    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * 테스트용 Member를 생성하고 저장한다.
     *
     * <p>email은 members 테이블에서 고유해야 하므로 호출 측에서 반드시 고유한 값을 전달해야 한다.</p>
     *
     * @param email 고유한 이메일 주소
     * @param name  회원 이름
     * @return 저장된 {@link Member} 엔티티
     */
    public Member createMember(String email, String name) {
        Member member = Member.builder()
                .email(email)
                .password("encoded")
                .name(name)
                .profileImage(null)
                .role(MemberRole.USER)
                .build();
        return memberRepository.save(member);
    }

    /**
     * 테스트용 Wallet을 생성하고 지정된 초기 잔액을 설정한 뒤 저장한다.
     *
     * <p>{@link Wallet#Wallet(Long)} 빌더로 생성하면 잔액이 {@link BigDecimal#ZERO}로 초기화되므로,
     * {@link ReflectionTestUtils#setField}로 {@code balance} 필드를 직접 주입한 뒤 재저장한다.</p>
     *
     * @param memberId       지갑 소유 회원 ID
     * @param initialBalance 초기 설정할 잔액 (0 이상)
     * @return 잔액이 설정된 후 저장된 {@link Wallet} 엔티티
     */
    public Wallet createWallet(Long memberId, BigDecimal initialBalance) {
        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .build();
        walletRepository.save(wallet);

        ReflectionTestUtils.setField(wallet, "balance", initialBalance);
        return walletRepository.save(wallet);
    }

    /**
     * WalletTransaction → Wallet → Member 순으로 모든 데이터를 삭제한다.
     *
     * <p>FK 참조 순서를 준수하여 자식 테이블부터 제거한 뒤 부모 테이블을 삭제한다.
     * 각 테스트의 {@code @AfterEach} 혹은 {@code @AfterAll}에서 호출하여 테스트 격리를 보장한다.</p>
     */
    public void cleanupAll() {
        walletTransactionRepository.deleteAll();
        walletRepository.deleteAll();
        memberRepository.deleteAll();
    }
}
