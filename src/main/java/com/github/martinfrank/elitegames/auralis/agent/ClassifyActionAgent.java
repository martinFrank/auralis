package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.adventure.Scene;
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

public class ClassifyActionAgent {

    public enum ClassificationType {
        DETAIL_INFO("DETAILINFORMATIONEN"),
        INAPPROPRIATE("UNPASSEND"),
        TRIVIAL("TRIVIAL");

        private final String germanLabel;

        ClassificationType(String germanLabel) {
            this.germanLabel = germanLabel;
        }

        public String germanLabel() {
            return germanLabel;
        }

        public static ClassificationType fromGermanLabel(String label) {
            String normalized = label.trim().toUpperCase()
                    .replace("Ä", "AE").replace("Ö", "OE").replace("Ü", "UE");
            for (ClassificationType t : values()) {
                if (t.germanLabel.equals(normalized)) {
                    return t;
                }
            }
            return null;
        }
    }

    public record Classification(ClassificationType classification,
                                 String begruendung,
                                 String hinweisFuerHerold,
                                 String raw) {
        public Classification {
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

            Du bekommst zwei Informationen:
            1. Die aktuelle Szene (wo sich die Helden gerade befinden,
               inkl. Meisterinformationen).
            2. Die neue, noch unbewertete Aktion des Spielers.

            Ordne die Aktion GENAU EINER dieser drei Klassen zu:

            - DETAILINFORMATIONEN: Die Aktion zielt darauf ab, aus der
              aktuellen Szene Informationen oder Reaktionen
              hervorzulocken, die dort in den "Speziellen Informationen"
              oder "Meisterinformationen" hinterlegt sind. Typisch sind
              Dialoge mit NSCs der aktuellen Szene, gezielte Fragen,
              Talenteinsatz (z. B. Faehrten lesen, Suchen, Gassenwissen,
              Menschenkenntnis) oder das Untersuchen von Objekten am
              aktuellen Schauplatz. Der Herold darf daraus passende
              Details enthuellen.

            - UNPASSEND: Die Aktion ist in der aktuellen Szene nicht
              sinnvoll oder nicht moeglich (z. B. "ich gehe jagen" in
              einer Taverne, "ich reite los" waehrend eines Gespraechs
              am Tisch). Der Herold soll dem Spieler freundlich, aber
              im Rollenspiel-Ton klarmachen, dass das hier nicht passt,
              ohne das Kapitel zu verlassen.

            - TRIVIAL: Die Aktion hat keinen Einfluss auf den Fortschritt
              im Abenteuer. Sie passt zwar zur Szene, foerdert aber weder
              neue Details zutage noch treibt sie den Plot voran (z. B.
              "ich bestelle noch ein Bier", "ich setze mich", "ich sehe
              mich um" ohne gezielte Absicht). Der Herold soll nur
              atmosphaerisch reagieren, ohne Geheimnisse oder
              Plot-relevante Details preiszugeben.

            STRIKTES AUSGABEFORMAT (exakt diese drei Zeilen, nichts sonst,
            keine Code-Blöcke, keine Einleitung, keine zusaetzlichen
            Kommentare):

            KLASSIFIKATION: <DETAILINFORMATIONEN|UNPASSEND|TRIVIAL>
            BEGRUENDUNG: <ein Satz, der die Einordnung in Bezug auf Szene
            und Spieleraktion erklaert>
            HINWEIS_FUER_HEROLD: <ein bis zwei Saetze Handlungsanweisung
            an den Spielleiter, wie er auf die Aktion reagieren soll, ohne
            dass du selbst die Szene erzaehlst>
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            === AKTUELLE SZENE ===
            %s
            === ENDE AKTUELLE SZENE ===

            === NEUE SPIELERAKTION ===
            %s
            === ENDE SPIELERAKTION ===

            Beurteile ausschliesslich die oben genannte neue Spieleraktion
            im Kontext der aktuellen Szene. Halte dich strikt an das
            vorgegebene Ausgabeformat.
            """;

    private final ChatLanguageModel model;

    public ClassifyActionAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public static ClassifyActionAgent withDefaults() {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(DEFAULT_OLLAMA_URL)
                .modelName(DEFAULT_MODEL)
                .timeout(Duration.ofMinutes(2))
                .build();
        return new ClassifyActionAgent(model);
    }

    public Classification classifyAction(Scene currentScene, String playerAction) {
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(USER_PROMPT_TEMPLATE.formatted(
                        currentScene.content(), playerAction))
        ));
        return parse(response.content().text());
    }

    static Classification parse(String raw) {
        String body = stripThinking(raw).strip();

        ClassificationType classification = extractClassification(body);
        String begruendung = extractField(body, "BEGRUENDUNG", "HINWEIS_FUER_HEROLD");
        String hinweis = extractField(body, "HINWEIS_FUER_HEROLD", null);

        if (classification == null || begruendung == null || hinweis == null) {
            throw new IllegalStateException(
                    "Antwort des ClassifyActionAgent konnte nicht geparst werden:\n" + raw);
        }
        return new Classification(classification, begruendung, hinweis, raw);
    }

    private static String stripThinking(String text) {
        return text.replaceAll("(?is)<think>.*?</think>", "");
    }

    private static ClassificationType extractClassification(String body) {
        Matcher m = Pattern.compile("(?im)^\\s*\\**\\s*KLASSIFIKATION\\s*\\**\\s*:\\s*\\**\\s*([A-Za-zÄÖÜäöü]+)")
                .matcher(body);
        if (!m.find()) {
            return null;
        }
        return ClassificationType.fromGermanLabel(m.group(1));
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

}
