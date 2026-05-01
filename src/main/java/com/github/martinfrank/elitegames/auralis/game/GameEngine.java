package com.github.martinfrank.elitegames.auralis.game;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.Flag;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.PersonPresence;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionResponseAgent;
import com.github.martinfrank.elitegames.auralis.agent.Agents;
import com.github.martinfrank.elitegames.auralis.agent.chat.AmbientResponseAgent;
import com.github.martinfrank.elitegames.auralis.agent.chat.ClassifyInputAgent;
import com.github.martinfrank.elitegames.auralis.agent.chat.ClassifyInputAgent.Classification;
import com.github.martinfrank.elitegames.auralis.agent.chat.DialogResponseAgent;
import com.github.martinfrank.elitegames.auralis.agent.chat.OocResponseAgent;
import com.github.martinfrank.elitegames.auralis.agent.chat.QuestionResponseAgent;
import com.github.martinfrank.elitegames.auralis.agent.chat.UnclearResponseAgent;
import com.github.martinfrank.elitegames.auralis.character.Party;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class GameEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GameEngine.class);
    private static final int HISTORY_WINDOW = 10;

    private final GameSession gameSession;
    private final Agents agents;

    public GameEngine(Adventure adventure, Party party, Agents agents) {
        this.gameSession = new GameSession(adventure, party);
        this.agents = agents;
    }

    public void start() {
        Quest current = gameSession.getCurrentQuest();

        gameSession.getChat().addHeroldMessage("statischer Introtext",
                "willkommen bei dem Abenteuer \"" + gameSession.getAdventure().title()
                        + "\" von " + gameSession.getAdventure().author());
        gameSession.getChat().addHeroldMessage("statischer Introtext",
                gameSession.getAdventure().description());

        startQuest(current);

        Scanner scanner = new Scanner(System.in);
        for (int i = 0; i < 10 && scanner.hasNextLine(); i++) {
            String playerInput = scanner.nextLine();
            gameSession.getChat().addPlayerMessage(playerInput);

            handleTurn(playerInput);

            Quest next = gameSession.getCurrentQuest();
            if (next != null && (current == null || !Objects.equals(current.id(), next.id()))) {
                startQuest(next);
                current = next;
            }
        }
    }

    private void startQuest(Quest quest) {
        gameSession.setCurrentLocationId(quest.startLocationId());
        gameSession.setCurrentTime(quest.startTime());

        Location location = gameSession.getAdventure().getLocation(gameSession.getCurrentLocationId());
        String setup = agents.getSetupQuestAgent().setupQuest(quest, location, quest.startTime());
        gameSession.getChat().addHeroldMessage(
                "AI generiert Text für das Einleiten eines neuen quests", setup);
        gameSession.getChat().addHeroldMessage(
                "quest-location allgemeine beschreibung", location.generalInfo());
    }

    private void handleTurn(String playerInput) {
        Quest quest = gameSession.getCurrentQuest();
        Location location = currentLocation();
        List<Person> persons = presentPersons(location);
        List<GameChat.Turn> history = recentHistory();

        Classification classification = agents.getClassifyInputAgent().classify(
                new ClassifyInputAgent.Context(quest, location, persons, history, playerInput));
        LOG.info("Klassifikation: {} | {} | target={}/{}",
                classification.category(), classification.reasoning(),
                classification.targetType(), classification.targetId());

        switch (classification.category()) {
            case QUESTION -> handleQuestion(classification, quest, location, persons, history, playerInput);
            case AMBIENT  -> handleAmbient(classification, quest, location, persons, history, playerInput);
            case DIALOG   -> handleDialog(classification, quest, location, persons, history, playerInput);
            case ACTION   -> handleAction(classification, quest, location, persons, history, playerInput);
            case OOC      -> handleOoc(classification, quest, location, playerInput);
            case UNCLEAR  -> handleUnclear(classification, quest, location, persons, playerInput);
        }
    }

    private void handleQuestion(Classification c, Quest quest, Location location,
                                List<Person> persons, List<GameChat.Turn> history, String playerInput) {
        QuestionResponseAgent.Context ctx = new QuestionResponseAgent.Context(
                location, quest, persons, gameSession.getCurrentTime(),
                gameSession.getRevealedLocations(),
                gameSession.getRevealedPersons(),
                gameSession.getRevealedItems(),
                history, c.hints(), playerInput);
        String reply = agents.getQuestionResponseAgent().respond(ctx);
        gameSession.getChat().addHeroldMessage("[QUESTION] " + c.reasoning(), reply);
    }

    private void handleAmbient(Classification c, Quest quest, Location location,
                               List<Person> persons, List<GameChat.Turn> history, String playerInput) {
        AmbientResponseAgent.Context ctx = new AmbientResponseAgent.Context(
                location, quest, persons, gameSession.getCurrentTime(),
                gameSession.getFlags(), history, c.hints(), playerInput);
        String reply = agents.getAmbientResponseAgent().respond(ctx);
        gameSession.getChat().addHeroldMessage("[AMBIENT] " + c.reasoning(), reply);
    }

    private void handleDialog(Classification c, Quest quest, Location location,
                              List<Person> persons, List<GameChat.Turn> history, String playerInput) {
        Person addressed = persons.stream()
                .filter(p -> p.id().equals(c.targetId()))
                .findFirst()
                .orElse(null);
        if (addressed == null) {
            LOG.warn("DIALOG ohne aufloesbares Target (targetId={}), downgrade auf UNCLEAR", c.targetId());
            UnclearResponseAgent.Context ctx = new UnclearResponseAgent.Context(
                    quest, location, persons,
                    "Adressat unklar — bitte konkret benennen, wen du ansprichst.",
                    playerInput);
            String reply = agents.getUnclearResponseAgent().respond(ctx);
            gameSession.getChat().addHeroldMessage("[DIALOG → UNCLEAR] kein Target", reply);
            return;
        }
        List<Person> others = persons.stream()
                .filter(p -> !p.id().equals(addressed.id()))
                .toList();
        DialogResponseAgent.Context ctx = new DialogResponseAgent.Context(
                addressed, location, others, gameSession.getCurrentTime(),
                history, c.hints(), playerInput);
        String reply = agents.getDialogResponseAgent().respond(ctx);
        gameSession.getChat().addHeroldMessage("[DIALOG] " + addressed.name(), reply);
    }

    private void handleAction(Classification c, Quest quest, Location location,
                              List<Person> persons, List<GameChat.Turn> history, String playerInput) {
        ActionJudgeAgent.Verdict verdict = agents.getActionJudgeAgent().judge(
                new ActionJudgeAgent.Context(quest, location, gameSession.getFlags(),
                        c.hints(), playerInput));
        applyFlagChanges(verdict);

        ActionResponseAgent.Context ctx = new ActionResponseAgent.Context(
                verdict, quest, location, persons, gameSession.getCurrentTime(),
                history, c.hints(), playerInput);
        String reply = agents.getActionResponseAgent().respond(ctx);
        gameSession.getChat().addHeroldMessage("[ACTION] " + verdict.summary(), reply);
    }

    private void handleOoc(Classification c, Quest quest, Location location, String playerInput) {
        OocResponseAgent.Context ctx = new OocResponseAgent.Context(
                quest, location, gameSession.getCurrentTime(), c.hints(), playerInput);
        String reply = agents.getOocResponseAgent().respond(ctx);
        gameSession.getChat().addHeroldMessage("[OOC] " + c.reasoning(), reply);
    }

    private void handleUnclear(Classification c, Quest quest, Location location,
                               List<Person> persons, String playerInput) {
        UnclearResponseAgent.Context ctx = new UnclearResponseAgent.Context(
                quest, location, persons, c.hints(), playerInput);
        String reply = agents.getUnclearResponseAgent().respond(ctx);
        gameSession.getChat().addHeroldMessage("[UNCLEAR] " + c.reasoning(), reply);
    }

    private void applyFlagChanges(ActionJudgeAgent.Verdict verdict) {
        Set<String> knownFlags = gameSession.getAdventure().content().flags().stream()
                .map(Flag::id)
                .collect(Collectors.toSet());
        for (ActionJudgeAgent.FlagChange fc : verdict.flagChanges()) {
            if (!knownFlags.contains(fc.flagId())) {
                LOG.warn("Judge schlaegt unbekanntes Flag vor (verworfen): id={} value={} grund={}",
                        fc.flagId(), fc.value(), fc.reason());
                continue;
            }
            if (gameSession.isFlagSet(fc.flagId()) == fc.value()) {
                LOG.info("Judge schlaegt no-op vor (Flag schon im gewuenschten Zustand): id={} value={}",
                        fc.flagId(), fc.value());
                continue;
            }
            LOG.info("Judge setzt Flag: id={} {} -> {} (grund: {})",
                    fc.flagId(), gameSession.isFlagSet(fc.flagId()), fc.value(), fc.reason());
            gameSession.setFlag(fc.flagId(), fc.value());
        }
    }

    private Location currentLocation() {
        return gameSession.getAdventure().getLocation(gameSession.getCurrentLocationId());
    }

    private List<Person> presentPersons(Location location) {
        if (location.persons() == null || location.persons().isEmpty()) {
            return List.of();
        }
        return location.persons().stream()
                .map(PersonPresence::personId)
                .map(id -> gameSession.getAdventure().content().persons().stream()
                        .filter(p -> p.id().equals(id))
                        .findAny().orElseThrow(() ->
                                new IllegalStateException("Person nicht gefunden: " + id)))
                .toList();
    }

    private List<GameChat.Turn> recentHistory() {
        List<GameChat.Turn> all = gameSession.getChat().transcript();
        if (all.size() <= HISTORY_WINDOW) return all;
        return all.subList(all.size() - HISTORY_WINDOW, all.size());
    }
}
