package eu.theblob42.idea.whichkey.provider.descriptions

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import eu.theblob42.idea.whichkey.provider.Description
import eu.theblob42.idea.whichkey.provider.DescriptionProvider
import javax.swing.KeyStroke

class DictionaryDescriptionProvider(private val variableName: String) : DescriptionProvider {
    private tailrec fun getPath(dict: VimDictionary, path: List<KeyStroke>): VimDataType? {
        if (path.isEmpty()) return dict
        val head = path.first()
        val tail = path.drop(1)
        return when (val child =
            dict.dictionary.entries.find { injector.parser.parseKeys(it.key.value) == listOf(head) }?.value) {
            is VimDictionary -> getPath(child, tail)
            else -> child.takeIf { tail.isEmpty() }
        }
    }

    override fun getDescriptions(path: List<KeyStroke>): List<Description> {
        val allDescriptions = injector.variableService.getGlobalVariableValue(variableName)
        if (allDescriptions == null || allDescriptions !is VimDictionary) return emptyList()
        return when (val nodeDescriptions = getPath(allDescriptions, path)) {
            is VimDictionary -> dictToDescriptions(nodeDescriptions)
            is VimList -> when (val lastEntry = nodeDescriptions.values.lastOrNull()) {
                is VimDictionary -> dictToDescriptions(lastEntry)
                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun dictToDescriptions(nodeDescriptions: VimDictionary): List<Description> {
        return nodeDescriptions.dictionary.entries.mapNotNull { entry ->
            val key = injector.parser.parseKeys(entry.key.value).first()
            when (val value = entry.value) {
                is VimDictionary -> Description(key, null)
                is VimList -> Description(key, value.values.firstOrNull()?.asString())
                is VimString -> Description(key, value.value)
                else -> null
            }
        }
    }
}