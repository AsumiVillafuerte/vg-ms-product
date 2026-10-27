package pe.edu.vallegrande.sigrc.product.infrastructure.config;

import io.r2dbc.spi.Option;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ajustes de conexion R2DBC para PostgreSQL.
 *
 * <p>Se desactiva el cache de sentencias preparadas del driver
 * ({@code preparedStatementCacheQueries=0}) para que cada consulta use una sentencia
 * anonima. El driver r2dbc-postgresql nombra las sentencias preparadas con un contador
 * por conexion ({@code S_0}, {@code S_1}, {@code S_3}...) y PostgreSQL las conserva a
 * nivel de sesion. Cuando la base de datos esta detras de un pooler/proxy en modo
 * transaction (PgBouncer, Supavisor, pgcat, etc.) dos conexiones logicas comparten la
 * misma sesion fisica y el nombre generado colisiona, provocando el error
 * {@code [42P05] prepared statement "S_n" already exists}. El uso de sentencias anonimas
 * evita esa colision sin afectar la logica de negocio.</p>
 */
@Configuration
public class R2dbcConnectionConfig {

    private static final Option<Integer> PREPARED_STATEMENT_CACHE_QUERIES =
            Option.valueOf("preparedStatementCacheQueries");

    @Bean
    public ConnectionFactoryOptionsBuilderCustomizer preparedStatementCacheCustomizer() {
        return options -> options.option(PREPARED_STATEMENT_CACHE_QUERIES, 0);
    }
}
