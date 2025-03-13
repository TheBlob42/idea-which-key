package eu.theblob42.idea.whichkey.config

// Copied from https://github.com/folke/which-key.nvim/blob/main/lua/which-key/plugins/presets.lua
// Thanks folke!

val defaultBindings = mapOf(
    "$" to "End of line",
    "0" to "Start of line",
    "b" to "Prev word",
    "B" to "Prev WORD",
    "c" to "Change",
    "<c-w>h" to "Go to the left window",
    "<c-w>j" to "Go to the down window",
    "<c-w>k" to "Go to the up window",
    "<c-w>l" to "Go to the right window",
    "<c-w>o" to "Close all other windows",
    "<c-w>q" to "Quit a window",
    "<c-w>s" to "Split window",
    "<c-w>-" to "Decrease height",
    "<c-w><" to "Decrease width",
    "<c-w>=" to "Equally high and wide",
    "<c-w>+" to "Increase height",
    "<c-w>>" to "Increase width",
    "<c-w>_" to "Max out the height",
    "<c-w>|" to "Max out the width",
    "<c-w>" to "window",
    "<c-w>T" to "Break out into a new tab",
    "<c-w>v" to "Split window vertically",
    "<c-w>w" to "Switch windows",
    "<c-w>x" to "Swap current with next",
    "d" to "Delete",
    "e" to "Next end of word",
    "E" to "Next end of WORD",
    "f" to "Move to next char",
    "F" to "Move to prev char",
    "ge" to "Prev end of word",
    "gf" to "Go to file under cursor",
    "gg" to "First line",
    "gi" to "Go to last insert",
    "gN" to "Search backwards and select",
    "gn" to "Search forwards and select",
    "g%" to "Cycle backwards through results",
    "g," to "Go to [count] newer position in change list",
    "g;" to "Go to [count] older position in change list",
    "G" to "Last line",
    "g~" to "Toggle case",
    "gt" to "Go to next tab page",
    "gT" to "Go to previous tab page",
    "gu" to "Lowercase",
    "gU" to "Uppercase",
    "gv" to "Last visual selection",
    "gw" to "Format",
    "gx" to "Open file with system app",
    "H" to "Home line of window (top)",
    "h" to "Left",
    "j" to "Down",
    "k" to "Up",
    "L" to "Last line of window",
    "l" to "Right",
    "M" to "Middle line of window",
    "]M" to "Next method end",
    "]m" to "Next method start",
    "[M" to "Previous method end",
    "[m" to "Previous method start",
    "r" to "Replace",
    "]s" to "Next misspelled word",
    "[s" to "Previous misspelled word",
    "<" to "Indent left",
    ">" to "Indent right",
    "%" to "Matching (){}[]",
    "](" to "Next (",
    "]<" to "Next <",
    "]{" to "Next {",
    "}" to "Next empty line",
    ";" to "Next ftFT",
    "]%" to "Next unmatched group",
    "{" to "Prev empty line",
    "," to "Prev ftFT",
    "[(" to "Previous (",
    "[<" to "Previous <",
    "[{" to "Previous {",
    "[%" to "Previous unmatched group",
    "!" to "Run program",
    "?" to "Search backward",
    "/" to "Search forward",
    "^" to "Start of line (non ws)",
    "~" to "Toggle case",
    "t" to "Move before next char",
    "T" to "Move before prev char",
    "v" to "Visual",
    "V" to "Visual Line",
    "w" to "Next word",
    "W" to "Next WORD",
    "y" to "Yank",
    "zA" to "Toggle all folds under cursor",
    "za" to "Toggle fold under cursor",
    "zb" to "Bottom this line",
    "z<CR>" to "Top this line",
    "zC" to "Close all folds under cursor",
    "zc" to "Close fold under cursor",
    "zD" to "Delete all folds under cursor",
    "zd" to "Delete fold under cursor",
    "zE" to "Delete all folds in file",
    "ze" to "Right this line",
    "zf" to "Create fold",
    "zg" to "Add word to spell list",
    "zH" to "Half screen to the left",
    "zi" to "Toggle folding",
    "zL" to "Half screen to the right",
    "zM" to "Close all folds",
    "zm" to "Fold more",
    "zO" to "Open all folds under cursor",
    "zo" to "Open fold under cursor",
    "zr" to "Fold less",
    "zR" to "Open all folds",
    "zs" to "Left this line",
    "z=" to "Spelling suggestions",
    "zt" to "Top this line",
    "zv" to "Show cursor line",
    "zw" to "Mark word as bad/misspelling",
    "zx" to "Update folds",
    "zz" to "Center this line",
)
