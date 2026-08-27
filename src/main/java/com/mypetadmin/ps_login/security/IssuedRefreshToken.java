package com.mypetadmin.ps_login.security;

public record IssuedRefreshToken(String tokenValue, long expiresInSeconds) {
}
