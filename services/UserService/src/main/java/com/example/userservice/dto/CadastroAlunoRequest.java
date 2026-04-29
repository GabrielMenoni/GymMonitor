package com.example.userservice.dto;

import lombok.Data;

import java.util.Date;

@Data
public class CadastroAlunoRequest {

    private String name;
    private String email;
    private Date birthDate;
    private String password;
}
