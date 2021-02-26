[![Version](https://img.shields.io/jetbrains/plugin/v/15976-idea-which-key.svg?style=flat-square)](https://plugins.jetbrains.com/plugin/15976-idea-which-key)

# IDEA-Which-Key

[IdeaVim](https://github.com/JetBrains/ideavim) extension that displays available keybindings in a popup similar to [vim-which-key](https://github.com/liuchengxu/vim-which-key)

![idea which key](/assets/idea_which_key.gif)

## Installation

### JetBrains Marketplace

Install the plugin from the official [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/15976-idea-which-key)

Withing Intellij go to *File -> Settings -> Plugins -> Marketplace* search for "Idea Which Key" and click on *Install*

### Manual Installation

To build and install the plugin directly from source run the Gradle task `buildPlugin`

Afterwards you find the built jar under `/build/libs`

Install the jar in Intellij via *File -> Settings -> Plugins -> Install Plugin from Disk...*

![install plugin from disk](/assets/manual_installation.png)

### Activation

Since this is an extension plugin for IdeaVim you have to activate it explicitly within your `.ideavimrc` file  
Furthermore you should either disable the `timeout` option or increase the value for `timeoutlen`

```text
set which-key

" disable the timeout option
set notimeout

" increase the timeoutlen (default: 1000)
set timeoutlen = 5000
```

#### Explanation: `timeout` & `timeoutlen`

By default (Idea)VIM will wait for `timeoutlen` milliseconds after each key press of any unfinished mapping sequence before it cancels the whole sequence and processes each key press individually one after another. To match this behavior the IDEA-Which-Key popup will only be visible till a mapping sequence is either completed or canceled. Without any adaptions the popup is only displayed for a brief moment and will not be very useful. Therefore you should definitively modify `timout` or `timeoutlen` according to your preferences.

> IdeaVIM does not differentiate between `timeout`|`timeoutlen` and `ttimeout`|`ttimeoutlen`

## Customization

You can customize several aspects of IDEA-Which-Key via variables in your `.ideavimrc`

> Use the `:source ~/.ideavimrc` command to reload your config file

### Descriptions

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

Every variable's name has to start with `g:WhichKeyDesc_` in order to be recognized by the plugin. The rest of the variable name can be set to whatever fits best with you. For the value of each variable use the right-hand side of the mapping followed by at least one space or tab characters and finished with the description string you want to be displayed. 

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

### VIM Actions

By default Idea-Which-Key will only display key mappings which are defined in the `.ideavimrc` file. If you wish it can also display default VIM actions like `gg`, `zz`, `zt`, `<C-w>k`, etc. in the popup. For this you need to set the following variable within your `.ideavimrc`:

```vim
let g:WhichKey_ShowVimActions = "true"
```

If you wish you can also add custom descriptions for VIM Actions the same way as for other key mappings:

```vim
let g:WhichKeyDesc_goto_top = "gg   goto first line"
```

### Typed Keys

At the bottom of the popup you can see the keys you have typed so far as well as the current prefix description

You can hide this by adding the following to your `.ideavimrc`:

```vim
let g:WhichKey_ShowTypedSequence = "false"
```

### Appearance

You can configure the appearance of certain UI elements by setting the following options:

| Variable                  | Description                            | Values                                                                                       | Default Value |
|---------------------------|----------------------------------------|----------------------------------------------------------------------------------------------|---------------|
| `g:WhichKey_Divider`      | String to separate key and description | any string                                                                                   | ` → `         |
| `g:WhichKey_FontFamily`   | Font to use for the popup              | CSS `font-family` (see [examples](https://www.w3schools.com/cssref/pr_font_font-family.asp)) | `monospace`   |
| `g:WhichKey_FontSize`     | Font size for the popup                | Font size in `point` (for example `15`, `22`, etc.)                                          | IDE default³  |
| `g:WhichKey_KeyStyle`     | Font style for the keys                | `bold`, `italic`, `none`                                                                     | `bold`        |
| `g:WhichKey_KeyColor`     | Font color for the keys                | hex code or color keyword<br/>(`default`¹, `keyword`², "red", "blue", etc.)                  | `default`     |
| `g:WhichKey_PrefixStyle`  | Font style for the prefixes            | `bold`, `italic`, `none`                                                                     | `none`        |
| `g:WhichKey_PrefixColor`  | Font color for the prefixes            | hex code or color keyword<br/>(`default`¹, `keyword`², "red", "blue", etc.)                  | `keyword`     |
| `g:WhichKey_CommandStyle` | Font style for the commands            | `bold`, `italic`, `none`                                                                     | `none`        |
| `g:WhichKey_CommandColor` | Font color for the commands            | hex code or color keyword<br/>(`default`¹, `keyword`², "red", "blue", etc.)                  | `default`     |

¹`default`: the default foreground color of the currently used theme  
²`keyword`: the color for "keywords" of the currently used theme  
³Uses the IDE default value for the font size (without any configuration this should be `15`)

## Known issues

Should you check your `idea.log` file you will stumble across the following error:

```text
ERROR - nSystem.impl.ActionManagerImpl - action with the ID "VimShortcutKeyAction" was already registered. Action being registered is  (null); Registered action is Shortcuts () Plugin: IDEA Which-Key [Plugin: eu.theblob42.idea.whichkey] 
com.intellij.diagnostic.PluginException: action with the ID "VimShortcutKeyAction" was already registered. Action being registered is  (null); Registered action is Shortcuts () Plugin: IDEA Which-Key [Plugin: eu.theblob42.idea.whichkey]
```

**This is a known issue which will not compromise your usage of Intellij, IdeaVIM or IDEA-Which-Key in any way!**

In order for IDEA-Which-Key to work the plugin needs to intercept every key stroke and check if there are any key mappings which should be displayed. In order to intercept any modified key stroke (`Ctrl + w`, `Alt + n`, etc.) or special keys without a corresponding character (`Escape`, `Enter`, etc.) the plugin overwrites the `VimShortcutKeyAction` of IdeaVIM which usually handles these (this is the reason for the shown error). After checking for anything to display the event is passed to the original class. Therefore no functionality is lost by using the IDEA-Which-Key plugin.
