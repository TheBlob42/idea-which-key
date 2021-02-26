package eu.theblob42.idea.whichkey.config

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.handler.ActionBeanClass
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.key.CommandNode
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import eu.theblob42.idea.whichkey.model.Mapping
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

object MappingConfig {

    const val DEFAULT_PREFIX_LABEL = "Prefix"

    private const val DEFAULT_LEADER_KEY = "\\"
    private val DESCRIPTION_REGEX = Regex("(.*?)[ \\t]+(.*)")

    /**
     * All VIM default mappings per [MappingMode].
     * This value only considers mappings which have more than one key stroke
     */
    private val VIM_ACTIONS = mutableMapOf<MappingMode, MutableMap<List<KeyStroke>, String>>()

    init {
        // since the VIM default mappings do not change during runtime we are extracting them once during initialization
        for (mode in enumValues<MappingMode>()) {
            val modeMap = VIM_ACTIONS.getOrPut(mode) { mutableMapOf() }
            VimPlugin.getKey().getKeyRoot(mode)
                // we are only interested in VIM actions with more than one key stroke
                .filter {
                    it.value is CommandPartNode
                }
                .forEach {
                    extractVimActions(modeMap, listOf(it.key), it.value)
                }
        }
    }

    /**
     * Extract the given VIM default mapping into a more usable format
     * This function walks the tree of [Node]s and writes all found mappings along the way in to the given [vimActionsMap]
     * The function itself does not have a return value
     *
     * @param vimActionsMap The (mutable) map which should be filled with the results
     * @param keyStrokes The [KeyStroke]s related to the given [node]
     * @param node The current mapping node
     */
    private fun extractVimActions(vimActionsMap: MutableMap<List<KeyStroke>, String>, keyStrokes: List<KeyStroke>, node: Node<ActionBeanClass>) {
        if (node is CommandPartNode<ActionBeanClass>) {
            node.map {
                val keys = keyStrokes + listOf(it.key)
                extractVimActions(vimActionsMap, keys, it.value)
            }
            return
        }

        val actionId = (node as CommandNode<ActionBeanClass>).actionHolder.actionId
        vimActionsMap[keyStrokes] = actionId
    }


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
        val whichKeyDescriptions = extractWhichKeyDescriptions()

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
        val keyMapping = VimPlugin.getKey().getKeyMapping(mode)
        /*
         * map both key mappings and VIM actions to Pair<List<KeyStroke>, String>
         * then iterate over both combined and check for nested mappings
         */
        val keyMappingPairs = keyMapping.map {
            Pair(it.toList(), keyMapping[it]?.getPresentableString() ?: "no description")
        }
        // check .ideavimrc if the default VIM actions should be displayed along other mappings (default: false)
        val showVimActions = when (val value = VimScriptGlobalEnvironment.getInstance().variables["g:WhichKey_ShowVimActions"]) {
            null -> false
            is String -> value.toBoolean()
            else -> false
        }
        val vimActionsPairs = if (showVimActions) {
                VIM_ACTIONS[mode]?.entries
                    ?.map { it.toPair() }
                    ?: listOf()
        } else listOf()

        val nestedMappings = mutableMapOf<String, Mapping>()
        for ((mappedKeyStrokes, defaultDescription) in (keyMappingPairs + vimActionsPairs)) {
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
                ?: if (isPrefix) DEFAULT_PREFIX_LABEL else defaultDescription

            nestedMappings[nextKey] = Mapping(isPrefix, description)
        }

        return nestedMappings
    }

    /**
     * Extract all custom WhichKey descriptions from the .ideavimrc file and replace "leader" with the actual mapped key
     *
     * @return [Map] of 'key sequence' to 'custom description'
     */
    private fun extractWhichKeyDescriptions(): Map<String, String> {
        val leaderKey = when (val leader = VimScriptGlobalEnvironment.getInstance().variables["mapleader"]) {
            null -> DEFAULT_LEADER_KEY
            is String -> leader.map { keyToString(it, 0, 0) }.joinToString(separator = "")
            else -> DEFAULT_LEADER_KEY
        }
        return VimScriptGlobalEnvironment.getInstance().variables.entries
            .asSequence()
            .filter { it.key.startsWith("g:WhichKeyDesc_") }
            .map { it.value.toString() }
            .map { it.replace("<leader>", leaderKey) }
            .mapNotNull {
                // destructure the regex groups into Pairs
                DESCRIPTION_REGEX.find(it)?.groupValues?.let { (_, keySequence, description) ->
                    Pair(keySequence, description)
                }
            }
            .toMap()
    }

    /**
     * Return the custom description for the given [keyStrokes] (if any exists)
     *
     * This function is currently only called from outside of [MappingConfig]
     *
     * @param keyStrokes The key strokes to check
     * @return The corresponding custom description (if any exists)
     */
    fun getWhichKeyDescription(keyStrokes: List<KeyStroke>): String? {
        val whichKeyDescriptions = extractWhichKeyDescriptions()
        val keySequence = keyStrokes.joinToString(separator = "") { keyToString(it) }
        return whichKeyDescriptions[keySequence]
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