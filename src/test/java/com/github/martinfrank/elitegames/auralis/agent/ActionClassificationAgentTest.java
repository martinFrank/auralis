package com.github.martinfrank.elitegames.auralis.agent;

import org.junit.jupiter.api.Test;

public class ActionClassificationAgentTest {

    @Test
    void classificationTest() {
//        String OLLAMA_URL = "http://192.168.0.251:11434";
//        String MODEL = "qwen3:30b";
//
//        ChatLanguageModel model = OllamaChatModel.builder()
//                .baseUrl(OLLAMA_URL)
//                .modelName(MODEL)
//                .timeout(Duration.ofMinutes(5))
//                .build();
//
//        Adventure adventure = SampleAdventure.build();
//        AdventureSession session = new AdventureSession();
//
//        LocationNode scene1 = firstLocationAfter(adventure, SampleAdventure.TAVERN_ID);
//
//        String introSzene1 = """
//                die Helden schlendern durch die belebten Gassen von Gareth, waehrend
//                die Sonne langsam untergeht und die Stadt in ein warmes, goldenes
//                Licht taucht.
//                """;
//        session.addHeroldMessage(scene1.id(), "[PREPARE_SCENE] " + introSzene1);
//        session.addAdventureMessage(scene1.id(), scene1.generalInfo());
//
//        String playerInputScene1 = "Wir setzen uns an die Theke, bestellen Bier und fragen den Wirt nach seinen Namen";
//        session.addPlayerMessage(scene1.id(), playerInputScene1);
//
//        ClassifyActionAgent classifyAgent = new ClassifyActionAgent(model);
//        ClassifyActionAgent.Classification classificationActionScene1 =
//                classifyAgent.classifyAction(scene1.generalInfo(), playerInputScene1);
//        System.out.println(classificationActionScene1);
//
//        ActionResponseAgent ambienteAgent = new ActionResponseAgent(model);
//        if (classificationActionScene1.classificationType() == ClassifyActionAgent.ClassificationType.DETAIL_INFO) {
//            String reply = ambienteAgent.generateDetailResponse(
//                    SampleAdventure.fullText(scene1),
//                    playerInputScene1,
//                    classificationActionScene1.heroldHints());
//            session.addHeroldMessage(scene1.id(), "[AMBIENT] " + reply);
//        }
//        if (classificationActionScene1.classificationType() == ClassifyActionAgent.ClassificationType.TRIVIAL) {
//            String reply = ambienteAgent.generateTrivialResponse(
//                    SampleAdventure.fullText(scene1),
//                    playerInputScene1,
//                    classificationActionScene1.heroldHints());
//            session.addHeroldMessage(scene1.id(), "[AMBIENT] " + reply);
//        }
    }

//    private static LocationNode firstLocationAfter(Adventure adventure, String id) {
//        for (AdventureNode node : adventure.content().nodes()) {
//            if (node instanceof LocationNode loc && loc.id().equals(id)) {
//                return loc;
//            }
//        }
//        throw new IllegalArgumentException("LocationNode not found: " + id);
//    }
}
