package com.mypetadmin.ps_login.security;

import com.mypetadmin.ps_login.client.dto.UsuarioIdentityResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final Duration accessTtl;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${security.jwt.issuer:ps-login}") String issuer,
            @Value("${security.jwt.access-ttl:PT15M}") Duration accessTtl) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.accessTtl = accessTtl;
    }

    public IssuedAccessToken issue(UsuarioIdentityResponseDTO identity) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(accessTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(identity.userId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("empresaId", identity.empresaId().toString())
                .claim("roles", identity.roles())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new IssuedAccessToken(token, accessTtl.toSeconds());
    }
}
