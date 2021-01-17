package eu.theblob42.idea.whichkey

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx
import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.command.CommandState
import javax.swing.KeyStroke

class WhichKeyTypeActionHandler(private val vimTypedActionHandler: VimTypedActionHandler): TypedActionHandlerEx {

    override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
        vimTypedActionHandler.execute(editor, charTyped, dataContext)
    }

    override fun beforeExecute(editor: Editor, charTyped: Char, dataContext: DataContext, plan: ActionPlan) {
        PopupConfig.hidePopup()


        val ideFrame = WindowManager.getInstance().getFrame(editor.project)
        if (ideFrame != null) {
            val mappingState = CommandState.getInstance(editor).mappingState
            val typedKeySequence = mappingState.keys + listOf(KeyStroke.getKeyStroke(charTyped))
            val nestedMappings = MappingConfig.getNestedMappings(mappingState.mappingMode, typedKeySequence)

            PopupConfig.showPopup(ideFrame, nestedMappings)
        }

        // continue with the default behavior of IdeaVim
        vimTypedActionHandler.beforeExecute(editor, charTyped, dataContext, plan)
    }
}