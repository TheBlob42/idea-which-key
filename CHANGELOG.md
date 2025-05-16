# Changelog

## Unreleased

### Changed

- Refactor to use `AnActionListener` on the [Message Bus](https://plugins.jetbrains.com/docs/intellij/messaging-infrastructure.html#message-bus)
  - Enables proper functionality for special keys like `<C-c>`, `<M-c>`, `<Esc>`
  - Fixes **the** big know issues of the plugin
  - Fixes [#52](https://github.com/TheBlob42/idea-which-key/issues/52) and [#81](https://github.com/TheBlob42/idea-which-key/issues/81)
- Properly check for recursive mappings
  - Fixes [#71](https://github.com/TheBlob42/idea-which-key/issues/71)
  ```vim
  nnoremap <space>abc :action ShowTips<CR>
  " , should trigger the popup showing the 'a' prefix as next option
  nmap , <space>
  " ; should NOT trigger the popup but instead execute the 'original' mapping for <space>
  nnoremap ; <space>
  ```

## 0.10.3

### Changed

- Fix plugin crash due to exception with `ActionUpdateThread.OLD_EDT`
  - Fixes [#73](https://github.com/TheBlob42/idea-which-key/issues/73)

## 0.10.2

### Changed

- Fix the handling of `<Bslash>` characters for mapping descriptions
  - Fixes [#61](https://github.com/TheBlob42/idea-which-key/issues/61)
- Fix edge case for mappings that accept a motion as argument & `WhichKey_ProcessUnknownMappings`
  - This lead to issues with the builtin surround extension
  - Fixes [#48](https://github.com/TheBlob42/idea-which-key/issues/48)

## 0.10.1

### Changed

- Match latest IdeaVim API
- Update dependencies (Kotlin, Java, Intellij, IdeaVim)

## 0.10.0

**Breaking**: Needs IdeaVim version >= `2.8.0`

### Changed

- Switch to new internal APIs
- Update dependencies (Kotlin, Intellij, IdeaVim & Gradle)

## 0.9.0

### Added

- New sort option `BY_KEY_PREFIX_LAST`
- New `g:WhichKey_SortCaseSensitive` variable
  - Controls if the sorting of elements in the popup should be case-sensitive or not (default: `true`)

### Changed

- Fix missing getters for several properties to make them reloadable without restart
  - `WhichKey_DefaultDelay`
  - `WhichKey_SortOrder`
  - `WhichKey_SortCaseSensitive`
  - `WhichKey_ProcessUnknownMappings`

## 0.8.0

### Added

- Option to not process unmapped keys and just close the popup
- Make plugin toggleable via `set nowhich-key`

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
