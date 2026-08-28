package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.client.PsUserClient;
import com.mypetadmin.ps_login.client.dto.UsuarioIdentityResponseDTO;
import com.mypetadmin.ps_login.dto.InvitationRequest;
import com.mypetadmin.ps_login.entity.ActivationToken;
import com.mypetadmin.ps_login.entity.CredentialStatus;
import com.mypetadmin.ps_login.entity.LoginCredential;
import com.mypetadmin.ps_login.exception.CredentialAlreadyActiveException;
import com.mypetadmin.ps_login.exception.IdentityValidationException;
import com.mypetadmin.ps_login.mail.ActivationMailSender;
import com.mypetadmin.ps_login.repository.ActivationTokenRepository;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.security.TokenCodec;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationServiceTest {

    private PsUserClient psUserClient;
    private LoginCredentialRepository credentialRepository;
    private ActivationTokenRepository tokenRepository;
    private ActivationMailSender mailSender;
    private InvitationLockService invitationLockService;
    private TokenCodec tokenCodec;
    private InvitationService service;
    private UUID userId;
    private UUID empresaId;

    @BeforeEach
    void setUp() {
        psUserClient = mock(PsUserClient.class);
        credentialRepository = mock(LoginCredentialRepository.class);
        tokenRepository = mock(ActivationTokenRepository.class);
        mailSender = mock(ActivationMailSender.class);
        invitationLockService = mock(InvitationLockService.class);
        tokenCodec = new TokenCodec();
        Clock clock = Clock.fixed(Instant.parse("2026-08-27T20:00:00Z"), ZoneOffset.UTC);
        service = new InvitationService(
                psUserClient,
                credentialRepository,
                tokenRepository,
                tokenCodec,
                mailSender,
                invitationLockService,
                clock,
                Duration.ofHours(24));
        userId = UUID.randomUUID();
        empresaId = UUID.randomUUID();
    }

    @Test
    void criaCredencialPendenteTokenHashEEnviaConviteLegado() {
        stubIdentity(userId);
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(credentialRepository.save(any(LoginCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(any())).thenReturn(List.of());

        service.createInvitation(new InvitationRequest(userId, " USER@EXAMPLE.COM "));

        ArgumentCaptor<ActivationToken> tokenCaptor = ArgumentCaptor.forClass(ActivationToken.class);
        verify(tokenRepository).saveAndFlush(tokenCaptor.capture());
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailSender).sendActivation(org.mockito.ArgumentMatchers.eq("user@example.com"), rawTokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getRequestId()).isNull();
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(tokenCodec.hash(rawTokenCaptor.getValue()));
        assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(Instant.parse("2026-08-28T20:00:00Z"));
        verify(invitationLockService, never()).lock(any());
    }

    @Test
    void criaConviteComRequestIdPersistido() {
        UUID requestId = UUID.randomUUID();
        stubIdentity(userId);
        when(tokenRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(credentialRepository.save(any(LoginCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(any())).thenReturn(List.of());

        service.createInvitation(new InvitationRequest(userId, "user@example.com", requestId));

        ArgumentCaptor<ActivationToken> tokenCaptor = ArgumentCaptor.forClass(ActivationToken.class);
        verify(invitationLockService).lock(requestId);
        verify(tokenRepository).saveAndFlush(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getRequestId()).isEqualTo(requestId);
        verify(mailSender).sendActivation(org.mockito.ArgumentMatchers.eq("user@example.com"), any());
    }

    @Test
    void replayComMesmoRequestIdNaoGeraNovoTokenNemReenviaEmail() {
        UUID requestId = UUID.randomUUID();
        LoginCredential credential = new LoginCredential(
                UUID.randomUUID(), userId, CredentialStatus.PENDING_ACTIVATION, Instant.now());
        ActivationToken existente = new ActivationToken(
                UUID.randomUUID(), credential.getId(), requestId, "a".repeat(64),
                Instant.now().plusSeconds(3600), Instant.now());
        stubIdentity(userId);
        when(tokenRepository.findByRequestId(requestId)).thenReturn(Optional.of(existente));
        when(credentialRepository.findById(credential.getId())).thenReturn(Optional.of(credential));

        service.createInvitation(new InvitationRequest(userId, "user@example.com", requestId));

        verify(invitationLockService).lock(requestId);
        verify(credentialRepository, never()).findByUserId(any());
        verify(tokenRepository, never()).saveAndFlush(any());
        verify(mailSender, never()).sendActivation(any(), any());
    }

    @Test
    void rejeitaRequestIdReutilizadoParaOutroUsuario() {
        UUID requestId = UUID.randomUUID();
        UUID outroUserId = UUID.randomUUID();
        LoginCredential outraCredencial = new LoginCredential(
                UUID.randomUUID(), outroUserId, CredentialStatus.PENDING_ACTIVATION, Instant.now());
        ActivationToken existente = new ActivationToken(
                UUID.randomUUID(), outraCredencial.getId(), requestId, "b".repeat(64),
                Instant.now().plusSeconds(3600), Instant.now());
        stubIdentity(userId);
        when(tokenRepository.findByRequestId(requestId)).thenReturn(Optional.of(existente));
        when(credentialRepository.findById(outraCredencial.getId())).thenReturn(Optional.of(outraCredencial));

        assertThatThrownBy(() -> service.createInvitation(
                new InvitationRequest(userId, "user@example.com", requestId)))
                .isInstanceOf(IdentityValidationException.class)
                .hasMessageContaining("requestId");

        verify(mailSender, never()).sendActivation(any(), any());
    }

    @Test
    void rejeitaIdentidadeDiferenteDaSolicitada() {
        stubIdentity(UUID.randomUUID());

        assertThatThrownBy(() -> service.createInvitation(new InvitationRequest(userId, "user@example.com")))
                .isInstanceOf(IdentityValidationException.class);
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void rejeitaUsuarioInativo() {
        when(psUserClient.buscarIdentidade("user@example.com"))
                .thenReturn(new UsuarioIdentityResponseDTO(userId, empresaId, "user@example.com", "INATIVO", Set.of("LOJA")));

        assertThatThrownBy(() -> service.createInvitation(new InvitationRequest(userId, "user@example.com")))
                .isInstanceOf(IdentityValidationException.class);
    }

    @Test
    void rejeitaConviteQuandoCredencialJaEstaAtiva() {
        LoginCredential credential = new LoginCredential(UUID.randomUUID(), userId, CredentialStatus.PENDING_ACTIVATION, Instant.now());
        credential.activate("hash", Instant.now());
        stubIdentity(userId);
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service.createInvitation(new InvitationRequest(userId, "user@example.com")))
                .isInstanceOf(CredentialAlreadyActiveException.class);
    }

    @Test
    void revogaConvitePendenteAnteriorAoReenviarComNovaRequisicao() {
        LoginCredential credential = new LoginCredential(UUID.randomUUID(), userId, CredentialStatus.PENDING_ACTIVATION, Instant.now());
        ActivationToken oldToken = new ActivationToken(
                UUID.randomUUID(), credential.getId(), "a".repeat(64), Instant.now().plusSeconds(3600), Instant.now());
        stubIdentity(userId);
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(credential.getId())).thenReturn(List.of(oldToken));

        service.createInvitation(new InvitationRequest(userId, "user@example.com"));

        assertThat(oldToken.getRevokedAt()).isEqualTo(Instant.parse("2026-08-27T20:00:00Z"));
        verify(tokenRepository).saveAndFlush(any(ActivationToken.class));
    }

    private void stubIdentity(UUID identityUserId) {
        when(psUserClient.buscarIdentidade("user@example.com"))
                .thenReturn(new UsuarioIdentityResponseDTO(
                        identityUserId, empresaId, "user@example.com", "ATIVO", Set.of("LOJA")));
    }
}
