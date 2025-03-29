package eu.theblob42.idea.whichkey.provider.descriptions

import eu.theblob42.idea.whichkey.provider.Description
import eu.theblob42.idea.whichkey.provider.DescriptionProvider
import javax.swing.KeyStroke

class CombinedDescriptionProvider(vararg val providers: DescriptionProvider) : DescriptionProvider {
    override fun getDescriptions(path: List<KeyStroke>): List<Description> {
        return providers
            .flatMap { it.getDescriptions(path) }
            .asReversed()
    }
}