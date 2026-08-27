package com.mypetadmin.ps_login.controller;

import com.mypetadmin.ps_login.dto.ActivationRequest;
import com.mypetadmin.ps_login.service.ActivationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/activation")
public class ActivationController {

    private final ActivationService activationService;

    public ActivationController(ActivationService activationService) {
        this.activationService = activationService;
    }

    @PostMapping
    public ResponseEntity<Void> activate(@Valid @RequestBody ActivationRequest request) {
        activationService.activate(request);
        return ResponseEntity.noContent().build();
    }
}
