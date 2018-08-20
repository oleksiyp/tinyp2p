package p2p

import net.Connection

class NetworkPeer(
    val selfId: String,
    val id: String,
    val connection: Connection
) {

    fun stop() {
        connection.close()
    }
}