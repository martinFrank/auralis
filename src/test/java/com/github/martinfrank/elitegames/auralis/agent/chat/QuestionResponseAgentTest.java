package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.adventure.Location;
import com.github.martinfrank.elitegames.auralis.adventure.Person;
import com.github.martinfrank.elitegames.auralis.adventure.PersonPresence;
import com.github.martinfrank.elitegames.auralis.adventure.Quest;
import com.github.martinfrank.elitegames.auralis.agent.chat.QuestionResponseAgent.Context;
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

class QuestionResponseAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String WALFISCH_ID = "0bd69e14-713c-4c65-ab28-898eb9d4a381";
    private static final String SUCHE_QUEST_ID = "1f5858e2-a28c-4f2b-b435-01b9051adf20";

    @Test
    void testAskForTavern() throws IOException {
        InputStream in = ClassifyInputAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest("7a91e163-860f-4db0-b231-08d76123afdb");
        Location location = adventure.getLocation(quest.startLocationId());

        GameSession session = new GameSession(adventure, new Party(List.of(new Adventurer("Rolf"))));

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
        new GameChat.Turn(GameChat.Source.HEROLD, "eingabe des spielers",
                """
                        Ich frage einen Händler, ob es hier eine gute Taverne gibt.
                        """)
        );

        //Klassifikation vom ClassificationAgent
        ClassifyInputAgent.Classification classifikation = new ClassifyInputAgent.Classification(
                ClassifyInputAgent.Category.QUESTION,
                "Der Spieler stellt eine Frage an den Spielleiter um Wissen über die Umgebung zu erhalten.",
                null,
                null,
                "Stelle dem Spieler eine Antwort als Händler in der Marktgasse zur Verfügung und teile ihm Informationen über die Taverne zum tanzenden Stier.",
                """
                     KATEGORIE: QUESTION
                     BEGRUENDUNG: Der Spieler stellt eine Frage an den Spielleiter um Wissen über die Umgebung zu erhalten.
                     TARGET_TYP: KEINE
                     TARGET_ID: ->
                     HINWEIS: Stelle dem Spieler eine Antwort als Händler in der Marktgasse zur Verfügung und teile ihm Informationen über die Taverne zum tanzenden Stier.
                     """
        );

        //Kontext, wie er aufgebaut werden muss
        Context context = new Context(
                location,
                quest,
                List.of(), //leider keine personen anwesend
                "nachmittags",
                session.getRevealedLocations(),
                session.getRevealedPersons(),
                session.getRevealedItems(),
                conversation,
                "Stelle dem Spieler eine Antwort als Händler in der Marktgasse zur Verfügung und teile ihm Informationen über die Taverne zum tanzenden Stier.",
                "Ich frage einen Händler, ob es hier eine gute Taverne gibt."
        );

        //antwort generieren
        ChatLanguageModel model = chatModel();
        QuestionResponseAgent agent = new QuestionResponseAgent(model);

        long start = System.currentTimeMillis();
        String reply = agent.respond(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== ANTWORT ===");
        System.out.println(reply);
        System.out.println();
        System.out.println("took " + duration + "ms");
    }

    @Test
    void testAskForWeaponshop() throws IOException {
        InputStream in = ClassifyInputAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest("7a91e163-860f-4db0-b231-08d76123afdb");
        Location location = adventure.getLocation(quest.startLocationId());
        GameSession session = new GameSession(adventure, new Party(List.of(new Adventurer("Rolf"))));

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
                new GameChat.Turn(GameChat.Source.HEROLD, "eingabe des spielers",
                        """
                                Gibt es hier einen Waffenladen?.
                                """)
        );

        //Klassifikation vom ClassificationAgent
        ClassifyInputAgent.Classification classifikation = new ClassifyInputAgent.Classification(
                ClassifyInputAgent.Category.QUESTION,
                "Der Spieler fragt nach dem Verfügbarkeit eines Waffenladens, was Weltwissen impliziert.",
                ClassifyInputAgent.TargetType.LOCATION,
                "->",
                "Antworte mit einer Bestätigung oder Ablehnung und informiere denSpieler, ob es in der Stadt einen solchen Laden gibt.",
                """
                     KATEGORIE: QUESTION
                     BEGRUENDUNG: Der Spieler fragt nach dem Verfügbarkeit eines Waffenladens, was Weltwissen impliziert.
                     TARGET_TYP: LOCATION
                     TARGET_ID: ->
                     HINWEIS: Antworte mit einer Bestätigung oder Ablehnung und informiere denSpieler, ob es in der Stadt einen solchen Laden gibt.
                     """
        );

        //Kontext, wie er aufgebaut werden muss
        Context context = new Context(
                location,
                quest,
                List.of(), //leider keine personen anwesend
                "nachmittags",
                session.getRevealedLocations(),
                session.getRevealedPersons(),
                session.getRevealedItems(),
                conversation,
                "Antworte mit einer Bestätigung oder Ablehnung und informiere denSpieler, ob es in der Stadt einen solchen Laden gibt.",
                "Gibt es hier einen Waffenladen?."
        );

        //antwort generieren
        ChatLanguageModel model = chatModel();
        QuestionResponseAgent agent = new QuestionResponseAgent(model);

        long start = System.currentTimeMillis();
        String reply = agent.respond(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== ANTWORT ===");
        System.out.println(reply);
        System.out.println();
        System.out.println("took " + duration + "ms");
    }

    @Test
    void answersQuestionAboutInnkeeper() throws IOException {
        InputStream in = QuestionResponseAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest(SUCHE_QUEST_ID);
        Location location = adventure.getLocation(WALFISCH_ID);
        List<Person> present = location.persons().stream()
                .map(PersonPresence::personId)
                .map(id -> adventure.content().persons().stream()
                        .filter(p -> p.id().equals(id))
                        .findAny().orElseThrow())
                .toList();

        Context context = new Context(
                location,
                quest,
                present,
                "spaet abends",
                List.of(location),
                present,
                List.of(),
                List.of(),
                "Antworte als Spielleiter, beschreibe den Wirt anhand seiner allgemeinen Informationen.",
                "Was kannst du mir ueber den Wirt erzaehlen?"
        );

        ChatLanguageModel model = chatModel();
        QuestionResponseAgent agent = new QuestionResponseAgent(model);

        long start = System.currentTimeMillis();
        String reply = agent.respond(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== ANTWORT ===");
        System.out.println(reply);
        System.out.println();
        System.out.println("took " + duration + "ms");
    }

    private static ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
