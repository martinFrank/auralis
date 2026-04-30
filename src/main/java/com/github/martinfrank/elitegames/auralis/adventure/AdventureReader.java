package com.github.martinfrank.elitegames.auralis.adventure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class AdventureReader {

    private final ObjectMapper mapper;

    public AdventureReader() {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Adventure read(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream in = Files.newInputStream(path)) {
            return mapper.readValue(in, Adventure.class);
        }
    }

    public Adventure read(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        return mapper.readValue(in, Adventure.class);
    }

    public Adventure read(String json) throws IOException {
        Objects.requireNonNull(json, "json");
        return mapper.readValue(json, Adventure.class);
    }
}
