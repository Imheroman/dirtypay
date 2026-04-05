package com.dirtypay.domain.order.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OrderCreateRequest} Bean Validation 테스트.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class OrderCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("수량 50 — 정상 통과")
    void quantity_max_boundary_valid() {
        OrderCreateRequest request = createRequest(1L, 50, List.of(1L));

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("수량 1 — 정상 통과")
    void quantity_min_boundary_valid() {
        OrderCreateRequest request = createRequest(1L, 1, List.of(1L));

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("수량 51 — 검증 실패")
    void quantity_exceeds_max() {
        OrderCreateRequest request = createRequest(1L, 51, List.of(1L));

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("수량은 최대 50개입니다");
    }

    @Test
    @DisplayName("수량 0 — 검증 실패")
    void quantity_below_min() {
        OrderCreateRequest request = createRequest(1L, 0, List.of(1L));

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("수량은 1 이상이어야 합니다");
    }

    private OrderCreateRequest createRequest(Long menuId, int quantity, List<Long> memberIds) {
        OrderCreateRequest request = new OrderCreateRequest();
        ReflectionTestUtils.setField(request, "groupId", 1L);
        ReflectionTestUtils.setField(request, "menuId", menuId);
        ReflectionTestUtils.setField(request, "quantity", quantity);
        ReflectionTestUtils.setField(request, "memberIds", memberIds);
        return request;
    }
}
