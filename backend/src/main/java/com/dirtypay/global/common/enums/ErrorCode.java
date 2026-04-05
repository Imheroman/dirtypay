package com.dirtypay.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_003", "허용되지 않은 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_004", "요청한 리소스를 찾을 수 없습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "COMMON_005", "서비스가 일시적으로 과부하 상태입니다. 잠시 후 다시 시도해주세요."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "COMMON_006", "다른 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "만료된 토큰입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_005", "이미 사용 중인 이메일입니다."),

    // Session
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_001", "세션을 찾을 수 없습니다."),
    SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SESSION_002", "세션에 대한 접근 권한이 없습니다."),
    SESSION_ALREADY_ARCHIVED(HttpStatus.BAD_REQUEST, "SESSION_003", "이미 완료된 세션입니다."),

    // Node
    NODE_NOT_FOUND(HttpStatus.NOT_FOUND, "NODE_001", "노드를 찾을 수 없습니다."),
    NODE_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "NODE_002", "노드 최대 깊이를 초과했습니다."),
    NODE_SESSION_MISMATCH(HttpStatus.BAD_REQUEST, "NODE_003", "부모 노드가 다른 세션에 속해 있습니다."),
    NODE_CIRCULAR_REFERENCE(HttpStatus.BAD_REQUEST, "NODE_004", "순환 참조가 발생합니다. 자기 자신 또는 하위 노드로 이동할 수 없습니다."),
    NODE_SYSTEM_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, "NODE_005", "시스템 노드는 수정할 수 없습니다."),
    NODE_SYSTEM_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "NODE_006", "시스템 노드는 삭제할 수 없습니다."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "멤버를 찾을 수 없습니다."),
    MEMBER_ALREADY_LINKED(HttpStatus.CONFLICT, "MEMBER_002", "이미 회원이 연결된 멤버입니다."),
    MEMBER_SEARCH_QUERY_EMPTY(HttpStatus.BAD_REQUEST, "MEMBER_003", "검색어를 입력해주세요."),
    MEMBER_NODE_SESSION_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER_004", "이동 대상 노드가 같은 세션에 속해 있지 않습니다."),
    MEMBER_SEARCH_QUERY_TOO_LONG(HttpStatus.BAD_REQUEST, "MEMBER_005", "검색어는 최대 100자까지 입력 가능합니다."),

    // Round
    ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUND_001", "라운드를 찾을 수 없습니다."),
    ROUND_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "ROUND_002", "이미 마감된 라운드입니다."),
    ROUND_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUND_003", "라운드 참여자를 찾을 수 없습니다."),
    ROUND_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "ROUND_004", "세션당 최대 10개의 라운드만 생성할 수 있습니다."),
    ROUND_HAS_ORDERS(HttpStatus.BAD_REQUEST, "ROUND_005", "주문이 존재하는 라운드의 가게를 변경할 수 없습니다."),

    // Group
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP_001", "그룹을 찾을 수 없습니다."),
    GROUP_ALREADY_JOINED(HttpStatus.BAD_REQUEST, "GROUP_002", "이미 해당 그룹에 참여 중입니다."),
    GROUP_NOT_JOINED(HttpStatus.BAD_REQUEST, "GROUP_003", "해당 그룹에 참여하지 않은 상태입니다."),
    GROUP_SAME_GROUP(HttpStatus.BAD_REQUEST, "GROUP_004", "같은 그룹으로는 변경할 수 없습니다."),
    GROUP_ALREADY_IN_ROUND(HttpStatus.CONFLICT, "GROUP_005", "이미 해당 라운드의 다른 그룹에 참여 중입니다."),
    GROUP_HAS_MEMBERS(HttpStatus.BAD_REQUEST, "GROUP_006", "참여 중인 멤버가 있는 그룹은 삭제할 수 없습니다."),
    GROUP_ROUND_MISMATCH(HttpStatus.BAD_REQUEST, "GROUP_007", "부모 그룹이 다른 라운드에 속해 있습니다."),
    GROUP_CHANGE_DIFFERENT_ROUND(HttpStatus.BAD_REQUEST, "GROUP_008", "다른 라운드의 그룹으로는 이동할 수 없습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_001", "주문을 찾을 수 없습니다."),
    ORDER_GROUP_ROUND_MISMATCH(HttpStatus.BAD_REQUEST, "ORDER_002", "주문의 그룹이 해당 라운드에 속하지 않습니다."),
    ORDER_MEMBER_NOT_IN_GROUP(HttpStatus.BAD_REQUEST, "ORDER_003", "주문 참여자가 해당 그룹(또는 하위 그룹)에 속하지 않습니다."),

    // Store
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_001", "매장을 찾을 수 없습니다."),
    STORE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STORE_002", "매장에 대한 접근 권한이 없습니다."),
    STORE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "STORE_003", "활성화된 매장이 아닙니다."),
    STORE_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "STORE_004", "이미 폐업한 매장입니다."),
    STORE_BUSINESS_NUMBER_DUPLICATED(HttpStatus.CONFLICT, "STORE_005", "이미 등록된 사업자 등록번호입니다."),
    STORE_MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_006", "매장 메뉴를 찾을 수 없습니다."),
    STORE_MENU_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "STORE_007", "현재 주문할 수 없는 메뉴입니다."),
    STORE_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_008", "매장 주문을 찾을 수 없습니다."),
    STORE_ORDER_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, "STORE_009", "변경할 수 없는 주문 상태입니다."),
    STORE_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_010", "매장 리뷰를 찾을 수 없습니다."),
    STORE_POS_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "STORE_011", "POS 연동 매장은 POS 연동 키가 필수입니다."),
    STORE_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "STORE_012", "지원하지 않는 매장 유형입니다."),
    STORE_CUSTOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STORE_013", "CUSTOM 매장은 소유자만 접근할 수 있습니다."),

    // Settlement
    SETTLEMENT_INVALID_PAID_AMOUNT(HttpStatus.BAD_REQUEST, "SETTLEMENT_001", "납부 금액이 유효하지 않습니다."),

    // Wallet
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "WALLET_001", "지갑을 찾을 수 없습니다."),
    WALLET_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "WALLET_002", "잔액이 부족합니다."),
    WALLET_ALREADY_EXISTS(HttpStatus.CONFLICT, "WALLET_003", "이미 지갑이 존재합니다."),
    WALLET_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "WALLET_004", "금액이 유효하지 않습니다."),
    WALLET_DAILY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "WALLET_005", "일일 충전 한도를 초과했습니다."),
    WALLET_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "WALLET_006", "비활성 상태의 지갑입니다."),
    WALLET_TRANSFER_SAME_WALLET(HttpStatus.BAD_REQUEST, "WALLET_007", "같은 지갑으로 송금할 수 없습니다."),
    WALLET_DUPLICATE_TRANSACTION(HttpStatus.CONFLICT, "WALLET_008", "이미 처리된 거래입니다."),
    WALLET_LOCK_FAILED(HttpStatus.CONFLICT, "WALLET_009", "다른 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),

    // Transfer
    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRANSFER_001", "정산 송금을 찾을 수 없습니다."),
    TRANSFER_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "TRANSFER_002", "취소할 수 없는 상태입니다."),
    TRANSFER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "TRANSFER_003", "정산 송금에 대한 접근 권한이 없습니다."),

    // JoinRequest
    JOIN_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "JOIN_001", "참여 요청을 찾을 수 없습니다."),
    JOIN_REQUEST_ALREADY_PENDING(HttpStatus.CONFLICT, "JOIN_002", "이미 대기 중인 참여 요청이 있습니다."),
    JOIN_REQUEST_NOT_PENDING(HttpStatus.BAD_REQUEST, "JOIN_003", "대기 중인 요청만 처리할 수 있습니다."),
    JOIN_REQUEST_ALREADY_MEMBER(HttpStatus.CONFLICT, "JOIN_004", "이미 세션에 참여 중인 회원입니다."),
    ALREADY_SESSION_MEMBER(HttpStatus.CONFLICT, "JOIN_005", "동시 처리로 인해 이미 세션에 참여 중인 회원입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
