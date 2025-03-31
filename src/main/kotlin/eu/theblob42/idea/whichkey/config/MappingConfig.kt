package eu.theblob42.idea.whichkey.config

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.*
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import eu.theblob42.idea.whichkey.model.Mapping
import eu.theblob42.idea.whichkey.provider.descriptions.CombinedDescriptionProvider
import eu.theblob42.idea.whichkey.provider.descriptions.DefaultDescriptionProvider
import eu.theblob42.idea.whichkey.provider.descriptions.DictionaryDescriptionProvider
import eu.theblob42.idea.whichkey.provider.descriptions.FlatDescriptionProvider
import eu.theblob42.idea.whichkey.provider.mappings.UserMappingProvider
import eu.theblob42.idea.whichkey.provider.mappings.VimMappingProvider
import javax.swing.KeyStroke

object MappingConfig {
    const val DEFAULT_PREFIX_LABEL = "Prefix"
    private val descriptionProvider = CombinedDescriptionProvider(
        DefaultDescriptionProvider,
        FlatDescriptionProvider("WhichKeyDesc"),
        DictionaryDescriptionProvider("WhichKeyDesc")
    )

    private val processUnknownMappings: Boolean
        get() = when (val option = injector.variableService.getGlobalVariableValue("WhichKey_ProcessUnknownMappings")) {
            null -> true
            is VimString -> option.asString().toBoolean()
            else -> true
        }

    // check .ideavimrc if the default VIM actions should be displayed along other mappings (default: false)
    private val showVimActions: Boolean
        get() = when (val option = injector.variableService.getGlobalVariableValue("WhichKey_ShowVimActions")) {
            null -> false
            is VimString -> option.asString().toBoolean()
            else -> false
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
        val whichKeyDescriptions = descriptionProvider.getDescriptions(keyStrokes)
        fun getDescription(keyStroke: KeyStroke): String? {
            return whichKeyDescriptions.find { it.keyStroke == keyStroke }?.description
        }

        val mappings =
            VimMappingProvider.getMappings(mode, keyStrokes)
                .filter { showVimActions || getDescription(it.keyStroke) != null } +
                    UserMappingProvider.getMappings(mode, keyStrokes)

        return mappings.associate {
            keyToString(it.keyStroke) to Mapping(
                it.isPrefix,
                getDescription(it.keyStroke) ?: it.defaultDescription ?: injector.parser.toKeyNotation(it.keyStroke)
            )
        }.toList()
    }

    /**
     * Check if the given keystrokes represent a valid key mapping (user or VIM internal)
     *
     * @param mode The [MappingMode] to check the keystrokes
     * @param keyStrokes The list of keystrokes to check
     * @return `true` if the sequence represents an actual mapping, `false` if they are just unrelated keystrokes
     */
    fun isAction(mode: MappingMode, keyStrokes: List<KeyStroke>): Boolean {
        val root = injector.keyGroup.getBuiltinCommandsTrie(mode)
        if (root.getData(keyStrokes) != null) {
            return true
        }

        return injector.keyGroup.getKeyMapping(mode)[keyStrokes] != null
    }

    /**
     * Check if the given keystrokes represent the beginning of a valid key mapping (user or VIM internal)
     *
     * @param mode The [MappingMode] to check the keystrokes
     * @param keyStrokes The list of keystrokes to check
     * @return `true` if the sequence represents a mapping prefix, `false` if they are just unrelated keystrokes
     */
    fun isPrefix(mode: MappingMode, keyStrokes: List<KeyStroke>): Boolean {
        return injector.keyGroup.getBuiltinCommandsTrie(mode).isPrefix(keyStrokes) ||
                injector.keyGroup.getKeyMapping(mode).isPrefix(keyStrokes)
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
        if (isAction(mode, keyStrokes)) {
            return true
        }

        // check if the given sequence might be an operator mapping
        if (isAction(MappingMode.OP_PENDING, keyStrokes)) {
            return true
        }

        val node = injector.keyGroup.getKeyRoot(mode)
        val prefix = keyStrokes.dropLast(1)

        // check for a motion argument, even if the new key is not a valid motion its action would be blocked by default
        if (node.any {
            it.key == prefix.last()
                    && it.value is CommandNode
                    && (it.value as CommandNode).actionHolder.instance.argumentType == Argument.Type.MOTION
        }) {
            return true
        }

        val operatorNode = injector.keyGroup.getKeyRoot(MappingMode.OP_PENDING)

        // NOTE: there might be cases were this will lead to false positives (I have no idea tbh @_@) but it fixes the issue with the builtin surround extension (at least for now)
        // the prefix is a valid mapping and the last key is a valid motion, so we don't want to block the motion here which probably "belongs" to the mapping
        // for example: 'ys<motion>' and 'yss' (builtin surround) and pressing 'ysiw'
        if (isAction(mode, prefix) && operatorNode.any { it.key == keyStrokes.last() }) {
            return true
        }

        // check for duplicate operator actions like dd, yy or cc
        if (keyStrokes.size == 2
                && keyStrokes.first() == keyStrokes.last()
                && operatorNode.any {
                    it.key == keyStrokes.first()
                            && it.value is CommandNode
                            && (it.value as CommandNode).actionHolder.instance is DuplicableOperatorAction
                }
        ) {
            return true
        }

        return false
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
        if (keyStrokes.isEmpty())
            return null
        val whichKeyDescriptions = descriptionProvider.getDescriptions(keyStrokes.dropLast(1))
        return whichKeyDescriptions.find { it.keyStroke == keyStrokes.asReversed().first() }?.description
    }

    /**
     * Convert a single key press or combination into an appropriate [String] representation
     *
     * @param keyStroke The pressed key (combination) as [KeyStroke]
     * @return String representation of the pressed key or key combination
     */
    fun keyToString(keyStroke: KeyStroke): String {
        return injector.parser.toKeyNotation(keyStroke)
    }
}