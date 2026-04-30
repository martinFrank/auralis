package com.github.martinfrank.elitegames.auralis.agent;

import com.github.martinfrank.elitegames.auralis.adventure.*;
import com.github.martinfrank.elitegames.auralis.agent.ClassifyInputAgent.Category;
import com.github.martinfrank.elitegames.auralis.agent.ClassifyInputAgent.Classification;
import com.github.martinfrank.elitegames.auralis.agent.ClassifyInputAgent.Context;
import com.github.martinfrank.elitegames.auralis.agent.ClassifyInputAgent.TargetType;
import com.github.martinfrank.elitegames.auralis.game.GameChat;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyInputAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen2.5:7b";
//    private static final String MODEL = "mistral:7b";
//    private static final String MODEL = "qwen3.5:9b";
//    private static final String MODEL = "qwen3:30b";
//    private static final String MODEL = "deepseek-r1:8b";
//    private static final String MODEL = "qwen3:32b"; //SPILL!!! von cahtgpt empfohlen
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    private static final String WALFISCH_ID = "0bd69e14-713c-4c65-ab28-898eb9d4a381";
    private static final String SUCHE_QUEST_ID = "1f5858e2-a28c-4f2b-b435-01b9051adf20";

    @Test
    void classifiesDialogWithWirt() throws IOException {
        InputStream in = ClassifyInputAgentTest.class.getResourceAsStream(RESOURCE);
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
                quest,
                location,
                present,
                List.of(),
                "Ich frage den Wirt nach dem entfuehrten Vater."
        );

        ChatLanguageModel model = chatModel();
        ClassifyInputAgent agent = new ClassifyInputAgent(model);

        Classification c = agent.classify(context);

        System.out.println("=== KLASSIFIKATION ===");
        System.out.println("Kategorie:   " + c.category());
        System.out.println("Begruendung: " + c.reasoning());
        System.out.println("TargetTyp:   " + c.targetType());
        System.out.println("TargetId:    " + c.targetId());
        System.out.println("Hinweis:     " + c.hints());
        System.out.println();
        System.out.println("=== ROHANTWORT ===");
        System.out.println(c.raw());
    }

    @Test
    void testMyWords() throws IOException {
        InputStream in = ClassifyInputAgentTest.class.getResourceAsStream(RESOURCE);
        Adventure adventure = new AdventureReader().read(in);

        Quest quest = adventure.getQuest("7a91e163-860f-4db0-b231-08d76123afdb");
        Location location = adventure.getLocation(quest.startLocationId());

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
                                """)
        );

        Context context = new Context(
                quest,
                location,
                List.of(),
                conversation,
                "Ich frage einen Händler, ob es hier eine gute Taverne gibt."
//                "Gibt es hier einen Waffenladen?."
        );

        ChatLanguageModel model = chatModel();
        ClassifyInputAgent agent = new ClassifyInputAgent(model);
        long start = System.currentTimeMillis();
        Classification c = agent.classify(context);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== KLASSIFIKATION ===");
        System.out.println("Kategorie:   " + c.category());
        System.out.println("Begruendung: " + c.reasoning());
        System.out.println("TargetTyp:   " + c.targetType());
        System.out.println("TargetId:    " + c.targetId());
        System.out.println("Hinweis:     " + c.hints());
        System.out.println();
        System.out.println("=== ROHANTWORT ===");
        System.out.println(c.raw());
        System.out.println("took "+duration+"ms");
    }

    private static ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
//                .numCtx(4096)
                .build();
    }


}
