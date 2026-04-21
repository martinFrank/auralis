package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.AdventureSession;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class AmbienteAgent {

    private static final String DEFAULT_OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String DEFAULT_MODEL = "mistral";

    private static final String SYSTEM_PROMPT = """
            Du bist der Herold von Auralis in der Rolle eines Spielleiters,
            der ausschliesslich ERGAENZENDEN Szenen-Inhalt liefert. Die
            Handlung des Kapitels bleibt stehen — du baust nur Atmosphaere,
            Sinneseindruecke und kleine Reaktionen der Welt auf die
            letzte Aktion des Spielers. Antworte stets auf Deutsch, im
            Ton eines klassischen Spielleiters am Tisch.

            STRIKTE REGELN:
            - Du TREIBST die Handlung NICHT voran. Keine neuen NSCs, die
              den Plot tragen, keine neuen Hinweise, keine Wendungen,
              keine Zeitspruenge, keine Ortwechsel.
            - Keine Meisterinformationen oder "Speziellen Informationen"
              aus dem Hintergrund preisgeben — sie dienen dir nur zur
              konsistenten Ausgestaltung.
            - Halte dich an den mitgelieferten HINWEIS fuer den Herold:
              er beschreibt, wie du auf die Spieleraktion reagieren
              sollst. Setze diesen Hinweis in erzaehlten Text um,
              zitiere ihn aber NICHT woertlich.
            - Beschreibe die unmittelbare Umgebung, Geraeusche, Gerueche,
              Gesten von Nebenfiguren, das Ergebnis der Spieleraktion im
              Kleinen (z. B. Getraenk wird gebracht, Blicke werden
              gewechselt, ein Lied beginnt).
            - Halte die Antwort kompakt: ein kurzer Absatz. Schliesse
              mit einer offenen Einladung an den Spieler (z. B.
              "Was tust du?"), ohne den Plot anzubieten.
            - Bleibe konsistent mit dem bisherigen Chat-Verlauf: Ort,
              Tageszeit, anwesende Personen, bereits Gesagtes.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            === HINTERGRUND / AKTUELLE SZENE ===
            %s
            === ENDE HINTERGRUND ===

            === BISHERIGER CHAT-VERLAUF ===
            %s
            === ENDE CHAT-VERLAUF ===

            === LETZTE SPIELERAKTION ===
            %s
            === ENDE SPIELERAKTION ===

            === HINWEIS FUER DEN HEROLD (Handlungsanweisung des Schiedsrichters) ===
            %s
            === ENDE HINWEIS ===

            Liefere jetzt eine rein ergaenzende, atmosphaerische Reaktion
            auf die Spieleraktion gemaess dem Hinweis. Keine Plot-Fortschritte.
            """;

    private final ChatLanguageModel model;

    public AmbienteAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public static AmbienteAgent withDefaults() {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(DEFAULT_OLLAMA_URL)
                .modelName(DEFAULT_MODEL)
                .timeout(Duration.ofMinutes(2))
                .build();
        return new AmbienteAgent(model);
    }

    public String generateAmbiente(String backgroundContext,
                                   List<AdventureSession.Turn> transcript,
                                   String playerAction,
                                   String hinweisFuerHerold) {
        Objects.requireNonNull(backgroundContext, "backgroundContext");
        Objects.requireNonNull(transcript, "transcript");
        Objects.requireNonNull(playerAction, "playerAction");
        Objects.requireNonNull(hinweisFuerHerold, "hinweisFuerHerold");

        String renderedTranscript = renderTranscript(transcript);
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(USER_PROMPT_TEMPLATE.formatted(
                        backgroundContext, renderedTranscript, playerAction, hinweisFuerHerold))
        ));
        return response.content().text();
    }

    private static String renderTranscript(List<AdventureSession.Turn> transcript) {
        if (transcript.isEmpty()) {
            return "(noch kein Verlauf)";
        }
        StringBuilder sb = new StringBuilder();
        for (AdventureSession.Turn turn : transcript) {
            sb.append(switch (turn.source()) {
                case HEROLD -> "HEROLD: ";
                case PLAYER -> "SPIELER: ";
                case ADVENTURE -> "ABENTEUER: ";
            });
            sb.append(turn.content().replace("\n", " ").strip()).append('\n');
        }
        return sb.toString().stripTrailing();
    }
}
