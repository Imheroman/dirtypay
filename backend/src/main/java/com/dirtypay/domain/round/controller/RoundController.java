package com.dirtypay.domain.round.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.round.dto.request.RoundCreateRequest;
import com.dirtypay.domain.round.dto.request.RoundStatusChangeRequest;
import com.dirtypay.domain.round.dto.request.RoundUpdateRequest;
import com.dirtypay.domain.round.dto.response.RoundParticipantResponse;
import com.dirtypay.domain.round.dto.response.RoundResponse;
import com.dirtypay.domain.round.service.RoundService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 라운드 컨트롤러.
 *
 * <p>라운드 CRUD, 상태 변경, 참여자 관리 API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Round", description = "라운드 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoundController {

    private final RoundService roundService;

    /**
     * 새로운 라운드를 생성한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param sessionId     세션 ID
     * @param request       라운드 생성 요청
     * @return 생성된 라운드 응답
     */
    @Operation(summary = "라운드 생성", description = "세션에 새로운 라운드를 생성하고 참여자를 초기화합니다.")
    @SessionAccess(value = "sessionId", level = AccessLevel.MEMBER)
    @PostMapping("/sessions/{sessionId}/rounds")
    public ResponseEntity<ApiResponse<RoundResponse>> createRound(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @Valid @RequestBody RoundCreateRequest request) {

        RoundResponse response = this.roundService.createRound(sessionId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 세션의 라운드 목록을 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 라운드 목록
     */
    @Operation(summary = "라운드 목록 조회", description = "세션에 속한 라운드 목록을 조회합니다.")
    @GetMapping("/sessions/{sessionId}/rounds")
    public ResponseEntity<ApiResponse<List<RoundResponse>>> getRounds(@PathVariable Long sessionId) {

        List<RoundResponse> response = this.roundService.getRounds(sessionId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 라운드 상세 정보를 조회한다.
     *
     * @param roundId 라운드 ID
     * @return 라운드 응답
     */
    @Operation(summary = "라운드 상세 조회", description = "라운드의 상세 정보를 조회합니다.")
    @GetMapping("/rounds/{roundId}")
    public ResponseEntity<ApiResponse<RoundResponse>> getRound(@PathVariable Long roundId) {

        RoundResponse response = this.roundService.getRound(roundId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 라운드 정보를 수정한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param roundId       라운드 ID
     * @param request       수정 요청
     * @return 수정된 라운드 응답
     */
    @Operation(summary = "라운드 수정", description = "라운드 정보를 수정합니다.")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @PutMapping("/rounds/{roundId}")
    public ResponseEntity<ApiResponse<RoundResponse>> updateRound(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roundId,
            @Valid @RequestBody RoundUpdateRequest request) {

        RoundResponse response = this.roundService.updateRound(roundId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 라운드를 삭제한다. (Soft Delete)
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param roundId       라운드 ID
     * @return 빈 응답
     */
    @Operation(summary = "라운드 삭제", description = "라운드를 삭제합니다. (Soft Delete)")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @DeleteMapping("/rounds/{roundId}")
    public ResponseEntity<ApiResponse<Void>> deleteRound(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roundId) {

        this.roundService.deleteRound(roundId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success());
    }

    /**
     * 라운드 상태를 변경한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param roundId       라운드 ID
     * @param request       상태 변경 요청
     * @return 변경된 라운드 응답
     */
    @Operation(summary = "라운드 상태 변경", description = "라운드의 상태를 OPEN/CLOSED로 변경합니다.")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @PutMapping("/rounds/{roundId}/status")
    public ResponseEntity<ApiResponse<RoundResponse>> changeStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roundId,
            @Valid @RequestBody RoundStatusChangeRequest request) {

        RoundResponse response = this.roundService.changeStatus(roundId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 라운드의 참여자 목록을 조회한다.
     *
     * @param roundId 라운드 ID
     * @return 참여자 목록
     */
    @Operation(summary = "참여자 목록 조회", description = "라운드의 참여자 목록을 조회합니다.")
    @GetMapping("/rounds/{roundId}/participants")
    public ResponseEntity<ApiResponse<List<RoundParticipantResponse>>> getParticipants(
            @PathVariable Long roundId) {

        List<RoundParticipantResponse> response = this.roundService.getParticipants(roundId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 참여자를 정산에서 제외한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param roundId       라운드 ID
     * @param participantId 참여자 ID
     * @return 변경된 참여자 응답
     */
    @Operation(summary = "참여자 제외", description = "참여자를 정산에서 제외합니다.")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @PutMapping("/rounds/{roundId}/participants/{participantId}/exclude")
    public ResponseEntity<ApiResponse<RoundParticipantResponse>> excludeParticipant(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roundId,
            @PathVariable Long participantId) {

        RoundParticipantResponse response = this.roundService.excludeParticipant(roundId, participantId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 참여자를 정산에 포함한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param roundId       라운드 ID
     * @param participantId 참여자 ID
     * @return 변경된 참여자 응답
     */
    @Operation(summary = "참여자 포함", description = "제외된 참여자를 정산에 다시 포함합니다.")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @PutMapping("/rounds/{roundId}/participants/{participantId}/include")
    public ResponseEntity<ApiResponse<RoundParticipantResponse>> includeParticipant(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roundId,
            @PathVariable Long participantId) {

        RoundParticipantResponse response = this.roundService.includeParticipant(roundId, participantId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }
}
