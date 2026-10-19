package org.acme.transfer.dto.response;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {}
