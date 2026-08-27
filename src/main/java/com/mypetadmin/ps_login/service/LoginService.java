package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.client.PsUserClient;
import com.mypetadmin.ps_login.client.dto.UsuarioIdentityResponseDTO;
import com.mypetadmin.ps_login.dto.LoginRequest;
import com.mypetadmin.ps_login.dto.LoginResponse;
import com.mypetadmin.ps_login.entity.CredentialStatus;
import com.mypetadmin.ps_login.entity.LoginCredential;
import com.mypetadmin.ps_login.exception.InvalidCredentialsException;
import com.mypetadmin.ps_login.exception.PsUserIntegrationException;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.security.IssuedAccessToken;
import com.mypetadmin.ps_login.security.IssuedRefreshToken;
import com.mypetadmin.ps_login.security.JwtTokenService;
import feign.FeignException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class LoginService {

    private static final String ACTIVE_USER = "ATIVO";

    private final PsUserClient psUserClient;
    private final LoginCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SessionService sessionService;
    private final String dummyPasswordHash;

    public LoginService(
            PsUserClient psUserClient,
            LoginCredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            SessionService sessionService) {
        this.psUserClient = psUserClient;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.sessionService = sessionService;
        this.dummyPasswordHash = passwordEncoder.encode("mypetadmin-dummy-password-never-valid");
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UsuarioIdentityResponseDTO identity = loadIdentity(email, request.password());

        if (!ACTIVE_USER.equals(identity.status())) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new InvalidCredentialsException();
        }

        Optional<LoginCredential> credential = credentialRepository.findByUserId(identity.userId())
                .filter(found -> found.getStatus() == CredentialStatus.ACTIVE)
                .filter(found -> found.getPasswordHash() != null && !found.getPasswordHash().isBlank());

        String hashToCheck = credential.map(LoginCredential::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
        if (credential.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        IssuedAccessToken access = jwtTokenService.issue(identity);
        IssuedRefreshToken refresh = sessionService.issue(credential.get());
        return new LoginResponse(
                access.tokenValue(),
                "Bearer",
                access.expiresInSeconds(),
                refresh.tokenValue(),
                refresh.expiresInSeconds());
    }

    private UsuarioIdentityResponseDTO loadIdentity(String email, String rawPassword) {
        try {
            return psUserClient.buscarIdentidade(email);
        } catch (FeignException ex) {
            if (ex.status() == 404) {
                passwordEncoder.matches(rawPassword, dummyPasswordHash);
                throw new InvalidCredentialsException();
            }
            throw new PsUserIntegrationException();
        }
    }
}
