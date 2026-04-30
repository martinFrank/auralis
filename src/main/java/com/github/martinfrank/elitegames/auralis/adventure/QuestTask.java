package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestTask(
        String id,
        String description,
        Condition completionCondition,
        Boolean optional
) {}
