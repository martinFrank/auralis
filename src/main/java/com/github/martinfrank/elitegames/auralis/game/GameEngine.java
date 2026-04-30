package com.github.martinfrank.elitegames.auralis.game;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.Agents;
import com.github.martinfrank.elitegames.auralis.character.Party;

public class GameEngine {

    private final GameSession gameSession;
    private final Agents agents;

    public GameEngine (Adventure adventure, Party party, Agents agents) {
        this.gameSession = new GameSession(adventure, party);
        this.agents = agents;
    }


    public void start() {
        Quest current = gameSession.getCurrentQuest();

    }

    private void createIntro() {

    }
}
