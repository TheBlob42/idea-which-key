package eu.theblob42.idea.whichkey.provider.descriptions

import com.maddyhome.idea.vim.api.injector
import eu.theblob42.idea.whichkey.provider.Description
import eu.theblob42.idea.whichkey.provider.DescriptionProvider
import javax.swing.KeyStroke

class FlatDescriptionProvider(private val prefix: String = "WhichKeyDesc") : DescriptionProvider {
    private val DESCRIPTION_REGEX = Regex("([^ \\t]+)[ \\t]*(.*)")

    override fun getDescriptions(path: List<KeyStroke>): List<Description> {
        val descriptions = injector.variableService.getGlobalVariables().entries
            .asSequence()
            .filter { it.key.startsWith("${prefix}_") }
            .map { it.value.asString() }
            .mapNotNull {
                DESCRIPTION_REGEX.find(it)?.groupValues?.let { (_, keySequence, description) ->
                    val keys = injector.parser.parseKeys(keySequence)
                    if (keys.size - path.size == 1 && keys.zip(path).all { (a, b) -> a == b }) {
                        Description(keys.asReversed().first(), description)
                    } else {
                        null
                    }
                }
            }
        return descriptions.toList()
    }
}