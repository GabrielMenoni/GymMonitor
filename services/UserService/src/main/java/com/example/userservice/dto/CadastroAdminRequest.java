package com.example.userservice.dto;

import lombok.Data;

@Data
public class CadastroAdminRequest {

    private String name;
    private String email;
    private String password;
}
