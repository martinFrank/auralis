package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.adventure.QuestTask;
import com.github.martinfrank.elitegames.auralis.adventure.Transition;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionJudgeAgent {

    public record Context(
            Quest currentQuest,
            Location location,
            Map<String, Boolean> flags,
            String hints,
            String playerInput
    ) {}

    public record FlagChange(String flagId, boolean value, String reason) {}

    public record Verdict(String summary, List<FlagChange> flagChanges, String raw) {}

    private static final String SYSTEM_PROMPT = """
            Du bist der Judge eines deutschsprachigen Pen-&-Paper-Adventures. Der
            Spieler hat eine mechanisch relevante Aktion ausgefuehrt (Bewegung,
            Plot-Aktion, Item-Interaktion, Skill-Check). Deine einzige Aufgabe:
            ENTSCHEIDEN, welche Flags sich durch diese Aktion aendern.

            Du schreibst KEINE Erzaehlung. Ein anderer Agent uebernimmt die Prosa
            auf Basis deiner Entscheidung.

            DEINE QUELLEN:
            - AKTUELLES QUEST mit TASKS + deren completionCondition zeigen, welche
              Flags durch Quest-Fortschritt gesetzt werden sollen.
            - AKTUELLE LOCATION mit ihren TRANSITIONS zeigen, welche Flags eine
              Bewegung ausloesen kann.
            - FLAG_STATE: bereits gesetzte Flags. Schlage NUR Aenderungen vor, die
              einen Wert tatsaechlich kippen — keine Bestaetigungen bestehender
              Werte.
            - MASTER-Bloecke sind Spielleiter-Wissen: nutze sie zur Orientierung
              deiner Entscheidung. NIE im `grund`-Feld zitieren oder andeuten.

            REGELN:
            - Schlage Flag-Aenderungen NUR vor, wenn die Spieler-Aktion sie
              eindeutig rechtfertigt. Im Zweifel: keine Aenderung.
            - Verwende ausschliesslich flagIds aus der FLAG_STATE-Liste. Erfinde
              niemals welche.
            - Mehrere Aenderungen sind erlaubt, wenn die Aktion mehrere
              Bedingungen gleichzeitig erfuellt.
            - Wenn die Aktion keinen Spielzustand veraendert (z. B. Spieler hat
              sich vertan, Wunsch nicht erfuellbar), gib eine leere
              FLAG_AENDERUNGEN-Liste aus.
            - ZUSAMMENFASSUNG ist ein neutraler Satz fuer den Narrator — was
              passiert ist, ohne Master-Info zu zitieren.

            ANTWORTFORMAT (genau diese Struktur, kein Markdown, keine Codeblocks):
            ZUSAMMENFASSUNG: <ein Satz>
            FLAG_AENDERUNGEN:
            - id=<flagId> wert=<true|false> grund="<ein Satz>"
            - id=<flagId> wert=<true|false> grund="<ein Satz>"

            Bei keiner Aenderung: ZUSAMMENFASSUNG-Zeile gefolgt von der leeren
            FLAG_AENDERUNGEN: Sektion (keine `- id=`-Zeilen).
            """;

    private final ChatLanguageModel model;

    public ActionJudgeAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public Verdict judge(Context context) {
        Objects.requireNonNull(context, "context");
        String userPrompt = buildUserPrompt(context);
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(userPrompt)
        ));
        return parse(response.content().text());
    }

    private static String buildUserPrompt(Context ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== HINWEIS DES KLASSIFIZIERERS ===\n");
        sb.append(nz(ctx.hints())).append("\n");
        sb.append("=== ENDE HINWEIS ===\n\n");

        sb.append("=== AKTUELLES QUEST ===\n");
        Quest quest = ctx.currentQuest();
        if (quest == null) {
            sb.append("(kein aktives Quest)\n");
        } else {
            sb.append("Titel: ").append(nz(quest.title())).append("\n");
            sb.append("GENERAL: ").append(nz(quest.generalInfo())).append("\n");
            sb.append("SPECIAL: ").append(nz(quest.specialInfo())).append("\n");
            sb.append("MASTER: ").append(nz(quest.masterInfo())).append("\n");
            sb.append("TASKS:\n");
            List<QuestTask> tasks = quest.tasks();
            if (tasks == null || tasks.isEmpty()) {
                sb.append("  (keine)\n");
            } else {
                for (QuestTask t : tasks) {
                    sb.append("  - description=\"").append(nz(t.description())).append("\"");
                    if (t.completionCondition() != null) {
                        sb.append(" completionCondition=").append(t.completionCondition());
                    }
                    sb.append("\n");
                }
            }
        }
        sb.append("=== ENDE QUEST ===\n\n");

        sb.append("=== AKTUELLE LOCATION ===\n");
        Location loc = ctx.location();
        if (loc == null) {
            sb.append("(unbekannt)\n");
        } else {
            sb.append("Titel: ").append(nz(loc.title())).append("\n");
            sb.append("GENERAL: ").append(nz(loc.generalInfo())).append("\n");
            sb.append("SPECIAL: ").append(nz(loc.specialInfo())).append("\n");
            sb.append("MASTER: ").append(nz(loc.masterInfo())).append("\n");
            sb.append("TRANSITIONS:\n");
            List<Transition> ts = loc.transitions();
            if (ts == null || ts.isEmpty()) {
                sb.append("  (keine)\n");
            } else {
                for (Transition t : ts) {
                    sb.append("  - to=").append(nz(t.to()));
                    if (t.label() != null) sb.append(" label=\"").append(t.label()).append("\"");
                    if (t.condition() != null) sb.append(" condition=").append(t.condition());
                    if (t.effects() != null && !t.effects().isEmpty()) sb.append(" effects=").append(t.effects());
                    sb.append("\n");
                }
            }
        }
        sb.append("=== ENDE LOCATION ===\n\n");

        sb.append("=== FLAG_STATE ===\n");
        Map<String, Boolean> flags = ctx.flags();
        if (flags == null || flags.isEmpty()) {
            sb.append("(keine)\n");
        } else {
            for (Map.Entry<String, Boolean> e : flags.entrySet()) {
                sb.append("- ").append(e.getKey()).append("=").append(e.getValue()).append("\n");
            }
        }
        sb.append("=== ENDE FLAG_STATE ===\n\n");

        sb.append("=== AKTION DES SPIELERS ===\n");
        sb.append(nz(ctx.playerInput()).strip()).append("\n");
        sb.append("=== ENDE AKTION ===\n");

        return sb.toString();
    }

    static Verdict parse(String raw) {
        String body = stripThinking(raw);
        String summary = requireField(body, "ZUSAMMENFASSUNG");
        List<FlagChange> changes = parseFlagChanges(body);
        return new Verdict(summary, changes, raw);
    }

    private static List<FlagChange> parseFlagChanges(String body) {
        List<FlagChange> out = new ArrayList<>();
        Pattern p = Pattern.compile(
                "(?im)^\\s*-\\s*id=(\\S+)\\s+wert=(true|false)\\s+grund=[\"']?(.*?)[\"']?\\s*$");
        Matcher m = p.matcher(body);
        while (m.find()) {
            out.add(new FlagChange(m.group(1), Boolean.parseBoolean(m.group(2)), m.group(3).trim()));
        }
        return out;
    }

    private static String requireField(String body, String field) {
        Pattern p = Pattern.compile("(?im)^\\s*" + Pattern.quote(field) + "\\s*:\\s*(.+?)\\s*$");
        Matcher m = p.matcher(body);
        if (!m.find()) {
            throw new IllegalStateException(
                    "Feld " + field + " fehlt in Judge-Antwort:\n" + body);
        }
        return m.group(1).strip();
    }

    private static String stripThinking(String raw) {
        return raw.replaceAll("(?is)<think>.*?</think>", "").strip();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
