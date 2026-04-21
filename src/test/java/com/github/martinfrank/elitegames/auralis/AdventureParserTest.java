package com.github.martinfrank.elitegames.auralis;

import com.github.martinfrank.elitegames.auralis.adventure.Adventure;
import com.github.martinfrank.elitegames.auralis.adventure.AdventureParser;
import com.github.martinfrank.elitegames.auralis.adventure.Chapter;
import com.github.martinfrank.elitegames.auralis.adventure.Scene;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureParserTest {

    @Test
    void parsesTitleChaptersScenesAndInfoBlocks() {
        String md = """
                # My Adventure
                ## Chapter One
                ### Scene A
                #### Allgemeine Informationen
                common text A
                #### Spezielle Informationen
                details text A
                #### Meisterinformationen
                herald text A
                ### Scene B
                #### Allgemeine Informationen
                common B only
                ## Chapter Two
                ### Scene C
                #### Meisterinformationen
                herald C only
                """;

        Adventure adventure = AdventureParser.parse(md);

        assertEquals("My Adventure", adventure.title());
        assertEquals(2, adventure.chapters().size());

        Chapter one = adventure.chapters().get(0);
        assertEquals("Chapter One", one.title());
        assertEquals(2, one.scenes().size());

        Scene sceneA = one.scenes().get(0);
        assertEquals("Scene A", sceneA.title());
        assertEquals("common text A", sceneA.common());
        assertEquals("details text A", sceneA.details());
        assertEquals("herald text A", sceneA.heraldInfo());

        Scene sceneB = one.scenes().get(1);
        assertEquals("common B only", sceneB.common());
        assertNull(sceneB.details());
        assertNull(sceneB.heraldInfo());

        Chapter two = adventure.chapters().get(1);
        Scene sceneC = two.scenes().get(0);
        assertNull(sceneC.common());
        assertNull(sceneC.details());
        assertEquals("herald C only", sceneC.heraldInfo());
    }

    @Test
    void ignoresUnknownH4TitlesInsideScene() {
        String md = """
                # T
                ## C
                ### S
                #### Allgemeine Informationen
                keep me
                #### Irgendwas anderes
                drop me
                """;

        Scene scene = AdventureParser.parse(md).chapters().get(0).scenes().get(0);
        assertEquals("keep me", scene.common());
        assertNull(scene.details());
        assertNull(scene.heraldInfo());
    }

    @Test
    void ignoresLooseTextBetweenHeadings() {
        String md = """
                # T
                stray title text
                ## C
                stray chapter text
                ### S
                stray scene text
                #### Allgemeine Informationen
                kept body
                """;

        Scene scene = AdventureParser.parse(md).chapters().get(0).scenes().get(0);
        assertEquals("kept body", scene.common());
    }

    @Test
    void fencedCodeBlockContentIsKeptButNewlinesAreRemoved() {
        String md = """
                # T
                ## C
                ### S
                #### Allgemeine Informationen
                before
                ```
                # not a heading
                ```
                after
                """;

        Scene scene = AdventureParser.parse(md).chapters().get(0).scenes().get(0);
        assertEquals("before ``` # not a heading ``` after", scene.common());
    }

    @Test
    void removesNewlinesAndCollapsesWhitespaceInBody() {
        String md = """
                # T
                ## C
                ### S
                #### Allgemeine Informationen
                line one
                line two

                line three
                """;

        Scene scene = AdventureParser.parse(md).chapters().get(0).scenes().get(0);
        assertEquals("line one line two line three", scene.common());
    }

    @Test
    void chaptersWithoutScenesResultInEmptyList() {
        String md = """
                # T
                ## Empty Chapter
                """;

        Chapter chapter = AdventureParser.parse(md).chapters().get(0);
        assertEquals("Empty Chapter", chapter.title());
        assertTrue(chapter.scenes().isEmpty());
    }

    @Test
    void rejectsMarkdownWithoutH1() {
        assertThrows(IllegalArgumentException.class,
                () -> AdventureParser.parse("## no h1\n### scene\n"));
    }

    @Test
    void rejectsMultipleH1() {
        assertThrows(IllegalArgumentException.class,
                () -> AdventureParser.parse("# One\n# Two\n"));
    }

    @Test
    void rejectsH3BeforeChapter() {
        assertThrows(IllegalArgumentException.class,
                () -> AdventureParser.parse("# T\n### stray scene\n"));
    }

    @Test
    void rejectsH4BeforeScene() {
        assertThrows(IllegalArgumentException.class,
                () -> AdventureParser.parse("# T\n## C\n#### Allgemeine Informationen\nx\n"));
    }

    @Test
    void parsesEntfuehrFromClasspath() throws IOException {
        Adventure adventure = AdventureParser.fromClasspath("entfuehr.md");

        assertNotNull(adventure);
        assertTrue(adventure.title().startsWith("Verführung zur Entführung"));
        assertFalse(adventure.chapters().isEmpty());

        Chapter taverne = adventure.chapters().stream()
                .filter(c -> c.title().equals("Kapitel 1: Eine fröhliche Taverne"))
                .findFirst()
                .orElseThrow();
        assertEquals(3, taverne.scenes().size());

        Scene scene1 = taverne.scenes().get(0);
        assertEquals("Szene 1: Betreten der Taverne", scene1.title());
        assertNotNull(scene1.common());
        assertNotNull(scene1.details());
        assertNull(scene1.heraldInfo());

        Scene scene2 = taverne.scenes().get(1);
        assertEquals("Szene 2: Auftritt des Auftraggebers", scene2.title());
        assertNotNull(scene2.common());
        assertNotNull(scene2.details());
        assertNotNull(scene2.heraldInfo());
    }
}
