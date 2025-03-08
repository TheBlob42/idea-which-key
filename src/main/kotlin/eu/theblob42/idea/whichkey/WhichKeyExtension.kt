package eu.theblob42.idea.whichkey

import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.maddyhome.idea.vim.extension.VimExtension

class WhichKeyExtension : VimExtension {
    companion object {
        val logger = logger<WhichKeyExtension>()
    }

    override fun getName() = "which-key"

    override fun init() {
        logger.debug("init IDEA Which-Key extension")

        ApplicationManager.getApplication()
            .messageBus.connect(WhichKeyPluginDisposable.instance)
            .subscribe(AnActionListener.TOPIC, WhichKeyActionListener())
    }

}