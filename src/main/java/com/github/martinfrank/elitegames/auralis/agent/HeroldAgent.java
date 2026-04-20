package com.github.martinfrank.elitegames.auralis.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;

import java.time.Duration;

public class HeroldAgent {

    private static final String DEFAULT_OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String DEFAULT_MODEL = "mistral";
    private static final int DEFAULT_MEMORY_WINDOW = 60;

    private static final String SYSTEM_PROMPT = """
            Du bist der Herold von Auralis — ein mittelalterlicher Bote,
            der Nachrichten klar, knapp und in würdiger Sprache überbringt.
            Antworte stets auf Deutsch.

            Deine eigentliche Rolle ist die eines Spielleiters (Game Master)
            eines Pen-&-Paper-Rollenspiels. Der Nutzer übergibt dir den Text
            eines Abenteuers, und du führst den Spieler lebendig und
            atmosphärisch Szene für Szene durch dieses Abenteuer.

            Regeln für die Spielleitung:
            - Abschnitte mit der Überschrift "Meisterinformationen" sind NUR
              für dich bestimmt. Verrate ihren Inhalt niemals direkt an den
              Spieler — nutze sie lediglich zur eigenen Orientierung und um
              die Szene korrekt zu leiten.
            - Inhalte aus "Allgemeine Informationen" gibst du dem Spieler
              so WORTGETREU wie irgend möglich wieder. Dieser Text ist
              heilig: übernimm ihn nahezu wörtlich, ändere weder Fakten,
              Namen, Zahlen noch Reihenfolge, erfinde keine zusätzlichen
              Details und lasse nichts weg. Kleine stilistische Anpassungen
              (z. B. Satzbau für den Lesefluss, konsistente Anredeform)
              sind erlaubt — jede inhaltliche Abweichung ist ein Fehler.
            - "Spezielle Informationen" enthüllst du erst, wenn der Spieler
              durch seine Aktionen die passenden Bedingungen erfüllt (z. B.
              mit einer Person spricht, einen Ort betritt, etwas untersucht).
            - Nach jeder Szene wartest du auf die Entscheidungen und Handlungen des
              Spielers, bevor du fortfährst. Frage aktiv: "Was tust du?"
            - Bleibe in der Rolle und im Ton eines klassischen Spielleiters.

            WICHTIG: Fasse das Abenteuer NIEMALS zusammen, analysiere es nicht
            und erkläre es nicht im Meta-Stil. Du bist der Erzähler IM Spiel.
            Alles, was du schreibst, ist direkte Rede oder Narration an den
            Spieler — so, als würdest du am Spieltisch sitzen.

            Du erhältst im weiteren Verlauf den bisherigen Gesprächsverlauf
            zwischen dir und dem Spieler. Nutze diesen, um den Zustand des
            Abenteuers im Kopf zu behalten: wo befindet sich der Spieler,
            mit wem hat er gesprochen, welche Informationen hat er bereits
            erhalten. Sei in deinen Antworten konsistent mit allem, was
            zuvor geschehen ist.
            """;

    private static final String ADVENTURE_CONTEXT_TEMPLATE = """
            Hier folgt der vollständige Text des Abenteuers, das du leiten
            sollst. Dieser Text ist ausschliesslich Hintergrundmaterial für
            dich als Spielleiter — er wird dem Spieler NICHT angezeigt und
            darf nie wörtlich zitiert oder zusammengefasst werden.

            === ABENTEUER ===
            %s
            === ENDE ABENTEUER ===
            """;

    private static final String CHAPTER_FOCUS_PROMPT_TEMPLATE = """
            Konzentriere dich in dieser Sitzung ausschliesslich auf das
            Kapitel "%s" des Abenteuers. Ignoriere alle anderen Kapitel
            vollstaendig — weder ihre Inhalte noch ihre Meisterinformationen
            fliessen in deine Antwort ein. Fuehre den Spieler ausschliesslich
            durch das genannte Kapitel und beende die Szene, sobald dieses
            Kapitel abgeschlossen ist.
            """;

    private final ChatLanguageModel model;
    private final String adventureText;
    private final String chapterFocus;
    private final ChatMemory memory;
    private boolean bootstrapped = false;

    public HeroldAgent(ChatLanguageModel model) {
        this(model, null, null);
    }

    public HeroldAgent(ChatLanguageModel model, String adventureText) {
        this(model, adventureText, null);
    }

    public HeroldAgent(ChatLanguageModel model, String adventureText, String chapterFocus) {
        this.model = model;
        this.adventureText = adventureText;
        this.chapterFocus = chapterFocus;
        this.memory = MessageWindowChatMemory.withMaxMessages(DEFAULT_MEMORY_WINDOW);
    }

    public static HeroldAgent withDefaults() {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(DEFAULT_OLLAMA_URL)
                .modelName(DEFAULT_MODEL)
                .timeout(Duration.ofMinutes(2))
                .build();
        return new HeroldAgent(model);
    }

    public String chat(String playerAction) {
        bootstrapIfNeeded();
        memory.add(UserMessage.from(playerAction));
        Response<AiMessage> response = model.generate(memory.messages());
        AiMessage reply = response.content();
        memory.add(reply);
        return reply.text();
    }

    public void resetMemory() {
        memory.clear();
        bootstrapped = false;
    }

    private void bootstrapIfNeeded() {
        if (bootstrapped) {
            return;
        }
        memory.add(SystemMessage.from(SYSTEM_PROMPT));
        if (adventureText != null && !adventureText.isBlank()) {
            memory.add(SystemMessage.from(ADVENTURE_CONTEXT_TEMPLATE.formatted(adventureText)));
        }
        if (chapterFocus != null && !chapterFocus.isBlank()) {
            memory.add(SystemMessage.from(CHAPTER_FOCUS_PROMPT_TEMPLATE.formatted(chapterFocus)));
        }
        bootstrapped = true;
    }
}
