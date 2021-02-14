package eu.theblob42.idea.whichkey.config

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import eu.theblob42.idea.whichkey.model.Mapping
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

object MappingConfig {

    private const val DEFAULT_LEADER_KEY = "\\"
    private val DESCRIPTION_REGEX = Regex("(.*?)[ \\t]+(.*)")


    /**
     * Return all nested mappings which are direct children of the given key sequence
     * The result are [Pair]s of the next key to press and a corresponding description
     *
     * Consider the following mappings:
     *
     * - `nnoremap ggab :action 1`
     * - `nnoremap ggc  :action 2`
     *
     * Calling [getNestedMappings] with the keystroke sequence "gg" would return:
     *
     * - "a" -> `prefix`
     * - "c" -> :action 2
     *
     * @param mode The corresponding [MappingMode]
     * @param keyStrokes The [List] of typed [KeyStroke]s
     * @return A [List] of [Pair]s with the next key to press and the corresponding [Mapping] (default: empty list)
     */
    fun getNestedMappings(mode: MappingMode, keyStrokes: List<KeyStroke>): List<Pair<String, Mapping>> {
        /*
         * extract all custom WhichKey descriptions from the .ideavimrc file
         * replace <leader> with the actual mapped value
         */
        val leaderKey = when (val leader = VimScriptGlobalEnvironment.getInstance().variables["mapleader"]) {
            null -> DEFAULT_LEADER_KEY
            is String -> leader.map { keyToString(it, 0, 0) }.joinToString(separator = "")
            else -> DEFAULT_LEADER_KEY
        }
        val whichKeyDescriptions = VimScriptGlobalEnvironment.getInstance().variables.entries
            .asSequence()
            .filter { it.key.startsWith("g:WhichKeyDesc_") }
            .map { it.value.toString() }
            .map { it.replace("<leader>", leaderKey) }
            .mapNotNull {
                DESCRIPTION_REGEX.find(it)?.groupValues?.let { groups ->
                    Pair(groups[1], groups[2])
                }
            }
            .toMap()

        // we use a Map to make sure every key is unique in the result
        val nestedMappings = mutableMapOf<String, Mapping>()

        // check mappings for the exact key sequence
        nestedMappings.putAll(extractNestedMappings(mode, keyStrokes, whichKeyDescriptions))

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
            nestedMappings.putAll(extractNestedMappings(mode, replacedKeyStrokes, whichKeyDescriptions))
        }

        // check if the "exact" key stroke sequence maps to another sequence which has nested mappings
        val sequenceMapping = VimPlugin.getKey().getKeyMapping(mode)[keyStrokes]
        if (sequenceMapping != null
            && sequenceMapping is ToKeysMappingInfo
            && sequenceMapping.toKeys != replacedKeyStrokes){
            nestedMappings.putAll(extractNestedMappings(mode, sequenceMapping.toKeys,whichKeyDescriptions))
        }

        return nestedMappings.map {
            Pair(it.key, it.value)
        }
    }

    /**
     * Helper function to avoid duplicate code within [getNestedMappings]
     *
     * @param mode The [MappingMode]
     * @param keyStrokes The [List] of [KeyStroke]s to check for
     * @param whichKeyDescriptions All custom descriptions from the .ideavimrc file
     * @return A [Map] combining the next keys to press with their corresponding [Mapping]s
     */
    private fun extractNestedMappings(mode: MappingMode, keyStrokes: List<KeyStroke>, whichKeyDescriptions: Map<String, String>): Map<String, Mapping> {
        val keyMappings = VimPlugin.getKey().getKeyMapping(mode)
        val nestedMappings = mutableMapOf<String, Mapping>()
        for (mappedKeyStrokes in keyMappings) {
            // Check for "fake" <Plug> mappings and ignore them
            // Check 'VimExtensionFacade.putExtensionHandlerMapping(...)' for more information
            val vkPlug = KeyEvent.CHAR_UNDEFINED.toInt().dec() // the "original" VK_PLUG constant is private
            val code = mappedKeyStrokes[0].keyCode
            if (code == vkPlug || code == StringHelper.VK_ACTION) {
                continue
            }

            // only consider sequences that are longer than the typed sequence
            if (mappedKeyStrokes.size <= keyStrokes.size) {
                continue
            }

            // only consider sequences which start with the typed sequence
            if (mappedKeyStrokes.subList(0, keyStrokes.size) != keyStrokes) {
                continue
            }

            // if there is already an entry for the next key, skip the rest
            val nextKey = keyToString(mappedKeyStrokes[keyStrokes.size])
            if (nestedMappings[nextKey] != null) {
                continue
            }

            // check if there is a custom description for the next key press
            val sequenceString = mappedKeyStrokes.subList(0, keyStrokes.size.inc()).joinToString(separator = "") {
                keyToString(it)
            }
            val isPrefix = mappedKeyStrokes.size > keyStrokes.size.inc()
            val description = whichKeyDescriptions[sequenceString]
                ?: if (isPrefix) "Prefix" else keyMappings[mappedKeyStrokes]!!.getPresentableString()

            nestedMappings[nextKey] = Mapping(isPrefix, description)
        }

        return nestedMappings
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
    private fun keyToString(keyChar: Char, keyCode: Int, modifiers: Int): String {
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
}