package com.mypetadmin.ps_login.client.dto;

import java.util.Set;
import java.util.UUID;

public record UsuarioIdentityResponseDTO(
        UUID userId,
        UUID empresaId,
        String email,
        String status,
        Set<String> roles
) {
}
