package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Location(
        String id,
        String title,
        String generalInfo,
        String specialInfo,
        String masterInfo,
        List<Effect> onEnter,
        List<Effect> onExit,
        List<Transition> transitions,
        List<PersonPresence> persons,
        List<ItemPresence> items
) {}
