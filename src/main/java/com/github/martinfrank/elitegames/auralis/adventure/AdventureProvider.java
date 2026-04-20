package com.github.martinfrank.elitegames.auralis.adventure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdventureProvider {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*?)\\s*#*\\s*$");
    private static final String CODE_FENCE = "```";

    private final String content;
    private final List<HeadingNode> headingTree;

    public AdventureProvider(String content) {
        this.content = Objects.requireNonNull(content, "content");
        this.headingTree = buildTree(parseSections(content));
    }

    public static AdventureProvider fromClasspath(String resourcePath) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = AdventureProvider.class.getClassLoader();
        }
        InputStream stream = loader.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IOException("Adventure resource not found on classpath: " + resourcePath);
        }
        try (stream) {
            return new AdventureProvider(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public static AdventureProvider fromPath(Path path) throws IOException {
        return new AdventureProvider(Files.readString(path, StandardCharsets.UTF_8));
    }

    public String getContent() {
        return content;
    }

    public List<HeadingNode> getHeadingTree() {
        return headingTree;
    }

    private static List<Section> parseSections(String content) {
        List<Section> sections = new ArrayList<>();
        Heading currentHeading = null;
        StringBuilder bodyBuffer = new StringBuilder();
        boolean insideFencedCode = false;

        for (String line : content.split("\\R", -1)) {
            String trimmed = line.stripLeading();

            if (trimmed.startsWith(CODE_FENCE)) {
                insideFencedCode = !insideFencedCode;
                if (currentHeading != null) {
                    bodyBuffer.append(line).append('\n');
                }
                continue;
            }

            if (!insideFencedCode) {
                Matcher matcher = HEADING_PATTERN.matcher(trimmed);
                if (matcher.matches()) {
                    String title = matcher.group(2).trim();
                    if (!title.isEmpty()) {
                        if (currentHeading != null) {
                            sections.add(new Section(currentHeading, bodyBuffer.toString().strip()));
                            bodyBuffer.setLength(0);
                        }
                        HeadingLevel level = HeadingLevel.ofHashCount(matcher.group(1).length());
                        currentHeading = new Heading(level, title);
                        continue;
                    }
                }
            }

            if (currentHeading != null) {
                bodyBuffer.append(line).append('\n');
            }
        }

        if (currentHeading != null) {
            sections.add(new Section(currentHeading, bodyBuffer.toString().strip()));
        }
        return sections;
    }

    private static List<HeadingNode> buildTree(List<Section> sections) {
        List<TreeBuilder> roots = new ArrayList<>();
        Deque<TreeBuilder> stack = new ArrayDeque<>();
        for (Section section : sections) {
            int depth = section.heading().level().getHashCount();
            while (!stack.isEmpty() && stack.peek().heading.level().getHashCount() >= depth) {
                stack.pop();
            }
            TreeBuilder node = new TreeBuilder(section.heading(), section.body());
            if (stack.isEmpty()) {
                roots.add(node);
            } else {
                stack.peek().children.add(node);
            }
            stack.push(node);
        }
        List<HeadingNode> built = new ArrayList<>(roots.size());
        for (TreeBuilder root : roots) {
            built.add(root.build());
        }
        return Collections.unmodifiableList(built);
    }

    private record Section(Heading heading, String body) {
    }

    private static final class TreeBuilder {
        private final Heading heading;
        private final String body;
        private final List<TreeBuilder> children = new ArrayList<>();

        private TreeBuilder(Heading heading, String body) {
            this.heading = heading;
            this.body = body;
        }

        private HeadingNode build() {
            List<HeadingNode> kids = new ArrayList<>(children.size());
            for (TreeBuilder child : children) {
                kids.add(child.build());
            }
            return new HeadingNode(heading, body, kids);
        }
    }
}
