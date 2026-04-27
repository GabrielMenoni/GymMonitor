package com.example.accesscontrol.dto;

import lombok.Data;

@Data
public class CadastroAdminRequest {

    private String name;
    private String email;
    private String password;
}
