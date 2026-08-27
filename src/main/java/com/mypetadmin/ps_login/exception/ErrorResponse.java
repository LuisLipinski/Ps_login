package com.mypetadmin.ps_login.exception;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String code, String message) {
}
