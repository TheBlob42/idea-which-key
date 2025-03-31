package eu.theblob42.idea.whichkey.provider.mappings

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import eu.theblob42.idea.whichkey.provider.Mapping
import eu.theblob42.idea.whichkey.provider.MappingProvider
import javax.swing.KeyStroke

object UserMappingProvider : MappingProvider {
    override fun getMappings(mode: MappingMode, path: List<KeyStroke>): List<Mapping> {
        val mapping = injector.keyGroup.getKeyMapping(mode)
        if (!mapping.isPrefix(path))
            return emptyList()
        return mapping.getAll(path).filter { it.getPath().size > path.size }.groupBy { it.getPath()[path.size] }.map {
            Mapping(it.key, it.value.size > 1, null)
        }
    }
}