package eu.theblob42.idea.whichkey

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.*
import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionAccessScope
import eu.theblob42.idea.whichkey.config.MappingConfig
import eu.theblob42.idea.whichkey.config.PopupConfig
import eu.theblob42.idea.whichkey.model.Mapping
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class BlockNextTypedActionHandler(private val originalHandler: VimTypedActionHandler) : TypedActionHandlerEx {
    override fun execute(editor: Editor, char: Char, dataContext: DataContext) {
        // do nothing then restore the original handler
        TypedAction.getInstance().setupRawHandler(originalHandler)
        return
    }

    override fun beforeExecute(editor: Editor, char: Char, dataContext: DataContext, plan: ActionPlan) {
        return
    }
}

class WhichKeyActionListener : AnActionListener {
    private var blockNextAction: Boolean = false

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        if (blockNextAction) {
            blockNextAction = false

            val dataContext = event.dataContext
            val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return

            KeyHandler.getInstance().handleKey(
                editor.vim,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK),
                dataContext.vim,
                KeyHandler.getInstance().keyHandlerState)
        }
    }

    override fun beforeShortcutTriggered(
        shortcut: Shortcut,
        actions: MutableList<AnAction>,
        dataContext: DataContext
    ) {
        PopupConfig.hidePopup()
        if (shortcut !is KeyboardShortcut) {
            return
        }

        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val commandState = CommandState.getInstance(editor)
        val mappingState = commandState.mappingState
        val escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
        val mappingMode = editor.vim.mode.toMappingMode()
        val vimCurrentKeySequence = commandState.mappingState.keys.toList().ifEmpty { commandState.commandBuilder.keys.toList() }

        // check if user pressed escape and we are in the middle of a sequence
        // and ESC is not a possible next element in the sequence.
        // If this is the case, treat it as 'cancel'
        if (listOfNotNull(shortcut.firstKeyStroke, shortcut.secondKeyStroke) == listOf(escapeKeyStroke)
            && MappingConfig.getNestedMappings(mappingMode, vimCurrentKeySequence).isNotEmpty()
            && MappingConfig.getNestedMappings(mappingMode, vimCurrentKeySequence + escapeKeyStroke).isEmpty()
        ) {
            mappingState.resetMappingSequence()
            return
        }

        // this event fires BEFORE handling by ideavim, so we need to construct the sequence ourselves
        val typedKeySequence =
            vimCurrentKeySequence + listOfNotNull(shortcut.firstKeyStroke, shortcut.secondKeyStroke)
        // SPACE registers both as a shortcut as well as "typed space" event, so ignore the shortcut variant
        if (typedKeySequence == listOf(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0))) {
            return
        }
        processKeySequence(editor, typedKeySequence, true)
    }

    override fun beforeEditorTyping(charTyped: Char, dataContext: DataContext) {
        PopupConfig.hidePopup()
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return

        val commandState = CommandState.getInstance(editor)

        val typedKeySequence = commandState.mappingState.keys.toList().ifEmpty { commandState.commandBuilder.keys.toList() } + listOf(KeyStroke.getKeyStroke(charTyped))
        processKeySequence(editor, typedKeySequence, false)
    }

    private fun processKeySequence(editor: Editor, typedKeySequence: List<KeyStroke>, isShortcut: Boolean) {
        val vimEditor = editor.vim
        val startTime = System.currentTimeMillis() // save start time for the popup delay

        val mappingMode = vimEditor.mode.toMappingMode()
        val mappingState = CommandState.getInstance(editor).mappingState
        val nestedMappings = getMappingsToDisplay(editor, typedKeySequence)
        val window = WindowManager.getInstance().getFrame(editor.project)

        if (nestedMappings.isEmpty()) {
            if (mappingMode != MappingMode.INSERT
                && !MappingConfig.processWithUnknownMapping(mappingMode, typedKeySequence)
            ) {
                // reset the mapping state, do not open a popup & ignore the next call to `update`
                mappingState.resetMappingSequence()

                if (isShortcut) {
                    // this is a hack to block the next action :-)
                    // we are switching to the command line by pressing ':'
                    // then we use CTRL-V to insert the next key literally
                    // in the afterActionPerformed method we will leave the command line via CTRL-C
                    mappingState.addKey(KeyStroke.getKeyStroke(':'))
                    mappingState.addKey(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK))
                    blockNextAction = true
                } else {
                    // block the next typing by using a custom raw handler which will be reset afterwards
                    val typedAction = TypedAction.getInstance()
                    val rawTypedActionHandler = typedAction.rawHandler as VimTypedActionHandler
                    typedAction.setupRawHandler(BlockNextTypedActionHandler(rawTypedActionHandler))
                }
            }
        } else {
            PopupConfig.showPopup(window!!, typedKeySequence, nestedMappings, startTime)
        }
    }

    private fun getMappingsToDisplay(editor: Editor, typedKeySequence: List<KeyStroke>): List<Pair<String, Mapping>> {
        if (typedKeySequence.isEmpty()) {
            return emptyList()
        }
        val whichKeyOption = injector.optionGroup.getOption("which-key") ?: return emptyList()
        if (!injector.optionGroup.getOptionValue(whichKeyOption, OptionAccessScope.EFFECTIVE(editor.vim)).asBoolean()) {
            return emptyList()
        }
        /*
         * check if the CommandBuilder is waiting for a DIGRAPH argument to the last typed action e.g. f<char>, t<char>, d<motion>
         * in those cases don't show any nested mappings as these keys are handled directly by their corresponding actions instead
         *
         * for example pressing "fg" should not show any nested mappings for "g" as the key press for "g" was already handled
         * and therefore any subsequent key press would not execute any of the actions presented in the popup
         */
        if (CommandState.getInstance(editor).commandBuilder.expectedArgumentType == Argument.Type.DIGRAPH) {
            return emptyList()
        }

        val nestedMappings = MappingConfig.getNestedMappings(editor.vim.mode.toMappingMode(), typedKeySequence)
        return nestedMappings
    }
}
