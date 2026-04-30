package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.CheckinResponse;
import com.example.accesscontrol.dto.CheckoutResponse;
import com.example.accesscontrol.entity.UserType;
import com.example.accesscontrol.security.AuthenticatedUser;
import com.example.accesscontrol.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/access")
@RequiredArgsConstructor
public class AccessController {

    private final AccessService accessService;

    @PostMapping("/checkin")
    public ResponseEntity<CheckinResponse> checkin(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        CheckinResponse response = accessService.checkin(user.userId(), UserType.valueOf(user.role()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        CheckoutResponse response = accessService.checkout(user.userId(), UserType.valueOf(user.role()));
        return ResponseEntity.ok(response);
    }
}
