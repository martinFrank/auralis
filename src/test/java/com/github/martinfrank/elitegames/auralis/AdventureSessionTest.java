package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.adventure.AdventureProgress;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureProvider;
import com.github.martinfrank.elitegames.auralis.adventure.HeadingNode;
import com.github.martinfrank.elitegames.auralis.agent.AmbienteAgent;
import com.github.martinfrank.elitegames.auralis.agent.HeroldAgent;
import com.github.martinfrank.elitegames.auralis.agent.PlayerActionJudgeAgent;
import com.github.martinfrank.elitegames.auralis.agent.ScenePrepareAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureSessionTest {

    @Test
    void markChapterExperiencedAdvancesNextOpenAndEventuallyFinishes() throws IOException {
        AdventureProvider adventure = AdventureProvider.fromClasspath("entfuehr.md");
        AdventureProgress progress = AdventureProgress.fromHeadingTree(adventure.getHeadingTree());
        AdventureSession session = new AdventureSession(new RecordingHeroldStub(null), adventure, progress);

        String first = progress.nextOpen().orElseThrow().title();
        assertTrue(session.markChapterExperienced(first));
        assertNotEquals(first, progress.nextOpen().map(AdventureProgress.Chapter::title).orElse(null));
        assertFalse(session.isFinished());

        for (AdventureProgress.Chapter chapter : progress.chapters()) {
            session.markChapterExperienced(chapter.title());
        }
        assertTrue(session.isFinished());
        assertTrue(progress.openChapters().isEmpty());
    }

    @Test
    void unknownChapterTitleIsNotMarked() throws IOException {
        AdventureProvider adventure = AdventureProvider.fromClasspath("entfuehr.md");
        AdventureProgress progress = AdventureProgress.fromHeadingTree(adventure.getHeadingTree());
        AdventureSession session = new AdventureSession(new RecordingHeroldStub(null), adventure, progress);

        assertFalse(session.markChapterExperienced("Kapitel das es nicht gibt"));
    }


    //my session test
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


        AdventureProvider adventure = AdventureProvider.fromClasspath("entfuehr.md");
        AdventureProgress progress = AdventureProgress.fromHeadingTree(adventure.getHeadingTree());

        RecordingHeroldStub recordingHerold = new RecordingHeroldStub(adventure.getContent());
        AdventureSession session = new AdventureSession(recordingHerold, adventure, progress);

        HeadingNode tavern = adventure.getHeadingTree().getFirst().children().get(1); //eine fröhliche taverne

        ScenePrepareAgent prep = new ScenePrepareAgent(model);
        HeadingNode root = adventure.getHeadingTree().getFirst();
        String backgroundInfo = root.children().getFirst().body();          // "Einleitung (Meisterinformationen)"
        String tavernTitle = tavern.title();          // "Eine fröhliche Taverne"
        String upcomingText = tavern.children().getFirst().body();

        String scene = prep.prepareScene(tavernTitle, backgroundInfo, upcomingText);
        session.recordHeroldFragment(scene);

        session.recordHeroldFragment(upcomingText.replace("\n", " "));

        List<AdventureSession.Turn> transcript = session.transcript();
        transcript.forEach(System.out::println);
        System.out.println("----------------");

        //----------------------------------

        String playerInput = "wir versuchen uns einen platz zu suchen und ein paar getränke zu bestellen";
//        String playerInput = "wir trinken unsere Getränke und geniessen die Stimmung.";
//        String playerInput = "wir wollen den ganzen Abend in dieser Kneippe verbringen."; //hoffetlich ist das ein weiterführendes entscheidung
//        String playerInput = "wir holen uns Getränke und warten darauf, was passiert"; //hoffetlich ist das ein weiterführendes entscheidung
//        session.recordPlayerInput(playerInput);


        String background = tavern.body();
        String nextScene = tavern.children().get(2).body();

        String judgeBackground = """
                Aktuelles Kapitel: %s

                === MEISTERINFORMATIONEN / HINTERGRUND ===
                %s

                === AKTUELLE SZENE / FOLGETEXT ===
                %s
                """.formatted(tavernTitle, background, nextScene);

        PlayerActionJudgeAgent judge = new PlayerActionJudgeAgent(model);
        PlayerActionJudgeAgent.Judgement judgement =
                judge.judgeAction(judgeBackground, session.transcript(), playerInput);
        System.out.println("=== JUDGEMENT ===");
        System.out.println("Klassifikation: " + judgement.classification());
        System.out.println("Begruendung:    " + judgement.begruendung());
        System.out.println("Hinweis:        " + judgement.hinweisFuerHerold());
        System.out.println("--- raw ---");
        System.out.println(judgement.raw());
        System.out.println("=== ENDE JUDGEMENT ===");

        session.recordPlayerInput(playerInput);

        int additionalCount = 2;

        if (additionalCount < 2 &&
                judgement.classification() == PlayerActionJudgeAgent.Classification.ERGAENZEND) {
            AmbienteAgent ambiente = new AmbienteAgent(model);
            String ambienteReply = ambiente.generateAmbiente(
                    judgeBackground,
                    session.transcript(),
                    playerInput,
                    judgement.hinweisFuerHerold());
            System.out.println("=== AMBIENTE ===");
            System.out.println(ambienteReply);
            System.out.println("=== ENDE AMBIENTE ===");
            session.recordHeroldFragment(ambienteReply);
        }

        if (additionalCount >= 2 || judgement.classification() == PlayerActionJudgeAgent.Classification.WEITERFUEHREND) {

            String bgInfo = session.transcript().get(session.transcript().size()-2).content() + "\n"+
                    session.transcript().getLast().content();

            String nnexxtt = prep.prepareScene(tavernTitle, bgInfo, nextScene);
            session.recordHeroldFragment(nnexxtt);
            session.recordAdventureFragment(nextScene);
        }

//        List<AdventureSession.Turn> transcript = session.transcript();
        transcript.forEach(System.out::println);
    }


    private static final class RecordingHeroldStub extends HeroldAgent {
        private final List<String> seenInputs = new ArrayList<>();
        private int counter = 0;

        private RecordingHeroldStub(String adventureText) {
            super(null, adventureText);
        }

        @Override
        public String chat(String playerAction) {
            seenInputs.add(playerAction);
            counter++;
            return "Stub-Antwort #" + counter + " auf: " + playerAction;
        }
    }
}
