package com.github.martinfrank.elitegames.auralis.game;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.Condition;
import com.github.martinfrank.elitegames.auralis.adventure.Flag;
import com.github.martinfrank.elitegames.auralis.adventure.FlagCondition;
import com.github.martinfrank.elitegames.auralis.adventure.Item;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.PersonPresence;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.adventure.QuestTask;
import com.github.martinfrank.elitegames.auralis.adventure.Questbook;
import com.github.martinfrank.elitegames.auralis.character.Party;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    public List<QuestTask> getOpenTasks(Quest quest) {
        Objects.requireNonNull(quest, "quest");
        List<QuestTask> tasks = quest.tasks();
        if (tasks == null) return List.of();
        return tasks.stream().filter(t -> !isTaskCompleted(t)).toList();
    }

    private boolean isTaskCompleted(QuestTask task) {
        return task.completionCondition() != null && evaluate(task.completionCondition());
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

    public List<Location> getRevealedLocations() {
        List<Location> all = adventure.content().locations();
        if (all == null) return List.of();
        return all.stream().filter(l -> isRevealed(l.revealed())).toList();
    }

    public List<Person> getRevealedPersons() {
        List<Person> all = adventure.content().persons();
        if (all == null) return List.of();
        return all.stream().filter(p -> isRevealed(p.revealed())).toList();
    }

    public List<Person> getPresentPersons(Location location) {
        Objects.requireNonNull(location, "location");
        List<PersonPresence> presences = location.persons();
        if (presences == null || presences.isEmpty()) return List.of();
        return presences.stream()
                .filter(pp -> pp.condition() == null || evaluate(pp.condition()))
                .map(pp -> findPerson(pp.personId()))
                .toList();
    }

    private Person findPerson(String id) {
        return adventure.content().persons().stream()
                .filter(p -> p.id().equals(id))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Person nicht gefunden: " + id));
    }

    public List<Item> getRevealedItems() {
        List<Item> all = adventure.content().items();
        if (all == null) return List.of();
        return all.stream().filter(i -> isRevealed(i.revealed())).toList();
    }

    private boolean isRevealed(String flagId) {
        return flagId == null || isFlagSet(flagId);
    }
}
