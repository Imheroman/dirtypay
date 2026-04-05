package com.dirtypay.domain.organization.service;

import com.dirtypay.domain.organization.dto.request.NodeCreateRequest;
import com.dirtypay.domain.organization.dto.request.NodeMoveRequest;
import com.dirtypay.domain.organization.dto.request.NodeUpdateRequest;
import com.dirtypay.domain.organization.dto.response.NodeResponse;
import com.dirtypay.domain.organization.dto.response.NodeTreeResponse;
import com.dirtypay.domain.organization.entity.Node;
import com.dirtypay.domain.organization.repository.NodeRepository;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 노드 서비스.
 *
 * <p>조직도 노드의 생성, 조회, 수정, 삭제(Soft Delete) 비즈니스 로직을 처리한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NodeService {

    private final NodeRepository nodeRepository;
    private final SessionRepository sessionRepository;

    /**
     * 새로운 노드를 생성한다.
     *
     * <p>parentNodeId가 null이면 루트 노드(depth=0)로 생성되고,
     * 값이 있으면 부모 노드의 depth+1로 생성된다.</p>
     *
     * @param sessionId 세션 ID
     * @param request   노드 생성 요청
     * @return 생성된 노드 응답
     */
    @Transactional
    public NodeResponse createNode(Long sessionId, NodeCreateRequest request) {
        int depth = 0;

        if (request.getParentNodeId() != null) {
            Node parentNode = this.findNodeById(request.getParentNodeId());

            if (!parentNode.getSessionId().equals(sessionId)) {
                throw new BusinessException(ErrorCode.NODE_SESSION_MISMATCH);
            }

            depth = parentNode.getDepth() + 1;

            if (depth > 4) {
                throw new BusinessException(ErrorCode.NODE_DEPTH_EXCEEDED);
            }
        }

        Node node = Node.builder()
                .sessionId(sessionId)
                .parentNodeId(request.getParentNodeId())
                .name(request.getName())
                .depth(depth)
                .sortOrder(request.getSortOrder())
                .build();

        Node saved = this.nodeRepository.save(node);
        return NodeResponse.from(saved);
    }

    /**
     * 세션의 노드 트리를 조회한다.
     *
     * <p>세션에 속한 전체 노드를 트리 구조로 반환하며,
     * 각 노드에 소속 멤버 목록을 포함한다.
     * 노드는 sortOrder 기준으로 정렬된다.</p>
     *
     * @param sessionId 세션 ID
     * @return 노드 트리 목록 (루트 노드 리스트)
     */
    public List<NodeTreeResponse> getNodeTree(Long sessionId) {
        this.findSessionById(sessionId);

        // 세션의 전체 노드 조회
        List<Node> nodes = this.nodeRepository.findBySessionId(sessionId);

        if (nodes.isEmpty()) {
            return List.of();
        }

        // Node → NodeTreeResponse 변환 및 ID 기준 맵 생성
        Map<Long, NodeTreeResponse> nodeMap = new LinkedHashMap<>();
        for (Node node : nodes) {
            NodeTreeResponse treeNode = NodeTreeResponse.from(node);
            nodeMap.put(node.getId(), treeNode);
        }

        // 트리 구조 빌드 (flat → tree)
        List<NodeTreeResponse> roots = new ArrayList<>();
        for (NodeTreeResponse treeNode : nodeMap.values()) {
            if (treeNode.getParentNodeId() == null) {
                roots.add(treeNode);
            } else {
                NodeTreeResponse parent = nodeMap.get(treeNode.getParentNodeId());
                if (parent != null) {
                    parent.getChildren().add(treeNode);
                } else {
                    roots.add(treeNode);
                }
            }
        }

        // sortOrder 기준 정렬 (재귀)
        sortTreeBySortOrder(roots);

        return roots;
    }

    /**
     * 노드 상세 정보를 조회한다.
     *
     * @param nodeId 노드 ID
     * @return 노드 응답
     */
    public NodeResponse getNode(Long nodeId) {
        Node node = this.findNodeById(nodeId);
        return NodeResponse.from(node);
    }

    /**
     * 노드 정보를 수정한다.
     *
     * @param nodeId  노드 ID
     * @param request 수정 요청
     * @return 수정된 노드 응답
     */
    @Transactional
    public NodeResponse updateNode(Long nodeId, NodeUpdateRequest request) {
        Node node = this.findNodeById(nodeId);

        if (node.isSystem()) {
            throw new BusinessException(ErrorCode.NODE_SYSTEM_NOT_MODIFIABLE);
        }

        node.update(request.getName(), request.getSortOrder());

        return NodeResponse.from(node);
    }

    /**
     * 노드를 삭제한다. (Soft Delete)
     *
     * <p>삭제 시 다음 정책이 적용된다:</p>
     * <ol>
     *   <li>자식 노드를 삭제 대상의 부모로 승격 (parentNodeId, depth 변경)</li>
     *   <li>소속 멤버를 삭제 대상의 부모로 이동 (nodeId 변경)</li>
     *   <li>대상 노드를 Soft Delete</li>
     * </ol>
     *
     * @param nodeId 노드 ID
     */
    @Transactional
    public void deleteNode(Long nodeId) {
        Node node = this.findNodeById(nodeId);

        if (node.isSystem()) {
            throw new BusinessException(ErrorCode.NODE_SYSTEM_NOT_DELETABLE);
        }

        this.promoteChildNodes(node);

        node.delete();
    }

    /**
     * 노드를 다른 부모 노드 아래로 이동한다.
     *
     * <p>순환 참조를 방지하기 위해 이동 대상이 자기 자신의 하위 노드인지 검증한다.
     * 이동 후 depth는 새 부모의 depth + 1로 재계산되며,
     * 하위 노드의 depth도 재귀적으로 갱신한다.</p>
     *
     * @param nodeId  이동할 노드 ID
     * @param request 이동 요청 (targetParentNodeId, sortOrder)
     * @return 이동 후 노드 응답
     */
    @Transactional
    public NodeResponse moveNode(Long nodeId, NodeMoveRequest request) {
        Node node = this.findNodeById(nodeId);

        if (node.isSystem()) {
            throw new BusinessException(ErrorCode.NODE_SYSTEM_NOT_MODIFIABLE);
        }

        Long targetParentNodeId = request.getTargetParentNodeId();
        int newDepth = 0;

        if (targetParentNodeId != null) {
            // 자기 자신으로 이동 방지
            if (targetParentNodeId.equals(nodeId)) {
                throw new BusinessException(ErrorCode.NODE_CIRCULAR_REFERENCE);
            }

            Node targetParent = this.findNodeById(targetParentNodeId);

            // 같은 세션인지 확인
            if (!targetParent.getSessionId().equals(node.getSessionId())) {
                throw new BusinessException(ErrorCode.NODE_SESSION_MISMATCH);
            }

            // 순환 참조 검증: targetParent가 node의 하위 노드인지 확인
            this.validateNotDescendant(nodeId, targetParentNodeId);

            newDepth = targetParent.getDepth() + 1;
        }

        int depthDiff = newDepth - node.getDepth();
        node.move(targetParentNodeId, newDepth);
        node.update(node.getName(), request.getSortOrder());

        // 하위 노드 depth 재귀 갱신
        if (depthDiff != 0) {
            this.updateDescendantsDepth(node.getId(), depthDiff);
        }

        return NodeResponse.from(node);
    }

    /**
     * 삭제 대상 노드의 자식 노드를 부모로 승격한다.
     *
     * <p>자식 노드의 parentNodeId를 삭제 대상의 parentNodeId로 변경하고,
     * depth를 1 감소시킨다. 루트 노드 삭제 시 자식은 새로운 루트(depth=0)가 된다.</p>
     *
     * @param node 삭제 대상 노드
     */
    private void promoteChildNodes(Node node) {
        List<Node> children = this.nodeRepository
                .findByParentNodeId(node.getId());

        for (Node child : children) {
            child.move(node.getParentNodeId(), child.getDepth() - 1);
        }
    }

    /**
     * targetParentNodeId가 nodeId의 하위 노드인지 검증한다.
     *
     * <p>targetParentNodeId부터 상위로 올라가면서 nodeId와 일치하는지 확인한다.
     * 최대 depth가 4이므로 최대 4번의 조회로 충분하다.</p>
     *
     * @param nodeId             이동 대상 노드 ID
     * @param targetParentNodeId 새 부모 노드 ID
     */
    private void validateNotDescendant(Long nodeId, Long targetParentNodeId) {
        List<Node> descendants = this.nodeRepository
                .findByParentNodeId(nodeId);

        for (Node child : descendants) {
            if (child.getId().equals(targetParentNodeId)) {
                throw new BusinessException(ErrorCode.NODE_CIRCULAR_REFERENCE);
            }
            validateNotDescendant(child.getId(), targetParentNodeId);
        }
    }

    /**
     * 하위 노드의 depth를 재귀적으로 갱신한다.
     *
     * @param parentNodeId 부모 노드 ID
     * @param depthDiff    depth 변화량
     */
    private void updateDescendantsDepth(Long parentNodeId, int depthDiff) {
        List<Node> children = this.nodeRepository
                .findByParentNodeId(parentNodeId);

        for (Node child : children) {
            child.move(child.getParentNodeId(), child.getDepth() + depthDiff);
            updateDescendantsDepth(child.getId(), depthDiff);
        }
    }

    /**
     * 트리 노드 목록을 sortOrder 기준으로 재귀 정렬한다.
     *
     * @param nodes 정렬할 노드 목록
     */
    private void sortTreeBySortOrder(List<NodeTreeResponse> nodes) {
        nodes.sort(Comparator.comparingInt(NodeTreeResponse::getSortOrder));
        for (NodeTreeResponse node : nodes) {
            if (!node.getChildren().isEmpty()) {
                sortTreeBySortOrder(node.getChildren());
            }
        }
    }

    private Node findNodeById(Long nodeId) {
        return this.nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.NODE_NOT_FOUND));
    }

    private void findSessionById(Long sessionId) {
        this.sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SESSION_NOT_FOUND));
    }
}
