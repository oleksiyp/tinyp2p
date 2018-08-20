package net

import kotlinx.serialization.ElementValueInput
import java.nio.ByteBuffer
import java.nio.CharBuffer
import kotlin.reflect.KClass

class NetMsgInput(val inp: ByteBuffer) : ElementValueInput() {
    val decoder = Charsets.UTF_8.newDecoder()

    override fun readNotNullMark(): Boolean = inp.get() != 0.toByte()
    override fun readBooleanValue(): Boolean = inp.get().toInt() != 0
    override fun readByteValue(): Byte = inp.get()
    override fun readShortValue(): Short = inp.getShort()
    override fun readIntValue(): Int = inp.getInt()
    override fun readLongValue(): Long = inp.getLong()
    override fun readFloatValue(): Float = inp.getFloat()
    override fun readDoubleValue(): Double = inp.getDouble()
    override fun readCharValue(): Char = inp.getChar()
    override fun readStringValue(): String {
        val len = inp.getInt()
        val buf = CharBuffer.allocate(len)
        decoder.decode(inp, buf, true)
        buf.flip()
        return buf.toString()
    }
    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T = enumClass.java.enumConstants[inp.getInt()]
}