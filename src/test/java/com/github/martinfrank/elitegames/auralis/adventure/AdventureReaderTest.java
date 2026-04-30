package com.github.martinfrank.elitegames.auralis.adventure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureReaderTest {

    private static final String RESOURCE = "/verf-hrung-zur-entf-hrung.json";

    @Test
    void readsSampleAdventure() throws IOException {
        Adventure adventure;
        try (InputStream in = AdventureReaderTest.class.getResourceAsStream(RESOURCE)) {
            assertNotNull(in, "Test-Resource fehlt: " + RESOURCE);
            adventure = new AdventureReader().read(in);
        }

        assertEquals("Verführung zur Entführung", adventure.title());
        assertEquals(1, adventure.version());
        assertEquals("start", adventure.startLocationId());

        Content content = adventure.content();
        assertNotNull(content);
        assertEquals(4, content.locations().size());
        assertEquals(5, content.persons().size());
        assertEquals(7, content.flags().size());

        Location start = content.locations().getFirst();
        assertEquals("start", start.id());
        assertEquals("Zentraler Platz im Handelsviertel", start.title());
        assertEquals(3, start.transitions().size());

        Location walfisch = content.locations().get(3);
        assertEquals("Taverne Weisser Walfisch", walfisch.title());
        assertEquals(2, walfisch.persons().size());

        Questbook questbook = content.questbook();
        assertNotNull(questbook);
        assertEquals("Verführung zur Entführung", questbook.mainQuest().title());
        assertEquals(4, questbook.quests().size());

        Quest tavernQuest = questbook.quests().get(1);
        assertEquals("die Taverne zum Tanzenden Stier", tavernQuest.title());
        assertTrue(tavernQuest.startCondition() instanceof FlagCondition);
        FlagCondition startCondition = (FlagCondition) tavernQuest.startCondition();
        assertEquals("walk_to_tavern", startCondition.flag());
        assertEquals(Boolean.TRUE, startCondition.equals());

    }
}
