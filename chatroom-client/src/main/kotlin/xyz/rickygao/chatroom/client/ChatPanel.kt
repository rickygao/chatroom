package xyz.rickygao.chatroom.client

import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.*
import kotlin.properties.Delegates

class ChatPanel : JPanel(), SocketListener {

    init {
        minimumSize = Dimension(320, 240)
        preferredSize = Dimension(640, 480)

        layout = BorderLayout()
    }

    // ================================ CHAT BEGIN ================================ //
    val chatArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    val chatScrollPane = JScrollPane(chatArea).also {
        add(it, BorderLayout.CENTER)
    }

    private fun scrollChatAreaToBottom() {
        chatArea.caretPosition = chatArea.text.length
        chatScrollPane.verticalScrollBar.value = chatScrollPane.verticalScrollBar.maximum + 1
    }

    init {
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) = scrollChatAreaToBottom()
        })
    }
    // ================================ CHAT END ================================ //


    // ================================ SEND BEGIN ================================ //
    private val sendActionListener: ActionListener = ActionListener {
        if (chatField.text == "" || toComboBox.selectedItem == null) return@ActionListener
        val content = chatField.text
        val to = toComboBox.selectedItem.toString()
        SocketManager.sendJson(JSONObject().put("command", "chat").put("content", content).put("to", to))
        chatField.text = ""
        chatArea.append("""
            你在 ${Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE_TIME)} 对 $to 说：
            $content

            """.trimIndent())
    }

    val toComboBox = JComboBox<String>()

    val chatField = JTextField().apply {
        registerKeyboardAction(sendActionListener, KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_FOCUSED)
    }

    val sendButton = JButton("发送").apply {
        addActionListener(sendActionListener)
    }

    val fileChooser = JFileChooser().apply {
        //        fileFilter = object : FileFilter() {
//            val acceptableExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp")
//            override fun accept(f: File): Boolean = f.extension in acceptableExtensions
//
//            val acceptableDescription = "图片文件(${acceptableExtensions.joinToString { "*.$it" }})"
//            override fun getDescription(): String = acceptableDescription
//
//        }
    }

    val sendImageButton = JButton("发送文件").apply {
        addActionListener {
            if (fileChooser.showOpenDialog(this@ChatPanel) == JFileChooser.APPROVE_OPTION) {
                if (toComboBox.selectedItem == null) return@addActionListener
                val file = fileChooser.selectedFile
                val content = Base64.getEncoder().encodeToString(file.readBytes())
                val filename = file.name
                val to = toComboBox.selectedItem.toString()

                SocketManager.sendJson(JSONObject().put("command", "file").put("content", content).put("filename", filename).put("to", to))
            }
        }
    }

    val sendBox = Box.createHorizontalBox().apply {
        add(toComboBox)
        add(chatField)
        add(sendButton)
        add(sendImageButton)
    }.also {
        add(it, BorderLayout.SOUTH)
    }
    // ================================ SEND END ================================ //


    // ================================ LIST BEGIN ================================ //
    val onlineLabel = JLabel("在线用户")
    val onlineList = JList<String>()
    val onlineScrollPane = JScrollPane(onlineList).apply {
        preferredSize = Dimension(120, 0)
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
    }
    val addButton = JButton("添加好友").apply {
        addActionListener {
            val to = onlineList.selectedValue
            if (to == null || to in friends || to == SocketManager.username) return@addActionListener

            JOptionPane.showInputDialog(this, "向 $to 发送验证信息，以添加为好友：")?.let { content ->
                SocketManager.sendJson(JSONObject().put("command", "add").put("content", content).put("to", to))
            }
        }
    }
    val friendLabel = JLabel("好友用户")
    val friendList = JList<String>()
    val friendScrollPane = JScrollPane(friendList).apply {
        preferredSize = Dimension(120, 0)
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
    }
    val listBox = Box.createVerticalBox().apply {
        add(onlineLabel)
        add(onlineScrollPane)
        add(addButton)
        add(friendLabel)
        add(friendScrollPane)
    }.also {
        add(it, BorderLayout.WEST)
    }
    // ================================ LIST END ================================ //

    var onlines by Delegates.observable(emptyList<String>()) { _, _, newOnlines ->
        onlineList.setListData(newOnlines.toTypedArray())
        ensureOnlineFriends()
    }

    var friends by Delegates.observable(emptyList<String>()) { _, _, newFriends ->
        friendList.setListData(newFriends.toTypedArray())
        ensureOnlineFriends()
    }

    private fun ensureOnlineFriends() {
        toComboBox.model = DefaultComboBoxModel<String>((onlines intersect friends).toTypedArray())
    }

    override fun onJsonReceive(json: JSONObject) {
        val command = json.getString("command")

        SwingUtilities.invokeLater {
            when (command) {
                "chat" -> {
                    val content = json.getString("content")
                    val from = json.getString("from")
                    val timestamp = json.getLong("timestamp")
                    chatArea.append("""
                        $from 在 ${Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE_TIME)} 对你说：
                        $content

                        """.trimIndent())
                    scrollChatAreaToBottom()
                }
                "file" -> {
                    val content = json.getString("content")
                    val filename = json.getString("filename")
                    val from = json.getString("from")
                    val timestamp = json.getLong("timestamp")

                    val extension = filename.substringAfterLast(".", "")
                    val localFilename = UUID.randomUUID().toString() + if (extension != "") ".$extension" else ""
                    chatArea.append("""
                        $from 在 ${Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE_TIME)} 给你发送了一个文件 $filename，将存储在 $localFilename

                        """.trimIndent())
                    scrollChatAreaToBottom()

                    File(localFilename).writeBytes(Base64.getDecoder().decode(content))
                }
                "list-onlines" ->
                    onlines = json.getJSONArray("onlines").filterIsInstance<String>()
                "list-friends" ->
                    friends = json.getJSONArray("friends").filterIsInstance<String>()
                "add" -> {
                    val content = json.getString("content")
                    val from = json.getString("from")
                    val agreed = JOptionPane.showConfirmDialog(this,
                            "接受 $from 的好友申请吗？他说 $content",
                            UIManager.getString("OptionPane.titleText"),
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION

                    SocketManager.sendJson(JSONObject().put("command", "confirm").put("to", from).put("agreed", agreed))
                }
            }
        }
    }
}
