package com.groupvm.catalog.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.Map;
import java.io.IOException;

@Configuration
public class CatalogSecurity {
    private static final String ADMIN_ROLE = "catalog_admin";

    @Bean
    JwtDecoder catalogJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri) {
        NimbusJwtDecoder decoder = jwkSetUri.isBlank()
                ? (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer)
                : NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>("aud", audiences ->
                        audiences != null && audiences.contains("catalog-service")),
                new JwtClaimValidator<String>("sub", subject ->
                        subject != null && !subject.isBlank())));
        return decoder;
    }

    @Bean
    SecurityFilterChain catalogFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        var authenticationEntryPoint = new BearerTokenAuthenticationEntryPoint();
        var accessDeniedHandler = new BearerTokenAccessDeniedHandler();
        AuthenticationEntryPoint unauthorized = (request, response, failure) -> {
            authenticationEntryPoint.commence(request, response, failure);
            writeProblem(response, objectMapper, HttpStatus.UNAUTHORIZED, "Unauthorized");
        };
        AccessDeniedHandler forbidden = (request, response, failure) -> {
            accessDeniedHandler.handle(request, response, failure);
            writeProblem(response, objectMapper, HttpStatus.FORBIDDEN, "Forbidden");
        };
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/{productId}")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/products", "/api/v1/admin/products/{productId}")
                        .hasAuthority(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/products",
                                "/api/v1/admin/products/{productId}/activate",
                                "/api/v1/admin/products/{productId}/archive")
                        .hasAuthority(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/products/{productId}")
                        .hasAuthority(ADMIN_ROLE)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(unauthorized)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter())))
                .build();
    }

    private static void writeProblem(HttpServletResponse response, ObjectMapper mapper,
                                     HttpStatus status, String title) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        var problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        mapper.writeValue(response.getOutputStream(), problem);
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(CatalogSecurity::catalogAuthorities);
        return converter;
    }

    private static List<GrantedAuthority> catalogAuthorities(Jwt jwt) {
        Object access = jwt.getClaim("resource_access");
        if (!(access instanceof Map<?, ?> clients)
                || !(clients.get("catalog-service") instanceof Map<?, ?> catalog)
                || !(catalog.get("roles") instanceof List<?> roles)
                || !roles.contains(ADMIN_ROLE)) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority(ADMIN_ROLE));
    }
}
