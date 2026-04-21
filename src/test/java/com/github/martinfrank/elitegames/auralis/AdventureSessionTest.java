package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureParser;
import com.github.martinfrank.elitegames.auralis.adventure.Chapter;
import com.github.martinfrank.elitegames.auralis.adventure.Scene;
import com.github.martinfrank.elitegames.auralis.agent.AmbienteAgent;
import com.github.martinfrank.elitegames.auralis.agent.ClassifyActionAgent;
import com.github.martinfrank.elitegames.auralis.agent.ScenePrepareAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureSessionTest {

    @Test
    void sessionTest() throws IOException {
//        String OLLAMA_URL = "http://localhost:11434";
        String OLLAMA_URL = "http://192.168.0.251:11434";
//        String MODEL = "Mistral-Small:latest";
//        String MODEL = "mistral:7b";
        String MODEL = "qwen3:32b";

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();


        Adventure adventure = AdventureParser.fromClasspath("entfuehr.md");
        AdventureSession session = new AdventureSession();

        Chapter intro = adventure.chapters().getFirst();
        Scene gareth = intro.scenes().getFirst();

        Chapter chapter = adventure.chapters().get(1); //0 = intro, 1=taverne
        System.out.println(chapter.title());
        Scene scene1 = chapter.scenes().getFirst();
        System.out.println(scene1.title());

        //erste AI interaktion: szene aufsetzen
        ScenePrepareAgent prep = new ScenePrepareAgent(model);
        String introSzene1 = prep.prepareScene(gareth.content(), scene1.common());
        session.addHeroldMessage(introSzene1);
        session.addAdventureMessage(scene1.common());


//        String playerInput = "wir versuchen uns einen platz zu ergattern und ein paar Getränke zu bestellen";
        String playerInput = "wir feiern und geniessen die Stimmung.";
//        String playerInput = "wir wollen den ganzen Abend in dieser Kneippe verbringen."; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInput = "wir holen uns Getränke und warten darauf, was passiert"; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInput = "wir setzen uns an die Theke und bestellen Bier beim Wirt."; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInput = "Wir fragen den Wirt nach Gerüchten"; //hoffetlich ist das ein weiterführendes entscheidung
        session.addPlayerMessage(playerInput);

        ClassifyActionAgent classifyAgent = new ClassifyActionAgent(model);

        ClassifyActionAgent.Classification classification =
                classifyAgent.classifyAction(scene1, playerInput);
        System.out.println(classification);

        if (classification.classification() == ClassifyActionAgent.ClassificationType.TRIVIAL) {
            AmbienteAgent ambiente = new AmbienteAgent(model);
            String ambienteReply = ambiente.generateAmbiente(
                    scene1,
                    playerInput,
                    classification.hinweisFuerHerold());
            session.addHeroldMessage(ambienteReply);
        }

        //jetzt kommt die überleitung zur Szene 2

        Scene scene2 = chapter.scenes().get(1);
        String introSzene2 = prep.prepareScene(scene1.common(), scene2.common());
        session.addHeroldMessage(introSzene2);
        session.addAdventureMessage(scene2.common());
//
//        if (additionalCount >= 2 || judgement.classification() == ClassifyActionAgent.Classification.WEITERFUEHREND) {
//
//            String bgInfo = session.transcript().get(session.transcript().size()-2).content() + "\n"+
//                    session.transcript().getLast().content();
//
//            String nnexxtt = prep.prepareScene(tavernTitle, bgInfo, nextScene);
//            session.recordHeroldFragment(nnexxtt);
//            session.recordAdventureFragment(nextScene);
//        }
//
////        List<AdventureSession.Turn> transcript = session.transcript();
//        transcript.forEach(System.out::println);

        List<AdventureSession.Turn> transcript = session.transcript();
        transcript.forEach(System.out::println);
        System.out.println("----------------");
    }
//
//
//    private static final class RecordingHeroldStub extends HeroldAgent {
//        private final List<String> seenInputs = new ArrayList<>();
//        private int counter = 0;
//
//        private RecordingHeroldStub(String adventureText) {
//            super(null, adventureText);
//        }
//
//        @Override
//        public String chat(String playerAction) {
//            seenInputs.add(playerAction);
//            counter++;
//            return "Stub-Antwort #" + counter + " auf: " + playerAction;
//        }
//    }
}
