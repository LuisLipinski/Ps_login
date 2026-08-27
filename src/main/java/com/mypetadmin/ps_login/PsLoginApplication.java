package com.mypetadmin.ps_login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PsLoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsLoginApplication.class, args);
    }
}
