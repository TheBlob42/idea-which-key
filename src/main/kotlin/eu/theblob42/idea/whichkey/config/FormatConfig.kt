package eu.theblob42.idea.whichkey.config

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import eu.theblob42.idea.whichkey.model.Mapping
import java.awt.Color
import java.awt.Font
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
     * Format all given mappings with the appropriate HTML tags and font colors
     * @param mappings The mappings which should be formatted
     * @return A [List] of the [String]s representing the formatted mappings
     */
    fun formatMappings(mappings: List<Pair<String, Mapping>>): List<String> {
        return mappings
            .map { (key, mapping) ->
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

                "$formattedKey$formattedDivider$formattedDescription"
            }
    }

    /**
     * Calculate the string width (number of characters) without any HTML tags or style attributes for the given [entry]
     *
     * @param entry A [Pair] of the next key to press and the corresponding [Mapping] that should be used for the calculation
     * @return The raw string width (number of characters)
     */
    fun calcRawMappingWidth(entry: Pair<String, Mapping>): Int {
        val (key, mapping) = entry
        return "${key}$divider${mapping.description}".length
    }

    /**
     * Calculate the pixel width of the given [entry] after formatting it according to the configured
     * font-family, font-size and font-style for each individual part (key, divider & description).
     *
     * The calculated width is not 100% exact but its approximation is very close and from testing only a few pixels of,
     * which is good enough for our use case.
     *
     * @param entry A [Pair] of the next key to press and the corresponding [Mapping] that should be used for the calculation
     * @return The approximate width of the formatted entry in pixels
     */
    fun calcFormattedMappingWidth(entry: Pair<String, Mapping>): Int {
        val (key, mapping) = entry
        val keyFont = Font(fontFamily, toFontStyle(keyStyle), fontSize)
        val dividerFont = Font(fontFamily, Font.PLAIN, fontSize)
        val descriptionFont = Font(
            fontFamily,
            toFontStyle(if (mapping.prefix) descPrefixStyle else descCommandStyle),
            fontSize)

        val keyLabel = JLabel(key)
        keyLabel.font = keyFont
        val keyWidth = keyLabel.preferredSize.width

        val dividerLabel = JLabel(divider)
        dividerLabel.font = dividerFont
        val dividerWidth = dividerLabel.preferredSize.width

        val descriptionLabel = JLabel(mapping.description)
        descriptionLabel.font = descriptionFont
        val descriptionWidth = descriptionLabel.preferredSize.width

        return keyWidth + dividerWidth + descriptionWidth
    }

    /**
     * Format the typed keys sequence as paragraph with appropriate HTML tags and font colors
     * @param keyStrokes The key strokes which should be formatted
     * @return The formatted sequence as HTML paragraph
     */
    fun formatTypedSequence(keyStrokes: List<KeyStroke>): String {
        val currentPrefix = escapeForHtml(MappingConfig.getWhichKeyDescription(keyStrokes) ?: MappingConfig.DEFAULT_PREFIX_LABEL)
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
     * Convert the given HTML font [tag] into an appropriate [Font] style value
     * @param tag The HTML tag to convert (e.g. "i", "b")
     * @return The appropriate constant (e.g. [Font.ITALIC], [Font.BOLD])
     */
    private fun toFontStyle(tag: String): Int {
        return when (tag) {
            "i" -> Font.ITALIC
            "b" -> Font.BOLD
            else -> Font.PLAIN
        }
    }

    /**
     * Build an HTML string for the usage with [String.format] to add tags and CSS colors
     * @param tagName The HTML tag to use
     * @param color The font color
     * @return The built format string
     */
    private fun buildHtmlFormatString(tagName: String, color: String): String {
        return "<$tagName style=\"font-family: $fontFamily; font-size: ${fontSize}pt; color:$color;\">%s</$tagName>"
    }

    /**
     * Convert [Color] to hex color code
     * @param color The AWT [Color] object to convert
     * @return The appropriate hex code for the given [Color]
     */
    private fun toHexColor(color: Color): String {
        return Integer.toHexString(color.rgb).substring(2)
    }

    /**
     * Replace any problematic HTML characters with the appropriate escaped ones
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