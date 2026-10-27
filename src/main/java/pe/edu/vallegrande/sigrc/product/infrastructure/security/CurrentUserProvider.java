package pe.edu.vallegrande.sigrc.product.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.domain.exception.UnauthorizedException;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class CurrentUserProvider implements ICurrentUserProvider {

    static final String DEFAULT_ORGANIZATION_CLAIM = "organizationId";

    private static final String USER_ID_CLAIM = "userId";

    private static final String[] ORGANIZATION_CLAIM_FALLBACKS = {
            "organizationId", "organization_id", "org", "organizations"
    };

    @Value("${app.security.organization-claim:" + DEFAULT_ORGANIZATION_CLAIM + "}")
    private String organizationClaim;

    @Value("${app.security.default-organization-id:default}")
    private String defaultOrganizationId;

    @Value("${app.security.default-user-id:system}")
    private String defaultUserId;

    @Override
    public Mono<String> getOrganizationId() {
        return jwt()
                .flatMap(token -> {
                    String organizationId = readOrganizationClaim(token.getToken());
                    if (organizationId == null || organizationId.isBlank()) {
                        return Mono.error(new UnauthorizedException(
                                "El token no contiene el claim de organizacion (" + organizationClaim
                                        + "). No se puede determinar el contexto del inquilino."));
                    }
                    return Mono.just(organizationId);
                })
                .switchIfEmpty(Mono.fromSupplier(() -> defaultOrganizationId));
    }

    @Override
    public Mono<String> getUserId() {
        return jwt()
                .flatMap(token -> {
                    Jwt jwt = token.getToken();
                    String userId = asString(jwt.getClaim(USER_ID_CLAIM));
                    if (userId == null || userId.isBlank()) {
                        // Tokens previos al claim "userId": mismo fallback que el gateway
                        // (sub). Nunca se usa preferred_username como identificador.
                        userId = jwt.getSubject();
                    }
                    if (userId == null || userId.isBlank()) {
                        return Mono.error(new UnauthorizedException(
                                "El token no contiene un identificador de usuario utilizable"));
                    }
                    return Mono.just(userId);
                })
                .switchIfEmpty(Mono.fromSupplier(() -> defaultUserId));
    }

    private String readOrganizationClaim(Jwt jwt) {
        String configured = asString(jwt.getClaim(organizationClaim));
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        for (String claim : ORGANIZATION_CLAIM_FALLBACKS) {
            if (claim.equals(organizationClaim)) {
                continue;
            }
            String value = asString(jwt.getClaim(claim));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object claim) {
        if (claim == null) {
            return null;
        }
        if (claim instanceof String stringValue) {
            return stringValue;
        }
        if (claim instanceof List<?> list) {
            return list.isEmpty() ? null : asString(list.get(0));
        }
        if (claim instanceof Map<?, ?> map) {
            Object id = map.get("id");
            if (id == null) {
                id = map.get("organizationId");
            }
            return id != null ? String.valueOf(id) : null;
        }
        return String.valueOf(claim);
    }

    private Mono<JwtAuthenticationToken> jwt() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class);
    }
}
