package xyz.rickygao.chatroom.server

import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONArray
import org.json.JSONObject
import xyz.rickygao.chatroom.writeJson

class SocketThreadManager : MutableSet<SocketThread> by mutableSetOf() {
    fun broadcast(data: JSONObject, filter: (SocketThread) -> Boolean = { true }) =
            asSequence().filter(filter).forEach { it.sink.writeJson(data) }
}

val SocketThreadManager.onlineUsernames
    get() = JSONArray(mapNotNull(SocketThread::username))

inline fun SocketThreadManager.broadcastLoggedIn(data: JSONObject, crossinline filter: (SocketThread) -> Boolean = { true }) =
        broadcast(data, { it.loggedIn && filter(it) })

inline fun SocketThreadManager.broadcastOnlineUsernames(crossinline filter: (SocketThread) -> Boolean = { true }) =
        broadcastLoggedIn(JSONObject().put("command", "list-onlines").put("onlines", onlineUsernames), filter)

fun SocketThreadManager.send(data: JSONObject, username: String) =
        broadcast(data) { it.username == username }

fun SocketThreadManager.sendOnlineUsernames(username: String) =
        broadcastOnlineUsernames { it.username == username }

fun SocketThreadManager.sendFriendUsernames(username: String) =
        transaction {
            val me = User.findByUsername(username)!!
            println(me.toMapWithFriends())
            me.pings union me.pongs
        }.let {
            send(JSONObject().put("command", "list-friends").put("friends", JSONArray(it.map(User::username))), username)
        }