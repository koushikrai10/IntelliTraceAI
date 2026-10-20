package com.intellitrace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("IntelliTraceApplication Context Loading Integration Test")
class IntelliTraceApplicationTests {

    @Test
    @DisplayName("Application context should load successfully with all components and configurations")
    void contextLoads() {
        // Verifies Spring ApplicationContext startup
    }
}
