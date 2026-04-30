package com.github.martinfrank.elitegames.auralis.game;

import org.junit.jupiter.api.Test;

class GameSessionTest {

    @Test
    void sessionTest() {
//        String OLLAMA_URL = "http://192.168.0.251:11434";
//        String MODEL = "qwen3:32b";
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
//        LocationNode gareth = locationByIndex(adventure, 0);
//        LocationNode scene1 = locationByIndex(adventure, 1);
//        LocationNode scene2 = locationByIndex(adventure, 2);
//
//        // erste AI interaktion: szene aufsetzen
//        ScenePrepareAgent prepareAgent = new ScenePrepareAgent(model);
//        String introSzene1 = prepareAgent.prepareScene(gareth.generalInfo(), scene1.generalInfo());
//        session.addHeroldMessage(scene1.id(), "[PREPARE_SCENE] " + introSzene1);
//        session.addAdventureMessage(scene1.id(), scene1.generalInfo());
//
//        String playerInputScene1 = "Wir setzen uns an die Theke, bestellen Bier und fragen den Wirt nach Geruechten";
//        session.addPlayerMessage(scene1.id(), playerInputScene1);
//
//        ClassifyActionAgent classifyAgent = new ClassifyActionAgent(model);
//        ClassifyActionAgent.Classification classificationActionScene1 =
//                classifyAgent.classifyAction(scene1.generalInfo(), playerInputScene1);
//        System.out.println("action type: " + classificationActionScene1.classificationType());
//
//        ActionResponseAgent ambienteAgent = new ActionResponseAgent(model);
//        String ambienteReplyScene1 = ambienteAgent.generateTrivialResponse(
//                scene1.generalInfo(),
//                playerInputScene1,
//                classificationActionScene1.heroldHints());
//        session.addHeroldMessage(scene1.id(), "[AMBIENT] " + ambienteReplyScene1);
//
//        // jetzt kommt die ueberleitung zur Szene 2
//        session.addAdventureMessage(scene2.id(), scene2.generalInfo());
//
//        String playerInputScene2 = "Wir versuchen dem Mann hinterher zu laufen!";
//        session.addPlayerMessage(scene2.id(), playerInputScene2);
//
//        ClassifyActionAgent.Classification classificationActionScene2 =
//                classifyAgent.classifyAction(scene2.generalInfo(), playerInputScene2);
//        System.out.println("action type: " + classificationActionScene2.classificationType());
//
//        String ambienteReplyScene2 = ambienteAgent.generateTrivialResponse(
//                scene2.generalInfo(),
//                playerInputScene2,
//                classificationActionScene2.heroldHints());
//        session.addHeroldMessage(scene2.id(), "[AMBIENT] " + ambienteReplyScene2);
    }

//    private static LocationNode locationByIndex(Adventure adventure, int index) {
//        int seen = 0;
//        for (AdventureNode node : adventure.content().nodes()) {
//            if (node instanceof LocationNode loc) {
//                if (seen == index) {
//                    return loc;
//                }
//                seen++;
//            }
//        }
//        throw new IllegalArgumentException("No location at index " + index);
//    }
}
