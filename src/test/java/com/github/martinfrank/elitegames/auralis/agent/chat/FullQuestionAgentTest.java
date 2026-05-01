package com.github.martinfrank.elitegames.auralis.agent.chat;

import com.github.martinfrank.elitegames.auralis.adventure.*;
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

class FullQuestionAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
//    private static final String MODEL = "qwen2.5:14b";
//    private static final String MODEL = "qwen3.5:9b";
//    private static final String MODEL = "deepseek-r1:7b";
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";


    @Test
    void testAskAnything() throws IOException {
        InputStream in = ClassifyInputAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest("7a91e163-860f-4db0-b231-08d76123afdb");
        Location location = adventure.getLocation(quest.startLocationId());
        GameSession session = new GameSession(adventure, new Party(List.of(new Adventurer("Rolf"))));

        String question = "Ich will ein Bier trinken - gibt es hier eine Brauerei?";

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
                new GameChat.Turn(GameChat.Source.HEROLD, "eingabe des spielers", question)
        );

        ClassifyInputAgent.Context classificationContext = new ClassifyInputAgent.Context(
                quest,
                location,
                List.of(),
                conversation,
                question
        );

        ChatLanguageModel model = chatModel();
        ClassifyInputAgent classificationAgent = new ClassifyInputAgent(model);
        long start = System.currentTimeMillis();
        ClassifyInputAgent.Classification c = classificationAgent.classify(classificationContext);

        //Kontext, wie er aufgebaut werden muss
        Context questionResponseContext = new Context(
                location,
                quest,
                List.of(), //leider keine personen anwesend
                "nachmittags",
                session.getRevealedLocations(),
                session.getRevealedPersons(),
                session.getRevealedItems(),
                conversation,
                c.hints(),
                question
        );

        //antwort generieren
        QuestionResponseAgent questionResponseAgent = new QuestionResponseAgent(model);
        String reply = questionResponseAgent.respond(questionResponseContext);
        System.out.println("=== ANTWORT ===");
        System.out.println(reply);
        System.out.println();
    }

    private static ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .numCtx(8192)
                .build();
    }
}
