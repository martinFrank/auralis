package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.adventure.AdventureProgress;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureProvider;
import com.github.martinfrank.elitegames.auralis.adventure.HeadingNode;
import com.github.martinfrank.elitegames.auralis.agent.HeroldAgent;
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
    void recordsPlayerAndHeroldTurnsAndTracksChapterProgress() throws IOException {
        AdventureProvider adventure = AdventureProvider.fromClasspath("entfuehr.md");
        AdventureProgress progress = AdventureProgress.fromHeadingTree(adventure.getHeadingTree());
        assertFalse(progress.chapters().isEmpty(), "expected H2 chapters in entfuehr.md");
        assertEquals(progress.size(), progress.openChapters().size(), "all chapters start OPEN");

        RecordingHeroldStub herold = new RecordingHeroldStub(adventure.getContent());
        AdventureSession session = new AdventureSession(herold, adventure, progress);

        String reply1 = session.handlePlayerInput("Ich betrete die Taverne.");
        String reply2 = session.handlePlayerInput("Ich spreche den Wirt an.");

        assertEquals("Stub-Antwort #1 auf: Ich betrete die Taverne.", reply1);
        assertEquals("Stub-Antwort #2 auf: Ich spreche den Wirt an.", reply2);
        assertEquals(List.of("Ich betrete die Taverne.", "Ich spreche den Wirt an."), herold.seenInputs);

        List<AdventureSession.Turn> transcript = session.transcript();
        assertEquals(4, transcript.size());
        assertEquals(AdventureSession.Source.PLAYER, transcript.get(0).source());
        assertEquals(AdventureSession.Source.HEROLD, transcript.get(1).source());
        assertEquals(AdventureSession.Source.PLAYER, transcript.get(2).source());
        assertEquals(AdventureSession.Source.HEROLD, transcript.get(3).source());

        session.recordAdventureFragment("=== Kapitel-Text aus dem Abenteuerbuch ===");
        assertEquals(AdventureSession.Source.ADVENTURE, session.transcript().get(4).source());
    }

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
        System.out.println(transcript);

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
