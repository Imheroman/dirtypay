package com.dirtypay.global.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.spel.SpelEvaluationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * {@link CustomSpelExpressionParser} 단위 테스트.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomSpelExpressionParser 단위 테스트")
class CustomSpelExpressionParserTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Nested
    @DisplayName("parse")
    class ParseTest {

        @Test
        @DisplayName("단순 파라미터 참조: #receiverId -> 실제 값 반환")
        void parse_singleParam_returnsValue() {
            // given
            given(joinPoint.getSignature()).willReturn(signature);
            given(signature.getParameterNames()).willReturn(new String[]{"receiverId"});
            given(joinPoint.getArgs()).willReturn(new Object[]{1L});

            // when
            String result = CustomSpelExpressionParser.parse("#receiverId", joinPoint);

            // then
            assertThat(result).isEqualTo("1");
        }

        @Test
        @DisplayName("복합 표현식: 'wallet:' + #receiverId -> 'wallet:1' 반환")
        void parse_concatExpression_returnsConcatenatedValue() {
            // given
            given(joinPoint.getSignature()).willReturn(signature);
            given(signature.getParameterNames()).willReturn(new String[]{"receiverId"});
            given(joinPoint.getArgs()).willReturn(new Object[]{1L});

            // when
            String result = CustomSpelExpressionParser.parse("'wallet:' + #receiverId", joinPoint);

            // then
            assertThat(result).isEqualTo("wallet:1");
        }

        @Test
        @DisplayName("다중 파라미터 중 두 번째 파라미터를 SpEL로 참조한다")
        void parse_multipleParams_referencesSecondParam() {
            // given
            given(joinPoint.getSignature()).willReturn(signature);
            given(signature.getParameterNames()).willReturn(new String[]{"senderId", "receiverId", "amount"});
            given(joinPoint.getArgs()).willReturn(new Object[]{10L, 20L, new BigDecimal("50000")});

            // when
            String result = CustomSpelExpressionParser.parse("'wallet:' + #receiverId", joinPoint);

            // then
            assertThat(result).isEqualTo("wallet:20");
        }

        @Test
        @DisplayName("null 파라미터 참조 시 문자열 'null'을 반환한다")
        void parse_nullParam_returnsStringNull() {
            // given
            given(joinPoint.getSignature()).willReturn(signature);
            given(signature.getParameterNames()).willReturn(new String[]{"key"});
            given(joinPoint.getArgs()).willReturn(new Object[]{null});

            // when
            String result = CustomSpelExpressionParser.parse("#key", joinPoint);

            // then
            assertThat(result).isEqualTo("null");
        }

        @Test
        @DisplayName("존재하지 않는 변수의 메서드를 호출하면 SpelEvaluationException이 발생한다")
        void parse_nonExistentVariable_throwsSpelEvaluationException() {
            // given
            given(joinPoint.getSignature()).willReturn(signature);
            given(signature.getParameterNames()).willReturn(new String[]{"senderId"});
            given(joinPoint.getArgs()).willReturn(new Object[]{1L});

            // when & then
            // #nonExistent 자체는 null 반환이지만, .length() 호출 시 SpelEvaluationException 발생
            assertThatThrownBy(() -> CustomSpelExpressionParser.parse("#nonExistent.length()", joinPoint))
                    .isInstanceOf(SpelEvaluationException.class);
        }
    }
}
