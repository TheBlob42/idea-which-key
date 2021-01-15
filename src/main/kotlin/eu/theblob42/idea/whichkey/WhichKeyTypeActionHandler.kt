package eu.theblob42.idea.whichkey

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.option.OptionsManager
import kotlinx.coroutines.*
import javax.swing.JLabel
import javax.swing.KeyStroke

class WhichKeyTypeActionHandler(private val vimTypedActionHandler: VimTypedActionHandler): TypedActionHandlerEx {

    private var currentBalloon: Balloon? = null
    private var displayBalloonJob: Job? = null

    override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
        vimTypedActionHandler.execute(editor, charTyped, dataContext)
    }

    override fun beforeExecute(editor: Editor, charTyped: Char, dataContext: DataContext, plan: ActionPlan) {
        // cancel job or wait till it's done (if it already started)
        runBlocking {
            displayBalloonJob?.cancelAndJoin()
        }
        // hide Balloon if present and reset value
        currentBalloon?.let {
            it.hide()
            currentBalloon = null
        }


        val ideFrame = WindowManager.getInstance().findVisibleFrame()

        val mappingState = CommandState.getInstance(editor).mappingState
        val typedKeySequence = mappingState.keys + listOf(KeyStroke.getKeyStroke(charTyped))
        val nestedMappings = MappingConfig.getNestedMappings(mappingState.mappingMode, typedKeySequence)
        // show all available nested mappings in a balloon
        if (ideFrame != null && nestedMappings.isNotEmpty()) {
            val frameWidth = ideFrame.width
            val maxString: String = nestedMappings.maxByOrNull { it.length }!! // we have manually checked that nestedMappings is not empty
            /*
             * there might be a better way to measure the pixel width of a given String than using a JLabel
             * so far this method has worked quite well and since I'm missing a better alternative this stays for now
             */
            val maxStringWidth = JLabel(maxString).preferredSize.width
            val possibleColumns = frameWidth / maxStringWidth
            val elementsPerColumn = Math.ceil(nestedMappings.size / possibleColumns.toDouble()).toInt()
            val windowedMappings = nestedMappings.windowed(elementsPerColumn, elementsPerColumn, true)

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

            val fadeoutTime = if (OptionsManager.timeout.value) {
                OptionsManager.timeoutlen.value().toLong()
            } else {
                0L
            }
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

        // continue with the default behavior of IdeaVim
        vimTypedActionHandler.beforeExecute(editor, charTyped, dataContext, plan)
    }
}