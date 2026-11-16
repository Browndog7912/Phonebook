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

## Build
Requires JDK 17.

    gradle wrapper --gradle-version 8.5    # once, if you don't have a wrapper yet
    ./gradlew build

The jar is in `build/libs/`. Drop `phonebook-1.0.0.jar` and Fabric API into `mods/`.
For development use `./gradlew runClient`.
