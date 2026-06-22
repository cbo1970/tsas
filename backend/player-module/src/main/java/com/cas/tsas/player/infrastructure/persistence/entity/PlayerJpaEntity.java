package com.cas.tsas.player.infrastructure.persistence.entity;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "players")
public class PlayerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private Handedness handedness;

    @Enumerated(EnumType.STRING)
    @Column(name = "backhand_type")
    private BackhandType backhandType;

    private String ranking;
    private String nationality;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    public PlayerJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public Handedness getHandedness() { return handedness; }
    public void setHandedness(Handedness handedness) { this.handedness = handedness; }

    public BackhandType getBackhandType() { return backhandType; }
    public void setBackhandType(BackhandType backhandType) { this.backhandType = backhandType; }

    public String getRanking() { return ranking; }
    public void setRanking(String ranking) { this.ranking = ranking; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
