# Changelog

## Unreleased

### Added

- Make the appearance of keys, prefixes, command descriptions and the divider configurable

### Changed

- Dismantle key mapping and description caching
  - Changes are reflected on `.ideavimrc` reload, no restart of Intellij required anymore
- Fixed prefix comparison (issue with the '<' character)
- Improve the usage of the available space for the popup
- By default display keys in bold and prefixes in the current theme's keyword color

## 0.1 - 2021-01-31

### Added

- Basic functionality of displaying available key bindings within a popup
- Configure displayed descriptions in your `.ideavimrc` file
