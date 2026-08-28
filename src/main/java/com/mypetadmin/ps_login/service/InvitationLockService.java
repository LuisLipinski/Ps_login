package com.mypetadmin.ps_login.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvitationLockService {

    private final JdbcTemplate jdbcTemplate;

    public void lock(UUID requestId) {
        long lockKey = requestId.getMostSignificantBits() ^ requestId.getLeastSignificantBits();
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(?)", rs -> null, lockKey);
    }
}
