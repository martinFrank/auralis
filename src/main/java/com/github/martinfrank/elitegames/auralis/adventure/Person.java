package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Person(
        String id,
        String name,
        String generalInfo,
        String specialInfo,
        String masterInfo,
        String revealed,
        Map<String, Object> properties
) {}
