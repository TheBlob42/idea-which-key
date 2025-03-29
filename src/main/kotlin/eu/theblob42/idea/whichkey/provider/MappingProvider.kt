package eu.theblob42.idea.whichkey.provider

import com.maddyhome.idea.vim.command.MappingMode
import javax.swing.KeyStroke

interface MappingProvider {
    fun getMappings(mode: MappingMode, path: List<KeyStroke>): List<Mapping>
}
