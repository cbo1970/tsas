package com.cas.tsas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class OpenApiContractIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void openApiDocumentIsServed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.openapi").value(startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("TSaS API"))
                .andExpect(jsonPath("$.paths['/api/players']").exists())
                .andExpect(jsonPath("$.paths['/api/matches/{id}/points']").exists())
                .andExpect(jsonPath("$.paths['/api/matches/{matchId}/analysis']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"));
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
}
