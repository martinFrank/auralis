package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AttributeCondition(String attribute, Integer gte, Integer lte) implements Condition {}
