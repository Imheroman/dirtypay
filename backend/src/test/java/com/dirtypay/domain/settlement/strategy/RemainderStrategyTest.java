package com.dirtypay.domain.settlement.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RemainderStrategyTest {

    @Nested
    @DisplayName("OWNER 전략 테스트")
    class OwnerStrategyTest {

        @Test
        @DisplayName("나머지가 총무에게 할당된다")
        void owner_remainderGoesToOwner() {
            // given
            Map<Integer, BigDecimal> amounts = new HashMap<>();
            amounts.put(0, new BigDecimal("3333"));
            amounts.put(1, new BigDecimal("3333"));
            amounts.put(2, new BigDecimal("3333"));

            BigDecimal remainder = new BigDecimal("1");
            int ownerIndex = 0;

            // when
            new OwnerRemainderStrategy().apply(amounts, remainder, ownerIndex);

            // then
            assertThat(amounts.get(0)).isEqualByComparingTo(new BigDecimal("3334"));
            assertThat(amounts.get(1)).isEqualByComparingTo(new BigDecimal("3333"));
            assertThat(amounts.get(2)).isEqualByComparingTo(new BigDecimal("3333"));

            BigDecimal sum = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(new BigDecimal("10000"));
        }
    }

    @Nested
    @DisplayName("RANDOM 전략 테스트")
    class RandomStrategyTest {

        @Test
        @DisplayName("분배 후 합계가 원금과 같다")
        void random_sumEqualsTotal() {
            // given
            Map<Integer, BigDecimal> amounts = new HashMap<>();
            amounts.put(0, new BigDecimal("3333"));
            amounts.put(1, new BigDecimal("3333"));
            amounts.put(2, new BigDecimal("3333"));

            BigDecimal remainder = new BigDecimal("1");

            // when
            new RandomRemainderStrategy().apply(amounts, remainder, 0);

            // then
            BigDecimal sum = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(new BigDecimal("10000"));
        }
    }

    @Nested
    @DisplayName("ROUND_UP 전략 테스트")
    class RoundUpStrategyTest {

        @Test
        @DisplayName("총무 외 멤버는 10원 단위 올림, 분배합은 원금과 같다")
        void roundUp_membersRoundedUp() {
            // given
            Map<Integer, BigDecimal> amounts = new HashMap<>();
            amounts.put(0, new BigDecimal("3333"));
            amounts.put(1, new BigDecimal("3333"));
            amounts.put(2, new BigDecimal("3333"));

            BigDecimal remainder = new BigDecimal("1");
            int ownerIndex = 0;

            // when
            new RoundUpRemainderStrategy().apply(amounts, remainder, ownerIndex);

            // then
            assertThat(amounts.get(1)).isEqualByComparingTo(new BigDecimal("3340"));
            assertThat(amounts.get(2)).isEqualByComparingTo(new BigDecimal("3340"));

            BigDecimal sum = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("나머지가 0이면 변화 없음")
        void roundUp_noRemainder() {
            // given
            Map<Integer, BigDecimal> amounts = new HashMap<>();
            amounts.put(0, new BigDecimal("5000"));
            amounts.put(1, new BigDecimal("5000"));

            BigDecimal remainder = BigDecimal.ZERO;
            int ownerIndex = 0;

            // when
            new RoundUpRemainderStrategy().apply(amounts, remainder, ownerIndex);

            // then
            assertThat(amounts.get(0)).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(amounts.get(1)).isEqualByComparingTo(new BigDecimal("5000"));

            BigDecimal sum = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(new BigDecimal("10000"));
        }

    }
}
