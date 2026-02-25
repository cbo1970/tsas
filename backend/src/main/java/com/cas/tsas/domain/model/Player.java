package com.cas.tsas.domain.model;

/**
 * Domain entity representing a tennis player.
 * Pure POJO — no framework dependencies.
 */
public class Player {

    private Long id;
    private String name;
    private String gender;
    private Integer ranking;
    private String handedness;
    private String backhandType;

    public Player() {}

    public Player(Long id, String name, String gender, Integer ranking,
                  String handedness, String backhandType) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.ranking = ranking;
        this.handedness = handedness;
        this.backhandType = backhandType;
    }

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
