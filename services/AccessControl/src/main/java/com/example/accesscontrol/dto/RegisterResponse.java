package com.example.accesscontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RegisterResponse {

    private UUID id;
    private String name;
    private String email;
    private String role;
}
