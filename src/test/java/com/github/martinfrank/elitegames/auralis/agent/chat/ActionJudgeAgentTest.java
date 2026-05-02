package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.Context;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.FlagChange;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.Verdict;
import com.github.martinfrank.elitegames.auralis.character.Adventurer;
import com.github.martinfrank.elitegames.auralis.character.Party;
import com.github.martinfrank.elitegames.auralis.game.GameChat;
import com.github.martinfrank.elitegames.auralis.game.GameSession;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionJudgeAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String TANZENDEN_STIER_LOCATION_ID = "e5815fa0-1885-46a1-b477-ea5ddcee04e2";
    private static final String TANZENDEN_STIER_QUEST_ID = "5ce0f5a5-a9d4-467a-bfc8-85019aab6aff";

    @Test
    void testWalkToDancingBull() throws IOException {
        InputStream in = ClassifyInputAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest("7a91e163-860f-4db0-b231-08d76123afdb");
        Location location = adventure.getLocation(quest.startLocationId());
        GameSession session = new GameSession(adventure, new Party(List.of(new Adventurer("Rolf"))));

        String playerAction = "wir machen uns auf, um die Taverne zum tanzenden Stier zu besuchen.";

        //aktueller chatverlauf bisher
        List<GameChat.Turn> conversation = List.of(
                new GameChat.Turn(GameChat.Source.HEROLD, "statischer Introtext",
                        """
                                willkommen bei dem Abenteuer "Verführung zur Entführung" von Oliver Eickenberg (1997)
                                """),
                new GameChat.Turn(GameChat.Source.HEROLD, "AI generiert Text für das Einleiten eines neuen quests",
                        """
                                Nun findet ihr euch am Zentralen Platz im Handelsviertel von Gareth. Der Nachmittag zieht
                                sich langsam dahin, und die Stadt lebt in voller Pracht. Blicke empor zu dem prächtigen
                                Brunnen, dessen Wasserstrahlen in der sanften Abendsonne funkeln und das Licht spiegeln. Um
                                euch herum drängt sich die Menge – Händler, Handwerker, Adlige – alle bereit für den
                                abendlichen Ausflug.
                                
                                Fragt umher, und ihr werdet hören, dass man von der Taverne zum tanzenden Stier spricht, die
                                das beste Gasthaus der Stadt ist. Es liegt direkt hier in der Nähe, und es ist noch früh
                                genug, um vor dem Eintritt des Abendlichts ein wenig umherzuschnüffeln oder vielleicht
                                einige Einkäufe zu erledigen, bevor ihr euch entscheidet, ob das Geleise in die Taverne
                                geführt hat.
                                """),
                new GameChat.Turn(GameChat.Source.HEROLD, "quest-location allgemeine beschreibung",
                        """
                                Der Marktplatz liegt inmitten der Kaiserstadt und ist der Herzpunkt aller Aktivitäten.
                                Er wird von einem großen, prächtigen Brunnen dominiert, der mit Figuren aus Bronze
                                geschmückt ist. Die Wasserstrahlen des Brunnens springen aufwärts in einem bunten Spiel
                                aus Licht und Wasser, beleuchtet durch das sanfte Licht, das von den umliegenden
                                Gebäuden reflektiert wird. Um den Marktplatz herum stehen prachtvolle Gebäude, in denen
                                sich luxuriöse Geschäfte, Gasthöfe und sogar das königliche Amt befinden
                                
                                Der Platz ist ein Treffpunkt für alle Schichten der Gesellschaft. An den Ständen des
                                Marktes bietet man alles vom frischesten Obst und Gemüse bis hin zu hochwertigen
                                Edelsteinen an. Die Atmosphäre ist lebendig, mit dem Läuten von Glöckchen bei jedem
                                neuen Kauf, den Gesang der Händlerinnen beim Verkauf ihrer Waren und das Gelächter des
                                Volksgeschehens.
                                """),
                new GameChat.Turn(GameChat.Source.HEROLD, "eingabe des spielers", playerAction)
        );

        //Klassifikation vom ClassificationAgent
        ClassifyInputAgent.Classification classifikation = new ClassifyInputAgent.Classification(
                ClassifyInputAgent.Category.ACTION,
                "Der Spieler beschreibt eine Bewegung in Richtung der Taverne.",
                ClassifyInputAgent.TargetType.LOCATION,
                "->",
                "Setze die Helden auf den Weg zur Taverne zum tanzenden Stier.",
                """
                        KATEGORIE: ACTION
                        BEGRUENDUNG: Der Spieler beschreibt eine Bewegung in Richtung der Taverne.
                        TARGET_TYP: TRANSITION
                        TARGET_ID: ->
                        HINWEIS: Setze die Helden auf den Weg zur Taverne zum tanzenden Stier.
                        """
        );

        Context context = new Context(
                quest,
                location,
                session.getFlags(),
                "afternoon",
                List.of("evening", "night", "midnight"),
                "Setze die Helden auf den Weg zur Taverne zum tanzenden Stier.",
                playerAction
        );

        long start = System.currentTimeMillis();
        ChatLanguageModel model = chatModel();
        ActionJudgeAgent agent = new ActionJudgeAgent(model);

        Verdict verdict = agent.judge(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== ZUSAMMENFASSUNG ===");
        System.out.println(verdict.summary());
        System.out.println();
        System.out.println("=== FLAG-AENDERUNGEN ===");
        for (FlagChange fc : verdict.flagChanges()) {
            System.out.println("- " + fc.flagId() + " = " + fc.value() + "  (grund: " + fc.reason() + ")");
        }
        System.out.println();
        System.out.println("=== ROHANTWORT ===");
        System.out.println(verdict.raw());
        System.out.println("took " + duration + "ms");
    }

    @Test
    void parsesVerdictWithMultipleFlagChanges() {
        String raw = """
                ZUSAMMENFASSUNG: Spieler feiert ausgelassen und bleibt bis Mitternacht.
                BEWEGUNG: keine grund="Gruppe bleibt in der Taverne"
                ZEIT: midnight grund="Feier zieht sich bis spaet"
                FLAG_AENDERUNGEN:
                - id=party_bonus wert=true grund="Spieler feiert aktiv mit"
                - id=await_arberds_intro wert=true grund="Spieler bleibt bis spaet abends"
                """;

        Verdict v = ActionJudgeAgent.parse(raw);

        assertEquals("Spieler feiert ausgelassen und bleibt bis Mitternacht.", v.summary());
        assertNull(v.locationChange());
        assertNotNull(v.timeChange());
        assertEquals("midnight", v.timeChange().newTime());
        assertEquals(2, v.flagChanges().size());
        assertEquals(new FlagChange("party_bonus", true, "Spieler feiert aktiv mit"), v.flagChanges().get(0));
        assertEquals("await_arberds_intro", v.flagChanges().get(1).flagId());
    }

    @Test
    void parsesVerdictWithEmptyFlagList() {
        String raw = """
                ZUSAMMENFASSUNG: Spieler probiert eine Aktion, die nichts veraendert.
                BEWEGUNG: keine
                ZEIT: keine
                FLAG_AENDERUNGEN:
                """;

        Verdict v = ActionJudgeAgent.parse(raw);

        assertTrue(v.flagChanges().isEmpty());
        assertNull(v.locationChange());
        assertNull(v.timeChange());
    }

    @Test
    void parsesVerdictWithLocationChange() {
        String raw = """
                ZUSAMMENFASSUNG: Die Helden brechen zur Taverne auf.
                BEWEGUNG: tavern-id-123 grund="Die Gruppe macht sich auf den Weg"
                ZEIT: keine
                FLAG_AENDERUNGEN:
                - id=walk_to_tavern wert=true grund="Aufbruch zur Taverne"
                """;

        Verdict v = ActionJudgeAgent.parse(raw);

        assertNotNull(v.locationChange());
        assertEquals("tavern-id-123", v.locationChange().toLocationId());
        assertEquals("Die Gruppe macht sich auf den Weg", v.locationChange().reason());
        assertNull(v.timeChange());
    }

    @Test
    void stripsThinkingAndStillParses() {
        String raw = """
                <think>
                Hmm, der Spieler stellt sich an die Theke und feiert.
                Das setzt party_bonus.
                </think>
                ZUSAMMENFASSUNG: Spieler stuerzt sich in die Feierei.
                BEWEGUNG: keine
                ZEIT: keine
                FLAG_AENDERUNGEN:
                - id=party_bonus wert=true grund="Mitgesungen, mitgetrunken"
                """;

        Verdict v = ActionJudgeAgent.parse(raw);

        assertEquals(1, v.flagChanges().size());
        assertEquals("party_bonus", v.flagChanges().get(0).flagId());
    }

    @Test
    void judgesPartyAction() throws IOException {
        InputStream in = ActionJudgeAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest(TANZENDEN_STIER_QUEST_ID);
        Location location = adventure.getLocation(TANZENDEN_STIER_LOCATION_ID);

        Map<String, Boolean> flags = new HashMap<>();
        flags.put("walk_to_tavern", true);
        flags.put("party_bonus", false);
        flags.put("await_arberds_intro", false);
        flags.put("brawling_bonus", false);
        flags.put("escape_the_brawl", false);
        flags.put("found_arberds_address", false);
        flags.put("ready_to_visit_arberds", false);

        Context context = new Context(
                quest,
                location,
                flags,
                "evening",
                List.of("night", "midnight"),
                "Spieler beteiligt sich aktiv an der Feierei in der Taverne.",
                "Wir bestellen reichlich Bier, stossen mit den Trunkenbolden an und singen mit."
        );

        ChatLanguageModel model = chatModel();
        ActionJudgeAgent agent = new ActionJudgeAgent(model);

        long start = System.currentTimeMillis();
        Verdict verdict = agent.judge(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== ZUSAMMENFASSUNG ===");
        System.out.println(verdict.summary());
        System.out.println();
        System.out.println("=== FLAG-AENDERUNGEN ===");
        for (FlagChange fc : verdict.flagChanges()) {
            System.out.println("- " + fc.flagId() + " = " + fc.value() + "  (grund: " + fc.reason() + ")");
        }
        System.out.println();
        System.out.println("=== ROHANTWORT ===");
        System.out.println(verdict.raw());
        System.out.println("took " + duration + "ms");
    }

    private static ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
