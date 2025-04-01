package eu.theblob42.idea.whichkey.provider.descriptions

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import eu.theblob42.idea.whichkey.provider.Description
import javax.swing.KeyStroke

class FlatDescriptionProviderTest : BasePlatformTestCase() {
    override fun tearDown() {
        injector.variableService.clear()
        super.tearDown()
    }

    private fun initializeWithConfig(config: List<String>): FlatDescriptionProvider {
        val prefix = "SomePrefix"
        myFixture.configureByText(PlainTextFileType.INSTANCE, "something")
        config.mapIndexed { index: Int, s: String ->
            injector.variableService.storeGlobalVariable("${prefix}_${index}", VimString(s))
        }
        return FlatDescriptionProvider(prefix)
    }

    fun `test simple config`() {
        val provider =
            initializeWithConfig(listOf(
                "<leader> follow the leader",
                "<leader>a something",
                "<leader>b something else"
            ))

        assertEquals(listOf(
            Description(KeyStroke.getKeyStroke('a'), "something"),
            Description(KeyStroke.getKeyStroke('b'), "something else"),

        ),
            provider.getDescriptions(injector.parser.parseKeys("<leader>"))
        )
        assertEquals(listOf(Description(injector.parser.parseKeys("<leader>").first(), "follow the leader")),
            provider.getDescriptions(emptyList())
        )
    }

    fun `test deeply nested`() {
        val provider =
            initializeWithConfig(listOf(
                "a aDesc",
                "ab abDesc",
                "abc abcDesc",
                "abcd abcdDesc",
            ))

        assertEquals(listOf(
            Description(KeyStroke.getKeyStroke('b'), "abDesc"),
            ),
            provider.getDescriptions(injector.parser.parseKeys("a"))
        )
        assertEquals(listOf(
            Description(KeyStroke.getKeyStroke('d'), "abcdDesc"),
        ),
            provider.getDescriptions(injector.parser.parseKeys("abc"))
        )
    }

    fun `test missing nodes`() {
        val provider =
            initializeWithConfig(listOf(
                "abcd abcdDesc",
            ))

        assertEquals(listOf(
            Description(KeyStroke.getKeyStroke('d'), "abcdDesc"),
        ),
            provider.getDescriptions(injector.parser.parseKeys("abc"))
        )
    }

}