package com.dirtypay.domain.organization.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.organization.dto.request.OrgMemberCreateRequest;
import com.dirtypay.domain.organization.dto.request.OrgMemberUpdateRequest;
import com.dirtypay.domain.organization.dto.response.OrgMemberResponse;
import com.dirtypay.domain.organization.service.OrgMemberService;
import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.security.annotation.SessionAccess;
import com.dirtypay.global.security.annotation.SessionAccess.AccessLevel;
import com.dirtypay.global.security.annotation.SessionAccess.ResourceType;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 조직도 멤버 API 컨트롤러.
 *
 * <p>멤버의 생성, 조회, 수정, 삭제(Soft Delete) API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Member", description = "조직도 멤버 API")
@RestController
@RequiredArgsConstructor
public class OrgMemberController {

    private final OrgMemberService orgMemberService;

    /**
     * 새로운 멤버를 생성한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param sessionId     세션 ID
     * @param request       멤버 생성 요청 DTO
     * @return 생성된 멤버 정보
     */
    @Operation(summary = "멤버 생성", description = "세션에 새로운 조직도 멤버를 생성합니다.")
    @SessionAccess(value = "sessionId")
    @PostMapping("/api/sessions/{sessionId}/members")
    public ResponseEntity<ApiResponse<OrgMemberResponse>> createMember(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @Valid @RequestBody OrgMemberCreateRequest request) {

        OrgMemberResponse response = this.orgMemberService.createMember(sessionId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 세션의 전체 멤버를 조회한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param sessionId     세션 ID
     * @return 멤버 목록
     */
    @Operation(summary = "세션 전체 멤버 조회", description = "세션에 속한 모든 멤버를 조회합니다.")
    @SessionAccess(value = "sessionId", level = AccessLevel.MEMBER)
    @GetMapping("/api/sessions/{sessionId}/members")
    public ResponseEntity<ApiResponse<List<OrgMemberResponse>>> getMembersBySessionId(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId) {

        List<OrgMemberResponse> response = this.orgMemberService.getMembersBySessionId(sessionId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 멤버 정보를 수정한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param id            멤버 ID
     * @param request       멤버 수정 요청 DTO
     * @return 수정된 멤버 정보
     */
    @Operation(summary = "멤버 수정", description = "멤버 정보를 수정합니다.")
    @SessionAccess(value = "id", type = ResourceType.MEMBER)
    @PutMapping("/api/members/{id}")
    public ResponseEntity<ApiResponse<OrgMemberResponse>> updateMember(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody OrgMemberUpdateRequest request) {

        OrgMemberResponse response = this.orgMemberService.updateMember(id, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * OrgMember에 현재 인증된 사용자를 연결한다.
     *
     * <p>userId가 null인 OrgMember에 인증된 사용자의 회원 ID를 연결한다.
     * 이미 회원이 연결된 경우 실패한다.</p>
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param id            멤버 ID
     * @return 연결된 멤버 정보
     */
    @Operation(summary = "멤버에 회원 연결", description = "OrgMember에 현재 로그인한 사용자를 연결합니다.")
    @SessionAccess(value = "id", type = ResourceType.MEMBER)
    @PatchMapping("/api/members/{id}/link")
    public ResponseEntity<ApiResponse<OrgMemberResponse>> linkMemberToUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {

        OrgMemberResponse response = this.orgMemberService.linkMemberToUser(id, userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 멤버를 삭제한다. (Soft Delete)
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param id            멤버 ID
     * @return 빈 응답
     */
    @Operation(summary = "멤버 삭제", description = "멤버를 삭제합니다. (Soft Delete)")
    @SessionAccess(value = "id", type = ResourceType.MEMBER)
    @DeleteMapping("/api/members/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {

        this.orgMemberService.deleteMember(id);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success());
    }
}
