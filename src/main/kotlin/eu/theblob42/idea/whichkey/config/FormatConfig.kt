package eu.theblob42.idea.whichkey.config

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import eu.theblob42.idea.whichkey.model.Mapping
import java.awt.Color
import javax.swing.JLabel
import javax.swing.KeyStroke

object FormatConfig {

    private const val DEFAULT_FOREGROUND_KEY = "default"
    private val defaultForegroundColor: String
    get() = toHexColor(EditorColorsManager.getInstance().schemeForCurrentUITheme.defaultForeground)

    private const val DEFAULT_KEYWORD_KEY = "keyword"
    private val defaultKeywordColor: String
    get() = toHexColor(EditorColorsManager.getInstance().schemeForCurrentUITheme.getAttributes(TextAttributesKey.createTextAttributesKey("DEFAULT_KEYWORD")).foregroundColor)

    private const val DEFAULT_DIVIDER = " → "
    private val divider: String
    get() = when (val div = VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_Divider"]) {
        null -> DEFAULT_DIVIDER
        !is String -> DEFAULT_DIVIDER
        else -> escapeForHtml(div)
    }

    private const val DEFAULT_FONT_FAMILY = "monospace" // the alignment works best with a monospaced font
    private val fontFamily: String
    get() = when (val font = VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_FontFamily"]) {
        null -> DEFAULT_FONT_FAMILY
        !is String -> DEFAULT_FONT_FAMILY
        else -> font
    }

    private val fontSize: Int // font size in point
    get() = when (val size = VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_FontSize"]) {
        // get default value from a "basic" JLabel
        null -> JLabel().font.size
        !is Int -> JLabel().font.size
        else -> size
    }

    // configuration variables for the keys
    private val keyColor: String
    get() = when (val color = VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_KeyColor"]) {
        // default color is the foreground color of the current theme
        null -> defaultForegroundColor
        !is String -> defaultForegroundColor
        DEFAULT_FOREGROUND_KEY -> defaultForegroundColor
        DEFAULT_KEYWORD_KEY -> defaultKeywordColor
        else -> color
    }

    private val keyStyle: String
    get() = when (VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_KeyStyle"]) {
        "none" -> "span"
        "italic" -> "i"
        // default style is bold
        else -> "b"
    }

    // configuration variables for the description of commands
    private val descCommandColor: String
    get() = when (val color = VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_CommandColor"]) {
        // default color is the foreground color of the current theme
        null -> defaultForegroundColor
        !is String -> defaultForegroundColor
        DEFAULT_FOREGROUND_KEY -> defaultForegroundColor
        DEFAULT_KEYWORD_KEY -> defaultKeywordColor
        else -> color
    }

    private val descCommandStyle: String
    get() = when (VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_CommandStyle"]) {
        "bold" -> "b"
        "italic" -> "i"
        // default style is none (nothing)
        else -> "span"
    }

    // configuration variables for the description of prefixes
    private val descPrefixColor: String
    get() = when (val color = VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_PrefixColor"]) {
        // default color is the keyword color of the current theme
        null -> defaultKeywordColor
        !is String -> defaultKeywordColor
        DEFAULT_KEYWORD_KEY -> defaultKeywordColor
        DEFAULT_FOREGROUND_KEY -> defaultForegroundColor
        else -> color
    }

    private val descPrefixStyle: String
    get() = when (VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_PrefixStyle"]) {
        "bold" -> "b"
        "italic" -> "i"
        // default style is none (nothing)
        else -> "span"
    }

    /**
     * Format the given mapping with the appropriate HTML tags and font attributes
     *
     * @param entry A [Pair] of the next key to press and the corresponding [Mapping]
     * @return An HTML [String] representation of the formatted mapping
     */
    fun formatMappingEntry(entry: Pair<String, Mapping>): String {
        val (key, mapping) = entry
        val formattedKey = String.format(
            buildHtmlFormatString(keyStyle, keyColor),
            escapeForHtml(key))
        val formattedDivider = String.format(
            buildHtmlFormatString("span", defaultForegroundColor),
            divider)
        val formattedDescription = String.format(
            buildHtmlFormatString(
                if (mapping.prefix) descPrefixStyle else descCommandStyle,
                if (mapping.prefix) descPrefixColor else descCommandColor
            ),
            escapeForHtml(mapping.description))

        return "$formattedKey$formattedDivider$formattedDescription"
    }

    /**
     * Format the typed keys sequence as paragraph with appropriate HTML tags and font colors
     *
     * @param keyStrokes The key strokes which should be formatted
     * @return The formatted sequence as HTML paragraph
     */
    fun formatTypedSequence(keyStrokes: List<KeyStroke>): String {
        val currentPrefix = escapeForHtml(MappingConfig.getWhichKeyDescription(keyStrokes) ?: MappingConfig.DEFAULT_PREFIX_LABEL)
            .ifBlank { MappingConfig.DEFAULT_PREFIX_LABEL } // if custom description is blank (prefix was not displayed) use default prefix label
        val keyString = escapeForHtml(keyStrokes.joinToString(separator = "") { MappingConfig.keyToString(it) })

        return "<p>" +
                String.format(buildHtmlFormatString(keyStyle, keyColor), keyString) +
                " " +
                String.format(buildHtmlFormatString(descPrefixStyle, descPrefixColor), "[$currentPrefix]") +
                "</p>"
    }

    // *****************************************************************************************************************
    // ***** UTILITY FUNCTIONS
    // *****************************************************************************************************************

    /**
     * Build an HTML string for the usage with [String.format] to add tags and CSS colors
     *
     * @param tagName The HTML tag to use
     * @param color The font color
     * @return The built format string
     */
    private fun buildHtmlFormatString(tagName: String, color: String): String {
        return "<$tagName style=\"font-family: $fontFamily; font-size: ${fontSize}pt; color:$color;\">%s</$tagName>"
    }

    /**
     * Convert [Color] to hex color code
     *
     * @param color The AWT [Color] object to convert
     * @return The appropriate hex code for the given [Color]
     */
    private fun toHexColor(color: Color): String {
        return Integer.toHexString(color.rgb).substring(2)
    }

    /**
     * Replace any problematic HTML characters with the appropriate escaped ones
     *
     * @param text The text to escape for the usage in HTML
     * @return The escaped [String]
     */
    private fun escapeForHtml(text: String): String {
        return text
            // escape angle brackets for usage in HTML
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}