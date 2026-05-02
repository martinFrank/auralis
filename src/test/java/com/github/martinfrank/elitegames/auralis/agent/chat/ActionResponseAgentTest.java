package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.FlagChange;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionJudgeAgent.Verdict;
import com.github.martinfrank.elitegames.auralis.agent.chat.ActionResponseAgent.Context;
import com.github.martinfrank.elitegames.auralis.character.Adventurer;
import com.github.martinfrank.elitegames.auralis.character.Party;
import com.github.martinfrank.elitegames.auralis.game.GameChat;
import com.github.martinfrank.elitegames.auralis.game.GameSession;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

class ActionResponseAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String TANZENDEN_STIER_LOCATION_ID = "e5815fa0-1885-46a1-b477-ea5ddcee04e2";
    private static final String TANZENDEN_STIER_QUEST_ID = "5ce0f5a5-a9d4-467a-bfc8-85019aab6aff";

    @Test
    void testgoToDancingBull() throws IOException {
        InputStream in = ClassifyInputAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest("7a91e163-860f-4db0-b231-08d76123afdb");
        Location location = adventure.getLocation(quest.startLocationId());
        GameSession session = new GameSession(adventure, new Party(List.of(new Adventurer("Rolf"))));


        String playerAction = "wir machen uns auf, um die Taverne zum tanzenden Stier zu besuchen.";

        //aktueller chatverlauf bisher
        List<GameChat.Turn> conversation = List.of(
                new GameChat.Turn(GameChat.Source.HEROLD, "statischer Introtext",
                        """
                                willkommen bei dem Abenteuer "Verführung zur Entführung" von Oliver Eickenberg (1997)
                                """),
                new GameChat.Turn(GameChat.Source.HEROLD, "AI generiert Text für das Einleiten eines neuen quests",
                        """
                                Nun findet ihr euch am Zentralen Platz im Handelsviertel von Gareth. Der Nachmittag zieht
                                sich langsam dahin, und die Stadt lebt in voller Pracht. Blicke empor zu dem prächtigen
                                Brunnen, dessen Wasserstrahlen in der sanften Abendsonne funkeln und das Licht spiegeln. Um
                                euch herum drängt sich die Menge – Händler, Handwerker, Adlige – alle bereit für den
                                abendlichen Ausflug.
                                
                                Fragt umher, und ihr werdet hören, dass man von der Taverne zum tanzenden Stier spricht, die
                                das beste Gasthaus der Stadt ist. Es liegt direkt hier in der Nähe, und es ist noch früh
                                genug, um vor dem Eintritt des Abendlichts ein wenig umherzuschnüffeln oder vielleicht
                                einige Einkäufe zu erledigen, bevor ihr euch entscheidet, ob das Geleise in die Taverne
                                geführt hat.
                                """),
                new GameChat.Turn(GameChat.Source.HEROLD, "quest-location allgemeine beschreibung",
                        """
                                Der Marktplatz liegt inmitten der Kaiserstadt und ist der Herzpunkt aller Aktivitäten.
                                Er wird von einem großen, prächtigen Brunnen dominiert, der mit Figuren aus Bronze
                                geschmückt ist. Die Wasserstrahlen des Brunnens springen aufwärts in einem bunten Spiel
                                aus Licht und Wasser, beleuchtet durch das sanfte Licht, das von den umliegenden
                                Gebäuden reflektiert wird. Um den Marktplatz herum stehen prachtvolle Gebäude, in denen
                                sich luxuriöse Geschäfte, Gasthöfe und sogar das königliche Amt befinden
                                
                                Der Platz ist ein Treffpunkt für alle Schichten der Gesellschaft. An den Ständen des
                                Marktes bietet man alles vom frischesten Obst und Gemüse bis hin zu hochwertigen
                                Edelsteinen an. Die Atmosphäre ist lebendig, mit dem Läuten von Glöckchen bei jedem
                                neuen Kauf, den Gesang der Händlerinnen beim Verkauf ihrer Waren und das Gelächter des
                                Volksgeschehens.
                                """),
                new GameChat.Turn(GameChat.Source.HEROLD, "eingabe des spielers", playerAction)
        );

        ClassifyInputAgent.Classification classification = new ClassifyInputAgent.Classification(
                ClassifyInputAgent.Category.ACTION,
                "Der Spieler beschreibt eine mechanisch relevante Handlung.",
                ClassifyInputAgent.TargetType.TRANSITION,
                "->",
                "Führe den Spielzustand zu der Taverne zum tanzenden Stier über.",
                """
                    KATEGORIE: ACTION
                    BEGRUENDUNG: Der Spieler beschreibt eine mechanisch relevante Handlung.
                    TARGET_TYP: TRANSITION
                    TARGET_ID: ->
                    HINWEIS: Führe den Spielzustand zu der Taverne zum tanzenden Stier über.
                    """
        );

        Verdict verdict = new Verdict(
                "Die Helden beginnen ihren Weg zur Taverne zum tanzenden Stier.",
                new ActionJudgeAgent.LocationChange(
                        "e5815fa0-1885-46a1-b477-ea5ddcee04e2",
                        "Die Gruppe bewegt sich vom Marktplatz zum Tor zur Kaiserstadt, um die Taverne zu erreichen."
                ),
                new ActionJudgeAgent.TimeChange(
                        "evening",
                        "Es ist Nachmittag und sie beginnen den Marsch zur Taverne, was das Abendlicht einsetzen lässt."
                ),
                List.of(new FlagChange(
                        "walk_to_tavern", true, "(grund: Die Helden machen sich auf den Weg zur gewünschten Taverne.)"
                )),
                """
                    ZUSAMMENFASSUNG: Die Helden beginnen ihren Weg zur Taverne zum tanzenden Stier.
                    BEWEGUNG: keine
                    ZEIT: keine
                    FLAG_AENDERUNGEN:
                    - id=walk_to_tavern wert=true grund="Die Helden machen sich auf den Weg zur gewünschten Taverne."
                    """
        );

        Context context = new Context(
                verdict,
                quest,
                location,
                session.getPresentPersons(location),
                "abends",
                conversation,
                classification.hints(),
                playerAction);

        ActionResponseAgent agent = new ActionResponseAgent(chatModel());
        long start = System.currentTimeMillis();
        String reply = agent.respond(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== EINGABE ===");
        System.out.println(context.playerInput());
        System.out.println("=== VERDICT ===");
        System.out.println(context.verdict());
        System.out.println("=== ANTWORT ===");
        System.out.println(reply);
        System.out.println("took " + duration + "ms");
        System.out.println();

    }


    @Test
    void narratesActionWithFlagFlip() throws IOException {
        Context context = loadContext(
                new Verdict(
                        "Die Helden stuerzen sich in die Feier und trinken reichlich.",
                        null,
                        null,
                        List.of(new FlagChange("party_bonus", true,
                                "Spieler beteiligt sich aktiv an der Feierei")),
                        ""),
                "Wir bestellen reichlich Bier, stossen mit den Trunkenbolden an und singen mit.",
                "Beschreibe die Feier-Szene, der Spieler ist dabei.");
        runAndPrint(context);
    }

    @Test
    void narratesActionWithoutFlagFlip() throws IOException {
        Context context = loadContext(
                new Verdict(
                        "Spieler probiert eine Aktion, die nichts veraendert.",
                        null,
                        null,
                        List.of(),
                        ""),
                "Ich versuche, das Wappen ueber der Tuer von innen zu drehen.",
                "Aktion ohne Spielmechanik-Auswirkung, atmosphaerisch beschreiben.");
        runAndPrint(context);
    }

    private static Context loadContext(Verdict verdict, String playerInput, String hints) throws IOException {
        InputStream in = ActionResponseAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);
        Quest quest = adventure.getQuest(TANZENDEN_STIER_QUEST_ID);
        Location location = adventure.getLocation(TANZENDEN_STIER_LOCATION_ID);
        return new Context(
                verdict, quest, location, List.of(), "abends",
                List.of(), hints, playerInput);
    }

    private static void runAndPrint(Context context) {
        ChatLanguageModel model = chatModel();
        ActionResponseAgent agent = new ActionResponseAgent(model);

        long start = System.currentTimeMillis();
        String reply = agent.respond(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== EINGABE ===");
        System.out.println(context.playerInput());
        System.out.println("=== VERDICT ===");
        System.out.println(context.verdict());
        System.out.println("=== ANTWORT ===");
        System.out.println(reply);
        System.out.println("took " + duration + "ms");
        System.out.println();
    }

    private static ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
