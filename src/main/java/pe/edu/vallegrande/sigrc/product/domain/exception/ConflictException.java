package pe.edu.vallegrande.sigrc.product.domain.exception;

/**
 * Indica un conflicto con datos existentes (por ejemplo, crear una marca
 * con un nombre ya registrado en la misma organizacion).
 * Se traduce a HTTP 409 Conflict en {@code GlobalExceptionHandler}.
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}