package xyz.rickygao.chatroom.client

import org.json.JSONException
import org.json.JSONObject
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*


class WelcomePanel : JPanel(), SocketListener {

    init {
        Dimension(320, 200).also {
            minimumSize = it
            maximumSize = it
            preferredSize = it
        }

        layout = GridBagLayout()
    }

    // ================================ USERNAME BEGIN ================================ //
    private val usernameLabel = JLabel("用户名").also {
        add(it, GridBagConstraints().apply {
            anchor = GridBagConstraints.EAST
            insets = INSETS
            ipadx = PADDING
            ipady = PADDING
            gridx = 0
            gridy = 0
        })
    }

    private val usernameField = JTextField(16).also {
        add(it, GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = INSETS
            ipadx = PADDING
            ipady = PADDING
            gridx = 1
            gridy = 0
        })
    }
    // ================================ USERNAME END ================================ //


    // ================================ PASSWORD BEGIN ================================ //
    private val passwordLabel = JLabel("密码").also {
        add(it, GridBagConstraints().apply {
            anchor = GridBagConstraints.EAST
            insets = INSETS
            ipadx = PADDING
            ipady = PADDING
            gridx = 0
            gridy = 1
        })
    }

    private val passwordField = JPasswordField(16).also {
        add(it, GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = INSETS
            ipadx = PADDING
            ipady = PADDING
            gridx = 1
            gridy = 1
        })
    }
    // ================================ PASSWORD END ================================ //


    // ================================ BUTTON BEGIN ================================ //
    private val registerButton = JButton("注册").apply {
        addActionListener {
            val username = usernameField.text
            val password = String(passwordField.password)
            if (username.isNotBlank() && password.isNotBlank())
                SocketManager.sendJson(JSONObject().apply {
                    put("command", "register")
                    put("username", username)
                    put("password", password)
                })
        }
    }

    private val loginButton = JButton("登录").apply {
        addActionListener {
            val username = usernameField.text
            val password = String(passwordField.password)
            if (username.isNotBlank() && password.isNotBlank())
                SocketManager.sendJson(JSONObject().apply {
                    put("command", "login")
                    put("username", username.also {
                        SocketManager.username = it
                    })
                    put("password", String(passwordField.password))
                })
        }
    }

    private val buttonBox = Box.createHorizontalBox().apply {
        add(registerButton)
        add(loginButton)
    }.also {
        add(it, GridBagConstraints().apply {
            anchor = GridBagConstraints.EAST
            gridwidth = GridBagConstraints.REMAINDER
            insets = INSETS
            ipadx = PADDING
            ipady = PADDING
            gridx = 0
            gridy = 2
        })
    }
    // ================================ BUTTON END ================================ //


    // ================================ INFO BEGIN ================================ //
    private val infoLabel = JLabel("正在连接到服务器").also {
        add(it, GridBagConstraints().apply {
            anchor = GridBagConstraints.EAST
            gridwidth = GridBagConstraints.REMAINDER
            insets = INSETS
            ipadx = PADDING
            ipady = PADDING
            gridx = 0
            gridy = 3
        })
    }
    // ================================ INFO END ================================ //


    init {
        SocketManager.connect()
    }

    override fun onConnect(t: Throwable?) {
        SwingUtilities.invokeLater {
            infoLabel.text = if (t == null) "连接服务器成功"
            else {
                JOptionPane.showMessageDialog(this, buildString {
                    appendln(t)
                    t.stackTrace.forEach { appendln("\tat $it") }
                }, "错误", JOptionPane.ERROR_MESSAGE)
                "连接服务器失败"
            }
        }
    }

    override fun onJsonReceive(json: JSONObject) {
        val command = json.getString("command")

        val message = try {
            json.getString("message")
        } catch (e: JSONException) {
            null
        }

        val info = when (command) {
            "register" -> when (message) {
                "success" -> "注册成功"
                "failure" -> "注册失败"
                else -> null
            }
            "login" -> when (message) {
                "success" -> "登录成功"
                "failure" -> "登录失败"
                else -> null
            }
            else -> null
        }

        SwingUtilities.invokeLater {
            infoLabel.text = info.orEmpty()
            if (command == "login" && message == "success")
                (rootPane.parent as? LoginSucceedListener)?.onLoginSucceed()
        }

    }
}

interface LoginSucceedListener {
    fun onLoginSucceed()
}