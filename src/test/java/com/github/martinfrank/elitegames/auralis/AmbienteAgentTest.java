package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureParser;
import com.github.martinfrank.elitegames.auralis.adventure.Chapter;
import com.github.martinfrank.elitegames.auralis.adventure.Scene;
import com.github.martinfrank.elitegames.auralis.agent.ActionResponseAgent;
import com.github.martinfrank.elitegames.auralis.agent.ClassifyActionAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

public class AmbienteAgentTest {

    @Test
    void ambienteTest() throws IOException {
//        String OLLAMA_URL = "http://localhost:11434";
        String OLLAMA_URL = "http://192.168.0.251:11434";
//        String MODEL = "Mistral-Small:latest";
//        String MODEL = "mistral:7b";
//        String MODEL = "qwen3.5:27b";
//        String MODEL = "qwen3:32b";
        String MODEL = "qwen3:30b";
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
        //wird hier vorgefertigt eingefügt
        String introSzene1 = """
                die Helden schlendern durch die belebten Gassen von Gareth, während
                die Sonne langsam untergeht und die Stadt in ein warmes, goldenes
                Licht taucht. Die Luft ist erfüllt vom Lachen und Reden der Passanten,
                und der Duft von gebratenem Fleisch und frischem Brot zieht durch die
                Straßen. Sie nähern sich dem Herzen der Kaiserstadt, wo die Geräusche
                lauter werden und die Menschen dichter zusammenstehen.

                Plötzlich hören sie das laute Gelächter und die fröhliche Musik aus
                einer nahen Taverne. Die Tür steht weit offen, und die Stimmen der
                Gäste dringen nach draußen, ein Zeichen für eine ausgelassene Stimmung
                im Inneren.
                """;
        session.addHeroldMessage("[PREPARE_SCENE] "+introSzene1);
        session.addAdventureMessage(scene1.common());

//        String playerInputScene1 = "wir versuchen uns einen platz zu ergattern und ein paar Getränke zu bestellen";
        String playerInputScene1 = "wir feiern und geniessen die Stimmung.";
//        String playerInputScene1 = "wir wollen den ganzen Abend in dieser Kneippe verbringen."; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInputScene1 = "wir holen uns Getränke und warten darauf, was passiert"; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInputScene1 = "wir setzen uns an die Theke und bestellen Bier beim Wirt."; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInputScene1 = "Wir setzen uns an die Theke, bestellen Bier und fragen den Wirt nach Gerüchten"; //hoffetlich ist das ein weiterführendes entscheidung
        session.addPlayerMessage(playerInputScene1);

        ClassifyActionAgent.Classification classificationActionScene1 = new ClassifyActionAgent.Classification(
                ClassifyActionAgent.ClassificationType.TRIVIAL,
                "Die Aktion hat keinen Einfluss auf den Fortschritt im Abenteuer, da sie nur die allgemeine Stimmung beschreibt ohne spezifische Details oder Interaktionen zu fördern.",
                "Der Herold soll atmosphärisch reagieren und beschreiben, wie sich die Helden in der fröhlichen Taverne bewegen.",
                "KLASSIFIKATION: TRIVIAL");

        System.out.println("Klassifikation Spieler Aktion: "+classificationActionScene1);

        //FIXME letzer output.
//        session.addHeroldMessage("[AMBIENT] "+"Als ihr die Taverne betretet, werdet ihr sofort von einer Welle aus Gelächter und fröhlichem Geplapper umspült. Die Luft ist schwer vom Duft nach gebratenem Fleisch, süßem Met und dem Rauch der zahlreichen Kerzen und Fackeln, die die Taverne in ein warmes, goldenes Licht tauchen. Die Gäste, die sich dicht an dicht drängen, heben ihre Becher und prosten euch zu, als ihr eintretet. Ein paar von ihnen klopfen euch auf die Schultern oder ziehen euch in ihre Gespräche hinein. Die Musik eines fiedelnden Barden erfüllt den Raum, und einige der Gäste tanzen ausgelassen zwischen den Tischen herum. Die fröhliche Stimmung ist ansteckend, und ihr könnt nicht anders, als euch von ihr mitreißen zu lassen.");

        ActionResponseAgent ambienteAgent = new ActionResponseAgent(model);
        String ambienteReplyScene1 = ambienteAgent.generateTrivialResponse(
                scene1.common(),
                playerInputScene1,
                classificationActionScene1.heroldHints());
        session.addHeroldMessage("[AMBIENT] "+ambienteReplyScene1);

    }


    @Test
    void detailTest() throws IOException {
//        String OLLAMA_URL = "http://localhost:11434";
        String OLLAMA_URL = "http://192.168.0.251:11434";
//        String MODEL = "Mistral-Small:latest";
//        String MODEL = "mistral:7b";
//        String MODEL = "qwen3.5:27b";
//        String MODEL = "qwen3:32b";
        String MODEL = "qwen3:30b";
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
        //wird hier vorgefertigt eingefügt
        String introSzene1 = """
                die Helden schlendern durch die belebten Gassen von Gareth, während
                die Sonne langsam untergeht und die Stadt in ein warmes, goldenes
                Licht taucht. Die Luft ist erfüllt vom Lachen und Reden der Passanten,
                und der Duft von gebratenem Fleisch und frischem Brot zieht durch die
                Straßen. Sie nähern sich dem Herzen der Kaiserstadt, wo die Geräusche
                lauter werden und die Menschen dichter zusammenstehen.

                Plötzlich hören sie das laute Gelächter und die fröhliche Musik aus
                einer nahen Taverne. Die Tür steht weit offen, und die Stimmen der
                Gäste dringen nach draußen, ein Zeichen für eine ausgelassene Stimmung
                im Inneren.
                """;
        session.addHeroldMessage("[PREPARE_SCENE] "+introSzene1);
        session.addAdventureMessage(scene1.common());

//        String playerInputScene1 = "wir versuchen uns einen platz zu ergattern und ein paar Getränke zu bestellen";
//        String playerInputScene1 = "wir feiern und geniessen die Stimmung.";
//        String playerInputScene1 = "wir wollen den ganzen Abend in dieser Kneippe verbringen."; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInputScene1 = "wir holen uns Getränke und warten darauf, was passiert"; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInputScene1 = "wir setzen uns an die Theke und bestellen Bier beim Wirt."; //hoffetlich ist das ein weiterführendes entscheidung
        String playerInputScene1 = "Wir setzen uns an die Theke, bestellen Bier und fragen den Wirt nach Gerüchten"; //hoffetlich ist das ein weiterführendes entscheidung
        session.addPlayerMessage(playerInputScene1);

        ClassifyActionAgent.Classification classificationActionScene1 = new ClassifyActionAgent.Classification(
                ClassifyActionAgent.ClassificationType.DETAIL_INFO,
                "Die Aktion zielt gezielt auf die Erschließung von Gerüchten durch den Wirt, die als Meisterinformationen im Szenekontext vorgesehen sind.",
                "Der Spielleiter preisgibt die relevanten Gerüchte aus den Meisterinformationen und verknüpft sie mit der aktuellen Szene, ohne zusätzliche Handlungsanweisungen zu geben.",
                "KLASSIFIKATION: DETAILINFORMATIONEN");

//        ClassifyActionAgent classifyAgent = new ClassifyActionAgent(model);
//        ClassifyActionAgent.Classification classificationActionScene1 =
//                classifyAgent.classifyAction(scene1, playerInputScene1);
//        System.out.println("action type: "+classificationActionScene1.classificationType());

        System.out.println("Klassifikation Spieler Aktion: "+classificationActionScene1);

        //FIXME letzer output.
//        session.addHeroldMessage("[AMBIENT] "+"Als ihr die Taverne betretet, werdet ihr sofort von einer Welle aus Gelächter und fröhlichem Geplapper umspült. Die Luft ist schwer vom Duft nach gebratenem Fleisch, süßem Met und dem Rauch der zahlreichen Kerzen und Fackeln, die die Taverne in ein warmes, goldenes Licht tauchen. Die Gäste, die sich dicht an dicht drängen, heben ihre Becher und prosten euch zu, als ihr eintretet. Ein paar von ihnen klopfen euch auf die Schultern oder ziehen euch in ihre Gespräche hinein. Die Musik eines fiedelnden Barden erfüllt den Raum, und einige der Gäste tanzen ausgelassen zwischen den Tischen herum. Die fröhliche Stimmung ist ansteckend, und ihr könnt nicht anders, als euch von ihr mitreißen zu lassen.");

        ActionResponseAgent ambienteAgent = new ActionResponseAgent(model);
        String ambienteReplyScene1 = ambienteAgent.generateDetailResponse(
                scene1.fullContent(),
                playerInputScene1,
                classificationActionScene1.heroldHints());
        session.addHeroldMessage("[AMBIENT] "+ambienteReplyScene1);

    }

}
