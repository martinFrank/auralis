package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.agent.HeroldAgent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: App <adventure-file.md> [chapter-focus]");
            System.exit(1);
        }

        String adventure = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        String chapterFocus = args.length > 1 ? args[1] : null;

        HeroldAgent herold = chapterFocus != null
                ? new HeroldAgent(defaultModel(), adventure, chapterFocus)
                : new HeroldAgent(defaultModel(), adventure);

        System.out.println("=== Herold von Auralis — Spielsitzung ===");
        System.out.println("Abenteuer: " + args[0]);
        if (chapterFocus != null) {
            System.out.println("Fokus: " + chapterFocus);
        }
        System.out.println("Gib deine Aktion ein. '/quit' zum Beenden, '/reset' für neue Sitzung.");
        System.out.println();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            System.out.print("> ");
            String line = in.readLine();
            if (line == null || "/quit".equalsIgnoreCase(line.trim())) {
                System.out.println("Die Sitzung endet.");
                break;
            }
            if ("/reset".equalsIgnoreCase(line.trim())) {
                herold.resetMemory();
                System.out.println("(Sitzung zurückgesetzt.)");
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            String response = herold.chat(line);
            System.out.println();
            System.out.println(response);
            System.out.println();
        }
    }

    private static dev.langchain4j.model.chat.ChatLanguageModel defaultModel() {
        return dev.langchain4j.model.ollama.OllamaChatModel.builder()
                .baseUrl("http://192.168.0.251:11434")
                .modelName("mistral:7b")
                .timeout(java.time.Duration.ofMinutes(5))
                .build();
    }
}
