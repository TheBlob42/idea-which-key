package eu.theblob42.idea.whichkey.provider

import javax.swing.KeyStroke

data class Mapping(val keyStroke: KeyStroke, val isPrefix: Boolean, val defaultDescription: String?)