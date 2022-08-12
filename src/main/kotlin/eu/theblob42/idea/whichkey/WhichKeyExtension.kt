package eu.theblob42.idea.whichkey

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import java.util.*

class WhichKeyExtension: VimExtension {

    companion object {
        val logger = logger<WhichKeyExtension>()
    }

    override fun getName() = "which-key"

    override fun init() {
        logger.debug("init IDEA Which-Key extension")

        /*
         * We have to exchange the 'raw' typing handler which is set by IdeaVim with our own in order to intercept all keystrokes
         * Unfortunately during the VIM extension init process the IdeaVim handler hasn't been set yet
         * To ensure the handler is replaced as soon as possible (before the project has loaded completed) we use the following Timer
         *
         * Furthermore, we also have to replace the 'VimShortcutKeyAction' of IdeaVim with our own implementation
         * This is needed to intercept modified (e.g. Ctrl + W) or special key strokes (e.g. Escape) properly
         */
        val typedAction = TypedAction.getInstance()
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                // assign to local val for the type check and smart casting to work properly
                val rawTypedActionHandler = typedAction.rawHandler
                if (rawTypedActionHandler is VimTypedActionHandler) {
                    logger.debug("replacing IdeaVim type handler")
                    typedAction.setupRawHandler(WhichKeyTypeActionHandler(rawTypedActionHandler))

                    logger.debug("replacing IdeaVim 'VimShortcutKeyAction'")
                    ActionManager.getInstance().replaceAction("VimShortcutKeyAction", WhichKeyShortcutKeyAction())
                    timer.cancel()
                }
            }
        }, 0L, 500L)
    }

}