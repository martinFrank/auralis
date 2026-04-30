package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoveItemEffect(String itemId, Integer quantity) implements Effect {}
