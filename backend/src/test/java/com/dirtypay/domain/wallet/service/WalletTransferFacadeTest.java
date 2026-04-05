package com.dirtypay.domain.wallet.service;

import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.domain.wallet.repository.WalletRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * WalletTransferFacade 단위 테스트.
 *
 * <p>분산 락 Facade의 지갑 조회 및 transferInternal 위임을 검증한다.
 * AOP 분산 락({@link com.dirtypay.global.lock.DistributedLock})은 단위 테스트에서 우회되므로
 * 비즈니스 로직(지갑 조회, transferInternal 위임)만 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletTransferFacade 단위 테스트")
class WalletTransferFacadeTest {

    @InjectMocks
    private WalletTransferFacade walletTransferFacade;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletService walletService;

    @Nested
    @DisplayName("transferWithLock")
    class TransferWithLock {

        @Test
        @DisplayName("성공: 지갑 조회 후 transferInternal이 올바른 파라미터로 호출되고 지갑 ID가 반환된다")
        void transferWithLock_success() {
            // given
            Long senderId = 1L;
            Long receiverId = 2L;
            Wallet senderWallet = createWallet(100L, senderId, new BigDecimal("100000"));
            Wallet receiverWallet = createWallet(200L, receiverId, new BigDecimal("0"));
            BigDecimal amount = new BigDecimal("50000");

            given(walletRepository.findByMemberId(senderId)).willReturn(Optional.of(senderWallet));
            given(walletRepository.findByMemberId(receiverId)).willReturn(Optional.of(receiverWallet));

            // when
            WalletTransferResult result = walletTransferFacade.transferWithLock(senderId, receiverId, amount,
                    "settle:1:10", "SETTLEMENT", 1L, "정산 송금");

            // then: transferInternal이 올바른 파라미터로 호출되었는지 검증
            verify(walletService).transferInternal(
                    eq(senderWallet), eq(receiverWallet),
                    eq(amount), eq("settle:1:10"),
                    eq("SETTLEMENT"), eq(1L), eq("정산 송금"));

            // then: 지갑 ID가 결과에 포함되어 있는지 검증
            assertThat(result.senderWalletId()).isEqualTo(100L);
            assertThat(result.receiverWalletId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("실패: 송금자 지갑이 없으면 WALLET_NOT_FOUND 예외가 발생한다")
        void transferWithLock_senderNotFound_throwsEntityNotFoundException() {
            // given
            Long senderId = 1L;
            Long receiverId = 2L;

            given(walletRepository.findByMemberId(senderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    walletTransferFacade.transferWithLock(senderId, receiverId, BigDecimal.TEN,
                            "key", "TYPE", 1L, "desc"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ErrorCode.WALLET_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 수신자 지갑이 없으면 WALLET_NOT_FOUND 예외가 발생한다")
        void transferWithLock_receiverNotFound_throwsEntityNotFoundException() {
            // given
            Long senderId = 1L;
            Long receiverId = 2L;
            Wallet senderWallet = createWallet(100L, senderId, new BigDecimal("100000"));

            given(walletRepository.findByMemberId(senderId)).willReturn(Optional.of(senderWallet));
            given(walletRepository.findByMemberId(receiverId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    walletTransferFacade.transferWithLock(senderId, receiverId, BigDecimal.TEN,
                            "key", "TYPE", 1L, "desc"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ErrorCode.WALLET_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("cancelTransferWithLock")
    class CancelTransferWithLock {

        @Test
        @DisplayName("성공: 환불 방향(오너→페이어)으로 transferInternal이 호출된다")
        void cancelTransferWithLock_success() {
            // given
            Long ownerMemberId = 2L;
            Long payerMemberId = 3L;
            Wallet ownerWallet = createWallet(200L, ownerMemberId, new BigDecimal("50000"));
            Wallet payerWallet = createWallet(100L, payerMemberId, new BigDecimal("50000"));
            BigDecimal amount = new BigDecimal("50000");

            given(walletRepository.findByMemberId(ownerMemberId)).willReturn(Optional.of(ownerWallet));
            given(walletRepository.findByMemberId(payerMemberId)).willReturn(Optional.of(payerWallet));

            // when
            walletTransferFacade.cancelTransferWithLock(ownerMemberId, payerMemberId, amount,
                    "refund:settle:1:10", "SETTLEMENT_REFUND", 1L, "정산 송금 취소");

            // then: 오너(refundSender) → 페이어(refundReceiver) 방향
            verify(walletService).transferInternal(
                    eq(ownerWallet), eq(payerWallet),
                    eq(amount), eq("refund:settle:1:10"),
                    eq("SETTLEMENT_REFUND"), eq(1L), eq("정산 송금 취소"));
        }
    }

    // =========================================================
    // 헬퍼 메서드
    // =========================================================

    /**
     * 테스트용 Wallet 엔티티를 생성한다.
     *
     * @param id       지갑 ID (ReflectionTestUtils로 주입)
     * @param memberId 소유 회원 ID
     * @param balance  초기 잔액
     * @return 생성된 Wallet 인스턴스
     */
    private Wallet createWallet(Long id, Long memberId, BigDecimal balance) {
        Wallet wallet = Wallet.builder().memberId(memberId).build();
        ReflectionTestUtils.setField(wallet, "id", id);
        ReflectionTestUtils.setField(wallet, "balance", balance);
        return wallet;
    }
}
