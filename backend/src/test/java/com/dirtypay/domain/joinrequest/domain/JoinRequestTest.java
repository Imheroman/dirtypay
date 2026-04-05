package com.dirtypay.domain.joinrequest.domain;

import com.dirtypay.domain.joinrequest.entity.JoinRequest;
import com.dirtypay.domain.joinrequest.entity.JoinRequestStatus;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JoinRequest} Entity 단위 테스트.
 *
 * <p>참여 요청 생성, 상태 전이(approve/reject), verifyPending 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class JoinRequestTest {

    // === 생성 테스트 ===

    @Nested
    @DisplayName("JoinRequest 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("Builder로 JoinRequest 생성 시 초기 상태는 PENDING이다")
        void create_defaultStatusIsPending() {
            // given & when
            JoinRequest joinRequest = JoinRequest.builder()
                    .sessionId(1L)
                    .requesterId(10L)
                    .nickname("테스트유저")
                    .message("참여 요청합니다")
                    .build();

            // then
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        }

        @Test
        @DisplayName("Builder로 JoinRequest 생성 시 모든 필드가 올바르게 설정된다")
        void create_allFieldsSet() {
            // given & when
            JoinRequest joinRequest = JoinRequest.builder()
                    .sessionId(1L)
                    .requesterId(10L)
                    .nickname("홍길동")
                    .message("안녕하세요")
                    .build();

            // then
            assertThat(joinRequest.getSessionId()).isEqualTo(1L);
            assertThat(joinRequest.getRequesterId()).isEqualTo(10L);
            assertThat(joinRequest.getNickname()).isEqualTo("홍길동");
            assertThat(joinRequest.getMessage()).isEqualTo("안녕하세요");
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        }

        @Test
        @DisplayName("message는 null로 생성할 수 있다")
        void create_nullMessage() {
            // given & when
            JoinRequest joinRequest = JoinRequest.builder()
                    .sessionId(1L)
                    .requesterId(10L)
                    .nickname("홍길동")
                    .message(null)
                    .build();

            // then
            assertThat(joinRequest.getMessage()).isNull();
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        }

        @Test
        @DisplayName("신규 생성된 JoinRequest는 isPending()이 true이다")
        void create_isPendingReturnsTrue() {
            // given & when
            JoinRequest joinRequest = JoinRequest.builder()
                    .sessionId(1L)
                    .requesterId(10L)
                    .nickname("테스트유저")
                    .build();

            // then
            assertThat(joinRequest.isPending()).isTrue();
        }
    }

    // === 정상 상태 전이 테스트 ===

    @Nested
    @DisplayName("approve() 정상 전이 테스트")
    class ApproveSuccessTest {

        @Test
        @DisplayName("PENDING 상태에서 approve() 호출 시 APPROVED 상태로 전환된다")
        void approve_pendingToApproved() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.PENDING);

            // when
            joinRequest.approve();

            // then
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);
        }

        @Test
        @DisplayName("approve() 호출 후 isPending()은 false이다")
        void approve_isPendingReturnsFalse() {
            // given
            JoinRequest joinRequest = createPendingRequest();

            // when
            joinRequest.approve();

            // then
            assertThat(joinRequest.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("reject() 정상 전이 테스트")
    class RejectSuccessTest {

        @Test
        @DisplayName("PENDING 상태에서 reject() 호출 시 REJECTED 상태로 전환된다")
        void reject_pendingToRejected() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.PENDING);

            // when
            joinRequest.reject();

            // then
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.REJECTED);
        }

        @Test
        @DisplayName("reject() 호출 후 isPending()은 false이다")
        void reject_isPendingReturnsFalse() {
            // given
            JoinRequest joinRequest = createPendingRequest();

            // when
            joinRequest.reject();

            // then
            assertThat(joinRequest.isPending()).isFalse();
        }
    }

    // === 비정상 전이 예외 테스트 ===

    @Nested
    @DisplayName("approve() 비정상 전이 예외 테스트")
    class ApproveFailTest {

        @Test
        @DisplayName("APPROVED 상태에서 approve() 재호출 시 BusinessException이 발생한다")
        void approve_alreadyApproved_throwsBusinessException() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            joinRequest.approve();
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);

            // when & then
            assertThatThrownBy(joinRequest::approve)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }

        @Test
        @DisplayName("REJECTED 상태에서 approve() 호출 시 BusinessException이 발생한다")
        void approve_alreadyRejected_throwsBusinessException() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            joinRequest.reject();
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.REJECTED);

            // when & then
            assertThatThrownBy(joinRequest::approve)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }
    }

    @Nested
    @DisplayName("reject() 비정상 전이 예외 테스트")
    class RejectFailTest {

        @Test
        @DisplayName("APPROVED 상태에서 reject() 호출 시 BusinessException이 발생한다")
        void reject_alreadyApproved_throwsBusinessException() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            joinRequest.approve();
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);

            // when & then
            assertThatThrownBy(joinRequest::reject)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }

        @Test
        @DisplayName("REJECTED 상태에서 reject() 재호출 시 BusinessException이 발생한다")
        void reject_alreadyRejected_throwsBusinessException() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            joinRequest.reject();
            assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.REJECTED);

            // when & then
            assertThatThrownBy(joinRequest::reject)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }
    }

    // === 경계 조건 테스트 ===

    @Nested
    @DisplayName("경계 조건 테스트")
    class BoundaryTest {

        @Test
        @DisplayName("예외 메시지는 JOIN_REQUEST_NOT_PENDING ErrorCode 메시지와 일치한다")
        void approve_exceptionMessage_matchesErrorCode() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            joinRequest.approve();

            // when & then
            assertThatThrownBy(joinRequest::approve)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.JOIN_REQUEST_NOT_PENDING.getMessage());
        }

        @Test
        @DisplayName("approve() 이후 reject() 호출 시 BusinessException이 발생한다 — 순차 상태 전이 보호")
        void approvedThenReject_throwsBusinessException() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            joinRequest.approve();

            // when & then
            assertThatThrownBy(joinRequest::reject)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }

        @Test
        @DisplayName("reject() 이후 approve() 호출 시 BusinessException이 발생한다 — 순차 상태 전이 보호")
        void rejectedThenApprove_throwsBusinessException() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            joinRequest.reject();

            // when & then
            assertThatThrownBy(joinRequest::approve)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }

        @Test
        @DisplayName("isPending()은 APPROVED 상태에서 false를 반환한다")
        void isPending_returnsFalse_whenApproved() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            joinRequest.approve();

            // then
            assertThat(joinRequest.isPending()).isFalse();
        }

        @Test
        @DisplayName("isPending()은 REJECTED 상태에서 false를 반환한다")
        void isPending_returnsFalse_whenRejected() {
            // given
            JoinRequest joinRequest = createPendingRequest();
            joinRequest.reject();

            // then
            assertThat(joinRequest.isPending()).isFalse();
        }
    }

    // === Helper Methods ===

    /**
     * 테스트용 PENDING 상태의 JoinRequest 생성 헬퍼.
     *
     * @return PENDING 상태의 JoinRequest 인스턴스
     */
    private JoinRequest createPendingRequest() {
        return JoinRequest.builder()
                .sessionId(1L)
                .requesterId(10L)
                .nickname("테스트유저")
                .message("참여 요청합니다")
                .build();
    }
}
