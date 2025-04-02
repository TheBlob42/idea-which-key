package eu.theblob42.idea.whichkey.provider

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import eu.theblob42.idea.whichkey.model.Mapping
import kotlinx.coroutines.*
import javax.swing.KeyStroke
import kotlin.coroutines.CoroutineContext

interface PopupProvider {
    fun showPopup(
        editor: Editor,
        typedKeys: List<KeyStroke>,
        nestedMappings: List<Pair<String, Mapping>>
    )
    fun hidePopup()
}

@Service
class PopupDebounceService(
    private val cs: CoroutineScope
) {
    private var debounceJob: Job? = null
    fun showPopupDebounced(
        provider: PopupProvider,
        delay: Long,
        editor: Editor,
        typedKeys: List<KeyStroke>,
        nestedMappings: List<Pair<String, Mapping>>,
    ) {
        if (debounceJob?.isCompleted != false) {
            debounceJob = cs.launch {
                delay(delay)
                provider.showPopup(editor, typedKeys, nestedMappings)
            }
        }
    }
    fun cancel() {
        debounceJob?.cancel()
    }
}

@OptIn(DelicateCoroutinesApi::class)
class DebouncingPopupProvider(private val provider: PopupProvider) : PopupProvider {
    private val DEFAULT_POPUP_DELAY = 200L
    private val defaultPopupDelay: Long
        get() = when (val delay = injector.variableService.getGlobalVariableValue("WhichKey_DefaultDelay")) {
            null -> DEFAULT_POPUP_DELAY
            !is VimInt -> DEFAULT_POPUP_DELAY
            else -> delay.value.toLong()
        }
    private var debounceJob: Job? = null
    override fun showPopup(
        editor: Editor,
        typedKeys: List<KeyStroke>,
        nestedMappings: List<Pair<String, Mapping>>
    ) {
        when (val delay = defaultPopupDelay) {
            0L -> provider.showPopup(editor, typedKeys, nestedMappings)
            else -> {
                if (debounceJob?.isCompleted != false) {
                    debounceJob = GlobalScope.launch {
                        delay(delay)
                        withContext(Dispatchers.EDT) {
                            provider.showPopup(editor, typedKeys, nestedMappings)
                        }
                    }
                }
            }
        }
    }

    override fun hidePopup() {
//if (debounceJob?.isCompleted != true) {
//    runBlocking {
//        debounceJob?.cancelAndJoin()
//    }
//}
        debounceJob?.cancel()
        provider.hidePopup()
    }

}