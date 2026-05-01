package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.PersonPresence;
import com.github.martinfrank.elitegames.auralis.agent.chat.DialogResponseAgent.Context;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

class DialogResponseAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String WALFISCH_ID = "0bd69e14-713c-4c65-ab28-898eb9d4a381";
    private static final String GUNDOLF_ID = "aa5f1044-f47c-4dd5-8dca-761a2aa28b6f";

    @Test
    void gundolfAnswersAboutVater() throws IOException {
        InputStream in = DialogResponseAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Location location = adventure.getLocation(WALFISCH_ID);
        List<Person> allPresent = location.persons().stream()
                .map(PersonPresence::personId)
                .map(id -> adventure.content().persons().stream()
                        .filter(p -> p.id().equals(id))
                        .findAny().orElseThrow())
                .toList();
        Person gundolf = allPresent.stream()
                .filter(p -> p.id().equals(GUNDOLF_ID))
                .findAny().orElseThrow();
        List<Person> others = allPresent.stream()
                .filter(p -> !p.id().equals(GUNDOLF_ID))
                .toList();

        Context context = new Context(
                gundolf,
                location,
                others,
                "spaet abends",
                List.of(),
                "Spieler fragt nach dem verzweifelten Vater. Gundolf weiss Bescheid und darf antworten.",
                "Sagt, Wirt — kennt Ihr einen Vater, der seine Tochter sucht? Er soll vorhin durch die Tavernen gezogen sein."
        );

        ChatLanguageModel model = chatModel();
        DialogResponseAgent agent = new DialogResponseAgent(model);

        long start = System.currentTimeMillis();
        String reply = agent.respond(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== EINGABE ===");
        System.out.println(context.playerInput());
        System.out.println("=== ANTWORT (Gundolf) ===");
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
