package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Objects;

public class UnclearResponseAgent {

    public record Context(
            Quest quest,
            Location location,
            List<Person> presentPersons,
            String hints,
            String playerInput
    ) {}

    private static final String SYSTEM_PROMPT = """
            Du bist NICHT der Spielleiter, sondern ein hilfsbereiter Begleiter
            ausserhalb der Spielfiktion. Die Eingabe des Spielers konnte nicht
            eindeutig einer Spielhandlung zugeordnet werden — sie ist unklar,
            leer, widerspruechlich oder themenfremd.

            DEINE AUFGABE:
            - Bitte den Spieler freundlich um eine eindeutigere Eingabe.
            - Biete ihm zwei bis drei konkrete Optionen aus der aktuellen
              Situation an: anwesende Personen ansprechen, eine Frage stellen,
              eine Aktion ausfuehren. Stuetze dich dabei nur auf die unten
              gelieferten Titel und Namen.
            - Erinnere ihn bei Bedarf in einem halben Satz an das aktuelle Ziel
              (Quest-Titel), damit er sich orientieren kann.

            REGELN:
            - KEIN Spielleiter-Ton, KEINE Atmosphaere, KEINE Sinneseindruecke.
              Du sprichst auf Meta-Ebene mit dem Spieler.
            - Verrate keinen Quest-Inhalt jenseits des Quest-Titels. Keine
              Geheimnisse, keine Plot-Wendungen.
            - Halte dich kurz: zwei bis vier Saetze, inklusive der Optionen.
            - Erfinde keine Personen, Orte oder Aktionen, die nicht im Kontext
              gelistet sind.
            """;

    private final ChatLanguageModel model;

    public UnclearResponseAgent(ChatLanguageModel model) {
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

        sb.append("=== AKTUELLE SITUATION ===\n");
        sb.append("Quest-Titel: ").append(ctx.quest() == null ? "(kein aktives Quest)" : nz(ctx.quest().title())).append("\n");
        sb.append("Ort-Titel:   ").append(ctx.location() == null ? "(unbekannt)" : nz(ctx.location().title())).append("\n");
        sb.append("=== ENDE AKTUELLE SITUATION ===\n\n");

        sb.append("=== ANWESENDE PERSONEN ===\n");
        List<Person> persons = ctx.presentPersons();
        if (persons == null || persons.isEmpty()) {
            sb.append("(niemand)\n");
        } else {
            for (Person p : persons) {
                sb.append("- ").append(nz(p.name())).append("\n");
            }
        }
        sb.append("=== ENDE PERSONEN ===\n\n");

        sb.append("=== SPIELER-EINGABE ===\n");
        sb.append(nz(ctx.playerInput()).strip()).append("\n");
        sb.append("=== ENDE SPIELER-EINGABE ===\n");

        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
