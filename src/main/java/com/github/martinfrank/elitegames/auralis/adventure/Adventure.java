package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Adventure(
        String id,
        String title,
        String slug,
        Integer version,
        String author,
        String description,
        String startLocationId,
        Content content,
        String createdAt,
        String updatedAt
) {

    public Location getLocation(String currentLocationId) {
        return content.locations().stream().filter(l -> l.id().equals(currentLocationId)).findAny().orElseThrow();
    }

    public Quest getQuest(String adventureId) {
        return content.questbook().quests().stream().filter(q -> q.id().equals(adventureId)).findAny().orElseThrow();
    }
}
