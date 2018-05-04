package xyz.rickygao.chatroom.server

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object Users : IntIdTable("users") {
    val username = varchar("username", 32).uniqueIndex()
    val password = varchar("password", 32)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var username by Users.username
    var password by Users.password
    private val _pings by Friend referrersOn Friends.pongId
    private val _pongs by Friend referrersOn Friends.pingId
    val pings get() = _pings.map(Friend::ping).asIterable()
    val pongs get() = _pongs.map(Friend::pong).asIterable()
}

fun User.Companion.findByUsername(username: String) = User.find(Users.username eq username).singleOrNull()

fun User.toMap() = mapOf(
        "username" to username
)

fun User.toMapWithFriends() = mapOf(
        "username" to username,
        "pings" to pings.asSequence().map(User::toMap).toSet(),
        "pongs" to pongs.asSequence().map(User::toMap).toSet()
)

object Friends : IntIdTable("friends") {
    val pingId = integer("ping_id").entityId().references(Users.id)
    val pongId = integer("pong_id").entityId().references(Users.id)
}

class Friend(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Friend>(Friends)

    var ping by User referencedOn Friends.pingId
    var pong by User referencedOn Friends.pongId
}

fun Friend.Companion.check(me: String, him: String) = transaction {
    if (me == him) return@transaction true
    val _me = User.findByUsername(me)
    val _him = User.findByUsername(him)
    if (_me == null || _him == null) return@transaction false
    else _him in _me.pings || _him in _me.pongs
}

fun Friend.Companion.new(me: String, him: String) {
    Friend.new {
        ping = User.findByUsername(me)!!
        pong = User.findByUsername(him)!!
    }
}