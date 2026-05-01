package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.Context;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.FlagChange;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.Verdict;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionJudgeAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String TANZENDEN_STIER_LOCATION_ID = "e5815fa0-1885-46a1-b477-ea5ddcee04e2";
    private static final String TANZENDEN_STIER_QUEST_ID = "5ce0f5a5-a9d4-467a-bfc8-85019aab6aff";

    @Test
    void parsesVerdictWithMultipleFlagChanges() {
        String raw = """
                ZUSAMMENFASSUNG: Spieler feiert ausgelassen und bleibt bis Mitternacht.
                FLAG_AENDERUNGEN:
                - id=party_bonus wert=true grund="Spieler feiert aktiv mit"
                - id=await_arberds_intro wert=true grund="Spieler bleibt bis spaet abends"
                """;

        Verdict v = ActionJudgeAgent.parse(raw);

        assertEquals("Spieler feiert ausgelassen und bleibt bis Mitternacht.", v.summary());
        assertEquals(2, v.flagChanges().size());
        assertEquals(new FlagChange("party_bonus", true, "Spieler feiert aktiv mit"), v.flagChanges().get(0));
        assertEquals("await_arberds_intro", v.flagChanges().get(1).flagId());
    }

    @Test
    void parsesVerdictWithEmptyFlagList() {
        String raw = """
                ZUSAMMENFASSUNG: Spieler probiert eine Aktion, die nichts veraendert.
                FLAG_AENDERUNGEN:
                """;

        Verdict v = ActionJudgeAgent.parse(raw);

        assertTrue(v.flagChanges().isEmpty());
    }

    @Test
    void stripsThinkingAndStillParses() {
        String raw = """
                <think>
                Hmm, der Spieler stellt sich an die Theke und feiert.
                Das setzt party_bonus.
                </think>
                ZUSAMMENFASSUNG: Spieler stuerzt sich in die Feierei.
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
