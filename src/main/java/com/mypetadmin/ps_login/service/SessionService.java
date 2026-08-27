package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.client.PsUserClient;
import com.mypetadmin.ps_login.client.dto.UsuarioContextResponseDTO;
import com.mypetadmin.ps_login.client.dto.UsuarioIdentityResponseDTO;
import com.mypetadmin.ps_login.dto.LoginResponse;
import com.mypetadmin.ps_login.entity.CredentialStatus;
import com.mypetadmin.ps_login.entity.LoginCredential;
import com.mypetadmin.ps_login.entity.RefreshToken;
import com.mypetadmin.ps_login.exception.InvalidRefreshTokenException;
import com.mypetadmin.ps_login.exception.PsUserIntegrationException;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.repository.RefreshTokenRepository;
import com.mypetadmin.ps_login.security.IssuedAccessToken;
import com.mypetadmin.ps_login.security.IssuedRefreshToken;
import com.mypetadmin.ps_login.security.JwtTokenService;
import com.mypetadmin.ps_login.security.TokenCodec;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class SessionService {

    private static final String ACTIVE_USER = "ATIVO";

    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginCredentialRepository credentialRepository;
    private final PsUserClient psUserClient;
    private final TokenCodec tokenCodec;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;
    private final Duration refreshTtl;

    public SessionService(
            RefreshTokenRepository refreshTokenRepository,
            LoginCredentialRepository credentialRepository,
            PsUserClient psUserClient,
            TokenCodec tokenCodec,
            JwtTokenService jwtTokenService,
            Clock clock,
            @Value("${security.jwt.refresh-ttl:P30D}") Duration refreshTtl) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.credentialRepository = credentialRepository;
        this.psUserClient = psUserClient;
        this.tokenCodec = tokenCodec;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public IssuedRefreshToken issue(LoginCredential credential) {
        return createToken(credential.getId(), UUID.randomUUID(), clock.instant());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public LoginResponse refresh(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenCodec.hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isUsed()) {
            revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isRevoked() || current.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        LoginCredential credential = credentialRepository.findById(current.getCredentialId())
                .filter(found -> found.getStatus() == CredentialStatus.ACTIVE)
                .orElseThrow(() -> {
                    revokeFamily(current.getFamilyId(), now);
                    return new InvalidRefreshTokenException();
                });

        UsuarioIdentityResponseDTO identity = loadCurrentIdentity(credential.getUserId(), current.getFamilyId(), now);

        current.markUsed(now);
        refreshTokenRepository.save(current);
        IssuedRefreshToken replacement = createToken(credential.getId(), current.getFamilyId(), now);
        IssuedAccessToken access = jwtTokenService.issue(identity);

        return new LoginResponse(
                access.tokenValue(),
                "Bearer",
                access.expiresInSeconds(),
                replacement.tokenValue(),
                replacement.expiresInSeconds());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHashForUpdate(tokenCodec.hash(rawToken))
                .ifPresent(token -> revokeFamily(token.getFamilyId(), clock.instant()));
    }

    private IssuedRefreshToken createToken(UUID credentialId, UUID familyId, Instant now) {
        String rawToken = tokenCodec.generate();
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                credentialId,
                familyId,
                tokenCodec.hash(rawToken),
                now,
                now.plus(refreshTtl));
        refreshTokenRepository.save(token);
        return new IssuedRefreshToken(rawToken, refreshTtl.toSeconds());
    }

    private UsuarioIdentityResponseDTO loadCurrentIdentity(UUID userId, UUID familyId, Instant now) {
        final UsuarioContextResponseDTO context;
        try {
            context = psUserClient.buscarContexto(userId, userId);
        } catch (FeignException ex) {
            if (ex.status() >= 400 && ex.status() < 500) {
                revokeFamily(familyId, now);
                throw new InvalidRefreshTokenException();
            }
            throw new PsUserIntegrationException();
        }

        if (context == null || !userId.equals(context.id()) || !ACTIVE_USER.equals(context.status())) {
            revokeFamily(familyId, now);
            throw new InvalidRefreshTokenException();
        }

        Set<String> roles = context.roles() == null ? Set.of() : Set.copyOf(context.roles());
        return new UsuarioIdentityResponseDTO(
                context.id(),
                context.empresaId(),
                context.email(),
                context.status(),
                roles);
    }

    private void revokeFamily(UUID familyId, Instant now) {
        refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(familyId)
                .forEach(token -> token.revoke(now));
    }
}
