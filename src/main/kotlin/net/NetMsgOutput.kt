package net

import kotlinx.serialization.ElementValueOutput
import java.nio.ByteBuffer
import java.nio.CharBuffer
import kotlin.reflect.KClass

class NetMsgOutput(val out: ByteBuffer) : ElementValueOutput() {

    val encoder = Charsets.UTF_8.newEncoder()

    override fun writeNullValue() { out.put(0) }
    override fun writeNotNullMark() { out.put(1) }
    override fun writeBooleanValue(value: Boolean) { out.put((if (value) 1 else 0).toByte()) }
    override fun writeByteValue(value: Byte) { out.put(value) }
    override fun writeShortValue(value: Short) { out.putShort(value) }
    override fun writeIntValue(value: Int) { out.putInt(value) }
    override fun writeLongValue(value: Long) { out.putLong(value) }
    override fun writeFloatValue(value: Float) { out.putFloat(value) }
    override fun writeDoubleValue(value: Double) { out.putDouble(value) }
    override fun writeCharValue(value: Char) { out.putChar(value) }
    override fun writeStringValue(value: String) {
        out.putInt(value.length)
        encoder.encode(CharBuffer.wrap(value), out, true)
    }
    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) { out.putInt(value.ordinal) }
}