package pe.edu.vallegrande.sigrc.product.infrastructure.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.response.ErrorResponse;
import pe.edu.vallegrande.sigrc.product.domain.exception.ConflictException;
import pe.edu.vallegrande.sigrc.product.domain.exception.DomainException;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.exception.UnauthorizedException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica la traduccion de conflictos de datos a HTTP 409 en
 * {@link GlobalExceptionHandler}: duplicados detectados en el dominio
 * ({@link ConflictException}) y violaciones UNIQUE de la base de datos
 * en condiciones de carrera ({@link DuplicateKeyException}).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private Logger handlerLogger;
    private Level previousLevel;

    @BeforeEach
    void muteExpectedErrorLog() {
        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        previousLevel = handlerLogger.getLevel();
        handlerLogger.setLevel(Level.OFF);
    }

    @AfterEach
    void restoreLogLevel() {
        handlerLogger.setLevel(previousLevel);
    }

    @Test
    void conflictExceptionMapsTo409() {
        String message = "Ya existe una marca con el nombre 'Samsung' en su organizacion";
        ResponseEntity<ErrorResponse> response = handler.handleConflictException(new ConflictException(message));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals(message, response.getBody().getMessage());
    }

    @Test
    void duplicateKeyExceptionMapsTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateKeyException(
                new DuplicateKeyException("duplicate key value violates unique constraint"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Ya existe un registro con los mismos datos", response.getBody().getMessage());
    }

    @Test
    void notFoundExceptionMapsTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFoundException(new NotFoundException("Producto no encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Producto no encontrado", response.getBody().getMessage());
        assertEquals(List.of("Producto no encontrado"), response.getBody().getErrors());
    }

    @Test
    void domainExceptionMapsTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDomainException(new DomainException("El nombre no puede estar en blanco"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("El nombre no puede estar en blanco", response.getBody().getMessage());
    }

    @Test
    void unauthorizedExceptionMapsTo403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnauthorizedException(new UnauthorizedException("Sin organizacion"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Sin organizacion", response.getBody().getMessage());
    }

    @Test
    void validationExceptionMapsFieldAndGlobalErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "createProductRequest");
        bindingResult.addError(new FieldError(
                "createProductRequest", "commercialName", "El nombre comercial es obligatorio"));
        bindingResult.addError(new ObjectError("createProductRequest", "error global"));
        MethodParameter parameter = new MethodParameter(String.class.getConstructor(String.class), 0);
        WebExchangeBindException ex = new WebExchangeBindException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Error de validación", response.getBody().getMessage());
        assertTrue(response.getBody().getErrors()
                .contains("commercialName: El nombre comercial es obligatorio"));
        assertTrue(response.getBody().getErrors().contains("error global"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void constraintViolationMapsTo400() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("getProducts.status");
        when(violation.getMessage()).thenReturn("no debe estar vacio");
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);
        ConstraintViolationException ex = new ConstraintViolationException(violations);

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Error de validación de parámetros", response.getBody().getMessage());
        assertEquals(List.of("getProducts.status: no debe estar vacio"), response.getBody().getErrors());
    }

    @Test
    void serverWebInputExceptionMapsTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleServerWebInput(new ServerWebInputException("Required body missing"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Parámetro de solicitud inválido o faltante", response.getBody().getMessage());
    }

    @Test
    void genericExceptionMapsTo500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Error interno del servidor", response.getBody().getMessage());
    }
}