package eu.theblob42.idea.whichkey.model

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * @param prefix Indicator if this mapping is a prefix or an actual command
 * @param description A descriptive string for the mapping: category for a prefix ("Files"), descriptive name for a command ("close window") or the presentable String for a command ("<C-w>l")
 */
data class Mapping(val prefix: Boolean, val description: String)

object Mappings {
    private val DEFAULT_SORT_OPTION = SortOption.BY_KEY
    private val sortOption: SortOption
        get() = when (val option = injector.variableService.getGlobalVariableValue("WhichKey_SortOrder")) {
            null -> DEFAULT_SORT_OPTION
            !is VimString -> DEFAULT_SORT_OPTION
            else -> SortOption.values().firstOrNull { it.name.equals(option.asString(), ignoreCase = true) } ?: DEFAULT_SORT_OPTION
        }

    private const val DEFAULT_SORT_CASE_SENSITIVE = true
    private val sortCaseSensitive: Boolean
        get() = when (val option = injector.variableService.getGlobalVariableValue("WhichKey_SortCaseSensitive")) {
            null -> DEFAULT_SORT_CASE_SENSITIVE
            !is VimString -> DEFAULT_SORT_CASE_SENSITIVE
            else -> option.asString().toBoolean()
        }

    enum class SortOption {
        BY_KEY,
        BY_KEY_PREFIX_FIRST,
        BY_KEY_PREFIX_LAST,
        BY_DESCRIPTION
    }

    /**
     * Sort mappings dependent on the configured sort options
     * @param nestedMappings The list of mappings to sort
     * @return The sorted list of mappings
     */
    fun sort(nestedMappings: List<Pair<String, Mapping>>): List<Pair<String, Mapping>> {
        // String::compareTo is by default case-sensitive
        val cmp = if (sortCaseSensitive) String::compareTo else String.CASE_INSENSITIVE_ORDER::compare

        return when (sortOption) {
            SortOption.BY_KEY -> nestedMappings.sortedWith(compareBy(cmp) { it.first })
            SortOption.BY_KEY_PREFIX_FIRST -> nestedMappings.sortedWith(compareBy<Pair<String, Mapping>> { !it.second.prefix }.thenBy(cmp) { it.first })
            SortOption.BY_KEY_PREFIX_LAST -> nestedMappings.sortedWith(compareBy<Pair<String, Mapping>> { it.second.prefix }.thenBy(cmp) { it.first })
            SortOption.BY_DESCRIPTION -> nestedMappings.sortedWith(compareBy(cmp) { it.second.description })
        }
    }
}