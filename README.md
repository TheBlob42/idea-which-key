# IDEA-Which-Key

[IdeaVim](https://github.com/JetBrains/ideavim) extension that displays available keybindings in a popup

![idea which key](/assets/idea_which_key.gif)

## Installation

### Manual Installation

To build and install the plugin directly from source run the Gradle task `buildPlugin`

Afterwards you find the built jar under `/build/libs`

Install the jar in Intellij via *File -> Settings -> Plugins -> Install Plugin from Disk...*

![install plugin from disk](/assets/manual_installation.png)

## Customization

If no custom descriptions are defined, the right-hand side of all mappings will be displayed:

![default popup](/assets/popup_default.png)

To provide custom names for prefixes and commands we need to configure one variable for every mapping:

```vim
let g:WhichKeyDesc_windows        = "<Space>w  +Windows"
let g:WhichKeyDesc_windows_delete = "<Space>wd delete-window"
let g:WhichKeyDesc_windows_split  = "<Space>ws split-window-below"
...
```

The `<leader>` key is also supported

```vim
let g:WhichKeyDesc_buffer        = "<leader>b  +Buffer"
```

![configured popup](/assets/popup_configured.png)

Every variable's name has to start with `g:WhichKeyDesc_` in order to be recognized by the plugin. The rest of the variable name can be set to whatever fits best with you. For the value of each variable use the right-hand side of the mapping followed by at least one space or tab characters and finished with the description string you want to be used. 

Or if you prefer it in regular expressions:

| Part  | Regex                          | Details                                                       |
|-------|--------------------------------|---------------------------------------------------------------|
| Name  | `g:WhichKeyDesc_[a-zA-Z0-9_]+` | the only valid characters for variable names are `a-zA-Z0-9_` |
| Value | `(.*?)[ \t]+(.*)`              | group one is the mapping, group two your description          |

If you are familiar with [vim-which-key](https://github.com/liuchengxu/vim-which-key) or the emacs package [which-key](https://github.com/justbur/emacs-which-key) this handling seems odd and very inconvenient, as it requires a lot of variable definitions and therefore a lot of repetition. Unfortunately this is the case because the IdeaVim plugin only supports the following four types of variables:

- single quoted string
- double quoted string
- decimal number
- reference to another variable

Due to this limitation and the lack of any more convenient data types (array, list, dictionary, etc.) there is currently no "nicer" way of handling custom descriptions.

> As of writing the current versions are Intellij 2020.3 and IdeaVim 0.64
