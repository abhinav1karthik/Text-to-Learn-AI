package com.texttolearn.security;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class Auth0UserInfoService {

    private final Auth0Properties auth0Properties;
    private final RestClient restClient;

    public Auth0UserInfoService(Auth0Properties auth0Properties) {
        this.auth0Properties = auth0Properties;
        this.restClient = RestClient.create();
    }

    public AuthenticatedUserProfile getUserProfile(Jwt jwt) {
        if (!auth0Properties.isConfigured()) {
            return fromJwt(jwt);
        }

        try {
            Map<?, ?> body = restClient.get()
                    .uri(userInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                    .retrieve()
                    .body(Map.class);

            if (body == null) {
                return fromJwt(jwt);
            }

            return new AuthenticatedUserProfile(
                    stringClaim(body, "sub", jwt.getSubject()),
                    stringClaim(body, "email", jwt.getClaimAsString("email")),
                    stringClaim(body, "name", jwt.getClaimAsString("name")),
                    stringClaim(body, "picture", jwt.getClaimAsString("picture"))
            );
        } catch (RestClientException ex) {
            return fromJwt(jwt);
        }
    }

    private AuthenticatedUserProfile fromJwt(Jwt jwt) {
        return new AuthenticatedUserProfile(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture")
        );
    }

    private String userInfoUri() {
        String issuerUri = auth0Properties.issuerUri();
        return issuerUri.endsWith("/") ? issuerUri + "userinfo" : issuerUri + "/userinfo";
    }

    private String stringClaim(Map<?, ?> claims, String key, String fallback) {
        Object value = claims.get(key);
        return value instanceof String stringValue ? stringValue : fallback;
    }
}
