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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerActionJudgeAgent {

    public enum Classification { ERGAENZEND, UNPASSEND, WEITERFUEHREND }

    public record Judgement(Classification classification,
                            String begruendung,
                            String hinweisFuerHerold,
                            String raw) {
        public Judgement {
            Objects.requireNonNull(classification, "classification");
            Objects.requireNonNull(begruendung, "begruendung");
            Objects.requireNonNull(hinweisFuerHerold, "hinweisFuerHerold");
            Objects.requireNonNull(raw, "raw");
        }
    }

    private static final String DEFAULT_OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String DEFAULT_MODEL = "mistral";

    private static final String SYSTEM_PROMPT = """
            Du bist ein stiller Schiedsrichter an einem Pen-&-Paper-Spieltisch.
            Deine einzige Aufgabe ist es, die naechste Aktion des Spielers zu
            beurteilen und einzuordnen. Du erzaehlst NICHTS, du leitest NICHTS
            und du sprichst den Spieler NICHT an. Antworte stets auf Deutsch.

            Du bekommst drei Informationen:
            1. Hintergrund- und Szenen-Informationen (Meisterinformationen,
               aktuelle Szene, was als naechstes passieren soll).
            2. Den bisherigen Chat-Verlauf zwischen Herold (Spielleiter) und
               Spieler.
            3. Die neue, noch unbewertete Aktion des Spielers.

            Ordne die Aktion GENAU EINER dieser drei Klassen zu:

            - ERGAENZEND: Die Aktion passt stimmig in die aktuelle Szene,
              treibt sie aber nicht weiter. Sie vertieft Atmosphaere,
              Rollenspiel oder Interaktion mit dem bestehenden Schauplatz
              (z. B. "ich setze mich", "ich bestelle ein Bier", "ich sehe
              mich um"). Der Herold soll darauf reagieren, ohne das
              Kapitel voranzutreiben.

            - UNPASSEND: Die Aktion ist in der aktuellen Szene nicht
              sinnvoll oder nicht moeglich (z. B. "ich gehe jagen" in
              einer Taverne, "ich reite los" waehrend eines Gespraechs
              am Tisch). Der Herold soll dem Spieler freundlich, aber
              im Rollenspiel-Ton klarmachen, dass das hier nicht passt,
              ohne das Kapitel zu verlassen.

            - WEITERFUEHREND: Die Aktion stoesst aktiv in das naechste
              Szenen-Element oder in die naechste Szene vor (z. B. der
              Spieler spricht den NSC an, der die Handlung traegt, oder
              verlaesst den aktuellen Ort in Richtung des Plots). Der
              Herold darf jetzt den Folgetext / die naechste Szene
              einleiten.

            STRIKTES AUSGABEFORMAT (exakt diese drei Zeilen, nichts sonst,
            keine Code-Blöcke, keine Einleitung, keine zusaetzlichen
            Kommentare):

            KLASSIFIKATION: <ERGAENZEND|UNPASSEND|WEITERFUEHREND>
            BEGRUENDUNG: <ein Satz, der die Einordnung in Bezug auf Szene
            und Spieleraktion erklaert>
            HINWEIS_FUER_HEROLD: <ein bis zwei Saetze Handlungsanweisung
            an den Spielleiter, wie er auf die Aktion reagieren soll, ohne
            dass du selbst die Szene erzaehlst>
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            === HINTERGRUND / AKTUELLE SZENE ===
            %s
            === ENDE HINTERGRUND ===

            === BISHERIGER CHAT-VERLAUF ===
            %s
            === ENDE CHAT-VERLAUF ===

            === NEUE SPIELERAKTION ===
            %s
            === ENDE SPIELERAKTION ===

            Beurteile ausschliesslich die oben genannte neue Spieleraktion
            im Kontext von Hintergrund und Chat-Verlauf. Halte dich strikt
            an das vorgegebene Ausgabeformat.
            """;

    private final ChatLanguageModel model;

    public PlayerActionJudgeAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public static PlayerActionJudgeAgent withDefaults() {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(DEFAULT_OLLAMA_URL)
                .modelName(DEFAULT_MODEL)
                .timeout(Duration.ofMinutes(2))
                .build();
        return new PlayerActionJudgeAgent(model);
    }

    public Judgement judgeAction(String backgroundContext,
                                 List<AdventureSession.Turn> transcript,
                                 String playerAction) {
        Objects.requireNonNull(backgroundContext, "backgroundContext");
        Objects.requireNonNull(transcript, "transcript");
        Objects.requireNonNull(playerAction, "playerAction");

        String renderedTranscript = renderTranscript(transcript);
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(USER_PROMPT_TEMPLATE.formatted(
                        backgroundContext, renderedTranscript, playerAction))
        ));
        return parse(response.content().text());
    }

    static Judgement parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        String body = stripThinking(raw).strip();

        Classification classification = extractClassification(body);
        String begruendung = extractField(body, "BEGRUENDUNG", "HINWEIS_FUER_HEROLD");
        String hinweis = extractField(body, "HINWEIS_FUER_HEROLD", null);

        if (classification == null || begruendung == null || hinweis == null) {
            throw new IllegalStateException(
                    "Antwort des PlayerActionJudgeAgent konnte nicht geparst werden:\n" + raw);
        }
        return new Judgement(classification, begruendung, hinweis, raw);
    }

    private static String stripThinking(String text) {
        return text.replaceAll("(?is)<think>.*?</think>", "");
    }

    private static Classification extractClassification(String body) {
        Matcher m = Pattern.compile("(?im)^\\s*\\**\\s*KLASSIFIKATION\\s*\\**\\s*:\\s*\\**\\s*([A-Za-zÄÖÜäöü]+)")
                .matcher(body);
        if (!m.find()) {
            return null;
        }
        String value = m.group(1).trim().toUpperCase()
                .replace("Ä", "AE").replace("Ö", "OE").replace("Ü", "UE");
        for (Classification c : Classification.values()) {
            if (c.name().equals(value)) {
                return c;
            }
        }
        return null;
    }

    private static String extractField(String body, String key, String nextKey) {
        String stop = nextKey == null ? "\\z" : "(?=^\\s*\\**\\s*" + nextKey + "\\s*\\**\\s*:)";
        Pattern p = Pattern.compile(
                "(?ims)^\\s*\\**\\s*" + key + "\\s*\\**\\s*:\\s*(.*?)" + stop);
        Matcher m = p.matcher(body);
        if (!m.find()) {
            return null;
        }
        String value = m.group(1).strip();
        return value.isEmpty() ? null : value;
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
