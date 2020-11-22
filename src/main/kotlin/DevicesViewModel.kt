import androidx.compose.runtime.mutableStateOf
import java.util.ArrayList

class DevicesViewModel {

    val devicesList = mutableStateOf(ArrayList<DevicesItem>())

    var searchState = mutableStateOf(false)

    var bulbToggleState = mutableStateOf(false)

    var connectionState = mutableStateOf(false)

    var cmdRun = mutableStateOf(false)

    var brightnessState = mutableStateOf(0f)
}