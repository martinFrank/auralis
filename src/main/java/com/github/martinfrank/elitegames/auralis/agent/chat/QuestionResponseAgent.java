package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Item;
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
import java.util.Objects;

public class QuestionResponseAgent {

    public record Context(
            Location location,
            Quest quest,
            List<Person> presentPersons,
            String currentTime,
            List<Location> visibleLocations,
            List<Person> visiblePersons,
            List<Item> visibleItems,
            List<GameChat.Turn> recentHistory,
            String hints,
            String playerInput
    ) {}

    private static final String SYSTEM_PROMPT = """
            Du bist der Spielleiter (Herold) eines deutschsprachigen Pen-&-Paper-
            Adventures. Der Spieler hat dir eine Frage gestellt. Beantworte sie
            aus dem dir gelieferten Wissen — als Spielleiter am Tisch.

            REGELN:
            - Antworte ausschliesslich auf die gestellte Frage. Nicht erzaehlen,
              nicht vorwegnehmen, nicht vom Thema abweichen.
            - Du verraetst NIEMALS Meisterinformationen (MASTER-Bloecke). Sie
              dienen dir ausschliesslich zur Orientierung, damit deine Antwort
              nicht der verborgenen Wahrheit widerspricht.
            - "Spezielle Informationen" (SPECIAL-Bloecke) sind Details, die du
              nur preisgibst, wenn die Frage direkt darauf abzielt — gezielte
              Beobachtung, gezieltes Nachfragen, konkrete Suche. Bei allgemeinen
              Fragen halte sie zurueck.
            - Wenn die Antwort aus dem gegebenen Wissen NICHT abgeleitet werden
              kann, sag ehrlich, dass dein Charakter dazu nichts weiss — statt
              zu erfinden.
            - Fasse dich kurz: ein bis drei Saetze, im Spielleiter-Ton.
            - Der HINWEIS des Klassifizierers ist Leitfaden, ueberschreibt aber
              niemals die obigen Wissensgrenzen.
            """;

    private final ChatLanguageModel model;

    public QuestionResponseAgent(ChatLanguageModel model) {
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

        sb.append("=== BEKANNTE ORTE ===\n");
        List<Location> locs = ctx.visibleLocations();
        if (locs == null || locs.isEmpty()) {
            sb.append("(noch keine)\n");
        } else {
            for (Location loc : locs) {
                sb.append("- ").append(nz(loc.title())).append("\n");
                sb.append("  GENERAL: ").append(nz(loc.generalInfo())).append("\n");
                sb.append("  SPECIAL: ").append(nz(loc.specialInfo())).append("\n");
                sb.append("  MASTER: ").append(nz(loc.masterInfo())).append("\n");
            }
        }
        sb.append("=== ENDE BEKANNTE ORTE ===\n\n");

        sb.append("=== BEKANNTE PERSONEN ===\n");
        List<Person> knownPersons = ctx.visiblePersons();
        if (knownPersons == null || knownPersons.isEmpty()) {
            sb.append("(noch keine)\n");
        } else {
            for (Person p : knownPersons) {
                sb.append("- name: ").append(nz(p.name())).append("\n");
                sb.append("  GENERAL: ").append(nz(p.generalInfo())).append("\n");
                sb.append("  SPECIAL: ").append(nz(p.specialInfo())).append("\n");
                sb.append("  MASTER: ").append(nz(p.masterInfo())).append("\n");
            }
        }
        sb.append("=== ENDE BEKANNTE PERSONEN ===\n\n");

        sb.append("=== BEKANNTE GEGENSTAENDE ===\n");
        List<Item> knownItems = ctx.visibleItems();
        if (knownItems == null || knownItems.isEmpty()) {
            sb.append("(noch keine)\n");
        } else {
            for (Item it : knownItems) {
                sb.append("- name: ").append(nz(it.name())).append("\n");
                sb.append("  GENERAL: ").append(nz(it.generalInfo())).append("\n");
                sb.append("  SPECIAL: ").append(nz(it.specialInfo())).append("\n");
                sb.append("  MASTER: ").append(nz(it.masterInfo())).append("\n");
            }
        }
        sb.append("=== ENDE BEKANNTE GEGENSTAENDE ===\n\n");

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

        sb.append("=== FRAGE DES SPIELERS ===\n");
        sb.append(nz(ctx.playerInput()).strip()).append("\n");
        sb.append("=== ENDE FRAGE ===\n");

        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
