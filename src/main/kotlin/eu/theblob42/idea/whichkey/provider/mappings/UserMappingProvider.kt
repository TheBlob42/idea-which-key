package eu.theblob42.idea.whichkey.provider.mappings

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.*
import eu.theblob42.idea.whichkey.provider.Mapping
import eu.theblob42.idea.whichkey.provider.MappingProvider
import eu.theblob42.idea.whichkey.util.getPath
import javax.swing.KeyStroke

object UserMappingProvider : MappingProvider {
    override fun getMappings(mode: MappingMode, path: List<KeyStroke>): List<Mapping> {
        val mappings = injector.keyGroup.getKeyMapping(mode)
        if (!mappings.isPrefix(path)) {
            return emptyList()
        }
        val root = RootNode<MappingInfo>().apply {
            for (ks in mappings) {
                val mappingInfo = mappings[ks] ?: continue
                addLeafs(ks, mappingInfo)
            }
        }
        return when (val entry = root.getPath(path)) {
            is CommandPartNode<MappingInfo> -> entry.entries.map { (key, value) ->
                when(value) {
                    is CommandNode<MappingInfo> ->
                        Mapping(key, false, value.actionHolder.getPresentableString())
                    else -> Mapping(key, true, null)
                }
            }

            else -> emptyList()
        }
    }
}