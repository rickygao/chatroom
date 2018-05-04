package xyz.rickygao.chatroom.client

import org.json.JSONObject
import java.awt.Container
import java.awt.Insets
import javax.swing.JFrame

const val PADDING = 4
val INSETS = Insets(PADDING, PADDING, PADDING, PADDING)

class ClientFrame : JFrame(), SocketListener, LoginSucceedListener {

    private val functionPanel by lazy { ChatPanel() }
    private val welcomePanel by lazy { WelcomePanel() }

    private var content: Container
        get() = contentPane
        set(value) {
            contentPane = value
            minimumSize = value.minimumSize
            maximumSize = value.maximumSize
            preferredSize = value.preferredSize
            pack()
        }

    override fun onLoginSucceed() {
        content = functionPanel
        SocketManager.sendJson(JSONObject().put("command", "list-onlines"))
        SocketManager.sendJson(JSONObject().put("command", "list-friends"))
    }

    override fun onConnect(t: Throwable?) {
        (content as? SocketListener)?.onConnect(t)
    }

    override fun onJsonReceive(json: JSONObject) {
        (content as? SocketListener)?.onJsonReceive(json)
    }

    init {
        SocketManager.socketListener = this

        title = "聊天室"
        content = welcomePanel
    }

}