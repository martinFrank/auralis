package com.github.martinfrank.elitegames.auralis.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameChat {

    private static final Logger LOG = LoggerFactory.getLogger(GameChat.class);

    public enum Source { PLAYER, HEROLD, ADVENTURE }

    public record Turn(String nodeId, Source source, String content) {}

    private final List<Turn> history = new ArrayList<>();

    public List<Turn> transcript() {
        return Collections.unmodifiableList(history);
    }

    public void addPlayerMessage(String nodeId, String playerInput) {
        add(nodeId, Source.PLAYER, playerInput);
    }

    public void addHeroldMessage(String nodeId, String message) {
        add(nodeId, Source.HEROLD, message);
    }

    public void addAdventureMessage(String nodeId, String message) {
        add(nodeId, Source.ADVENTURE, message);
    }

    private void add(String nodeId, Source source, String content) {
        Turn turn = new Turn(nodeId, source, content);
        LOG.info("[{}] {}", source, content);
        history.add(turn);
    }
}
