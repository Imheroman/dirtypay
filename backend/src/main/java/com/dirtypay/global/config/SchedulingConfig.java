package com.dirtypay.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Scheduling 활성화 설정 클래스.
 *
 * <p>{@code @Scheduled} 어노테이션 기반 스케줄링을 활성화한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
