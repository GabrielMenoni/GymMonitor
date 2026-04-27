package com.example.accesscontrol.dto;

import lombok.Data;

@Data
public class CadastroFuncionarioRequest {

    private String name;
    private String email;
    private String position;
    private float salary;
    private String password;
}
