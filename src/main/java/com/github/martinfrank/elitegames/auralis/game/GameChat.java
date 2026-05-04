package com.github.martinfrank.elitegames.auralis.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameChat {

    private static final Logger LOG = LoggerFactory.getLogger(GameChat.class);

    public enum Source { PLAYER, HEROLD, ADVENTURE }

    public record Turn(Source source, String explanation, String content) {}

    private final List<Turn> history = new ArrayList<>();

    public List<Turn> transcript() {
        return Collections.unmodifiableList(history);
    }

    public void addPlayerMessage(String playerInput) {
        add(Source.PLAYER, "", playerInput);
    }

    public void addHeroldMessage(String explanation, String message) {
        add(Source.HEROLD, explanation, message);
    }

    private void add(Source source, String msgInfo, String content) {
        Turn turn = new Turn(source, msgInfo, content);
        LOG.debug("[{}] [{}] {}", source, msgInfo, content);
        LOG.info(content);
        history.add(turn);
    }
}
