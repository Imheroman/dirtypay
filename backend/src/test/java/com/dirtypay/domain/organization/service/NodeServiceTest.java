package com.dirtypay.domain.organization.service;

import com.dirtypay.domain.organization.dto.request.NodeCreateRequest;
import com.dirtypay.domain.organization.dto.request.NodeMoveRequest;
import com.dirtypay.domain.organization.dto.request.NodeUpdateRequest;
import com.dirtypay.domain.organization.dto.response.NodeResponse;
import com.dirtypay.domain.organization.dto.response.NodeTreeResponse;
import com.dirtypay.domain.organization.entity.Node;
import com.dirtypay.domain.organization.repository.NodeRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @InjectMocks
    private NodeService nodeService;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Nested
    @DisplayName("노드 생성 테스트")
    class CreateNodeTest {

        @Test
        @DisplayName("루트 노드 생성 성공 (parentNodeId=null, depth=0)")
        void createNode_rootNode_success() {
            // given
            Long sessionId = 1L;

            NodeCreateRequest request = new NodeCreateRequest();
            ReflectionTestUtils.setField(request, "name", "본부");
            ReflectionTestUtils.setField(request, "sortOrder", 0);

            Node savedNode = createNode(1L, sessionId, null, "본부", 0, 0);

            given(nodeRepository.save(any(Node.class))).willReturn(savedNode);

            // when
            NodeResponse response = nodeService.createNode(sessionId, request);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getSessionId()).isEqualTo(sessionId);
            assertThat(response.getParentNodeId()).isNull();
            assertThat(response.getName()).isEqualTo("본부");
            assertThat(response.getDepth()).isZero();
        }

        @Test
        @DisplayName("자식 노드 생성 성공 (depth=parent.depth+1)")
        void createNode_childNode_success() {
            // given
            Long sessionId = 1L;
            Long parentNodeId = 1L;
            Node parentNode = createNode(parentNodeId, sessionId, null, "본부", 0, 0);

            NodeCreateRequest request = new NodeCreateRequest();
            ReflectionTestUtils.setField(request, "parentNodeId", parentNodeId);
            ReflectionTestUtils.setField(request, "name", "개발팀");
            ReflectionTestUtils.setField(request, "sortOrder", 1);

            Node savedNode = createNode(2L, sessionId, parentNodeId, "개발팀", 1, 1);

            given(nodeRepository.findById(parentNodeId))
                    .willReturn(Optional.of(parentNode));
            given(nodeRepository.save(any(Node.class))).willReturn(savedNode);

            // when
            NodeResponse response = nodeService.createNode(sessionId, request);

            // then
            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getParentNodeId()).isEqualTo(parentNodeId);
            assertThat(response.getName()).isEqualTo("개발팀");
            assertThat(response.getDepth()).isEqualTo(1);
        }

        @Test
        @DisplayName("깊이 제한 초과 시 실패 (depth 4인 노드의 자식)")
        void createNode_depthExceeded_failure() {
            // given
            Long sessionId = 1L;
            Long parentNodeId = 10L;
            Node parentNode = createNode(parentNodeId, sessionId, null, "최하위", 4, 0);

            NodeCreateRequest request = new NodeCreateRequest();
            ReflectionTestUtils.setField(request, "parentNodeId", parentNodeId);
            ReflectionTestUtils.setField(request, "name", "초과 노드");

            given(nodeRepository.findById(parentNodeId))
                    .willReturn(Optional.of(parentNode));

            // when & then
            assertThatThrownBy(() -> nodeService.createNode(sessionId, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 parentNodeId 시 실패")
        void createNode_parentNotFound_failure() {
            // given
            Long sessionId = 1L;
            Long parentNodeId = 999L;

            NodeCreateRequest request = new NodeCreateRequest();
            ReflectionTestUtils.setField(request, "parentNodeId", parentNodeId);
            ReflectionTestUtils.setField(request, "name", "고아 노드");

            given(nodeRepository.findById(parentNodeId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> nodeService.createNode(sessionId, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("부모 노드가 다른 세션에 속한 경우 실패")
        void createNode_sessionMismatch_failure() {
            // given
            Long sessionId = 1L;
            Long otherSessionId = 2L;
            Long parentNodeId = 10L;
            Node parentNode = createNode(parentNodeId, otherSessionId, null, "다른 세션 노드", 0, 0);

            NodeCreateRequest request = new NodeCreateRequest();
            ReflectionTestUtils.setField(request, "parentNodeId", parentNodeId);
            ReflectionTestUtils.setField(request, "name", "자식 노드");

            given(nodeRepository.findById(parentNodeId))
                    .willReturn(Optional.of(parentNode));

            // when & then
            assertThatThrownBy(() -> nodeService.createNode(sessionId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("노드 트리 조회 테스트")
    class GetNodeTreeTest {

        @Test
        @DisplayName("빈 세션 트리 조회 시 빈 리스트 반환")
        void getNodeTree_emptySession_returnsEmptyList() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, 1L);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(nodeRepository.findBySessionId(sessionId))
                    .willReturn(List.of());

            // when
            List<NodeTreeResponse> result = nodeService.getNodeTree(sessionId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("트리 구조 정상 조회 (루트 → 자식 → 손자)")
        void getNodeTree_withHierarchy_success() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, 1L);
            Node root = createNode(1L, sessionId, null, "본부", 0, 0);
            Node child1 = createNode(2L, sessionId, 1L, "개발본부", 1, 0);
            Node child2 = createNode(3L, sessionId, 1L, "경영본부", 1, 1);
            Node grandChild = createNode(4L, sessionId, 2L, "백엔드팀", 2, 0);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(nodeRepository.findBySessionId(sessionId))
                    .willReturn(List.of(root, child1, child2, grandChild));

            // when
            List<NodeTreeResponse> result = nodeService.getNodeTree(sessionId);

            // then
            assertThat(result).hasSize(1); // 루트 1개
            NodeTreeResponse rootResponse = result.get(0);
            assertThat(rootResponse.getName()).isEqualTo("본부");
            assertThat(rootResponse.getChildren()).hasSize(2);
            assertThat(rootResponse.getChildren().get(0).getName()).isEqualTo("개발본부");
            assertThat(rootResponse.getChildren().get(1).getName()).isEqualTo("경영본부");
            assertThat(rootResponse.getChildren().get(0).getChildren()).hasSize(1);
            assertThat(rootResponse.getChildren().get(0).getChildren().get(0).getName()).isEqualTo("백엔드팀");
        }

        @Test
        @DisplayName("sortOrder 기준 정렬 확인")
        void getNodeTree_sortedBySortOrder() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, 1L);
            Node root1 = createNode(1L, sessionId, null, "경영본부", 0, 2);
            Node root2 = createNode(2L, sessionId, null, "개발본부", 0, 0);
            Node root3 = createNode(3L, sessionId, null, "영업본부", 0, 1);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(nodeRepository.findBySessionId(sessionId))
                    .willReturn(List.of(root1, root2, root3));

            // when
            List<NodeTreeResponse> result = nodeService.getNodeTree(sessionId);

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getName()).isEqualTo("개발본부"); // sortOrder 0
            assertThat(result.get(1).getName()).isEqualTo("영업본부"); // sortOrder 1
            assertThat(result.get(2).getName()).isEqualTo("경영본부"); // sortOrder 2
        }

        @Test
        @DisplayName("존재하지 않는 세션 조회 시 실패")
        void getNodeTree_sessionNotFound_failure() {
            // given
            Long sessionId = 999L;

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> nodeService.getNodeTree(sessionId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("노드 조회 테스트")
    class GetNodeTest {

        @Test
        @DisplayName("노드 조회 성공")
        void getNode_success() {
            // given
            Long nodeId = 1L;
            Node node = createNode(nodeId, 1L, null, "본부", 0, 0);

            given(nodeRepository.findById(nodeId))
                    .willReturn(Optional.of(node));

            // when
            NodeResponse response = nodeService.getNode(nodeId);

            // then
            assertThat(response.getId()).isEqualTo(nodeId);
            assertThat(response.getName()).isEqualTo("본부");
        }

        @Test
        @DisplayName("존재하지 않는 노드 조회 시 실패")
        void getNode_notFound_failure() {
            // given
            Long nodeId = 999L;

            given(nodeRepository.findById(nodeId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> nodeService.getNode(nodeId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("노드 수정 테스트")
    class UpdateNodeTest {

        @Test
        @DisplayName("수정 성공")
        void updateNode_success() {
            // given
            Long nodeId = 1L;
            Node node = createNode(nodeId, 1L, null, "원래 이름", 0, 0);

            NodeUpdateRequest request = new NodeUpdateRequest();
            ReflectionTestUtils.setField(request, "name", "새 이름");
            ReflectionTestUtils.setField(request, "sortOrder", 5);

            given(nodeRepository.findById(nodeId))
                    .willReturn(Optional.of(node));

            // when
            NodeResponse response = nodeService.updateNode(nodeId, request);

            // then
            assertThat(response.getName()).isEqualTo("새 이름");
            assertThat(response.getSortOrder()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("노드 삭제 테스트")
    class DeleteNodeTest {

        @Test
        @DisplayName("Leaf 노드 삭제 성공 (자식 없음)")
        void deleteNode_leafNode_success() {
            // given
            Long nodeId = 3L;
            Node node = createNode(nodeId, 1L, 1L, "팀원", 2, 0);

            given(nodeRepository.findById(nodeId))
                    .willReturn(Optional.of(node));
            given(nodeRepository.findByParentNodeId(nodeId))
                    .willReturn(List.of());

            // when
            nodeService.deleteNode(nodeId);

            // then
            assertThat(node.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("중간 노드 삭제 시 자식 승격")
        void deleteNode_promoteChildren_success() {
            // given
            Long parentId = 1L;  // depth 0, 루트
            Long targetId = 2L;  // depth 1, 삭제 대상
            Node target = createNode(targetId, 1L, parentId, "개발본부", 1, 0);

            Node child1 = createNode(3L, 1L, targetId, "백엔드팀", 2, 0);
            Node child2 = createNode(4L, 1L, targetId, "프론트팀", 2, 1);

            given(nodeRepository.findById(targetId))
                    .willReturn(Optional.of(target));
            given(nodeRepository.findByParentNodeId(targetId))
                    .willReturn(List.of(child1, child2));

            // when
            nodeService.deleteNode(targetId);

            // then
            assertThat(target.isDeleted()).isTrue();
            assertThat(child1.getParentNodeId()).isEqualTo(parentId);
            assertThat(child1.getDepth()).isEqualTo(1);
            assertThat(child2.getParentNodeId()).isEqualTo(parentId);
            assertThat(child2.getDepth()).isEqualTo(1);
        }

        @Test
        @DisplayName("중간 노드 삭제 시 depth 재계산 검증")
        void deleteNode_depthRecalculation_success() {
            // given
            Long targetId = 5L;  // depth 2, 삭제 대상
            Long grandParentId = 1L;  // depth 1
            Node target = createNode(targetId, 1L, grandParentId, "중간노드", 2, 0);

            Node child = createNode(6L, 1L, targetId, "하위노드", 3, 0);

            given(nodeRepository.findById(targetId))
                    .willReturn(Optional.of(target));
            given(nodeRepository.findByParentNodeId(targetId))
                    .willReturn(List.of(child));

            // when
            nodeService.deleteNode(targetId);

            // then
            assertThat(child.getParentNodeId()).isEqualTo(grandParentId);
            assertThat(child.getDepth()).isEqualTo(2); // 3 - 1 = 2
        }

        @Test
        @DisplayName("루트 노드 삭제 시 자식이 루트로 승격")
        void deleteNode_rootNode_childrenBecomeRoot() {
            // given
            Long rootId = 1L;
            Node rootNode = createNode(rootId, 1L, null, "본부", 0, 0);

            Node child1 = createNode(2L, 1L, rootId, "개발본부", 1, 0);
            Node child2 = createNode(3L, 1L, rootId, "경영본부", 1, 1);

            given(nodeRepository.findById(rootId))
                    .willReturn(Optional.of(rootNode));
            given(nodeRepository.findByParentNodeId(rootId))
                    .willReturn(List.of(child1, child2));

            // when
            nodeService.deleteNode(rootId);

            // then
            assertThat(rootNode.isDeleted()).isTrue();
            assertThat(child1.getParentNodeId()).isNull();
            assertThat(child1.getDepth()).isZero();
            assertThat(child2.getParentNodeId()).isNull();
            assertThat(child2.getDepth()).isZero();
        }
    }

    @Nested
    @DisplayName("노드 이동 테스트")
    class MoveNodeTest {

        @Test
        @DisplayName("노드 이동 성공 (다른 부모로 이동)")
        void moveNode_success() {
            // given
            Long sessionId = 1L;
            Node child1 = createNode(2L, sessionId, 1L, "개발본부", 1, 0);
            Node child2 = createNode(3L, sessionId, 1L, "경영본부", 1, 1);

            NodeMoveRequest request = new NodeMoveRequest();
            ReflectionTestUtils.setField(request, "targetParentNodeId", 3L);
            ReflectionTestUtils.setField(request, "sortOrder", 0);

            given(nodeRepository.findById(2L))
                    .willReturn(Optional.of(child1));
            given(nodeRepository.findById(3L))
                    .willReturn(Optional.of(child2));
            given(nodeRepository.findByParentNodeId(2L))
                    .willReturn(List.of());

            // when
            NodeResponse response = nodeService.moveNode(2L, request);

            // then
            assertThat(response.getParentNodeId()).isEqualTo(3L);
            assertThat(response.getDepth()).isEqualTo(2);
        }

        @Test
        @DisplayName("루트로 이동 성공 (targetParentNodeId=null)")
        void moveNode_toRoot_success() {
            // given
            Long sessionId = 1L;
            Node child = createNode(2L, sessionId, 1L, "개발본부", 1, 0);

            NodeMoveRequest request = new NodeMoveRequest();
            ReflectionTestUtils.setField(request, "targetParentNodeId", null);
            ReflectionTestUtils.setField(request, "sortOrder", 0);

            given(nodeRepository.findById(2L))
                    .willReturn(Optional.of(child));
            given(nodeRepository.findByParentNodeId(2L))
                    .willReturn(List.of());

            // when
            NodeResponse response = nodeService.moveNode(2L, request);

            // then
            assertThat(response.getParentNodeId()).isNull();
            assertThat(response.getDepth()).isZero();
        }

        @Test
        @DisplayName("자기 자신으로 이동 시 실패 (순환 참조)")
        void moveNode_selfReference_failure() {
            // given
            Long sessionId = 1L;
            Node node = createNode(1L, sessionId, null, "본부", 0, 0);

            NodeMoveRequest request = new NodeMoveRequest();
            ReflectionTestUtils.setField(request, "targetParentNodeId", 1L);
            ReflectionTestUtils.setField(request, "sortOrder", 0);

            given(nodeRepository.findById(1L))
                    .willReturn(Optional.of(node));

            // when & then
            assertThatThrownBy(() -> nodeService.moveNode(1L, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("하위 노드로 이동 시 실패 (순환 참조)")
        void moveNode_descendantReference_failure() {
            // given
            Long sessionId = 1L;
            Node root = createNode(1L, sessionId, null, "본부", 0, 0);
            Node child = createNode(2L, sessionId, 1L, "개발본부", 1, 0);
            Node grandChild = createNode(3L, sessionId, 2L, "백엔드팀", 2, 0);

            NodeMoveRequest request = new NodeMoveRequest();
            ReflectionTestUtils.setField(request, "targetParentNodeId", 3L);
            ReflectionTestUtils.setField(request, "sortOrder", 0);

            given(nodeRepository.findById(1L))
                    .willReturn(Optional.of(root));
            given(nodeRepository.findById(3L))
                    .willReturn(Optional.of(grandChild));
            given(nodeRepository.findByParentNodeId(1L))
                    .willReturn(List.of(child));
            given(nodeRepository.findByParentNodeId(2L))
                    .willReturn(List.of(grandChild));

            // when & then
            assertThatThrownBy(() -> nodeService.moveNode(1L, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("이동 시 하위 노드 depth 갱신")
        void moveNode_updateDescendantsDepth() {
            // given
            Long sessionId = 1L;
            Node child = createNode(2L, sessionId, 1L, "개발본부", 1, 0);
            Node grandChild = createNode(3L, sessionId, 2L, "백엔드팀", 2, 0);

            // child(depth 1)를 루트(depth 0)로 이동 → depthDiff = -1
            NodeMoveRequest request = new NodeMoveRequest();
            ReflectionTestUtils.setField(request, "targetParentNodeId", null);
            ReflectionTestUtils.setField(request, "sortOrder", 1);

            given(nodeRepository.findById(2L))
                    .willReturn(Optional.of(child));
            given(nodeRepository.findByParentNodeId(2L))
                    .willReturn(List.of(grandChild));
            given(nodeRepository.findByParentNodeId(3L))
                    .willReturn(List.of());

            // when
            nodeService.moveNode(2L, request);

            // then
            assertThat(child.getDepth()).isZero(); // 1 → 0
            assertThat(grandChild.getDepth()).isEqualTo(1); // 2 → 1
        }
    }

    @Nested
    @DisplayName("시스템 노드 보호 테스트")
    class SystemNodeGuardTest {

        @Test
        @DisplayName("시스템 노드 수정 시 예외 발생")
        void updateNode_systemNode_failure() {
            // given
            Long nodeId = 1L;
            Node systemNode = createNode(nodeId, 1L, null, "전체", 0, 0, true);

            NodeUpdateRequest request = new NodeUpdateRequest();
            ReflectionTestUtils.setField(request, "name", "변경된 이름");
            ReflectionTestUtils.setField(request, "sortOrder", 1);

            given(nodeRepository.findById(nodeId))
                    .willReturn(Optional.of(systemNode));

            // when & then
            assertThatThrownBy(() -> nodeService.updateNode(nodeId, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("시스템 노드 삭제 시 예외 발생")
        void deleteNode_systemNode_failure() {
            // given
            Long nodeId = 1L;
            Node systemNode = createNode(nodeId, 1L, null, "전체", 0, 0, true);

            given(nodeRepository.findById(nodeId))
                    .willReturn(Optional.of(systemNode));

            // when & then
            assertThatThrownBy(() -> nodeService.deleteNode(nodeId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("시스템 노드 이동 시 예외 발생")
        void moveNode_systemNode_failure() {
            // given
            Long nodeId = 1L;
            Node systemNode = createNode(nodeId, 1L, null, "전체", 0, 0, true);

            NodeMoveRequest request = new NodeMoveRequest();
            ReflectionTestUtils.setField(request, "targetParentNodeId", 2L);
            ReflectionTestUtils.setField(request, "sortOrder", 0);

            given(nodeRepository.findById(nodeId))
                    .willReturn(Optional.of(systemNode));

            // when & then
            assertThatThrownBy(() -> nodeService.moveNode(nodeId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    private Node createNode(Long id, Long sessionId, Long parentNodeId, String name, int depth, int sortOrder) {
        return createNode(id, sessionId, parentNodeId, name, depth, sortOrder, false);
    }

    private Node createNode(Long id, Long sessionId, Long parentNodeId, String name, int depth, int sortOrder, boolean isSystem) {
        Node node = Node.builder()
                .sessionId(sessionId)
                .parentNodeId(parentNodeId)
                .name(name)
                .depth(depth)
                .sortOrder(sortOrder)
                .isSystem(isSystem)
                .build();
        ReflectionTestUtils.setField(node, "id", id);
        return node;
    }

    private Session createSession(Long id, Long ownerId) {
        Session session = Session.builder()
                .title("테스트 세션")
                .ownerId(ownerId)
                .build();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
