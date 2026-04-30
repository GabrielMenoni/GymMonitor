package com.example.accesscontrol.dto;

import java.util.UUID;

public record AccessEvent(UUID eventId, String eventType, UUID userId, String userType, String timestamp) {}
