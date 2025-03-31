package eu.theblob42.idea.whichkey.provider.mappings

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import eu.theblob42.idea.whichkey.provider.Mapping
import eu.theblob42.idea.whichkey.provider.MappingProvider
import javax.swing.KeyStroke

object VimMappingProvider : MappingProvider {
    override fun getMappings(mode: MappingMode, path: List<KeyStroke>): List<Mapping> {
        val mappings = injector.keyGroup.getBuiltinCommandsTrie(mode)

        if (!mappings.isPrefix(path)) {
            return emptyList()
        }
        return mappings.getEntries(path).map {
            Mapping(it.key, false, it.data?.actionId)
        }.toList()
    }
}