package eu.theblob42.idea.whichkey.config

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.awt.RelativePoint
import com.maddyhome.idea.vim.api.injector
import eu.theblob42.idea.whichkey.model.Mapping
import java.awt.*
import javax.swing.*
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.ui.popup.*
import eu.theblob42.idea.whichkey.model.Mappings
import eu.theblob42.idea.whichkey.provider.PopupProvider
import java.awt.event.KeyEvent

val WHICHKEY_MAPPING_BINDING =
    TextAttributesKey.createTextAttributesKey("WHICHKEY_MAPPING_BINDING", DefaultLanguageHighlighterColors.NUMBER)
val WHICHKEY_MAPPING_ASSIGMENT =
    TextAttributesKey.createTextAttributesKey("WHICHKEY_MAPPING_ASSIGMENT", DefaultLanguageHighlighterColors.CONSTANT)
val WHICHKEY_MAPPING_ICON =
    TextAttributesKey.createTextAttributesKey("WHICHKEY_MAPPING_ICON", DefaultLanguageHighlighterColors.CONSTANT)
val WHICHKEY_MAPPING_DESCRIPTION = TextAttributesKey.createTextAttributesKey(
    "WHICHKEY_MAPPING_DESCRIPTION",
    DefaultLanguageHighlighterColors.LINE_COMMENT
)
val WHICHKEY_MAPPING_DESCRIPTION_GROUP = TextAttributesKey.createTextAttributesKey(
    "WHICHKEY_MAPPING_DESCRIPTION_GROUP",
    DefaultLanguageHighlighterColors.KEYWORD
)

class NewPopupProvider: PopupProvider {
    private var currentPopup: JBPopup? = null
    override fun hidePopup() {
        currentPopup?.cancel()
        currentPopup = null
    }

    private fun show(editor: Editor, text: TextWithHighlights) {
        val editorFactory: EditorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument(text.text)
        val editor2: Editor = editorFactory.createViewer(document)
        editor2.document.setReadOnly(true)
        editor2.settings.apply {
            isLineMarkerAreaShown = false
            isIndentGuidesShown = false
            isLineNumbersShown = false
            isFoldingOutlineShown = false
            isAllowSingleLogicalLineFolding = true
            isAdditionalPageAtBottom = false
            additionalColumnsCount = 0
            additionalLinesCount = 0
            isRightMarginShown = false
            isCaretRowShown = false
            isShowingSpecialChars = false
        }

        val markupModel = editor2.markupModel

        text.highlights.forEach {
            markupModel.addRangeHighlighter(it.startOffset, it.endOffset, 0, it.textAttributes, it.targetArea)
        }

        val lines = text.text.lines()

        val contentSize = editor.component.visibleRect.size
        val popupHeight = lines.size * editor.lineHeight
        val component = object : JComponent() {
            init {
                layout = BorderLayout()

                val scrollPane = ScrollPaneFactory.createScrollPane(editor2.contentComponent, true).apply {
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                }
                scrollPane.preferredSize = Dimension(contentSize.width, popupHeight)
                add(scrollPane, BorderLayout.CENTER)
            }
        }


        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(component, null)
            .setFocusable(false)
//            .setRequestFocus(false)
            .setShowBorder(true)
//            .setMovable(true)
            .setMayBeParent(false)
            .setCancelOnClickOutside(false)
//            .setCancelOnWindowDeactivation(false)
            .setCancelOnOtherWindowOpen(true)
            .setCancelKeyEnabled(false)
//            .addListener(object: JBPopupListener {
//                override fun onClosed(event: LightweightWindowEvent) {
//                    val commandState = CommandState.getInstance(editor)
//                    commandState.mappingState.resetMappingSequence()
//                    super.onClosed(event)
//                }
//            })
            .createPopup()
        popup.show(RelativePoint(editor.component, Point(0, contentSize.height - popupHeight)))
        currentPopup = popup
    }


    private val keys = mapOf(
        KeyEvent.VK_UP to "",
        KeyEvent.VK_DOWN to "",
        KeyEvent.VK_LEFT to "",
        KeyEvent.VK_RIGHT to "",
        KeyEvent.VK_ENTER to "󰌑",
        KeyEvent.VK_ESCAPE to "󱊷",
        KeyEvent.VK_BACK_SPACE to "󰁮",
        KeyEvent.VK_SPACE to "󱁐",
        KeyEvent.VK_TAB to "󰌒",
        KeyEvent.VK_F1 to "󱊫",
        KeyEvent.VK_F2 to "󱊬",
        KeyEvent.VK_F3 to "󱊭",
        KeyEvent.VK_F4 to "󱊮",
        KeyEvent.VK_F5 to "󱊯",
        KeyEvent.VK_F6 to "󱊰",
        KeyEvent.VK_F7 to "󱊱",
        KeyEvent.VK_F8 to "󱊲",
        KeyEvent.VK_F9 to "󱊳",
        KeyEvent.VK_F10 to "󱊴",
        KeyEvent.VK_F11 to "󱊵",
        KeyEvent.VK_F12 to "󱊶",
    )

    override fun showPopup(
        editor: Editor,
        typedKeys: List<KeyStroke>,
        nestedMappings: List<Pair<String, Mapping>>
    ) {
        if (nestedMappings.isEmpty()) {
            return
        }
        val items = Mappings.sort(nestedMappings).map {
            val key = injector.parser.parseKeys(it.first).first()
            val keyCode = if (key.keyCode == 0) KeyEvent.getExtendedKeyCodeForChar(key.keyChar.code) else key.keyCode
            Item(
                Modifiers.fromKeyStroke(key),
                keys.getOrDefault(
                    keyCode,
                    if (key.keyCode == 0) key.keyChar.toString() else KeyEvent.getKeyText(key.keyCode)
                ),
                null,
                it.second.description,
                it.second.prefix
            )
        }
        val text = PopupLayout.layoutItems(
            40, editor.calculateSizeInCharacters()?.width ?: 50, WhichKeyConfig(), items
        )
        show(editor, text)
        return
    }
}
