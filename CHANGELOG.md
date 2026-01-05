<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# PMDPlugin Changelog

## [Unreleased]

### Changed

- 💥️ Breaking: Minimum required IntelliJ version is 2024.3+

## [2.0.9] - 2025-07-20

### Changed

- Minor bug fix - #257

## [2.0.8] - 2025-06-29

### Changed

- Improved rule violation documentation and examples 
- Custom rule configuration fixes
- Update to PMD version 7.15.0

## [2.0.7] - 2025-05-24

### Changed

- Bug fixes
- Support for Kotlin rules
- Update to PMD version 7.13.0

## [2.0.6] - 2025-05-04

### Changed

- Support for Kotlin rules
- Update to PMD version 7.13.0

## [2.0.4]

### Changed

- Update to PMD version 7.12.0
- Restore support for IntelliJ 2023.3+
- Upgraded IntelliJ Platform Gradle Plugin to 2.x

### Fixed

- Ease pmd updates with automated dependency update tool (#208)
- Remove upper bound of IntelliJ compatibility (#213)

## [2.0.3]

### Added

- Update to PMD version 7.7.0
- Support for IntelliJ 2024.3
- Plugin configuration is now stored in `.idea/PMDPlugin.xml` (and not anymore in `misc.xml`)
- PMD Settings moved into Settings>Tools
- Various fixes

## [2.0.2]

### Added

- Update to PMD version 7.5.0
- Support for Intellij 2024.2

## [2.0.1]

### Added

- Update to PMD version 7.2.0
- Fix issue with disabled PMD predefined rules menu

## [2.0.0]

### Added

- Update to PMD version 7.1.0 
- NOTE: custom rules built on PMD 6 will *NOT* work, if needed: stay on 1.9.2 until they are migrated

## [1.9.2]

### Added

- Bugfixes and improved stability

## [1.9.1]

### Added

- Fix issue running "All" rules

## [1.9.0]

### Added

- Support Intellij 2024 versions (only)

## [1.8.28]

### Added

- Support for running PMD task in background

## [1.8.27]

### Added

- Support for idea 2022.x and 2023.x

## [1.8.26]

### Added

- Update to PMD version 6.55.0 (#131)
- Bug fixes (#105, #121, #122, #123, #125, #126, #128)

## [1.8.25]

### Added

- Update to PMD version 6.52.0 (#120)
- Support for detecting redundant support warnings(#116, #117)

## [1.8.23]

### Added

- Update to PMD version 6.47.0
- Support for sorting violations by severity

## [1.8.22]

### Added

- Update to PMD version 6.44.0

## [1.8.21]

### Changed

- Fixed #101: bundle loading error during vcs commit

## [1.8.20]

### Added

- Update to PMD version 6.38.0

## [1.8.18]

### Changed

- Update to PMD version 6.35.0 and optional anonymous status reporting

[Unreleased]: https://github.com/amitdev/PMD-Intellij/compare/v2.0.9...HEAD
[2.0.9]: https://github.com/amitdev/PMD-Intellij/compare/v2.0.8...v2.0.9
[2.0.8]: https://github.com/amitdev/PMD-Intellij/compare/v2.0.7...v2.0.8
[2.0.7]: https://github.com/amitdev/PMD-Intellij/compare/v2.0.6...v2.0.7
[2.0.6]: https://github.com/amitdev/PMD-Intellij/compare/v2.0.4...v2.0.6
[2.0.4]: https://github.com/amitdev/PMD-Intellij/compare/v2.0.3...v2.0.4
[2.0.3]: https://github.com/amitdev/PMD-Intellij/compare/v2.0.2...v2.0.3
[2.0.2]: https://github.com/amitdev/PMD-Intellij/compare/v2.0.1...v2.0.2
[2.0.1]: https://github.com/amitdev/PMD-Intellij/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/amitdev/PMD-Intellij/compare/v1.9.2...v2.0.0
[1.9.2]: https://github.com/amitdev/PMD-Intellij/compare/v1.9.1...v1.9.2
[1.9.1]: https://github.com/amitdev/PMD-Intellij/compare/v1.9.0...v1.9.1
[1.9.0]: https://github.com/amitdev/PMD-Intellij/compare/v1.8.28...v1.9.0
[1.8.28]: https://github.com/amitdev/PMD-Intellij/compare/v1.8.27...v1.8.28
[1.8.27]: https://github.com/amitdev/PMD-Intellij/compare/v1.8.26...v1.8.27
[1.8.26]: https://github.com/amitdev/PMD-Intellij/compare/v1.8.25...v1.8.26
[1.8.25]: https://github.com/amitdev/PMD-Intellij/compare/v1.8.23...v1.8.25
[1.8.23]: https://github.com/amitdev/PMD-Intellij/compare/v1.8.22...v1.8.23
[1.8.22]: https://github.com/amitdev/PMD-Intellij/compare/v1.8.21...v1.8.22
[1.8.21]: https://github.com/amitdev/PMD-Intellij/compare/v1.8.20...v1.8.21
[1.8.20]: https://github.com/amitdev/PMD-Intellij/compare/v1.8.18...v1.8.20
[1.8.18]: https://github.com/amitdev/PMD-Intellij/commits/v1.8.18
