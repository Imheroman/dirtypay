package com.dirtypay.domain.session.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.session.dto.request.SessionCreateRequest;
import com.dirtypay.domain.session.dto.request.SessionUpdateRequest;
import com.dirtypay.domain.session.dto.response.SessionResponse;
import com.dirtypay.domain.session.service.SessionService;
import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.security.annotation.SessionAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 세션 API 컨트롤러.
 *
 * <p>세션의 생성, 목록 조회, 상세 조회, 수정, 삭제(Soft Delete) API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Session", description = "세션 API")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * 새로운 세션을 생성한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param request       세션 생성 요청 DTO
     * @return 생성된 세션 정보
     */
    @Operation(summary = "세션 생성", description = "새로운 정산 세션을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SessionCreateRequest request) {

        SessionResponse response = this.sessionService.createSession(request, userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 현재 사용자의 세션 목록을 조회한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @return 세션 목록
     */
    @Operation(summary = "세션 목록 조회", description = "현재 사용자가 참여 중인 세션 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<SessionResponse> response = this.sessionService.getSessions(userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 현재 사용자의 만료(ARCHIVED) 세션 목록을 조회한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @return 만료 세션 목록
     */
    @Operation(summary = "만료 세션 목록 조회", description = "현재 사용자가 참여 중인 만료(ARCHIVED) 세션 목록을 조회합니다.")
    @GetMapping("/archived")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getArchivedSessions(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<SessionResponse> response = this.sessionService.getArchivedSessions(userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 초대 코드로 세션을 조회한다.
     *
     * @param inviteCode 초대 코드
     * @return 세션 상세 정보
     */
    @Operation(summary = "초대 코드로 세션 조회", description = "초대 코드로 세션을 조회합니다.")
    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSessionByInviteCode(
            @PathVariable String inviteCode) {

        SessionResponse response = this.sessionService.getSessionByInviteCode(inviteCode);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 세션 상세 정보를 조회한다.
     *
     * @param id 세션 ID
     * @return 세션 상세 정보
     */
    @Operation(summary = "세션 상세 조회", description = "세션의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(@PathVariable Long id) {

        SessionResponse response = this.sessionService.getSession(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 세션 정보를 수정한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param id            세션 ID
     * @param request       세션 수정 요청 DTO
     * @return 수정된 세션 정보
     */
    @Operation(summary = "세션 수정", description = "세션 정보를 수정합니다.")
    @SessionAccess("id")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> updateSession(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody SessionUpdateRequest request) {

        SessionResponse response = this.sessionService.updateSession(id, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 세션을 완료(ARCHIVED) 상태로 변경한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param id            세션 ID
     * @return 변경된 세션 정보
     */
    @Operation(summary = "세션 완료", description = "세션을 완료(ARCHIVED) 상태로 변경합니다.")
    @SessionAccess("id")
    @PatchMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<SessionResponse>> archiveSession(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {

        SessionResponse response = this.sessionService.archiveSession(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 세션을 삭제한다. (Soft Delete)
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param id            세션 ID
     * @return 빈 응답
     */
    @Operation(summary = "세션 삭제", description = "세션을 삭제합니다. (Soft Delete)")
    @SessionAccess("id")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {

        this.sessionService.deleteSession(id);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success());
    }
}
