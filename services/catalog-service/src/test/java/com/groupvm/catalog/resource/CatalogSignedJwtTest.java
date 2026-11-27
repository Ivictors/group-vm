package com.groupvm.catalog.resource;

import com.groupvm.catalog.application.ProductService;
import com.groupvm.catalog.application.PublicProductService;
import com.groupvm.catalog.domain.Product;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

@WebMvcTest({ProductResource.class, PublicProductResource.class})
@Import(CatalogSecurity.class)
class CatalogSignedJwtTest {
    private static final String ISSUER = "https://identity.example.test/realms/catalog";
    private static final String ADMIN = "/api/v1/admin/products";
    private static final String KEY_ID = "catalog-test-key";
    private static final KeyPair KEY = newKey();
    private static final KeyPair OTHER_KEY = newKey();
    private static final HttpServer JWK_SERVER = startJwkServer();

    @Autowired MockMvc mvc;
    @MockBean ProductService adminService;
    @MockBean PublicProductService publicService;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + JWK_SERVER.getAddress().getPort() + "/jwks");
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences", () -> "catalog-service");
    }

    @AfterAll
    static void stopJwkServer() {
        JWK_SERVER.stop(0);
    }

    @Test
    void acceptsSignedAdminTokenAndKeepsPublicReadsAnonymous() throws Exception {
        when(adminService.list(0, 20)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(publicService.list(0, 20)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mvc.perform(get(ADMIN).header("Authorization", "Bearer " + token(KEY, ISSUER,
                        "catalog-service", Instant.now().plusSeconds(300), Instant.now().minusSeconds(10),
                        List.of("catalog_admin"))))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("test-operator"));
        mvc.perform(get("/api/v1/products")).andExpect(status().isOk());
    }

    @Test
    void signedAdministratorCanCreateProductWithValidatedSubject() throws Exception {
        when(adminService.create(any(), org.mockito.ArgumentMatchers.eq("test-operator")))
                .thenReturn(new Product("CHAIR-01", "Chair", null,
                new BigDecimal("129.90"), 4500, BigDecimal.valueOf(55),
                BigDecimal.valueOf(50), BigDecimal.valueOf(90)));
        String bearer = token(KEY, ISSUER, "catalog-service", Instant.now().plusSeconds(300),
                Instant.now().minusSeconds(10), List.of("catalog_admin"));
        mvc.perform(post(ADMIN).header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"CHAIR-01","name":"Chair","price":{"currency":"BRL","amount":"129.90"},
                                 "weightGrams":4500,"lengthCm":55,"widthCm":50,"heightCm":90}
                                """))
                .andExpect(status().isCreated())
                .andExpect(authenticated().withUsername("test-operator"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
        verify(adminService).create(any(), org.mockito.ArgumentMatchers.eq("test-operator"));
    }

    @Test
    void acceptsLongValidatedSubjectWithoutTruncation() throws Exception {
        String subject = "operator-" + "a".repeat(300);
        when(adminService.create(any(), org.mockito.ArgumentMatchers.eq(subject)))
                .thenReturn(new Product("CHAIR-02", "Chair", null, new BigDecimal("129.90"),
                        4500, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        mvc.perform(post(ADMIN).header("Authorization", "Bearer " + token(KEY, ISSUER,
                        "catalog-service", Instant.now().plusSeconds(300), Instant.now().minusSeconds(10),
                        List.of("catalog_admin"), subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku":"CHAIR-02","name":"Chair","price":{"currency":"BRL","amount":"129.90"},
                         "weightGrams":4500,"lengthCm":1,"widthCm":1,"heightCm":1}
                        """))
                .andExpect(status().isCreated());
        verify(adminService).create(any(), org.mockito.ArgumentMatchers.eq(subject));
    }

    @Test
    void rejectsInvalidSignatureIssuerAudienceAndTimeClaims() throws Exception {
        Instant now = Instant.now();
        for (String invalid : List.of(
                token(OTHER_KEY, ISSUER, "catalog-service", now.plusSeconds(300), now.minusSeconds(10), List.of("catalog_admin")),
                token(KEY, "https://other.example.test", "catalog-service", now.plusSeconds(300), now.minusSeconds(10), List.of("catalog_admin")),
                token(KEY, ISSUER, "other-service", now.plusSeconds(300), now.minusSeconds(10), List.of("catalog_admin")),
                token(KEY, ISSUER, "catalog-service", now.minusSeconds(300), now.minusSeconds(600), List.of("catalog_admin")),
                token(KEY, ISSUER, "catalog-service", now.plusSeconds(600), now.plusSeconds(300), List.of("catalog_admin")))) {
            mvc.perform(get(ADMIN).header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get(ADMIN).header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenWithoutCatalogRoleIsForbidden() throws Exception {
        mvc.perform(get(ADMIN).header("Authorization", "Bearer " + token(KEY, ISSUER,
                        "catalog-service", Instant.now().plusSeconds(300), Instant.now().minusSeconds(10),
                        List.of("other_role"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticationFailuresUseGenericProblemsWithoutEchoingBearer() throws Exception {
        String invalid = token(OTHER_KEY, ISSUER, "catalog-service", Instant.now().plusSeconds(300),
                Instant.now().minusSeconds(10), List.of("catalog_admin"));
        var unauthorized = mvc.perform(get(ADMIN).header("Authorization", "Bearer " + invalid))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized")).andReturn();
        assertFalse(unauthorized.getResponse().getContentAsString().contains(invalid));
        assertFalse(unauthorized.getResponse().getHeader("WWW-Authenticate").contains(invalid));

        String withoutRole = token(KEY, ISSUER, "catalog-service", Instant.now().plusSeconds(300),
                Instant.now().minusSeconds(10), List.of("other_role"));
        var forbidden = mvc.perform(get(ADMIN).header("Authorization", "Bearer " + withoutRole))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Forbidden")).andReturn();
        assertFalse(forbidden.getResponse().getContentAsString().contains(withoutRole));
        assertFalse(forbidden.getResponse().getHeader("WWW-Authenticate").contains(withoutRole));
    }

    @Test
    void missingOrBlankSubjectCannotAuthorizeAnAuditSensitiveWrite() throws Exception {
        for (String subject : java.util.Arrays.asList(null, "", "   ")) {
            String bearer = token(KEY, ISSUER, "catalog-service", Instant.now().plusSeconds(300),
                    Instant.now().minusSeconds(10), List.of("catalog_admin"), subject);
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(ADMIN)
                            .header("Authorization", "Bearer " + bearer))
                    .andExpect(status().isUnauthorized());
        }
    }

    private static String token(KeyPair signingKey, String issuer, String audience, Instant expiry,
                                Instant notBefore, List<String> roles) throws Exception {
        return token(signingKey, issuer, audience, expiry, notBefore, roles, "test-operator");
    }

    private static String token(KeyPair signingKey, String issuer, String audience, Instant expiry,
                                Instant notBefore, List<String> roles, String subject) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer).audience(audience).subject(subject)
                .issueTime(Date.from(Instant.now().minusSeconds(30)))
                .expirationTime(Date.from(expiry)).notBeforeTime(Date.from(notBefore))
                .claim("resource_access", Map.of("catalog-service", Map.of("roles", roles)))
                .build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims);
        jwt.sign(new RSASSASigner(signingKey.getPrivate()));
        return jwt.serialize();
    }

    private static KeyPair newKey() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer startJwkServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var publicKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) KEY.getPublic())
                    .keyID(KEY_ID).build();
            byte[] body = ("{\"keys\":[" + publicKey.toJSONString() + "]}").getBytes(StandardCharsets.UTF_8);
            server.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
