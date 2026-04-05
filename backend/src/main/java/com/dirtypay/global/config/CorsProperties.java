package com.dirtypay.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CORS 설정 프로퍼티.
 *
 * <p>application.yml의 cors 설정을 바인딩한다.
 * 프로필별로 허용 오리진을 외부에서 주입할 수 있어
 * 하드코딩 없이 로컬/운영 환경을 구분할 수 있다.</p>
 *
 * <pre>
 * cors:
 *   allowed-origins:
 *     - http://localhost:3000
 *     - http://127.0.0.1:3000
 * </pre>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * CORS 허용 오리진 목록.
     *
     * <p>로컬 개발 환경에서는 localhost:3000, 운영 환경에서는
     * 실제 프론트엔드 도메인을 설정한다.</p>
     */
    private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
    );
}
