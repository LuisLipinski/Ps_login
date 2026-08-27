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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class PasswordService {

    private static final String ACTIVE_USER = "ATIVO";

    private final PsUserClient psUserClient;
    private final LoginCredentialRepository credentialRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetMailSender mailSender;
    private final TokenCodec tokenCodec;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final Clock clock;
    private final Duration resetTtl;

    public PasswordService(
            PsUserClient psUserClient,
            LoginCredentialRepository credentialRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetMailSender mailSender,
            TokenCodec tokenCodec,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            SessionService sessionService,
            Clock clock,
            @Value("${app.password-reset.ttl:PT30M}") Duration resetTtl) {
        this.psUserClient = psUserClient;
        this.credentialRepository = credentialRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.tokenCodec = tokenCodec;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.clock = clock;
        this.resetTtl = resetTtl;
    }

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        final UsuarioIdentityResponseDTO identity;
        try {
            identity = psUserClient.buscarIdentidade(email);
        } catch (FeignException ex) {
            if (ex.status() == 404) {
                return;
            }
            throw new PsUserIntegrationException();
        }

        if (identity == null || identity.userId() == null || !ACTIVE_USER.equals(identity.status())) {
            return;
        }

        LoginCredential credential = credentialRepository.findByUserId(identity.userId())
                .filter(found -> found.getStatus() == CredentialStatus.ACTIVE)
                .filter(found -> found.getPasswordHash() != null && !found.getPasswordHash().isBlank())
                .orElse(null);
        if (credential == null) {
            return;
        }

        Instant now = clock.instant();
        revokePendingResetTokens(credential.getId(), now, null);

        String rawToken = tokenCodec.generate();
        PasswordResetToken token = new PasswordResetToken(
                UUID.randomUUID(),
                credential.getId(),
                tokenCodec.hash(rawToken),
                now.plus(resetTtl),
                now);
        tokenRepository.save(token);

        String targetEmail = identity.email() == null || identity.email().isBlank() ? email : identity.email();
        mailSender.sendReset(targetEmail, rawToken);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        ensureActiveUserForChange(userId);

        LoginCredential credential = credentialRepository.findByUserId(userId)
                .filter(found -> found.getStatus() == CredentialStatus.ACTIVE)
                .filter(found -> found.getPasswordHash() != null && !found.getPasswordHash().isBlank())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.currentPassword(), credential.getPasswordHash())) {
            throw new CurrentPasswordInvalidException();
        }

        passwordPolicy.validate(request.newPassword(), request.newPasswordConfirmation());
        Instant now = clock.instant();
        credential.updatePassword(passwordEncoder.encode(request.newPassword()), now);
        revokePendingResetTokens(credential.getId(), now, null);
        sessionService.revokeAllForCredential(credential.getId());
        credentialRepository.save(credential);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = clock.instant();
        PasswordResetToken token = tokenRepository.findByTokenHashForUpdate(tokenCodec.hash(request.token()))
                .filter(found -> found.isUsableAt(now))
                .orElseThrow(InvalidPasswordResetTokenException::new);

        LoginCredential credential = credentialRepository.findById(token.getCredentialId())
                .filter(found -> found.getStatus() == CredentialStatus.ACTIVE)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        ensureActiveUserForReset(credential.getUserId());
        passwordPolicy.validate(request.password(), request.passwordConfirmation());

        credential.updatePassword(passwordEncoder.encode(request.password()), now);
        token.markUsed(now);
        revokePendingResetTokens(credential.getId(), now, token.getId());
        sessionService.revokeAllForCredential(credential.getId());
        credentialRepository.save(credential);
        tokenRepository.save(token);
    }

    private void revokePendingResetTokens(UUID credentialId, Instant now, UUID exceptTokenId) {
        tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(credentialId)
                .stream()
                .filter(token -> exceptTokenId == null || !token.getId().equals(exceptTokenId))
                .forEach(token -> token.revoke(now));
    }

    private void ensureActiveUserForChange(UUID userId) {
        final UsuarioContextResponseDTO context;
        try {
            context = psUserClient.buscarContexto(userId, userId);
        } catch (FeignException ex) {
            if (ex.status() == 403 || ex.status() == 404) {
                throw new InvalidCredentialsException();
            }
            throw new PsUserIntegrationException();
        }
        if (context == null || !userId.equals(context.id()) || !ACTIVE_USER.equals(context.status())) {
            throw new InvalidCredentialsException();
        }
    }

    private void ensureActiveUserForReset(UUID userId) {
        final UsuarioContextResponseDTO context;
        try {
            context = psUserClient.buscarContexto(userId, userId);
        } catch (FeignException ex) {
            if (ex.status() == 403 || ex.status() == 404) {
                throw new InvalidPasswordResetTokenException();
            }
            throw new PsUserIntegrationException();
        }
        if (context == null || !userId.equals(context.id()) || !ACTIVE_USER.equals(context.status())) {
            throw new InvalidPasswordResetTokenException();
        }
    }
}
