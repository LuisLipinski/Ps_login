package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.client.PsUserClient;
import com.mypetadmin.ps_login.entity.RefreshToken;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.repository.RefreshTokenRepository;
import com.mypetadmin.ps_login.security.JwtTokenService;
import com.mypetadmin.ps_login.security.TokenCodec;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionRevocationServiceTest {

    @Test
    void revogaTodasAsSessoesDaCredencial() {
        Instant now = Instant.parse("2026-08-27T22:00:00Z");
        RefreshTokenRepository refreshRepository = mock(RefreshTokenRepository.class);
        UUID credentialId = UUID.randomUUID();
        RefreshToken first = token(credentialId, now);
        RefreshToken second = token(credentialId, now);
        when(refreshRepository.findAllByCredentialIdAndRevokedAtIsNull(credentialId))
                .thenReturn(List.of(first, second));

        SessionService service = new SessionService(
                refreshRepository,
                mock(LoginCredentialRepository.class),
                mock(PsUserClient.class),
                mock(TokenCodec.class),
                mock(JwtTokenService.class),
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofDays(30));

        service.revokeAllForCredential(credentialId);

        assertThat(first.getRevokedAt()).isEqualTo(now);
        assertThat(second.getRevokedAt()).isEqualTo(now);
    }

    private RefreshToken token(UUID credentialId, Instant now) {
        return new RefreshToken(
                UUID.randomUUID(), credentialId, UUID.randomUUID(), "hash-" + UUID.randomUUID(),
                now.minusSeconds(60), now.plusSeconds(3600));
    }
}
