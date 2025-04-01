package eu.theblob42.idea.whichkey.provider

import javax.swing.KeyStroke

interface DescriptionProvider {
    fun getDescriptions(path: List<KeyStroke>): List<Description>
}

