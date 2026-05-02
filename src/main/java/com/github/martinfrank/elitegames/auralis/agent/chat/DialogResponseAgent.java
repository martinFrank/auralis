package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.game.GameChat;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Objects;

public class DialogResponseAgent {

    //Dialog = eine konkrete NPC redet zurück, mit ihrem Persona-Wissen und nur grobem Umgebungskontext.

    public record Context(
            Person addressedPerson,
            Location location,
            List<Person> otherPresent,
            String currentTime,
            List<GameChat.Turn> recentHistory,
            String hints,
            String playerInput
    ) {}

    private static final String SYSTEM_PROMPT = """
            Du verkoerperst eine Nicht-Spieler-Figur in einem deutschsprachigen
            Pen-&-Paper-Adventure. Der Spieler hat dich direkt angesprochen.
            Deine Aufgabe: aus deiner Persona heraus antworten, in erster Person,
            optional mit einer knappen Geste oder Mimik als Rahmen.

            DEINE PERSONA:
            - Name, Wesen und Wissen sind im DEINE-PERSON-Block beschrieben.
            - GENERAL: dein offen erkennbares Wesen und Wissen — kannst du im
              Gespraech ohne Weiteres einbringen.
            - SPECIAL: Details, die du nur preisgibst, wenn der Spieler gezielt
              danach fragt, Vertrauen aufbaut oder ein passender Anlass kommt.
              Bei beilaeufigem Smalltalk halte sie zurueck.
            - MASTER: reines Spielleiter-Wissen ueber dich. NIEMALS preisgeben,
              nicht andeuten, nichts davon ableiten.

            REGELN:
            - Antworte in erster Person, wie *du* sprechen wuerdest. Bleibe in
              Persona — nicht aus dem Erzaehler-Modus heraus.
            - Sprich Deutsch.
            - Halte dich kurz: ein bis vier Saetze, optional mit einer kleinen
              Geste oder Mimik als Rahmen ("Ich poliere ein Glas, dann sage ich:
              '...'").
            - Wenn der Spieler etwas fragt, wozu du nichts weisst, sag das
              ehrlich aus Persona heraus ("Davon hab' ich nie gehoert, fremder
              Reisender.") — erfinde keine Plot-Inhalte.
            - Beziehe dich, wenn passend, auf vorhergehende Turns aus dem
              Dialog. Wiederhole dich nicht woertlich.
            - Der HINWEIS des Klassifizierers ist Leitfaden (z. B. welche
              Plot-Linie ggf. anzustossen ist), ueberschreibt aber niemals die
              Wissensgrenzen.
            """;

    private final ChatLanguageModel model;

    public DialogResponseAgent(ChatLanguageModel model) {
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

        sb.append("=== DEINE PERSON ===\n");
        Person me = ctx.addressedPerson();
        if (me == null) {
            sb.append("(nicht gesetzt)\n");
        } else {
            sb.append("Name:    ").append(nz(me.name())).append("\n");
            sb.append("GENERAL: ").append(nz(me.generalInfo())).append("\n");
            sb.append("SPECIAL: ").append(nz(me.specialInfo())).append("\n");
            sb.append("MASTER:  ").append(nz(me.masterInfo())).append("\n");
        }
        sb.append("=== ENDE DEINE PERSON ===\n\n");

        sb.append("=== AKTUELLER ORT ===\n");
        if (ctx.location() == null) {
            sb.append("(unbekannt)\n");
        } else {
            sb.append("Titel: ").append(nz(ctx.location().title())).append("\n");
            sb.append("Beschreibung: ").append(nz(ctx.location().generalInfo())).append("\n");
        }
        sb.append("Tageszeit: ").append(nz(ctx.currentTime())).append("\n");
        sb.append("=== ENDE ORT ===\n\n");

        sb.append("=== ANDERE ANWESENDE ===\n");
        List<Person> others = ctx.otherPresent();
        if (others == null || others.isEmpty()) {
            sb.append("(niemand sonst)\n");
        } else {
            for (Person p : others) {
                sb.append("- ").append(nz(p.name())).append("\n");
            }
        }
        sb.append("=== ENDE ANWESENDE ===\n\n");

        sb.append("=== BISHERIGER DIALOG (juengste zuletzt) ===\n");
        List<GameChat.Turn> hist = ctx.recentHistory();
        if (hist == null || hist.isEmpty()) {
            sb.append("(noch leer)\n");
        } else {
            for (GameChat.Turn turn : hist) {
                sb.append("[").append(turn.source()).append("] ")
                        .append(nz(turn.content()).strip()).append("\n");
            }
        }
        sb.append("=== ENDE DIALOG ===\n\n");

        sb.append("=== WAS DER SPIELER ZU DIR SAGT ===\n");
        sb.append(nz(ctx.playerInput()).strip()).append("\n");
        sb.append("=== ENDE EINGABE ===\n");

        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
