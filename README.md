# Phone Book (Fabric, Minecraft 1.20.4)

Adds a **Phone Book** item. Right-click it to open a book where every page is a table
(Name | Number). Contacts are automatically sorted into alphabet sections (A-Z, then `#`
for digits and symbols) with a header row per letter. A clickable A-Z index on the side
jumps straight to a section.

## Use
- Craft: Book + Paper + Ink Sac (shapeless). Also in the Tools creative tab.
- Type a name and number, press **Add** (or Enter).
- Click a row to select it, edit the fields, then **Save**; **Del** removes it.
- Scroll the mouse wheel or use `<` `>` to flip pages.
- Contacts are stored in the item itself (NBT), so each book is its own phone book.

## Checklist
Also adds a **Checklist** item (Paper + Ink Sac + Stick, shapeless; Tools tab). Right-click to open it.
- Type a task and press **Add** (or Enter).
- Click a row to tick it off (the text is struck through); click it again to untick.
- Click the **x** at the right of a row to delete it; **Clear done** removes all ticked tasks.
- A progress bar and "done / total" counter sit at the top; scroll or use `<` `>` for more pages.
- Tasks are stored in the item itself, so each checklist is its own list.

## Build
Requires JDK 17.

    gradle wrapper --gradle-version 8.5    # once, if you don't have a wrapper yet
    ./gradlew build

The jar is in `build/libs/`. Drop `phonebook-1.0.0.jar` and Fabric API into `mods/`.
For development use `./gradlew runClient`.

## Building on GitHub
Push this project to a GitHub repo (the `.github/workflows/build.yml` file must be in the repo root).
Every push, pull request, or manual run (Actions tab > Build > Run workflow) compiles the mod.
Download the jar from the run's **Artifacts** section. Pushing a tag such as `v1.0.0`
(`git tag v1.0.0 && git push origin v1.0.0`) also attaches the jar to a GitHub Release.
