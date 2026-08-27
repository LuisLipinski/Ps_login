package com.mypetadmin.ps_login.repository;

import com.mypetadmin.ps_login.entity.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, UUID> {
    Optional<ActivationToken> findByTokenHash(String tokenHash);
    List<ActivationToken> findAllByCredentialIdAndUsedAtIsNullAndRevokedAtIsNull(UUID credentialId);
}
