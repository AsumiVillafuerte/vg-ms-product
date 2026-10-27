package pe.edu.vallegrande.sigrc.product;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.r2dbc.pool.initial-size=0")
@ActiveProfiles("test")
class ProductApplicationTests {

	@Test
	void contextLoads() {
	}

}
