package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.client.PsUserClient;
import com.mypetadmin.ps_login.client.dto.UsuarioIdentityResponseDTO;
import com.mypetadmin.ps_login.dto.InvitationRequest;
import com.mypetadmin.ps_login.entity.ActivationToken;
import com.mypetadmin.ps_login.entity.CredentialStatus;
import com.mypetadmin.ps_login.entity.LoginCredential;
import com.mypetadmin.ps_login.exception.CredentialAlreadyActiveException;
import com.mypetadmin.ps_login.exception.IdentityValidationException;
import com.mypetadmin.ps_login.exception.PsUserIntegrationException;
import com.mypetadmin.ps_login.mail.ActivationMailSender;
import com.mypetadmin.ps_login.repository.ActivationTokenRepository;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.security.TokenCodec;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvitationService {

    private final PsUserClient psUserClient;
    private final LoginCredentialRepository credentialRepository;
    private final ActivationTokenRepository tokenRepository;
    private final TokenCodec tokenCodec;
    private final ActivationMailSender mailSender;
    private final InvitationLockService invitationLockService;
    private final Clock clock;
    private final Duration tokenTtl;

    public InvitationService(
            PsUserClient psUserClient,
            LoginCredentialRepository credentialRepository,
            ActivationTokenRepository tokenRepository,
            TokenCodec tokenCodec,
            ActivationMailSender mailSender,
            InvitationLockService invitationLockService,
            Clock clock,
            @Value("${app.activation.ttl:PT24H}") Duration tokenTtl) {
        this.psUserClient = psUserClient;
        this.credentialRepository = credentialRepository;
        this.tokenRepository = tokenRepository;
        this.tokenCodec = tokenCodec;
        this.mailSender = mailSender;
        this.invitationLockService = invitationLockService;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    @Transactional
    public void createInvitation(InvitationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UsuarioIdentityResponseDTO identity = loadIdentity(email);
        validateIdentity(request.userId(), email, identity);

        if (request.requestId() != null) {
            invitationLockService.lock(request.requestId());
            ActivationToken replay = tokenRepository.findByRequestId(request.requestId()).orElse(null);
            if (replay != null) {
                validateReplay(replay, identity);
                return;
            }
        }

        Instant now = clock.instant();
        LoginCredential credential = credentialRepository.findByUserId(identity.userId())
                .orElseGet(() -> credentialRepository.save(new LoginCredential(
                        UUID.randomUUID(), identity.userId(), CredentialStatus.PENDING_ACTIVATION, now)));

        if (credential.getStatus() == CredentialStatus.ACTIVE) {
            throw new CredentialAlreadyActiveException();
        }

        tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(credential.getId())
                .forEach(token -> token.revoke(now));

        String rawToken = tokenCodec.generate();
        ActivationToken token = new ActivationToken(
                UUID.randomUUID(),
                credential.getId(),
                request.requestId(),
                tokenCodec.hash(rawToken),
                now.plus(tokenTtl),
                now);
        tokenRepository.saveAndFlush(token);
        mailSender.sendActivation(identity.email(), rawToken);
    }

    private UsuarioIdentityResponseDTO loadIdentity(String email) {
        try {
            return psUserClient.buscarIdentidade(email);
        } catch (FeignException ex) {
            throw new PsUserIntegrationException();
        }
    }

    private void validateIdentity(UUID expectedUserId, String expectedEmail, UsuarioIdentityResponseDTO identity) {
        if (!expectedUserId.equals(identity.userId()) || !expectedEmail.equals(identity.email().trim().toLowerCase(Locale.ROOT))) {
            throw new IdentityValidationException("A identidade recebida do PS_User não corresponde ao usuário solicitado.");
        }
        if (!"ATIVO".equals(identity.status())) {
            throw new IdentityValidationException("Usuário inativo não pode receber ativação de credencial.");
        }
    }

    private void validateReplay(ActivationToken token, UsuarioIdentityResponseDTO identity) {
        LoginCredential credential = credentialRepository.findById(token.getCredentialId())
                .orElseThrow(() -> new IdentityValidationException("Convite idempotente sem credencial correspondente."));
        if (!credential.getUserId().equals(identity.userId())) {
            throw new IdentityValidationException("requestId já utilizado para outro usuário.");
        }
    }
}
