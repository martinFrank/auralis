# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`herold` is a standalone Maven project under groupId `com.github.martinfrank.elitegames.auralis` (part of an "Auralis" game effort — unrelated to the surrounding `biketrip-advisor` parent directory, despite being nested inside it). The parent `../CLAUDE.md` describes a different project and does not apply here.

Current state: minimal LangChain4j agent scaffold. `HeroldAgent` wraps an Ollama `ChatLanguageModel` behind a fixed system prompt (German "Herold von Auralis" persona); `App.main` runs a single chat turn against it. Ollama is expected at `192.168.0.251:11434` with model `mistral` (see parent `../CLAUDE.md` for the host setup). No Spring, no tools, no RAG yet — everything is wired programmatically via `OllamaChatModel.builder(...)`.

`HeroldAgentTest` is a **live integration test**: it loads `src/test/resources/entfuehr.md` and pipes the whole markdown into `herold.chat(...)`, then prints the response for manual prompt-iteration. It requires Ollama on `192.168.0.251:11434` with the `mistral` model pulled — it will hang/fail otherwise, so don't run it in CI without gating.

## Build & Run Commands

Standard Maven (no wrapper scripts present — `.mvn/` is empty). Run from this directory:

```bash
mvn compile                              # Compile
mvn test                                 # Run all tests
mvn -Dtest=AppTest test                  # Run a single test class
mvn -Dtest=AppTest#testApp test          # Run a single test method
mvn package                              # Build jar into target/
java -cp target/classes com.github.martinfrank.elitegames.auralis.App   # Run App
```

## Notes for Future Changes

- `pom.xml` uses `junit-jupiter` 5.11.4 with `maven.compiler.release=21`. No explicit `maven-surefire-plugin` — relies on a modern Maven (3.9+) bringing in a surefire version that discovers JUnit 5 out of the box. Pin surefire explicitly if builds on older Maven start skipping tests.
- Package layout is `com.github.martinfrank.elitegames.auralis` even though the artifactId is `herold` — keep new classes under that package to match the groupId convention.
