package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.agent.HeroldAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AdventureSession {

    public enum Source { PLAYER, HEROLD, ADVENTURE }

    public record Turn(Source source, String content) {}

    private final List<Turn> turns = new ArrayList<>();

    public List<Turn> transcript() {
        return Collections.unmodifiableList(turns);
    }

    public void addPlayerMessage(String playerInput) {
        turns.add(new Turn(Source.PLAYER, playerInput));
    }

    public void addAdventureMessage(String message) {
        turns.add(new Turn(Source.ADVENTURE, message));
    }

    public void addHeroldMessage(String message) {
        turns.add(new Turn(Source.HEROLD, message));
    }

}
