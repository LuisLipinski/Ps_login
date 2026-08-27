package com.mypetadmin.ps_login.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class PsUserFeignConfig {

    @Bean
    RequestInterceptor internalKeyInterceptor(@Value("${security.internal-key}") String internalKey) {
        return template -> template.header("X-Internal-Key", internalKey);
    }
}
