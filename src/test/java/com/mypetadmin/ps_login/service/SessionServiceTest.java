package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.client.PsUserClient;
import com.mypetadmin.ps_login.client.dto.UsuarioContextResponseDTO;
import com.mypetadmin.ps_login.entity.CredentialStatus;
import com.mypetadmin.ps_login.entity.LoginCredential;
import com.mypetadmin.ps_login.entity.RefreshToken;
import com.mypetadmin.ps_login.exception.InvalidRefreshTokenException;
import com.mypetadmin.ps_login.exception.PsUserIntegrationException;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.repository.RefreshTokenRepository;
import com.mypetadmin.ps_login.security.IssuedAccessToken;
import com.mypetadmin.ps_login.security.JwtTokenService;
import com.mypetadmin.ps_login.security.TokenCodec;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    private RefreshTokenRepository refreshRepository;
    private LoginCredentialRepository credentialRepository;
    private PsUserClient psUserClient;
    private TokenCodec tokenCodec;
    private JwtTokenService jwtTokenService;
    private SessionService service;
    private Instant now;
    private LoginCredential credential;

    @BeforeEach
    void setUp() {
        refreshRepository = mock(RefreshTokenRepository.class);
        credentialRepository = mock(LoginCredentialRepository.class);
        psUserClient = mock(PsUserClient.class);
        tokenCodec = mock(TokenCodec.class);
        jwtTokenService = mock(JwtTokenService.class);
        now = Instant.parse("2026-08-27T22:00:00Z");
        credential = new LoginCredential(UUID.randomUUID(), UUID.randomUUID(), CredentialStatus.PENDING_ACTIVATION, now.minusSeconds(100));
        credential.activate("hash", now.minusSeconds(50));
        service = new SessionService(
                refreshRepository,
                credentialRepository,
                psUserClient,
                tokenCodec,
                jwtTokenService,
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofDays(30));
    }

    @Test
    void issuePersisteSomenteHashERetornaTokenPuro() {
        when(tokenCodec.generate()).thenReturn("refresh-raw");
        when(tokenCodec.hash("refresh-raw")).thenReturn("a".repeat(64));

        var issued = service.issue(credential);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshRepository).save(captor.capture());
        assertThat(issued.tokenValue()).isEqualTo("refresh-raw");
        assertThat(issued.expiresInSeconds()).isEqualTo(Duration.ofDays(30).toSeconds());
        assertThat(captor.getValue().getTokenHash()).isEqualTo("a".repeat(64));
    }

    @Test
    void refreshRotacionaTokenERevalidaContextoAtual() {
        RefreshToken current = validToken();
        when(tokenCodec.hash("old-raw")).thenReturn(current.getTokenHash());
        when(refreshRepository.findByTokenHashForUpdate(current.getTokenHash())).thenReturn(Optional.of(current));
        when(credentialRepository.findById(credential.getId())).thenReturn(Optional.of(credential));
        when(psUserClient.buscarContexto(credential.getUserId(), credential.getUserId())).thenReturn(activeContext());
        when(tokenCodec.generate()).thenReturn("new-raw");
        when(tokenCodec.hash("new-raw")).thenReturn("b".repeat(64));
        when(jwtTokenService.issue(any())).thenReturn(new IssuedAccessToken("access", 900));

        var response = service.refresh("old-raw");

        assertThat(current.getUsedAt()).isEqualTo(now);
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("new-raw");
        assertThat(response.refreshExpiresIn()).isEqualTo(Duration.ofDays(30).toSeconds());
    }

    @Test
    void reutilizacaoDeTokenRevogaFamilia() {
        RefreshToken current = validToken();
        current.markUsed(now.minusSeconds(1));
        RefreshToken sibling = validToken(current.getFamilyId());
        when(tokenCodec.hash("reused")).thenReturn(current.getTokenHash());
        when(refreshRepository.findByTokenHashForUpdate(current.getTokenHash())).thenReturn(Optional.of(current));
        when(refreshRepository.findAllByFamilyIdAndRevokedAtIsNull(current.getFamilyId())).thenReturn(List.of(sibling));

        assertThatThrownBy(() -> service.refresh("reused")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(sibling.getRevokedAt()).isEqualTo(now);
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        RefreshToken expired = new RefreshToken(UUID.randomUUID(), credential.getId(), UUID.randomUUID(), "c".repeat(64), now.minusSeconds(100), now.minusSeconds(1));
        when(tokenCodec.hash("expired")).thenReturn(expired.getTokenHash());
        when(refreshRepository.findByTokenHashForUpdate(expired.getTokenHash())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.refresh("expired")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void usuarioInativoRevogaFamilia() {
        RefreshToken current = validToken();
        RefreshToken sibling = validToken(current.getFamilyId());
        when(tokenCodec.hash("raw")).thenReturn(current.getTokenHash());
        when(refreshRepository.findByTokenHashForUpdate(current.getTokenHash())).thenReturn(Optional.of(current));
        when(credentialRepository.findById(credential.getId())).thenReturn(Optional.of(credential));
        when(psUserClient.buscarContexto(credential.getUserId(), credential.getUserId())).thenReturn(inactiveContext());
        when(refreshRepository.findAllByFamilyIdAndRevokedAtIsNull(current.getFamilyId())).thenReturn(List.of(sibling));

        assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(sibling.getRevokedAt()).isEqualTo(now);
    }

    @Test
    void erro5xxDoPsUserNaoConverteEmCredencialInvalida() {
        RefreshToken current = validToken();
        when(tokenCodec.hash("raw")).thenReturn(current.getTokenHash());
        when(refreshRepository.findByTokenHashForUpdate(current.getTokenHash())).thenReturn(Optional.of(current));
        when(credentialRepository.findById(credential.getId())).thenReturn(Optional.of(credential));
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(503);
        when(psUserClient.buscarContexto(credential.getUserId(), credential.getUserId())).thenThrow(ex);

        assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(PsUserIntegrationException.class);
    }

    @Test
    void logoutRevogaTodaFamiliaEehIdempotenteParaTokenDesconhecido() {
        RefreshToken current = validToken();
        RefreshToken sibling = validToken(current.getFamilyId());
        when(tokenCodec.hash("raw")).thenReturn(current.getTokenHash());
        when(refreshRepository.findByTokenHashForUpdate(current.getTokenHash())).thenReturn(Optional.of(current));
        when(refreshRepository.findAllByFamilyIdAndRevokedAtIsNull(current.getFamilyId())).thenReturn(List.of(current, sibling));

        service.logout("raw");

        assertThat(current.getRevokedAt()).isEqualTo(now);
        assertThat(sibling.getRevokedAt()).isEqualTo(now);

        when(tokenCodec.hash("unknown")).thenReturn("d".repeat(64));
        when(refreshRepository.findByTokenHashForUpdate("d".repeat(64))).thenReturn(Optional.empty());
        service.logout("unknown");
    }

    private RefreshToken validToken() {
        return validToken(UUID.randomUUID());
    }

    private RefreshToken validToken(UUID familyId) {
        return new RefreshToken(UUID.randomUUID(), credential.getId(), familyId, "a".repeat(64), now.minusSeconds(60), now.plusSeconds(3600));
    }

    private UsuarioContextResponseDTO activeContext() {
        return new UsuarioContextResponseDTO(credential.getUserId(), UUID.randomUUID(), "User", "user@example.com", "ATIVO", false, Set.of("MASTER"), null, null);
    }

    private UsuarioContextResponseDTO inactiveContext() {
        return new UsuarioContextResponseDTO(credential.getUserId(), UUID.randomUUID(), "User", "user@example.com", "INATIVO", false, Set.of("MASTER"), null, null);
    }
}
