
import javafx.application.Application
import javafx.event.EventHandler
import javafx.stage.Stage
import javafx.stage.StageStyle
import tornadofx.App
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface


fun main(args: Array<String>) {
    Application.launch(MainApp::class.java, *args)
}

class MainApp : App(MainView::class) {

    private var xOffset = .0
    private var yOffset = .0

    override fun start(stage: Stage) {
        stage.initStyle(StageStyle.UNDECORATED)
        stage.isResizable = false
        super.start(stage)

        stage.scene.onMousePressed = EventHandler {
            xOffset = stage.x - it.screenX
            yOffset = stage.y - it.screenY
        }

        stage.scene.onMouseDragged = EventHandler {
            stage.x = it.screenX + xOffset
            stage.y = it.screenY + yOffset
        }
    }
}

fun getNetworkInterfaceIpAddress(): String? {
    try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val networkInterface = en.nextElement()
            val addresses = networkInterface.inetAddresses

            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()

                if (!address.isLoopbackAddress && !address.isAnyLocalAddress && !address.isLinkLocalAddress && address is Inet4Address) {
                    val host = address.getHostAddress()

                    if (host != null && host != "") {
                        return host
                    }
                }
            }

        }
    } catch (ex: Exception) {
        System.out.println(ex.stackTrace)
    }

    return null
}

fun getInetAddress(): InetAddress? {
    try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val networkInterface = en.nextElement()
            val addresses = networkInterface.inetAddresses

            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()

                if (!address.isLoopbackAddress && !address.isAnyLocalAddress && !address.isLinkLocalAddress && address is Inet4Address) {
                    return address
                }
            }

        }
    } catch (ex: Exception) {
        System.out.println(ex.stackTrace)
    }

    return null
}