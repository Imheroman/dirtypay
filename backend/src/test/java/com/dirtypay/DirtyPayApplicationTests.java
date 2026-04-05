package com.dirtypay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
class DirtyPayApplicationTests {

    @Test
    void contextLoads() {
    }
}
