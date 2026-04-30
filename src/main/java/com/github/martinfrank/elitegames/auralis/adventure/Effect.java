package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SetFlagEffect.class, name = "setFlag"),
        @JsonSubTypes.Type(value = SetVarEffect.class, name = "setVar"),
        @JsonSubTypes.Type(value = IncVarEffect.class, name = "incVar"),
        @JsonSubTypes.Type(value = GiveItemEffect.class, name = "giveItem"),
        @JsonSubTypes.Type(value = RemoveItemEffect.class, name = "removeItem")
})
public sealed interface Effect
        permits SetFlagEffect, SetVarEffect, IncVarEffect, GiveItemEffect, RemoveItemEffect {
}
