package net

import kotlinx.coroutines.experimental.channels.*
import net.NetMsgFrameWriter.WriteResult.OK
import net.NetMsgFrameWriter.WriteResult.TOO_BIG_MESSAGE
import java.io.IOException
import java.nio.channels.SelectionKey
import java.nio.channels.SelectionKey.*
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicInteger

class AsyncConnection(
    private val key: SelectionKey,
    private val onConnect: OnConnect,
    private val ops: Ops
) : Connection {

    private val rxCh = Channel<NetMsg>(Channel.UNLIMITED)
    private val txCh = Channel<NetMsg>(100)

    private val frameReader = NetMsgFrameReader(1024 * 1024)
    private val frameWriter = NetMsgFrameWriter(1024 * 1024)

    private val rxMsgCount = AtomicInteger(0)
    private val txMsgCount = AtomicInteger(0)

    var rxWaterline = 100
        set(value) = if (value > 0) field = value else field = 1

    override val rx: ReceiveChannel<NetMsg> = produce {
        rxCh.consumeEach {
            if (rxMsgCount.decrementAndGet() < rxWaterline) {
                ops.on(OP_READ)
            }
            send(it)
        }
    }

    fun handleRead() {
        val msg = frameReader.read(key.channel() as SocketChannel) ?: return
        if (!rxCh.offer(msg)) {
            throw RuntimeException("failed to send")
        }
        val cnt = rxMsgCount.incrementAndGet()
        if (cnt >= rxWaterline) {
            ops.off(OP_READ)
        }
    }

    override val tx: SendChannel<NetMsg> = actor {
        for (msg in channel) {
            val cnt = txMsgCount.getAndIncrement()
            if (cnt == 0) {
                ops.on(OP_WRITE)
            }
            txCh.send(msg)
        }
        txCh.close()
    }

    private var savedMsg: NetMsg? = null

    fun handleWrite() {
        if (!writeMessage(savedMsg)) {
            return
        }
        writeMessage(txCh.poll())
        writeMessage(savedMsg)

        if (txMsgCount.get() == 0 && frameWriter.isEmpty()) {
            ops.off(OP_WRITE)
        }

    }

    private fun writeMessage(msg: NetMsg?): Boolean {
        if (msg == null) return true

        val outcome = frameWriter.write(msg)
        if (outcome == OK || outcome == TOO_BIG_MESSAGE) {
            txMsgCount.decrementAndGet()
            savedMsg = null
            frameWriter.drainTo(key.channel() as SocketChannel)
            return true
        } else {
            savedMsg = msg
            frameWriter.drainTo(key.channel() as SocketChannel)
            return false
        }
    }

    override fun close() {
        rxCh.close()
        txCh.close()
        try {
            key.channel().close()
        } catch (e: IOException) {
            // skip
        }
        key.cancel()
    }

    fun connected() {
        ops.on(OP_READ)
        onConnect.invoke(this)
    }
}

