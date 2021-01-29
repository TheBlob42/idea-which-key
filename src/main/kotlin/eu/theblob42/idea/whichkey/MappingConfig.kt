package eu.theblob42.idea.whichkey

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

object MappingConfig {

    private val mappingsPerMode = mutableMapOf<MappingMode, MutableMap<MappingSequence, String>>()
    private val whichKeyDescriptions: List<String>

    init {
        // check for the defined leader key, default is "\"
        // for reference check StringHelper.parseMapLeader(String s)
        val leaderKey = VimScriptGlobalEnvironment.getInstance().variables["mapleader"]?.let {
            when (it) {
                is String -> it.map { keyToString(it, 0, 0) }.joinToString(separator = "")
                else -> "\\"
            }
        } ?: "\\"
        // extract all WhichKey description variables from the '.ideavimrc' file
        whichKeyDescriptions = VimScriptGlobalEnvironment.getInstance().variables.entries
            .asSequence()
            .filter { it.key.startsWith("g:WhichKeyDesc_") }
            .map { it.value.toString() }
            .map { it.replace("<leader>", leaderKey) }
            .toList()

        // retrieve all mappings for all modes and save them
        for (mode in enumValues<MappingMode>()) {
            val keyMapping = VimPlugin.getKey().getKeyMapping(mode)
            for (keyStrokes in keyMapping) {
                // Check for fake <Plug> mappings and ignore them here
                // Check 'VimExtensionFacade.putExtensionHandlerMapping(...)' for more information
                val code = keyStrokes[0].keyCode
                if (code == KeyEvent.CHAR_UNDEFINED.toInt().dec() // VK_PLUG constant is private
                    || code == StringHelper.VK_ACTION) {
                    continue
                }

                val description = keyMapping[keyStrokes]!!.getPresentableString()
                addMapping(mode, keyStrokes, description)
            }
        }
    }

    /**
     * Convert a single key press or combination into an appropriate [String] representation
     *
     * @param keyStroke The pressed key (combination) as [KeyStroke]
     * @return String representation of the pressed key or key combination
     */
    fun keyToString(keyStroke: KeyStroke): String {
        return keyToString(keyStroke.keyChar, keyStroke.keyCode, keyStroke.modifiers)
    }

    /**
     * Convert a single key press or combination into an appropriate [String] representation
     *
     * @param keyChar Character associated with the pressed key
     * @param keyCode The key code of the pressed key
     * @param modifiers The modifier code for the pressed key combination (0 if not applicable)
     * @return String representation of the pressed key or key combination
     */
    fun keyToString(keyChar: Char, keyCode: Int, modifiers: Int): String {
        // special case for " "
        if (keyChar == ' ') {
            return "<Space>"
        }

        if (keyCode == 0) {
            return keyChar.toString()
        }

        val mod = KeyEvent.getKeyModifiersText(modifiers)
        val key = KeyEvent.getKeyText(keyCode).let {
            if (it.length == 1) {
                it.toLowerCase()
            } else {
                it
            }
        }

        if (mod.isNotEmpty()) {
            val modifiedMod = mod
                .replace("+", "-")
                .replace("Alt", "A")
                .replace("Ctrl", "C")
                .replace("Shift", "S")
                .replace("Escape", "Esc")
            return "<${modifiedMod}-${key}>"
        }

        return if (key.length > 1) "<$key>" else key
    }

    /**
     * Add a description for a key sequence
     * Automatically create entries for all prefix sequences (if they do not exist yet)
     * If the description is blank uses a default value
     *
     * @param mode The [MappingMode] to add the sequence to
     * @param keySequence The [List] of [KeyStroke]s representing the sequence
     * @param presentableString The default description for the action to add
     * (only used if there is no custom description in the `.ideavimrc` file)
     */
    private fun addMapping(mode: MappingMode, keySequence: List<KeyStroke>, presentableString: String) {
        val mappings = mappingsPerMode.getOrPut(mode) { mutableMapOf() }

        val tmpSequence = mutableListOf<KeyStroke>()
        for (keyStroke in keySequence) {
            tmpSequence.add(keyStroke)

            val description = getWhichKeyDescription(tmpSequence)
                ?: if (tmpSequence.size == keySequence.size) {
                    // add the description for the last element of the sequence
                    if (presentableString.isNotBlank()) presentableString else "No description"
                } else {
                    "Prefix"
                }
            mappings.putIfAbsent(MappingSequence(tmpSequence.toList()), description)
        }
    }

    /**
     * Check if there is a custom description for the given key stroke sequence defined in the `.ideavimrc`  file
     *
     * @param keySequence A [List] of [KeyStroke]s to check
     * @return Custom description if there is one. In case of more than one custom description
     * in the configuration, log the sequence and return the first value found
     */
    private fun getWhichKeyDescription(keySequence: List<KeyStroke>): String? {
        val sequenceString = keySequence.joinToString(separator = "") { keyToString(it) }
        val sequenceRegex = Regex("${Regex.escape(sequenceString)}[ \\t]+(.*)")

        val filteredDescriptions = whichKeyDescriptions
            .filter { it.matches(sequenceRegex) }
            .mapNotNull {
                sequenceRegex.find(it)?.groupValues?.get(1)
            }

        if (filteredDescriptions.size > 1) {
            WhichKeyExtension.logger.warn("Found more than one custom WhichKey descriptions for sequence: $sequenceString")
        }

        return filteredDescriptions.firstOrNull()
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
     * The actual entries contain the key to press and the corresponding description:
     *
     * `w ⟶ Prefix`
     *
     * @param mode The current [MappingMode]
     * @param keyStrokes The [List] of pressed [KeyStroke]s
     * @return A [List] with all relevant mappings (default: empty list)
     */
    fun getNestedMappings(mode: MappingMode, keyStrokes: List<KeyStroke>): List<String> {
        // search nested mappings for the "exact" key stroke sequence
        val nestedMappings = extractNestedMappings(mode, keyStrokes).toMutableList()

        // check if parts of the typed key sequence map to other key sequences,
        // replace them and search for nested mappings of the resulting sequence
        var replacedKeyStrokes = mutableListOf<KeyStroke>()
        for (keyStroke in keyStrokes) {
            replacedKeyStrokes.add(keyStroke)
            val mapping = VimPlugin.getKey().getKeyMapping(mode)[replacedKeyStrokes]

            if (mapping != null && mapping is ToKeysMappingInfo) {
                replacedKeyStrokes = mapping.toKeys.toMutableList()
            }
        }

        if (replacedKeyStrokes != keyStrokes) {
            nestedMappings.addAll(extractNestedMappings(mode, replacedKeyStrokes))
        }

        // check if the "exact" key stroke sequence maps to another sequence which has nested mappings
        val sequenceMapping = VimPlugin.getKey().getKeyMapping(mode)[keyStrokes]
        if (sequenceMapping != null
            && sequenceMapping is ToKeysMappingInfo
            && sequenceMapping.toKeys != replacedKeyStrokes){
            nestedMappings.addAll(extractNestedMappings(mode, sequenceMapping.toKeys))
        }

        return nestedMappings
            // sort all mappings alphabetically by there key
            .sortedBy { it.first }
            .map { (key, desc) ->
                "${key} -> ${desc}"
                    // escape angle brackets for usage in HTML
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
            }
    }

    /**
     * Helper function to avoid duplicate code within [getNestedMappings]
     *
     * @param mode The current [MappingMode]
     * @param keyStrokes The [List] of pressed [KeyStroke]s
     * @return A [List] of [Pair]<String, String> describing the extracted nested mappings.
     * The first value is the next key to press.
     * The second value is the mapping description.
     */
    private fun extractNestedMappings(mode: MappingMode, keyStrokes: List<KeyStroke>): List<Pair<String, String>> {
        val typedSequence = keyStrokes.joinToString(separator = "") { keyToString(it) }

        return mappingsPerMode[mode]?.entries
            ?.filter {
                // only mappings which start with the same prefix
                val samePrefix = it.key.sequence.startsWith(typedSequence)
                // only mappings which are direct children (length + 1)
                val directChild = it.key.keyStrokes.size == keyStrokes.size.inc()
                // filter for multiple checks at once to avoid unnecessary iterations because of multiple '.filter' calls
                samePrefix && directChild
            }
            ?.map {
                // only display the next key to press, instead of the whole sequence
                val key = it.key.sequence.replaceFirst(typedSequence, "")
                Pair(key, it.value)
            }
            ?: listOf()
    }
}