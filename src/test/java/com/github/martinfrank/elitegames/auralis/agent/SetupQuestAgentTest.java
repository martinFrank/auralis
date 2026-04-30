package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

public class SetupQuestAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
//    private static final String MODEL = "qwen3:30b";
    private static final String MODEL = "qwen2.5:7b"; //gut
    //    private static final String MODEL = "llama3.1:8b"; //gut
//    private static final String MODEL = "qwen3:32b"; //von cahtgpt empfohlen - SPILL
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    @Test
    void testFirstQuest() throws IOException {

        InputStream in = SetupQuestAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        ChatLanguageModel model = chatModel();
        SetupQuestAgent agent = new SetupQuestAgent(model);

        Quest quest = adventure.getQuest("7a91e163-860f-4db0-b231-08d76123afdb");
        Location location = adventure.getLocation(quest.startLocationId());
        String setup = agent.setupQuest(quest, location, quest.startTime());

        System.out.println(setup);
        System.out.println(location.generalInfo());
    }

    @Test
    void testSecondQuest() throws IOException {

        InputStream in = SetupQuestAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        ChatLanguageModel model = chatModel();
        SetupQuestAgent agent = new SetupQuestAgent(model);

        Quest quest = adventure.getQuest("5ce0f5a5-a9d4-467a-bfc8-85019aab6aff");
        Location location = adventure.getLocation(quest.startLocationId());
        String setup = agent.setupQuest(quest, location, quest.startTime());

        System.out.println(setup);
        System.out.println(location.generalInfo());
    }

    private static ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

}
