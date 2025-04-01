package eu.theblob42.idea.whichkey.provider.descriptions

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import eu.theblob42.idea.whichkey.provider.Description
import java.awt.event.InputEvent
import javax.swing.KeyStroke

class DictionaryDescriptionProviderTest : BasePlatformTestCase() {
    override fun tearDown() {
        injector.variableService.clear()
        super.tearDown()
    }

    private fun parseExpression(config: String): VimDataType {
        val executionContext = injector.executionContextManager.getEditorExecutionContext(myFixture.editor.vim)
        return injector.vimscriptParser.parseExpression(config)!!
            .evaluate(myFixture.editor.vim, executionContext, Script())
    }

    private fun initializeWithConfig(config: String): DictionaryDescriptionProvider {
        val variableName = "SomeDescriptions"
        myFixture.configureByText(PlainTextFileType.INSTANCE, "something")
        val config = parseExpression(config)
        injector.variableService.storeGlobalVariable(variableName, config)
        return DictionaryDescriptionProvider(variableName)
    }

    fun `test simple dictionary config with description`() {
        val provider =
            initializeWithConfig("{ '<leader>': ['someDescription', { 'g' : 'go somewhere' }] }")

        assertEquals(listOf(Description(KeyStroke.getKeyStroke('g'), "go somewhere")),
            provider.getDescriptions(injector.parser.parseKeys("<leader>"))
        )
        assertEquals(listOf(Description(injector.parser.parseKeys("<leader>").first(), "someDescription")),
            provider.getDescriptions(emptyList())
        )
    }

    fun `test deeply nested`() {
        val provider =
            initializeWithConfig("{ 'a': { 'b' : 'go somewhere', 'c' : { 'd' : { 'e' : 'f'} }, 'z': ['zzz', { 'x': 'xxx'}] } }")

        assertEquals(listOf(
            Description(KeyStroke.getKeyStroke('b'), "go somewhere"),
            Description(KeyStroke.getKeyStroke('c'), null),
            Description(KeyStroke.getKeyStroke('z'), "zzz"),
        ),
            provider.getDescriptions(injector.parser.parseKeys("a"))
        )

        assertEquals(listOf(
            Description(KeyStroke.getKeyStroke('x'), "xxx"),
        ),
            provider.getDescriptions(injector.parser.parseKeys("az"))
        )

        assertEquals(listOf(
            Description(KeyStroke.getKeyStroke('e'), "f"),
        ),
            provider.getDescriptions(injector.parser.parseKeys("acd"))
        )
    }

    fun `test control characters lowercase`() {
        val descriptions =
            initializeWithConfig("{ '<c-s-a>': 'something' }")
                .getDescriptions(emptyList())

        assertEquals(listOf(Description(KeyStroke.getKeyStroke('A'.code, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK), "something")), descriptions)
    }

    fun `test control characters uppercase`() {
        val descriptions =
            initializeWithConfig("{ '<C-S-A>': 'something' }")
                .getDescriptions(emptyList())

        assertEquals(listOf(Description(KeyStroke.getKeyStroke('A'.code, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK), "something")), descriptions)
    }
}