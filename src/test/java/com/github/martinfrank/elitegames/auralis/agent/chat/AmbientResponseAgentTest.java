package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.*;
import com.github.martinfrank.elitegames.auralis.agent.chat.AmbientResponseAgent.Context;
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
import java.util.Map;

class AmbientResponseAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String WALFISCH_ID = "0bd69e14-713c-4c65-ab28-898eb9d4a381";
    private static final String SUCHE_QUEST_ID = "1f5858e2-a28c-4f2b-b435-01b9051adf20";


    @Test
    void testMyWords() throws IOException {
        InputStream in = ClassifyInputAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest("7a91e163-860f-4db0-b231-08d76123afdb");
        Location location = adventure.getLocation(quest.startLocationId());

        GameSession session = new GameSession(adventure, new Party(List.of(new Adventurer("Rolf"))));

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
                new GameChat.Turn(GameChat.Source.HEROLD, "eingabe des spielers",
                        """
                                ich schaue mich auf dem Marktplatz um.
                                """)
        );

        ClassifyInputAgent.Classification c = new ClassifyInputAgent.Classification(
                ClassifyInputAgent.Category.AMBIENT,
                "Die Eingabe beschreibt die Atmosphäre des Marktplatzes.",
                null,
                null,
                "Beschreibe den Markplatz ausführlicher.",
                """
                     KATEGORIE: AMBIENT
                     BEGRUENDUNG: Die Eingabe beschreibt die Atmosphäre des Marktplatzes.
                     TARGET_TYP: KEINE
                     TARGET_ID: -
                     HINWEIS: Beschreibe den Markplatz ausführlicher.
                     """
        );

        Context context = new Context(
                location,
                quest,
                session.getOpenTasks(quest),
                session.getPresentPersons(location),
                "nachmittags",
                conversation,
                "Beschreibe den Markplatz ausführlicher.",
                "ich schaue mich auf dem Marktplatz um."
        );

        ChatLanguageModel model = chatModel();
        AmbientResponseAgent agent = new AmbientResponseAgent(model);
        String response = agent.respond(context);
        System.out.println(response);

    }


//    @Test
//    void ambientResponseInTavern() throws IOException {
//        InputStream in = AmbientResponseAgentTest.class.getResourceAsStream(RESOURCE);
//        Adventure adventure = new AdventureReader().read(in);
//
//        Quest quest = adventure.getQuest(SUCHE_QUEST_ID);
//        Location location = adventure.getLocation(WALFISCH_ID);
//        List<Person> present = location.persons().stream()
//                .map(PersonPresence::personId)
//                .map(id -> adventure.content().persons().stream()
//                        .filter(p -> p.id().equals(id))
//                        .findAny().orElseThrow())
//                .toList();
//
//        Context context = new Context(
//                location,
//                quest,
//                present,
//                "spaet abends",
//                Map.of(
//                        "walk_to_tavern", true,
//                        "await_arberds_intro", true,
//                        "escape_the_brawl", true,
//                        "found_arberds_address", false,
//                        "ready_to_visit_arberds", false,
//                        "party_bonus", false,
//                        "brawling_bonus", false
//                ),
//                List.of(),
//                "Atmosphaerische Antwort, baue einen subtilen Hinweis Richtung Wirt-Befragung ein.",
//                "Ich lehne mich an die Theke und schaue mich neugierig um."
//        );
//
//        ChatLanguageModel model = chatModel();
//        AmbientResponseAgent agent = new AmbientResponseAgent(model);
//
//        long start = System.currentTimeMillis();
//        String reply = agent.respond(context);
//        long duration = System.currentTimeMillis() - start;
//
//        System.out.println("=== ANTWORT ===");
//        System.out.println(reply);
//        System.out.println();
//        System.out.println("took " + duration + "ms");
//    }

    private static ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
