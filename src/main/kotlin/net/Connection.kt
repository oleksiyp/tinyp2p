package net

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel

interface Connection {
    val rx: ReceiveChannel<NetMsg>
    val tx: SendChannel<NetMsg>

    fun close()
}