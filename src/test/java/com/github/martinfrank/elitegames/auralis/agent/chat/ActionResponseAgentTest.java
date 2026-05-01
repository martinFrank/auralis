package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.FlagChange;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.Verdict;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionResponseAgent.Context;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

class ActionResponseAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String TANZENDEN_STIER_LOCATION_ID = "e5815fa0-1885-46a1-b477-ea5ddcee04e2";
    private static final String TANZENDEN_STIER_QUEST_ID = "5ce0f5a5-a9d4-467a-bfc8-85019aab6aff";

    @Test
    void narratesActionWithFlagFlip() throws IOException {
        Context context = loadContext(
                new Verdict(
                        "Die Helden stuerzen sich in die Feier und trinken reichlich.",
                        List.of(new FlagChange("party_bonus", true,
                                "Spieler beteiligt sich aktiv an der Feierei")),
                        ""),
                "Wir bestellen reichlich Bier, stossen mit den Trunkenbolden an und singen mit.",
                "Beschreibe die Feier-Szene, der Spieler ist dabei.");
        runAndPrint(context);
    }

    @Test
    void narratesActionWithoutFlagFlip() throws IOException {
        Context context = loadContext(
                new Verdict(
                        "Spieler probiert eine Aktion, die nichts veraendert.",
                        List.of(),
                        ""),
                "Ich versuche, das Wappen ueber der Tuer von innen zu drehen.",
                "Aktion ohne Spielmechanik-Auswirkung, atmosphaerisch beschreiben.");
        runAndPrint(context);
    }

    private static Context loadContext(Verdict verdict, String playerInput, String hints) throws IOException {
        InputStream in = ActionResponseAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);
        Quest quest = adventure.getQuest(TANZENDEN_STIER_QUEST_ID);
        Location location = adventure.getLocation(TANZENDEN_STIER_LOCATION_ID);
        return new Context(
                verdict, quest, location, List.of(), "abends",
                List.of(), hints, playerInput);
    }

    private static void runAndPrint(Context context) {
        ChatLanguageModel model = chatModel();
        ActionResponseAgent agent = new ActionResponseAgent(model);

        long start = System.currentTimeMillis();
        String reply = agent.respond(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== EINGABE ===");
        System.out.println(context.playerInput());
        System.out.println("=== VERDICT ===");
        System.out.println(context.verdict());
        System.out.println("=== ANTWORT ===");
        System.out.println(reply);
        System.out.println("took " + duration + "ms");
        System.out.println();
    }

    private static ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
