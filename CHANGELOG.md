# Lite2Edit Changelog

## Version 0.5

### 🖼️ New Features
- Added a custom mod icon to enhance Modrinth and launcher integration (`assets/icon.png`)
- Improved the mod’s appearance in mod loaders and repositories

### 🛠️ Build & Publishing Improvements
- Fixed file order on Modrinth for clearer downloads
- Various enhancements to `build.gradle.kts` and publish workflow for better versioning and metadata
- Updates to `fabric.mod.json` and Modrinth publishing for more accurate information

### 📄 Documentation
- Updated README.md several times for clarifications and polish
- Improved visibility and explanation for mod functions and compatibility

### ⚡ Workflow & Code Maintenance
- Centralized publish logic into a single, maintainable workflow file
- Removed unneeded cleanup and manual dispatch steps for automated reliability

### 🏷️ General Enhancements
- Multiple small fixes and workflow cleanups for long-term stability
- Polished metadata across distributions

---

## Version 0.4

### 📋 Publishing & Metadata Improvements
- Added integrated changelog mechanism to Modrinth publishing workflow
- Enhanced build metadata with proper changelog integration in POM
- Improved project description and naming consistency
- Streamlined release process with better automation

### 🔧 Build System Enhancements
- Fixed various workflow permission issues for more reliable CI/CD
- Removed CloudFlare validation issues affecting build process
- Enhanced GitHub Actions workflow reliability and error handling
- Improved multi-version build system stability

### 🛠️ Development Infrastructure
- Better separation of build environments and release processes
- Enhanced build script organization and maintenance
- Improved error handling in automated release workflows
- More robust handling of publishing edge cases

### 📦 What's New for Users
- More reliable release process ensures consistent availability
- Enhanced changelog visibility on Modrinth and GitHub
- Improved project metadata for better discoverability
- Continued stability improvements across all supported versions

### 🔄 Behind the Scenes
- Ongoing workflow optimizations for faster releases
- Better handling of edge cases in automated publishing
- Enhanced logging and debugging capabilities for CI/CD
- Preparation for future feature additions

---

## Version 0.3

### 🎯 Multi-Version Support
- Expanded Minecraft version support across all WorldEdit-compatible versions
- Released builds for Minecraft 1.20.2, 1.20.4, 1.20.5, 1.20.6, 1.21, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10
- Automated multi-version release pipeline with GitHub Actions
- Consistent functionality across all supported Minecraft versions

### 🚀 Release Infrastructure
- Enhanced build system for cross-version compatibility
- Improved automated testing and validation across versions
- Streamlined release process with simultaneous multi-version deployment
- Better version targeting based on WorldEdit Fabric compatibility

### 📦 What's New for Users
- Choose your exact Minecraft version - no more compatibility guesswork
- Same reliable .litematic to WorldEdit conversion across all versions
- Future-proofed for upcoming Minecraft releases
- Simplified installation with version-specific JAR files

---

## Version 0.2

### 🔄 Major Refactoring & Architecture Improvements
- Complete rewrite of core components for better stability and performance
- Reimplemented LitematicaSchematic.kt with improved NBT handling and data structures
- Refactored ReadConverter.kt for more efficient schematic conversion
- Enhanced NBT processing with proper gzipped file loading support via stream processing

### 🛠️ Technical Enhancements
- Added Adventure NBT library integration for robust NBT data handling
- Improved dependency management - removed unnecessary Yarn mappings
- Enhanced build system with better Gradle configuration
- Optimized file size (584KB vs 44KB in v0.1) due to bundled dependencies

### 🔧 Build & Distribution
- Updated GitHub Actions workflows for multi-version Minecraft support
- Enhanced release automation with better asset attachment
- Added support for all Minecraft versions compatible with WorldEdit Fabric
- Improved CI/CD pipeline reliability and error handling

### 📋 What's New for Users
- Better performance when loading large .litematic files
- More reliable conversion of complex schematics with multiple regions
- Enhanced compatibility with various Litematica file formats
- Improved error handling and logging for troubleshooting

---

## Version 0.1

### 🚀 Initial Release
- Core functionality to convert Litematica .litematic files to WorldEdit clipboards
- Multi-region support - combine multiple named regions into single clipboard
- Block preservation - maintains block properties, rotation, and waterlogged states
- Tile entity conversion - preserves chest contents, sign text, and other NBT data

### ✨ Key Features
- Seamless integration with WorldEdit and Fabric
- Automatic block mapping from Litematica palette to WorldEdit BlockStates
- Negative dimension handling - properly normalizes regions with negative sizes
- Comprehensive logging for conversion warnings and troubleshooting

### 🔧 Technical Foundation
- Built for Minecraft 1.20.4 with Fabric Loader
- Requires Fabric Language Kotlin and WorldEdit Fabric
- MIT License for open-source development and contributions
- Gradle Kotlin DSL build system with proper dependency management

### 📋 Initial Limitations
- Read-only support - .litematic writer not yet implemented
- Basic entity conversion - some entity types not fully supported
- Modded block compatibility may vary depending on block complexity
