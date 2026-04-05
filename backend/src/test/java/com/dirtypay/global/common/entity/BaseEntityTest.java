package com.dirtypay.global.common.entity;

import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.repository.RoundGroupRepository;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BaseEntity} 단위 테스트.
 *
 * <p>JPA Auditing에 의한 createdDate/updatedDate 자동 설정,
 * Auto Increment ID 생성, Soft Delete(delete/isDeleted) 동작을 검증한다.
 * 추상 클래스이므로 구체 엔티티({@link RoundGroup})를 통해 간접 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class BaseEntityTest {

    @Autowired
    private RoundGroupRepository roundGroupRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("엔티티 저장 시 id가 Auto Increment로 생성된다")
    void save_generatesAutoIncrementId() {
        // given
        RoundGroup group = RoundGroup.builder()
                .roundId(1L)
                .name("테스트 그룹")
                .depth(0)
                .build();

        // when
        RoundGroup saved = roundGroupRepository.save(group);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("엔티티 저장 시 JPA Auditing에 의해 createdDate와 updatedDate가 자동 설정된다")
    void save_auditingFieldsSetAutomatically() {
        // given
        RoundGroup group = RoundGroup.builder()
                .roundId(1L)
                .name("감사 테스트 그룹")
                .depth(0)
                .build();

        // when
        RoundGroup saved = roundGroupRepository.save(group);
        entityManager.flush();
        entityManager.clear();

        Optional<RoundGroup> found = roundGroupRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCreatedDate()).isNotNull();
        assertThat(found.get().getUpdatedDate()).isNotNull();
    }

    @Test
    @DisplayName("delete() 호출 전 isDeleted()는 false이고 deletedDate는 null이다")
    void isDeleted_returnsFalseBeforeDelete() {
        // given
        RoundGroup group = RoundGroup.builder()
                .roundId(1L)
                .name("삭제 전 그룹")
                .depth(0)
                .build();

        // when & then
        assertThat(group.isDeleted()).isFalse();
        assertThat(group.getDeletedDate()).isNull();
    }

    @Test
    @DisplayName("delete() 호출 후 isDeleted()는 true이고 deletedDate가 설정된다")
    void delete_setsDeletedDateAndIsDeletedTrue() {
        // given
        RoundGroup group = RoundGroup.builder()
                .roundId(1L)
                .name("삭제 대상 그룹")
                .depth(0)
                .build();

        // when
        group.delete();

        // then
        assertThat(group.isDeleted()).isTrue();
        assertThat(group.getDeletedDate()).isNotNull();
    }
}
