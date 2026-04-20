package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.adventure.AdventureProvider;
import com.github.martinfrank.elitegames.auralis.adventure.HeadingLevel;
import com.github.martinfrank.elitegames.auralis.adventure.HeadingNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureProviderTest {

    @Test
    void parsesAllHeadingLevelsAsNestedChain() {
        String md = """
                # Title
                ## Chapter One
                ### Subsection
                #### Deep
                ##### Deeper
                ###### Deepest
                """;

        List<HeadingNode> tree = new AdventureProvider(md).getHeadingTree();

        HeadingNode node = single(tree);
        assertEquals(HeadingLevel.H1, node.level());
        assertEquals("Title", node.title());
        for (HeadingLevel expected : new HeadingLevel[]{
                HeadingLevel.H2, HeadingLevel.H3, HeadingLevel.H4, HeadingLevel.H5, HeadingLevel.H6}) {
            node = single(node.children());
            assertEquals(expected, node.level());
        }
    }

    @Test
    void ignoresHeadingsInsideFencedCodeBlocks() {
        String md = """
                # Real
                ```
                # Not a heading
                ## Also not
                ```
                ## Also Real
                """;

        List<HeadingNode> tree = new AdventureProvider(md).getHeadingTree();

        HeadingNode root = single(tree);
        assertEquals("Real", root.title());
        HeadingNode child = single(root.children());
        assertEquals(HeadingLevel.H2, child.level());
        assertEquals("Also Real", child.title());
    }

    @Test
    void stripsTrailingClosingHashes() {
        HeadingNode node = single(new AdventureProvider("## Closed ##\n").getHeadingTree());
        assertEquals("Closed", node.title());
    }

    @Test
    void buildsHierarchicalHeadingTree() {
        String md = """
                # Book
                ## Chapter A
                ### Section A1
                ### Section A2
                #### Deep A2a
                ## Chapter B
                ### Section B1
                # Epilogue
                """;

        List<HeadingNode> tree = new AdventureProvider(md).getHeadingTree();

        assertEquals(2, tree.size());

        HeadingNode book = tree.get(0);
        assertEquals(HeadingLevel.H1, book.level());
        assertEquals("Book", book.title());
        assertEquals(2, book.children().size());

        HeadingNode chapterA = book.children().get(0);
        assertEquals("Chapter A", chapterA.title());
        assertEquals(HeadingLevel.H2, chapterA.level());
        assertEquals(2, chapterA.children().size());
        assertEquals("Section A1", chapterA.children().get(0).title());
        assertTrue(chapterA.children().get(0).children().isEmpty());
        HeadingNode sectionA2 = chapterA.children().get(1);
        assertEquals("Section A2", sectionA2.title());
        assertEquals(1, sectionA2.children().size());
        assertEquals("Deep A2a", sectionA2.children().get(0).title());
        assertEquals(HeadingLevel.H4, sectionA2.children().get(0).level());

        HeadingNode chapterB = book.children().get(1);
        assertEquals("Chapter B", chapterB.title());
        assertEquals(1, chapterB.children().size());
        assertEquals("Section B1", chapterB.children().get(0).title());

        HeadingNode epilogue = tree.get(1);
        assertEquals("Epilogue", epilogue.title());
        assertTrue(epilogue.children().isEmpty());
    }

    @Test
    void treeHandlesLevelJumps() {
        String md = """
                # Root
                ### SkippedToThree
                ## Back
                """;

        List<HeadingNode> tree = new AdventureProvider(md).getHeadingTree();

        HeadingNode root = single(tree);
        assertEquals(2, root.children().size());
        assertEquals("SkippedToThree", root.children().get(0).title());
        assertEquals(HeadingLevel.H3, root.children().get(0).level());
        assertEquals("Back", root.children().get(1).title());
        assertEquals(HeadingLevel.H2, root.children().get(1).level());
    }

    @Test
    void exposesBodyContentDirectlyUnderHeading() {
        String md = """
                # Title
                intro line one
                intro line two

                ## Chapter
                chapter body
                """;

        List<HeadingNode> tree = new AdventureProvider(md).getHeadingTree();
        HeadingNode title = single(tree);
        HeadingNode chapter = single(title.children());

        assertEquals("intro line one\nintro line two", title.body());
        assertEquals("chapter body", chapter.body());
    }

    @Test
    void bodyStopsAtNextHeadingOfAnyLevel() {
        String md = """
                # One
                belongs to One
                ### Deep
                belongs to Deep
                # Two
                belongs to Two
                """;

        List<HeadingNode> tree = new AdventureProvider(md).getHeadingTree();

        HeadingNode one = tree.get(0);
        assertEquals("belongs to One", one.body());
        HeadingNode deep = single(one.children());
        assertEquals("belongs to Deep", deep.body());

        HeadingNode two = tree.get(1);
        assertEquals("belongs to Two", two.body());
    }

    @Test
    void bodyIsEmptyWhenNothingFollowsHeading() {
        String md = """
                # Alone
                ## Next
                """;

        HeadingNode alone = single(new AdventureProvider(md).getHeadingTree());
        assertEquals("", alone.body());
        HeadingNode next = single(alone.children());
        assertEquals("", next.body());
    }

    @Test
    void bodyKeepsFencedCodeBlocksVerbatim() {
        String md = """
                # Demo
                prefix
                ```
                # not a heading
                ```
                suffix
                """;

        HeadingNode demo = single(new AdventureProvider(md).getHeadingTree());
        assertEquals("""
                prefix
                ```
                # not a heading
                ```
                suffix""", demo.body());
    }

    @Test
    void loadsEntfuehrFromClasspath() throws IOException {
        AdventureProvider provider = AdventureProvider.fromClasspath("entfuehr.md");

        assertNotNull(provider.getContent());
        assertFalse(provider.getContent().isBlank());

        List<HeadingNode> tree = provider.getHeadingTree();
        HeadingNode root = single(tree);
        assertEquals(HeadingLevel.H1, root.level());
        assertEquals("Verführung zur Entführung", root.title());
        assertFalse(root.children().isEmpty(), "Expected H2 chapters under the H1 root");
        assertTrue(
                root.children().stream().allMatch(c -> c.level() == HeadingLevel.H2),
                "All direct children of the H1 root should be H2");

        Optional<HeadingNode> taverne = root.children().stream()
                .filter(c -> c.title().equals("Eine fröhliche Taverne"))
                .findFirst();
        assertTrue(taverne.isPresent(), "Expected H2 'Eine fröhliche Taverne'");
        assertFalse(taverne.get().children().isEmpty(),
                "Expected nested H3 sections under 'Eine fröhliche Taverne'");
        HeadingNode firstSection = taverne.get().children().get(0);
        assertEquals(HeadingLevel.H3, firstSection.level());
        assertFalse(firstSection.body().isBlank(),
                "Expected body text under the first H3 section of 'Eine fröhliche Taverne'");
    }

    @Test
    void entfuehrTest() throws IOException {
        AdventureProvider provider = AdventureProvider.fromClasspath("entfuehr.md");
        List<HeadingNode> tree = provider.getHeadingTree();

        HeadingNode root = tree.getFirst();
        HeadingNode tavern = root.children().stream().filter(n -> n.title().equals("Eine fröhliche Taverne")).findAny().orElseThrow();
        tavern.children().stream().map(HeadingNode::title).toList().forEach(System.out::println);

    }

    private static HeadingNode single(List<HeadingNode> nodes) {
        assertEquals(1, nodes.size(), () -> "Expected exactly one node, got " + nodes.size());
        return nodes.get(0);
    }
}
