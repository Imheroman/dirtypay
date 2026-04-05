package com.dirtypay.domain.order.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OrderUpdateRequest} Bean Validation 테스트.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class OrderUpdateRequestTest {

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
        OrderUpdateRequest request = createRequest(50);

        Set<ConstraintViolation<OrderUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("수량 1 — 정상 통과")
    void quantity_min_boundary_valid() {
        OrderUpdateRequest request = createRequest(1);

        Set<ConstraintViolation<OrderUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("수량 51 — 검증 실패")
    void quantity_exceeds_max() {
        OrderUpdateRequest request = createRequest(51);

        Set<ConstraintViolation<OrderUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("수량은 최대 50개입니다");
    }

    @Test
    @DisplayName("수량 0 — 검증 실패")
    void quantity_below_min() {
        OrderUpdateRequest request = createRequest(0);

        Set<ConstraintViolation<OrderUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("수량은 1 이상이어야 합니다");
    }

    private OrderUpdateRequest createRequest(int quantity) {
        OrderUpdateRequest request = new OrderUpdateRequest();
        ReflectionTestUtils.setField(request, "quantity", quantity);
        return request;
    }
}
