package eu.theblob42.idea.whichkey

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.command.CommandState
import eu.theblob42.idea.whichkey.config.MappingConfig
import eu.theblob42.idea.whichkey.config.PopupConfig
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class WhichKeyShortcutKeyAction: AnAction(), DumbAware {

    // default IdeaVim shortcut action handler
    private val vimShortcutKeyAction = VimShortcutKeyAction()

    override fun actionPerformed(actionEvent: AnActionEvent) {
        PopupConfig.hidePopup()

        val editor = actionEvent.getData(PlatformDataKeys.EDITOR)
        if (editor != null) {
            val inputEvent = actionEvent.inputEvent
            if (inputEvent is KeyEvent) {
                val startTime = System.currentTimeMillis() // save start time for the popup delay

                val mappingState = CommandState.getInstance(editor).mappingState
                val typedKeySequence = mappingState.keys + listOf(KeyStroke.getKeyStroke(inputEvent.keyCode, inputEvent.modifiersEx))
                val nestedMappings = MappingConfig.getNestedMappings(mappingState.mappingMode, typedKeySequence)
                val window = WindowManager.getInstance().getFrame(editor.project)

                PopupConfig.showPopup(window!!, typedKeySequence, nestedMappings, startTime)
            }
        }

        vimShortcutKeyAction.actionPerformed(actionEvent)
    }

    override fun update(actionEvent: AnActionEvent) {
        vimShortcutKeyAction.update(actionEvent)
    }
}
