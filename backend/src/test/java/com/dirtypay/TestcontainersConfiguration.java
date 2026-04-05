package com.dirtypay;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트 컨테이너 공유 설정.
 *
 * <p>@ServiceConnection을 통해 MariaDB 컨테이너의 접속 정보를 Spring DataSource에 자동 주입한다.
 * static 컨테이너를 Bean으로 등록하여 같은 Spring 컨텍스트 내에서 재사용한다.</p>
 *
 * <p>사용법: @Import(TestcontainersConfiguration.class) 를 테스트 클래스에 추가</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * MariaDB 10.11 테스트 컨테이너.
     *
     * <p>@ServiceConnection이 자동으로 Spring DataSource 설정을 오버라이드한다.
     * application.yml의 datasource 설정은 이 Bean으로 대체된다.</p>
     *
     * @return MariaDB 컨테이너
     */
    @Bean
    @ServiceConnection
    MariaDBContainer<?> mariadbContainer() {
        return new MariaDBContainer<>("mariadb:10.11");
    }

    /**
     * Redis 7.2 테스트 컨테이너.
     *
     * <p>@ServiceConnection이 자동으로 Spring Data Redis(Lettuce) 연결 설정을 오버라이드한다.
     * JWT 블랙리스트 통합 테스트에서 실제 Redis 환경을 사용한다.</p>
     *
     * @return Redis 컨테이너
     */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                .withExposedPorts(6379);
    }

    /**
     * 테스트용 Redisson 클라이언트 Bean.
     *
     * <p>{@link #redisContainer()}가 할당한 랜덤 포트를 사용하여 Redisson을 초기화한다.
     * 프로덕션 {@link com.dirtypay.global.config.RedisConfig#redissonClient()}를 오버라이드한다.</p>
     *
     * @param redisContainer 실행 중인 Redis Testcontainer
     * @return 테스트용 RedissonClient 인스턴스
     */
    @Bean(destroyMethod = "shutdown")
    RedissonClient redissonClient(GenericContainer<?> redisContainer) {
        String host = redisContainer.getHost();
        int port = redisContainer.getMappedPort(6379);
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setConnectionPoolSize(5)
                .setConnectionMinimumIdleSize(1);
        return Redisson.create(config);
    }
}
