package com.texttolearn.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(Auth0Properties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Auth0Properties auth0Properties) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                );

        if (auth0Properties.isConfigured()) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        }

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(Auth0Properties auth0Properties) {
        if (!auth0Properties.isConfigured()) {
            return token -> {
                throw new IllegalStateException(
                        "Auth0 is not configured. Set AUTH0_ISSUER_URI and AUTH0_AUDIENCE."
                );
            };
        }

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(auth0Properties.issuerUri()).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(auth0Properties.issuerUri());
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(auth0Properties.audience());
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return jwtDecoder;
    }
}
