import androidx.compose.animation.animate
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.SpringSpec
import androidx.compose.desktop.AppManager
import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.schedule

const val UDP_HOST = "239.255.255.250"
const val UDP_PORT = 1982
const val message = "M-SEARCH * HTTP/1.1\r\n" +
        "HOST:$UDP_HOST:$UDP_PORT\r\n" +
        "MAN:\"ssdp:discover\"\r\n" +
        "ST:wifi_bulb\r\n"

private const val searchDelay = 5000L

private lateinit var datagramSocket: DatagramSocket
private val devicesViewModel = DevicesViewModel()

private lateinit var socket: Socket
private var bos: BufferedOutputStream? = null
private lateinit var reader: BufferedReader

fun main() {

    val notifier = Notifier()
    Window(
            title = "Yeelight Light Controler",
            size = IntSize(1000, 600),
            icon = getAppIcon(),
    ) {
//        onActive {
//            val tray = Tray().apply {
//                icon(getAppIcon())
//                menu(
//                        MenuItem(name = "Exit",
//                                onClick = {
//                                    AppManager.exit()
//                                }
//                        )
//                )
//            }
//            onDispose {
//                tray.remove()
//            }
//        }

        DesktopMaterialTheme {
            MainContent(devicesViewModel.devicesList, devicesViewModel.searchState, devicesViewModel) { write(it) }
        }
        searchDevices(devicesViewModel.devicesList, devicesViewModel.searchState)
    }
}

@Composable
private fun MainContent(
        devicesList: MutableState<ArrayList<DevicesItem>>,
        searchState: MutableState<Boolean>,
        devicesViewModel: DevicesViewModel,
        write: (String) -> Unit
) {
    val panelState = remember { PanelState() }

    val animatedSize = if (panelState.isExpanded) {
        panelState.expandedSize
    } else {
        panelState.collapsedSize
        animate(
                if (panelState.isExpanded) {
                    panelState.expandedSize
                } else {
                    panelState.collapsedSize
                }, SpringSpec(stiffness = StiffnessLow)
        )
    }
    Row {
        ResizablePanel(Modifier.width(animatedSize).fillMaxHeight(), panelState) {
            Scaffold(
                    topBar = {
                        TopAppBar(
                                title = { Text("Available devices") },
                                elevation = 8.dp,
                        )
                    },
                    bodyContent = { innerPadding ->
                        BodyContent(Modifier.padding(innerPadding), devicesList, searchState)
                    }
            )
        }
        Spacer(Modifier.fillMaxHeight().width(1.dp).background(Color.Gray))
        Box {
            SelectedLampView(devicesViewModel, write)
        }
    }
}

@Composable
fun SelectedLampView(
        devicesViewModel: DevicesViewModel,
        write: (String) -> Unit
) {
    Surface(color = MaterialTheme.colors.surface,
            modifier = Modifier
                    .padding(8.dp)
    ) {
        ScrollableColumn {
            TurnDevice(write, devicesViewModel)
            ChangeBrigthness(write, devicesViewModel)
        }
    }
}

@Composable
fun BodyContent(
        modifier: Modifier = Modifier,
        devicesList: MutableState<ArrayList<DevicesItem>>,
        searchState: MutableState<Boolean>
) {

    Column(modifier) {
        Box(modifier
                .weight(1f)
        ) {

            val state = rememberScrollState(0f)

            ScrollableColumn(
                    modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 8.dp),
                    scrollState = state
            ) {
                for (item in devicesList.value) {
                    DeviceItemCard(item, devicesViewModel) { ip, port, viewModel -> connect(ip, port, viewModel) }
                }
            }
            VerticalScrollbar(
                    modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd),
                    adapter = rememberScrollbarAdapter(state)
            )
        }
        Button(
                onClick = { searchDevices(devicesList, searchState) },
                enabled = !searchState.value,
                modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()

        ) {
            if (searchState.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(1f).wrapContentHeight())
            } else {
                Text(
                        style = MaterialTheme.typography.button,
                        text = "Search"
                )
            }
        }
    }
}

private class PanelState {
    val collapsedSize = 64.dp
    var expandedSize by mutableStateOf(320.dp)
    val expandedSizeMin = 90.dp
    var isExpanded by mutableStateOf(true)
}

@Composable
private fun ResizablePanel(
        modifier: Modifier,
        state: PanelState,
        content: @Composable () -> Unit,
) {
    val alpha = animate(if (state.isExpanded) 1f else 0f, SpringSpec(stiffness = StiffnessLow))

    Box(modifier) {
        Box(Modifier.fillMaxSize().drawLayer(alpha = alpha)) {
            content()
        }

        Icon(
                if (state.isExpanded) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                tint = if (state.isExpanded) Color.White else Color.Black,
                modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            state.isExpanded = !state.isExpanded
                        }
                        .align(Alignment.TopEnd)
        )
    }
}

private fun connect(ip: String, port: Int, devicesViewModel: DevicesViewModel) {
    GlobalScope.launch(Dispatchers.IO) {
        devicesViewModel.cmdRun.value = true
        socket = Socket(ip, port)
        socket.keepAlive = true
        bos = BufferedOutputStream(socket.getOutputStream())
        devicesViewModel.connectionState.value = true
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        write(Commands.CMD_STATE.replace("%id", "1"))

        while (devicesViewModel.cmdRun.value) {
            try {
                val value = reader.readLine()
                val response = Gson().fromJson(value, BulbResult::class.java)
                println("ReadFromLamp $value")

                handleResponse(response, devicesViewModel)

            } catch (e: Exception) {
                println("ErrorRead ${e.stackTraceToString()}")
            }
        }
    }
}

private fun write(command: String) {
    GlobalScope.launch(Dispatchers.IO) {
        if (bos != null && socket.isConnected) {
            try {
                bos!!.write(command.toByteArray())
                bos!!.flush()
            } catch (e: Exception) {
                println("WriteError ${e.stackTraceToString()}")
            }
        } else {
            println("Write INFO: bos = null or socket is closed")
        }
    }
}

private fun searchDevices(devicesList: MutableState<ArrayList<DevicesItem>>, searchState: MutableState<Boolean>) {
    devicesList.value.clear()
    searchState.value = true

    Timer().schedule(searchDelay) {
        searchState.value = false
    }

    GlobalScope.launch(Dispatchers.IO) {
        try {
            println("socket: start Datagram etc")
            datagramSocket = DatagramSocket()
            val messageByteArray = message.toByteArray()
            val dpSend = DatagramPacket(
                    messageByteArray,
                    messageByteArray.size,
                    InetAddress.getByName(UDP_HOST),
                    UDP_PORT
            )
            datagramSocket.send(dpSend)
            while (searchState.value) {
                val buf = ByteArray(1024)
                val dpReceive = DatagramPacket(buf, buf.size)
                datagramSocket.receive(dpReceive)
                val bytes: ByteArray = dpReceive.data
                val buffer = StringBuffer()
                for (index in 0..dpReceive.length) {
                    if (bytes[index] == 13.toByte()) {
                        continue
                    }
                    buffer.append(bytes[index].toChar())
                }
                println("socket got message: $buffer")
                if (!buffer.toString().contains("yeelight")) {
                    println("Received a message, not Yeelight bulb!")
                }
                val infos = buffer.toString().split("\n")
                val bulbInfo = HashMap<String, String>()
                for (str in infos) {
                    val index = str.indexOf(":")
                    if (index == -1) {
                        continue
                    }
                    val title = str.substring(0, index)
                    val value = str.substring(index + 1)
                    bulbInfo[title] = value
                }
                if (!hasAdd(bulbInfo, devicesList)) {
                    val location = bulbInfo["Location"]!!.split(":")
                    devicesList.value.add(
                            DevicesItem(
                                    bulbInfo,
                                    location[1].substring(2, location[1].length),
                                    location[2].toInt()
                            )
                    )
                }
            }
        } catch (ex: Exception) {
            println("socketError: ${ex.stackTraceToString()}")
        }
    }
}

private fun hasAdd(bulbInfo: HashMap<String, String>, devicesList: MutableState<ArrayList<DevicesItem>>): Boolean {
    for (info in devicesList.value) {
        println("MainActivity hasAdd: location params = ${bulbInfo["Location"]}")
        if (info.bulbInfo["Location"].equals(bulbInfo["Location"])) {
            return true
        }
    }
    return false
}