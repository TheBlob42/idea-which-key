package eu.theblob42.idea.whichkey

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.wm.WindowManager
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

class WhichKeyActionListener : AnActionListener {
    private var ignoreNextExecute = false

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
            && MappingConfig.isPrefix(mappingMode, vimCurrentKeySequence)
            && !MappingConfig.isAction(mappingMode, vimCurrentKeySequence + escapeKeyStroke)
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
        processKeySequence(editor, typedKeySequence)
    }

    override fun afterEditorTyping(charTyped: Char, dataContext: DataContext) {
        PopupConfig.hidePopup()
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return

        val commandState = CommandState.getInstance(editor)

        val typedKeySequence = commandState.mappingState.keys.toList().ifEmpty { commandState.commandBuilder.keys.toList() }
        processKeySequence(editor, typedKeySequence)
    }

    private fun processKeySequence(editor: Editor, typedKeySequence: List<KeyStroke>) {
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
                ignoreNextExecute = true
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

        if (!MappingConfig.isPrefix(editor.vim.mode.toMappingMode(), typedKeySequence)) {
            return emptyList()
        }

        val nestedMappings = MappingConfig.getNestedMappings(editor.vim.mode.toMappingMode(), typedKeySequence)
        return nestedMappings
    }
}