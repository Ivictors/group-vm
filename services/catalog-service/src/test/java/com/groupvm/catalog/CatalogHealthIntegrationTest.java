package com.groupvm.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class CatalogHealthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HealthEndpointGroups groups;

    @Test
    void probesAreAnonymousAndReadyWhenPostgresIsAvailable() throws Exception {
        org.junit.jupiter.api.Assertions.assertTrue(groups.getNames().contains("liveness"), groups.getNames().toString());
        org.junit.jupiter.api.Assertions.assertTrue(groups.getNames().contains("readiness"), groups.getNames().toString());
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void operationalEndpointsDoNotExposeOtherInformation() throws Exception {
        for (String path : new String[] {"/actuator", "/actuator/health", "/actuator/metrics"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(jwt().authorities(new SimpleGrantedAuthority("catalog_admin"))))
                    .andExpect(status().isForbidden());
        }
    }
}
