package eu.theblob42.idea.whichkey

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.option.OptionsManager

class WhichKeyTypeActionHandler(private val vimTypedActionHandler: VimTypedActionHandler): TypedActionHandlerEx {

    override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
        vimTypedActionHandler.execute(editor, charTyped, dataContext)
    }

    override fun beforeExecute(editor: Editor, charTyped: Char, dataContext: DataContext, plan: ActionPlan) {
        val mappingState = CommandState.getInstance(editor).mappingState

        // retrieve previously typed keys
        val typedKeysStringBuilder = StringBuilder()
        mappingState.keys.forEach { typedKeysStringBuilder.append(it) }
        // append the last typed character
        typedKeysStringBuilder.append(charTyped)
        val typedSequence = typedKeysStringBuilder.toString()
            .replace("typed ", "") // remove prefix

        val nestedMappings = MappingConfig.getNestedMappings(mappingState.mappingMode, typedSequence)
        // show all available nested mappings in a balloon
        if (nestedMappings.isNotEmpty()) {
            val text = nestedMappings.joinToString(separator = "\n")
            val fadeoutTime = if (OptionsManager.timeout.value) {
                OptionsManager.timeoutlen.value().toLong()
            } else {
                0L
            }
            val target = RelativePoint.getSouthWestOf(WindowManager.getInstance().getFrame(editor.project)!!.rootPane)

            JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, MessageType.INFO, null)
                .setFadeoutTime(fadeoutTime)
                .createBalloon()
                .show(target, Balloon.Position.above)
        }

        // continue with the default behavior of IdeaVim
        vimTypedActionHandler.beforeExecute(editor, charTyped, dataContext, plan)
    }
}