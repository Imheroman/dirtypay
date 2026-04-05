package com.dirtypay.domain.organization.repository;

import com.dirtypay.domain.organization.entity.Node;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class NodeRepositoryTest {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EntityManager entityManager;

    private Node rootNode;
    private Node childNode;
    private Node deletedNode;

    @BeforeEach
    void setUp() {
        nodeRepository.deleteAll();

        rootNode = nodeRepository.save(Node.builder()
                .sessionId(1L)
                .name("전체")
                .depth(0)
                .sortOrder(0)
                .build());

        childNode = nodeRepository.save(Node.builder()
                .sessionId(1L)
                .parentNodeId(rootNode.getId())
                .name("개발팀")
                .depth(1)
                .sortOrder(1)
                .build());

        deletedNode = nodeRepository.save(Node.builder()
                .sessionId(1L)
                .parentNodeId(rootNode.getId())
                .name("삭제된 팀")
                .depth(1)
                .sortOrder(2)
                .build());
        deletedNode.delete();
        nodeRepository.save(deletedNode);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findById: @SQLRestriction에 의해 삭제되지 않은 노드를 ID로 조회한다")
    void findById_success() {
        // when
        Optional<Node> found = nodeRepository.findById(rootNode.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("전체");
    }

    @Test
    @DisplayName("findById: @SQLRestriction에 의해 삭제된 노드는 조회되지 않는다")
    void findById_excludesDeleted() {
        // when
        Optional<Node> found = nodeRepository.findById(deletedNode.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findBySessionId: 세션별 전체 노드를 조회한다")
    void findBySessionId_success() {
        // when
        List<Node> nodes = nodeRepository.findBySessionId(1L);

        // then
        assertThat(nodes).hasSize(2);
        assertThat(nodes).extracting(Node::getName)
                .containsExactlyInAnyOrder("전체", "개발팀");
    }

    @Test
    @DisplayName("findByParentNodeId: 부모 노드 ID로 자식 노드를 조회한다")
    void findByParentNodeId_success() {
        // when
        List<Node> children = nodeRepository.findByParentNodeId(rootNode.getId());

        // then
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getName()).isEqualTo("개발팀");
    }

    @Test
    @DisplayName("findBySessionIdAndParentNodeIdIsNull: 루트 노드를 조회한다")
    void findRootNodes_success() {
        // when
        List<Node> roots = nodeRepository.findBySessionIdAndParentNodeIdIsNull(1L);

        // then
        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).getName()).isEqualTo("전체");
        assertThat(roots.get(0).isRoot()).isTrue();
    }

    @Test
    @DisplayName("findBySessionIdAndParentNodeId: 세션+부모 조건으로 조회한다")
    void findBySessionIdAndParentNodeId_success() {
        // when
        List<Node> children = nodeRepository
                .findBySessionIdAndParentNodeId(1L, rootNode.getId());

        // then
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getName()).isEqualTo("개발팀");
    }

    @Test
    @DisplayName("save: 노드 저장 시 ID와 Auditing 필드가 자동 생성된다")
    void save_generatesId() {
        // given
        Node node = Node.builder()
                .sessionId(2L)
                .name("새 노드")
                .depth(0)
                .sortOrder(0)
                .build();

        // when
        Node saved = nodeRepository.save(node);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getUpdatedDate()).isNotNull();
    }
}
