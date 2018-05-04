package xyz.rickygao.chatroom.server

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.ServerSocket
import java.net.Socket
import java.sql.Connection


fun main(vararg args: String) {

    Database.connect("jdbc:sqlite::resource:chatroom.db", driver = "org.sqlite.JDBC", manager = {
        // SQLite doesn't support the default default isolation level
        ThreadLocalTransactionManager(it, Connection.TRANSACTION_SERIALIZABLE)
    })

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Users, Friends)
    }

    ServerSocket(6000).use { serverSocket ->
        val manager = SocketThreadManager()
        serverSocket.acceptForever { socket ->
            SocketThread(socket, manager).start()
        }
    }
}

private inline fun ServerSocket.acceptForever(crossinline block: (socket: Socket) -> Unit): Nothing {
    while (true) {
        block(accept())
    }
}