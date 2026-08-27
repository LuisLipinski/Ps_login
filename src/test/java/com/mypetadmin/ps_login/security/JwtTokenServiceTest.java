package com.mypetadmin.ps_login.security;

import com.mypetadmin.ps_login.client.dto.UsuarioIdentityResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private static final String BASE64_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void emiteJwtComClaimsDeTenantERoles() {
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(BASE64_SECRET), "HmacSHA256");
        var encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        Instant now = Instant.parse("2026-08-27T20:00:00Z");
        JwtTokenService service = new JwtTokenService(encoder, Clock.fixed(now, ZoneOffset.UTC), "ps-login", Duration.ofMinutes(15));
        UUID userId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        UsuarioIdentityResponseDTO identity = new UsuarioIdentityResponseDTO(userId, empresaId, "user@example.com", "ATIVO", Set.of("MASTER", "LOJA"));

        IssuedAccessToken issued = service.issue(identity);

        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var jwt = decoder.decode(issued.tokenValue());
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("empresaId")).isEqualTo(empresaId.toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactlyInAnyOrder("MASTER", "LOJA");
        assertThat(jwt.getIssuer().toString()).isEqualTo("ps-login");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plusSeconds(900));
        assertThat(issued.expiresInSeconds()).isEqualTo(900);
    }
}
