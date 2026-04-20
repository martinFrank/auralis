package com.github.martinfrank.elitegames.auralis.adventure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AdventureProgress {

    public enum Status { OPEN, EXPERIENCED, SKIPPED }

    public static final class Chapter {
        private final String title;
        private Status status;

        private Chapter(String title, Status status) {
            this.title = Objects.requireNonNull(title, "title");
            this.status = Objects.requireNonNull(status, "status");
        }

        public String title() {
            return title;
        }

        public Status status() {
            return status;
        }

        public boolean isOpen() {
            return status == Status.OPEN;
        }

        public boolean isExperienced() {
            return status == Status.EXPERIENCED;
        }
    }

    private final List<Chapter> chapters;

    public AdventureProgress(List<String> chapterTitles) {
        Objects.requireNonNull(chapterTitles, "chapterTitles");
        List<Chapter> list = new ArrayList<>(chapterTitles.size());
        for (String title : chapterTitles) {
            list.add(new Chapter(title, Status.OPEN));
        }
        this.chapters = list;
    }

    public static AdventureProgress fromHeadingTree(List<HeadingNode> roots) {
        return fromHeadingTree(roots, HeadingLevel.H2);
    }

    public static AdventureProgress fromHeadingTree(List<HeadingNode> roots, HeadingLevel chapterLevel) {
        Objects.requireNonNull(roots, "roots");
        Objects.requireNonNull(chapterLevel, "chapterLevel");
        List<String> titles = new ArrayList<>();
        collectTitles(roots, chapterLevel, titles);
        return new AdventureProgress(titles);
    }

    private static void collectTitles(List<HeadingNode> nodes, HeadingLevel target, List<String> out) {
        for (HeadingNode node : nodes) {
            if (node.level() == target) {
                out.add(node.title());
            }
            collectTitles(node.children(), target, out);
        }
    }

    public List<Chapter> chapters() {
        return Collections.unmodifiableList(chapters);
    }

    public Optional<Chapter> nextOpen() {
        return chapters.stream().filter(Chapter::isOpen).findFirst();
    }

    public List<Chapter> openChapters() {
        return chapters.stream().filter(Chapter::isOpen).toList();
    }

    public List<Chapter> experiencedChapters() {
        return chapters.stream().filter(Chapter::isExperienced).toList();
    }

    public boolean markExperienced(String title) {
        Objects.requireNonNull(title, "title");
        for (Chapter chapter : chapters) {
            if (chapter.title.equals(title)) {
                chapter.status = Status.EXPERIENCED;
                return true;
            }
        }
        return false;
    }

    public boolean isFinished() {
        return !chapters.isEmpty() && chapters.stream().allMatch(Chapter::isExperienced);
    }

    public int size() {
        return chapters.size();
    }
}
