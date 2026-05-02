package com.github.martinfrank.elitegames.auralis.game;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.Flag;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.adventure.Transition;
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

    static final List<String> TIME_ORDER = List.of(
            "morning", "noon", "afternoon", "evening", "night", "midnight");

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
        List<Person> persons = gameSession.getPresentPersons(location);
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
                location, quest,
                quest == null ? List.of() : gameSession.getOpenTasks(quest),
                persons, gameSession.getCurrentTime(),
                history, c.hints(), playerInput);
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
                        gameSession.getCurrentTime(), forwardTimes(gameSession.getCurrentTime()),
                        c.hints(), playerInput));
        applyVerdict(verdict, location);

        Location locationAfter = currentLocation();
        List<Person> personsAfter = gameSession.getPresentPersons(locationAfter);
        ActionResponseAgent.Context ctx = new ActionResponseAgent.Context(
                verdict, quest, locationAfter, personsAfter, gameSession.getCurrentTime(),
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

    private void applyVerdict(ActionJudgeAgent.Verdict verdict, Location locationBefore) {
        applyFlagChanges(verdict);
        applyLocationChange(verdict.locationChange(), locationBefore);
        applyTimeChange(verdict.timeChange());
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

    private void applyLocationChange(ActionJudgeAgent.LocationChange lc, Location locationBefore) {
        if (lc == null) return;
        Set<String> reachable = locationBefore == null || locationBefore.transitions() == null
                ? Set.of()
                : locationBefore.transitions().stream().map(Transition::to).collect(Collectors.toSet());
        if (!reachable.contains(lc.toLocationId())) {
            LOG.warn("Judge schlaegt nicht erreichbare Location vor (verworfen): to={} grund={} | erreichbar={}",
                    lc.toLocationId(), lc.reason(), reachable);
            return;
        }
        if (lc.toLocationId().equals(gameSession.getCurrentLocationId())) {
            LOG.info("Judge schlaegt Bewegung zur aktuellen Location vor (no-op): to={}", lc.toLocationId());
            return;
        }
        LOG.info("Judge bewegt Gruppe: {} -> {} (grund: {})",
                gameSession.getCurrentLocationId(), lc.toLocationId(), lc.reason());
        gameSession.setCurrentLocationId(lc.toLocationId());
    }

    private void applyTimeChange(ActionJudgeAgent.TimeChange tc) {
        if (tc == null) return;
        String current = gameSession.getCurrentTime();
        int currentIdx = TIME_ORDER.indexOf(current);
        int newIdx = TIME_ORDER.indexOf(tc.newTime());
        if (newIdx < 0) {
            LOG.warn("Judge schlaegt unbekannte Tageszeit vor (verworfen): newTime={} grund={} | erlaubt={}",
                    tc.newTime(), tc.reason(), TIME_ORDER);
            return;
        }
        if (currentIdx < 0) {
            LOG.warn("Aktuelle Tageszeit nicht in TIME_ORDER: current={} — setze auf {} (grund: {})",
                    current, tc.newTime(), tc.reason());
            gameSession.setCurrentTime(tc.newTime());
            return;
        }
        if (newIdx == currentIdx) {
            LOG.info("Judge schlaegt unveraenderte Tageszeit vor (no-op): {}", tc.newTime());
            return;
        }
        if (newIdx < currentIdx) {
            LOG.warn("Judge schlaegt Zeitumkehr vor (verworfen): {} -> {} grund={}",
                    current, tc.newTime(), tc.reason());
            return;
        }
        LOG.info("Judge setzt Tageszeit: {} -> {} (grund: {})", current, tc.newTime(), tc.reason());
        gameSession.setCurrentTime(tc.newTime());
    }

    static List<String> forwardTimes(String currentTime) {
        int idx = TIME_ORDER.indexOf(currentTime);
        if (idx < 0) return TIME_ORDER;
        return TIME_ORDER.subList(idx + 1, TIME_ORDER.size());
    }

    private Location currentLocation() {
        return gameSession.getAdventure().getLocation(gameSession.getCurrentLocationId());
    }

    private List<GameChat.Turn> recentHistory() {
        List<GameChat.Turn> all = gameSession.getChat().transcript();
        if (all.size() <= HISTORY_WINDOW) return all;
        return all.subList(all.size() - HISTORY_WINDOW, all.size());
    }
}
