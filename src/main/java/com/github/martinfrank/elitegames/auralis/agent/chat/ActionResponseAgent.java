package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.FlagChange;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.Verdict;
import com.github.martinfrank.elitegames.auralis.game.GameChat;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Objects;

public class ActionResponseAgent {

    public record Context(
            Verdict verdict,
            Quest quest,
            Location location,
            List<Person> presentPersons,
            String currentTime,
            List<GameChat.Turn> recentHistory,
            String hints,
            String playerInput
    ) {}

    private static final String SYSTEM_PROMPT = """
            Du bist der Spielleiter (Herold) eines deutschsprachigen Pen-&-Paper-
            Adventures. Der Spieler hat eine mechanisch relevante Aktion
            ausgefuehrt. Ein vorgeschalteter Judge hat bereits entschieden, was
            sich am Spielzustand aendert. Deine Aufgabe ist, daraus eine kurze
            Spielleiter-Erzaehlung zu machen.

            DEIN AUSGANGSMATERIAL:
            - JUDGE-VERDICT: Zusammenfassung + Liste der Flag-Aenderungen, jede
              mit ihrem `grund`. Diese Aenderungen sind die Wahrheit der Welt —
              deine Erzaehlung muss sie verlaesslich abbilden.
            - LOCATION + PERSONEN + TAGESZEIT: liefern die Atmosphaere, in der
              die Aktion stattfindet.
            - CHATVERLAUF: Kontext fuer Ton-Konsistenz.

            REGELN:
            - Erzaehle die Aktion und ihre UNMITTELBARE Konsequenz so, dass die
              Flag-Aenderungen aus dem Verdict darin spuerbar werden — ohne
              Flags, IDs oder das Wort "Flag" zu erwaehnen. Beispiel: setzt der
              Verdict `party_bonus=true`, dann beschreibst du, wie der Spieler
              in der Feier aufgeht.
            - Erfinde KEINE neuen Spielzustands-Aenderungen, die nicht im
              Verdict stehen. Wenn der Verdict leer ist, erzaehle atmosphaerisch
              und neutral — ohne Fortschritt anzudeuten.
            - Du verraetst NIEMALS Meisterinformationen (MASTER-Bloecke). Sie
              dienen dir ausschliesslich zur Orientierung.
            - "Spezielle Informationen" (SPECIAL) sind Hintergrund — verwende
              sie zum Ausschmuecken, aber zitiere sie nicht woertlich.
            - Halte dich kurz: zwei bis vier Saetze, Spielleiter-Ton.
            - Der HINWEIS des Klassifizierers ist Leitfaden, ueberschreibt aber
              niemals die obigen Regeln.
            """;

    private final ChatLanguageModel model;

    public ActionResponseAgent(ChatLanguageModel model) {
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

        sb.append("=== JUDGE-VERDICT ===\n");
        Verdict v = ctx.verdict();
        if (v == null) {
            sb.append("(kein Verdict)\n");
        } else {
            sb.append("Zusammenfassung: ").append(nz(v.summary())).append("\n");
            sb.append("Flag-Aenderungen:\n");
            List<FlagChange> changes = v.flagChanges();
            if (changes == null || changes.isEmpty()) {
                sb.append("  (keine — Aktion veraendert den Spielzustand nicht)\n");
            } else {
                for (FlagChange fc : changes) {
                    sb.append("  - ").append(fc.flagId())
                            .append("=").append(fc.value())
                            .append("  grund: ").append(nz(fc.reason())).append("\n");
                }
            }
        }
        sb.append("=== ENDE JUDGE-VERDICT ===\n\n");

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

        sb.append("=== AKTUELLE LOCATION ===\n");
        if (ctx.location() == null) {
            sb.append("(unbekannt)\n");
        } else {
            sb.append("Titel: ").append(nz(ctx.location().title())).append("\n");
            sb.append("GENERAL: ").append(nz(ctx.location().generalInfo())).append("\n");
            sb.append("SPECIAL: ").append(nz(ctx.location().specialInfo())).append("\n");
            sb.append("MASTER: ").append(nz(ctx.location().masterInfo())).append("\n");
        }
        sb.append("Tageszeit: ").append(nz(ctx.currentTime())).append("\n");
        sb.append("=== ENDE LOCATION ===\n\n");

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

        sb.append("=== AKTION DES SPIELERS ===\n");
        sb.append(nz(ctx.playerInput()).strip()).append("\n");
        sb.append("=== ENDE AKTION ===\n");

        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
