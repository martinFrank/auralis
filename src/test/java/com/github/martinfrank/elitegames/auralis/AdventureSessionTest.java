package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureParser;
import com.github.martinfrank.elitegames.auralis.adventure.Chapter;
import com.github.martinfrank.elitegames.auralis.adventure.Scene;
import com.github.martinfrank.elitegames.auralis.agent.ActionResponseAgent;
import com.github.martinfrank.elitegames.auralis.agent.ClassifyActionAgent;
import com.github.martinfrank.elitegames.auralis.agent.ScenePrepareAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureSessionTest {

    @Test
    void sessionTest() throws IOException {
//        String OLLAMA_URL = "http://localhost:11434";
        String OLLAMA_URL = "http://192.168.0.251:11434";
//        String MODEL = "mistral-Small:latest";
//        String MODEL = "mistral:7b";
//        String MODEL = "qwen3.5:27b";
        String MODEL = "qwen3:32b";
//        String MODEL = "qwen3:30b";
//        String MODEL = "gemma4:31b";
//        String MODEL = "deepseek-r1:32b";

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
        Scene scene1 = chapter.scenes().getFirst();

        //erste AI interaktion: szene aufsetzen
        ScenePrepareAgent prepareAgent = new ScenePrepareAgent(model);
        String introSzene1 = prepareAgent.prepareScene(gareth.common(), scene1.common());
        session.addHeroldMessage("[PREPARE_SCENE] "+introSzene1);
        session.addAdventureMessage(scene1.common());


//        String playerInputScene1 = "wir versuchen uns einen platz zu ergattern und ein paar Getränke zu bestellen";
//        String playerInputScene1 = "wir feiern und geniessen die Stimmung.";
//        String playerInputScene1 = "wir wollen den ganzen Abend in dieser Kneippe verbringen."; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInputScene1 = "wir holen uns Getränke und warten darauf, was passiert"; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInputScene1 = "wir setzen uns an die Theke und bestellen Bier beim Wirt."; //hoffetlich ist das ein weiterführendes entscheidung
        String playerInputScene1 = "Wir setzen uns an die Theke, bestellen Bier und fragen den Wirt nach Gerüchten"; //hoffetlich ist das ein weiterführendes entscheidung
        session.addPlayerMessage(playerInputScene1);

        ClassifyActionAgent classifyAgent = new ClassifyActionAgent(model);

        ClassifyActionAgent.Classification classificationActionScene1 =
                classifyAgent.classifyAction(scene1, playerInputScene1);
        System.out.println("action type: "+classificationActionScene1.classificationType());

//        if (classificationActionScene1.classificationType() == ClassifyActionAgent.ClassificationType.TRIVIAL) {
            ActionResponseAgent ambienteAgent = new ActionResponseAgent(model);
            String ambienteReplyScene1 = ambienteAgent.generateTrivialResponse(
                    scene1.common(),
                    playerInputScene1,
                    classificationActionScene1.heroldHints());
            session.addHeroldMessage("[AMBIENT] "+ambienteReplyScene1);
//        }


        //jetzt kommt die überleitung zur Szene 2

        Scene scene2 = chapter.scenes().get(1);
//        String introSzene2 = prepareAgent.prepareScene(scene1.common(), scene2.common());
//        session.addHeroldMessage("[PREPARE_SCENE] "+introSzene2);
        session.addAdventureMessage(scene2.common());

        String playerInputScene2 = "wir versuchen dem Mann hinterher zu laufen!";
        session.addPlayerMessage(playerInputScene2);

        ClassifyActionAgent.Classification classificationActionScene2 =
                classifyAgent.classifyAction(scene2, playerInputScene2);
        System.out.println("action type: "+classificationActionScene2.classificationType());

//        if (classificationActionScene2.classificationType() == ClassifyActionAgent.ClassificationType.DETAIL_INFO) {
//            AmbienteAgent ambienteAgent = new AmbienteAgent(model);
            String ambienteReplyScene2 = ambienteAgent.generateTrivialResponse(
                    scene2.common(),
                    playerInputScene2,
                    classificationActionScene2.heroldHints());
            session.addHeroldMessage("[AMBIENT] "+ambienteReplyScene2);
//        }

        //übergang zu scene 3
        System.out.println("------------------------");

        Scene scene3 = chapter.scenes().get(2);
        String introSzene3 = prepareAgent.prepareScene(scene2.common(), scene3.common());
        session.addHeroldMessage("[PREPARE_SCENE] "+introSzene3);
        session.addAdventureMessage(scene3.common());

//        String playerInputScene3 = "wir versuchen uns aus der Kneipe zu schmuggeln, ohne, dass wir weiter in die Schlägerei verwickelt werden!";
//        session.addPlayerMessage(playerInputScene3);
//
//
//        ClassifyActionAgent.Classification classificationActionScene3 =
//                classifyAgent.classifyAction(scene3, playerInputScene3);
//        System.out.println(classificationActionScene2);
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
