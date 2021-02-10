package eu.theblob42.idea.whichkey

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.maddyhome.idea.vim.option.OptionsManager
import kotlinx.coroutines.*
import javax.swing.JFrame
import javax.swing.JLabel
import kotlin.math.ceil

object PopupConfig {

    private var currentBalloon: Balloon? = null
    private var displayBalloonJob: Job? = null

    private val fadeoutTime = if (OptionsManager.timeout.value) {
        OptionsManager.timeoutlen.value().toLong()
    } else {
        0L
    }

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
     * Show the popup presenting the nested mappings
     * Do not show the popup instantly but instead start a coroutine job to show the popup after a delay
     *
     * If there are no 'nestedMappings' (empty list) this function does nothing
     *
     * @param ideFrame The [JFrame] to attach the popup to
     * @param nestedMappings A [List] of nested mappings to display
     */
    fun showPopup(ideFrame: JFrame, nestedMappings: List<Pair<String, Mapping>>) {
        if (nestedMappings.isEmpty()) {
            return
        }

        val frameWidth = ideFrame.width
        // check for the longest string without HTML tags or styling (we have manually checked that 'nestedMappings' is not empty)
        val maxString: String = nestedMappings.maxByOrNull { FormatConfig.formatRawMapping(it).length }!!.let { FormatConfig.formatRawMapping(it) }
        /*
         * there might be a better way to measure the pixel width of a given String than using a JLabel
         * so far this method has worked quite well and since I'm missing a better alternative this stays for now
         */
        val maxStringWidth = JLabel(maxString).preferredSize.width
        val possibleColumns = (frameWidth / maxStringWidth).let {
            // ensure a minimum value of 1 to avoid dividing by zero
            if (it < 1) 1 else it
        }
        val elementsPerColumn = ceil(nestedMappings.size / possibleColumns.toDouble()).toInt()
        val windowedMappings = FormatConfig.formatMappings(
            // TODO implement other sort options
            nestedMappings.sortedBy { it.first }
        ).windowed(elementsPerColumn, elementsPerColumn, true)

        // to properly align the columns within HTML use a table with fixed with cells
        val mappingsStringBuilder = StringBuilder()
        mappingsStringBuilder.append("<table>")
        for (i in 0..(elementsPerColumn.dec())) {
            mappingsStringBuilder.append("<tr>")
            for (column in windowedMappings) {
                val entry = column.getOrNull(i)
                if (entry != null) {
                    mappingsStringBuilder.append("<td width=\"$maxStringWidth\">$entry</td>")
                }
            }
            mappingsStringBuilder.append("</tr>")
        }
        mappingsStringBuilder.append("</table>")

        val target = RelativePoint.getSouthWestOf(ideFrame.rootPane)

        // the extra variable 'newWhichKeyBalloon' is needed so that the currently displayed Balloon
        // can be hidden in case the 'displayBalloonJob' gets canceled before execution
        val newWhichKeyBalloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(mappingsStringBuilder.toString(), null, EditorColorsManager.getInstance().globalScheme.defaultBackground, null)
            .setAnimationCycle(10) // shorten animation time
            .setFadeoutTime(fadeoutTime)
            .createBalloon()

        // wait for a few ms before showing the Balloon to prevent
        // flickering on fast consecutive key presses
        displayBalloonJob = GlobalScope.launch {
            delay(200)
            newWhichKeyBalloon.show(target, Balloon.Position.above)
            currentBalloon = newWhichKeyBalloon
        }
    }
}