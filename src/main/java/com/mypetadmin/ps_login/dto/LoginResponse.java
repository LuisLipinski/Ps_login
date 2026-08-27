package com.mypetadmin.ps_login.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn
) {
}
