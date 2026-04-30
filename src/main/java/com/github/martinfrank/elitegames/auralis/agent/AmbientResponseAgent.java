package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.adventure.QuestTask;
import com.github.martinfrank.elitegames.auralis.game.GameChat;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AmbientResponseAgent {

    public record Context(
            Location location,
            Quest quest,
            List<Person> presentPersons,
            String currentTime,
            Map<String, Boolean> flags,
            List<GameChat.Turn> recentHistory,
            String hints,
            String playerInput
    ) {}

    private static final String SYSTEM_PROMPT = """
            Du bist der Spielleiter (Herold) eines deutschsprachigen Pen-&-Paper-
            Adventures. Der Spieler hat eine atmosphaerische Handlung beschrieben
            — kein Plot-Schritt, keine Frage. Deine Antwort tut zwei Dinge:

            1. SZENERIE: Greife den atmosphaerischen Anteil auf — Sinneseindruecke,
               Reaktionen der Umgebung, kleine Details aus Ort und Tageszeit.
            2. SUBTILER QUEST-HINWEIS: Webe einen leisen, atmosphaerischen Hinweis
               ein, der den Spieler sanft in Richtung des aktuellen Quest schubst
               — etwa eine Geste eines NSC, ein beilaeufiger Blick, ein Geraeusch,
               ein zufaelliger Gedanke. NIE direkt ("du solltest...", "geh zu..."):
               der Spieler muss frei bleiben, den Hinweis aufzugreifen oder zu
               ignorieren.

            REGELN:
            - Du verraetst NIEMALS Meisterinformationen (MASTER-Bloecke). Sie
              dienen dir nur zur Orientierung.
            - "Spezielle Informationen" (SPECIAL) und Quest-Aufgaben (TASKS) sind
              Steuerungswissen — nutze sie, um den Hinweis zu lenken, aber zitiere
              sie nie woertlich.
            - Konzentriere dich auf Aufgaben, die noch offen sind (deren Flags
              nicht den Erfuellungswert haben). Bereits erfuellte Aufgaben sind
              kein Hinweis-Ziel mehr.
            - Dein Antwort aendert den Spielzustand nicht.
            - Halte dich kurz: zwei bis vier Saetze, im Spielleiter-Ton.
            - Der HINWEIS des Klassifizierers ist Leitfaden, ueberschreibt aber
              niemals die Wissensgrenzen.
            """;

    private final ChatLanguageModel model;

    public AmbientResponseAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public String respond(Context context) {
        Objects.requireNonNull(context, "context");
        String userPrompt = buildUserPrompt(context);
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(userPrompt)
        ));
        return response.content().text();
    }

    private static String buildUserPrompt(Context ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== HINWEIS DES KLASSIFIZIERERS ===\n");
        sb.append(nz(ctx.hints())).append("\n");
        sb.append("=== ENDE HINWEIS ===\n\n");

        sb.append("=== AKTUELLES QUEST ===\n");
        if (ctx.quest() == null) {
            sb.append("(kein aktives Quest)\n");
        } else {
            sb.append("Titel: ").append(nz(ctx.quest().title())).append("\n");
            sb.append("GENERAL: ").append(nz(ctx.quest().generalInfo())).append("\n");
            sb.append("SPECIAL: ").append(nz(ctx.quest().specialInfo())).append("\n");
            sb.append("MASTER: ").append(nz(ctx.quest().masterInfo())).append("\n");
            sb.append("TASKS (Steuerungshinweise, nicht zitieren):\n");
            List<QuestTask> tasks = ctx.quest().tasks();
            if (tasks == null || tasks.isEmpty()) {
                sb.append("  (keine)\n");
            } else {
                for (QuestTask t : tasks) {
                    sb.append("  - ").append(nz(t.description()));
                    if (t.completionCondition() != null) {
                        sb.append("  [erfuellt durch: ").append(t.completionCondition()).append("]");
                    }
                    sb.append("\n");
                }
            }
        }
        sb.append("=== ENDE QUEST ===\n\n");

        sb.append("=== AKTUELLER ORT ===\n");
        if (ctx.location() == null) {
            sb.append("(unbekannt)\n");
        } else {
            sb.append("Titel: ").append(nz(ctx.location().title())).append("\n");
            sb.append("GENERAL: ").append(nz(ctx.location().generalInfo())).append("\n");
            sb.append("SPECIAL: ").append(nz(ctx.location().specialInfo())).append("\n");
            sb.append("MASTER: ").append(nz(ctx.location().masterInfo())).append("\n");
        }
        sb.append("Tageszeit: ").append(nz(ctx.currentTime())).append("\n");
        sb.append("=== ENDE ORT ===\n\n");

        sb.append("=== ANWESENDE PERSONEN ===\n");
        List<Person> persons = ctx.presentPersons();
        if (persons == null || persons.isEmpty()) {
            sb.append("(niemand)\n");
        } else {
            for (Person p : persons) {
                sb.append("- name: ").append(nz(p.name())).append("\n");
                sb.append("  GENERAL: ").append(nz(p.generalInfo())).append("\n");
                sb.append("  SPECIAL: ").append(nz(p.specialInfo())).append("\n");
                sb.append("  MASTER: ").append(nz(p.masterInfo())).append("\n");
            }
        }
        sb.append("=== ENDE PERSONEN ===\n\n");

        sb.append("=== SPIEL-FLAGS ===\n");
        Map<String, Boolean> flags = ctx.flags();
        if (flags == null || flags.isEmpty()) {
            sb.append("(keine)\n");
        } else {
            for (Map.Entry<String, Boolean> e : flags.entrySet()) {
                sb.append("- ").append(e.getKey()).append("=").append(e.getValue()).append("\n");
            }
        }
        sb.append("=== ENDE FLAGS ===\n\n");

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

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
