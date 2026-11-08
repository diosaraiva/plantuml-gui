# AI-WORKFLOW

Operating rules for any AI agent (or human) changing code in this repository.
These rules are **binding**. A change that breaks one of them is not done.

---

## 1. Golden rules

1. **Read before you write.** Locate the existing owner of a concept before adding anything. Never introduce a second way to do something that already exists.
2. **Reuse, then extend, then create.** Creating a new class is the last resort and must be justified by a second caller.
3. **One concept, one home.** Every rule, constant, format, path or string lives in exactly one place.
4. **Delete what you replace.** No dead code, no unused imports, no orphan config keys, no "just in case" methods.
5. **Leave the tree compiling clean.** `javac --release 17 -Xlint:all -Werror` must pass with zero output.

---

## 2. Comment rules (strict)

- **Single-line `//` only.** No Javadoc (`/** */`), no block comments (`/* */`), no banner or separator comments.
- **One comment above a class**, stating what it owns and why it exists. Every class has exactly one.
- **Comments explain *why*, never *what*.** If the comment restates the code, delete it.
- **Comment only the non-obvious**: an invariant, an ordering constraint, a workaround, a threading rule, a fallback.
- **No comments on getters, setters, trivial delegation or self-describing methods.**
- **No `TODO`, `FIXME`, author tags, dates, changelogs or commented-out code.**
- Comments are written in **English**, sentence case, and fit on one line.

```java
// Good - explains a constraint the reader cannot infer.
// Both pipes must be drained concurrently or the child blocks on a full buffer.

// Bad - restates the code.
// Loops over the list and adds each item.
```

---

## 3. Reuse map — check here first

Before writing any of these, use the existing owner.

| Need | Use | Never |
|------|-----|-------|
| Read a bundled file | `ResourceLocator.readString` / `openStream` / `find` | Hand-rolled classpath or `File` lookups |
| Any user-visible text | `I18n.get(key, args)` + all three bundles | String literals in Java code |
| A setting | `AppSettings` constant + key in `java_config.ini` | Ad-hoc `System.getProperty` or magic values |
| Change a setting | `AppSettings.set` (memory only) | `AppSettings.save()` outside the Save button |
| A file extension, CLI flag or export label | `PlantUmlFormat` | Extension string literals, parallel format maps |
| Split, strip or swap a file extension | `FileNames` | Inline `lastIndexOf('.')` |
| Run an external JAR | `JarUtils.runJar` | Raw `ProcessBuilder` |
| Work off the EDT | `Background.run` | `new Thread`, `SwingWorker`, virtual threads |
| Dialog, file chooser, clipboard, toolbar, menu item, look and feel, font | `SwingUtils` | Direct `JOptionPane` / `JFileChooser` / `Toolkit` / `UIManager` calls, `new Font` / `deriveFont` in panels |
| A scratch file for rendering | `PlantUmlRenderer` | A temp directory of your own |

---

## 4. Architecture rules

- Package dependency direction is strictly **`ui` → `service` → `util`**. Never import upwards.
- `service` and `util` must stay **headless**: no Swing imports, no dialogs, no EDT assumptions.
- `ui` holds **no business logic** — it collects input, delegates to `service`, and renders the result.
- All classes are `final` unless designed for extension. All utility classes are `final` with a private constructor.
- Prefer `record` for immutable data carriers. Prefer `enum` for closed sets of options.
- Constants are `private static final` and named; no magic numbers or repeated string literals.

---

## 5. Language level and build

- The compile target is **Java 17**, set once in `plantuml-gui-java/pom.xml` (`maven.compiler.release`). Never raise it to use a convenience API.
- 17 is the floor because the code uses records, switch rules and pattern matching (16+), and the ceiling because nothing here justifies dropping older JDKs. The bundled PlantUML JAR is compiled for Java 11, so 17 always runs it.
- Anything newer than 17 is banned: `Locale.of`, `Math.clamp`, `Thread.ofVirtual`, `Executors.newVirtualThreadPerTaskExecutor`, sequenced-collection methods. Verify with `javac --release 17`, not just "it compiles".
- Worker threads come only from `Background`; they are daemons so a pending task cannot block shutdown.
- The build is **one `pom.xml` with no dependencies**. `mvn package` produces two artifacts: the self-contained JAR in `plantuml-gui-java/target/` and the deliverable `plantuml-gui-java/release/plantuml-gui-java.zip`, which contains exactly the JAR and the two user launchers kept in `plantuml-gui-java/release/`.
- Everything in the zip sits at its root, so an unzipped folder is a working installation with no nesting.
- Artifact names live in three places that must agree: `pom.xml` (`finalName`, assembly descriptor), `java_config.ini` (`launcher.buildDir`, `launcher.releaseDir`, `launcher.jarName`, `launcher.zipName`) and the user launchers. Rename in all of them or in none.

---

## 6. Tree-shaking checklist

Run this on every touched file:

- [ ] No two methods do the same thing — merge them.
- [ ] No two constants hold the same value for the same reason — merge them.
- [ ] No parallel data structures keyed the same way — merge into one record-valued map.
- [ ] Every `private` member has a caller.
- [ ] Every `public` member has a caller outside its class.
- [ ] Every import is used.
- [ ] Every i18n key is referenced; every referenced key exists in **all** bundles.
- [ ] Every `java_config.ini` key is read by the app or a launcher, and every key read exists in the file.

---

## 7. Internationalization rules

- Bundles live in `plantuml-gui-java/src/main/resources/i18n/messages_<lang>_<COUNTRY>.properties`.
- Files are **UTF-8** with real accented characters. Never commit escaped or double-encoded text.
- `messages_en_US.properties` is the reference. **All bundles must contain exactly the same key set.**
- Keys are dot-namespaced by area (`menu.`, `export.`, `config.`, `console.`, `preview.`, `archimate.`).
- Placeholders use `MessageFormat` (`{0}`, `{1}`). Avoid literal single quotes in translated values.

---

## 8. Configuration rules

- Adding a setting means: a constant in `AppSettings` (or a load in both launchers), a key in `src/main/resources/java_config.ini`, and a row in the README table.
- The bundled INI is the template — it defines both defaults and key order, and its comments are preserved on write.
- **Settings never persist implicitly.** `AppSettings.set` updates memory and marks the state dirty; only `AppSettings.save()` touches the file, and only the **Save** button calls it. A menu, toggle or dialog that changes a setting must not save.
- Writing must round-trip: saving unmodified values reproduces the file byte for byte, and a single edit changes exactly one line.
- Keep `java_config.ini` at the repository root in sync with the bundled template.
- Never read a key that is not in the template.
- Restoring defaults means **producing the bundled template verbatim** — nothing else. It is implemented exactly once per launcher (`restore_config` / `:restore_config`) and once in the app (`AppSettings.resetToDefaults`). Reuse those; never inline a `cp`/`copy`.
- Anything that restores or discards config must reload the settings it derived from it, so the running process reflects the new values.

---

## 9. Launcher rules

There are two pairs of scripts, and they must not blur into each other.

**User launchers** — `plantuml-gui-java/release/launcher_java_unix.sh` and `launcher_java_windows.bat`, tracked in git and copied verbatim into the release zip:

- They **only start the app**: no menu, no prompt, no build, no clean.
- They must work from an unzipped folder that holds nothing but the JAR beside them, and from the repository, where the JAR is one level up in the build dir. Those are the only two lookups allowed.
- They pass `-Dplantumlgui.config=<script dir>/java_config.ini` so settings stay next to the app instead of one directory up.
- They check only two preconditions: `java` on the `PATH`, and (Unix) that their own folder is readable, since macOS privacy controls can block Downloads, Desktop and Documents. Each failure prints the fix; a missing JAR points at the releases page.
- They start the JVM from their own folder, so it always has a readable working directory.
- `release/` is therefore **source, not output**. Never delete the directory; the packaged zip inside it is the file the README links to, so rebuild and commit it whenever a change reaches users.

**Developer launchers** — `dev_java_unix.sh`, `dev_java_windows.bat`, kept in the repository root:

- They are **feature-mirrors**. A menu option added to one must be added to the other, with the same number, wording and confirmation prompt.
- The menu is **six options**, in this order: package and run the JAR, run from sources, compile and run, clean, restore config, exit. Option 1 calls Maven only when the JAR or the release zip is missing or older than a source or resource.
- They read every path and name from `java_config.ini`; no hard-coded project layout beyond the fallback defaults.
- Every destructive action asks for confirmation and prints exactly what it will touch beforehand. Clean removes `launcher.cleanDirs`, stray `.class` files and the release zip — never the `release/` directory itself.

Verify shell scripts with `bash -n`, and check batch files for undefined labels and missing `exit /b`.

---

## 10. Definition of done

A change is complete only when **all** of the following hold:

1. `javac --release 17 -Xlint:all -Werror` produces no output, and `mvn package` builds `release/plantuml-gui-java.zip`.
2. The app starts from the JAR unzipped out of that zip and renders the default sample.
3. Every new user-visible string exists in `en_US`, `pt_BR` and `es_ES`, and no bundle has an unmatched key.
4. Nothing was duplicated: the reuse map in §3 was checked.
5. Comments follow §2 exactly — one single-line comment per class, and none that restate code.
6. Any code, key or file the change made redundant has been deleted.
7. Both developer launchers still offer the same options, both user launchers still start the app with no prompt, and `bash -n` passes on both shell scripts.
8. The README reflects any change to behaviour, configuration, menus or layout.

---

## 11. Verification commands

```bash
cd plantuml-gui-java

# Compile the whole tree at the supported language level, warnings are errors.
find src/main/java -name "*.java" > /tmp/sources.txt
javac --release 17 -Xlint:all -Werror -encoding UTF-8 -d /tmp/build @/tmp/sources.txt

# Package, then check the deliverable holds exactly the JAR and the two user launchers.
mvn -B package
unzip -l release/plantuml-gui-java.zip

# Run exactly what ships, from a clean unzipped folder.
rm -rf /tmp/pgtest && mkdir /tmp/pgtest && cd /tmp/pgtest
unzip -q "$OLDPWD/release/plantuml-gui-java.zip" && ./launcher_java_unix.sh
cd "$OLDPWD"

# Run from the project directory so resources resolve.
java -cp /tmp/build:src/main/resources com.diosaraiva.plantumlgui.Main

# Confirm every bundle carries the same keys.
for f in src/main/resources/i18n/*.properties; do
  echo "$f: $(grep -c '^[a-z].*=' "$f") keys"
done

# Script syntax, and confirm the active config matches the template after a reset.
cd ..
bash -n dev_java_unix.sh && bash -n plantuml-gui-java/release/launcher_java_unix.sh
diff java_config.ini plantuml-gui-java/src/main/resources/java_config.ini
```
