package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.PersonPresence;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.UnclearResponseAgent.Context;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

class UnclearResponseAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String WALFISCH_ID = "0bd69e14-713c-4c65-ab28-898eb9d4a381";
    private static final String SUCHE_QUEST_ID = "1f5858e2-a28c-4f2b-b435-01b9051adf20";

    @Test
    void asksForClarificationOnGibberish() throws IOException {
        Context context = loadContext("asdfasdf",
                "Eingabe ist Buchstabensalat, hoeflich nachfragen.");
        runAndPrint(context);
    }

    @Test
    void asksForClarificationOnAmbiguousInput() throws IOException {
        Context context = loadContext("ich tue irgendwas",
                "Eingabe ist zu vage, biete konkrete Optionen an.");
        runAndPrint(context);
    }

    private static Context loadContext(String playerInput, String hints) throws IOException {
        InputStream in = UnclearResponseAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);
        Quest quest = adventure.getQuest(SUCHE_QUEST_ID);
        Location location = adventure.getLocation(WALFISCH_ID);
        List<Person> present = location.persons().stream()
                .map(PersonPresence::personId)
                .map(id -> adventure.content().persons().stream()
                        .filter(p -> p.id().equals(id))
                        .findAny().orElseThrow())
                .toList();
        return new Context(quest, location, present, hints, playerInput);
    }

    private static void runAndPrint(Context context) {
        ChatLanguageModel model = chatModel();
        UnclearResponseAgent agent = new UnclearResponseAgent(model);

        long start = System.currentTimeMillis();
        String reply = agent.respond(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== EINGABE ===");
        System.out.println(context.playerInput());
        System.out.println("=== ANTWORT ===");
        System.out.println(reply);
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
