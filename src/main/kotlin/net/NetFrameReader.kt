package net

import kotlinx.serialization.KInput
import kotlinx.serialization.serializer
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class NetMsgFrameReader(
    val maxMsgSize: Int
) {
    var inBuffer = ByteBuffer.allocate(1024)
    var deserializeBuf = inBuffer.duplicate()
    var deserializeIn: KInput = NetMsgInput(deserializeBuf)
    var skipInNextNBytes = 0

    fun read(channel: SocketChannel): NetMsg? {
        while (true) {
            var bufSize = inBuffer.position()

            val n = Math.min(bufSize, skipInNextNBytes)
            if (n > 0) {
                inBuffer.position(n).limit(bufSize)
                inBuffer.compact()
                skipInNextNBytes -= n
                continue
            }

            val frameStart = 4

            if (frameStart > bufSize) {
                channel.read(inBuffer)
                bufSize = inBuffer.position()
                if (frameStart > bufSize) {
                    return null
                }
            }

            val frameEnd = inBuffer.getInt(0) + frameStart

            if (frameEnd >= maxMsgSize) {
                skipInNextNBytes = frameEnd
                continue
            } else if (frameEnd > inBuffer.capacity()) {
                reallocateIn(frameEnd)
                continue
            } else if (frameEnd > bufSize) {
                channel.read(inBuffer)
                bufSize = inBuffer.position()
                if (frameEnd > bufSize) {
                    return null
                }
            }

            deserializeBuf.position(frameStart).limit(frameEnd)
            inBuffer.position(frameEnd).limit(bufSize)

            try {
                val type = NetMsg.classOf(deserializeIn.readIntValue())
                return deserializeIn.read(type.serializer())
            } finally {
                inBuffer.compact()
            }
        }
    }


    private fun reallocateIn(bufSz: Int) {
        val prevBuf = inBuffer
        var newSize = inBuffer.capacity() * 2
        while (newSize < bufSz) {
            newSize *= 2
        }
        inBuffer = ByteBuffer.allocate(newSize)
        deserializeBuf = inBuffer.duplicate()
        deserializeIn = NetMsgInput(deserializeBuf)
        prevBuf.flip()
        inBuffer.put(prevBuf)
    }

}