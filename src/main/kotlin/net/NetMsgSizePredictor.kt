package net

import kotlinx.serialization.ElementValueOutput
import kotlinx.serialization.KSerialSaver
import kotlinx.serialization.serializer
import java.nio.CharBuffer
import kotlin.reflect.KClass

class NetMsgSizePredictor() : ElementValueOutput(), SizePredictor {
    private val encoder = Charsets.UTF_8.newEncoder()
    private var bytes = 0

    override fun sizeOf(data: Any): Int {
        bytes = 0
        writeIntValue(NetMsg.tagOf(data::class))
        write(data::class.serializer() as KSerialSaver<Any>, data)
        return bytes
    }

    override fun writeNullValue() { bytes++ }
    override fun writeNotNullMark() { bytes++ }
    override fun writeBooleanValue(value: Boolean) { bytes++ }
    override fun writeByteValue(value: Byte) { bytes++ }
    override fun writeShortValue(value: Short) { bytes += 2 }
    override fun writeIntValue(value: Int) { bytes += 4 }
    override fun writeLongValue(value: Long) { bytes += 8 }
    override fun writeFloatValue(value: Float) { bytes += 4 }
    override fun writeDoubleValue(value: Double) { bytes += 8 }
    override fun writeCharValue(value: Char) { bytes += 2 }
    override fun writeStringValue(value: String) {
        bytes += 4
        bytes += encoder.encode(CharBuffer.wrap(value)).limit()
    }
    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) { bytes += 4 }
}