package com.github.martinfrank.elitegames.auralis.game;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.agent.Agents;
import com.github.martinfrank.elitegames.auralis.agent.SetupQuestAgent;
import com.github.martinfrank.elitegames.auralis.character.Adventurer;
import com.github.martinfrank.elitegames.auralis.character.Party;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

class GameEngineTest {

    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    //    private static final String MODEL = "gemma4:e2b";
//    private static final String MODEL = "mistral:7b";
//    private static final String MODEL = "deepseek-r1:14b";
    private static final String MODEL = "qwen2.5:7b"; //gut
    //    private static final String MODEL = "llama3.1:8b"; //gut

    @Test
    void testGameEngine() throws IOException {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
        Agents agents = new Agents();
        agents.setSetupQuestAgent(new SetupQuestAgent(model));


        Adventure adventure = new AdventureReader().read(GameEngineTest.class.getResourceAsStream(RESOURCE));
        Party party = new Party(List.of(new Adventurer("Thorsten Grambush")));
        GameEngine engine = new GameEngine(adventure, party, agents);

        engine.start();
    }

}