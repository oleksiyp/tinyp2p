package net

import java.nio.channels.SelectionKey
import java.nio.channels.SelectionKey.*
import java.util.concurrent.atomic.AtomicInteger

class Ops(val key: SelectionKey) {
    var flags = AtomicInteger(key.interestOps())

    fun on(mask: Int) {
        key.interestOps(update { it or mask } ?: return)
    }

    fun off(mask: Int) {
        key.interestOps(update { it and mask.inv() } ?: return)
    }

    operator fun contains(mask: Int) = flags.get() and mask > 0

    private fun update(op: (Int) -> Int): Int? {
        var next: Int
        do {
            val prev = flags.get()
            next = op(prev)
            if (next == prev) {
                return null
            }
        } while (!flags.compareAndSet(prev, next))
        return next
    }

    override fun toString(): String {
        val lst = mutableListOf<String>()
        if (contains(OP_READ)) lst.add("OP_READ")
        if (contains(OP_WRITE)) lst.add("OP_WRITE")
        if (contains(OP_CONNECT)) lst.add("OP_CONNECT")
        if (contains(OP_ACCEPT)) lst.add("OP_ACCEPT")

        return lst.joinToString(" | ")
    }
}