package eu.theblob42.idea.whichkey.util

import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.Node
import javax.swing.KeyStroke

tailrec fun <T> Node<T>.getPath(path: List<KeyStroke>): Node<T>? {
    if (path.isEmpty()) return this
    return when (this) {
        is CommandPartNode<T> -> {
            val head = path.first()
            val tail = path.drop(1)
            this[head]?.getPath(tail)
        }

        else -> null
    }
}
