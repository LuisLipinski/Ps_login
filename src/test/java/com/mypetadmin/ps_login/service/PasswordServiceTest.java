package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.client.PsUserClient;
import com.mypetadmin.ps_login.client.dto.UsuarioContextResponseDTO;
import com.mypetadmin.ps_login.client.dto.UsuarioIdentityResponseDTO;
import com.mypetadmin.ps_login.dto.ChangePasswordRequest;
import com.mypetadmin.ps_login.dto.ForgotPasswordRequest;
import com.mypetadmin.ps_login.dto.ResetPasswordRequest;
import com.mypetadmin.ps_login.entity.CredentialStatus;
import com.mypetadmin.ps_login.entity.LoginCredential;
import com.mypetadmin.ps_login.entity.PasswordResetToken;
import com.mypetadmin.ps_login.exception.CurrentPasswordInvalidException;
import com.mypetadmin.ps_login.exception.InvalidCredentialsException;
import com.mypetadmin.ps_login.exception.InvalidPasswordResetTokenException;
import com.mypetadmin.ps_login.exception.PsUserIntegrationException;
import com.mypetadmin.ps_login.mail.PasswordResetMailSender;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.repository.PasswordResetTokenRepository;
import com.mypetadmin.ps_login.security.PasswordPolicy;
import com.mypetadmin.ps_login.security.TokenCodec;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-27T22:00:00Z");

    private PsUserClient psUserClient;
    private LoginCredentialRepository credentialRepository;
    private PasswordResetTokenRepository tokenRepository;
    private PasswordResetMailSender mailSender;
    private TokenCodec tokenCodec;
    private PasswordPolicy passwordPolicy;
    private PasswordEncoder passwordEncoder;
    private SessionService sessionService;
    private PasswordService service;
    private UUID userId;
    private UUID empresaId;

    @BeforeEach
    void setUp() {
        psUserClient = mock(PsUserClient.class);
        credentialRepository = mock(LoginCredentialRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        mailSender = mock(PasswordResetMailSender.class);
        tokenCodec = mock(TokenCodec.class);
        passwordPolicy = mock(PasswordPolicy.class);
        passwordEncoder = mock(PasswordEncoder.class);
        sessionService = mock(SessionService.class);
        service = new PasswordService(
                psUserClient,
                credentialRepository,
                tokenRepository,
                mailSender,
                tokenCodec,
                passwordPolicy,
                passwordEncoder,
                sessionService,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(30));
        userId = UUID.randomUUID();
        empresaId = UUID.randomUUID();
    }

    @Test
    void solicitaResetRevogandoTokenAnteriorEEnviandoNovo() {
        LoginCredential credential = activeCredential();
        PasswordResetToken oldToken = resetToken(credential.getId(), NOW.plusSeconds(600));
        when(psUserClient.buscarIdentidade("user@example.com")).thenReturn(identity("ATIVO", "user@example.com"));
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(credential.getId()))
                .thenReturn(List.of(oldToken));
        when(tokenCodec.generate()).thenReturn("raw-token");
        when(tokenCodec.hash("raw-token")).thenReturn("token-hash");

        service.requestReset(new ForgotPasswordRequest(" USER@EXAMPLE.COM "));

        assertThat(oldToken.getRevokedAt()).isEqualTo(NOW);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getCredentialId()).isEqualTo(credential.getId());
        assertThat(captor.getValue().getTokenHash()).isEqualTo("token-hash");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(1800));
        verify(mailSender).sendReset("user@example.com", "raw-token");
    }

    @Test
    void solicitacaoDeResetEhNeutraParaUsuarioInexistente() {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(404);
        when(psUserClient.buscarIdentidade("missing@example.com")).thenThrow(exception);

        service.requestReset(new ForgotPasswordRequest("missing@example.com"));

        verifyNoInteractions(credentialRepository, mailSender);
    }

    @Test
    void solicitacaoDeResetEhNeutraParaUsuarioInativoOuCredencialAusente() {
        when(psUserClient.buscarIdentidade("user@example.com")).thenReturn(identity("INATIVO", "user@example.com"));
        service.requestReset(new ForgotPasswordRequest("user@example.com"));
        verifyNoInteractions(credentialRepository, mailSender);

        when(psUserClient.buscarIdentidade("other@example.com"))
                .thenReturn(new UsuarioIdentityResponseDTO(UUID.randomUUID(), empresaId, "other@example.com", "ATIVO", Set.of("LOJA")));
        when(credentialRepository.findByUserId(any())).thenReturn(Optional.empty());
        service.requestReset(new ForgotPasswordRequest("other@example.com"));
        verify(mailSender, never()).sendReset(any(), any());
    }

    @Test
    void falhaDoPsUserNaSolicitacaoDeResetNaoEhMascarada() {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(503);
        when(psUserClient.buscarIdentidade("user@example.com")).thenThrow(exception);

        assertThatThrownBy(() -> service.requestReset(new ForgotPasswordRequest("user@example.com")))
                .isInstanceOf(PsUserIntegrationException.class);
    }

    @Test
    void trocaSenhaAtualERevogaResetESessoes() {
        LoginCredential credential = activeCredential();
        PasswordResetToken pending = resetToken(credential.getId(), NOW.plusSeconds(600));
        when(psUserClient.buscarContexto(userId, userId)).thenReturn(context("ATIVO"));
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("senha-atual", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("senha-nova-segura")).thenReturn("new-hash");
        when(tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(credential.getId()))
                .thenReturn(List.of(pending));

        service.changePassword(userId, new ChangePasswordRequest(
                "senha-atual", "senha-nova-segura", "senha-nova-segura"));

        verify(passwordPolicy).validate("senha-nova-segura", "senha-nova-segura");
        verify(sessionService).revokeAllForCredential(credential.getId());
        verify(credentialRepository).save(credential);
        assertThat(credential.getPasswordHash()).isEqualTo("new-hash");
        assertThat(credential.getPasswordUpdatedAt()).isEqualTo(NOW);
        assertThat(pending.getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    void trocaSenhaRejeitaSenhaAtualIncorreta() {
        LoginCredential credential = activeCredential();
        when(psUserClient.buscarContexto(userId, userId)).thenReturn(context("ATIVO"));
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("errada", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(userId,
                new ChangePasswordRequest("errada", "senha-nova-segura", "senha-nova-segura")))
                .isInstanceOf(CurrentPasswordInvalidException.class);
        verify(sessionService, never()).revokeAllForCredential(any());
    }

    @Test
    void trocaSenhaRejeitaUsuarioQueNaoEstaMaisAtivo() {
        when(psUserClient.buscarContexto(userId, userId)).thenReturn(context("INATIVO"));

        assertThatThrownBy(() -> service.changePassword(userId,
                new ChangePasswordRequest("atual", "nova-segura-123", "nova-segura-123")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(credentialRepository);
    }

    @Test
    void trocaSenhaPropagaIndisponibilidadeDoPsUser() {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(503);
        when(psUserClient.buscarContexto(userId, userId)).thenThrow(exception);

        assertThatThrownBy(() -> service.changePassword(userId,
                new ChangePasswordRequest("atual", "nova-segura-123", "nova-segura-123")))
                .isInstanceOf(PsUserIntegrationException.class);
    }

    @Test
    void redefineSenhaConsumindoTokenERevogandoDemaisTokensESessoes() {
        LoginCredential credential = activeCredential();
        PasswordResetToken current = resetToken(credential.getId(), NOW.plusSeconds(600));
        PasswordResetToken other = resetToken(credential.getId(), NOW.plusSeconds(700));
        when(tokenCodec.hash("reset-token")).thenReturn("reset-hash");
        when(tokenRepository.findByTokenHashForUpdate("reset-hash")).thenReturn(Optional.of(current));
        when(credentialRepository.findById(credential.getId())).thenReturn(Optional.of(credential));
        when(psUserClient.buscarContexto(userId, userId)).thenReturn(context("ATIVO"));
        when(passwordEncoder.encode("nova-senha-segura")).thenReturn("reset-password-hash");
        when(tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(credential.getId()))
                .thenReturn(List.of(current, other));

        service.resetPassword(new ResetPasswordRequest("reset-token", "nova-senha-segura", "nova-senha-segura"));

        verify(passwordPolicy).validate("nova-senha-segura", "nova-senha-segura");
        verify(sessionService).revokeAllForCredential(credential.getId());
        assertThat(current.getUsedAt()).isEqualTo(NOW);
        assertThat(other.getRevokedAt()).isEqualTo(NOW);
        assertThat(credential.getPasswordHash()).isEqualTo("reset-password-hash");
        verify(tokenRepository).save(current);
    }

    @Test
    void redefineSenhaRejeitaTokenExpirado() {
        PasswordResetToken expired = resetToken(UUID.randomUUID(), NOW.minusSeconds(1));
        when(tokenCodec.hash("expired")).thenReturn("expired-hash");
        when(tokenRepository.findByTokenHashForUpdate("expired-hash")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest("expired", "nova-senha-segura", "nova-senha-segura")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
        verifyNoInteractions(credentialRepository);
    }

    @Test
    void redefineSenhaRejeitaUsuarioInativadoDepoisDaSolicitacao() {
        LoginCredential credential = activeCredential();
        PasswordResetToken token = resetToken(credential.getId(), NOW.plusSeconds(600));
        when(tokenCodec.hash("token")).thenReturn("hash");
        when(tokenRepository.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));
        when(credentialRepository.findById(credential.getId())).thenReturn(Optional.of(credential));
        when(psUserClient.buscarContexto(userId, userId)).thenReturn(context("INATIVO"));

        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest("token", "nova-senha-segura", "nova-senha-segura")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    private UsuarioIdentityResponseDTO identity(String status, String email) {
        return new UsuarioIdentityResponseDTO(userId, empresaId, email, status, Set.of("MASTER"));
    }

    private UsuarioContextResponseDTO context(String status) {
        return new UsuarioContextResponseDTO(
                userId, empresaId, "Usuário", "user@example.com", status, true,
                Set.of("MASTER"), OffsetDateTime.now(), OffsetDateTime.now());
    }

    private LoginCredential activeCredential() {
        LoginCredential credential = new LoginCredential(
                UUID.randomUUID(), userId, CredentialStatus.PENDING_ACTIVATION, NOW.minusSeconds(3600));
        credential.activate("stored-hash", NOW.minusSeconds(3000));
        return credential;
    }

    private PasswordResetToken resetToken(UUID credentialId, Instant expiresAt) {
        return new PasswordResetToken(UUID.randomUUID(), credentialId, UUID.randomUUID().toString().replace("-", ""), expiresAt, NOW.minusSeconds(10));
    }
}
