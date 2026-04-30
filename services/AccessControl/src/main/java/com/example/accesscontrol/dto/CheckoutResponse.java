package com.example.accesscontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CheckoutResponse {
    private UUID sessaoId;
    private UUID userId;
    private String userType;
    private String entradaEm;
    private String saidaEm;
}
