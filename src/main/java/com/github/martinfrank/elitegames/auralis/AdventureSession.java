package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.adventure.AdventureProgress;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureProvider;
import com.github.martinfrank.elitegames.auralis.agent.HeroldAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AdventureSession {

    public enum Source { PLAYER, HEROLD, ADVENTURE }

    public record Turn(Source source, String content) {
        public Turn {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(content, "content");
        }
    }

    private final HeroldAgent herold;
    private final AdventureProvider adventure;
    private final AdventureProgress progress;
    private final List<Turn> transcript = new ArrayList<>();

    public AdventureSession(HeroldAgent herold, AdventureProvider adventure, AdventureProgress progress) {
        this.herold = Objects.requireNonNull(herold, "herold");
        this.adventure = Objects.requireNonNull(adventure, "adventure");
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    public HeroldAgent herold() {
        return herold;
    }

    public AdventureProvider adventure() {
        return adventure;
    }

    public AdventureProgress progress() {
        return progress;
    }

    public List<Turn> transcript() {
        return Collections.unmodifiableList(transcript);
    }

    public void recordPlayerInput(String playerInput) {
        Objects.requireNonNull(playerInput, "playerInput");
        transcript.add(new Turn(Source.PLAYER, playerInput));
    }

    public void recordAdventureFragment(String fragment) {
        Objects.requireNonNull(fragment, "fragment");
        transcript.add(new Turn(Source.ADVENTURE, fragment));
    }

    public void recordHeroldFragment(String fragment) {
        Objects.requireNonNull(fragment, "fragment");
        transcript.add(new Turn(Source.HEROLD, fragment));
    }

    public boolean markChapterExperienced(String chapterTitle) {
        return progress.markExperienced(chapterTitle);
    }

    public boolean isFinished() {
        return progress.isFinished();
    }
}
