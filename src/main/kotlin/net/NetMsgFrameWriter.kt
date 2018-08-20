package net

import kotlinx.serialization.KSerialSaver
import kotlinx.serialization.serializer
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class NetMsgFrameWriter(
    val maxMsgSize: Int
) {
    private val predictor = NetMsgSizePredictor()

    private var outBuffer = ByteBuffer.allocate(1024)
    private var out = NetMsgOutput(outBuffer)

    enum class WriteResult {
        OK, TOO_BIG_MESSAGE, NEED_MORE_BUFFER
    }


    fun write(msg: Any): WriteResult {
        val requiredSpace = predictor.sizeOf(msg) + Integer.BYTES
        if (requiredSpace > maxMsgSize) {
            return WriteResult.TOO_BIG_MESSAGE
        }

        if (requiredSpace + outBuffer.position() > maxMsgSize) {
            return WriteResult.NEED_MORE_BUFFER
        }

        if (requiredSpace > outBuffer.remaining()) {
            reallocateOut(requiredSpace + outBuffer.position())
        }

        val sizePos = outBuffer.position()
        outBuffer.putInt(0)
        val frameStart = outBuffer.position()
        out.writeIntValue(NetMsg.tagOf(msg::class))
        out.write(msg::class.serializer() as KSerialSaver<Any>, msg)
        val frameEnd = outBuffer.position()

        outBuffer.putInt(sizePos, frameEnd - frameStart)

        return WriteResult.OK
    }

    fun drainTo(ch: SocketChannel) {
        outBuffer.flip()
        ch.write(outBuffer)
        outBuffer.compact()
    }

    private fun reallocateOut(requiredSpace: Int) {
        if (outBuffer.remaining() >= requiredSpace) {
            return
        }

        val prevBuf = outBuffer
        var newSize = outBuffer.capacity() * 2
        while (newSize - outBuffer.position() < requiredSpace) {
            newSize *= 2
        }

        outBuffer = ByteBuffer.allocate(newSize)
        out = NetMsgOutput(outBuffer)

        prevBuf.flip()
        outBuffer.put(prevBuf)
    }

    fun isEmpty() = outBuffer.position() == 0
}

