package p2p

import kotlinx.coroutines.experimental.launch
import net.Connection
import net.HelloMessage
import net.SharedSelector
import net.WelcomeMessage
import java.net.InetSocketAddress
import java.nio.channels.AlreadyBoundException
import java.nio.channels.ClosedChannelException
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.IntStream

class P2PServer(
    val selector: SharedSelector,
    val onPeerConnected: suspend (NetworkPeer) -> Unit
) {
    val id = genId()
    val address = listen()
    val peers = mutableMapOf<String, NetworkPeer>()

    private fun listen(): InetSocketAddress {
        (0..65536)
            .shuffled()
            .forEach {
                try {
                    val addr = InetSocketAddress("localhost", it)
                    selector.listen(addr, ::onConnect)
                    return addr
                } catch (ex: Exception) {
                    // skip
                }
            }
        throw AlreadyBoundException()
    }

    private fun genId() = IntStream.range(0, 16).mapToObj {
        "0123456789ABCDEF"
            .toList()
            .shuffled()
            .first()
            .toString()
    }.reduce("", { a, b -> a + b })

    fun join(addr: InetSocketAddress) {
        selector.connect(addr, ::onConnect)
    }


    val x = AtomicInteger()
    private fun onConnect(connection: Connection) {
        launch {
            val y = "${address} ${x.incrementAndGet()} "
            try {
                println("${y}a")
                connection.tx.send(HelloMessage(id))
                println("${y}b")
                val msg = connection.rx.receive()
                println("${y}c")
                if (msg !is HelloMessage) {
                    println("${y}d")
                    return@launch
                }
                println("${y}d ${msg.id}")
                if (msg.id == id) {
                    println("${y}e")
                    return@launch
                }
                println("${y}f")
                val peer = NetworkPeer(id, msg.id, connection)
                println("${y}g")
                val prevPeer = peers.putIfAbsent(msg.id, peer)
                println("${y}h")
                if (prevPeer != null) {
                    println("${y}i")
                    return@launch
                }
                println("${y}j")
                connection.tx.send(WelcomeMessage())

                println("${y}k")
                val msg2 = connection.rx.receive()
                println("${y}l")
                if (msg2 !is WelcomeMessage) {
                    println("${y}m")
                    peers.remove(msg.id, peer)
                    println("${y}n")
                    return@launch
                }

                println("${y}o")
                onPeerConnected(peer)
                println("${y}p")
                peers.remove(msg.id, peer)
                println("${y}q")
            } catch (ex: ClosedChannelException) {
                println("${y}r")
                // skip
            } finally {
                println("${y}s")
                connection.close()
                println("${y}t")
            }
        }

    }
}

fun main(args: Array<String>) {
    val selector = SharedSelector()
    selector.start()
    val p1 = P2PServer(selector) {
        println(it.id)
    }
    val p2 = P2PServer(selector) {
        println(it.id)
    }
    p2.join(p1.address)
    p2.join(p1.address)
//    p1.join(p2.address)
//    p1.join(p1.address)
//    p2.join(p2.address)
}