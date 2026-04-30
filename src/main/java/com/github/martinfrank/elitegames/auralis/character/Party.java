package com.github.martinfrank.elitegames.auralis.character;

import java.util.List;

public class Party {

    private final List<Adventurer> characters;
    private String locationId;

    public Party(List<Adventurer> characters) {
        this.characters = characters;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationId() {
        return locationId;
    }
}
