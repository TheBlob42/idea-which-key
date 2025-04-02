package eu.theblob42.idea.whichkey.config

import com.intellij.ide.ui.AntialiasingType
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.impl.view.FontLayoutService
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.util.ImageLoader
import java.awt.event.KeyEvent
import java.awt.font.FontRenderContext
import javax.swing.KeyStroke
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class Modifier(val icon: String)
object Modifiers {
    private val icons = mapOf(
        KeyEvent.CTRL_DOWN_MASK to Modifier("󰘴"),
        KeyEvent.ALT_DOWN_MASK to Modifier("󰘵"),
        KeyEvent.SHIFT_DOWN_MASK to Modifier("󰘶"),
    )

    fun fromKeyStroke(key: KeyStroke): List<Modifier> {
        return icons.entries.filter { (it.key and key.modifiers) == it.key }.map { it.value }
    }
}

data class Item(
    val modifiers: List<Modifier>,
    val code: String,
    val icon: String?,
    val description: String,
    val isGroup: Boolean
)

data class Highlight(
    val startOffset: Int,
    val endOffset: Int,
//            val layer: Int,
    val textAttributes: TextAttributes?,
    val targetArea: HighlighterTargetArea
)

data class TextWithHighlights(val text: String, val highlights: List<Highlight>)

fun Editor.getCharSize(): ImageLoader.Dimension2DDouble {
    val baseContext = FontInfo.getFontRenderContext(contentComponent)
    val context = FontRenderContext(
        baseContext.transform,
        AntialiasingType.getKeyForCurrentScope(true),
        UISettings.editorFractionalMetricsHint
    )
    val fontMetrics = FontInfo.getFontMetrics(colorsScheme.getFont(EditorFontType.PLAIN), context)
    // Using the '%' to calculate the size as it's usually one of the widest non-double-width characters.
    // For monospaced fonts this shouldn't really matter, but let's stay on the safe side.
    // Otherwise, we may end up with some characters falsely displayed as double-width ones.
    val width = FontLayoutService.getInstance().charWidth2D(fontMetrics, '%'.code)
    return ImageLoader.Dimension2DDouble(width.toDouble(), lineHeight.toDouble())
}

data class EditorSizeInCharacters(val width: Int, val height: Int)

fun Editor.calculateSizeInCharacters(): EditorSizeInCharacters? {
//        val contentSize = scrollingModel.visibleArea.size
    val contentSize = component.visibleRect.size
    val charSize = getCharSize()

    return if (contentSize.width > 0 && contentSize.height > 0) {
        return EditorSizeInCharacters(
            (contentSize.width / charSize.width).toInt(),
            (contentSize.height / charSize.height).toInt()
        )
    } else null
}

object PopupLayout {
    private fun dim(size: Int, parent: Int, vararg dims: SizeConfig): Int {
        var adjustedSize = if (kotlin.math.abs(size) < 1) parent * size else size
        adjustedSize = if (adjustedSize < 0) parent + adjustedSize else adjustedSize

        for (dim in dims) {
            val min = dim(dim.min, parent) ?: 0
            val max = dim(dim.max, parent) ?: parent
            adjustedSize = max(min, min(max, adjustedSize))
        }
        return floor(max(0, min(parent, adjustedSize)) + 0.5).toInt()
    }

    private fun makeExactly(str: String, widthInCodePoints: Int): String {
        val builder = StringBuilder(str)
        var len = builder.length
        while (builder.codePointCount(0, len) > widthInCodePoints) {
            len -= 1
        }
        while (builder.codePointCount(0, len) < widthInCodePoints) {
            builder.append(' ')
            len += 1
        }
        return builder.toString().substring(0, len - 1)
    }

    fun layoutItems(
        maxRowWidth: Int,
        containerWidth: Int,
        config: WhichKeyConfig,
        items: List<Item>,
    ): TextWithHighlights {
        val text = StringBuilder()
        val highlights = mutableListOf<Highlight>()
        var boxWidth = dim(maxRowWidth, containerWidth, config.width)
        val boxCount = max((containerWidth / (boxWidth + config.spacing)), 1)
        boxWidth = containerWidth / boxCount
        val boxHeight = max(ceil(items.size.toDouble() / boxCount).toInt(), 2)

        val scheme = EditorColorsManager.getInstance().globalScheme

//    val rows = t.layout(LayoutParams(width = boxWidth - config.layout.spacing))

        repeat(config.padding.first) {
            text.append("\n")
        }
        fun appendWithHighlight(str: String, highlight: TextAttributesKey) {
            highlights.add(
                Highlight(
                    text.length,
                    text.length + str.length,
                    scheme.getAttributes(highlight),
                    HighlighterTargetArea.EXACT_RANGE
                )
            )
            text.append(str)
        }

        val columns = items.withIndex().groupBy { it.index / boxHeight }

        for (lineIndex in 0..boxHeight - 1) {
            if (lineIndex > 0)
                text.append("\n")
            text.append(" ".repeat(config.padding.second))
            for (b in 0..boxCount - 1) {
                val i = b * boxHeight + lineIndex
                val item = items.getOrNull(i)
                if (b > 0) {
                    text.append(" ".repeat(config.spacing))
                }
                if (item == null) {
                    text.append(" ".repeat(boxWidth))
                    continue
                }
                val column = columns[b]
                val modifierSpaceRequired = column?.maxOf { it.value.modifiers.size } ?: 0
                var nonDescriptionLength = 5 + modifierSpaceRequired
                if (modifierSpaceRequired > 0) {
                    text.append(" ".repeat(modifierSpaceRequired - item.modifiers.size))
                    nonDescriptionLength++
                }
                for (mod in item.modifiers) {
                    appendWithHighlight(mod.icon, WHICHKEY_MAPPING_BINDING)
                }
                if (modifierSpaceRequired > 0) {
                    text.append(" ")
                }
                appendWithHighlight(item.code, WHICHKEY_MAPPING_BINDING)
                appendWithHighlight(" ➜ ", WHICHKEY_MAPPING_ASSIGMENT)

                val needsIconSpace = column?.any { it.value.icon != null } ?: false
                if (needsIconSpace) {
                    appendWithHighlight(item.icon ?: " ", WHICHKEY_MAPPING_ICON)
                    text.append(" ")
                    nonDescriptionLength += 2
                }
                val description = if (item.isGroup)
                    "+${item.description}"
                else
                    item.description

                appendWithHighlight(
                    description,
                    if (item.isGroup) WHICHKEY_MAPPING_DESCRIPTION_GROUP else WHICHKEY_MAPPING_DESCRIPTION
                )
                text.append(" ".repeat((boxWidth - description.length - nonDescriptionLength).coerceAtLeast(0)))
            }
            text.append(" ".repeat(config.padding.second))
        }

        repeat(config.padding.first) {
            text.append("\n")
        }
        return TextWithHighlights(text.toString(), highlights)
    }
}