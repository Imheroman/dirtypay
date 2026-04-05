package com.dirtypay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * DirtyPay 애플리케이션 진입점.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DirtyPayApplication {

    /**
     * 애플리케이션 시작점.
     *
     * @param args 커맨드라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(DirtyPayApplication.class, args);
    }
}
