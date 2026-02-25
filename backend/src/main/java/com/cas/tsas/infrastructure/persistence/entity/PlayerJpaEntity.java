package com.cas.tsas.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class PlayerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String gender;
    private Integer ranking;
    private String handedness;
    private String backhandType;

    public PlayerJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getRanking() { return ranking; }
    public void setRanking(Integer ranking) { this.ranking = ranking; }

    public String getHandedness() { return handedness; }
    public void setHandedness(String handedness) { this.handedness = handedness; }

    public String getBackhandType() { return backhandType; }
    public void setBackhandType(String backhandType) { this.backhandType = backhandType; }
}
