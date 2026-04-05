package com.dirtypay.domain.organization.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeTest {

    @Test
    @DisplayName("루트 노드 생성 시 parentNodeId가 null이다")
    void createRootNode() {
        // given & when
        Node node = Node.builder()
                .sessionId(1L)
                .name("전체")
                .depth(0)
                .sortOrder(0)
                .build();

        // then
        assertThat(node.getParentNodeId()).isNull();
        assertThat(node.isRoot()).isTrue();
        assertThat(node.getDepth()).isZero();
    }

    @Test
    @DisplayName("자식 노드 생성 시 모든 필드가 올바르게 설정된다")
    void createChildNode() {
        // given & when
        Node node = Node.builder()
                .sessionId(1L)
                .parentNodeId(10L)
                .name("개발팀")
                .depth(1)
                .sortOrder(2)
                .build();

        // then
        assertThat(node.getSessionId()).isEqualTo(1L);
        assertThat(node.getParentNodeId()).isEqualTo(10L);
        assertThat(node.getName()).isEqualTo("개발팀");
        assertThat(node.getDepth()).isEqualTo(1);
        assertThat(node.getSortOrder()).isEqualTo(2);
        assertThat(node.isRoot()).isFalse();
    }

    @Test
    @DisplayName("depth 최대값(4)으로 노드 생성이 가능하다")
    void createNode_maxDepth() {
        // given & when
        Node node = Node.builder()
                .sessionId(1L)
                .parentNodeId(10L)
                .name("최하위")
                .depth(4)
                .sortOrder(0)
                .build();

        // then
        assertThat(node.getDepth()).isEqualTo(4);
    }

    @Test
    @DisplayName("depth가 음수이면 예외가 발생한다")
    void createNode_negativeDepth() {
        // when & then
        assertThatThrownBy(() -> Node.builder()
                .sessionId(1L)
                .name("잘못된 노드")
                .depth(-1)
                .sortOrder(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0~4 범위");
    }

    @Test
    @DisplayName("depth가 4를 초과하면 예외가 발생한다")
    void createNode_exceedMaxDepth() {
        // when & then
        assertThatThrownBy(() -> Node.builder()
                .sessionId(1L)
                .name("잘못된 노드")
                .depth(5)
                .sortOrder(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0~4 범위");
    }

    @Test
    @DisplayName("update() 호출 시 name과 sortOrder가 변경된다")
    void update_success() {
        // given
        Node node = Node.builder()
                .sessionId(1L)
                .name("원래 이름")
                .depth(0)
                .sortOrder(0)
                .build();

        // when
        node.update("새 이름", 5);

        // then
        assertThat(node.getName()).isEqualTo("새 이름");
        assertThat(node.getSortOrder()).isEqualTo(5);
    }

    @Test
    @DisplayName("move() 호출 시 parentNodeId와 depth가 변경된다")
    void move_success() {
        // given
        Node node = Node.builder()
                .sessionId(1L)
                .name("이동 대상")
                .depth(1)
                .sortOrder(0)
                .build();

        // when
        node.move(20L, 2);

        // then
        assertThat(node.getParentNodeId()).isEqualTo(20L);
        assertThat(node.getDepth()).isEqualTo(2);
        assertThat(node.isRoot()).isFalse();
    }

    @Test
    @DisplayName("move() 시 depth가 범위를 초과하면 예외가 발생한다")
    void move_exceedDepth() {
        // given
        Node node = Node.builder()
                .sessionId(1L)
                .name("이동 대상")
                .depth(1)
                .sortOrder(0)
                .build();

        // when & then
        assertThatThrownBy(() -> node.move(20L, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
