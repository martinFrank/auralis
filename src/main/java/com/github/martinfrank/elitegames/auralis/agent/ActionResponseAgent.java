package com.github.martinfrank.elitegames.auralis.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class ActionResponseAgent {

    private static final String DEFAULT_OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String DEFAULT_MODEL = "mistral";

    private static final String SYSTEM_PROMPT_TRIVIAL = """
            Du bist der Herold von Auralis in der Rolle eines Spielleiters,
            der ausschliesslich ERGAENZENDEN Szenen-Inhalt liefert. Die
            Handlung des Kapitels bleibt stehen — du baust nur Atmosphaere,
            Sinneseindruecke und kleine Reaktionen der Welt auf die
            letzte Aktion des Spielers. Antworte stets auf Deutsch, im
            Ton eines klassischen Spielleiters am Tisch.

            STRIKTE REGELN:
            - Du TREIBST die Handlung NICHT voran. Keine neuen NSCs, die
              den Plot tragen, keine neuen Hinweise, keine Wendungen,
              keine Zeitspruenge, keine Ortwechsel.
            - Die SZENE ist deine zentrale Referenz: Ort, Stimmung,
              anwesende Figuren, Atmosphaere und Details musst du
              konsequent aus der beschriebenen Szene nehmen. Erfinde
              nichts, was der Szene widerspricht.
            - Der HINWEIS des Schiedsrichters ist NUR ein Vorschlag,
              keine Vorgabe. Nimm ihn als Impuls auf, wenn er zur Szene
              passt; ignoriere oder biege ihn ab, wenn er der Szene
              widerspricht oder ueber sie hinausgeht. Zitiere ihn NICHT
              woertlich.
            - Beschreibe die unmittelbare Umgebung, Geraeusche, Gerueche,
              Gesten von Nebenfiguren, das Ergebnis der Spieleraktion im
              Kleinen (z. B. Getraenk wird gebracht, Blicke werden
              gewechselt, ein Lied beginnt) — immer aus der Szene heraus.
            - Halte die Antwort kompakt: ein kurzer Absatz ohne den Plot anzubieten.
            """;

    private static final String SYSTEM_PROMPT_DETAIL = """
            Du bist der Herold von Auralis in der Rolle eines Spielleiters,
            der auf eine gezielte Spieleraktion reagiert, indem er
            PASSGENAU Details aus der aktuellen Szene enthuellt. Antworte
            stets auf Deutsch, im Ton eines klassischen Spielleiters am
            Tisch.

            STRIKTE REGELN:
            - Die SZENE mit allen drei Abschnitten (Allgemeine
              Informationen, Spezielle Informationen, Meisterinformationen)
              ist deine EINZIGE Quelle. Was dort steht, darfst du im
              Rollenspiel preisgeben; was dort nicht steht, erfindest du
              NICHT.
            - Enthuelle GENAU die Teile der Speziellen- oder Meister-
              informationen, die zur Spieleraktion passen. Alles andere
              bleibt geheim — auch nicht angedeutet.
            - Zitiere die Meisterinformationen NIEMALS woertlich und
              lies sie nicht vor. Uebersetze sie in Szene: NSC-Dialog,
              Beobachtung, Geruecht, Gerucheindruck, Geste, Tonfall.
            - Keine neuen NSCs von ausserhalb der Szene, keine neuen
              Orte, keine Zeitspruenge, kein Plot-Sprung ueber das
              hinaus, was die Szene selbst hergibt.
            - Der HINWEIS des Schiedsrichters ist nur ein Impuls, keine
              Vorgabe. Nimm ihn auf, wenn er zur Szene passt; ignoriere
              ihn, wenn er ueber die Szene hinausgeht. Zitiere ihn nicht
              woertlich.
            - Gibt die Szene zur Aktion nichts her, reagiere knapp und
              ehrlich im Rollenspiel-Ton (Schulterzucken, ausweichender
              Blick, "davon weiss ich nichts"), statt etwas zu erfinden.
            - Halte die Antwort kompakt: ein bis zwei kurze Absaetze,
              szenisch verankert.
            """;

    private static final String USER_PROMPT_TRIVIAL_TEMPLATE = """
            === AKTUELLE SZENE ===
            %s
            === ENDE AKTUELLE SZENE ===

            === LETZTE SPIELERAKTION ===
            %s
            === ENDE SPIELERAKTION ===

            === VORSCHLAG DES SCHIEDSRICHTERS (optionaler Impuls, kein Befehl) ===
            %s
            === ENDE VORSCHLAG ===

            Liefere jetzt eine rein ergaenzende, atmosphaerische Reaktion
            auf die Spieleraktion. Beziehe dich PRIMAER auf die AKTUELLE
            SZENE; der Vorschlag dient nur als Inspiration und darf
            ignoriert werden, wenn er nicht zur Szene passt. Keine
            Plot-Fortschritte.
            """;

    private static final String USER_PROMPT_DETAIL_TEMPLATE = """
            === AKTUELLE SZENE (inkl. Allgemeine Informationen,
                Spezielle Informationen und Meisterinformationen) ===
            %s
            === ENDE AKTUELLE SZENE ===

            === LETZTE SPIELERAKTION ===
            %s
            === ENDE SPIELERAKTION ===

            === HINWEIS DES SCHIEDSRICHTERS (Impuls, kein Befehl) ===
            %s
            === ENDE HINWEIS ===

            Die Spieleraktion zielt gezielt darauf ab, Informationen aus
            der Szene hervorzulocken. Enthuelle GENAU jene Teile aus
            "Spezielle Informationen" oder "Meisterinformationen", die
            zur Aktion passen — nicht mehr, nicht weniger — und bette
            sie ins Rollenspiel ein.

            HINWEISE FUER DIESEN FALL:
            - Waehle nur die Fragmente der Detail-/Meisterinformationen,
              die zur konkreten Aktion passen. Alles andere bleibt
              geheim.
            - Richtet sich die Aktion an einen NSC der Szene, antworte
              aus dessen Perspektive mit Stimme, Temperament und
              Eigenheiten, wie sie in der Szene angelegt sind.
            - Verknuepfe die Enthuellung mit sinnlichen Details der
              Szene (Laerm, Licht, Gesten), damit sich die Antwort wie
              ein Moment am Spieltisch anfuehlt.
            - Zitiere die Meisterinformationen nicht woertlich; erzaehle
              sie in Szene.
            """;

    private final ChatLanguageModel model;

    public ActionResponseAgent(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public static ActionResponseAgent withDefaults() {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(DEFAULT_OLLAMA_URL)
                .modelName(DEFAULT_MODEL)
                .timeout(Duration.ofMinutes(2))
                .build();
        return new ActionResponseAgent(model);
    }

    public String generateTrivialResponse(String scene, String playerInput, String heroldHint) {
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT_TRIVIAL),
                UserMessage.from(USER_PROMPT_TRIVIAL_TEMPLATE.formatted(
                        scene, playerInput, heroldHint))
        ));
        return response.content().text();
    }

    public String generateDetailResponse(String fullContent, String playerInput, String heroldHint) {
        Response<AiMessage> response = model.generate(List.of(
                SystemMessage.from(SYSTEM_PROMPT_DETAIL),
                UserMessage.from(USER_PROMPT_DETAIL_TEMPLATE.formatted(
                        fullContent, playerInput, heroldHint))
        ));
        return response.content().text();
    }
}
