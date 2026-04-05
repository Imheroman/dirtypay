package com.dirtypay.domain.member.controller;

import com.dirtypay.domain.member.dto.request.MemberUpdateRequest;
import com.dirtypay.domain.member.dto.response.MemberResponse;
import com.dirtypay.domain.member.dto.response.MemberSearchResponse;
import com.dirtypay.domain.member.service.MemberService;
import com.dirtypay.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 API 컨트롤러.
 *
 * <p>회원 조회, 수정, 삭제 API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "User", description = "회원 API")
@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 이메일 또는 이름으로 회원을 검색한다.
     *
     * <p>검색 키워드는 최대 100자로 제한된다. 이를 초과하면 400 Bad Request를 반환한다.</p>
     *
     * @param query    검색 키워드 (최대 100자)
     * @param pageable 페이징 정보
     * @return 검색된 회원 목록
     */
    @Operation(summary = "회원 검색", description = "이메일 또는 이름으로 회원을 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<MemberSearchResponse>>> searchMembers(
            @Size(max = 100, message = "검색어는 최대 100자까지 입력 가능합니다.")
            @RequestParam String query,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<MemberSearchResponse> result = this.memberService.searchMembers(query, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(result));
    }

    /**
     * 회원 정보를 조회한다.
     *
     * @param id 조회할 회원의 ID
     * @return 회원 정보
     */
    @Operation(summary = "회원 조회", description = "회원 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(@PathVariable Long id) {

        MemberResponse response = this.memberService.getMember(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 회원 정보를 수정한다.
     *
     * <p>본인 계정 또는 ADMIN 역할을 가진 사용자만 수정할 수 있다.
     * 타인의 리소스에 접근하면 403 Forbidden을 반환한다.</p>
     *
     * @param id      수정할 회원의 ID
     * @param request 회원 정보 수정 요청 DTO
     * @return 수정된 회원 정보
     */
    @Operation(summary = "회원 정보 수정", description = "회원 정보를 수정합니다.")
    @PreAuthorize("authentication.principal.id == #id or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody MemberUpdateRequest request) {

        MemberResponse response = this.memberService.updateMember(id, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 회원을 삭제한다. (Soft Delete)
     *
     * <p>본인 계정 또는 ADMIN 역할을 가진 사용자만 삭제할 수 있다.
     * 타인의 리소스에 접근하면 403 Forbidden을 반환한다.</p>
     *
     * @param id 삭제할 회원의 ID
     * @return 성공 응답
     */
    @Operation(summary = "회원 삭제", description = "회원을 삭제합니다. (Soft Delete)")
    @PreAuthorize("authentication.principal.id == #id or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {

        this.memberService.deleteMember(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success());
    }
}
