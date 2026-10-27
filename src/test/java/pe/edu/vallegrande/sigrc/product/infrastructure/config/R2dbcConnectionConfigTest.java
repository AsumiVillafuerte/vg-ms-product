package pe.edu.vallegrande.sigrc.product.infrastructure.config;

import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class R2dbcConnectionConfigTest {

    @Test
    void disablesPreparedStatementCacheForPostgres() {
        ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.HOST, "localhost");

        new R2dbcConnectionConfig().preparedStatementCacheCustomizer().customize(builder);

        ConnectionFactoryOptions options = builder.build();
        assertTrue(options.hasOption(PostgresqlConnectionFactoryProvider.PREPARED_STATEMENT_CACHE_QUERIES));
        assertEquals(0, ((Number) options.getValue(PostgresqlConnectionFactoryProvider.PREPARED_STATEMENT_CACHE_QUERIES)).intValue());
    }
}
