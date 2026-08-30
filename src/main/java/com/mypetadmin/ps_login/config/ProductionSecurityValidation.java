package com.mypetadmin.ps_login.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.charset.StandardCharsets;

@Configuration
@Profile("prod")
public class ProductionSecurityValidation {

    public ProductionSecurityValidation(@Value("${security.internal-key}") String internalKey) {
        if (internalKey == null || internalKey.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_KEY deve estar configurada em producao.");
        }
        if (internalKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("INTERNAL_API_KEY deve possuir pelo menos 256 bits de entropia representada.");
        }
    }
}
