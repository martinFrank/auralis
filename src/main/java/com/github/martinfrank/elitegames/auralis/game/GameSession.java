package com.github.martinfrank.elitegames.auralis.game;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.character.Party;

public class GameSession {

    private final Adventure adventure;
    private final Party party;
    private final GameChat chat = new GameChat();
    private String currentTime = "morning";


    public GameSession(Adventure adventure, Party party) {
        this.adventure = adventure;
        this.party = party;
        party.setLocation(adventure.getLocation(adventure.startLocationId()));
        currentTime = "afternoon"; //FIXME muss noch im Editor erstellt werden
    }

    public Quest getCurrentQuest() {
        return null;
    }
}
