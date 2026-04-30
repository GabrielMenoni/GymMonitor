package com.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AlunoResponse {

    private UUID id;
    private String name;
    private String email;
    private Date birthDate;
    private Date monthlyPaymentDueDate;
}
