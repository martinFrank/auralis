package com.github.martinfrank.elitegames.auralis.character;

import com.github.martinfrank.elitegames.auralis.adventure.Location;

import java.util.List;

public class Party {

    private final List<Adventurer> characters;
    private Location location;

    public Party(List<Adventurer> characters) {
        this.characters = characters;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
