package com.dirtypay.domain.wallet.service;

import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.domain.settlement.dto.response.MemberSettlementResponse;
import com.dirtypay.domain.settlement.service.SettlementService;
import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
import com.dirtypay.domain.wallet.dto.response.SettlementTransferResponse;
import com.dirtypay.domain.wallet.entity.SettlementTransfer;
import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.domain.wallet.repository.SettlementTransferRepository;
import com.dirtypay.domain.wallet.repository.WalletRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

/**
 * SettlementTransferService 단위 테스트.
 *
 * <p>정산 송금 생성 및 취소의 성공/실패 시나리오를 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementTransferService 단위 테스트")
class SettlementTransferServiceTest {

    @InjectMocks
    private SettlementTransferService settlementTransferService;

    @Mock
    private WalletService walletService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransferFacade walletTransferFacade;

    @Mock
    private SettlementTransferRepository settlementTransferRepository;

    @Mock
    private SettlementService settlementService;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private SessionRepository sessionRepository;

    // =========================================================
    // 정산 송금 생성 (createSettlementTransfer)
    // =========================================================

    @Nested
    @DisplayName("정산 송금 생성 (createSettlementTransfer)")
    class CreateSettlementTransfer {

        @Test
        @DisplayName("성공: 정산 금액 계산 후 지갑 송금 및 SettlementTransfer 저장, 납부 상태 업데이트가 수행된다")
        void createSettlementTransfer_success() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 10L;
            Long ownerId = 2L;       // 세션 소유자(총무) userId
            Long senderUserId = 3L;  // 조직 멤버의 회원 userId

            Session session = createSession(sessionId, ownerId);
            OrgMember orgMember = createOrgMember(orgMemberId, senderUserId);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(orgMemberRepository.findById(orgMemberId)).willReturn(Optional.of(orgMember));
            given(settlementTransferRepository.existsBySessionIdAndOrgMemberId(sessionId, orgMemberId))
                    .willReturn(false);

            MemberSettlementResponse settlementResponse = MemberSettlementResponse.builder()
                    .orgMemberId(orgMemberId)
                    .totalAmount(new BigDecimal("50000"))
                    .build();
            given(settlementService.calculateMemberSettlement(
                    eq(sessionId), eq(orgMemberId), any(RemainderStrategyType.class)))
                    .willReturn(settlementResponse);

            // 지갑 조회는 facade 내부에서 수행 — facade가 WalletTransferResult를 반환
            given(walletTransferFacade.transferWithLock(
                    eq(senderUserId), eq(ownerId), any(BigDecimal.class),
                    any(String.class), any(String.class), any(Long.class), any(String.class)))
                    .willReturn(new WalletTransferResult(100L, 200L));

            // SettlementTransfer saveAndFlush 시 저장된 엔티티 그대로 반환
            given(settlementTransferRepository.saveAndFlush(any(SettlementTransfer.class)))
                    .willAnswer(invocation -> {
                        SettlementTransfer saved = invocation.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", 999L);
                        return saved;
                    });

            // when
            SettlementTransferResponse response = settlementTransferService.createSettlementTransfer(
                    sessionId, orgMemberId, RemainderStrategyType.OWNER);

            // then
            assertThat(response.getSessionId()).isEqualTo(sessionId);
            assertThat(response.getOrgMemberId()).isEqualTo(orgMemberId);
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(response.getStatus().name()).isEqualTo("COMPLETED");

            verify(walletTransferFacade).transferWithLock(
                    eq(senderUserId), eq(ownerId), any(BigDecimal.class),
                    any(String.class), any(String.class), any(Long.class), any(String.class));
            verify(settlementTransferRepository).saveAndFlush(any(SettlementTransfer.class));
            verify(settlementService).updateSettlementPayment(
                    eq(sessionId), eq(orgMemberId),
                    any(BigDecimal.class), any(RemainderStrategyType.class));
        }

        @Test
        @DisplayName("실패: 이미 정산 송금이 처리된 경우 WALLET_DUPLICATE_TRANSACTION 예외가 발생한다")
        void createSettlementTransfer_duplicate_throwsBusinessException() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 10L;
            Long ownerId = 2L;
            Long senderUserId = 3L;

            Session session = createSession(sessionId, ownerId);
            OrgMember orgMember = createOrgMember(orgMemberId, senderUserId);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(orgMemberRepository.findById(orgMemberId)).willReturn(Optional.of(orgMember));
            // 이미 처리된 송금 존재
            given(settlementTransferRepository.existsBySessionIdAndOrgMemberId(sessionId, orgMemberId))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> settlementTransferService.createSettlementTransfer(
                    sessionId, orgMemberId, RemainderStrategyType.OWNER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.WALLET_DUPLICATE_TRANSACTION.getMessage());
        }

        @Test
        @DisplayName("실패: 동시 송금 요청으로 DB UNIQUE 위반 시 WALLET_DUPLICATE_TRANSACTION 예외가 발생한다")
        void createSettlementTransfer_concurrentDuplicate_throwsBusinessException() {
            // given - check-then-act 이후 동시 스레드가 먼저 삽입하여 DB UNIQUE 위반 시나리오
            Long sessionId = 1L;
            Long orgMemberId = 10L;
            Long ownerId = 2L;
            Long senderUserId = 3L;

            Session session = createSession(sessionId, ownerId);
            OrgMember orgMember = createOrgMember(orgMemberId, senderUserId);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(orgMemberRepository.findById(orgMemberId)).willReturn(Optional.of(orgMember));
            // existsBy 시점에는 중복 없음 (check-then-act Race Condition 상황)
            given(settlementTransferRepository.existsBySessionIdAndOrgMemberId(sessionId, orgMemberId))
                    .willReturn(false);

            MemberSettlementResponse settlementResponse = MemberSettlementResponse.builder()
                    .orgMemberId(orgMemberId)
                    .totalAmount(new BigDecimal("50000"))
                    .build();
            given(settlementService.calculateMemberSettlement(
                    eq(sessionId), eq(orgMemberId), any(RemainderStrategyType.class)))
                    .willReturn(settlementResponse);

            // 지갑 조회는 facade 내부에서 수행 — facade가 WalletTransferResult를 반환
            given(walletTransferFacade.transferWithLock(
                    eq(senderUserId), eq(ownerId), any(BigDecimal.class),
                    any(String.class), any(String.class), any(Long.class), any(String.class)))
                    .willReturn(new WalletTransferResult(100L, 200L));

            // saveAndFlush 시 DB UNIQUE 인덱스(active_transfer_key) 위반 발생
            given(settlementTransferRepository.saveAndFlush(any(SettlementTransfer.class)))
                    .willThrow(new DataIntegrityViolationException(
                            "Duplicate entry for key 'uk_settlement_transfer_active'"));

            // when & then
            assertThatThrownBy(() -> settlementTransferService.createSettlementTransfer(
                    sessionId, orgMemberId, RemainderStrategyType.OWNER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.WALLET_DUPLICATE_TRANSACTION.getMessage());
        }

        @Test
        @DisplayName("실패: 세션이 존재하지 않는 경우 SESSION_NOT_FOUND 예외가 발생한다")
        void createSettlementTransfer_sessionNotFound_throwsEntityNotFoundException() {
            // given
            Long sessionId = 99L;
            Long orgMemberId = 10L;

            given(sessionRepository.findById(sessionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> settlementTransferService.createSettlementTransfer(
                    sessionId, orgMemberId, RemainderStrategyType.OWNER))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ErrorCode.SESSION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 조직 멤버가 존재하지 않는 경우 MEMBER_NOT_FOUND 예외가 발생한다")
        void createSettlementTransfer_orgMemberNotFound_throwsEntityNotFoundException() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 99L;
            Long ownerId = 2L;

            Session session = createSession(sessionId, ownerId);
            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(orgMemberRepository.findById(orgMemberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> settlementTransferService.createSettlementTransfer(
                    sessionId, orgMemberId, RemainderStrategyType.OWNER))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 비회원(userId=null)인 경우 MEMBER_NOT_FOUND 예외가 발생한다")
        void createSettlementTransfer_guestMember_throwsEntityNotFoundException() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 10L;
            Long ownerId = 2L;

            Session session = createSession(sessionId, ownerId);
            OrgMember guestMember = createOrgMember(orgMemberId, null); // userId=null → 비회원

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(orgMemberRepository.findById(orgMemberId)).willReturn(Optional.of(guestMember));

            // when & then
            assertThatThrownBy(() -> settlementTransferService.createSettlementTransfer(
                    sessionId, orgMemberId, RemainderStrategyType.OWNER))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 지갑이 존재하지 않는 경우 WALLET_NOT_FOUND 예외가 발생한다")
        void createSettlementTransfer_walletNotFound_throwsEntityNotFoundException() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 10L;
            Long ownerId = 2L;
            Long senderUserId = 3L;

            Session session = createSession(sessionId, ownerId);
            OrgMember orgMember = createOrgMember(orgMemberId, senderUserId);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(orgMemberRepository.findById(orgMemberId)).willReturn(Optional.of(orgMember));
            given(settlementTransferRepository.existsBySessionIdAndOrgMemberId(sessionId, orgMemberId))
                    .willReturn(false);

            MemberSettlementResponse settlementResponse = MemberSettlementResponse.builder()
                    .orgMemberId(orgMemberId)
                    .totalAmount(new BigDecimal("10000"))
                    .build();
            given(settlementService.calculateMemberSettlement(
                    eq(sessionId), eq(orgMemberId), any(RemainderStrategyType.class)))
                    .willReturn(settlementResponse);

            // 지갑 조회는 facade 내부에서 수행 — facade가 WALLET_NOT_FOUND 예외를 발생시킴
            given(walletTransferFacade.transferWithLock(
                    eq(senderUserId), eq(ownerId), any(BigDecimal.class),
                    any(String.class), any(String.class), any(Long.class), any(String.class)))
                    .willThrow(new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> settlementTransferService.createSettlementTransfer(
                    sessionId, orgMemberId, RemainderStrategyType.OWNER))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ErrorCode.WALLET_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("성공: senderId와 receiverId로 transferWithLock이 올바르게 호출된다")
        void createSettlementTransfer_senderIdSmallerThanReceiverId_lockAcquiredInOrder() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 10L;
            Long ownerId = 5L;       // receiverId
            Long senderUserId = 2L;  // senderId

            Session session = createSession(sessionId, ownerId);
            OrgMember orgMember = createOrgMember(orgMemberId, senderUserId);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(orgMemberRepository.findById(orgMemberId)).willReturn(Optional.of(orgMember));
            given(settlementTransferRepository.existsBySessionIdAndOrgMemberId(sessionId, orgMemberId))
                    .willReturn(false);

            MemberSettlementResponse settlementResponse = MemberSettlementResponse.builder()
                    .orgMemberId(orgMemberId)
                    .totalAmount(new BigDecimal("30000"))
                    .build();
            given(settlementService.calculateMemberSettlement(
                    eq(sessionId), eq(orgMemberId), any(RemainderStrategyType.class)))
                    .willReturn(settlementResponse);

            // 지갑 조회는 facade 내부에서 수행 — facade가 WalletTransferResult를 반환
            given(walletTransferFacade.transferWithLock(
                    eq(senderUserId), eq(ownerId), any(BigDecimal.class),
                    any(String.class), any(String.class), any(Long.class), any(String.class)))
                    .willReturn(new WalletTransferResult(100L, 200L));

            given(settlementTransferRepository.saveAndFlush(any(SettlementTransfer.class)))
                    .willAnswer(invocation -> {
                        SettlementTransfer saved = invocation.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", 888L);
                        return saved;
                    });

            // when
            SettlementTransferResponse response = settlementTransferService.createSettlementTransfer(
                    sessionId, orgMemberId, RemainderStrategyType.OWNER);

            // then
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("30000"));
            verify(walletTransferFacade).transferWithLock(
                    eq(senderUserId), eq(ownerId),
                    any(BigDecimal.class), any(String.class),
                    any(String.class), any(Long.class), any(String.class));
        }
    }

    // =========================================================
    // 정산 송금 취소 (cancelSettlementTransfer)
    // =========================================================

    @Nested
    @DisplayName("정산 송금 취소 (cancelSettlementTransfer)")
    class CancelSettlementTransfer {

        @Test
        @DisplayName("성공: PENDING 상태의 정산 송금 취소 시 환불 이체가 실행되고 CANCELLED 상태로 변경된다")
        void cancelSettlementTransfer_success() {
            // given
            Long transferId = 1L;
            Long memberId = 3L; // senderWallet의 memberId와 동일해야 소유권 검증 통과
            Long senderWalletId = 100L;
            Long receiverWalletId = 200L;

            // PENDING 상태의 SettlementTransfer 생성 (complete() 미호출)
            SettlementTransfer transfer = SettlementTransfer.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(new BigDecimal("50000"))
                    .build();
            ReflectionTestUtils.setField(transfer, "id", transferId);

            given(settlementTransferRepository.findById(transferId)).willReturn(Optional.of(transfer));

            Wallet senderWallet = createWallet(senderWalletId, memberId, new BigDecimal("50000"));
            Wallet receiverWallet = createWallet(receiverWalletId, 2L, new BigDecimal("50000"));

            // 소유권 검증: 요청자의 지갑이 senderWallet과 동일한지 확인
            given(walletService.getWalletEntity(memberId)).willReturn(senderWallet);

            // ownerWalletId = receiverWalletId=200 → ownerMemberId=2L
            // payerWalletId = senderWalletId=100 → payerMemberId=memberId=3L
            given(walletRepository.findById(receiverWalletId)).willReturn(Optional.of(receiverWallet));
            given(walletRepository.findById(senderWalletId)).willReturn(Optional.of(senderWallet));

            willDoNothing().given(walletTransferFacade).cancelTransferWithLock(
                    eq(2L),       // ownerMemberId = receiverWallet.getMemberId()
                    eq(memberId), // payerMemberId = senderWallet.getMemberId()
                    any(BigDecimal.class), any(String.class),
                    any(String.class), any(Long.class), any(String.class));

            // when
            SettlementTransferResponse response = settlementTransferService.cancelSettlementTransfer(
                    transferId, memberId);

            // then
            assertThat(response.getStatus().name()).isEqualTo("CANCELLED");
            verify(walletTransferFacade).cancelTransferWithLock(
                    eq(2L),       // ownerMemberId
                    eq(memberId), // payerMemberId
                    any(BigDecimal.class), any(String.class),
                    any(String.class), any(Long.class), any(String.class));
        }

        @Test
        @DisplayName("실패: COMPLETED 상태의 정산 송금 취소 시도 시 TRANSFER_NOT_CANCELLABLE 예외가 발생한다")
        void cancelSettlementTransfer_completedStatus_throwsBusinessException() {
            // given
            Long transferId = 1L;
            Long memberId = 3L;
            Long senderWalletId = 100L;

            // PENDING 상태로 생성 후 complete() 호출하여 COMPLETED 상태로 전환
            SettlementTransfer transfer = SettlementTransfer.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(200L)
                    .amount(new BigDecimal("50000"))
                    .build();
            ReflectionTestUtils.setField(transfer, "id", transferId);
            transfer.complete(); // PENDING → COMPLETED

            given(settlementTransferRepository.findById(transferId)).willReturn(Optional.of(transfer));

            // 소유권 검증용 지갑 mock
            Wallet senderWallet = createWallet(senderWalletId, memberId, new BigDecimal("50000"));
            given(walletService.getWalletEntity(memberId)).willReturn(senderWallet);

            // when & then
            assertThatThrownBy(() -> settlementTransferService.cancelSettlementTransfer(
                    transferId, memberId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.TRANSFER_NOT_CANCELLABLE.getMessage());
        }

        @Test
        @DisplayName("실패: 정산 송금이 존재하지 않는 경우 TRANSFER_NOT_FOUND 예외가 발생한다")
        void cancelSettlementTransfer_transferNotFound_throwsEntityNotFoundException() {
            // given
            Long transferId = 99L;
            Long memberId = 3L;

            given(settlementTransferRepository.findById(transferId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> settlementTransferService.cancelSettlementTransfer(
                    transferId, memberId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ErrorCode.TRANSFER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 요청자가 송금자 본인이 아닌 경우 TRANSFER_ACCESS_DENIED 예외가 발생한다")
        void cancelSettlementTransfer_notOwner_throwsBusinessException() {
            // given
            Long transferId = 1L;
            Long memberId = 99L; // 실제 송금자가 아닌 다른 회원
            Long senderWalletId = 100L;
            Long receiverWalletId = 200L;

            SettlementTransfer transfer = SettlementTransfer.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(new BigDecimal("50000"))
                    .build();
            ReflectionTestUtils.setField(transfer, "id", transferId);

            given(settlementTransferRepository.findById(transferId)).willReturn(Optional.of(transfer));

            // 요청자의 지갑 ID가 transfer.senderWalletId(100)와 다른 경우
            Wallet requesterWallet = createWallet(999L, memberId, new BigDecimal("0"));
            given(walletService.getWalletEntity(memberId)).willReturn(requesterWallet);

            // when & then
            assertThatThrownBy(() -> settlementTransferService.cancelSettlementTransfer(
                    transferId, memberId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.TRANSFER_ACCESS_DENIED.getMessage());
        }

        @Test
        @DisplayName("실패: CANCELLED 상태의 정산 송금 취소 시도 시 TRANSFER_NOT_CANCELLABLE 예외가 발생한다")
        void cancelSettlementTransfer_cancelledStatus_throwsBusinessException() {
            // given
            Long transferId = 1L;
            Long memberId = 3L;
            Long senderWalletId = 100L;

            // PENDING으로 생성 후 cancel()을 호출하여 CANCELLED 상태로 전환한 뒤 재시도
            SettlementTransfer transfer = SettlementTransfer.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(200L)
                    .amount(new BigDecimal("50000"))
                    .build();
            ReflectionTestUtils.setField(transfer, "id", transferId);
            transfer.cancel(); // PENDING → CANCELLED

            given(settlementTransferRepository.findById(transferId)).willReturn(Optional.of(transfer));

            Wallet senderWallet = createWallet(senderWalletId, memberId, new BigDecimal("0"));
            given(walletService.getWalletEntity(memberId)).willReturn(senderWallet);

            // when & then
            assertThatThrownBy(() -> settlementTransferService.cancelSettlementTransfer(
                    transferId, memberId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.TRANSFER_NOT_CANCELLABLE.getMessage());
        }

        @Test
        @DisplayName("실패: 환불 이체 시 지갑이 존재하지 않는 경우 WALLET_NOT_FOUND 예외가 발생한다")
        void cancelSettlementTransfer_walletNotFound_throwsEntityNotFoundException() {
            // given
            Long transferId = 1L;
            Long memberId = 3L;
            Long senderWalletId = 100L;
            Long receiverWalletId = 200L;

            SettlementTransfer transfer = SettlementTransfer.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(new BigDecimal("50000"))
                    .build();
            ReflectionTestUtils.setField(transfer, "id", transferId);

            given(settlementTransferRepository.findById(transferId)).willReturn(Optional.of(transfer));

            Wallet senderWallet = createWallet(senderWalletId, memberId, new BigDecimal("50000"));
            given(walletService.getWalletEntity(memberId)).willReturn(senderWallet);

            // ownerWalletId = receiverWalletId=200 → findById 결과 없음 → WALLET_NOT_FOUND
            given(walletRepository.findById(receiverWalletId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> settlementTransferService.cancelSettlementTransfer(
                    transferId, memberId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ErrorCode.WALLET_NOT_FOUND.getMessage());
        }
    }

    // =========================================================
    // 헬퍼 메서드
    // =========================================================

    /**
     * 테스트용 Session 엔티티를 생성한다.
     *
     * @param id      세션 ID (ReflectionTestUtils로 주입)
     * @param ownerId 세션 소유자(총무) 회원 ID
     * @return 생성된 Session 인스턴스
     */
    private Session createSession(Long id, Long ownerId) {
        Session session = Session.builder()
                .title("테스트 세션")
                .ownerId(ownerId)
                .build();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    /**
     * 테스트용 OrgMember 엔티티를 생성한다.
     *
     * @param id     조직 멤버 ID (ReflectionTestUtils로 주입)
     * @param userId 연결된 회원 ID (null이면 비회원)
     * @return 생성된 OrgMember 인스턴스
     */
    private OrgMember createOrgMember(Long id, Long userId) {
        OrgMember orgMember = OrgMember.builder()
                .sessionId(1L)
                .userId(userId)
                .nickname("테스트멤버")
                .build();
        ReflectionTestUtils.setField(orgMember, "id", id);
        return orgMember;
    }

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
