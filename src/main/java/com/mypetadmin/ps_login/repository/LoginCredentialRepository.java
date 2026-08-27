package com.mypetadmin.ps_login.repository;

import com.mypetadmin.ps_login.entity.LoginCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoginCredentialRepository extends JpaRepository<LoginCredential, UUID> {
    Optional<LoginCredential> findByUserId(UUID userId);
}
