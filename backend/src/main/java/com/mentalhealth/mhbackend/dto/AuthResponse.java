package com.mentalhealth.mhbackend.dto;

public record AuthResponse(String token, String role, String email) {}
