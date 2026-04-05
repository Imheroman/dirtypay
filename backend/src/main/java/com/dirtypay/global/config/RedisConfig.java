package com.dirtypay.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 설정.
 *
 * <p>{@link StringRedisTemplate} Bean을 등록하여 JWT 블랙리스트 서비스에서
 * 문자열 기반 키-값 조작을 수행할 수 있도록 한다.</p>
 *
 * <p>Lettuce 커넥션 풀 설정은 {@code application.yml}의
 * {@code spring.data.redis.lettuce.pool}에서 관리한다.</p>
 *
 * <p>{@link RedissonClient} Bean은 분산 락 전용으로, Lettuce(StringRedisTemplate)와 별도로 공존한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${redisson.connection-pool-size:10}")
    private int connectionPoolSize;

    @Value("${redisson.connection-minimum-idle-size:2}")
    private int connectionMinimumIdleSize;

    /**
     * JWT 블랙리스트 저장에 사용할 {@link StringRedisTemplate} Bean을 등록한다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return StringRedisTemplate 인스턴스
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Redisson 클라이언트 Bean. 분산 락 전용.
     * Lettuce(StringRedisTemplate)와 별도로 공존한다.
     *
     * <p>테스트 환경에서 {@link TestcontainersConfiguration}이 Bean을 먼저 등록한 경우 건너뛴다.</p>
     *
     * @return RedissonClient 인스턴스
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize);
        return Redisson.create(config);
    }
}
