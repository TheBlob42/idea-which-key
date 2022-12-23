# Changelog

## Unreleased

### Added

- Option to not process unmapped keys and just close the popup

## 0.7.1

### Changed

- Update to work with version 2022.3

## 0.7.0

### Changed

- Update gradle wrapper to (`6.7.1`)
- Update intellij gradle plugin (`1.8.0`)
- Update kotlin version (`1.6.20`)
- Update kotlin coroutines (`1.6.4`)
- Remove log4j dependencies completely
- Bump minimum Idea version to `2022.2`
- Bump minimum IdeaVim version to `1.11.1`
- Use new `VariableService` to get values
- Fix several "scheduled for removal" issues

## 0.6.2 - 2021-12-15

### Changed

- Update log4j dependency to `2.16.0`
- Use proper `replaceAction` function for "VimShortcutKeyAction"

## 0.6.1 - 2021-12-13

### Changed

- Update dependencies (log4j vulnerability)

## 0.6 - 2021-11-14

### Added

- Mappings can be "removed" from the popup (not displayed, still executable)

## 0.5 - 2021-07-15

### Changed

- Unregister IdeaVIM 'VimShortcutKeyAction' to avoid startup error

## 0.4 - 2021-05-02

### Changed

- Changed plugin name to "Which-Key" (on request from Jetbrains)

### Added

- Make the order of elements configurable
- Make the popup delay configurable

## 0.3 - 2021-02-27

### Added

- Show typed keys and current prefix within popup
- Make FontFamily and FontSize configurable
- Added documentation about `timeout` and `timeoutlen`

### Changed

- Subtract the time needed for calculations from the popup delay
- Arguments for VIM motions (e.g. f, t, F, T) do not trigger the popup anymore

## 0.2 - 2021-02-17

### Added

- Display default VIM actions (`gg`, `zt`, `<C-w>k`, etc.) in the popup.
  - This feature can be toggled on or off (default: off)
- Make the appearance of keys, prefixes, command descriptions and the divider configurable

### Changed

- Minimum IdeaVIM version is now `0.65`
- Dismantle key mapping and description caching
  - Changes are reflected on `.ideavimrc` reload, no restart of Intellij required anymore
- Fixed prefix comparison (issue with the '<' character)
- Improve the usage of the available space for the popup
- By default display keys in bold and prefixes in the current theme's keyword color

## 0.1 - 2021-01-31

### Added

- Basic functionality of displaying available key bindings within a popup
- Configure displayed descriptions in your `.ideavimrc` file
