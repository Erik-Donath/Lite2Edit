# Lite2Edit

Lite2Edit is a small Fabric mod that lets WorldEdit open Litematica schematic files (.litematic / .ltc). In short: if you have a schematic created with Litematica, Lite2Edit helps WorldEdit read it so you can paste or edit it in Minecraft.

---

## Mod on Modrinth and Github
You can download the mod from Modrinth via: [https://modrinth.com/mod/lite2edit](https://modrinth.com/mod/lite2edit)
You can see the source code via: [https://github.com/Erik-Donath/lite2edit](https://github.com/Erik-Donath/lite2edit)

## Quick introduction

- What it does: Converts Litematica `.litematic` schematics into a WorldEdit clipboard that you can paste or manipulate.
- Who it's for: Players and server admins who use Fabric + WorldEdit and want to import Litematica files easily.
- Simple install: drop the mod JAR into your Fabric `mods/` folder (alongside WorldEdit and required Fabric components) and open `.litematic` files with a WorldEdit-aware tool.

---

## How to use

1. Put the Lite2Edit mod JAR into your Fabric `mods/` folder.
2. Make sure [Fabric Loader](https://fabricmc.net/), [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin), and [WorldEdit (Fabric build)](https://modrinth.com/plugin/worldedit) are installed.
3. Start Minecraft (client or server).
4. Open or import a `.litematic` / `.ltc` file. The schematic will load as a clipboard you can paste and edit. (Note: the `.litematic` writer is currently not implemented.)

If you prefer building from source, run:
```bash
git clone https://github.com/Erik-Donath/Lite2Edit.git
./gradlew build
# copy the generated JAR from build/libs/ into your mods/ folder
```

---

## How it works

This section explains what the mod does behind the scenes:

- Litematica files store world pieces as NBT data (a structured binary format Minecraft uses). Schematics contain regions made of blocks, a palette that lists unique block types used, tile-entity data (for chests, signs, furnaces, etc.), and optionally entities.

- Lite2Edit reads the Litematica file and parses the NBT structure. It understands:
    - The schematic metadata (name, author, timestamps).
    - One or more regions, each with a position and size.
    - The block palette (a list of block names and property sets).
    - The block data as a compact, bit-packed list of palette indexes.
    - Tile entities and entities stored with their coordinates and NBT.

- Block conversion:
    - The mod maps each palette entry (for example "minecraft:oak_log" with properties like facing) to a WorldEdit BlockState. When possible it preserves block properties (like rotation or waterlogged state) so the pasted structure looks correct.
    - The bit-packed block data is decoded to determine which palette entry belongs at each block position.

- Tile entities:
    - Tile-entity NBT (chest contents, sign text, custom block data) is converted into the format WorldEdit expects and attached to the corresponding block when the clipboard is created, so chests and other NBT-bearing blocks keep their data where possible.

- Regions and negative sizes:
    - Litematica can store multiple named regions in a single file. Lite2Edit can combine multiple regions into a single WorldEdit clipboard by computing a bounding box that contains all regions.
    - Some regions are defined with negative dimensions; the mod normalizes these so blocks end up in the correct positions in the clipboard.

- What’s not done yet:
    - Exporting/saving WorldEdit clipboards back into `.litematic` files (the writer is currently not implemented).
    - Full entity conversion (living mobs and some entity types are not converted; tile entities are supported).

---

## Practical notes & limits

- Most blocks and common properties are converted correctly; very unusual custom blocks or modded blocks might not map perfectly and could be skipped or logged.
- If a block type is unknown to WorldEdit, the mod will warn in the logs and skip that block.
- Tile entities are converted when possible, but very complex or mod-specific NBT may not round-trip perfectly.
- Check Minecraft logs when troubleshooting imports; conversion warnings and errors are recorded there. Please report any issues you encounter on the [Issue Page](https://github.com/Erik-Donath/Lite2Edit/issues).

---

## License & contributing

- License: MIT see [LICENSE.txt](LICENSE.txt) in this repository.
- Contributions: Pull requests and issues are welcome. If you want to:
    - Add the `.litematic` writer (export support), include an example schematic to validate round-trip correctness.
    - Improve block/property mapping or tile-entity handling, keep changes small and document behavior differences.

To contribute:
1. Fork the repository.
2. Create a feature branch.
3. Submit a pull request describing your change and any testing you performed.

Thank you for using Lite2Edit. If you run into problems or want a feature, open an issue on [GitHub](https://github.com/Erik-Donath/Lite2Edit/issues).
