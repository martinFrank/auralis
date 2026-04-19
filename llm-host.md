# LLM Host — Verfügbare Modelle

**Host:** `192.168.0.251:11434` (Ollama)
**Hardware:** AMD Ryzen 7 9700X, NVIDIA RTX 4060 Ti **16 GB VRAM**, 96 GB DDR5-6000
**Abfrage-Datum:** 2026-04-19 (via `curl http://192.168.0.251:11434/api/tags`)

Alle Modelle in Quantisierung **Q4_K_M** (ausser Embeddings: F16).

## VRAM-Hinweis

Die RTX 4060 Ti hat 16 GB VRAM. In der Praxis gehen neben den reinen Gewichten noch KV-Cache und CUDA-Kernel drauf — realistisch bleiben **~13–14 GB** für das Modell selbst, wenn der Kontext moderat bleibt. Grössere Modelle:

- spillen auf CPU/RAM über → deutlich langsamer, aber läuft (96 GB RAM sind reichlich)
- werden bei vollem Spill rein auf CPU ausgeführt → brauchbar nur bei kleinen Anfragen

## Chat- / Instruktions-Modelle

Sortiert nach VRAM-Bedarf (aufsteigend).

| Modell | Parameter | Disk/VRAM | Passt ins VRAM? | Stärken / Einsatzzweck |
|---|---|---|---|---|
| `mistral:7b` | 7.2 B | 4.4 GB | ✅ komfortabel | Solider Allrounder, gutes Instruktions-Following, robustes Deutsch. Klassischer Go-to für balanced Chat / RAG. |
| `deepseek-r1:7b` | 7.6 B | 4.7 GB | ✅ komfortabel | Reasoning-Modell (Chain-of-Thought mit `<think>`-Tags). Gut für logische Ableitungen, weniger für freie Erzählung. |
| `qwen2.5:7b` | 7.6 B | 4.7 GB | ✅ komfortabel | Sehr gutes Instruktions-Following, strukturierte Outputs (JSON/Tabellen), mehrsprachig. Stark für Planning/Tool-Use. |
| `llama3.1:8b` | 8.0 B | 4.9 GB | ✅ komfortabel | Starke Sprachgenerierung, gute Erzählqualität, langer Kontext (128k). Gut für kreative Texte und Reports. |
| `qwen3:8b` | 8.2 B | 5.2 GB | ✅ komfortabel | Neuere Qwen-Generation, allround besser als 2.5 bei gleicher Grösse. Guter Default für neue Agents. |
| `deepseek-r1:8b` | 8.2 B | 5.2 GB | ✅ komfortabel | Gleiche Reasoning-Stärke wie 7b, minimal mehr Headroom. Thinking-Tokens beachten! |
| `gemma4:e2b` | 5.1 B | 7.2 GB | ✅ | Klein & schnell, aber schwächer beim Instruktions-Following. Für Prototypen okay, für präzise Aufgaben (z. B. wortgetreue Wiedergabe) oft zu klein. |
| `qwen3.5:9b` | 9.7 B | 6.6 GB | ✅ komfortabel | Neueste Qwen-Dense-Variante. Voraussichtlich bester Allrounder in der 8–10 B-Klasse. |
| `deepseek-r1:14b` | 14.8 B | 9.0 GB | ✅ | Deutlich stärkere Reasoning-Fähigkeiten als 7b/8b, noch knapp im VRAM. Gut für komplexe Analyse / Planung. |
| `qwen3:14b` | 14.8 B | 9.3 GB | ✅ | Starkes Mittelfeld-Modell, gut für anspruchsvolle Chat-Aufgaben und Coding. |
| `gemma4:e4b` | 8.0 B | 9.6 GB | ✅ (knapp) | Grösserer Gemma-Effizienz-Build. Multimodal-Features falls benötigt. |
| `qwen3.5:27b` | 27.8 B | 17.4 GB | ❌ (Spill) | Top-Qualität in der 27 B-Klasse, läuft aber nur mit Offload → langsam. |
| `gemma4:26b` | 25.8 B | 18.0 GB | ❌ (Spill) | Google-Flaggschiff der mittleren Klasse. Stark in Multilingualität. |
| `qwen3:30b` | 30.5 B | 18.6 GB | ❌ (Spill, MoE) | MoE-Architektur — aktiviert nur ~3–4 B pro Token. Trotz Spill oft schneller als dense 14 B. Spannend für Qualität/Geschwindigkeit-Abwägung. |
| `gemma4:31b` | 31.3 B | 19.9 GB | ❌ (Spill) | Hochwertig, langsam durch Spill. |
| `deepseek-r1:32b` | 32.8 B | 19.9 GB | ❌ (Spill) | Sehr starkes Reasoning, aber durch Spill und Thinking-Overhead langsam. Für schwere Analyse-Aufgaben. |
| `qwen3:32b` | 32.8 B | 20.2 GB | ❌ (Spill) | Dense Qwen3-Flaggschiff, starker Allrounder — aber CPU-Offload. |
| `qwen3.5:35b` | 36.0 B | 23.9 GB | ❌ (Spill, MoE) | Neuestes MoE — Qualität sehr hoch, Speed moderat trotz Spill. |
| `llama4:latest` | 108.6 B | 67.4 GB | ❌ (voller CPU/RAM-Run) | Meta-Flaggschiff (MoE). Nur auf CPU/RAM sinnvoll → sehr langsam, aber hohe Qualität für Offline-Batch-Jobs. |

## Embedding-Modelle

| Modell | Parameter | Grösse | Stärken |
|---|---|---|---|
| `nomic-embed-text:latest` | 137 M | 274 MB | Etabliert, 768-dim Embeddings, gutes Preis-/Leistungsverhältnis. Default-Wahl für RAG. |
| `snowflake-arctic-embed:latest` | 334 M | 669 MB | Top auf MTEB-Retrieval-Benchmarks. Bessere Precision als Nomic für anspruchsvolle RAG. |
| `mxbai-embed-large:latest` | 334 M | 669 MB | 1024-dim, stark auf semantischer Ähnlichkeit, gut für mehrsprachige Queries. |

## Empfehlungen für typische Einsatzzwecke

| Aufgabe | Erste Wahl (passt ins VRAM) | Alternative (wenn Qualität > Speed) |
|---|---|---|
| Allgemeiner Chat / RAG-Antworten | `mistral:7b`, `qwen3:8b` | `qwen3.5:9b` |
| Wortgetreue Wiedergabe von Quelltexten (z. B. Spielleiter `HeroldAgent`) | `qwen2.5:7b`, `llama3.1:8b` | `qwen3:14b`, `qwen3.5:9b` |
| Strukturierte Outputs (JSON, Tabellen, Tool-Use) | `qwen2.5:7b`, `qwen3:8b` | `qwen3:14b` |
| Reasoning / logische Ableitungen | `deepseek-r1:8b` | `deepseek-r1:14b`, `deepseek-r1:32b` (Offload) |
| Kreatives Schreiben / lange Narration | `llama3.1:8b` | `qwen3.5:9b`, `gemma4:26b` (Offload) |
| Coding-Assistenz | `qwen3:8b`, `qwen3:14b` | `qwen3:32b` (Offload) |
| RAG-Embeddings | `nomic-embed-text` | `snowflake-arctic-embed` (höhere Retrieval-Precision) |

## Konkrete Empfehlung für `HeroldAgent`

Der aktuelle `gemma4:e2b` (5.1 B) ist für die Aufgabe "Abenteuertext **wortgetreu** wiedergeben, Meisterinfos **nicht** preisgeben" zu klein — kleine Modelle neigen zur Paraphrase und zum Zusammenfassen. Besser geeignet, alle passen bequem ins VRAM:

1. **`qwen2.5:7b`** — stärkstes Instruction-Following in der 7 B-Klasse, gutes Deutsch
2. **`llama3.1:8b`** — beste Erzählqualität, neigt weniger zum Zusammenfassen
3. **`qwen3:14b`** — wenn Qualität wichtiger ist als Antwortzeit (2–3× langsamer, aber deutlich konsistenter)

## Modelle nachladen

```bash
# Auf dem LLM-Host:
ollama pull qwen2.5:7b
ollama pull llama3.1:8b
ollama list           # prüfen
```
