# Releasing Lite2Edit

## Before publishing

- Confirm the intended release version and update `mod_version` in `gradle.properties` when preparing a release from a normal branch build.
- Review the supported Minecraft versions in [`.github/minecraft-versions.json`](https://github.com/Erik-Donath/Lite2Edit/blob/master/.github/minecraft-versions.json). Add or remove a version only after verifying that its release build succeeds and the required Fabric and WorldEdit dependencies are available.
- Run `./gradlew test build generatePomFileForMavenPublication`.
- Update user-facing behavior, compatibility, and known limitations in `README.md`.
- Create a GitHub release with a tag such as `v1.0` and meaningful release notes in the release body. The optional leading `v` is removed for the published artifact version.

## Automated publication

Publishing starts when a GitHub release is published. The workflow builds one artifact per Minecraft version in the shared matrix, uploads each version to Modrinth with the GitHub release notes as its changelog, publishes Maven artifacts to GitHub Packages, and attaches the JARs to the GitHub release.

After all matrix builds and uploads succeed, a separate job syncs the repository `README.md` to the Modrinth project page body. Use absolute links for repository content in the README because it is also rendered on Modrinth.

Configure these repository Actions secrets:

- `MODRINTH_TOKEN`: a Modrinth token with `CREATE_VERSION` and `PROJECT_WRITE` scopes.
- `MODRINTH_ID`: the Lite2Edit Modrinth project ID.
- `GITHUB_TOKEN` is supplied by GitHub Actions; the workflow uses it to publish to GitHub Packages and attach release files.

The GitHub Packages publication uses `GITHUB_ACTOR` from the workflow. Verify package permissions and the repository’s Actions settings before publishing.
