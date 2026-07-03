package com.cas.tsas.player;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import com.cas.tsas.player.domain.model.Player;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerPersistenceAdapter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
class PlayerPersistenceAdapterIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private PlayerPersistenceAdapter adapter;

    private static Player newPlayer(String first, String last) {
        return new Player(null, UUID.randomUUID(), first, last, Gender.MALE, Handedness.RIGHT,
                BackhandType.TWO_HANDED, "4.0", "CH", LocalDate.of(2000, 1, 1));
    }

    // =========================================================================
    @Nested
    class Save {

        @Test
        void assigns_id_on_first_save() {
            Player saved = adapter.savePlayer(newPlayer("Max", "Muster"));
            assertThat(saved.getId()).isNotNull();
        }
    }

    // =========================================================================
    @Nested
    class LoadById {

        @Test
        void returns_player_when_found() {
            Player saved = adapter.savePlayer(newPlayer("Max", "Muster"));

            assertThat(adapter.loadPlayer(saved.getId()))
                    .isPresent()
                    .hasValueSatisfying(p -> {
                        assertThat(p.getFirstName()).isEqualTo("Max");
                        assertThat(p.getLastName()).isEqualTo("Muster");
                        assertThat(p.isActive()).isTrue();
                    });
        }

        @Test
        void returns_empty_when_not_found() {
            assertThat(adapter.loadPlayer(UUID.randomUUID())).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    class LoadAll {

        @Test
        void returns_all_saved_players() {
            adapter.savePlayer(newPlayer("Max", "Muster"));
            adapter.savePlayer(newPlayer("Anna", "Müller"));

            assertThat(adapter.loadAllPlayers()).hasSize(2);
        }
    }

    // =========================================================================
    @Nested
    class Delete {

        @Test
        void removes_player_from_db() {
            Player saved = adapter.savePlayer(newPlayer("Max", "Muster"));

            adapter.deletePlayer(saved.getId());

            assertThat(adapter.loadPlayer(saved.getId())).isEmpty();
        }
    }
}
