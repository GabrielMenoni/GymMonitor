package com.example.accesscontrol.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, String role) {}
