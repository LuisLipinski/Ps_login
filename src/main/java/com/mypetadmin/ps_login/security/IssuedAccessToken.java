package com.mypetadmin.ps_login.security;

public record IssuedAccessToken(String tokenValue, long expiresInSeconds) {
}
