package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Content(
        Map<String, Object> initialState,
        List<Location> locations,
        List<Person> persons,
        List<Item> items,
        List<Flag> flags,
        Questbook questbook
) {}
