package pe.edu.vallegrande.sigrc.product.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomainExceptionsTest {

    @Test
    void domainExceptionStoresMessageAndCause() {
        Throwable cause = new IllegalStateException("root cause");

        DomainException exception = new DomainException("error de dominio", cause);

        assertEquals("error de dominio", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void conflictExceptionStoresMessageAndCause() {
        Throwable cause = new IllegalStateException("root cause");

        ConflictException exception = new ConflictException("conflicto", cause);

        assertEquals("conflicto", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void unauthorizedExceptionStoresMessageAndCause() {
        Throwable cause = new IllegalStateException("root cause");

        UnauthorizedException exception = new UnauthorizedException("no autorizado", cause);

        assertEquals("no autorizado", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void simpleConstructorsStoreMessage() {
        assertEquals("domain", new DomainException("domain").getMessage());
        assertEquals("conflict", new ConflictException("conflict").getMessage());
        assertEquals("unauthorized", new UnauthorizedException("unauthorized").getMessage());
        assertEquals("not found", new NotFoundException("not found").getMessage());
    }
}
