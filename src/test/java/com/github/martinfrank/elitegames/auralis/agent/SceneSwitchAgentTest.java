package com.github.martinfrank.elitegames.auralis.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class SceneSwitchAgentTest {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
    private static final String MODEL = "qwen3:30b";

    @Test
    void sceneSwitchAgentTest() {
//        ChatLanguageModel model = chatModel();
//        Adventure adventure = SampleAdventure.build();
//        AdventureSession session = new AdventureSession();
//        LocationNode scene1 = location(adventure, SampleAdventure.TAVERN_ID);
//
//        String introSzene1 = """
//                die Helden schlendern durch die belebten Gassen von Gareth, waehrend
//                die Sonne langsam untergeht und die Stadt in ein warmes, goldenes
//                Licht taucht.
//                """;
//        session.addHeroldMessage(scene1.id(), introSzene1);
//        session.addAdventureMessage(scene1.id(), scene1.generalInfo());
//
//        String playerInputScene1 = "wir feiern und geniessen die Stimmung.";
//        session.addPlayerMessage(scene1.id(), playerInputScene1);
//        session.addHeroldMessage(scene1.id(),
//                "Der Geruch von Rauch, Bier und gebratenem Fleisch liegt schwer in der Luft, "
//                        + "waehrend ein Geiger auf dem Tresen eine froehliche Melodie spielt.");
//
//        List<AdventureSession.Turn> conversation = session.getConversation(scene1.id());
//        conversation.forEach(System.out::println);
//
//        SceneSwitchAgent switchAgent = new SceneSwitchAgent(model);
//        SceneSwitchAgent.Verdict verdict = switchAgent.evaluate(session.getConversation(scene1.id()));
//        System.out.println(verdict);
    }

    @Test
    void detailTest() {
//        ChatLanguageModel model = chatModel();
//        Adventure adventure = SampleAdventure.build();
//        AdventureSession session = new AdventureSession();
//        LocationNode scene1 = location(adventure, SampleAdventure.TAVERN_ID);
//
//        String introSzene1 = """
//                die Helden schlendern durch die belebten Gassen von Gareth, waehrend
//                die Sonne langsam untergeht und die Stadt in ein warmes, goldenes
//                Licht taucht.
//                """;
//        session.addHeroldMessage(scene1.id(), introSzene1);
//        session.addAdventureMessage(scene1.id(), scene1.generalInfo());
//
//        String playerInputScene1 = "Wir setzen uns an die Theke, bestellen Bier und fragen den Wirt nach Geruechten";
//        session.addPlayerMessage(scene1.id(), playerInputScene1);
//
//        ClassifyActionAgent.Classification classification = new ClassifyActionAgent.Classification(
//                ClassifyActionAgent.ClassificationType.DETAIL_INFO,
//                "Die Aktion zielt gezielt auf die Erschliessung von Geruechten durch den Wirt.",
//                "Der Spielleiter preisgibt die relevanten Geruechte aus den Meisterinformationen.",
//                "KLASSIFIKATION: DETAILINFORMATIONEN");
//        System.out.println("Klassifikation Spieler Aktion: " + classification);
//
//        ActionResponseAgent ambienteAgent = new ActionResponseAgent(model);
//        String reply = ambienteAgent.generateDetailResponse(
//                SampleAdventure.fullText(scene1),
//                playerInputScene1,
//                classification.heroldHints());
//        session.addHeroldMessage(scene1.id(), "[AMBIENT] " + reply);
    }

    private static ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

//    private static LocationNode location(Adventure adventure, String id) {
//        for (AdventureNode node : adventure.content().nodes()) {
//            if (node instanceof LocationNode loc && loc.id().equals(id)) {
//                return loc;
//            }
//        }
//        throw new IllegalArgumentException("LocationNode not found: " + id);
//    }
}
