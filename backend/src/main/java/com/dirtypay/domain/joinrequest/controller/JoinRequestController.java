package com.dirtypay.domain.joinrequest.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.joinrequest.dto.request.JoinRequestCreateRequest;
import com.dirtypay.domain.joinrequest.dto.response.JoinRequestResponse;
import com.dirtypay.domain.joinrequest.entity.JoinRequestStatus;
import com.dirtypay.domain.joinrequest.service.JoinRequestService;
import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.security.annotation.SessionAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 참여 요청 API 컨트롤러.
 *
 * <p>세션 참여 요청의 생성, 목록 조회, 승인, 거절 API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "JoinRequest", description = "참여 요청 API")
@RestController
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    /**
     * 초대 코드로 세션에 참여 요청을 제출한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param inviteCode    초대 코드
     * @param request       참여 요청 생성 요청 DTO
     * @return 생성된 참여 요청 정보
     */
    @Operation(summary = "참여 요청 제출", description = "초대 코드로 세션에 참여 요청을 제출합니다.")
    @PostMapping("/api/sessions/invite/{inviteCode}/join-requests")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> createJoinRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String inviteCode,
            @Valid @RequestBody JoinRequestCreateRequest request) {

        JoinRequestResponse response = this.joinRequestService.createJoinRequest(
                inviteCode, userPrincipal.getId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 세션의 참여 요청 목록을 페이지 단위로 조회한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param sessionId     세션 ID
     * @param status        요청 상태 필터 (선택)
     * @param pageable      페이지 요청 정보 (기본: 20건, 생성일시 내림차순)
     * @return 참여 요청 페이지
     */
    @Operation(summary = "참여 요청 목록 조회", description = "세션의 참여 요청 목록을 페이지 단위로 조회합니다.")
    @SessionAccess("sessionId")
    @GetMapping("/api/sessions/{sessionId}/join-requests")
    public ResponseEntity<ApiResponse<Page<JoinRequestResponse>>> getJoinRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @RequestParam(required = false) JoinRequestStatus status,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<JoinRequestResponse> response = this.joinRequestService.getJoinRequests(sessionId, status, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 참여 요청을 승인한다.
     *
     * <p>승인 시 미배정 노드에 OrgMember가 자동 생성된다.</p>
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param sessionId     세션 ID
     * @param requestId     참여 요청 ID
     * @return 승인된 참여 요청 정보
     */
    @Operation(summary = "참여 요청 승인", description = "참여 요청을 승인하고, 미배정 노드에 OrgMember를 자동 생성합니다.")
    @SessionAccess("sessionId")
    @PatchMapping("/api/sessions/{sessionId}/join-requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> approveJoinRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @PathVariable Long requestId) {

        JoinRequestResponse response = this.joinRequestService.approveJoinRequest(
                sessionId, requestId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 참여 요청을 거절한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param sessionId     세션 ID
     * @param requestId     참여 요청 ID
     * @return 거절된 참여 요청 정보
     */
    @Operation(summary = "참여 요청 거절", description = "참여 요청을 거절합니다.")
    @SessionAccess("sessionId")
    @PatchMapping("/api/sessions/{sessionId}/join-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> rejectJoinRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @PathVariable Long requestId) {

        JoinRequestResponse response = this.joinRequestService.rejectJoinRequest(sessionId, requestId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }
}
