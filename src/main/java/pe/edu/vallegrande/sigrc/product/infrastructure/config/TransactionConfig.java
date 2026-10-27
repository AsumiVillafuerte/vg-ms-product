package pe.edu.vallegrande.sigrc.product.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Habilita el manejo declarativo de transacciones reactivas.
 *
 * <p>Los casos de uso que persisten el producto/marca en la misma base de datos
 * (PostgreSQL via R2DBC) se anotan con {@code @Transactional}, de modo que los cambios
 * se confirmen o reviertan de forma atomica usando el
 * {@code R2dbcTransactionManager} autoconfigurado por Spring Boot.</p>
 */
@Configuration
@EnableTransactionManagement
@Profile("!mock")
public class TransactionConfig {
}
