package com.mypetadmin.ps_login.client;

import com.mypetadmin.ps_login.client.dto.UsuarioIdentityResponseDTO;
import com.mypetadmin.ps_login.config.PsUserFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ps-user", url = "${clients.ps-user.url}", configuration = PsUserFeignConfig.class)
public interface PsUserClient {

    @GetMapping("/internal/usuarios/identity")
    UsuarioIdentityResponseDTO buscarIdentidade(@RequestParam("email") String email);
}
