package eu.theblob42.idea.whichkey

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.StringHelper
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

object MappingConfig {

    private val mappingsPerMode = mutableMapOf<MappingMode, MutableMap<MappingSequence, String>>()

    init {
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
     * @param description A description which should be added for the given sequence
     */
    private fun addMapping(mode: MappingMode, keySequence: List<KeyStroke>, description: String) {
        val mappings = mappingsPerMode.getOrPut(mode) { mutableMapOf() }

        val tmpSequence = mutableListOf<KeyStroke>()
        for (keyStroke in keySequence) {
            tmpSequence.add(keyStroke)

            // add the description for the last element of the sequence
            val desc = if (tmpSequence.size == keySequence.size) {
                if (description.isNotBlank()) description else "No description"
            } else {
                "Prefix"
            }
            mappings.putIfAbsent(MappingSequence(tmpSequence.toList()), desc)
        }
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
        val typedSequence = keyStrokes.joinToString(separator = "") { keyToString(it) }
        return mappingsPerMode[mode]?.entries
            // only consider mappings which could be direct children (length + 1)
            ?.filter { it.key.keyStrokes.size == keyStrokes.size.inc() }
            ?.filter { it.key.sequence.startsWith(typedSequence) }
            ?.map {
                val key = it.key.sequence.replace(typedSequence, "")
                val value = it.value
                "${key} -> ${value}"
                    // escape angle brackets for usage in HTML
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
            }
            ?: listOf()
    }
}