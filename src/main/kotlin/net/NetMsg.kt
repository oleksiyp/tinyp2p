package net

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
abstract class NetMsg {
    companion object {
        val messages = arrayOf(
            HelloMessage::class,
            WelcomeMessage::class,
            PingMessage::class,
            PongMessage::class
        )

        fun tagOf(cls: KClass<out Any>) = messages.indexOf(cls).let {
            if (it == -1) throw RuntimeException("not known NetMsg: $cls")
            it
        }

        fun classOf(tag: Int) = messages[tag]
    }
}

@Serializable
data class HelloMessage(val id: String) : NetMsg()

@Serializable
class WelcomeMessage : NetMsg()

@Serializable
data class PingMessage(val n: Int) : NetMsg()

@Serializable
data class PongMessage(val n: Int) : NetMsg()
