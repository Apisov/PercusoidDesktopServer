import com.illposed.osc.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import javafx.util.Duration
import themidibus.MidiBus
import tornadofx.Controller
import tornadofx.View
import tornadofx.runLater
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException


class MainView : View() {
    private val controller: MyController by inject()

    override val root: VBox by fxml()
    private val connectionLabel: Label by fxid("connection")
    private val hits: ProgressBar by fxid("hits")
    private val controls: ProgressBar by fxid("controls")

    init {
        controller.init(connectionLabel, hits, controls)
    }

    @FXML
    private fun handleCloseAction() {
        Platform.exit()
    }
}

const val SERVER_PORT = 12100

class MyController : Controller() {
    private lateinit var midiBus: MidiBus
    private lateinit var connectionLabel: Label
    private lateinit var hits: ProgressBar
    private lateinit var controls: ProgressBar

    private val connections: MutableList<Connection> = mutableListOf()

    private val addressSelector = AddressSelector { true } // TODO: Remove passing all messages

    private val oscListener = OSCListener { _, message -> handleMessage(message) }

    fun init(
        connectionLabel: Label,
        hits: ProgressBar,
        controls: ProgressBar
    ) {
        this.hits = hits
        this.controls = controls
        this.connectionLabel = connectionLabel

        try {
            val oscServer = OSCPortIn(DatagramSocket(SERVER_PORT, getInetAddress()))
            oscServer.startListening()
            oscServer.addListener(addressSelector, oscListener)
        } catch (e: SocketException) {
            connectionLabel.text = "Already in use"
        }
        midiBus = MidiBus(this, -1, "Virtual MIDI Bus")
    }

    private fun handleMessage(message: OSCMessage?) {
        val endpoints = message?.address!!.split("/")

        when (endpoints[1]) {
            "connect" -> {
                val networkAddress = message.arguments[0] as String
                val port = message.arguments[1] as Int

                val socket = DatagramSocket(port)
                socket.connect(InetAddress.getByName(networkAddress), port)
                val oscSender = OSCPortOut(
                    InetAddress.getByName(networkAddress),
                    port,
                    socket
                )

                val connectMessage = OSCMessage("/connected")
                connectMessage.addArgument(getInetAddress()?.hostAddress)
                connectMessage.addArgument(SERVER_PORT)
                oscSender.send(connectMessage)

                connections.add(Connection(networkAddress, port))
                // remove connection that was used just for sending server info
                oscSender.close()
                socket.disconnect()

                runLater {
                    connectionLabel.textFill = Paint.valueOf("#FFF")
                    connectionLabel.text = "Connected"
                }
            }
            "disconnect" -> {
                connections.remove(
                    Connection(
                        message.arguments[0] as String,
                        message.arguments[1] as Int
                    )
                )

                if (connections.isEmpty()) {
                    runLater {
                        connectionLabel.textFill = Paint.valueOf("#727272")
                        connectionLabel.text = "Not connected"
                    }
                }
            }
            "controllerChange" -> {
                val channel = message.arguments[0] as Int
                val control = message.arguments[1] as Int
                val velocity = message.arguments[2] as Int
                midiBus.sendControllerChange(channel, control, velocity)
                runLater { controls.progress = velocity / .127 }
            }
            "noteOff" -> {
                val channel = message.arguments[0] as Int
                val note = message.arguments[1] as Int
                val velocity = message.arguments[2] as Int
                midiBus.sendNoteOff(channel, note, velocity)
            }
            "noteOn" -> {
                val channel = message.arguments[0] as Int
                val note = message.arguments[1] as Int
                val velocity = message.arguments[2] as Int
                runLater { hits.progress = velocity / .127 }
                runLater(Duration(1000.0), op = {
                    hits.progress = 0.0
                })
                midiBus.sendNoteOn(channel, note, velocity)
            }
        }
    }
}

private data class Connection(val address: String, val port: Int)