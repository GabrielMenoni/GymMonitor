package com.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class FuncionarioResponse {

    private UUID id;
    private String name;
    private String email;
    private String position;
    private float salary;
}
