package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.dto.ActivationRequest;
import com.mypetadmin.ps_login.entity.ActivationToken;
import com.mypetadmin.ps_login.entity.CredentialStatus;
import com.mypetadmin.ps_login.entity.LoginCredential;
import com.mypetadmin.ps_login.exception.ActivationTokenInvalidException;
import com.mypetadmin.ps_login.exception.PasswordPolicyException;
import com.mypetadmin.ps_login.repository.ActivationTokenRepository;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.security.PasswordPolicy;
import com.mypetadmin.ps_login.security.TokenCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivationServiceTest {

    private ActivationTokenRepository tokenRepository;
    private LoginCredentialRepository credentialRepository;
    private PasswordEncoder passwordEncoder;
    private TokenCodec tokenCodec;
    private ActivationService service;
    private Instant now;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(ActivationTokenRepository.class);
        credentialRepository = mock(LoginCredentialRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenCodec = new TokenCodec();
        now = Instant.parse("2026-08-27T20:00:00Z");
        service = new ActivationService(
                tokenRepository,
                credentialRepository,
                tokenCodec,
                new PasswordPolicy(12),
                passwordEncoder,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void ativaCredencialEConsomeToken() {
        String rawToken = "activation-token";
        LoginCredential credential = new LoginCredential(UUID.randomUUID(), UUID.randomUUID(), CredentialStatus.PENDING_ACTIVATION, now.minusSeconds(60));
        ActivationToken token = new ActivationToken(UUID.randomUUID(), credential.getId(), tokenCodec.hash(rawToken), now.plusSeconds(3600), now.minusSeconds(60));
        ActivationToken other = new ActivationToken(UUID.randomUUID(), credential.getId(), "b".repeat(64), now.plusSeconds(3600), now.minusSeconds(30));
        when(tokenRepository.findByTokenHash(tokenCodec.hash(rawToken))).thenReturn(Optional.of(token));
        when(credentialRepository.findById(credential.getId())).thenReturn(Optional.of(credential));
        when(tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(credential.getId())).thenReturn(List.of(token, other));
        when(passwordEncoder.encode("SenhaSegura123")).thenReturn("{pbkdf2}hash");

        service.activate(new ActivationRequest(rawToken, "SenhaSegura123", "SenhaSegura123"));

        assertThat(credential.getStatus()).isEqualTo(CredentialStatus.ACTIVE);
        assertThat(credential.getPasswordHash()).isEqualTo("{pbkdf2}hash");
        assertThat(token.getUsedAt()).isEqualTo(now);
        assertThat(other.getRevokedAt()).isEqualTo(now);
        verify(credentialRepository).save(credential);
        verify(tokenRepository).save(token);
    }

    @Test
    void rejeitaTokenInexistente() {
        String rawToken = "missing";
        when(tokenRepository.findByTokenHash(tokenCodec.hash(rawToken))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(new ActivationRequest(rawToken, "SenhaSegura123", "SenhaSegura123")))
                .isInstanceOf(ActivationTokenInvalidException.class);
    }

    @Test
    void rejeitaTokenExpirado() {
        String rawToken = "expired";
        ActivationToken token = new ActivationToken(UUID.randomUUID(), UUID.randomUUID(), tokenCodec.hash(rawToken), now.minusSeconds(1), now.minusSeconds(3600));
        when(tokenRepository.findByTokenHash(tokenCodec.hash(rawToken))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.activate(new ActivationRequest(rawToken, "SenhaSegura123", "SenhaSegura123")))
                .isInstanceOf(ActivationTokenInvalidException.class);
    }

    @Test
    void rejeitaSenhaQueNaoConfere() {
        String rawToken = "activation-token";
        LoginCredential credential = new LoginCredential(UUID.randomUUID(), UUID.randomUUID(), CredentialStatus.PENDING_ACTIVATION, now.minusSeconds(60));
        ActivationToken token = new ActivationToken(UUID.randomUUID(), credential.getId(), tokenCodec.hash(rawToken), now.plusSeconds(3600), now.minusSeconds(60));
        when(tokenRepository.findByTokenHash(tokenCodec.hash(rawToken))).thenReturn(Optional.of(token));
        when(credentialRepository.findById(credential.getId())).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service.activate(new ActivationRequest(rawToken, "SenhaSegura123", "OutraSenha123")))
                .isInstanceOf(PasswordPolicyException.class);
    }
}
