package eu.theblob42.idea.whichkey.config

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import eu.theblob42.idea.whichkey.model.Mapping
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

object MappingConfig {

    const val DEFAULT_PREFIX_LABEL = "Prefix"

    private const val DEFAULT_LEADER_KEY = "\\"
    private val DESCRIPTION_REGEX = Regex("([^ \\t]+)[ \\t]*(.*)")

    private val processUnknownMappings: Boolean
    get() = when (val option = injector.variableService.getGlobalVariableValue("WhichKey_ProcessUnknownMappings")) {
        null -> true
        is VimString -> option.asString().toBoolean()
        else -> true
    }

    /**
     * All VIM default mappings per [MappingMode].
     * This value only considers mappings which have more than one key stroke
     */
    private val VIM_ACTIONS = mutableMapOf<MappingMode, MutableMap<List<KeyStroke>, String>>()

    /**
     * All keystrokes that start a motion in operator-pending mode
     */
    private val OP_MOTION_KEYSTROKES: Set<KeyStroke>

    init {
        // since the VIM default mappings do not change during runtime we are extracting them once during initialization
        for (mode in enumValues<MappingMode>()) {
            val modeMap = VIM_ACTIONS.getOrPut(mode) { mutableMapOf() }
            injector.keyGroup.getBuiltinCommandsTrie(mode).getEntries()
                .mapNotNull {
                    if (it.data == null) {
                        null
                    }

                    var entry = it
                    val tmpKeyStrokes = mutableListOf(entry.key)
                    // rebuild the keystroke sequence by iterating through the parent nodes
                    // omit the "root node" as it is not related to any actual keystroke
                    while (entry.parent != null && entry.parent!!.parent != null) {
                        entry = entry.parent!!
                        // add parent keystrokes in front, to retain the correct order
                        tmpKeyStrokes.add(0, entry.key)
                    }

                    // we are only interested in VIM actions with more than one keystroke
                    if (tmpKeyStrokes.size == 1) {
                        null
                    }

                    Pair(tmpKeyStrokes.toList(), it.data!!.actionId)
                }
                .forEach {
                    modeMap[it.first] = it.second
                }
        }

        OP_MOTION_KEYSTROKES = injector.keyGroup.getBuiltinCommandsTrie(MappingMode.OP_PENDING)
            .getEntries()
            // we are only interested in motions and text object actions
            .filter { it.data?.instance is MotionActionHandler || it.data?.instance is TextObjectActionHandler }
            .map {
                var entry = it
                // omit the root node as it does not contain any keystroke information
                while (entry.parent != null && entry.parent!!.parent != null) {
                    entry = entry.parent!!
                }
                entry.key
            }
            .toSet()
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
            val mapping = injector.keyGroup.getKeyMapping(mode)[replacedKeyStrokes]

            if (mapping != null
                && mapping is ToKeysMappingInfo
                && mapping.isRecursive) {
                replacedKeyStrokes = mapping.toKeys.toMutableList()
            }
        }

        if (replacedKeyStrokes != keyStrokes) {
            nestedMappings.putAll(extractNestedMappings(mode, replacedKeyStrokes, whichKeyDescriptions))
        }

        // check if the "exact" key stroke sequence maps to another sequence which has nested mappings
        val sequenceMapping = injector.keyGroup.getKeyMapping(mode)[keyStrokes]
        if (sequenceMapping != null
            && sequenceMapping is ToKeysMappingInfo
            && sequenceMapping.isRecursive
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
        val keyMapping = injector.keyGroup.getKeyMapping(mode)
        /*
         * map both key mappings and VIM actions to Pair<List<KeyStroke>, String>
         * then iterate over both combined and check for nested mappings
         */
        val keyMappingPairs = keyMapping
            .filterNotNull()
            // mapping to pairs
            .map {
                Pair(it.toList(), keyMapping[it]?.getPresentableString() ?: "no description")
            }
        // check .ideavimrc if the default VIM actions should be displayed along other mappings (default: false)
        val showVimActions = when (val value = injector.variableService.getGlobalVariableValue("WhichKey_ShowVimActions")) {
            null -> false
            is VimString -> value.asString().toBoolean()
            else -> false
        }
        val vimActionsPairs =
            VIM_ACTIONS[mode]?.entries
                ?.map { it.key to if (showVimActions) it.value else "" }
                ?: listOf()


        val nestedMappings = mutableMapOf<String, Mapping>()
        for ((mappedKeyStrokes, defaultDescription) in (keyMappingPairs + vimActionsPairs)) {
            // Check for "fake" <Plug> mappings and ignore them
            // Check 'VimExtensionFacade.putExtensionHandlerMapping(...)' for more information
            val vkPlug = KeyEvent.CHAR_UNDEFINED.code.dec() // the "original" VK_PLUG constant is private
            val vkAction = KeyEvent.CHAR_UNDEFINED.code - 2 // the "original" VK_ACTION constant is private
            val code = mappedKeyStrokes[0].keyCode
            if (code == vkPlug || code == vkAction) {
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

            // only display mappings that have a non-blank description
            // this allows to "remove" mappings from appearing in the popup by specifying an empty description string
            if (description.isNotBlank()) {
                nestedMappings[nextKey] = Mapping(isPrefix, description)
            }
        }

        return nestedMappings
    }

    /**
     * Check if the given keystrokes represent a valid key mapping (user or VIM internal)
     *
     * @param mode The [MappingMode] to check the keystrokes
     * @param keyStrokes The list of keystrokes to check
     * @return `true` if the sequence represents an actual mapping, `false` if they are just unrelated keystrokes
     */
    fun isMapping(mode: MappingMode, keyStrokes: List<KeyStroke>): Boolean {
        val seq = keyStrokes.joinToString { keyToString(it) }

        // check if there is a custom user mapping for the given keystrokes
        val isCustomMapping = injector.keyGroup.getKeyMapping(mode)
            .asSequence()
            .filter { it.size == keyStrokes.size }
            .map { it.joinToString { keyStroke -> keyToString(keyStroke) } }
            .find { it == seq } != null

        if (isCustomMapping) {
            return true
        }

        // check if there is an VIM "internal" mapping for the given keystrokes
        return (VIM_ACTIONS[mode]?.keys ?: listOf())
            .filter { it.size == keyStrokes.size }
            .map { it.joinToString { keyStroke -> keyToString(keyStroke) } }
            .find { it == seq } != null
    }

    /**
     * Check if we should proceed with the possible unknown mapping or not
     *
     * @param mode The [MappingMode] to check the keystrokes
     * @param keyStrokes The list of keystrokes to check
     * @return `true` if we should process with the mapping, `false` if not
     */
    fun processWithUnknownMapping(mode: MappingMode, keyStrokes: List<KeyStroke>): Boolean {
        // check configurable user setting first
        if (processUnknownMappings) {
            return true
        }

        // single key mappings are always passed
        if (keyStrokes.size == 1) {
            return true
        }

        // maybe the given key sequence is actually a mapping (user or internal)
        if (isMapping(mode, keyStrokes)) {
            return true
        }

        val prefix = keyStrokes.dropLast(1)

        // is this a root key that expects a motion or digraph argument then providing this motion or digraph should not be blocked
        if (prefix.size == 1) {
            val prefixKey = prefix.first()
            val argTypes = injector.keyGroup.getBuiltinCommandsTrie(mode).getEntries()
                .filter { it.key == prefixKey && it.data !== null }
                .map { it.data!!.instance.argumentType }
            if (argTypes.any { it == Argument.Type.MOTION || it == Argument.Type.DIGRAPH }) {
                return true
            }
        }

        // the prefix is a valid custom mapping and the last key is a valid motion
        // therefore we don't want to block the motion here which probably "belongs" to the mapping
        // for example 'ys<motion>' (e.g. `ysiw`) and 'yss' from the builtin surround plugin
        if (isMapping(mode, prefix) && OP_MOTION_KEYSTROKES.contains(keyStrokes.last())) {
            return true
        }

        return false
    }

    /**
     * Extract all custom WhichKey descriptions from the .ideavimrc file and replace "leader" with the actual mapped key
     *
     * @return [Map] of 'key sequence' to 'custom description'
     */
    private fun extractWhichKeyDescriptions(): Map<String, String> {
        return injector.variableService.getGlobalVariables().entries
            .asSequence()
            .filter { it.key.startsWith("WhichKeyDesc_") }
            .map { it.value.asString() }
            .mapNotNull {
                // destructure the regex groups into Pairs
                DESCRIPTION_REGEX.find(it)?.groupValues?.let { (_, keySequence, description) ->
                    val keys = injector.parser.parseKeys(keySequence)
                    Pair(keys.joinToString(separator = "") { keyToString(it) }, description)
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

        if (keyChar == '\\') {
            return "<Bslash>"
        }

        if (keyCode == 0) {
            return keyChar.toString()
        }

        val mod = InputEvent.getModifiersExText(modifiers)
        val key = KeyEvent.getKeyText(keyCode).let {
            if (it.length == 1) {
                it.lowercase()
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