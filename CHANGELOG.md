# Changelog

## Unreleased

### Added

- Make FontFamily configurable
- Added documentation about `timeout` and `timeoutlen`

## 0.2 - 2021-02-17

### Added

- Display default VIM actions (`gg`, `zt`, `<C-w>k`, etc.) in the popup.
  - This feature can be toggled on or off (default: off)
- Make the appearance of keys, prefixes, command descriptions and the divider configurable

### Changed

- minimum IdeaVIM version is now `0.65`
- Dismantle key mapping and description caching
  - Changes are reflected on `.ideavimrc` reload, no restart of Intellij required anymore
- Fixed prefix comparison (issue with the '<' character)
- Improve the usage of the available space for the popup
- By default display keys in bold and prefixes in the current theme's keyword color

## 0.1 - 2021-01-31

### Added

- Basic functionality of displaying available key bindings within a popup
- Configure displayed descriptions in your `.ideavimrc` file
