package com.dirtypay.domain.organization.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.organization.dto.request.NodeCreateRequest;
import com.dirtypay.domain.organization.dto.request.NodeMoveRequest;
import com.dirtypay.domain.organization.dto.request.NodeUpdateRequest;
import com.dirtypay.domain.organization.dto.response.NodeResponse;
import com.dirtypay.domain.organization.dto.response.NodeTreeResponse;
import com.dirtypay.domain.organization.service.NodeService;
import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.security.annotation.SessionAccess;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 조직도 노드 API 컨트롤러.
 *
 * <p>노드의 생성, 조회, 수정, 삭제(Soft Delete) API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Node", description = "조직도 노드 API")
@RestController
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    /**
     * 세션의 노드 트리를 조회한다.
     *
     * <p>세션에 속한 전체 노드를 트리 구조로 반환하며,
     * 각 노드에 소속 멤버 목록을 포함한다.</p>
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param sessionId     세션 ID
     * @return 노드 트리 목록 (루트 노드 리스트)
     */
    @Operation(summary = "노드 트리 조회", description = "세션의 전체 조직도 노드를 트리 구조로 조회합니다. 각 노드에 소속 멤버 목록이 포함됩니다.")
    @SessionAccess("sessionId")
    @GetMapping("/api/sessions/{sessionId}/nodes")
    public ResponseEntity<ApiResponse<List<NodeTreeResponse>>> getNodeTree(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId) {

        List<NodeTreeResponse> response = this.nodeService.getNodeTree(sessionId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 새로운 노드를 생성한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param sessionId     세션 ID
     * @param request       노드 생성 요청 DTO
     * @return 생성된 노드 정보
     */
    @Operation(summary = "노드 생성", description = "세션 내에 새로운 조직도 노드를 생성합니다.")
    @SessionAccess("sessionId")
    @PostMapping("/api/sessions/{sessionId}/nodes")
    public ResponseEntity<ApiResponse<NodeResponse>> createNode(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @Valid @RequestBody NodeCreateRequest request) {

        NodeResponse response = this.nodeService.createNode(sessionId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 노드 상세 정보를 조회한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param nodeId        노드 ID
     * @return 노드 상세 정보
     */
    @Operation(summary = "노드 상세 조회", description = "노드의 상세 정보를 조회합니다.")
    @SessionAccess(value = "nodeId", type = ResourceType.NODE)
    @GetMapping("/api/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<NodeResponse>> getNode(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long nodeId) {

        NodeResponse response = this.nodeService.getNode(nodeId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 노드 정보를 수정한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param nodeId        노드 ID
     * @param request       노드 수정 요청 DTO
     * @return 수정된 노드 정보
     */
    @Operation(summary = "노드 수정", description = "노드 정보를 수정합니다.")
    @SessionAccess(value = "nodeId", type = ResourceType.NODE)
    @PutMapping("/api/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<NodeResponse>> updateNode(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long nodeId,
            @Valid @RequestBody NodeUpdateRequest request) {

        NodeResponse response = this.nodeService.updateNode(nodeId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 노드를 다른 부모 노드 아래로 이동한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param nodeId        이동할 노드 ID
     * @param request       노드 이동 요청 DTO
     * @return 이동된 노드 정보
     */
    @Operation(summary = "노드 이동", description = "노드를 다른 부모 노드 아래로 이동합니다. 순환 참조가 발생하면 에러를 반환합니다.")
    @SessionAccess(value = "nodeId", type = ResourceType.NODE)
    @PutMapping("/api/nodes/{nodeId}/move")
    public ResponseEntity<ApiResponse<NodeResponse>> moveNode(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long nodeId,
            @Valid @RequestBody NodeMoveRequest request) {

        NodeResponse response = this.nodeService.moveNode(nodeId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 노드를 삭제한다. (Soft Delete)
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param nodeId        노드 ID
     * @return 빈 응답
     */
    @Operation(summary = "노드 삭제", description = "노드를 삭제합니다. (Soft Delete)")
    @SessionAccess(value = "nodeId", type = ResourceType.NODE)
    @DeleteMapping("/api/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNode(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long nodeId) {

        this.nodeService.deleteNode(nodeId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success());
    }
}
