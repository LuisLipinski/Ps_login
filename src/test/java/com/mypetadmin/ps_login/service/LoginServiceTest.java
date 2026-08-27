package com.mypetadmin.ps_login.service;

import com.mypetadmin.ps_login.client.PsUserClient;
import com.mypetadmin.ps_login.client.dto.UsuarioIdentityResponseDTO;
import com.mypetadmin.ps_login.dto.LoginRequest;
import com.mypetadmin.ps_login.entity.CredentialStatus;
import com.mypetadmin.ps_login.entity.LoginCredential;
import com.mypetadmin.ps_login.exception.InvalidCredentialsException;
import com.mypetadmin.ps_login.exception.PsUserIntegrationException;
import com.mypetadmin.ps_login.repository.LoginCredentialRepository;
import com.mypetadmin.ps_login.security.IssuedAccessToken;
import com.mypetadmin.ps_login.security.JwtTokenService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private PsUserClient psUserClient;
    private LoginCredentialRepository credentialRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService jwtTokenService;
    private LoginService service;
    private UUID userId;
    private UUID empresaId;

    @BeforeEach
    void setUp() {
        psUserClient = mock(PsUserClient.class);
        credentialRepository = mock(LoginCredentialRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenService = mock(JwtTokenService.class);
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        service = new LoginService(psUserClient, credentialRepository, passwordEncoder, jwtTokenService);
        userId = UUID.randomUUID();
        empresaId = UUID.randomUUID();
    }

    @Test
    void autenticaCredencialAtivaEEmiteToken() {
        var identity = identity("ATIVO");
        LoginCredential credential = activeCredential();
        when(psUserClient.buscarIdentidade("user@example.com")).thenReturn(identity);
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("senha-correta", credential.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.issue(identity)).thenReturn(new IssuedAccessToken("jwt-token", 900));

        var response = service.login(new LoginRequest(" USER@EXAMPLE.COM ", "senha-correta"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
    }

    @Test
    void rejeitaUsuarioInativoSemEmitirToken() {
        when(psUserClient.buscarIdentidade("user@example.com")).thenReturn(identity("INATIVO"));
        when(passwordEncoder.matches("senha", "dummy-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("user@example.com", "senha")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtTokenService, never()).issue(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejeitaCredencialAusente() {
        when(psUserClient.buscarIdentidade("user@example.com")).thenReturn(identity("ATIVO"));
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("senha", "dummy-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("user@example.com", "senha")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejeitaCredencialAindaPendente() {
        LoginCredential credential = new LoginCredential(UUID.randomUUID(), userId, CredentialStatus.PENDING_ACTIVATION, Instant.now());
        when(psUserClient.buscarIdentidade("user@example.com")).thenReturn(identity("ATIVO"));
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("senha", "dummy-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("user@example.com", "senha")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejeitaSenhaIncorreta() {
        LoginCredential credential = activeCredential();
        when(psUserClient.buscarIdentidade("user@example.com")).thenReturn(identity("ATIVO"));
        when(credentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("senha-errada", credential.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("user@example.com", "senha-errada")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void trataUsuarioNaoEncontradoComoCredencialInvalida() {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(404);
        when(psUserClient.buscarIdentidade("missing@example.com")).thenThrow(exception);
        when(passwordEncoder.matches("senha", "dummy-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("missing@example.com", "senha")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void falhaDoPsUserRetornaErroDeIntegracao() {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(503);
        when(psUserClient.buscarIdentidade("user@example.com")).thenThrow(exception);

        assertThatThrownBy(() -> service.login(new LoginRequest("user@example.com", "senha")))
                .isInstanceOf(PsUserIntegrationException.class);
    }

    private UsuarioIdentityResponseDTO identity(String status) {
        return new UsuarioIdentityResponseDTO(userId, empresaId, "user@example.com", status, Set.of("MASTER"));
    }

    private LoginCredential activeCredential() {
        LoginCredential credential = new LoginCredential(UUID.randomUUID(), userId, CredentialStatus.PENDING_ACTIVATION, Instant.now());
        credential.activate("stored-hash", Instant.now());
        return credential;
    }
}
