package com.designmd.designapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.bootstrap-admin.enabled=false")
class DesignApiApplicationTests {

	@Test
	void contextLoads() {
	}

}

