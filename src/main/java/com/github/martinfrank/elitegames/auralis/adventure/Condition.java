package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(FlagCondition.class),
        @JsonSubTypes.Type(ItemCondition.class),
        @JsonSubTypes.Type(AttributeCondition.class),
        @JsonSubTypes.Type(AllOfCondition.class),
        @JsonSubTypes.Type(AnyOfCondition.class),
        @JsonSubTypes.Type(NotCondition.class)
})
public sealed interface Condition
        permits FlagCondition, ItemCondition, AttributeCondition,
                AllOfCondition, AnyOfCondition, NotCondition {
}
