# LLM Host — Verfügbare Modelle

**Host:** `192.168.0.251:11434` (Ollama)
**Hardware:** AMD Ryzen 7 9700X, AMD Radeon **32 GB VRAM** (ROCm, GPU[0] = 31.86 GiB), 96 GB DDR5-6000
**Abfrage-Datum:** 2026-04-20 (via `curl http://192.168.0.251:11434/api/tags`, VRAM via `rocm-smi --showmeminfo vram`)

Alle Modelle in Quantisierung **Q4_K_M** (ausser Embeddings: F16).

## VRAM-Hinweis

Die Radeon hat **32 GB VRAM** (31.86 GiB laut `rocm-smi`). Neben den reinen Gewichten gehen noch KV-Cache und ROCm-Kernel drauf — realistisch bleiben **~28–29 GB** für das Modell selbst, wenn der Kontext moderat bleibt. Damit passen jetzt auch die 27–32 B-Klasse-Modelle komfortabel ins VRAM. Grössere Modelle (>30 GB):

- spillen auf CPU/RAM über → deutlich langsamer, aber läuft (96 GB RAM sind reichlich)
- werden bei vollem Spill rein auf CPU ausgeführt → brauchbar nur bei kleinen Anfragen

**Hinweis:** Ollama unterstützt AMD/ROCm seit Längerem offiziell, aber je nach GPU-Generation kann `HSA_OVERRIDE_GFX_VERSION` nötig sein. Falls Modelle unerwartet auf CPU landen: `ollama ps` prüfen und ggf. Ollama-Logs auf ROCm-Init-Fehler checken.

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
| `qwen3.5:27b` | 27.8 B | 17.4 GB | ✅ komfortabel | Top-Qualität in der 27 B-Klasse. Mit 32 GB VRAM jetzt problemlos nativ. |
| `gemma4:26b` | 25.8 B | 18.0 GB | ✅ komfortabel | Google-Flaggschiff der mittleren Klasse. Stark in Multilingualität. |
| `qwen3:30b` | 30.5 B | 18.6 GB | ✅ komfortabel | MoE-Architektur — aktiviert nur ~3–4 B pro Token. Sehr schnell bei hoher Qualität. |
| `gemma4:31b` | 31.3 B | 19.9 GB | ✅ | Hochwertig, passt bequem. |
| `deepseek-r1:32b` | 32.8 B | 19.9 GB | ✅ | Sehr starkes Reasoning. Thinking-Overhead beachten, aber kein Spill mehr. |
| `qwen3:32b` | 32.8 B | 20.2 GB | ✅ | Dense Qwen3-Flaggschiff, starker Allrounder — jetzt nativ. |
| `qwen3.5:35b` | 36.0 B | 23.9 GB | ✅ (knapp) | Neuestes MoE — Qualität sehr hoch, bei grossem Kontext evtl. eng. |
| `llama4:latest` | 108.6 B | 67.4 GB | ❌ (voller CPU/RAM-Run) | Meta-Flaggschiff (MoE). Nur auf CPU/RAM sinnvoll → sehr langsam, aber hohe Qualität für Offline-Batch-Jobs. |

## Embedding-Modelle

| Modell | Parameter | Grösse | Stärken |
|---|---|---|---|
| `nomic-embed-text:latest` | 137 M | 274 MB | Etabliert, 768-dim Embeddings, gutes Preis-/Leistungsverhältnis. Default-Wahl für RAG. |
| `snowflake-arctic-embed:latest` | 334 M | 669 MB | Top auf MTEB-Retrieval-Benchmarks. Bessere Precision als Nomic für anspruchsvolle RAG. |
| `mxbai-embed-large:latest` | 334 M | 669 MB | 1024-dim, stark auf semantischer Ähnlichkeit, gut für mehrsprachige Queries. |

## Empfehlungen für typische Einsatzzwecke

| Aufgabe | Erste Wahl (schnell, 7–14 B) | Alternative (höhere Qualität, 27–32 B, nativ) |
|---|---|---|
| Allgemeiner Chat / RAG-Antworten | `mistral:7b`, `qwen3:8b` | `qwen3.5:27b`, `qwen3:30b` (MoE) |
| Wortgetreue Wiedergabe von Quelltexten (z. B. Spielleiter `HeroldAgent`) | `qwen2.5:7b`, `llama3.1:8b` | `qwen3:32b`, `qwen3.5:27b` |
| Strukturierte Outputs (JSON, Tabellen, Tool-Use) | `qwen2.5:7b`, `qwen3:8b` | `qwen3:32b` |
| Reasoning / logische Ableitungen | `deepseek-r1:8b` | `deepseek-r1:32b` (nativ, kein Spill mehr!) |
| Kreatives Schreiben / lange Narration | `llama3.1:8b` | `gemma4:26b`, `gemma4:31b` |
| Coding-Assistenz | `qwen3:8b`, `qwen3:14b` | `qwen3:32b` |
| RAG-Embeddings | `nomic-embed-text` | `snowflake-arctic-embed` (höhere Retrieval-Precision) |

## Konkrete Empfehlung für `HeroldAgent`

Der aktuelle `gemma4:e2b` (5.1 B) ist für die Aufgabe "Abenteuertext **wortgetreu** wiedergeben, Meisterinfos **nicht** preisgeben" zu klein — kleine Modelle neigen zur Paraphrase und zum Zusammenfassen. Mit den neuen 32 GB VRAM sind jetzt auch die grösseren Modelle bequem nativ verfügbar:

1. **`qwen2.5:7b`** / **`llama3.1:8b`** — schnelle Baseline, gutes Deutsch, solides Instruction-Following
2. **`qwen3:14b`** — deutlich konsistenter als 7–8 B, wenn Wortgetreue wichtig ist
3. **`qwen3:32b`** / **`qwen3.5:27b`** — jetzt ohne Spill nutzbar, Top-Qualität für Instruction-Following und Konsistenz bei langen Abenteuertexten

## Modelle nachladen

```bash
# Auf dem LLM-Host:
ollama pull qwen2.5:7b
ollama pull llama3.1:8b
ollama list           # prüfen
```
