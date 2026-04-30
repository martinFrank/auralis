package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.game.GameChat;
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

public class SceneSwitchAgent {

    public enum SwitchDecision {
        CAN_SWITCH("WECHSEL_MOEGLICH"),
        STAY("WEITERLAUFEN");

        private final String germanLabel;

        SwitchDecision(String germanLabel) {
            this.germanLabel = germanLabel;
        }

        public String germanLabel() {
            return germanLabel;
        }

        public static SwitchDecision fromGermanLabel(String label) {
            String normalized = label.trim().toUpperCase()
                    .replace("Ä", "AE").replace("Ö", "OE").replace("Ü", "UE");
            for (SwitchDecision d : values()) {
                if (d.germanLabel.equals(normalized)) {
                    return d;
                }
            }
            return null;
        }
    }

    public record Verdict(SwitchDecision decision, String reason, String raw) {
        public Verdict {
            Objects.requireNonNull(decision, "decision");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(raw, "raw");
        }
    }

    private static final String DEFAULT_OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String DEFAULT_MODEL = "mistral";

    private static final String SYSTEM_PROMPT = """
            Du bist ein stiller Beobachter an einem Pen-&-Paper-Spieltisch.
            Deine einzige Aufgabe ist es zu beurteilen, ob der gegenwaertige
            Dialog zwischen Spielleiter (Herold) und Spieler an einem Punkt
            angekommen ist, an dem man die Szene guten Gewissens beenden
            und zur naechsten Szene wechseln koennte. Du erzaehlst NICHTS,
            du leitest NICHTS, du sprichst den Spieler NICHT an. Antworte
            stets auf Deutsch.

            Du bekommst den bisherigen Gespraechsverlauf in chronologischer
            Reihenfolge (Beitraege des Spielers, des Herolds und ggf.
            vorgelesene Abenteuer-Abschnitte).

            Ordne den aktuellen Zustand GENAU EINER dieser beiden Klassen zu:

            - WECHSEL_MOEGLICH: Der Dialog ist an einem natuerlichen
              Ruhepunkt. Die letzte Aussage des Spielers ist beantwortet
              bzw. aufgeloest, es steht keine Frage im Raum, es laeuft
              keine begonnene Aktion, kein NSC wartet auf eine Antwort,
              und der Spieler signalisiert keine unmittelbare Absicht
              weiterzumachen. Ein Szenenwechsel wuerde den Spieler nicht
              mitten in einem Austausch unterbrechen.

            - WEITERLAUFEN: Der Dialog ist noch aktiv. Typische Signale:
              eine offene Frage (vom Spieler oder vom NSC), eine
              angekuendigte oder begonnene Aktion, die noch nicht
              aufgeloest ist, eine Reaktion des Herolds, auf die eine
              Spielerantwort naheliegt, oder spuerbare Absicht des
              Spielers, noch etwas zu tun oder zu fragen. Ein Wechsel
              waere jetzt eine Unterbrechung.

            Halte dich strikt an die Signale im Transkript. Wenn das
            Transkript leer ist oder keine klare Aussage zulaesst, waehle
            im Zweifel WEITERLAUFEN.

            STRIKTES AUSGABEFORMAT (exakt diese zwei Zeilen, nichts sonst,
            keine Code-Bloecke, keine Einleitung, keine zusaetzlichen
            Kommentare):

            ENTSCHEIDUNG: <WECHSEL_MOEGLICH|WEITERLAUFEN>
            BEGRUENDUNG: <ein Satz, der die Entscheidung anhand der
            letzten Beitraege des Transkripts belegt>
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            === BISHERIGER DIALOG ===
            %s
            === ENDE DIALOG ===

            Beurteile ausschliesslich, ob der Dialog in seinem jetzigen
            Zustand gut unterbrochen werden kann, oder ob ein Wechsel den
            Spieler mitten im Austausch abschneiden wuerde. Halte dich
            strikt an das vorgegebene Ausgabeformat.
            """;

    private final ChatLanguageModel model;

    public SceneSwitchAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public static SceneSwitchAgent withDefaults() {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(DEFAULT_OLLAMA_URL)
                .modelName(DEFAULT_MODEL)
                .timeout(Duration.ofMinutes(2))
                .build();
        return new SceneSwitchAgent(model);
    }

    public Verdict evaluate(GameChat session) {
        return evaluate(session.transcript());
    }

    public Verdict evaluate(List<GameChat.Turn> transcript) {
        String rendered = renderTranscript(transcript);
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(USER_PROMPT_TEMPLATE.formatted(rendered))
        ));
        return parse(response.content().text());
    }

    static String renderTranscript(List<GameChat.Turn> transcript) {
        if (transcript == null || transcript.isEmpty()) {
            return "(leer)";
        }
        StringBuilder sb = new StringBuilder();
        for (GameChat.Turn turn : transcript) {
            sb.append('[').append(label(turn.source())).append("] ")
              .append(turn.content().strip())
              .append("\n\n");
        }
        return sb.toString().strip();
    }

    private static String label(GameChat.Source source) {
        return switch (source) {
            case PLAYER -> "SPIELER";
            case HEROLD -> "HEROLD";
            case ADVENTURE -> "ABENTEUER";
        };
    }

    static Verdict parse(String raw) {
        String body = stripThinking(raw).strip();
        SwitchDecision decision = extractDecision(body);
        String reason = extractField(body, "BEGRUENDUNG", null);
        if (decision == null || reason == null) {
            throw new IllegalStateException(
                    "Antwort des SceneSwitchAgent konnte nicht geparst werden:\n" + raw);
        }
        return new Verdict(decision, reason, raw);
    }

    private static String stripThinking(String text) {
        return text.replaceAll("(?is)<think>.*?</think>", "");
    }

    private static SwitchDecision extractDecision(String body) {
        Matcher m = Pattern.compile("(?im)^\\s*\\**\\s*ENTSCHEIDUNG\\s*\\**\\s*:\\s*\\**\\s*([A-Za-z_ÄÖÜäöü]+)")
                .matcher(body);
        if (!m.find()) {
            return null;
        }
        return SwitchDecision.fromGermanLabel(m.group(1));
    }

    private static String extractField(String body, String key, String nextKey) {
        String flexKey = umlautFlexible(key);
        String stop = nextKey == null
                ? "\\z"
                : "(?=^\\s*\\**\\s*" + umlautFlexible(nextKey) + "\\s*\\**\\s*:)";
        Pattern p = Pattern.compile(
                "(?ims)^\\s*\\**\\s*" + flexKey + "\\s*\\**\\s*:\\s*(.*?)" + stop);
        Matcher m = p.matcher(body);
        if (!m.find()) {
            return null;
        }
        String value = m.group(1).strip();
        return value.isEmpty() ? null : value;
    }

    private static String umlautFlexible(String key) {
        return key
                .replace("UE", "(?:UE|[Üü])")
                .replace("OE", "(?:OE|[Öö])")
                .replace("AE", "(?:AE|[Ää])");
    }
}
