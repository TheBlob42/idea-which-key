package eu.theblob42.idea.whichkey

import javax.swing.KeyStroke

/**
 * Represent a sequence of [KeyStroke]s used for mapping.
 * Provide a formatted [String] representation to display the sequence.
 *
 * Also implement custom comparison and hash functions so that entities are correctly stored e.g. in a [HashMap].
 *
 * @param keyStrokes The [KeyStroke]s which represent the mapped sequence
 */
class MappingSequence(val keyStrokes: List<KeyStroke>) {

    // string representation of this KeyStrokes sequence
    val sequence = keyStrokes.joinToString(separator = "") { MappingConfig.keyToString(it) }

    // convert each KeyStroke and then join them with a "custom" separator before hashing
    // this way we can differentiate between sequences of characters and the String representation of special chars
    // e.g. "<Tab>" could be one single "Tab" character or the sequence "<", "T", "a", "b", ">"
    private val hashCode = keyStrokes.joinToString(separator = "&&&&&") { MappingConfig.keyToString(it) }.hashCode()

    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            !is MappingSequence -> false
            else -> other.hashCode == hashCode
        }
    }

    override fun hashCode(): Int {
        return hashCode
    }
}
