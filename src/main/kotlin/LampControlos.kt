import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

@Composable
fun DeviceItemCard(deviceItem: DevicesItem, devicesViewModel: DevicesViewModel, connect: (String, Int, DevicesViewModel) -> Unit) {
    Surface(
            elevation = 4.dp,
            color = MaterialTheme.colors.surface,
            modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .padding(8.dp)
                    .clickable(onClick = {
                        connect(deviceItem.ip, deviceItem.port, devicesViewModel)
                    }),
            shape = MaterialTheme.shapes.small
    ) {
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp)
        ) {
            Column(modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(
                        deviceItem.bulbInfo["model"] ?: "no-name",
                        style = MaterialTheme.typography.h6
                )
                Text(
                        "${deviceItem.ip}:${deviceItem.port}",
                        style = MaterialTheme.typography.body2
                )
            }
            Icon(
                    Icons.Default.KeyboardArrowRight,
                    modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun TurnDevice(write: (String) -> Unit, devicesViewModel: DevicesViewModel) {
    Card(
            elevation = 4.dp,
            modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .padding(8.dp)
                    .clickable(onClick = {
                        if (devicesViewModel.bulbToggleState.value) {
                            write(Commands.CMD_OFF.replace("%id", "2"))
                        } else {
                            write(Commands.CMD_ON.replace("%id", "3"))
                        }
                    }),
            shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier
                .padding(8.dp)
        ) {
            Text(
                    text = if (devicesViewModel.bulbToggleState.value) "Turn off" else "Turn on",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Switch(
                    checked = devicesViewModel.bulbToggleState.value,
                    onCheckedChange = {
                        if (devicesViewModel.bulbToggleState.value) {
                            write(Commands.CMD_OFF.replace("%id", "2"))
                        } else {
                            write(Commands.CMD_ON.replace("%id", "3"))
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ChangeBrigthness(write: (String) -> Unit, devicesViewModel: DevicesViewModel) {
    val sliderState = remember { mutableStateOf(0f)  }

    Card(
            elevation = 4.dp,
            modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .padding(8.dp),
            shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier
                .padding(8.dp)
        ) {
            Text(
                    text = "Bright level ${devicesViewModel.brightnessState.value.toInt()}%",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Slider(
                    value = devicesViewModel.brightnessState.value / 100,
                    onValueChange = { sliderState.value = it * 100 },
                    onValueChangeEnd = { write(Commands.CMD_BRIGHTNESS.replace("%id", "4").replace("%value", "${sliderState.value}")) },
            )
        }
    }
}