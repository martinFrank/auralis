package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Transition(
        String to,
        String label,
        Condition condition,
        Check check,
        List<Effect> effects
) {}
