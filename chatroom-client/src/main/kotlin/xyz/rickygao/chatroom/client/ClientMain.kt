package xyz.rickygao.chatroom.client

import javax.swing.UIManager
import javax.swing.WindowConstants.EXIT_ON_CLOSE

fun main(vararg args: String) {

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    ClientFrame().apply {
        defaultCloseOperation = EXIT_ON_CLOSE
        setLocationRelativeTo(null)
        isVisible = true
    }

}