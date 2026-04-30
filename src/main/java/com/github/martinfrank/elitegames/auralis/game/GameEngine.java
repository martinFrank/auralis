package com.github.martinfrank.elitegames.auralis.game;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.Agents;
import com.github.martinfrank.elitegames.auralis.agent.SetupQuestAgent;
import com.github.martinfrank.elitegames.auralis.character.Party;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class GameEngine {

    private final GameSession gameSession;
    private final Agents agents;

    public GameEngine (Adventure adventure, Party party, Agents agents) {
        this.gameSession = new GameSession(adventure, party);
        this.agents = agents;
    }


    public void start() {
        Quest current = gameSession.getCurrentQuest();

        gameSession.getChat().addHeroldMessage("statischer Introtext", "willkommen bei dem Abenteuer \""+gameSession.getAdventure().title() + "\" von "+gameSession.getAdventure().author());
        gameSession.getChat().addHeroldMessage("statischer Introtext", gameSession.getAdventure().description());

        startQuest(current);
    }

    private void startQuest(Quest quest) {
        gameSession.setCurrentLocationId(quest.startLocationId());
        gameSession.setCurrentTime(quest.startTime());

        Location location = gameSession.getAdventure().getLocation(gameSession.getCurrentLocationId());
        String setup = agents.getSetupQuestAgent().setupQuest(quest, location, quest.startTime());
        gameSession.getChat().addHeroldMessage("AI generiert Text für das Einleiten eines neuen quests", setup);
        gameSession.getChat().addHeroldMessage("quest-location allgemeine beschreibung", location.generalInfo());

//        agents.
        int i = 0;
    }
}
