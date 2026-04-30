package com.github.martinfrank.elitegames.auralis.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HeroldAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
//    private static final String MODEL = "gemma4:e2b";
//    private static final String MODEL = "mistral:7b";
//    private static final String MODEL = "deepseek-r1:14b";
    private static final String MODEL = "qwen2.5:7b"; //gut
//    private static final String MODEL = "llama3.1:8b"; //gut

    @Test
    void respondsToAdventureMarkdown() throws IOException {
        String adventure = loadResource("entfuehr.md");

//        String userMessage = """
//                Kannst du für eine Spielgruppe die Einleitung beschreiben?
//                Erfinde keine zusätzlichen Informationen, ausser den Informationen,
//                die im Abenteuerbuch beschrieben sind.
//
//                hier ist das Abentuerbuch:
//                """;
//
//        userMessage = userMessage + adventure;


        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
        HeroldAgent herold = new HeroldAgent(model, adventure, "Eine fröhliche Taverne");

        System.out.println("=== LLM HOST: " + OLLAMA_URL + " / MODEL: " + MODEL + " ===");

        String turn1 = "Ich bin ein fahrender Held und bereit, das Abenteuer zu beginnen. "
                + "Leite die Eroeffnungsszene ein.";
        String reply1 = herold.chat(turn1);
        printTurn(1, turn1, reply1);

        String turn2 = "Ich dränge mich durch die Menge zur Theke und spreche den Wirt an.";
        String reply2 = herold.chat(turn2);
        printTurn(2, turn2, reply2);

        assertNotNull(reply1, "Agent returned null on turn 1");
        assertFalse(reply1.isBlank(), "Agent returned blank response on turn 1");
        assertNotNull(reply2, "Agent returned null on turn 2");
        assertFalse(reply2.isBlank(), "Agent returned blank response on turn 2");
    }

    private static void printTurn(int n, String playerAction, String reply) {
        System.out.println();
        System.out.println("=== TURN " + n + " — PLAYER ===");
        System.out.println(playerAction);
        System.out.println("=== TURN " + n + " — HEROLD ===");
        System.out.println(reply);
    }

    private static String loadResource(String path) throws IOException {
        InputStream stream = Objects.requireNonNull(
                HeroldAgentTest.class.getClassLoader().getResourceAsStream(path),
                () -> "Resource not found on test classpath: " + path);
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
