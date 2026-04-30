package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.PersonPresence;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.QuestionResponseAgent.Context;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

class QuestionResponseAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String WALFISCH_ID = "0bd69e14-713c-4c65-ab28-898eb9d4a381";
    private static final String SUCHE_QUEST_ID = "1f5858e2-a28c-4f2b-b435-01b9051adf20";

    @Test
    void answersQuestionAboutInnkeeper() throws IOException {
        InputStream in = QuestionResponseAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest(SUCHE_QUEST_ID);
        Location location = adventure.getLocation(WALFISCH_ID);
        List<Person> present = location.persons().stream()
                .map(PersonPresence::personId)
                .map(id -> adventure.content().persons().stream()
                        .filter(p -> p.id().equals(id))
                        .findAny().orElseThrow())
                .toList();

        Context context = new Context(
                location,
                quest,
                present,
                "spaet abends",
                Map.of(
                        "walk_to_tavern", true,
                        "await_arberds_intro", true,
                        "escape_the_brawl", true,
                        "found_arberds_address", false,
                        "ready_to_visit_arberds", false,
                        "party_bonus", false,
                        "brawling_bonus", false
                ),
                List.of(),
                "Antworte als Spielleiter, beschreibe den Wirt anhand seiner allgemeinen Informationen.",
                "Was kannst du mir ueber den Wirt erzaehlen?"
        );

        ChatLanguageModel model = chatModel();
        QuestionResponseAgent agent = new QuestionResponseAgent(model);

        long start = System.currentTimeMillis();
        String reply = agent.respond(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== ANTWORT ===");
        System.out.println(reply);
        System.out.println();
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
