package com.dirtypay.domain.member.repository;

import com.dirtypay.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 회원 리포지토리.
 *
 * <p>회원 엔티티에 대한 데이터 접근을 담당한다.
 * {@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일로 회원을 조회한다.
     *
     * @param email 조회할 이메일
     * @return 회원 Optional
     */
    Optional<Member> findByEmail(String email);

    /**
     * 이메일 존재 여부를 확인한다.
     *
     * @param email 확인할 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 이메일 또는 이름으로 회원을 검색한다.
     *
     * <p>접두어(prefix) 매칭 방식을 사용하여 인덱스를 활용한 효율적인 검색을 수행한다.
     * Leading Wildcard({@code %keyword%}) 방식은 DB 인덱스를 무력화하여 Full Table Scan을
     * 유발하므로, 접두어 매칭({@code keyword%})으로 변경하였다. {@code email} 컬럼은
     * UNIQUE 인덱스를, {@code name} 컬럼은 일반 인덱스를 활용할 수 있다.</p>
     *
     * @param keyword  검색 키워드 (접두어 매칭)
     * @param pageable 페이징 정보
     * @return 검색된 회원 Page
     */
    @Query("SELECT m FROM Member m WHERE m.email LIKE :keyword% OR m.name LIKE :keyword%")
    Page<Member> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
