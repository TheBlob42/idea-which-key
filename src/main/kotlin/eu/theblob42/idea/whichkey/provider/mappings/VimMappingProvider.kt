package eu.theblob42.idea.whichkey.provider.mappings

import com.maddyhome.idea.vim.action.change.LazyVimCommand
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.CommandNode
import com.maddyhome.idea.vim.key.CommandPartNode
import eu.theblob42.idea.whichkey.provider.Mapping
import eu.theblob42.idea.whichkey.provider.MappingProvider
import eu.theblob42.idea.whichkey.util.getPath
import javax.swing.KeyStroke

object VimMappingProvider : MappingProvider {
    override fun getMappings(mode: MappingMode, path: List<KeyStroke>): List<Mapping> {
        val root = injector.keyGroup.getKeyRoot(mode)
        return when (val target = root.getPath(path)) {
            is CommandPartNode<LazyVimCommand> -> {
                target.entries.map { (key, value) ->
                    when (value) {
                        is CommandNode<LazyVimCommand> ->
                            Mapping(key, false, value.actionHolder.actionId)

                        else -> Mapping(key, true, null)
                    }

                }
            }

            else -> emptyList()
        }
    }
}