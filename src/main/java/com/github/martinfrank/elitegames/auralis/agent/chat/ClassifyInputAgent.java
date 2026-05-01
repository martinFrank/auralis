package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.game.GameChat;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassifyInputAgent {

    public enum Category { QUESTION, DIALOG, ACTION, AMBIENT, OOC, UNCLEAR }

    public enum TargetType { PERSON, ITEM, LOCATION, TRANSITION }

    public record Context(
            Quest quest,
            Location location,
            List<Person> presentPersons,
            List<GameChat.Turn> recentHistory,
            String playerInput
    ) {}

    public record Classification(
            Category category,
            String reasoning,
            TargetType targetType,
            String targetId,
            String hints,
            String raw
    ) {}

    private static final String SYSTEM_PROMPT = """
            Du bist der Klassifizierer fuer ein deutschsprachiges Pen-&-Paper-Adventure.
            Deine einzige Aufgabe ist es, die naechste Spieler-Eingabe einer von sechs
            Kategorien zuzuordnen und dem Antwort-Agenten einen kurzen Hinweis zu geben.

            KATEGORIEN:
            - QUESTION: Spieler fragt den Spielleiter (Welt-, Orts-, Charakter-Wissen,
              Tageszeit, Status). Antwort kommt vom Spielleiter. Kein Spielzustand
              aendert sich.
            - DIALOG: Spieler spricht eine anwesende Person an. Antwort kommt aus deren
              Persona. Kann den Spielzustand veraendern (Flags), wenn die Person etwas
              Plot-Relevantes preisgibt.
            - ACTION: Spieler tut etwas mechanisch Relevantes (Bewegung, Item-
              Interaktion, Plot-Aktion, Skill-Check). Kann den Spielzustand veraendern.
            - AMBIENT: Spieler beschreibt eine atmosphaerische Handlung ohne Spielmechanik
              ("ich strecke mich", "ich bestelle ein Bier"). Kein Spielzustand aendert
              sich.
            - OOC: Ausserhalb der Spielfiktion (Pause, Regelfrage, Meta-Kommentar).
            - UNCLEAR: Eingabe ist leer, unverstaendlich, widerspruechlich, themenfremd
              oder du kannst sie keiner anderen Kategorie eindeutig zuordnen. Antwort-
              Agent soll hoeflich nachfragen oder Optionen anbieten. Kein Spielzustand
              aendert sich.

            REGELN:
            - Genau eine Kategorie waehlen. Im Zweifel zwischen DIALOG und ACTION: wenn
              eine spezifische anwesende Person angesprochen wird, ist es DIALOG.
            - Im Zweifel zwischen einer der ersten fuenf Kategorien und UNCLEAR:
              waehle UNCLEAR statt zu raten.
            - Wenn die Eingabe eine anwesende Person, ein bekanntes Item oder einen
              offensichtlichen Ortswechsel benennt, fuelle TARGET_TYP und TARGET_ID
              ausschliesslich mit Werten aus den gelieferten Listen. Erfinde nie IDs.
              Sonst: TARGET_TYP=KEINE und TARGET_ID=-.
            - BEGRUENDUNG und HINWEIS sind kurz (je ein Satz). Der HINWEIS ist im
              Imperativ an den Antwort-Agenten gerichtet.

            ANTWORTFORMAT (genau diese Zeilen, je eine pro Feld, kein Markdown):
            KATEGORIE: <QUESTION|DIALOG|ACTION|AMBIENT|OOC|UNCLEAR>
            BEGRUENDUNG: <ein Satz>
            TARGET_TYP: <PERSON|ITEM|LOCATION|TRANSITION|KEINE>
            TARGET_ID: <id oder ->
            HINWEIS: <ein Satz>
            """;

    private final ChatLanguageModel model;

    public ClassifyInputAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public Classification classify(Context context) {
        Objects.requireNonNull(context, "context");
        String userPrompt = buildUserPrompt(context);
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(userPrompt)
        ));
        System.out.println("---MetaData start---");
        System.out.println(response.metadata());
        System.out.println("---MetaData end---");
        return parse(response.content().text());
    }

    private static String buildUserPrompt(Context ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== AKTUELLES QUEST ===\n");
        if (ctx.quest() == null) {
            sb.append("(kein aktives Quest)\n");
        } else {
            sb.append("Titel: ").append(nz(ctx.quest().title())).append("\n");
            sb.append("Beschreibung: ").append(nz(ctx.quest().generalInfo())).append("\n");
        }
        sb.append("=== ENDE QUEST ===\n\n");

        sb.append("=== AKTUELLER ORT ===\n");
        if (ctx.location() == null) {
            sb.append("(unbekannt)\n");
        } else {
            sb.append("Titel: ").append(nz(ctx.location().title())).append("\n");
            sb.append("Beschreibung: ").append(nz(ctx.location().generalInfo())).append("\n");
            sb.append("Zusatzinfos: ").append(nz(ctx.location().specialInfo())).append("\n");
            sb.append("Zusatzinfos: ").append(nz(ctx.location().masterInfo())).append("\n");
        }
        sb.append("=== ENDE ORT ===\n\n");

        sb.append("=== ANWESENDE PERSONEN ===\n");
        List<Person> persons = ctx.presentPersons();
        if (persons == null || persons.isEmpty()) {
            sb.append("(niemand)\n");
        } else {
            for (Person p : persons) {
                sb.append("- id=").append(nz(p.id()))
                        .append(" | name=").append(nz(p.name()))
                        .append(" | ").append(nz(p.generalInfo())).append("\n");
            }
        }
        sb.append("=== ENDE PERSONEN ===\n\n");

        sb.append("=== CHATVERLAUF (juengste zuletzt) ===\n");
        List<GameChat.Turn> hist = ctx.recentHistory();
        if (hist == null || hist.isEmpty()) {
            sb.append("(noch leer)\n");
        } else {
            for (GameChat.Turn turn : hist) {
                sb.append("[").append(turn.source()).append("] ")
                        .append(nz(turn.content()).strip()).append("\n");
            }
        }
        sb.append("=== ENDE CHATVERLAUF ===\n\n");

        sb.append("=== SPIELER-EINGABE ===\n");
        sb.append(nz(ctx.playerInput()).strip()).append("\n");
        sb.append("=== ENDE SPIELER-EINGABE ===\n");
        return sb.toString();
    }

    static Classification parse(String raw) {
        String body = stripThinking(raw);
        String categoryRaw = require(body, "KATEGORIE");
        Category category = Category.valueOf(categoryRaw.toUpperCase(Locale.ROOT));
        String reasoning = require(body, "BEGRUENDUNG");
        TargetType targetType = parseTargetType(extract(body, "TARGET_TYP"));
        String targetId = parseTargetId(extract(body, "TARGET_ID"));
        String hints = require(body, "HINWEIS");
        return new Classification(category, reasoning, targetType, targetId, hints, raw);
    }

    private static TargetType parseTargetType(String s) {
        if (s == null || s.isBlank() || s.equals("-") || s.equalsIgnoreCase("KEINE")) {
            return null;
        }
        return TargetType.valueOf(s.toUpperCase(Locale.ROOT));
    }

    private static String parseTargetId(String s) {
        if (s == null || s.isBlank() || s.equals("-")) {
            return null;
        }
        return s;
    }

    private static String require(String body, String field) {
        String value = extract(body, field);
        if (value == null) {
            throw new IllegalStateException(
                    "Feld " + field + " fehlt in Klassifikator-Antwort:\n" + body);
        }
        return value;
    }

    private static String extract(String body, String field) {
        Pattern p = Pattern.compile("(?im)^\\s*" + Pattern.quote(field) + "\\s*:\\s*(.+?)\\s*$");
        Matcher m = p.matcher(body);
        return m.find() ? m.group(1).strip() : null;
    }

    private static String stripThinking(String raw) {
        return raw.replaceAll("(?is)<think>.*?</think>", "").strip();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
