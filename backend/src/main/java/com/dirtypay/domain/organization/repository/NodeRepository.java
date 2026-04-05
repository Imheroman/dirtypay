package com.dirtypay.domain.organization.repository;

import com.dirtypay.domain.organization.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 노드 리포지토리.
 *
 * <p>조직도 노드 엔티티에 대한 데이터 접근을 담당한다.
 * {@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface NodeRepository extends JpaRepository<Node, Long> {

    /**
     * 세션 ID로 전체 노드를 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 노드 목록
     */
    List<Node> findBySessionId(Long sessionId);

    /**
     * 부모 노드 ID로 자식 노드를 조회한다.
     *
     * @param parentNodeId 부모 노드 ID
     * @return 자식 노드 목록
     */
    List<Node> findByParentNodeId(Long parentNodeId);

    /**
     * 세션 ID와 부모 노드 ID로 자식 노드를 조회한다.
     *
     * @param sessionId    세션 ID
     * @param parentNodeId 부모 노드 ID
     * @return 자식 노드 목록
     */
    List<Node> findBySessionIdAndParentNodeId(Long sessionId, Long parentNodeId);

    /**
     * 세션 ID에서 루트 노드(parentNodeId가 null)를 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 루트 노드 목록
     */
    List<Node> findBySessionIdAndParentNodeIdIsNull(Long sessionId);

    /**
     * 세션 ID와 노드 이름으로 노드를 조회한다.
     *
     * @param sessionId 세션 ID
     * @param name      노드 이름
     * @return 노드 (존재하지 않으면 빈 Optional)
     */
    Optional<Node> findBySessionIdAndName(Long sessionId, String name);
}
