package com.mypetadmin.ps_login.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InvitationLockServiceTest {

    @Test
    void deveUsarAdvisoryLockDerivadoDoRequestId() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        InvitationLockService service = new InvitationLockService(jdbcTemplate);
        UUID requestId = UUID.fromString("11111111-2222-4333-8444-555555555555");
        long expectedKey = requestId.getMostSignificantBits() ^ requestId.getLeastSignificantBits();

        service.lock(requestId);

        verify(jdbcTemplate).query(
                eq("SELECT pg_advisory_xact_lock(?)"),
                any(ResultSetExtractor.class),
                eq(expectedKey));
    }
}
