package xyz.rickygao.chatroom.server

import okio.BufferedSink
import okio.BufferedSource
import okio.Okio
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject
import xyz.rickygao.chatroom.putTimestamp
import xyz.rickygao.chatroom.readJson
import xyz.rickygao.chatroom.writeJson
import java.net.Socket
import java.sql.SQLException

class SocketThread(
        private val socket: Socket,
        private val manager: SocketThreadManager
) : Thread() {

    init {
        isDaemon = true
    }

    val source: BufferedSource = Okio.buffer(Okio.source(socket))
    val sink: BufferedSink = Okio.buffer(Okio.sink(socket))

    var username: String? = null
    val loggedIn get() = username != null

    override fun run() {
        printlnWithAddress("Connected")
        manager.add(this)
        try {
            source.use { source ->
                while (!source.exhausted())
                    onJsonReceive(source.readJson())
            }
        } finally {
            manager.remove(this)
            if (username != null) {
                username = null
                manager.broadcastOnlineUsernames()
            }
            printlnWithAddress("Disconnected")
        }
    }

    private fun onJsonReceive(json: JSONObject) {
        printlnWithAddress(json)
        val command = json.getString("command")
        when (command) {
            "register" -> {
                val username = json.getString("username")
                val password = json.getString("password")
                val message = try {
                    transaction {
                        User.new {
                            this.username = username
                            this.password = password
                        }
                    }
                    "success"
                } catch (e: SQLException) {
                    e.printStackTrace()
                    "failure"
                }

                sink.writeJson(JSONObject().put("command", "register").put("message", message))
            }

            "login" -> {
                val username = json.getString("username")
                val password = json.getString("password")
                val succeeded = !transaction {
                    User.find((Users.username eq username) and (Users.password eq password)).empty()
                }
                val message = if (succeeded) "success" else "failure"

                sink.writeJson(JSONObject().put("command", "login").put("message", message))
                if (succeeded) this.username = username

                manager.broadcastOnlineUsernames()
            }

            "list-onlines" -> if (loggedIn)
                manager.sendOnlineUsernames(username!!)

            "chat" -> if (loggedIn) {
                val content = json.getString("content")
                val to = json.getString("to")

                if (Friend.check(username!!, to)) manager.send(JSONObject().put("command", "chat").put("content", content).put("from", username).putTimestamp(), to)
            }

            "file" -> if (loggedIn) {
                val content = json.getString("content")
                val filename = json.getString("filename")
                val to = json.getString("to")

                if (Friend.check(username!!, to)) manager.send(JSONObject().put("command", "file").put("content", content).put("filename", filename).put("from", username).putTimestamp(), to)
            }

            "add" -> if (loggedIn) {
                val content = json.getString("content")
                val to = json.getString("to")

                if (!Friend.check(username!!, to)) manager.send(JSONObject().put("command", "add").put("content", content).put("from", username), to)
            }

            "confirm" -> if (loggedIn) {
                val to = json.getString("to")
                val agreed = json.getBoolean("agreed")

                if (agreed) {
                    transaction {
                        Friend.new(username!!, to)
                    }

                    manager.sendFriendUsernames(username!!)
                    manager.sendFriendUsernames(to)
                }
            }

            "list-friends" -> if (loggedIn)
                manager.sendFriendUsernames(username!!)
        }
    }

    private fun printlnWithAddress(message: Any?) = println("${socket.inetAddress}/$username -> $message")
}