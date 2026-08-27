package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.dto.ActivationRequest;
import com.mypetadmin.ps_login.entity.ActivationToken;
import com.mypetadmin.ps_login.entity.LoginCredential;
import com.mypetadmin.ps_login.exception.ActivationTokenInvalidException;
import com.mypetadmin.ps_login.repository.ActivationTokenRepository;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.security.PasswordPolicy;
import com.mypetadmin.ps_login.security.TokenCodec;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ActivationService {

    private final ActivationTokenRepository tokenRepository;
    private final LoginCredentialRepository credentialRepository;
    private final TokenCodec tokenCodec;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ActivationService(
            ActivationTokenRepository tokenRepository,
            LoginCredentialRepository credentialRepository,
            TokenCodec tokenCodec,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.credentialRepository = credentialRepository;
        this.tokenCodec = tokenCodec;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public void activate(ActivationRequest request) {
        Instant now = clock.instant();
        ActivationToken token = tokenRepository.findByTokenHash(tokenCodec.hash(request.token()))
                .filter(found -> found.isUsableAt(now))
                .orElseThrow(ActivationTokenInvalidException::new);

        LoginCredential credential = credentialRepository.findById(token.getCredentialId())
                .orElseThrow(ActivationTokenInvalidException::new);

        passwordPolicy.validate(request.password(), request.passwordConfirmation());
        String passwordHash = passwordEncoder.encode(request.password());

        tokenRepository.findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(credential.getId())
                .stream()
                .filter(other -> !other.getId().equals(token.getId()))
                .forEach(other -> other.revoke(now));

        credential.activate(passwordHash, now);
        token.markUsed(now);
        credentialRepository.save(credential);
        tokenRepository.save(token);
    }
}
