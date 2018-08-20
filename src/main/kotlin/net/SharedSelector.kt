package net

import java.net.InetSocketAddress
import java.nio.channels.*
import java.util.concurrent.ConcurrentLinkedQueue

typealias OnConnect = (Connection) -> Unit

class SharedSelector {
    private val selector = Selector.open()
    private val q = ConcurrentLinkedQueue<Runnable>()

    fun loopSelector() {
        while (!Thread.interrupted()) {
            val s = selector.select()
            processQ()
            if (s == 0) {
                continue
            }

            val keyIt = selector.selectedKeys().iterator()

            while (keyIt.hasNext()) {
                val key = keyIt.next()
                keyIt.remove()

                if (!key.isValid) {
                    continue
                }

                try {
                    if (key.isConnectable) {
                        val connection = (key.attachment() ?: continue) as AsyncConnection
                        val sChannel = key.channel() as SocketChannel
                        if (!sChannel.finishConnect()) {
                            connection.close()
                            continue
                        }
                        connection.connected()
                    } else if (key.isAcceptable) {
                        val serverChannel = key.channel() as ServerSocketChannel
                        val channel = serverChannel.accept() ?: continue
                        channel.configureBlocking(false)

                        val clientKey = channel.register(selector, SelectionKey.OP_READ)
                        val connection = AsyncConnection(clientKey, key.attachment() as OnConnect, Ops(clientKey))
                        clientKey.attach(connection)

                        connection.connected()
                    } else if (key.isWritable) {
                        val connection = (key.attachment() ?: continue) as AsyncConnection
                        connection.handleWrite()
                    } else if (key.isReadable) {
                        val connection = (key.attachment() ?: continue) as AsyncConnection
                        connection.handleRead()
                    }
                } catch (ex: Exception) {
                    println(ex)
                    // skip
                }
            }
        }
    }

    private fun processQ() {
        while (true) {
            val r = q.poll() ?: break
            r.run()
        }
    }

    fun connect(addr: InetSocketAddress, onConnect: OnConnect) {
        val channel = selector.provider().openSocketChannel();
        channel.configureBlocking(false)
        val connected = channel.connect(addr)
        q.add(Runnable {
            val key = channel.register(selector, 0)
            val ops = Ops(key)
            val connection = AsyncConnection(key, onConnect, ops)
            key.attach(connection)

            if (connected) {
                connection.connected()
            } else {
                ops.on(SelectionKey.OP_CONNECT)
            }
        })

        selector.wakeup()
    }

    fun listen(addr: InetSocketAddress, onConnect: OnConnect) {
        val channel = selector.provider().openServerSocketChannel();
        channel.configureBlocking(false)
        channel.bind(addr)
        q.add(Runnable {
            channel.register(selector, SelectionKey.OP_ACCEPT, onConnect)
        })
        selector.wakeup()
    }

    fun start() {
        val thread = Thread(
            Runnable(::loopSelector),
            "selector"
        )
        thread.start()
    }

    fun stop() {
        selector.close()
    }
}


