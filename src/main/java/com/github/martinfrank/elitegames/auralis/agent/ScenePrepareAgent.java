package com.github.martinfrank.elitegames.auralis.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class ScenePrepareAgent {

    private static final String DEFAULT_OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String DEFAULT_MODEL = "mistral";

    private static final String SYSTEM_PROMPT = """
            Du bist der Buehnenmeister von Auralis. Deine einzige Aufgabe ist es,
            eine kurze Uebergangsszene zu schreiben, die die Helden in die
            Ausgangssituation des naechsten Kapitels fuehrt. Antworte stets auf
            Deutsch und im Ton eines Spielleiters am Tisch.

            STRIKTE REGELN:
            - Du erzaehlst das eigentliche Kapitel NICHT. Keine Ereignisse, keine
              NSCs, keine Wendungen und keine Inhalte aus dem Kapitel vorwegnehmen.
            - Du verraetst NIEMALS Meisterinformationen oder "Spezielle
              Informationen" aus dem Abenteuer. Sie dienen dir ausschliesslich
              zur Orientierung.
            - Beschraenke dich auf Ort, Stimmung und die Tatsache, dass die
              Helden sich auf dem Weg zum Schauplatz des naechsten Kapitels
              befinden.
            - Zitiere den uebergebenen Hintergrundtext NICHT woertlich.
            - Halte die Szene kompakt (wenige Saetze, hoechstens ein kurzer
              Absatz).
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Bereite die Buehne fuer das Kapitel "%s" vor.

            Der folgende Text liefert dir den Hintergrund (Meisterinformationen,
            Welt-Kontext oder bisherige Ereignisse). Nutze ihn nur, um die
            Uebergangsszene zu schreiben — verrate keine darin enthaltenen
            Geheimnisse und zitiere ihn nicht woertlich.

            === HINTERGRUND ===
            %s
            === ENDE HINTERGRUND ===
            """;

    private final ChatLanguageModel model;

    public ScenePrepareAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public static ScenePrepareAgent withDefaults() {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(DEFAULT_OLLAMA_URL)
                .modelName(DEFAULT_MODEL)
                .timeout(Duration.ofMinutes(2))
                .build();
        return new ScenePrepareAgent(model);
    }

    public String prepareScene(String chapterTitle, String backgroundContext) {
        Objects.requireNonNull(chapterTitle, "chapterTitle");
        Objects.requireNonNull(backgroundContext, "backgroundContext");
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(USER_PROMPT_TEMPLATE.formatted(chapterTitle, backgroundContext))
        ));
        return response.content().text();
    }
}
