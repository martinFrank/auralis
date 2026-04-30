package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Quest(
        String id,
        String title,
        String generalInfo,
        String specialInfo,
        String masterInfo,
        String startLocationId,
        String startTime,
        Condition startCondition,
        Condition completionCondition,
        List<QuestTask> tasks,
        Boolean optional
) {}
