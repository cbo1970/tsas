package com.cas.tsas.player.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Domain entity representing a tennis player.
 * Pure POJO — no framework dependencies.
 */
public class Player {

    private UUID id;
    private String firstName;
    private String lastName;
    private Gender gender;
    private Handedness handedness;
    private BackhandType backhandType;
    private String ranking;
    private String nationality;
    private LocalDate birthDate;
    private boolean active = true;

    public Player() {}

    public Player(UUID id, String firstName, String lastName, Gender gender,
                  Handedness handedness, BackhandType backhandType,
                  String ranking, String nationality, LocalDate birthDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.handedness = handedness;
        this.backhandType = backhandType;
        this.ranking = ranking;
        this.nationality = nationality;
        this.birthDate = birthDate;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
