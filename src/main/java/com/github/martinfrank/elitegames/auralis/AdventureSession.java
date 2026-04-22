package com.github.martinfrank.elitegames.auralis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdventureSession {

    private static final Logger LOG = LoggerFactory.getLogger(AdventureSession.class);

    public enum Source { PLAYER, HEROLD, ADVENTURE }

    public record Turn(Source source, String content) {}

    private final List<Turn> turns = new ArrayList<>();

    public List<Turn> transcript() {
        return Collections.unmodifiableList(turns);
    }

    public void addPlayerMessage(String playerInput) {
        add(Source.PLAYER, playerInput);
    }

    public void addAdventureMessage(String message) {
        add(Source.ADVENTURE, message);
    }

    public void addHeroldMessage(String message) {
        add(Source.HEROLD, message);
    }

    private void add(Source source, String content) {
        Turn turn = new Turn(source, content);
        LOG.info("[{}] {}", source, content);
        turns.add(turn);
    }
}
