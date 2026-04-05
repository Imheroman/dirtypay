package com.dirtypay.domain.group.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.group.dto.request.GroupChangeRequest;
import com.dirtypay.domain.group.dto.request.GroupCreateRequest;
import com.dirtypay.domain.group.dto.request.GroupUpdateRequest;
import com.dirtypay.domain.group.dto.request.SharedMenuSaveRequest;
import com.dirtypay.domain.group.dto.response.GroupResponse;
import com.dirtypay.domain.group.service.GroupService;
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
 * 그룹 컨트롤러.
 *
 * <p>그룹 CRUD, 참여/탈퇴, 공유 메뉴 관리 API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Group", description = "그룹 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /**
     * 라운드의 그룹 목록을 트리 구조로 조회한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param roundId       라운드 ID
     * @return 그룹 트리 목록
     */
    @Operation(summary = "그룹 목록 조회", description = "라운드에 속한 그룹 목록을 계층 구조로 조회합니다.")
    @GetMapping("/rounds/{roundId}/groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getGroups(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roundId) {

        List<GroupResponse> response = this.groupService.getGroups(roundId, userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 새로운 그룹을 생성한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param roundId       라운드 ID
     * @param request       그룹 생성 요청
     * @return 생성된 그룹 응답
     */
    @Operation(summary = "그룹 생성", description = "라운드에 새로운 그룹을 생성합니다. 생성자가 자동 참여되지 않으므로 별도로 joinGroup을 호출해야 합니다.")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @PostMapping("/rounds/{roundId}/groups")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roundId,
            @Valid @RequestBody GroupCreateRequest request) {

        GroupResponse response = this.groupService.createGroup(roundId, request, userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 그룹명을 수정한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param groupId       그룹 ID
     * @param request       수정 요청
     * @return 수정된 그룹 응답
     */
    @Operation(summary = "그룹 수정", description = "그룹 이름을 수정합니다.")
    @SessionAccess(value = "groupId", type = ResourceType.GROUP, level = AccessLevel.MEMBER)
    @PutMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateRequest request) {

        GroupResponse response = this.groupService.updateGroup(groupId, request, userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 그룹을 삭제한다. (Soft Delete)
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param groupId       그룹 ID
     * @return 빈 응답
     */
    @Operation(summary = "그룹 삭제", description = "그룹을 삭제합니다. 참여 중인 멤버가 있으면 삭제할 수 없습니다. 하위 그룹도 함께 삭제됩니다. (Soft Delete)")
    @SessionAccess(value = "groupId", type = ResourceType.GROUP, level = AccessLevel.MEMBER)
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long groupId) {

        this.groupService.deleteGroup(groupId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success());
    }

    /**
     * 현재 사용자가 그룹에 참여한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param groupId       그룹 ID
     * @return 빈 응답
     */
    @Operation(summary = "그룹 참여", description = "현재 사용자가 그룹에 참여합니다. 라운드당 1개 그룹만 참여 가능하며, 이미 다른 그룹에 참여 중이면 changeGroup을 사용해야 합니다.")
    @PostMapping("/groups/{groupId}/join")
    public ResponseEntity<ApiResponse<Void>> joinGroup(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long groupId) {

        this.groupService.joinGroup(groupId, userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success());
    }

    /**
     * 현재 사용자의 그룹을 변경한다.
     *
     * <p>기존 그룹 탈퇴와 새 그룹 참여를 단일 트랜잭션으로 처리한다.</p>
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param groupId       현재 참여 중인 그룹 ID
     * @param request       그룹 변경 요청
     * @return 빈 응답
     */
    @Operation(summary = "그룹 변경", description = "현재 참여 중인 그룹에서 다른 그룹으로 이동합니다.")
    @SessionAccess(value = "groupId", type = ResourceType.GROUP, level = AccessLevel.MEMBER)
    @PutMapping("/groups/{groupId}/change")
    public ResponseEntity<ApiResponse<Void>> changeGroup(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long groupId,
            @Valid @RequestBody GroupChangeRequest request) {

        this.groupService.changeGroup(groupId, request.getToGroupId(), userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success());
    }

    /**
     * 현재 사용자가 그룹에서 탈퇴한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param groupId       그룹 ID
     * @return 빈 응답
     */
    @Operation(summary = "그룹 탈퇴", description = "현재 사용자가 그룹에서 탈퇴합니다.")
    @DeleteMapping("/groups/{groupId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long groupId) {

        this.groupService.leaveGroup(groupId, userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success());
    }

    /**
     * 그룹의 공유 메뉴를 저장한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param groupId       그룹 ID
     * @param request       공유 메뉴 저장 요청
     * @return 빈 응답
     */
    @Operation(summary = "공유 메뉴 저장", description = "그룹의 공유 메뉴를 전체 교체 방식으로 저장합니다.")
    @SessionAccess(value = "groupId", type = ResourceType.GROUP, level = AccessLevel.MEMBER)
    @PutMapping("/groups/{groupId}/shared-menus")
    public ResponseEntity<ApiResponse<Void>> saveSharedMenus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long groupId,
            @Valid @RequestBody SharedMenuSaveRequest request) {

        this.groupService.saveSharedMenus(groupId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success());
    }
}
