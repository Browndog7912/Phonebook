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

## Notepad
Also adds a **Notepad** item (2 Paper + String, shapeless; Tools tab). Right-click to open a free-form
multi-line text box (up to 2000 characters). It saves automatically whenever you close it, and the item
tooltip shows the first line of the note.

## Calendar
Also adds a **Calendar** item (Paper + Clock + Ink Sac, shapeless; Tools tab). Right-click to open a
month view using today's real-world date.
- `<` `>` (or the mouse wheel over the grid) change month; **Today** jumps back to the current date.
- Click a day to select it. Days with events show a red dot, and today has a blue outline.
- The selected day's events are listed below. Type text and press **Add** (or Enter); click the **x** to delete one.
- Scroll over the event list if a day has more than four events.

## Clock
Also adds a **Clock** item (Clock + Paper, shapeless; Tools tab). Right-click to see the in-game time
(analog face, digital time, day/night phase, day number, tick) next to the real-world time (analog face with
second hand, digital time, date, time zone). The **Format** button switches between 12h and 24h.
In-game time follows Minecraft's day: tick 0 is 6:00 AM, noon is tick 6000, midnight is tick 18000.
In the Nether and End, which have no day/night cycle, the game time is shown but labelled accordingly.

## Stopwatch
Also adds a **Stopwatch** item (Clock + Stick, shapeless; Tools tab). **Start**/**Stop**/**Resume**, **Lap**
records split times (newest first, up to 99), **Reset** clears everything. It runs on the real-world clock,
so it keeps counting after you close the screen. The tooltip shows the current time.

## Timer
Also adds a **Timer** item (Clock + Sand, shapeless; Tools tab). Set the time with the +/- buttons for hours,
minutes and seconds (up to 99:59:59) or a 1m/5m/10m/30m preset, then **Start**. **Pause**/**Resume** and **Reset**
work as you'd expect. It also runs on the real-world clock: when it reaches zero a chime sounds and
"Timer finished!" appears in chat and above the hotbar, even with the screen closed, as long as the timer
is in your inventory.

## Calculator
Also adds a **Calculator** item (Iron Ingot + Redstone + Stone Button, shapeless; Tools tab). Right-click to open a
pocket calculator. Use the keypad or just type: digits, `.` `+` `-` `*` `/` `=` `%`, Enter to equal, Backspace to
delete a digit, Delete to clear.
- Operations run as you go (2 + 3 x 4 = 20), `5 + =` gives 10, and pressing `=` again repeats the last operation.
- `%` is a percentage of the left side after `+`/`-` (50 + 10% = 55), otherwise divide by 100.
- Also has square root, square, 1/x, +/-, and **Copy** (copies the number to your clipboard).
- Maths is exact decimal (0.1 + 0.2 = 0.3) to 12 significant digits; dividing by zero shows `Error`.
- Whole-number results of 64 or more also show how many stacks that is, e.g. `3 stacks + 12`.
- The calculator isn't saved, so each use starts at 0.

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
