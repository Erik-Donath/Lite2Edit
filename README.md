# Lite2Edit

[![CI](https://github.com/Erik-Donath/Lite2Edit/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Erik-Donath/Lite2Edit/actions/workflows/ci.yml)
[![Build and Publish Release](https://github.com/Erik-Donath/Lite2Edit/actions/workflows/publish.yml/badge.svg)](https://github.com/Erik-Donath/Lite2Edit/actions/workflows/publish.yml)
[![Modrinth](https://img.shields.io/modrinth/dt/lite2edit?logo=modrinth)](https://modrinth.com/mod/lite2edit)

Lite2Edit is a Fabric mod that lets WorldEdit load and save Litematica `.litematic` schematics. Load a Litematica build into a WorldEdit clipboard, edit or paste it, and save the clipboard back as a Litematica file. Minecraft versions use their published version labels; Lite2Edit lists supported targets explicitly rather than deriving them from a version pattern.

## Install

Install Lite2Edit together with [Fabric Loader](https://fabricmc.net/), [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin), and the [Fabric version of WorldEdit](https://modrinth.com/plugin/worldedit). Use dependency versions compatible with your Minecraft version.

Download Lite2Edit from [Modrinth](https://modrinth.com/mod/lite2edit) or the [GitHub releases page](https://github.com/Erik-Donath/Lite2Edit/releases). The release provides a separate build for each Minecraft version in the [supported version matrix](https://github.com/Erik-Donath/Lite2Edit/blob/master/.github/minecraft-versions.json).

## Use

Use WorldEdit’s schematic commands:

```text
//schematic load "filename"
//schematic save -f "filename" litematica
```

Lite2Edit registers the `litematica`, `litematic`, and `ltc` format aliases.

## Compatibility and limitations

- Lite2Edit targets the Minecraft versions in the [supported version matrix](https://github.com/Erik-Donath/Lite2Edit/blob/master/.github/minecraft-versions.json). Install matching Fabric Loader, Fabric Language Kotlin, and WorldEdit builds.
- Imports combine all Litematica regions into one WorldEdit clipboard. Negative region dimensions are normalized.
- Block states and block-entity NBT are preserved when WorldEdit supports the block and state. Unknown block types are skipped with a log warning; unsupported properties or complex mod-specific NBT may not round-trip exactly.
- Entities are imported when WorldEdit recognizes their type and required position data. Unsupported or incomplete entities are skipped. Entity behavior can vary by Minecraft and WorldEdit version.
- Exports contain one region named `Unnamed`; original region names and boundaries are not retained.

## Troubleshooting and support

Check the Minecraft log for conversion warnings. For a bug report, include the Lite2Edit, Minecraft, Fabric Loader, Fabric Language Kotlin, and WorldEdit versions, along with reproducible steps and relevant log output. Remove private information before attaching logs.

Report bugs and request features using the [GitHub issue tracker](https://github.com/Erik-Donath/Lite2Edit/issues).

## Contributing

Contributions are welcome. See the [contribution guidelines](https://github.com/Erik-Donath/Lite2Edit/blob/master/CONTRIBUTING.md) before opening a pull request.

## License

Lite2Edit is licensed under the MIT License; see [`LICENSE.txt`](https://github.com/Erik-Donath/Lite2Edit/blob/master/LICENSE.txt).
