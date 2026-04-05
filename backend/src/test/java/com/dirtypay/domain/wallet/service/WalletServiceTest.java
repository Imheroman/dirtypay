package com.dirtypay.domain.wallet.service;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.domain.wallet.dto.request.WalletChargeRequest;
import com.dirtypay.domain.wallet.dto.request.WalletTransferRequest;
import com.dirtypay.domain.wallet.dto.response.WalletResponse;
import com.dirtypay.domain.wallet.dto.response.WalletTransactionResponse;
import com.dirtypay.domain.wallet.entity.TransactionType;
import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.domain.wallet.entity.WalletTransaction;
import com.dirtypay.domain.wallet.repository.WalletRepository;
import com.dirtypay.domain.wallet.repository.WalletTransactionRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * WalletService 단위 테스트.
 *
 * <p>지갑 생성, 조회, 충전, 송금의 성공/실패 시나리오를 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService 단위 테스트")
class WalletServiceTest {

    @InjectMocks
    private WalletService walletService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private MemberRepository memberRepository;

    // =========================================================
    // 지갑 생성 (createWallet)
    // =========================================================

    @Nested
    @DisplayName("지갑 생성 (createWallet)")
    class CreateWallet {

        @Test
        @DisplayName("성공: memberId로 지갑 생성 시 balance=0, status=ACTIVE인 WalletResponse를 반환한다")
        void createWallet_success() {
            // given
            Long memberId = 1L;
            given(walletRepository.existsByMemberId(memberId)).willReturn(false);

            Wallet savedWallet = createWallet(1L, memberId, BigDecimal.ZERO);
            given(walletRepository.save(any(Wallet.class))).willReturn(savedWallet);

            // when
            WalletResponse response = walletService.createWallet(memberId);

            // then
            assertThat(response.getMemberId()).isEqualTo(memberId);
            assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getStatus().name()).isEqualTo("ACTIVE");
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("실패: 이미 지갑이 존재하는 경우 WALLET_ALREADY_EXISTS 예외가 발생한다")
        void createWallet_alreadyExists_throwsBusinessException() {
            // given
            Long memberId = 1L;
            given(walletRepository.existsByMemberId(memberId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> walletService.createWallet(memberId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.WALLET_ALREADY_EXISTS.getMessage());

            verify(walletRepository, never()).save(any(Wallet.class));
        }
    }

    // =========================================================
    // 지갑 조회 (getWallet)
    // =========================================================

    @Nested
    @DisplayName("지갑 조회 (getWallet)")
    class GetWallet {

        @Test
        @DisplayName("성공: memberId로 조회 시 WalletResponse를 반환한다")
        void getWallet_success() {
            // given
            Long memberId = 1L;
            Wallet wallet = createWallet(1L, memberId, new BigDecimal("50000"));
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.of(wallet));

            // when
            WalletResponse response = walletService.getWallet(memberId);

            // then
            assertThat(response.getMemberId()).isEqualTo(memberId);
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 지갑 조회 시 WALLET_NOT_FOUND 예외가 발생한다")
        void getWallet_notFound_throwsEntityNotFoundException() {
            // given
            Long memberId = 999L;
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walletService.getWallet(memberId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ErrorCode.WALLET_NOT_FOUND.getMessage());
        }
    }

    // =========================================================
    // 충전 (charge)
    // =========================================================

    @Nested
    @DisplayName("충전 (charge)")
    class Charge {

        @Test
        @DisplayName("성공: 100,000원 충전 시 잔액이 증가하고 거래 이력이 저장된다")
        void charge_success() {
            // given
            Long memberId = 1L;
            BigDecimal initialBalance = new BigDecimal("10000");
            BigDecimal chargeAmount = new BigDecimal("100000");

            Wallet wallet = createWallet(1L, memberId, initialBalance);
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.of(wallet));

            WalletChargeRequest request = new WalletChargeRequest();
            ReflectionTestUtils.setField(request, "amount", chargeAmount);

            // when
            WalletResponse response = walletService.charge(memberId, request);

            // then
            assertThat(response.getBalance()).isEqualByComparingTo(initialBalance.add(chargeAmount));
            verify(walletTransactionRepository).save(any(WalletTransaction.class));
        }

        @Test
        @DisplayName("실패: 일일 한도(3,000,000원) 초과 충전 시 WALLET_DAILY_LIMIT_EXCEEDED 예외가 발생한다")
        void charge_dailyLimitExceeded_throwsBusinessException() {
            // given
            Long memberId = 1L;
            // 이미 2,900,001원 충전된 지갑 생성
            // lastChargedDate를 오늘로 설정해야 resetDailyLimitIfNeeded()가 dailyChargedAmount를 초기화하지 않는다
            Wallet wallet = createWallet(1L, memberId, new BigDecimal("2900001"));
            ReflectionTestUtils.setField(wallet, "dailyChargedAmount", new BigDecimal("2900001"));
            ReflectionTestUtils.setField(wallet, "lastChargedDate", LocalDate.now());

            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.of(wallet));

            // 100,000원 추가 충전 시도 → 2,900,001 + 100,000 = 3,000,001 > 3,000,000
            WalletChargeRequest request = new WalletChargeRequest();
            ReflectionTestUtils.setField(request, "amount", new BigDecimal("100000"));

            // when & then
            assertThatThrownBy(() -> walletService.charge(memberId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.WALLET_DAILY_LIMIT_EXCEEDED.getMessage());

            verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
        }
    }

    // =========================================================
    // 송금 (transfer)
    // =========================================================

    @Nested
    @DisplayName("송금 (transfer)")
    class Transfer {

        @Test
        @DisplayName("성공: sender에서 receiver로 10,000원 송금 시 TRANSFER_OUT 응답을 반환하고 양쪽 잔액이 변경된다")
        void transfer_success() {
            // given
            Long senderMemberId = 1L;
            Long receiverMemberId = 2L;
            String receiverEmail = "receiver@example.com";
            BigDecimal transferAmount = new BigDecimal("10000");

            Member receiverMember = Member.builder().email(receiverEmail).password("pass").name("수신자").build();
            ReflectionTestUtils.setField(receiverMember, "id", receiverMemberId);

            Wallet sender = createWallet(1L, senderMemberId, new BigDecimal("50000"));
            Wallet receiver = createWallet(2L, receiverMemberId, new BigDecimal("20000"));

            given(walletRepository.findByMemberId(senderMemberId)).willReturn(Optional.of(sender));
            given(memberRepository.findByEmail(receiverEmail)).willReturn(Optional.of(receiverMember));
            given(walletRepository.findByMemberId(receiverMemberId)).willReturn(Optional.of(receiver));

            WalletTransferRequest request = new WalletTransferRequest();
            ReflectionTestUtils.setField(request, "receiverEmail", receiverEmail);
            ReflectionTestUtils.setField(request, "amount", transferAmount);
            ReflectionTestUtils.setField(request, "idempotencyKey", null);
            ReflectionTestUtils.setField(request, "description", "테스트 송금");

            given(walletTransactionRepository.save(any(WalletTransaction.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            WalletTransactionResponse response = walletService.transfer(senderMemberId, request);

            // then
            assertThat(response.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(response.getAmount()).isEqualByComparingTo(transferAmount);

            // 양쪽 잔액 변경 확인
            assertThat(sender.getBalance()).isEqualByComparingTo(new BigDecimal("40000"));
            assertThat(receiver.getBalance()).isEqualByComparingTo(new BigDecimal("30000"));
        }

        @Test
        @DisplayName("실패: 잔액 부족 시 WALLET_INSUFFICIENT_BALANCE 예외가 발생한다")
        void transfer_insufficientBalance_throwsBusinessException() {
            // given
            Long senderMemberId = 1L;
            Long receiverMemberId = 2L;
            String receiverEmail = "receiver@example.com";

            Member receiverMember = Member.builder().email(receiverEmail).password("pass").name("수신자").build();
            ReflectionTestUtils.setField(receiverMember, "id", receiverMemberId);

            // 잔액 5,000원인 지갑에서 10,000원 송금 시도
            Wallet sender = createWallet(1L, senderMemberId, new BigDecimal("5000"));
            Wallet receiver = createWallet(2L, receiverMemberId, new BigDecimal("20000"));

            given(walletRepository.findByMemberId(senderMemberId)).willReturn(Optional.of(sender));
            given(memberRepository.findByEmail(receiverEmail)).willReturn(Optional.of(receiverMember));
            given(walletRepository.findByMemberId(receiverMemberId)).willReturn(Optional.of(receiver));

            WalletTransferRequest request = new WalletTransferRequest();
            ReflectionTestUtils.setField(request, "receiverEmail", receiverEmail);
            ReflectionTestUtils.setField(request, "amount", new BigDecimal("10000"));
            ReflectionTestUtils.setField(request, "idempotencyKey", null);
            ReflectionTestUtils.setField(request, "description", null);

            // when & then
            assertThatThrownBy(() -> walletService.transfer(senderMemberId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.WALLET_INSUFFICIENT_BALANCE.getMessage());
        }

        @Test
        @DisplayName("실패: 같은 지갑으로 송금 시 WALLET_TRANSFER_SAME_WALLET 예외가 발생한다")
        void transfer_sameWallet_throwsBusinessException() {
            // given
            Long memberId = 1L;
            String sameEmail = "same@example.com";

            Member sameMember = Member.builder().email(sameEmail).password("pass").name("본인").build();
            ReflectionTestUtils.setField(sameMember, "id", memberId);

            // sender와 receiver가 같은 memberId → 같은 지갑 반환
            Wallet wallet = createWallet(1L, memberId, new BigDecimal("50000"));

            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.of(wallet));
            given(memberRepository.findByEmail(sameEmail)).willReturn(Optional.of(sameMember));

            WalletTransferRequest request = new WalletTransferRequest();
            ReflectionTestUtils.setField(request, "receiverEmail", sameEmail);
            ReflectionTestUtils.setField(request, "amount", new BigDecimal("10000"));
            ReflectionTestUtils.setField(request, "idempotencyKey", null);
            ReflectionTestUtils.setField(request, "description", null);

            // when & then
            assertThatThrownBy(() -> walletService.transfer(memberId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.WALLET_TRANSFER_SAME_WALLET.getMessage());
        }

        @Test
        @DisplayName("성공(멱등성): 이미 처리된 idempotencyKey 제공 시 기존 거래 이력을 반환한다")
        void transfer_idempotency_returnsExistingTransaction() {
            // given
            Long senderMemberId = 1L;
            Long receiverMemberId = 2L;
            String receiverEmail = "receiver@example.com";
            String idempotencyKey = "transfer:existing-key-001";

            Member receiverMember = Member.builder().email(receiverEmail).password("pass").name("수신자").build();
            ReflectionTestUtils.setField(receiverMember, "id", receiverMemberId);

            Wallet sender = createWallet(1L, senderMemberId, new BigDecimal("50000"));
            Wallet receiver = createWallet(2L, receiverMemberId, new BigDecimal("20000"));

            given(walletRepository.findByMemberId(senderMemberId)).willReturn(Optional.of(sender));
            given(memberRepository.findByEmail(receiverEmail)).willReturn(Optional.of(receiverMember));
            given(walletRepository.findByMemberId(receiverMemberId)).willReturn(Optional.of(receiver));

            // 이미 처리된 거래 이력 준비
            WalletTransaction existingTx = WalletTransaction.builder()
                    .walletId(1L)
                    .type(TransactionType.TRANSFER_OUT)
                    .amount(new BigDecimal("10000"))
                    .balanceBefore(new BigDecimal("50000"))
                    .balanceAfter(new BigDecimal("40000"))
                    .counterpartyWalletId(2L)
                    .idempotencyKey(idempotencyKey)
                    .status(com.dirtypay.domain.wallet.entity.TransactionStatus.COMPLETED)
                    .build();

            given(walletTransactionRepository.findByIdempotencyKey(idempotencyKey))
                    .willReturn(Optional.of(existingTx));

            WalletTransferRequest request = new WalletTransferRequest();
            ReflectionTestUtils.setField(request, "receiverEmail", receiverEmail);
            ReflectionTestUtils.setField(request, "amount", new BigDecimal("10000"));
            ReflectionTestUtils.setField(request, "idempotencyKey", idempotencyKey);
            ReflectionTestUtils.setField(request, "description", null);

            // when
            WalletTransactionResponse response = walletService.transfer(senderMemberId, request);

            // then
            // 기존 거래 반환 — 새 save 호출이 없어야 한다
            assertThat(response.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
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
