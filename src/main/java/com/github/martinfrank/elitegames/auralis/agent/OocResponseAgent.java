package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Objects;

public class OocResponseAgent {

    public record Context(
            Quest quest,
            Location location,
            String currentTime,
            String hints,
            String playerInput
    ) {}

    private static final String SYSTEM_PROMPT = """
            Du bist NICHT der Spielleiter, sondern ein hilfsbereiter Begleiter
            ausserhalb der Spielfiktion. Der Spieler hat etwas geschrieben, das
            nicht Teil des Rollenspiels ist (Pause-Anfrage, Meta-Frage, Regel-
            Frage, Recap-Wunsch, Verstaendnisfrage).

            REGELN:
            - Antworte sachlich, auf Meta-Ebene. KEIN Erzaehl-Ton, KEIN Spielleiter-
              Stil, KEINE Atmosphaere oder Sinneseindruecke.
            - Verrate keinen Quest-Inhalt jenseits dessen, was der Spieler aus
              dem Spielverlauf bereits kennt. Bei Recap-Anfragen gib hoechstens
              Quest-Titel, Ort-Titel und Tageszeit zurueck — niemals Geheimnisse,
              Plot-Wendungen oder Spielleiter-Wissen.
            - Bei Pause- oder Wartebitten genuegt eine kurze Bestaetigung
              ("Alles klar, ich warte.").
            - Bei Regel- oder Mechanik-Fragen, die du aus dem gegebenen Kontext
              nicht beantworten kannst, sag das ehrlich, statt zu erfinden.
            - Halte dich kurz: ein bis zwei Saetze.
            - Der HINWEIS des Klassifizierers ist Leitfaden, ueberschreibt aber
              niemals diese Regeln.
            """;

    private final ChatLanguageModel model;

    public OocResponseAgent(ChatLanguageModel model) {
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

        sb.append("=== SPIEL-STATUS (nur fuer Recap, nicht erzaehlen) ===\n");
        sb.append("Quest-Titel: ").append(ctx.quest() == null ? "(kein aktives Quest)" : nz(ctx.quest().title())).append("\n");
        sb.append("Ort-Titel:   ").append(ctx.location() == null ? "(unbekannt)" : nz(ctx.location().title())).append("\n");
        sb.append("Tageszeit:   ").append(nz(ctx.currentTime())).append("\n");
        sb.append("=== ENDE SPIEL-STATUS ===\n\n");

        sb.append("=== SPIELER-EINGABE ===\n");
        sb.append(nz(ctx.playerInput()).strip()).append("\n");
        sb.append("=== ENDE SPIELER-EINGABE ===\n");

        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
