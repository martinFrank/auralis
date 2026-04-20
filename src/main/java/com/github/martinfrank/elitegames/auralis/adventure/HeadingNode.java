package com.github.martinfrank.elitegames.auralis.adventure;

import java.util.List;
import java.util.Objects;

public record HeadingNode(Heading heading, String body, List<HeadingNode> children) {

    public HeadingNode {
        Objects.requireNonNull(heading, "heading");
        Objects.requireNonNull(body, "body");
        children = List.copyOf(children);
    }

    public HeadingLevel level() {
        return heading.level();
    }

    public String title() {
        return heading.title();
    }
}
