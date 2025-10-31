# Lite2Edit Changelog

## Version 0.2 (October 31, 2025)

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

## Version 0.1 (October 25, 2025)

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
