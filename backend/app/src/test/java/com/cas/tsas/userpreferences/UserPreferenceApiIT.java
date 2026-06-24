package com.cas.tsas.userpreferences;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.app.userpreferences.UserPreferenceJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TEN-6 — User-Preferences API (Sprachpräferenz). */
class UserPreferenceApiIT extends AbstractIntegrationTest {

    @Autowired UserPreferenceJpaRepository repository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void get_returnsDefaultDe_whenNoRecordYet() throws Exception {
        mockMvc.perform(get("/api/user-preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("de"));
    }

    @Test
    void put_persistsAndEchoes_chosenLanguage() throws Exception {
        mockMvc.perform(put("/api/user-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"));

        mockMvc.perform(get("/api/user-preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    void put_updatesExistingRecord() throws Exception {
        mockMvc.perform(put("/api/user-preferences")
                .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"it\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/user-preferences")
                .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"fr\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("fr"));
    }

    @Test
    void put_rejectsUnsupportedLanguageWith400() throws Exception {
        mockMvc.perform(put("/api/user-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"es\"}"))
                .andExpect(status().isBadRequest());
    }
}
