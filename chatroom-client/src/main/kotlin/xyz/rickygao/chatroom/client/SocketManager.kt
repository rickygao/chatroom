package xyz.rickygao.chatroom.client

import okio.BufferedSink
import okio.BufferedSource
import okio.Okio
import org.json.JSONObject
import xyz.rickygao.chatroom.readJson
import xyz.rickygao.chatroom.writeJson
import java.net.Socket
import kotlin.concurrent.thread

object SocketManager {

    const val host = "rickygao.xyz"
    const val port = 6000

    lateinit var username: String

    lateinit var socket: Socket
        private set

    lateinit var source: BufferedSource
        private set

    lateinit var sink: BufferedSink
        private set

    fun connect() {
        thread(isDaemon = true, name = "Socket-Thread") {
            try {
                socket = Socket(host, port).also {
                    source = Okio.buffer(Okio.source(it))
                    sink = Okio.buffer(Okio.sink(it))
                }
                socketListener.onConnect(null)
            } catch (t: Throwable) {
                socketListener.onConnect(t)
            }

            while (!source.exhausted())
                socketListener.onJsonReceive(source.readJson())
        }
    }

    fun sendJson(jsonObject: JSONObject) = sink.writeJson(jsonObject)

    var socketListener: SocketListener = EmptySocketListener
}

interface SocketListener {
    fun onConnect(t: Throwable?) = Unit
    fun onJsonReceive(json: JSONObject) = Unit
}

object EmptySocketListener : SocketListener