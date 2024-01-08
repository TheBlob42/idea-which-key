package eu.theblob42.idea.whichkey.config

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import eu.theblob42.idea.whichkey.model.Mapping
import kotlinx.coroutines.*
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.KeyStroke
import kotlin.math.ceil

object PopupConfig {

    private const val DEFAULT_POPUP_DELAY = 200
    private val defaultPopupDelay: Int
    get() = when (val delay = injector.variableService.getGlobalVariableValue("WhichKey_DefaultDelay")) {
        null -> DEFAULT_POPUP_DELAY
        !is VimInt -> DEFAULT_POPUP_DELAY
        else -> delay.value
    }

    private val DEFAULT_SORT_OPTION = SortOption.BY_KEY
    private val sortOption: SortOption
    get() = when (val option = injector.variableService.getGlobalVariableValue("WhichKey_SortOrder")) {
        null -> DEFAULT_SORT_OPTION
        !is VimString -> DEFAULT_SORT_OPTION
        else -> SortOption.values().firstOrNull { it.name.equals(option.asString(), ignoreCase = true) } ?: DEFAULT_SORT_OPTION
    }

    private const val DEFAULT_SORT_CASE_SENSITIVE = true
    private val sortCaseSensitive: Boolean
    get() = when (val option = injector.variableService.getGlobalVariableValue("WhichKey_SortCaseSensitive")) {
        null -> DEFAULT_SORT_CASE_SENSITIVE
        !is VimString -> DEFAULT_SORT_CASE_SENSITIVE
        else -> option.asString().toBoolean()
    }

    private var currentBalloon: Balloon? = null
    private var displayBalloonJob: Job? = null

    /**
     * Either cancel the display job or hide the current popup
     */
    fun hidePopup() {
        // cancel job or wait till it's done (if it already started)
        runBlocking {
            displayBalloonJob?.cancelAndJoin()
        }
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
    fun showPopup(ideFrame: JFrame, typedKeys: List<KeyStroke>, nestedMappings: List<Pair<String, Mapping>>, startTime: Long) {
        if (nestedMappings.isEmpty()) {
            return
        }

        /*
         * the factor 0.65 was found by experimenting and comparing result lengths pixel by pixel
         * it might be erroneous and could change in the future
         */
        val frameWidth = (ideFrame.width * 0.65).toInt()
        // check for the longest string as this will most probably be the widest mapping
        val maxMapping = nestedMappings.maxByOrNull { (key, mapping) -> key.length + mapping.description.length }!! // (we have manually checked that 'nestedMappings' is not empty)
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
        val windowedMappings = sortMappings(nestedMappings)
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
        val showTypedSequence = when (val show = injector.variableService.getGlobalVariableValue("WhichKey_ShowTypedSequence")) {
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
            .createHtmlTextBalloonBuilder(mappingsStringBuilder.toString(), null, EditorColorsManager.getInstance().schemeForCurrentUITheme.defaultBackground, null)
            .setAnimationCycle(10) // shorten animation time
            .setFadeoutTime(fadeoutTime)
            .createBalloon()

        /*
         * wait for a few ms before showing the Balloon to prevent flickering on fast consecutive key presses
         * subtract the already passed time (for calculations etc.) to make the delay as consistent as possible
         */
        val delay = (defaultPopupDelay - (System.currentTimeMillis() - startTime)).let {
            if (it < 0) 0 else it
        }

        displayBalloonJob = GlobalScope.launch {
            delay(delay)
            newWhichKeyBalloon.show(target, Balloon.Position.above)
            currentBalloon = newWhichKeyBalloon
        }
    }

    /**
     * Sort mappings dependent on the configured sort options
     * @param nestedMappings The list of mappings to sort
     * @return The sorted list of mappings
     */
    private fun sortMappings(nestedMappings: List<Pair<String, Mapping>>): List<Pair<String, Mapping>> {
        // String::compareTo is by default case-sensitive
        val cmp = if (sortCaseSensitive) String::compareTo else String.CASE_INSENSITIVE_ORDER::compare

        return when (sortOption) {
            SortOption.BY_KEY -> nestedMappings.sortedWith(compareBy(cmp) { it.first })
            SortOption.BY_KEY_PREFIX_FIRST -> nestedMappings.sortedWith(compareBy<Pair<String, Mapping>> { !it.second.prefix }.thenBy(cmp) { it.first })
            SortOption.BY_KEY_PREFIX_LAST -> nestedMappings.sortedWith(compareBy<Pair<String, Mapping>> { it.second.prefix }.thenBy(cmp) { it.first })
            SortOption.BY_DESCRIPTION -> nestedMappings.sortedWith(compareBy(cmp) { it.second.description })
        }
    }
}

enum class SortOption {
    BY_KEY,
    BY_KEY_PREFIX_FIRST,
    BY_KEY_PREFIX_LAST,
    BY_DESCRIPTION
}