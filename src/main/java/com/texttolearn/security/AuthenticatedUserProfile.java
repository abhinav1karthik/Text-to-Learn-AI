package com.texttolearn.security;

public record AuthenticatedUserProfile(
        String auth0Subject,
        String email,
        String name,
        String pictureUrl
) {
}
