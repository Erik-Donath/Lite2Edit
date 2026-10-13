# Contributing to Lite2Edit

Bug reports, feature requests, and pull requests are welcome. Please keep changes focused and describe any behavior change.

## Development setup

1. Install JDK 21.
2. Clone the repository and use the included Gradle wrapper.
3. Run the test suite and build:

   ```sh
   ./gradlew test build
   ```

The release workflow builds each Minecraft version listed in [`.github/minecraft-versions.json`](https://github.com/Erik-Donath/Lite2Edit/blob/master/.github/minecraft-versions.json). When changing Minecraft, Fabric, or WorldEdit compatibility, verify affected versions and update that matrix only when the builds are supported.

## Pull requests

- Explain the problem and the intended change.
- Include or update tests for behavior changes where practical.
- Run `./gradlew test build` and report the result.
- Do not include credentials, private logs, or schematic files you do not have permission to redistribute.
- If the change affects supported versions, dependencies, release metadata, or user-facing behavior, update the relevant documentation.

The pull request template provides a checklist for the details maintainers need.
