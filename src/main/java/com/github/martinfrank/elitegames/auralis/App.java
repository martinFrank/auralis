package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureReader;
import com.github.martinfrank.elitegames.auralis.agent.Agents;
import com.github.martinfrank.elitegames.auralis.agent.SetupQuestAgent;
import com.github.martinfrank.elitegames.auralis.agent.chat.*;
import com.github.martinfrank.elitegames.auralis.character.Adventurer;
import com.github.martinfrank.elitegames.auralis.character.Party;
import com.github.martinfrank.elitegames.auralis.game.GameEngine;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

public class App {

    private static final String OLLAMA_URL = "http://192.168.0.251:11434";
//    private static final String OLLAMA_URL = "http://localhost:11434";
    private static final String MODEL = "qwen2.5:7b"; //gut
    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    public static void main(String[] args) throws IOException {

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL)
                .timeout(Duration.ofMinutes(5))
                .build();
        Agents agents = new Agents();
        agents.setSetupQuestAgent(new SetupQuestAgent(model));
        agents.setActionJudgeAgent(new ActionJudgeAgent(model));
        agents.setAmbientResponseAgent(new AmbientResponseAgent(model));
        agents.setClassifyInputAgent(new ClassifyInputAgent(model));
        agents.setDialogResponseAgent(new DialogResponseAgent(model));
        agents.setQuestionResponseAgent(new QuestionResponseAgent(model));
        agents.setActionResponseAgent(new ActionResponseAgent(model));
        agents.setOocResponseAgent(new OocResponseAgent(model));
        agents.setUnclearResponseAgent(new UnclearResponseAgent(model));


        Adventure adventure = new AdventureReader().read(App.class.getResourceAsStream(RESOURCE));
        Party party = new Party(List.of(new Adventurer("Thorsten Grambush")));
        GameEngine engine = new GameEngine(adventure, party, agents);
        engine.start();
        Scanner scanner = new Scanner(System.in);
        String playerMessage = "";
        while (!playerMessage.equalsIgnoreCase("exit")) {
            playerMessage = scanner.nextLine();
            engine.handleTurn(playerMessage);
        }

    }

}
