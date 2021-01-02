package eu.theblob42.idea.whichkey

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import java.lang.StringBuilder

object MappingConfig {

    private val mappingsPerMode = mutableMapOf<MappingMode, MutableMap<String, String>>()

    init {
        // retrieve all mappings for all modes and save them within this config
        for (mode in enumValues<MappingMode>()) {
            val keyMapping = VimPlugin.getKey().getKeyMapping(mode)
            for (keyStrokes in keyMapping) {
                val keySequence = keyStrokes
                    .map { it.keyChar }
                    .joinToString(separator = "") { it.toString() }
                val description = keyMapping[keyStrokes]!!.getPresentableString()
                addMapping(mode, keySequence, description)
            }
        }
    }

    /**
     * Add a description for a key sequence
     * Automatically create entries for all prefix sequences (if they do not exist yet)
     * If the description is blank uses a default value
     */
    private fun addMapping(mode: MappingMode, keySequence: CharSequence, description: String) {
        val mappings = mappingsPerMode.getOrPut(mode) { mutableMapOf() }

        val sequenceBuilder = StringBuilder()
        for (key in keySequence) {
            sequenceBuilder.append(key)
            // create non existing prefixes with default description
            mappings.putIfAbsent(sequenceBuilder.toString(), "Prefix")
        }
        // add passed description to last entry
        mappings[sequenceBuilder.toString()] = if (description.isNotBlank()) description else "No description"
    }

    /**
     * Return all nested mappings which are direct children of the given prefix key sequence
     *
     * Consider the following sequences are mapped (for `MappingMode.NORMAL`):
     *
     * `g, gw, gws, gb, gbb, gbd, gq`
     *
     * The call `getNestedMappings(MappingMode.NORMAL, "g")` would return the following sequences:
     *
     * `gw, gb, gq`
     *
     * @return A [List] with all relevant mappings (default: empty list)
     */
    fun getNestedMappings(mode: MappingMode, keySequence: CharSequence): List<String> {
        return mappingsPerMode[mode]?.entries
            // only consider mappings which could be direct children (length + 1)
            ?.filter { it.key.length == keySequence.length + 1}
            ?.filter { it.key.startsWith(keySequence) }
            ?.map { "${it.key} - ${it.value}" }
            ?: listOf()
    }

}