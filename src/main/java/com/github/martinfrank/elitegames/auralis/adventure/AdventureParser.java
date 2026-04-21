package com.github.martinfrank.elitegames.auralis.adventure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdventureParser {

    public static final String COMMON_INFO_TITLE = "Allgemeine Informationen";
    public static final String DETAILS_INFO_TITLE = "Spezielle Informationen";
    public static final String HERALD_INFO_TITLE = "Meisterinformationen";

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*?)\\s*#*\\s*$");
    private static final String CODE_FENCE = "```";

    private AdventureParser() {
    }

    public static Adventure parse(String markdown) {
        Objects.requireNonNull(markdown, "markdown");
        return new Parse().run(markdown);
    }

    public static Adventure fromClasspath(String resourcePath) throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = AdventureParser.class.getClassLoader();
        }
        try (InputStream stream = loader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Adventure resource not found on classpath: " + resourcePath);
            }
            return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public static Adventure fromPath(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    private enum Slot { COMMON, DETAILS, HERALD, IGNORE }

    private static final class SceneDraft {
        final String title;
        String common;
        String details;
        String herald;

        SceneDraft(String title) {
            this.title = title;
        }

        Scene toScene() {
            return new Scene(title, common, details, herald);
        }
    }

    private static final class ChapterDraft {
        final String title;
        final List<SceneDraft> scenes = new ArrayList<>();

        ChapterDraft(String title) {
            this.title = title;
        }

        Chapter toChapter() {
            List<Scene> out = new ArrayList<>(scenes.size());
            for (SceneDraft s : scenes) {
                out.add(s.toScene());
            }
            return new Chapter(title, List.copyOf(out));
        }
    }

    private static final class Parse {
        private String adventureTitle;
        private final List<ChapterDraft> chapters = new ArrayList<>();
        private ChapterDraft currentChapter;
        private SceneDraft currentScene;
        private Slot currentSlot;
        private final StringBuilder buffer = new StringBuilder();
        private boolean insideFence;

        Adventure run(String markdown) {
            for (String line : markdown.split("\\R", -1)) {
                String trimmed = line.stripLeading();
                if (trimmed.startsWith(CODE_FENCE)) {
                    insideFence = !insideFence;
                    appendLine(line);
                    continue;
                }
                if (!insideFence) {
                    Matcher m = HEADING_PATTERN.matcher(trimmed);
                    if (m.matches()) {
                        String title = m.group(2).trim();
                        if (!title.isEmpty()) {
                            flushSlot();
                            handleHeading(HeadingLevel.ofHashCount(m.group(1).length()), title);
                            continue;
                        }
                    }
                }
                appendLine(line);
            }
            flushSlot();

            if (adventureTitle == null) {
                throw new IllegalArgumentException("Markdown contains no H1 adventure title");
            }
            List<Chapter> built = new ArrayList<>(chapters.size());
            for (ChapterDraft c : chapters) {
                built.add(c.toChapter());
            }
            return new Adventure(adventureTitle, List.copyOf(built));
        }

        private void handleHeading(HeadingLevel level, String title) {
            switch (level) {
                case H1 -> {
                    if (adventureTitle != null) {
                        throw new IllegalArgumentException("Multiple H1 titles found; expected exactly one");
                    }
                    adventureTitle = title;
                    currentChapter = null;
                    currentScene = null;
                }
                case H2 -> {
                    if (adventureTitle == null) {
                        throw new IllegalArgumentException("H2 chapter '" + title + "' appears before H1 title");
                    }
                    currentChapter = new ChapterDraft(title);
                    chapters.add(currentChapter);
                    currentScene = null;
                }
                case H3 -> {
                    if (currentChapter == null) {
                        throw new IllegalArgumentException("H3 scene '" + title + "' appears outside a chapter");
                    }
                    currentScene = new SceneDraft(title);
                    currentChapter.scenes.add(currentScene);
                }
                case H4 -> {
                    if (currentScene == null) {
                        throw new IllegalArgumentException("H4 '" + title + "' appears outside a scene");
                    }
                    currentSlot = classify(title);
                }
                default -> currentSlot = Slot.IGNORE;
            }
        }

        private static Slot classify(String title) {
            if (title.equalsIgnoreCase(COMMON_INFO_TITLE)) return Slot.COMMON;
            if (title.equalsIgnoreCase(DETAILS_INFO_TITLE)) return Slot.DETAILS;
            if (title.equalsIgnoreCase(HERALD_INFO_TITLE)) return Slot.HERALD;
            return Slot.IGNORE;
        }

        private void appendLine(String line) {
            if (currentSlot == null || currentSlot == Slot.IGNORE) {
                return;
            }
            buffer.append(line).append('\n');
        }

        private void flushSlot() {
            if (currentSlot == null || currentSlot == Slot.IGNORE) {
                buffer.setLength(0);
                currentSlot = null;
                return;
            }
            String body = buffer.toString().replaceAll("\\s+", " ").strip();
            buffer.setLength(0);
            switch (currentSlot) {
                case COMMON -> currentScene.common = body;
                case DETAILS -> currentScene.details = body;
                case HERALD -> currentScene.herald = body;
                default -> { }
            }
            currentSlot = null;
        }
    }

    public enum HeadingLevel {
        H1(1),
        H2(2),
        H3(3),
        H4(4),
        H5(5),
        H6(6);

        private final int hashCount;

        HeadingLevel(int hashCount) {
            this.hashCount = hashCount;
        }

        public int getHashCount() {
            return hashCount;
        }

        public static HeadingLevel ofHashCount(int hashCount) {
            for (HeadingLevel level : values()) {
                if (level.hashCount == hashCount) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Unsupported markdown heading hash count: " + hashCount);
        }
    }
}
