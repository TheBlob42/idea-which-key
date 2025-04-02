package eu.theblob42.idea.whichkey.config

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import eu.theblob42.idea.whichkey.model.Mapping
import eu.theblob42.idea.whichkey.model.Mappings
import eu.theblob42.idea.whichkey.provider.PopupProvider
import kotlinx.coroutines.*
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.KeyStroke
import kotlin.math.ceil

class DefaultPopupProvider : PopupProvider {
    private var currentBalloon: Balloon? = null

    /**
     * Either cancel the display job or hide the current popup
     */
    override fun hidePopup() {
        // hide Balloon if present and reset value
        currentBalloon?.let {
            it.hide()
            currentBalloon = null
        }
    }

    /**
     * Show the popup presenting the nested mappings for [typedKeys]
     * Do not show the popup instantly but instead start a coroutine job to show the popup after a delay
     *
     * If there are no 'nestedMappings' (empty list) this function does nothing
     *
     * @param ideFrame The [JFrame] to attach the popup to
     * @param typedKeys The already typed key stroke sequence
     * @param nestedMappings A [List] of nested mappings to display
     * @param startTime Timestamp to consider for the calculation of the popup delay
     */
    override fun showPopup(editor: Editor, typedKeys: List<KeyStroke>, nestedMappings: List<Pair<String, Mapping>>) {
        if (nestedMappings.isEmpty()) {
            return
        }

        val ideFrame = WindowManager.getInstance().getFrame(editor.project)!!
        /*
         * the factor 0.65 was found by experimenting and comparing result lengths pixel by pixel
         * it might be erroneous and could change in the future
         */
        val frameWidth = (ideFrame.width * 0.65).toInt()
        // check for the longest string as this will most probably be the widest mapping
        val maxMapping =
            nestedMappings.maxByOrNull { (key, mapping) -> key.length + mapping.description.length }!! // (we have manually checked that 'nestedMappings' is not empty)
        // calculate the pixel width of the longest mapping string (with HTML formatting & styling)
        val maxStringWidth = JLabel("<html>${FormatConfig.formatMappingEntry(maxMapping)}</html>").preferredSize.width
        val possibleColumns = (frameWidth / maxStringWidth).let {
            when {
                // ensure a minimum value of 1 to avoid dividing by zero
                it < 1 -> 1
                // always use the full available screen space
                it > nestedMappings.size -> nestedMappings.size
                else -> it
            }
        }
        // use as much space for every column as possible
        val columnWidth = frameWidth / possibleColumns

        val elementsPerColumn = ceil(nestedMappings.size / possibleColumns.toDouble()).toInt()
        val windowedMappings = Mappings.sort(nestedMappings)
            .map(FormatConfig::formatMappingEntry)
            .windowed(elementsPerColumn, elementsPerColumn, true)

        // to properly align the columns within HTML use a table with fixed with cells
        val mappingsStringBuilder = StringBuilder()
        mappingsStringBuilder.append("<table>")
        for (i in 0..(elementsPerColumn.dec())) {
            mappingsStringBuilder.append("<tr>")
            for (column in windowedMappings) {
                val entry = column.getOrNull(i)
                if (entry != null) {
                    mappingsStringBuilder.append("<td width=\"${columnWidth}px\">$entry</td>")
                }
            }
            mappingsStringBuilder.append("</tr>")
        }
        mappingsStringBuilder.append("</table>")

        // append the already typed key sequence below the nested mappings table if configured (default: true)
        val showTypedSequence =
            when (val show = injector.variableService.getGlobalVariableValue("WhichKey_ShowTypedSequence")) {
                null -> true
                !is VimString -> true
                else -> show.asString().toBoolean()
            }
        if (showTypedSequence) {
            mappingsStringBuilder.append("<hr style=\"margin-bottom: 2px;\">") // some small margin to not look cramped
            mappingsStringBuilder.append(FormatConfig.formatTypedSequence(typedKeys))
        }

        val target = RelativePoint.getSouthWestOf(ideFrame.rootPane)
        val fadeoutTime = if (injector.globalOptions().timeout) {
            injector.globalOptions().timeoutlen.toLong()
        } else {
            0L
        }

        // the extra variable 'newWhichKeyBalloon' is needed so that the currently displayed Balloon
        // can be hidden in case the 'displayBalloonJob' gets canceled before execution
        val newWhichKeyBalloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                mappingsStringBuilder.toString(),
                null,
                EditorColorsManager.getInstance().schemeForCurrentUITheme.defaultBackground,
                null
            )
            .setAnimationCycle(10) // shorten animation time
            .setFadeoutTime(fadeoutTime)
            .createBalloon()

        newWhichKeyBalloon.show(target, Balloon.Position.above)
        currentBalloon = newWhichKeyBalloon
    }
}
