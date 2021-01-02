package eu.theblob42.idea.whichkey

import com.intellij.openapi.editor.actionSystem.TypedAction
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import java.util.*

class WhichKeyExtension: VimExtension {

    companion object {
        val logger: Logger = LogManager.getLogger(WhichKeyExtension::class.java)
    }

    override fun getName() = "which-key"

    override fun init() {
        logger.debug("init IDEA Which-Key extension")

        /*
         * We have to exchange the 'raw' typing handler which is set by IdeaVim with our own in order to intercept all key strokes
         * Unfortunately during the VIM extension init process the IdeaVim handler hasn't been set yet
         * To ensure the handler is replaced as soon as possible (before the project has loaded completed) we use the following Timer
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
                    timer.cancel()
                }
            }
        }, 0L, 500L)
    }

}