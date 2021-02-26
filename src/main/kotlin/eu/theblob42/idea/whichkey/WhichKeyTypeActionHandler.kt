package eu.theblob42.idea.whichkey

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx
import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandState
import eu.theblob42.idea.whichkey.config.MappingConfig
import eu.theblob42.idea.whichkey.config.PopupConfig
import javax.swing.KeyStroke

class WhichKeyTypeActionHandler(private val vimTypedActionHandler: VimTypedActionHandler): TypedActionHandlerEx {

    override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
        vimTypedActionHandler.execute(editor, charTyped, dataContext)
    }

    override fun beforeExecute(editor: Editor, charTyped: Char, dataContext: DataContext, plan: ActionPlan) {
        PopupConfig.hidePopup()

        val commandState = CommandState.getInstance(editor)
        /*
         * check if the CommandBuilder is waiting for a DIGRAPH argument to the last typed action e.g. f<char>, t<char>, d<motion>
         * in those cases don't show any nested mappings as these keys are handled directly by their corresponding actions instead
         *
         * for example pressing "fg" should not show any nested mappings for "g" as the key press for "g" was already handled
         * and therefore any subsequent key press would not execute any of the actions presented in the popup
         */
        if (commandState.commandBuilder.expectedArgumentType != Argument.Type.DIGRAPH) {
            val ideFrame = WindowManager.getInstance().getFrame(editor.project)
            if (ideFrame != null) {
                val mappingState = commandState.mappingState
                val typedKeySequence = mappingState.keys + listOf(KeyStroke.getKeyStroke(charTyped))
                val nestedMappings = MappingConfig.getNestedMappings(mappingState.mappingMode, typedKeySequence)

                PopupConfig.showPopup(ideFrame, typedKeySequence, nestedMappings)
            }
        }

        // continue with the default behavior of IdeaVim
        vimTypedActionHandler.beforeExecute(editor, charTyped, dataContext, plan)
    }
}