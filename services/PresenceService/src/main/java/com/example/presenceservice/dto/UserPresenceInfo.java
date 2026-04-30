package com.example.presenceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserPresenceInfo {
    private UUID userId;
    private String userType;
    private String entradaEm;
    private UUID sessaoId;
}
