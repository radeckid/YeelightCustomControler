fun handleResponse(response: BulbResult, devicesViewModel: DevicesViewModel) {
    when (response.id) {
        1 -> {
            devicesViewModel.bulbToggleState.value = response.result[0] == "on"
            devicesViewModel.brightnessState.value = response.result[1].toFloat()
        }
        2 -> {
            if (response.result[0] == "ok") {
                devicesViewModel.bulbToggleState.value = false
            }
        }
        3 -> {
            if (response.result[0] == "ok") {
                devicesViewModel.bulbToggleState.value = true
            }
        }
        4 -> {
            if (response.result[0] == "ok") {
                devicesViewModel.bulbToggleState.value = true
            }
        }
    }

    if (response.method == "props") {
        val params = response.params
        if (params.has("bright")) {
            devicesViewModel.brightnessState.value = params["bright"].asInt.toFloat()
        }
    }
}