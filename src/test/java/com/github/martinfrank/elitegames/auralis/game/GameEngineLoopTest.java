package com.github.martinfrank.elitegames.auralis.game;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.agent.Agents;
import com.github.martinfrank.elitegames.auralis.character.Adventurer;
import com.github.martinfrank.elitegames.auralis.character.Party;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

class GameEngineLoopTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    @Test
    void runsThreeTurnsThroughDispatch() throws IOException {
        Adventure adventure = new AdventureReader().read(
                GameEngineLoopTest.class.getResourceAsStream(RESOURCE));

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
        Agents agents = Agents.allWithDefaults(model);

        Party party = new Party(List.of(new Adventurer("Thorsten Grambush")));
        GameEngine engine = new GameEngine(adventure, party, agents);

        String scriptedInput = String.join("\n",
                "Ich strecke mich und atme tief durch.",
                "Hallo Wirt, was empfehlt Ihr heute?",
                "Wir bestellen reichlich Bier und stossen mit den Trunkenbolden an."
        ) + "\n";

        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(scriptedInput.getBytes(StandardCharsets.UTF_8)));
            long start = System.currentTimeMillis();
            engine.start();
            long duration = System.currentTimeMillis() - start;
            System.out.println();
            System.out.println("=== TOTAL ===");
            System.out.println("took " + duration + "ms");
        } finally {
            System.setIn(originalIn);
        }
    }
}
