package eu.theblob42.idea.whichkey.model

/**
 * @param prefix Indicator if this mapping is a prefix or an actual command
 * @param description A descriptive string for the mapping: category for a prefix ("Files"), descriptive name for a command ("close window") or the presentable String for a command ("<C-w>l")
 */
data class Mapping(val prefix: Boolean, val description: String)