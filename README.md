# arch-utils

A dependency-free Java Swing desktop app for architecture tooling: PlantUML editing/preview/export, ArchiMate Model Exchange XML generation, and CSV/JSON/Markdown tabular data conversion.

## AI Development Instructions

These instructions capture the project's design decisions and conventions. Follow them with a simplistic mindset: keep the code small, dependency-free, and idiomatic. **Do not add code comments** — code must be self-explanatory through naming and structure.

### Core Principles

- **Zero external dependencies.** Use only the JDK (`java.xml` DOM instead of JAXB, hand-rolled JSON parsing, `Preferences` instead of config files). The PlantUML jar is bundled as a resource and invoked as a subprocess, never linked.
- **Simplicity first.** Prefer tiny, dependency-free classes any code can use without wiring. Best-effort behavior over complex error handling: cleanup is best-effort, unmapped input produces warnings instead of guesses, failures are logged or shown — never thrown across UI boundaries.
- **Closed sets are enums.** Enums are implicitly sealed, so a fixed list of formats (PlantUML output formats, tabular export formats) is modeled as an enum carrying its own metadata (file extension, MIME type, CLI flag, `needsJar`).
- **Sealed interfaces for exhaustive switching.** Service families (e.g. `ExportService` for CSV/JSON/Markdown) are sealed to a fixed set of implementations so callers can switch exhaustively.
- **Immutability by default.** Records with defensive copies (e.g. `TableData` as the neutral intermediate representation: parse once, render to any format).

### Architecture & Layering

- **UI never touches PlantUML directly.** All jar invocation goes through the `plantuml` package: `PlantUmlRenderer` (preview) and `PlantUmlExporter` (file export). Renderer's file-export method is package-visible so only the exporter drives it.
- **Single entry points per concern:**
  - `I18n` — the only source of UI strings; UI classes never hard-code text or instantiate `ResourceBundle` ad hoc.
  - `PlantUmlConsole` — the single application-wide console capture mechanism (in-memory buffer + optional `System.out/err` tee).
  - `AppSettings` — persistent settings via `Preferences` (currently the UI language as a BCP-47 tag, defaulting to en-US).
- **Neutral intermediates.** Data conversion pivots through one shared model (`TableData` for CSV/JSON/Markdown; `ArchimateExchangeModel` for XML export) — never format-to-format directly. Markdown is always derived from the current CSV so both edit paths converge.

### Startup Sequence (Main)

1. Install the global console tee **first** so nothing is missed from startup.
2. Apply the persisted language so every window builds with the right locale.
3. Clean the `temp/` render folder on start and register a shutdown hook to clean it again (best-effort; ignore failures — the folder is transient).
4. Show `MainFrame` on the EDT with the platform default look and feel.

### Threading

- **Never block the EDT.** Blocking work (rendering, export, file I/O) runs via `Background`, backed by a virtual-thread-per-task executor (tasks are cheap; no pool sizing; long renders never starve each other). Success/error callbacks are always marshalled back to the EDT so callers may touch Swing directly.
- Live preview is debounced: a timer waits for an idle delay after the last keystroke, then renders into `temp/` on a virtual thread and refreshes the preview.
- Subprocess streams are drained concurrently on their own threads to avoid the subprocess blocking on a full pipe buffer. Streams are pumped through this JVM's `System.out/err` (not `inheritIO()`) so the teed console captures output while the real terminal still sees it.
- Console/UI mutators must be EDT-safe. Listener lists use `CopyOnWriteArrayList` so notification never holds the append lock.

### Internationalization

- Three locales: en-US, es-ES, pt-BR. `.properties` files are read strictly as UTF-8 (never ISO-8859-1 fallback).
- Missing keys resolve to the wrapped key itself (`!key!`) so gaps are visible in the UI instead of throwing.
- Runtime language switching: every panel exposes `applyLanguage()` to re-apply localized chrome without losing editor/preview state; the menu bar is rebuilt in place. Language menu options display their own endonyms (proper nouns, never localized). Format acronyms (PNG/SVG/PUML) stay literal.

### Swing Conventions

- Shared helpers live in `SwingUtils`: menu/toolbar factories, clipboard, look-and-feel utilities, platform menu-shortcut modifier (Cmd on macOS, Ctrl elsewhere). All toolbars/buttons use the shared factories so they look identical.
- Reusable components are parameterized by handlers (e.g. `ConsoleView` is shared by the PlantUML Console tab and the standalone Java Console window).
- Singleton-style windows (e.g. the Java Console) keep one reusable instance so repeated opens focus the same window.
- Guard against re-entrant document events when programmatically pushing derived text into editors.
- A freshly loaded sample resets the undo history (a clean starting point, not an undoable edit).
- Defer viewport-dependent layout (e.g. zoom-to-fit) until after a card switch so sizes are final.

### PlantUML Specifics

- Headless subprocess JVM options keep it off the macOS Dock; `plantuml.include.path` points at the samples dir so relative `!include`s resolve.
- Preview compiles use `-stdrpt:1` and capture stdout+stderr so syntax errors reach the console instead of only being embedded in the generated image.
- `.puml` export just writes the source — nothing to render. SVG export renders a PNG proxy for the preview (no inline raster).

### ArchiMate Export

- `ArchimateExchangeModel` builds Archi's native `.archimate` XML (ArchiMate 3.x metamodel) using the JDK DOM: `archimate:model` root, standard layer `<folder>` elements pre-created (so importers always find the expected structure), `xsi:type="archimate:<Type>"` and unique generated ids on every concept.
- `PlantUmlArchimateConverter` maps PlantUML ArchiMate macros (`Layer_Type(id, "Name")`, `Rel_Type(src, dst[, "label"])`) to model concepts. Anything unrecognised is collected as a warning, never guessed. Relationships referencing unknown aliases create the id lazily and emit a warning rather than dropping the relationship.

### Testing

- No test framework on the classpath. Tests are **self-verifying programs**: run their `main()`; they print one line per check and exit non-zero on any failure.
- Tests must not leak global state: use fresh instances (not the global console), restore original streams, and restore the default locale.

## Future releases:
- Archimate Themes Samples
- PlantUML AWS Support