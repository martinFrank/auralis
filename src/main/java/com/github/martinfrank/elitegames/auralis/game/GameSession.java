package com.github.martinfrank.elitegames.auralis.game;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.Condition;
import com.github.martinfrank.elitegames.auralis.adventure.Flag;
import com.github.martinfrank.elitegames.auralis.adventure.FlagCondition;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.adventure.Questbook;
import com.github.martinfrank.elitegames.auralis.character.Party;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GameSession {

    private final Adventure adventure;
    private final Party party;
    private final GameChat chat = new GameChat();
    private final Map<String, Boolean> flags = new HashMap<>();
    private String currentTime = "morning";

    public GameSession(Adventure adventure, Party party) {
        this.adventure = adventure;
        this.party = party;
        for (Flag flag : adventure.content().flags()) {
            flags.put(flag.id(), flag.initialValue());
        }
    }

    public GameChat getChat() {
        return chat;
    }

    public boolean isFlagSet(String id) {
        return flags.getOrDefault(id, false);
    }

    public void setFlag(String id, boolean value) {
        flags.put(id, value);
    }

    public Map<String, Boolean> getFlags() {
        return Collections.unmodifiableMap(flags);
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public Quest getCurrentQuest() {
        Questbook questbook = adventure.content().questbook();
        if (questbook == null || questbook.quests() == null) {
            return null;
        }
        Quest current = null;
        for (Quest quest : questbook.quests()) {
            if (isStarted(quest) && !isCompleted(quest)) {
                current = quest;
            }
        }
        return current;
    }

    private boolean isStarted(Quest quest) {
        return quest.startCondition() == null || evaluate(quest.startCondition());
    }

    private boolean isCompleted(Quest quest) {
        return quest.completionCondition() != null && evaluate(quest.completionCondition());
    }

    private boolean evaluate(Condition condition) {
        if (condition instanceof FlagCondition fc) {
            Object expected = fc.equals() == null ? Boolean.TRUE : fc.equals();
            return Objects.equals(isFlagSet(fc.flag()), expected);
        }
        throw new UnsupportedOperationException(
                "Condition-Typ noch nicht unterstützt: " + condition.getClass().getSimpleName());
    }

    public void setCurrentLocationId(String locationId) {
        party.setLocationId(locationId);
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public Adventure getAdventure() {
        return adventure;
    }

    public String getCurrentLocationId() {
        return party.getLocationId();
    }
}
