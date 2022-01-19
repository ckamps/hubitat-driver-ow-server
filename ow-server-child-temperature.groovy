def version() {'v0.2.0'}

metadata {
    definition (name: 'OW-Server 1-Wire - Child - Temperature',
                namespace: 'ckamps', 
                author: 'Christopher Kampmeier',
                importUrl: 'https://raw.githubusercontent.com/ckamps/hubitat-drivers-ow-server/master/ow-server-child-temperature.groovy') {
        
        capability 'TemperatureMeasurement'
        capability 'Refresh'
    }

    preferences {
        input name: 'offset',    type: 'decimal', title: 'Temperature Offset',   description: '-n, +n or n to adjust sensor reading', range:'*..*'
        input name: 'logEnable', type: 'bool',    title: 'Enable debug logging', defaultValue: false
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()   
}

def initialize() {
    state.version = version()
}

def poll() {
    refresh()   
}

def refresh() {
    sensorId = device.deviceNetworkId

    if (logEnable) log.debug("Getting temperature for sensor: ${sensorId}")

    try {
        temp = getTemperature(sensorId)
    }
    catch (Exception e) {
        log.warn("Can't obtain temperature for sensor ${sensorId}: ${e}")
        return
    }
    
    if (logEnable) log.debug("Temperature - C: ${temp}")
    temp = (location.temperatureScale == "F") ? ((temp * 1.8) + 32) : temp
    temp = offset ? (temp + offset) : temp
    temp = temp.round(2)

    sendEvent(
        name: 'temperature',
        value: temp,
        unit: "°${location.temperatureScale}",
        descriptionText: "Temperature is ${temp}°${location.temperatureScale}",
        translatable: true
    )
  
    def nowDay = new Date().format('MMM dd', location.timeZone)
    def nowTime = new Date().format('h:mm a', location.timeZone)

    sendEvent(
        name: 'lastUpdated',
        value: nowDay + " at " + nowTime,
        displayed: false
    )
}

private float getTemperature(sensorId) {
    def uri = "http://${parent.getOwServerAddress()}/details.xml"

    response = parent.doHttpGet(uri)

    if (!response) throw new Exception("doHttpGet to get temperature returned empty response ${sensorId}")

    element = response.'**'.find{ it.ROMId == sensorId }
    
    if (!element) throw new Exception("Can't find matching ROMId element in response from OW-Server for sensor ${sensorId}")

    if (!element.Temperature) throw new Exception("Temperature element does not exist in response from OW-Server for sensor ${sensorId}")

    return(element.Temperature.toFloat())
}