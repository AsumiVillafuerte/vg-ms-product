package pe.edu.vallegrande.sigrc.product.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.vallegrande.sigrc.product.domain.exception.UnauthorizedException;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;

/**
 * Verifica como {@link CurrentUserProvider} extrae {@code userId} y el claim de
 * organizacion del JWT real de Keycloak. El id de negocio es el claim {@code userId}
 * (no {@code sub}, que es el id interno de Keycloak, ni {@code preferred_username}).
 */
class CurrentUserProviderTest {

    private CurrentUserProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CurrentUserProvider();
        ReflectionTestUtils.setField(provider, "organizationClaim", CurrentUserProvider.DEFAULT_ORGANIZATION_CLAIM);
        ReflectionTestUtils.setField(provider, "defaultOrganizationId", "default");
        ReflectionTestUtils.setField(provider, "defaultUserId", "system");
    }

    private Context authenticatedContext(Jwt jwt) {
        return ReactiveSecurityContextHolder.withAuthentication(new JwtAuthenticationToken(jwt));
    }

    private Jwt.Builder baseToken() {
        return Jwt.withTokenValue("token").header("alg", "none");
    }

    @Test
    void shouldUseUserIdClaimAsBusinessUser() {
        Jwt jwt = baseToken()
                .subject("keycloak-sub")
                .claim("userId", "app-user-1")
                .claim("preferred_username", "jperez")
                .claim("organizationId", "org-1")
                .build();

        StepVerifier.create(provider.getUserId().contextWrite(authenticatedContext(jwt)))
                .expectNext("app-user-1")
                .verifyComplete();
    }

    @Test
    void shouldFallBackToSubjectWhenUserIdClaimIsAbsent() {
        Jwt jwt = baseToken()
                .subject("keycloak-sub")
                .claim("organizationId", "org-1")
                .build();

        StepVerifier.create(provider.getUserId().contextWrite(authenticatedContext(jwt)))
                .expectNext("keycloak-sub")
                .verifyComplete();
    }

    @Test
    void shouldNeverUsePreferredUsernameAsUserId() {
        Jwt jwt = baseToken()
                .claim("preferred_username", "jperez")
                .claim("organizationId", "org-1")
                .build();

        StepVerifier.create(provider.getUserId().contextWrite(authenticatedContext(jwt)))
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void shouldReadOrganizationIdFromCamelCaseClaim() {
        Jwt jwt = baseToken()
                .subject("kc-sub")
                .claim("userId", "u1")
                .claim("organizationId", "org-camel")
                .build();

        StepVerifier.create(provider.getOrganizationId().contextWrite(authenticatedContext(jwt)))
                .expectNext("org-camel")
                .verifyComplete();
    }

    @Test
    void shouldFallBackToSnakeCaseOrganizationId() {
        Jwt jwt = baseToken()
                .subject("kc-sub")
                .claim("userId", "u1")
                .claim("organization_id", "org-snake")
                .build();

        StepVerifier.create(provider.getOrganizationId().contextWrite(authenticatedContext(jwt)))
                .expectNext("org-snake")
                .verifyComplete();
    }

    @Test
    void shouldReadOrganizationFromNestedMapClaim() {
        Jwt jwt = baseToken()
                .subject("kc-sub")
                .claim("userId", "u1")
                .claim("org", Map.of("id", "org-map", "name", "Valle Grande"))
                .build();

        StepVerifier.create(provider.getOrganizationId().contextWrite(authenticatedContext(jwt)))
                .expectNext("org-map")
                .verifyComplete();
    }

    @Test
    void shouldFallBackToOrganizationIdKeyInsideMapClaim() {
        Jwt jwt = baseToken()
                .subject("kc-sub")
                .claim("userId", "u1")
                .claim("org", Map.of("organizationId", "org-map-alt"))
                .build();

        StepVerifier.create(provider.getOrganizationId().contextWrite(authenticatedContext(jwt)))
                .expectNext("org-map-alt")
                .verifyComplete();
    }

    @Test
    void shouldStringifyScalarOrganizationClaim() {
        Jwt jwt = baseToken()
                .subject("kc-sub")
                .claim("userId", "u1")
                .claim("organizationId", 12345)
                .build();

        StepVerifier.create(provider.getOrganizationId().contextWrite(authenticatedContext(jwt)))
                .expectNext("12345")
                .verifyComplete();
    }

    @Test
    void shouldReadOrganizationFromOrganizationsList() {
        Jwt jwt = baseToken()
                .subject("kc-sub")
                .claim("userId", "u1")
                .claim("organizations", List.of(Map.of("id", "org-list")))
                .build();

        StepVerifier.create(provider.getOrganizationId().contextWrite(authenticatedContext(jwt)))
                .expectNext("org-list")
                .verifyComplete();
    }

    @Test
    void shouldFailClosedWhenOrganizationClaimIsMissing() {
        Jwt jwt = baseToken()
                .subject("kc-sub")
                .claim("userId", "u1")
                .build();

        StepVerifier.create(provider.getOrganizationId().contextWrite(authenticatedContext(jwt)))
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void shouldNotTrustUnsupportedOrganizationClaim() {
        Jwt jwt = baseToken()
                .subject("kc-sub")
                .claim("userId", "u1")
                .claim("organization", "unsupported-org")
                .build();

        StepVerifier.create(provider.getOrganizationId().contextWrite(authenticatedContext(jwt)))
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void shouldUseDefaultOrganizationWhenNoAuthenticatedJwt() {
        StepVerifier.create(provider.getOrganizationId())
                .expectNext("default")
                .verifyComplete();
    }

    @Test
    void shouldUseDefaultUserWhenNoAuthenticatedJwt() {
        StepVerifier.create(provider.getUserId())
                .expectNext("system")
                .verifyComplete();
    }
}
