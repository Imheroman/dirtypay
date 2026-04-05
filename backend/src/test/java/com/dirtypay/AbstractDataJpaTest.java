package com.dirtypay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Repository 레이어 통합 테스트 베이스 클래스.
 *
 * <p>실제 MariaDB 10.11 컨테이너를 사용하여 H2와의 방언 불일치를 방지한다.
 * 상속받은 클래스는 @DataJpaTest의 슬라이스 컨텍스트를 그대로 활용하면서
 * 실제 DB 환경에서 테스트할 수 있다.</p>
 *
 * <h3>사용법</h3>
 * <pre>{@code
 * class MyRepositoryTest extends AbstractDataJpaTest {
 *     @Autowired MyRepository repository;
 *
 *     @BeforeEach
 *     void setUp() {
 *         repository.deleteAll();
 *         flushAndClear();
 *     }
 * }
 * }</pre>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(TestcontainersConfiguration.class)
public abstract class AbstractDataJpaTest {

    @Autowired
    protected TestEntityManager entityManager;

    /**
     * JPA 1차 캐시를 비우고 DB에 변경사항을 반영한다.
     *
     * <p>테스트 데이터 셋업 후 호출하여 다음 조회가 DB에서 읽도록 보장한다.</p>
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
