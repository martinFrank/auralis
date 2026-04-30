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

public class SetupQuestAgent {

    private static final String SYSTEM_PROMPT = """
            Du bist der Buehnenmeister von Auralis. Deine Aufgabe ist es,
            eine kurze Einleitung fuer das anstehende Quest zu schreiben.
            Antworte stets auf Deutsch und im Ton eines Spielleiters am Tisch.

            STRIKTE REGELN:
            - Du fuehrst die Helden in das Quest ein — du erzaehlst es NICHT
              voraus. Keine Wendungen, keine NSCs, keine Ergebnisse vorwegnehmen.
            - Du verraetst NIEMALS Meisterinformationen oder "Spezielle
              Informationen" aus dem Quest. Sie dienen dir ausschliesslich zur
              Orientierung.
            - Erwaehne keine Aufgaben, Teilziele oder Tasks — sie sind nicht
              Teil deines Wissens und gehoeren nicht in die Einleitung.
            - Direkt nach deiner Einleitung wird der allgemeine Beschreibungstext
              der Start-Location vorgelesen. Gestalte deine Einleitung so, dass
              dieser Folgetext nahtlos anschliesst — raeumlich, zeitlich und
              tonal. Greife dem Folgetext aber nicht vor und wiederhole ihn nicht.
            - Halte die Einleitung kompakt (wenige Saetze, hoechstens ein kurzer
              Absatz).
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Schreibe die Einleitung fuer das folgende Quest.

            === QUEST ===
            Titel: %s
            Allgemeine Information: %s
            Spezielle Information (nur Orientierung, nicht zitieren): %s
            Meisterinformation (nur Orientierung, nicht zitieren): %s
            === ENDE QUEST ===

            === START-SITUATION ===
            Ort: %s
            Tageszeit: %s
            === ENDE START-SITUATION ===

            Direkt nach deiner Einleitung wird der folgende Beschreibungstext
            der Start-Location vorgelesen. Lass deine Einleitung tonal,
            raeumlich und zeitlich darin muenden, ohne ihn zu wiederholen oder
            seinen Inhalt vorwegzunehmen.

            === FOLGETEXT (nur zur Orientierung, nicht wiedergeben) ===
            %s
            === ENDE FOLGETEXT ===
            """;

    private final ChatLanguageModel model;

    public SetupQuestAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public String setupQuest(Quest quest, Location startLocation, String time) {
        Objects.requireNonNull(quest, "quest");
        Objects.requireNonNull(startLocation, "startLocation");
        String prompt = USER_PROMPT_TEMPLATE.formatted(
                nullToEmpty(quest.title()),
                nullToEmpty(quest.generalInfo()),
                nullToEmpty(quest.specialInfo()),
                nullToEmpty(quest.masterInfo()),
                nullToEmpty(startLocation.title()),
                nullToEmpty(time),
                nullToEmpty(startLocation.generalInfo())
        );
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(prompt)
        ));
        return response.content().text();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
