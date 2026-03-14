package com.cas.tsas.infrastructure.persistence.entity;

import com.cas.tsas.domain.model.BackhandType;
import com.cas.tsas.domain.model.Gender;
import com.cas.tsas.domain.model.Handedness;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "players")
public class PlayerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    private Integer ranking;
    private String nationality;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    public PlayerJpaEntity() {}

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

    public Integer getRanking() { return ranking; }
    public void setRanking(Integer ranking) { this.ranking = ranking; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
}
