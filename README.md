# PlantUML GUI

Write PlantUML, see the diagram instantly, export it as PNG, SVG, PUML or ArchiMate XML.
A small desktop app — no installer, no account, no internet connection.

<p align="center">
  <img src="screenshot.png" alt="PlantUML GUI: the code editor on the left, the live diagram preview on the right, and the export and configuration tabs at the bottom" width="900">
  <br>
  <sub><b>PlantUML GUI in action</b> — type on the left, watch the diagram render on the right, export from the tabs below.</sub>
</p>

---

# 📥 For users

## 1. Download

**➡️ [Download `plantuml-gui-java.zip`](https://github.com/diosaraiva/plantuml-gui/raw/main/plantuml-gui-java/release/plantuml-gui-java.zip)** — one click, the file starts downloading.

That zip is everything you need. You do **not** need the source code.

<sub>You can also browse it in the [`plantuml-gui-java/release/`](plantuml-gui-java/release/) folder, or grab a tagged build from the [releases page](https://github.com/diosaraiva/plantuml-gui/releases/latest).</sub>

## 2. Install Java (once)

You need **Java 17 or newer**. Check what you have:

```
java -version
```

No Java, or an older version? Get it free from [Adoptium](https://adoptium.net/) and install it.

## 3. Run

1. Unzip `plantuml-gui-java.zip` into any folder you like.
2. Start the app:
   - **Windows** — double-click `launcher_java_windows.bat`
   - **macOS / Linux** — double-click `launcher_java_unix.sh`, or run `./launcher_java_unix.sh` in a terminal

Everything lives side by side in that one folder:

| File | What it is |
|------|------------|
| `plantuml-gui-java.jar` | The whole app, PlantUML included |
| `launcher_java_unix.sh` | Starts the app on macOS / Linux |
| `launcher_java_windows.bat` | Starts the app on Windows |
| `java_config.ini` | Your settings — created on first run, safe to delete |

> Prefer the terminal? `java -jar plantuml-gui-java.jar` works just as well.
>
> On macOS or Linux, if the shell script will not run, do `chmod +x launcher_java_unix.sh` once.

## What the app does

- **Write diagrams** in an editor with line numbers and unlimited undo/redo.
- **Start from a sample** — Activity, Class, Sequence, State, C4, ArchiMate, Gantt, Mindmap, WBS, JSON/YAML and more.
- **See it live** — the preview refreshes while you type, or only when you ask.
- **Zoom** in, out, fit to window, back to 100%, or `Ctrl`/`⌘` + scroll.
- **Export** to PNG, SVG, PUML or ArchiMate Model Exchange XML.
- **Copy** the picture, the code or the console output to the clipboard.
- **Make it yours** — theme, font, window size, language (English, Português, Español).
- **Use your own PlantUML** — point the app at any PlantUML JAR.

## The window

- **Left — Input.** The **Code** tab is where you type. The **Samples** tab loads a ready-made diagram. Underneath: the **Auto Preview** switch, a character/line counter and the **Preview** button.
- **Right — Output.** The **Preview** tab shows the picture with a zoom toolbar. The **Console** tab shows what PlantUML reported.
- **Bottom — Actions.** The **Export** tab picks the file and format. The **Config** tab holds the PlantUML JAR path and the **Save**, **Discard** and **Default** buttons.

## Everyday tasks

**Draw something**
Type in the **Code** tab. With **Auto Preview** on, the picture updates after a short pause; with it off, click **Preview**.

**Load a sample**
Open **Samples** and click an entry. It replaces what is in the editor.

**Zoom**
`+` and `−` to zoom, **Fit** to fill the window, **1:1** for 100%, or hold `Ctrl`/`⌘` and scroll.

**Export a file**
1. Open **Export**.
2. Set the **Target File** (type it or use **Browse…**).
3. Pick **PNG**, **SVG**, **PUML** or **ArchiMate Exchange (.xml)** — the extension follows automatically.
4. Click **Export File**. A dialog confirms where it landed.

Only need the picture? **Copy to Clipboard** puts it straight into any other app.

**Change the look**
Use the **Settings** menu for theme, font, window size and language.

**Save your settings**
Changes apply immediately but are **not** written to disk until you press **Save** in the **Config** tab. While something is pending the tab reads `Config *`.

- **Save** — keep the changes.
- **Discard** — go back to the stored values.
- **Default** — reset everything to factory settings.

Closing the app with pending changes simply forgets them.

**Use a different PlantUML**
Open **Config**, set **PlantUML JAR Path** (or **Browse…**), press **Save**. Clear the field to go back to the bundled one.

## Keyboard shortcuts

`⌘` on macOS, `Ctrl` everywhere else.

| Action | Shortcut |
|--------|----------|
| Undo | `⌘`/`Ctrl` + `Z` |
| Redo | `⌘`/`Ctrl` + `Shift` + `Z`, or `Ctrl` + `Y` |
| Cut | `⌘`/`Ctrl` + `X` |
| Copy | `⌘`/`Ctrl` + `C` |
| Copy Image | `⌘`/`Ctrl` + `Shift` + `C` |
| Paste | `⌘`/`Ctrl` + `V` |
| Select All | `⌘`/`Ctrl` + `A` |

**Edit** follows the tab you are working in. Cut needs a selection in the **Code** tab. Copy and Select All act on the **Console** when it is the tab you used last (or the Code tab is hidden), otherwise on the code; with nothing selected, Copy takes the whole text.

## Help, it does not work

| Symptom | What to do |
|---------|------------|
| Nothing happens when I launch it | Java is missing or too old. Run `java -version`; you need **17 or newer**. |
| "java was not found" | Install Java from [Adoptium](https://adoptium.net/), then open a new terminal or window. |
| The launcher says the JAR is missing | Keep `plantuml-gui-java.jar` in the same folder as the launcher. |
| "Cannot read the folder" or "Operation not permitted" (macOS) | macOS blocks apps from Downloads, Desktop and Documents until allowed. Move the unzipped folder elsewhere, or allow your terminal in **System Settings › Privacy & Security › Files and Folders**. |
| Empty preview or "Preview error" | Read the **Console** tab. If you set a custom PlantUML JAR, clear that field in **Config** to use the bundled one. |
| My settings disappear | Press **Save** in the **Config** tab; nothing is stored before that. |
| I want a clean slate | Close the app and delete `java_config.ini`; it comes back with the defaults. |

---

# 🛠 For developers

## Get the source and run it

```bash
git clone https://github.com/diosaraiva/plantuml-gui.git
cd plantuml-gui
./dev_java_unix.sh          # Linux / macOS
dev_java_windows.bat        # Windows
```

Both open the same menu:

1. **Package (if needed) and run the JAR** — calls `mvn package` when the JAR or the zip is stale, then runs the JAR.
2. **Run without compiling** — the Java source launcher, no `.class` files (needs Java 22+).
3. **Compile (if needed) and run** — plain `javac` into `bin/`.
4. **Clean build artifacts + reset config** — removes `launcher.cleanDirs`, stray `.class` files and the release zip, then restores `java_config.ini`.
5. **Restore default configuration**.
6. **Exit**.

## Build the release

```bash
cd plantuml-gui-java
mvn package
```

That produces two artifacts:

| Artifact | Path |
|----------|------|
| Self-contained JAR | `plantuml-gui-java/target/plantuml-gui-java.jar` |
| Distributable zip | `plantuml-gui-java/release/plantuml-gui-java.zip` |

The zip is assembled from `src/assembly/release.xml` and holds the JAR plus the two user launchers, which live in `plantuml-gui-java/release/` and are tracked in git. Everything sits at the root of the zip, so once it is unpacked the launchers find the JAR right next to them.

The zip in `release/` is the file the README links to, so commit the rebuilt zip whenever a change reaches users.

## Language level

The project targets **Java 17** — the lowest LTS that compiles the code (records, switch rules, pattern matching) and that runs the bundled PlantUML JAR, whose classes require Java 11+. There are no third-party dependencies, and APIs newer than 17 are not allowed. Verify with:

```bash
javac --release 17 -Xlint:all -Werror ...
```

## Repository layout

| Path | Purpose |
|------|---------|
| `plantuml-gui-java/` | The application: sources, resources and the bundled PlantUML JAR |
| `plantuml-gui-java/pom.xml` | Dependency-free Maven build |
| `plantuml-gui-java/release/` | The user launchers and the published `plantuml-gui-java.zip` |
| `plantuml-gui-js/` | Placeholder for a future browser port |
| `java_config.ini` | Settings shared by the developer launchers and the app |
| `dev_java_unix.sh`, `dev_java_windows.bat` | Developer menus: build, run, clean |
| `screenshot.png` | The window shot used at the top of this README |
| `AI-WORKFLOW.md` | The rules every change in this repository must follow |

## Application packages

All under `com.diosaraiva.plantumlgui`, with the dependency direction `ui` → `service` → `util`:

| Package | Responsibility |
|---------|----------------|
| `util` | Helpers: `ResourceLocator`, `FileNames`, `I18n`, `SwingUtils`, `JarUtils`, `Background`, `TextLineNumber` |
| `service` | Headless logic: `AppSettings`, `PlantUmlFormat`, `PlantUmlRenderer`, `SampleLoader`, `ArchimatePlantUmlConverter`, `ArchimateExchangeModel` |
| `ui` | Swing shell: `MainFrame`, `MenuBar`, `AboutDialog` |
| `ui.plantuml` | The working area: `PlantUmlPanel` and its input, output and footer panels |

## Configuration keys

The factory copy is `plantuml-gui-java/src/main/resources/java_config.ini`; the active copy sits next to the app. Point somewhere else with `-Dplantumlgui.config=/path/to/java_config.ini`.

| Key | Meaning |
|-----|---------|
| `launcher.projectDir`, `launcher.srcDir`, `launcher.resDir`, `launcher.outDir` | Paths used by the developer launchers |
| `launcher.buildDir`, `launcher.releaseDir`, `launcher.jarName`, `launcher.zipName` | Where Maven puts the JAR and the zip; must match `pom.xml` |
| `launcher.mainClass`, `launcher.cleanDirs`, `launcher.javacRelease` | Main class, folders removed by *Clean*, optional `javac --release` |
| `app.language`, `app.theme`, `app.font`, `app.window.width`, `app.window.height` | Look and feel |
| `app.autoPreview`, `app.previewDelayMs` | Live-preview behaviour |
| `plantuml.jarPath` | Custom PlantUML JAR (empty = bundled) |
| `plantuml.bundledJar`, `plantuml.includeDir`, `plantuml.jvmOptions` | Values used on every PlantUML run |
| `plantuml.outputDir` | Where relative export targets land |
| `export.format`, `export.targetFile` | Export defaults (`PNG`, `SVG`, `PUML`, `ARCHIMATE`) |

Previews render into a temp folder that is emptied on start-up and on exit.

## Contributing

Read **[AI-WORKFLOW.md](AI-WORKFLOW.md)** first — it defines the language level, comment style, reuse rules and the definition of done for this repository.
